package org.texttechnologylab.uce.common.services;

import com.google.gson.Gson;
import com.pgvector.PGvector;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.HttpStatusException;
import org.texttechnologylab.models.dto.RAGCompleteFullDto;
import org.texttechnologylab.models.dto.RAGCompleteFullMessageToolCallDto;
import org.texttechnologylab.models.rag.Tool;
import org.texttechnologylab.models.rag.ToolFunction;
import org.texttechnologylab.uce.common.config.CommonConfig;
import org.texttechnologylab.uce.common.config.uceConfig.RAGModelConfig;
import org.texttechnologylab.uce.common.exceptions.ExceptionUtils;
import org.texttechnologylab.uce.common.models.corpus.Document;
import org.texttechnologylab.uce.common.models.dto.*;
import org.texttechnologylab.uce.common.models.rag.*;
import org.texttechnologylab.uce.common.models.util.HealthStatus;
import org.texttechnologylab.uce.common.security.DocumentAccessManager;
import org.texttechnologylab.uce.common.utils.SystemStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.texttechnologylab.uce.common.models.rag.RAGChatMessage.cleanThinkTag;

/**
 * Service class for RAG: Retrieval Augmented Generation
 */
public class RAGService {
    private static final Logger logger = LogManager.getLogger(RAGService.class);

    private PostgresqlDataInterface_Impl postgresqlDataInterfaceImpl = null;
    private Connection vectorDbConnection = null;
    private CommonConfig config;

    static final Pattern patternAmount = Pattern.compile("\\d+");

    private McpSyncClient mcpClient;

    private final DocumentAccessManager accessManager;

    public RAGService(PostgresqlDataInterface_Impl postgresqlDataInterfaceImpl) {
        this.postgresqlDataInterfaceImpl = postgresqlDataInterfaceImpl;
        this.accessManager = postgresqlDataInterfaceImpl.getAccessManager();
        TestConnection();
    }

    private static McpSyncClient initMcpClient() {
        // own url?
        HttpClientStreamableHttpTransport transportProvider = HttpClientStreamableHttpTransport
                .builder("http://localhost:4567/mcp")
                .build();

        McpSyncClient client = McpClient.sync(transportProvider)
                .requestTimeout(Duration.ofSeconds(60))  // TODO config
                .capabilities(McpSchema.ClientCapabilities
                        .builder()
                        .roots(true)
                        .build()
                )
                .build();

        client.initialize();

        return client;
    }

    public void TestConnection(){
        try {
            this.config = new CommonConfig();
            this.vectorDbConnection = setupVectorDbConnection();

            var test = ExceptionUtils.tryCatchLog(
                    () -> getEmbeddingForText("This is an embedding test."),
                    (ex) -> SystemStatus.RagServiceStatus = new HealthStatus(false, "Embedding the text failed", ex));

            if(test != null){
                SystemStatus.RagServiceStatus = new HealthStatus(true, "", null);
            }
        } catch (Exception ex) {
            SystemStatus.RagServiceStatus = new HealthStatus(false, "Couldn't connect to the vector database.", ex);
        }
    }

    public String postRAGDocTitle(String userMessage, String part, RAGModelConfig modelConfig) throws URISyntaxException, IOException, InterruptedException {
        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        var url = config.getRAGWebserverBaseUrl() + "rag/complete";
        var config = new CommonConfig();

        // Prepare workload
        var gson = new Gson();
        var params = new HashMap<String, Object>();

        params.put("model", modelConfig.getModel());
        params.put("apiKey", modelConfig.getApiKey());
        params.put("url", modelConfig.getUrl());

        // Add the chat history
        var promptMessages = new ArrayList<HashMap<String, String>>();
        {
            var promptMessage = new HashMap<String, String>();
            promptMessage.put("role", Roles.SYSTEM.name().toLowerCase());
            promptMessage.put("content", "You are a helpful assistant that analyzes user messages given as ###Message to determine if the user specifies or mentions a document " + part + ". Extract the first " + part + " without any changes. This step prepares the system to retrieve the requested documents based on its " + part + " for the next stage.\nOnly return the first " + part + " the user wants AND NOTHING ELSE. If nothing is mentioned or implied, return null.");
            promptMessages.add(promptMessage);
            params.put("promptMessages", promptMessages);
        }
        {
            var promptMessage = new HashMap<String, String>();
            promptMessage.put("role", Roles.USER.name().toLowerCase());
            promptMessage.put("content", "###Message: " + userMessage + "\n\nYour task: Extract ONLY the first " + part + " of the document the user mentions in this message AND NOTHING ELSE. If nothing is mentioned, return null.");
            promptMessages.add(promptMessage);
            params.put("promptMessages", promptMessages);
        }
        var jsonData = gson.toJson(params);

        // Create request
        var request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .build();
        // Send request and get response
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        var statusCode = response.statusCode();
        if (statusCode != 200) throw new HttpStatusException("Request returned invalid status code: " + statusCode, statusCode, url);

        var responseBody = response.body();
        var ragCompleteDto = gson.fromJson(responseBody, RAGCompleteDto.class);
        if (ragCompleteDto.getStatus() != 200) throw new HttpStatusException(
                "Webservice replied with an internally wrong status code, something went wrong there: " + ragCompleteDto.getStatus(), statusCode, url);

        return cleanThinkTag(ragCompleteDto.getMessage());
    }

