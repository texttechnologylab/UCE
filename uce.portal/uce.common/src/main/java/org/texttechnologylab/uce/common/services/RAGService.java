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
import org.texttechnologylab.uce.common.exceptions.DatabaseOperationException;
import org.texttechnologylab.uce.common.exceptions.ExceptionUtils;
import org.texttechnologylab.uce.common.models.corpus.Document;
import org.texttechnologylab.uce.common.models.corpus.KeywordDistribution;
import org.texttechnologylab.uce.common.models.corpus.Sentence;
import org.texttechnologylab.uce.common.models.dto.*;
import org.texttechnologylab.uce.common.models.rag.*;
import org.texttechnologylab.uce.common.models.util.HealthStatus;
import org.texttechnologylab.uce.common.utils.SystemStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
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

    public RAGService(PostgresqlDataInterface_Impl postgresqlDataInterfaceImpl) {
        this.postgresqlDataInterfaceImpl = postgresqlDataInterfaceImpl;
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

    public <T extends KeywordDistribution> T getTextKeywordDistribution(Class<T> clazz, String text) throws
            URISyntaxException,
            IOException,
            InterruptedException,
            NoSuchMethodException,
            InvocationTargetException,
            InstantiationException,
            IllegalAccessException {
        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        var url = config.getRAGWebserverBaseUrl() + "topic-modelling";

        // Prepare workload
        var gson = new Gson();
        var params = new HashMap<String, Object>();
        params.put("text", text);
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
        var result = gson.fromJson(responseBody, TopicModellingDto.class);
        if (result.getStatus() != 200) throw new HttpStatusException(
                "Webservice replied with an internally wrong status code, something went wrong there: " + result.getStatus(), statusCode, url);

        // Map the dto to our datastructures
        T KeywordDistribution = clazz.getDeclaredConstructor().newInstance();

        // This looks shit yikes. Happens.
        if (result.getRakeKeywords().size() > 2) {
            KeywordDistribution.setRakeTopicOne(result.getRakeKeywords().get(0));
            KeywordDistribution.setRakeTopicTwo(result.getRakeKeywords().get(1));
            KeywordDistribution.setRakeTopicThree(result.getRakeKeywords().get(2));
        }
        if (result.getYakeKeywords().size() > 4) {
            KeywordDistribution.setYakeTopicOne(result.getYakeKeywords().get(0));
            KeywordDistribution.setYakeTopicTwo(result.getYakeKeywords().get(1));
            KeywordDistribution.setYakeTopicThree(result.getYakeKeywords().get(2));
            KeywordDistribution.setYakeTopicFour(result.getYakeKeywords().get(3));
            KeywordDistribution.setYakeTopicFive(result.getYakeKeywords().get(4));
        }

        return KeywordDistribution;
    }


    /**
     * Given the corpusId, returns a fully renderd tsne plot from our python webserver as a string
     *
     * @param corpusId
     * @return
     */
    public String getCorpusTsnePlot(long corpusId) throws DatabaseOperationException, URISyntaxException, IOException, InterruptedException, SQLException {
        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        var url = config.getRAGWebserverBaseUrl() + "plot/tsne";

        // Prepare workload
        var gson = new Gson();
        var params = new HashMap<String, Object>();

        // Get all documents of this corpus, loop through them, get the embeddings and
        // then send a request to our webserver.
        var corpusDocuments = postgresqlDataInterfaceImpl.getDocumentsByCorpusId(corpusId, 0, 9999999, null);
        var labels = new ArrayList<String>();
        var embeddings = new ArrayList<float[]>();
        for (var document : corpusDocuments) {
            if (document.getDocumentKeywordDistribution() == null) continue;
                /* Probably best to average the embeddings of each document paragraph to one embedding
                 > Update: yes, let's go with it. We mean pool the multiple embeddings of a document if needed
                var pooledEmbedding = EmbeddingUtils.meanPooling(getDocumentChunkEmbeddingsOfDocument(document.getId())
                        .stream()
                        .map(DocumentChunkEmbedding::getEmbedding)
                        .toList());
                if(pooledEmbedding == null) continue;
                embeddings.add(pooledEmbedding);*/
            // > Update2: We now already have a single embedding representation of a doc
            var documentEmbedding = getDocumentEmbeddingOfDocument(document.getId());
            if (documentEmbedding == null) continue;
            embeddings.add(documentEmbedding.getTsne2d());

            // The labels are the topics of that document
            labels.add(document.getDocumentKeywordDistribution().getYakeTopicOne());
        }
        params.put("labels", labels);
        params.put("embeddings", embeddings);
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
        var plotTsneDto = gson.fromJson(responseBody, PlotTsneDto.class);
        if (plotTsneDto.getStatus() != 200) throw new HttpStatusException(
                "Webservice replied with an internally wrong status code, something went wrong there: " + plotTsneDto.getStatus(), statusCode, url);

        return plotTsneDto.getPlot();
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
     * Gets the closest document embeddings from a given text controlled by the range variable
     *
     * @param range
     * @return
     */
    public List<DocumentEmbedding> getClosest3dDocumentEmbeddingsOfCorpus(float[] tsne3d, int range, long corpusId) throws SQLException {
        var query = "SELECT * FROM documentembeddings e "
                + "JOIN document d ON e.document_id = d.id "
                + "WHERE d.corpusid = ? "
                + "ORDER BY e.tsne3d <-> ? "
                + "LIMIT ?";
        var statement = vectorDbConnection.prepareStatement(query);
        statement.setObject(1, corpusId);
        statement.setObject(2, new PGvector(tsne3d));
        statement.setInt(3, range);
        var resultSet = statement.executeQuery();
        return buildDocumentEmbeddingsFromResultSet(resultSet);
    }

    /**
     * Gets the one embedding of a document. Can return NULL.
     *
     * @return
     */
    public DocumentEmbedding getDocumentEmbeddingOfDocument(long documentId) throws SQLException {
        var query = "SELECT * FROM documentembeddings WHERE document_id = ?";
        var statement = vectorDbConnection.prepareStatement(query);
        statement.setLong(1, documentId);
        var resultSet = statement.executeQuery();
        // We return the first found docucment embedding as there should be only one.
        var embeddings = buildDocumentEmbeddingsFromResultSet(resultSet);
        if (!embeddings.isEmpty()) return embeddings.stream().findFirst().get();
        return null;
    }

    /**
     * Given a list of docment, returns the list of DocumentEmbeddings from that.
     *
     * @param documentIds
     * @return
     */
    public List<DocumentEmbedding> getManyDocumentEmbeddingsOfDocuments(List<Long> documentIds) throws SQLException {
        List<DocumentEmbedding> embeddings = new ArrayList<>();
        if (documentIds == null || documentIds.isEmpty()) {
            return embeddings;
        }

        // Construct the query with an IN clause using placeholders
        // SQL injection isn't really a threat case here, but still, use the "?" syntax to ensure proper injection
        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM documentembeddings WHERE document_id IN (");
        String placeholders = String.join(",", documentIds.stream().map(id -> "?").toArray(String[]::new));
        queryBuilder.append(placeholders).append(")");

        var query = queryBuilder.toString();
        var statement = vectorDbConnection.prepareStatement(query);

        // Set the document IDs in the prepared statement
        for (int i = 0; i < documentIds.size(); i++) {
            statement.setLong(i + 1, documentIds.get(i));
        }

        var resultSet = statement.executeQuery();
        embeddings = buildDocumentEmbeddingsFromResultSet(resultSet);
        return embeddings;
    }

    private ArrayList<DocumentEmbedding> buildDocumentEmbeddingsFromResultSet(ResultSet resultSet) throws SQLException {
        var embeddings = new ArrayList<DocumentEmbedding>();
        while (resultSet.next()) {
            var embedding = new DocumentEmbedding();
            embedding.setEmbedding(resultSet.getObject("embedding") != null ? ((PGvector) resultSet.getObject("embedding")).toArray() : null);
            embedding.setTsne3d(resultSet.getObject("tsne3d") != null ? ((PGvector) resultSet.getObject("tsne3d")).toArray() : null);
            embedding.setTsne2d(resultSet.getObject("tsne2d") != null ? ((PGvector) resultSet.getObject("tsne2d")).toArray() : null);
            embedding.setDocument_id(resultSet.getLong("document_id"));
            embedding.setId(resultSet.getLong("id"));

            if(embedding.getEmbedding() != null) embeddings.add(embedding);
        }
        return embeddings;
    }

    /**
     * Gets all embedding chunks of a document
     *
     * @return
     */
    public ArrayList<DocumentChunkEmbedding> getDocumentChunkEmbeddingsOfDocument(long documentId) throws SQLException {
        var query = "SELECT * FROM documentchunkembeddings WHERE document_id = ?";
        var statement = vectorDbConnection.prepareStatement(query);
        statement.setLong(1, documentId);
        var resultSet = statement.executeQuery();
        return buildDocumentChunkEmbeddingsFromResultSet(resultSet);
    }

    /**
     * Gets the closest document embeddings from a given text controlled by the range variable
     *
     * @param text
     * @param range
     * @return
     */
    public List<DocumentChunkEmbedding> getClosestDocumentChunkEmbeddings(String text, int range, long corpusId)
            throws SQLException, IOException, URISyntaxException, InterruptedException {
        // If the corpusid = -1, then we look at ANY document. Otherwise, only at those documentchunkembeddings from
        // a document that is in the corpus.
        var query = "";
        if (corpusId == -1) {
            query = "SELECT * FROM documentchunkembeddings e "
                    + "WHERE TRIM(COALESCE(e.coveredtext, '')) <> '' "  // Checks if coveredtext is not null and not empty
                    + "ORDER BY e.embedding <-> ? "
                    + "LIMIT ?";
        } else {
            // Filter by corpusid
            query = "SELECT * FROM documentchunkembeddings e "
                    + "JOIN document d ON e.document_id = d.id "
                    + "WHERE d.corpusid = ? and TRIM(COALESCE(e.coveredtext, '')) <> '' "
                    + "ORDER BY e.embedding <-> ? "
                    + "LIMIT ?";
        }
        var statement = vectorDbConnection.prepareStatement(query);
        if (corpusId == -1) {
            statement.setObject(1, new PGvector(getEmbeddingForText(text)));
            statement.setInt(2, range);
        } else {
            statement.setLong(1, corpusId);
            statement.setObject(2, new PGvector(getEmbeddingForText(text)));
            statement.setInt(3, range);
        }
        var resultSet = statement.executeQuery();
        return buildDocumentChunkEmbeddingsFromResultSet(resultSet);
    }

    private ArrayList<DocumentChunkEmbedding> buildDocumentChunkEmbeddingsFromResultSet(ResultSet resultSet) throws SQLException {
        var embeddings = new ArrayList<DocumentChunkEmbedding>();
        while (resultSet.next()) {
            var embedding = new DocumentChunkEmbedding(resultSet.getInt("beginn"), resultSet.getInt("endd"));
            embedding.setCoveredText(resultSet.getString("coveredtext"));
            embedding.setEmbedding(resultSet.getObject("embedding") != null ? ((PGvector) resultSet.getObject("embedding")).toArray() : null);
            embedding.setTsne3D(resultSet.getObject("tsne3d") != null ? ((PGvector) resultSet.getObject("tsne3d")).toArray() : null);
            embedding.setTsne2D(resultSet.getObject("tsne2d") != null ? ((PGvector) resultSet.getObject("tsne2d")).toArray() : null);
            embedding.setDocument_id(resultSet.getLong("document_id"));
            embedding.setId(resultSet.getLong("id"));

            if(embedding.getEmbedding() != null) embeddings.add(embedding);
        }
        return embeddings;
    }

    /**
     * Gets all embedding sentences of a document
     *
     * @return
     */
    public ArrayList<DocumentSentenceEmbedding> getDocumentSentenceEmbeddingsOfDocument(long documentId) throws SQLException {
        var query = "SELECT * FROM documentsentenceembeddings WHERE document_id = ?";
        var statement = vectorDbConnection.prepareStatement(query);
        statement.setLong(1, documentId);
        var resultSet = statement.executeQuery();
        return buildDocumentSentenceEmbeddingsFromResultSet(resultSet);
    }


    private ArrayList<DocumentSentenceEmbedding> buildDocumentSentenceEmbeddingsFromResultSet(ResultSet resultSet) throws SQLException {
        var embeddings = new ArrayList<DocumentSentenceEmbedding>();
        while (resultSet.next()) {
            var embedding = new DocumentSentenceEmbedding();
            embedding.setEmbedding(resultSet.getObject("embedding") != null ? ((PGvector) resultSet.getObject("embedding")).toArray() : null);
            embedding.setTsne3d(resultSet.getObject("tsne3d") != null ? ((PGvector) resultSet.getObject("tsne3d")).toArray() : null);
            embedding.setTsne2d(resultSet.getObject("tsne2d") != null ? ((PGvector) resultSet.getObject("tsne2d")).toArray() : null);
            embedding.setDocument_id(resultSet.getLong("document_id"));
            embedding.setSentence_id(resultSet.getLong("sentence_id"));
            embedding.setId(resultSet.getLong("id"));

            if(embedding.getEmbedding() != null) embeddings.add(embedding);
        }
        return embeddings;
    }

    /**
     *  Returns true if the given document by its id has documentchunkembeddings in the database.
     */
    public boolean documentHasDocumentEmbedding(long documentId) throws SQLException {
        String query = "SELECT COUNT(*) FROM documentembeddings WHERE document_id = ?";
        try (var statement = vectorDbConnection.prepareStatement(query)) {
            statement.setLong(1, documentId);
            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    return count > 0;
                }
            }
        }
        return false;
    }

    /**
     * Saves a document embedding.
     */
    public void saveDocumentEmbedding(DocumentEmbedding documentEmbedding) throws SQLException {
        String query = "INSERT INTO documentembeddings (document_id, embedding, tsne2d, tsne3d) VALUES (?, ?, ?, ?)";
        executeUpdate(query,
                documentEmbedding.getDocument_id(),
                new PGvector(documentEmbedding.getEmbedding()),
                new PGvector(documentEmbedding.getTsne2d()),
                new PGvector(documentEmbedding.getTsne3d()));
    }

    /**
     * Updates a document embedding.
     */
    public void updateDocumentEmbedding(DocumentEmbedding documentEmbedding) throws SQLException {
        String query = "UPDATE documentembeddings SET embedding = ?, tsne2d = ?, tsne3d = ? WHERE document_id = ?";
        executeUpdate(query,
                new PGvector(documentEmbedding.getEmbedding()),
                new PGvector(documentEmbedding.getTsne2d()),
                new PGvector(documentEmbedding.getTsne3d()),
                documentEmbedding.getDocument_id());
    }

    /**
     *  Returns true if the given document by its id has documentchunkembeddings in the database.
     */
    public boolean documentHasDocumentChunkEmbeddings(long documentId) throws SQLException {
        String query = "SELECT COUNT(*) FROM documentchunkembeddings WHERE document_id = ?";
        try (var statement = vectorDbConnection.prepareStatement(query)) {
            statement.setLong(1, documentId);
            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    return count > 0;
                }
            }
        }
        return false;
    }

    /**
     * Saves a document chunk embedding.
     */
    public void saveDocumentChunkEmbedding(DocumentChunkEmbedding documentChunkEmbedding) throws SQLException {
        String query = "INSERT INTO documentchunkembeddings (document_id, embedding, coveredtext, beginn, endd, tsne2d, tsne3d) VALUES (?, ?, ?, ?, ?, ?, ?)";
        executeUpdate(query,
                documentChunkEmbedding.getDocument_id(),
                new PGvector(documentChunkEmbedding.getEmbedding()),
                documentChunkEmbedding.getCoveredText(),
                documentChunkEmbedding.getBegin(),
                documentChunkEmbedding.getEnd(),
                new PGvector(documentChunkEmbedding.getTsne2D()),
                new PGvector(documentChunkEmbedding.getTsne3D()));
    }

    /**
     * Updates a document chunk embedding.
     */
    public void updateDocumentChunkEmbedding(DocumentChunkEmbedding documentChunkEmbedding) throws SQLException {
        String query = "UPDATE documentchunkembeddings SET embedding = ?, coveredtext = ?, beginn = ?, endd = ?, tsne2d = ?, tsne3d = ? WHERE id = ?";
        executeUpdate(query,
                new PGvector(documentChunkEmbedding.getEmbedding()),
                documentChunkEmbedding.getCoveredText(),
                documentChunkEmbedding.getBegin(),
                documentChunkEmbedding.getEnd(),
                new PGvector(documentChunkEmbedding.getTsne2D()),
                new PGvector(documentChunkEmbedding.getTsne3D()),
                documentChunkEmbedding.getId());
    }

    /**
     * Executes an update on the RAG database
     *
     */
    private void executeUpdate(String query, Object... params) throws SQLException {
        var statement = vectorDbConnection.prepareStatement(query);
        for (int i = 0; i < params.length; i++) {
            if (params[i] instanceof PGvector) {
                statement.setObject(i + 1, params[i]);
            } else if (params[i] instanceof String) {
                statement.setString(i + 1, (String) params[i]);
            } else if (params[i] instanceof Integer) {
                statement.setInt(i + 1, (Integer) params[i]);
            } else if (params[i] instanceof Long) {
                statement.setLong(i + 1, (Long) params[i]);
            }
            // Add other types as needed
        }
        statement.executeUpdate();
    }

    /**
     * Gets a single DocumentEmbedding for a whole document.
     *
     * @param document
     * @return
     */
    public DocumentEmbedding getCompleteEmbeddingFromDocument(Document document) throws IOException, URISyntaxException, InterruptedException {
        var documentEmbedding = new DocumentEmbedding();
        documentEmbedding.setDocument_id(document.getId());
        documentEmbedding.setEmbedding(getEmbeddingForText(document.getFullText()));
        return documentEmbedding;
    }

    /**
     * Gets the complete and embedded lists of DocumentChunkEmbeddings for a single document
     */
    public List<DocumentChunkEmbedding> getCompleteEmbeddingChunksFromDocument(Document document) throws IOException, URISyntaxException, InterruptedException {
        // We also make an embedding from the title
        var emptyEmbeddings = getEmptyEmbeddingChunksFromText(document.getDocumentTitle() + " " + document.getFullText(), 900);
        for (var empty : emptyEmbeddings) {
            var embeddings = getEmbeddingForText(empty.getCoveredText());
            empty.setEmbedding(embeddings);
            empty.setDocument_id(document.getId());
        }
        return emptyEmbeddings;
    }


    /**
     * Gets a single DocumentSentenceEmbedding for a document.
     *
     * @param document
     * @return
     */
    public ArrayList<DocumentSentenceEmbedding> getSentenceEmbeddingFromDocument(Document document) throws IOException, URISyntaxException, InterruptedException {
        var sentenceEmbeddings = new ArrayList<DocumentSentenceEmbedding>();

        List<Sentence> sentences = document.getSentences();
        for (int i = 0; i < sentences.size(); i++) {
            var documentSentenceEmbedding = new DocumentSentenceEmbedding();
            documentSentenceEmbedding.setDocument_id(document.getId());
            var sentenceEmbedding = getEmbeddingForText(sentences.get(i).getCoveredText());
            documentSentenceEmbedding.setEmbedding(sentenceEmbedding);
            documentSentenceEmbedding.setSentence_id(sentences.get(i).getId());
            sentenceEmbeddings.add(documentSentenceEmbedding);
        }
        return sentenceEmbeddings;
    }


    /**
     *  Returns true if the given document by its id has documentsentenceembeddings in the database.
     */
    public boolean documentHasDocumentSentenceEmbeddings(long documentId) throws SQLException {
        String query = "SELECT COUNT(*) FROM documentsentenceembeddings WHERE document_id = ?";
        try (var statement = vectorDbConnection.prepareStatement(query)) {
            statement.setLong(1, documentId);
            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    return count > 0;
                }
            }
        }
        return false;
    }

    /**
     * Saves a document embedding.
     */
    public void saveDocumentSentenceEmbedding(DocumentSentenceEmbedding documentSentenceEmbedding) throws SQLException {
        String query = "INSERT INTO documentsentenceembeddings (document_id,sentence_id, embedding, tsne2d, tsne3d) VALUES (?, ?, ?, ?, ?)";
        executeUpdate(query,
                documentSentenceEmbedding.getDocument_id(),
                documentSentenceEmbedding.getSentence_id(),
                new PGvector(documentSentenceEmbedding.getEmbedding()),
                new PGvector(documentSentenceEmbedding.getTsne2d()),
                new PGvector(documentSentenceEmbedding.getTsne3d()));
    }

    /**
     * Updates a document sentence embedding.
     */
    public void updateDocumentSentenceEmbedding(DocumentSentenceEmbedding documentSentenceEmbedding) throws SQLException {
        String query = "UPDATE documentsentenceembeddings SET embedding = ?, tsne2d = ?, tsne3d = ? WHERE id = ? AND sentence_id = ? AND document_id = ?";
        executeUpdate(query,
                new PGvector(documentSentenceEmbedding.getEmbedding()),

                new PGvector(documentSentenceEmbedding.getTsne2d()),
                new PGvector(documentSentenceEmbedding.getTsne3d()),
                documentSentenceEmbedding.getId(),
            documentSentenceEmbedding.getSentence_id(),
            documentSentenceEmbedding.getDocument_id());

    }

    /**
     * A function that reduces the vector embeddings into 2D and 3D embeddings through tsne on our webserver.
     */
    public EmbeddingReduceDto getEmbeddingDimensionReductions(List<float[]> embeddings) throws IOException, InterruptedException, URISyntaxException {
        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        var url = config.getRAGWebserverBaseUrl() + "embed/reduce";

        // Prepare workload
        var gson = new Gson();
        var params = new HashMap<String, Object>();
        params.put("embeddings", embeddings);
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
        var reductionDto = gson.fromJson(responseBody, EmbeddingReduceDto.class);
        if (reductionDto.getStatus() != 200) throw new HttpStatusException(
                "Webservice replied with an internally wrong status code, something went wrong there: " + reductionDto.getStatus(), statusCode, url);

        return reductionDto;
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
     * Gets a list of empty DocumentChunkEmbeddings with proper text splitting
     *
     * @param chunkSize Good size would be 1.200 for example
     */
    public List<DocumentChunkEmbedding> getEmptyEmbeddingChunksFromText(String text, int chunkSize) {
        // Calculate the number of chunks needed
        // We want a cleaned single text without linebreaks or whatnot.
        text = text.replaceAll("\\s+", " ");
        int numChunks = (int) Math.ceil((double) text.length() / chunkSize);
        var emptyEmbeddings = new ArrayList<DocumentChunkEmbedding>();

        // Split the input text into chunks
        for (int i = 0; i < numChunks; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, text.length());
            var chunk = text.substring(start, end);
            var emptyEmbedding = new DocumentChunkEmbedding(start, end);
            emptyEmbedding.setCoveredText(chunk);
            emptyEmbeddings.add(emptyEmbedding);
        }
        return emptyEmbeddings;
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