    public Integer postRAGAmountDocs(String userMessage, RAGModelConfig modelConfig) throws URISyntaxException, IOException, InterruptedException {
        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        var url = config.getRAGWebserverBaseUrl() + "rag/complete";
        var config = new CommonConfig();

        // Prepare workload
        var gson = new Gson();
        var params = new HashMap<String, Object>();

        params.put("model", modelConfig.getModel());
        params.put("apiKey", modelConfig.getApiKey());
        params.put("url", modelConfig.getUrl());

        // Add the chat history
        var promptMessages = new ArrayList<HashMap<String, String>>();
        {
            var promptMessage = new HashMap<String, String>();
            promptMessage.put("role", Roles.SYSTEM.name().toLowerCase());
            promptMessage.put("content", "You are a helpful assistant that analyzes user messages given as ###Message to determine if the user specifies a desired number of documents or articles. Extract the number if provided (e.g., \"a few\", \"3\", \"top 5\", \"several\") and normalize it into an exact count when possible. This step prepares the system to retrieve the requested number of relevant documents for the next stage.\nOnly return the number of documents the user wants. If no number is mentioned or implied, return null.");
            promptMessages.add(promptMessage);
            params.put("promptMessages", promptMessages);
        }
        {
            var promptMessage = new HashMap<String, String>();
            promptMessage.put("role", Roles.USER.name().toLowerCase());
            promptMessage.put("content", "###Message: " + userMessage + "\n\nExtract the number of documents the user wants from this message. If no number is mentioned, return null.");
            promptMessages.add(promptMessage);
            params.put("promptMessages", promptMessages);
        }
        var jsonData = gson.toJson(params);

        // Create request
        var request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .build();
        // Send request and get response
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        var statusCode = response.statusCode();
        if (statusCode != 200) throw new HttpStatusException("Request returned invalid status code: " + statusCode, statusCode, url);

        var responseBody = response.body();
        var ragCompleteDto = gson.fromJson(responseBody, RAGCompleteDto.class);
        if (ragCompleteDto.getStatus() != 200) throw new HttpStatusException(
                "Webservice replied with an internally wrong status code, something went wrong there: " + ragCompleteDto.getStatus(), statusCode, url);

        Matcher matcher = patternAmount.matcher(cleanThinkTag(ragCompleteDto.getMessage()));
        if (matcher.find()) {
            return Integer.valueOf(matcher.group());
        }

        return null;
    }

    /**
     * Queries our RAG webserver which decides whether we should fetch new context or not.
     *
     * @return
     */
    public Integer postRAGContextNeeded(String userInput) throws URISyntaxException, IOException, InterruptedException {
        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        var url = config.getRAGWebserverBaseUrl() + "rag/context";

        // Prepare workload
        var gson = new Gson();
        var params = new HashMap<String, Object>();

        // Its better to end each sentence with a ? for the webservice.
        if(!userInput.endsWith("?")) userInput = userInput + "?";
        params.put("userInput", userInput);
        var jsonData = gson.toJson(params);

        // Create request
        var request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .build();
        // Send request and get response
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        var statusCode = response.statusCode();
        if (statusCode != 200) throw new HttpStatusException("Request returned invalid status code: " + statusCode, statusCode, url);

        var responseBody = response.body();
        var ragCompleteDto = gson.fromJson(responseBody, RAGCompleteDto.class);
        if (ragCompleteDto.getStatus() != 200) throw new HttpStatusException(
                "Webservice replied with an internally wrong status code, something went wrong there: " + ragCompleteDto.getStatus(), statusCode, url);

        return Integer.parseInt(ragCompleteDto.getMessage());
    }

    /**
     * Queries our RAG webserver with a list of prefaced prompts to get the new message from our llm
     *
     * @return
     */
    public String postNewRAGPrompt(RAGChatState chatState) throws URISyntaxException, IOException, InterruptedException {
        List<RAGChatMessage> chatHistory = chatState.getMessages();
        RAGModelConfig modelConfig = chatState.getModel();

        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        var url = config.getRAGWebserverBaseUrl() + "rag/complete";

        var gson = new Gson();

        // repeated requests if the models is performing tool calls
        while(true) {
            // Prepare workload
            var params = new HashMap<String, Object>();

            params.put("model", modelConfig.getModel());
            params.put("apiKey", modelConfig.getApiKey());
            params.put("url", modelConfig.getUrl());

            // Add the chat history
            var promptMessages = buildPromptMessages(chatHistory);
            params.put("promptMessages", promptMessages);

            // Add MCP tools
            // TODO initialize MCP client
            // TODO should we use only one client for all requests or per chat? when to create?
            mcpClient = initMcpClient();

            // Get available tools from MCP servers
            var mcpTools = mcpClient.listTools();
            var usingTools = mcpTools != null && !mcpTools.tools().isEmpty();
            if (usingTools) {
                var tools = new ArrayList<Tool>();
                // TODO check format
                for (var tool : mcpTools.tools()) {
                    var toolFunction = new ToolFunction(tool.name(), tool.description(), tool.inputSchema());
                    tools.add(new Tool(toolFunction));
                }
                params.put("tools", tools);
            }

            var jsonData = gson.toJson(params);

            // Create request
            var request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                    .build();
            // Send request and get response
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            var statusCode = response.statusCode();
            if (statusCode != 200)
                throw new HttpStatusException("Request returned invalid status code: " + statusCode, statusCode, url);

            var responseBody = response.body();
            if (usingTools) {
                // if using tools, we have to check if the response is a tool call or a final message and act accordingly
                var ragCompleteDto = gson.fromJson(responseBody, RAGCompleteFullDto.class);
                if (ragCompleteDto.getMessage().getMessage().getToolCalls() != null && !ragCompleteDto.getMessage().getMessage().getToolCalls().isEmpty()) {
                    // TODO only add if not empty?
                    var responseContent = ragCompleteDto.getMessage().getMessage().getContent();
                    if (responseContent == null || responseContent.isBlank()) {
                        responseContent = "[Tool call requested.]";
                    }
                    var responseMessage = new RAGChatMessage();
                    responseMessage.setRole(Roles.ASSISTANT);
                    responseMessage.setPrompt(responseContent);
                    responseMessage.setMessage(responseContent);
                    chatState.addMessage(responseMessage);

                    var answer = new StringBuilder();

                    for (RAGCompleteFullMessageToolCallDto toolCall : ragCompleteDto.getMessage().getMessage().getToolCalls()) {
                        if (toolCall.getFunction() != null) {
                            McpSchema.CallToolRequest toolRequest = McpSchema.CallToolRequest
                                    .builder()
                                    .name(toolCall.getFunction().getName())
                                    .arguments(toolCall.getFunction().getArguments())
                                    .build();
                            McpSchema.CallToolResult toolResult = mcpClient.callTool(toolRequest);
                            // TODO handle error
                            if (!toolResult.isError()) {
                                for (McpSchema.Content content : toolResult.content()) {
                                    if (content.type().equals("text")) {
                                        McpSchema.TextContent textContent = (McpSchema.TextContent) content;
                                        // TODO format result depending on model?
                                        if (!answer.isEmpty()) {
                                            answer.append("\n\n");
                                        }
                                        answer.append("Tool result for function \"").append(toolCall.getFunction().getName()).append("\":\n");
                                        answer.append("<tool>\n").append(textContent.text()).append("\n</tool>");
                                    } else {
                                        // TODO support more tool results
                                        logger.error("RAGService: Tool result content type not supported: " + content.type());
                                    }
                                }
                            }
                        }
                    }

                    if (answer.isEmpty()) {
                        answer.append("[Failed to get tool result.]");
                    }
                    var answerString = answer.toString();

                    var toolMessage = new RAGChatMessage();
                    toolMessage.setRole(Roles.TOOL);
                    toolMessage.setPrompt(answerString);
                    toolMessage.setMessage(answerString);
                    chatState.addMessage(toolMessage);
                }
                else {
                    // no tool call, return message as before
                    return ragCompleteDto.getMessage().getMessage().getContent();
                }
            } else {
                var ragCompleteDto = gson.fromJson(responseBody, RAGCompleteDto.class);
                if (ragCompleteDto.getStatus() != 200) throw new HttpStatusException(
                        "Webservice replied with an internally wrong status code, something went wrong there: " + ragCompleteDto.getStatus(), statusCode, url);
                return ragCompleteDto.getMessage();
            }
        }
    }

    private List<Map<String, Object>> buildPromptMessages(List<RAGChatMessage> chatHistory) {
        var promptMessages = new ArrayList<Map<String, Object>>();

        var history = chatHistory.stream().sorted(Comparator.comparing(RAGChatMessage::getCreated)).toList();
        // Make sure the last message has images
        var lastMessageHasImage = history.getLast().getImages() != null && !history.getLast().getImages().isEmpty();
        for (var chatInd = 0; chatInd < history.size(); chatInd++ ) {
            var chat = history.get(chatInd);

            // TODO make type instead of Map Object
            var promptMessage = new HashMap<String, Object>();
            promptMessage.put("role", chat.getRole().name().toLowerCase());
            promptMessage.put("content", chat.getPrompt());

            boolean isLast = (chatInd == chatHistory.size() - 1);

            // TODO we only send the images in the last message if available, else we use all images
            if (lastMessageHasImage && isLast) {
                if (chat.getImages() != null && !chat.getImages().isEmpty()) {
                    var images = new ArrayList<String>();
                    for (var image : chat.getImages()) {
                        // Use the base64 encoded image
                        images.add(image.getSrcResized());
                    }
                    promptMessage.put("images", images);
                }
            }

            promptMessages.add(promptMessage);
        }

        return promptMessages;
    }

    /**
     * Get the streaming response from the LLM. We cache the results internally, so that the external tools
     * can poll for new messages easily.
     * @param chatState
     * @return
     * @throws URISyntaxException
     * @throws IOException
     * @throws InterruptedException
     */
    public void postNewRAGPromptStreaming(
            RAGChatState chatState,
            List<DocumentChunkEmbedding> nearestDocumentChunkEmbeddings,
            List<Document> foundDocuments
    ) throws URISyntaxException, IOException, InterruptedException {
        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        var url = config.getRAGWebserverBaseUrl() + "rag/complete/stream";

        var gson = new Gson();
        var params = new HashMap<String, Object>();

        RAGModelConfig modelConfig = chatState.getModel();
        params.put("model", modelConfig.getModel());
        params.put("apiKey", modelConfig.getApiKey());
        params.put("url", modelConfig.getUrl());

        // Add the chat history
        // TODO tools+streaming not supported, check ollama blog post for this
        var promptMessages = buildPromptMessages(chatState.getMessages());
        params.put("promptMessages", promptMessages);

        // Add an empty chat message answer that will be filled after creating the history for the request
        var systemResponseMessage = new RAGChatMessage();
        systemResponseMessage.setDone(false);
        systemResponseMessage.setRole(Roles.ASSISTANT);
        systemResponseMessage.setPrompt("");
        systemResponseMessage.setMessage("");
        systemResponseMessage.setContextDocument_Ids(nearestDocumentChunkEmbeddings.stream().map(DocumentChunkEmbedding::getDocument_id).toList());
        systemResponseMessage.setContextDocuments(new ArrayList<>(foundDocuments));
        chatState.addMessage(systemResponseMessage);

        var jsonData = gson.toJson(params);

        // For both individually, connection/first data and then the streaming response
        var ragStreamTimeout = 10;

        // Create request
        var request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMinutes(ragStreamTimeout))  // timeout for connection and first data
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .build();
        // Send request and get response
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        var statusCode = response.statusCode();
        if (statusCode != 200) throw new HttpStatusException("Request returned invalid status code: " + statusCode, statusCode, url);

        // Run in executor as we want to make sure that this will stop after N minutes
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Void> future = executor.submit(() -> {
                StringBuilder streamingMessage = new StringBuilder();
                try (var reader = new BufferedReader(new InputStreamReader(response.body()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.isBlank()) {
                            var ragCompleteDto = gson.fromJson(line, RAGCompleteStreamingDto.class);

                            // save the updated message in the chat state, this will allow the user to poll for updates
                            streamingMessage.append(ragCompleteDto.getMessage().getContent());
                            var updatedMessage = streamingMessage.toString();
                            systemResponseMessage.setPrompt(updatedMessage);
                            systemResponseMessage.setMessage(updatedMessage);
                            systemResponseMessage.setDone(ragCompleteDto.isDone());
                            // TODO also update the "created" timestamp or keep the start?
                        }
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return null;
            });

            // wait max N minutes for completion
            future.get(ragStreamTimeout, TimeUnit.MINUTES);

        } catch (TimeoutException e) {
            logger.error("RAG streaming response timed out after " + ragStreamTimeout + " minutes for chat " + chatState.getChatId() + ": ", e);
            // TODO unify error handling of rag...
            var currentMessage = systemResponseMessage.getMessage();
            currentMessage += "\n\n[UCE: Timed out after " + ragStreamTimeout + " minutes. Please try again later.]";
            systemResponseMessage.setPrompt(currentMessage);
            systemResponseMessage.setMessage(currentMessage);
            systemResponseMessage.setDone(true);
            try {
                response.body().close(); // Close InputStream to unblock the reader
            } catch (IOException ignored) {}
        } catch (ExecutionException e) {
            throw new RuntimeException("RAG streaming reading error for chat " + chatState.getChatId(), e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdownNow();
        }

        // TODO check if the message contains any content, else set error message?
    }

    /**
     * A function that fetches the vector embeddings of a given text through our python webserver
     */
    public float[] getEmbeddingForText(String text) throws IOException, InterruptedException, URISyntaxException {
        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        var url = config.getRAGWebserverBaseUrl() + "embed";
        var embeddingBackend = config.getEmbeddingBackend();
        var embeddingParams = config.getEmbeddingParameters();
        var embeddingTimeout = config.getEmbeddingTimeout();

        // Prepare workload
        var gson = new Gson();
        var params = new HashMap<String, Object>();
        params.put("text", text);
        params.put("backend", embeddingBackend);
        params.put("config", embeddingParams);
        var jsonData = gson.toJson(params);

        // Create request
        var request = HttpRequest
                .newBuilder()
                .uri(new URI(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .timeout(Duration.ofSeconds(embeddingTimeout))
                .build();

        // Send request and get response
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        var statusCode = response.statusCode();
        if (statusCode != 200) throw new HttpStatusException("Request returned invalid status code: " + statusCode, statusCode, url);

        var responseBody = response.body();
        var ragEmbedDto = gson.fromJson(responseBody, RAGEmbedDto.class);
        if (ragEmbedDto.getStatus() != 200) throw new HttpStatusException(
                "Webservice replied with an internally wrong status code, something went wrong there: " + ragEmbedDto.getStatus(), statusCode, url);

        return ragEmbedDto.getMessage();
    }

    /**
     * We are using postgresql vector extension, which won't work with hibernate. Hence, we open a different connection
     * solely for the RAG service.
     */
    private Connection setupVectorDbConnection() throws ClassNotFoundException, SQLException {
        Class.forName(config.getPostgresqlProperty("connection.driver_class"));
        var connection = DriverManager.getConnection(config.getPostgresqlProperty("hibernate.connection.url"),
                config.getPostgresqlProperty("hibernate.connection.username"),
                config.getPostgresqlProperty("hibernate.connection.password"));

        // After we have the connection, we set up some vector extension requirements.
        var setupStmt = connection.createStatement();
        setupStmt.executeUpdate("CREATE EXTENSION IF NOT EXISTS vector");
        PGvector.addVectorType(connection);

        return connection;
    }

}
