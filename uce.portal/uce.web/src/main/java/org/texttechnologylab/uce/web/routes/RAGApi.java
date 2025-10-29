package org.texttechnologylab.uce.web.routes;

import com.google.gson.Gson;
import freemarker.template.Configuration;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.uce.common.annotations.auth.Authentication;
import org.texttechnologylab.uce.common.backgroundtasks.RAGStreamBackgroundTask;
import org.texttechnologylab.uce.common.config.CommonConfig;
import org.texttechnologylab.uce.common.exceptions.ExceptionUtils;
import org.texttechnologylab.uce.common.models.corpus.Document;
import org.texttechnologylab.uce.common.models.corpus.Image;
import org.texttechnologylab.uce.common.models.rag.*;
import org.texttechnologylab.uce.common.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.uce.common.services.RAGService;
import org.texttechnologylab.uce.common.utils.SystemStatus;
import org.texttechnologylab.uce.web.CustomFreeMarkerEngine;
import org.texttechnologylab.uce.web.LanguageResources;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RAGApi implements UceApi {
    private static final Logger logger = LogManager.getLogger(RAGApi.class);
    private Configuration freemarkerConfig;
    private RAGService ragService;
    private PostgresqlDataInterface_Impl db;
    private final CommonConfig commonConfig = new CommonConfig();
    private final Map<UUID, RAGChatState> activeRagChatStates = new HashMap<>();

    public RAGApi(ApplicationContext serviceContext,
                  Configuration freemarkerConfig) {
        this.freemarkerConfig = freemarkerConfig;
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
        this.ragService = serviceContext.getBean(RAGService.class);
    }

    /**
     * Returns a fully rendered Tsne plot for the given corpus.
     * Update: This was a short-handed implementation because we needed something fast. It is obsolete as off now.
     */
    public void getTsnePlot(Context ctx) {

        var corpusId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(ctx.queryParam("corpusId")),
                (ex) -> logger.error("Error: the url for the tsne plot requires a 'corpusId' query parameter. ", ex));
        if (corpusId == null) {
            ctx.render("defaultError.ftl");
            return;
        }

        try {
            var plotAsHtml = db.getCorpusTsnePlotByCorpusId(corpusId).getPlotHtml();
            ctx.result(plotAsHtml == null ? "" : plotAsHtml);
        } catch (Exception ex) {
            logger.error("Error fetching the tsne plot of corpus: " + corpusId, ex);
            ctx.result("");
        }
    }

    /**
     * Get the list of messages for a given chat.
     * This can be used fo get messages updates for streaming results.
     */
    public void getMessagesForChat(Context ctx) {
        ctx.contentType("application/json");

        var chatId = ExceptionUtils.tryCatchLog(() -> ctx.queryParam("chatId"),
                (ex) -> logger.error("Error: the 'chatId' query parameter is required to get the message list. ", ex));
        if (chatId == null || chatId.isEmpty()) {
            ctx.status(400);

            Map<String, Object> result = new HashMap<>();
            result.put("message", "The 'chatId' query parameter is required to get the messages.");
            ctx.json(result);
            return;
        }

        var stateId = UUID.fromString(chatId);
        logger.info("Fetching chat messages for chat with id: " + stateId);

        if (!activeRagChatStates.containsKey(stateId)) {
            logger.error("Error fetching the active rag chat states - state not found for stateId: " + stateId);

            ctx.status(400);

            Map<String, Object> result = new HashMap<>();
            result.put("message", "Not active chat with id 'chatId' found.");
            ctx.json(result);
            return;
        }
        var chatState = activeRagChatStates.get(stateId);

        // we always return JSON result, but depending on the Accept header we return the rendered HTML or just the data
        ctx.contentType("application/json");

        // send full chat state as JSON only
        var contentType = ctx.header("Accept");
        if (contentType != null && contentType.equals("application/json")) {
            RAGChatStateDTO returnState = RAGChatStateDTO.fromRAGChatState(chatState);
            ctx.json(returnState);
            return;
        }

        // sende render for HTML output
        var model = new HashMap<String, Object>();
        model.put("chatState", chatState);
        var renderedHtml = new CustomFreeMarkerEngine(this.freemarkerConfig).render("ragbot/chatHistory.ftl", model, ctx);
        Map<String, Object> result = new HashMap<>();
        result.put("html", renderedHtml);
        result.put("done", chatState.getNewestMessage().isDone());
        ctx.json(result);
    }

    static final Pattern patternIDGiven = Pattern.compile("ID=(\\d+)");

    @Authentication(required = Authentication.Requirement.LOGGED_IN,
            route = Authentication.RouteTypes.POST,
            path = "/api/rag/postUserMessage"
    )
    /**
     * Receives a user message and handles and returns a new RAGChatState
     */
    public void postUserMessage(Context ctx) {
        var model = new HashMap<String, Object>();
        var gson = new Gson();
        Map requestBody = gson.fromJson(ctx.body(), Map.class);

        try {
            var userMessage = requestBody.get("userMessage").toString();
            var stateId = UUID.fromString(requestBody.get("stateId").toString());

            // Stream the result?
            // NOTE this will start a new thread in the background and return the chat id immediately
            var stream = requestBody.getOrDefault("stream", null) != null ? ((Boolean)requestBody.get("stream")).booleanValue() : false;

            // A specific document id can be provided to only work with this document as LLM context
            // TODO check datatype again here
            var documentId = requestBody.getOrDefault("documentId", null) != null ? ((Double)requestBody.get("documentId")).longValue() : null;

            // TODO: This also needs some form of periodic cleanup. I could have used websockets, but at the time,
            // noone really knew if this feature is even needed or applicable. Websocket introduced more complexity to client
            // and server so we scraped it. For the future, it may be a good idea though.
            if (!activeRagChatStates.containsKey(stateId)) {
                logger.error("Error fetching the active rag chat states - state not found for stateId: " + stateId);
                ctx.render("defaultError.ftl");
                return;
            }
            // Get the cached state.
            var chatState = activeRagChatStates.get(stateId);

            // First, add the user message to the chat history
            var userRagMessage = new RAGChatMessage();
            userRagMessage.setRole(Roles.USER);
            userRagMessage.setMessage(userMessage);

            // if we are using MCP we can skip all the extras
            var usingMcp = SystemStatus.UceConfig.getSettings().getMcp().isEnabled();

            var prompt_replace_text = "";
            if (usingMcp) {
                prompt_replace_text = "[NO CONTEXT - CALL TOOLS TO GET CONTEXT OR PERFORM ACTIONS]";
            }
            else {
                prompt_replace_text = "[NO CONTEXT - USE CONTEXT FROM PREVIOUS QUESTION IF EXIST]";
            }
            var prompt = "### Context: " + prompt_replace_text + " \n### Instruktion: " + userMessage;

            // Now fetch some context through embeddings.
            // When do we actually fetch more context? Good paper here: https://arxiv.org/html/2401.06800v1
            // Update: 16.04.2024: I've trained a BERT model that classifies user inputs into context_needed or
            // context_not_needed. So: we ask our webserver: should we fetch context? if yes, do so, if not - then don't.
            // See also: https://www.kaggle.com/models/kevinbnisch/ccc-bert
            // NOTE if a specific document id is given we always add its content
            Integer contextNeeded;
            // TODO allow this to be set via parameter: should the "needed" model run if there is a documentId given?
//            if (documentId != null) {
//                contextNeeded = 1;
//            }
            if (usingMcp) {
                // no context needed, the model can get it by using tools
                contextNeeded = 0;
            }
            else {
                contextNeeded = ExceptionUtils.tryCatchLog(
                        () -> ragService.postRAGContextNeeded(userMessage),
                        (ex) -> logger.error("Error getting the ContextNeeded info from the rag service.", ex));
                if (contextNeeded == null) contextNeeded = 1;
            }

            // Check if the user wants to work with just one document or with multiple documents,
            // unless a specific id is already given in the request
            if (documentId == null && !usingMcp) {
                try {
                    if (userMessage.contains("ID")) {
                        Matcher matcher = patternIDGiven.matcher(userMessage);
                        if (matcher.find()) {
                            documentId = Long.parseLong(matcher.group(1));
                            System.out.println("Found ID: " + documentId);
                        } else {
                            System.out.println("No ID found.");
                        }
                    }
                } catch (Exception ex) {
                    logger.error("Error parsing the user message for a document ID.", ex);
                }
            }

            // Check for the amount of documents the user wants to work with
            String documentTitle = null;
            // Only if there is no document id given
            if (documentId == null && !usingMcp) {
                List<String> parts = List.of("URL", "title", "ID");
                for (String part : parts) {
                    if (documentId != null) {
                        break;
                    }
                    documentTitle = ExceptionUtils.tryCatchLog(
                            () -> ragService.postRAGDocTitle(userMessage, part, chatState.getModel()),
                            (ex) -> logger.error("Error getting the postRAGDocTitle info from the rag service.", ex));
                    if (documentTitle != null && documentTitle.equalsIgnoreCase("null")) {
                        documentTitle = null;
                    }
                    if (documentTitle != null && documentTitle.contains(" ")) {
                        documentTitle = null;
                    }
                    if (documentTitle != null && !documentTitle.isEmpty()) {
                        try {
                            Integer documentIdInt = Integer.parseInt(documentTitle);
                            Document doc = db.getDocumentById(documentIdInt);
                            if (doc != null) {
                                documentId = doc.getId();
                                System.out.println("Found document ID from ID: " + documentTitle);
                            } else {
                                System.out.println("No document found with title: " + documentTitle);
                            }
                        }
                        catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        if (documentId == null) {
                            try {
                                Document doc = db.getFirstDocumentByTitle(documentTitle, true);
                                if (doc != null) {
                                    documentId = doc.getId();
                                    System.out.println("Found document ID from title: " + documentTitle);
                                } else {
                                    System.out.println("No document found with title: " + documentTitle);
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }

            // Check for the amount of documents the user wants to work with
            Integer amountOfDocs = null;
            // Only if there is no document id given
            if (documentId == null && !usingMcp) {
                amountOfDocs = ExceptionUtils.tryCatchLog(
                        () -> ragService.postRAGAmountDocs(userMessage, chatState.getModel()),
                        (ex) -> logger.error("Error getting the AmountDocs info from the rag service.", ex));
                if (amountOfDocs == null) amountOfDocs = 3;
                // only max 10 docs...
                int docsLimit = 10;
                if (amountOfDocs > docsLimit) {
                    amountOfDocs = docsLimit;
                    logger.warn("The user requested more than " + docsLimit + " documents, limiting.");
                }
                logger.info("Using " + amountOfDocs + " documents.");
            }

            List<DocumentChunkEmbedding> nearestDocumentChunkEmbeddings = new ArrayList<>();
            List<Document> foundDocuments = new ArrayList<Document>();
            List<Image> foundImages = new ArrayList<>();
            // TODO max images should be a parameter
            int maxImages = 5;
            // if we need context or there is a document id given we include it in the context
            if (contextNeeded == 1 || documentId != null) {
                Set<String> hibernateInit = Set.of("image");
                if (documentId != null) {
                    // use a specific document only
                    // TODO cache documents in state
                    Document doc = db.getDocumentById(documentId, hibernateInit);

                    List<Image> docImages = doc.getImages();
                    foundImages.addAll(docImages);
                    // remove images if more than maxImages
                    if (foundImages.size() > maxImages) {
                        foundImages = foundImages.subList(0, maxImages);
                    }

                    StringBuilder contextText = new StringBuilder();
                    if (!docImages.isEmpty()) {
                        // TODO this should be further finetuned...
                        contextText.append("Provide your answer based on the given image").append(docImages.size()>1?"s":"").append(".\n\n");
                    }
                    else {
                        contextText.append("Provide your answer based on the contents of the following document :\n\n");
                        contextText.append("<document>").append("\n");
                        contextText.append("ID: ").append(doc.getId()).append("\n");
                        contextText.append("Title: ").append(doc.getDocumentTitle()).append("\n");
                        contextText.append("Language: ").append(doc.getLanguage()).append("\n");
                        contextText.append("Images: ").append(docImages.size()).append(" images provided.").append("\n");
                        contextText.append("Content:\n").append(doc.getFullText()).append("\n");
                        contextText.append("</document>").append("\n\n");
                    }
                    prompt = prompt.replace(prompt_replace_text, contextText);
                }
                else {
                    nearestDocumentChunkEmbeddings = ragService.getClosestDocumentChunkEmbeddings(userMessage, amountOfDocs, -1);
                    // foreach fetched document embedding, we also fetch the actual documents so the chat can show them
                    foundDocuments = db.getManyDocumentsByIds(nearestDocumentChunkEmbeddings.stream().map(d -> Math.toIntExact(d.getDocument_id())).toList(), hibernateInit);
                    StringBuilder contextText = new StringBuilder();
                    contextText.append("The following documents contain information, ordered by relevance.\n\n");
                    int docInd = 0;
                    for (var nearestDocumentChunkEmbedding : nearestDocumentChunkEmbeddings) {
                        if (docInd >= foundDocuments.size()) break; // TODO this should not happen?!
                        Document doc = foundDocuments.get(docInd);
                        docInd++;

                        List<Image> docImages = doc.getImages();
                        foundImages.addAll(docImages);
                        // remove images if more than maxImages
                        if (foundImages.size() > maxImages) {
                            foundImages = foundImages.subList(0, maxImages);
                        }

                        contextText.append("<document>").append("\n");
                        contextText.append("Document #").append(docInd).append("\n");
                        contextText.append("ID: ").append(doc.getId()).append("\n");
                        contextText.append("Title: ").append(doc.getDocumentTitle()).append("\n");
                        contextText.append("Language: ").append(doc.getLanguage()).append("\n");
                        contextText.append("Images: ").append(docImages.size()).append(" images provided.").append("\n");
                        contextText.append("Search result:\n").append(nearestDocumentChunkEmbedding.getCoveredText()).append("\n");
                        contextText.append("</document>").append("\n\n");
                    }
                    prompt = prompt.replace(prompt_replace_text, contextText);
                }
            }
            userRagMessage.setPrompt(prompt);
            userRagMessage.setImages(foundImages);

            // Add the message to the current chat
            chatState.addMessage(userRagMessage);

            // At this point, we distinguish between "streaming" and "non-streaming" responses of the LLM.
            // In the latter case, we query the LLM and wait for the response, this is the same as before.
            // If we want to stream the results, we will spawn a new thread that will handle the returns,
            // save them in the chat state as "partial" messages and, when all data is received, will
            // update the state to contain the complete message.
            // The user can poll for new parts of the message anytime using a different endpoint.
            // TODO we need to make sure, that we cannot accept another message from the user while the streaming is in progress.
            if (stream) {
                // Start a background thread that will handle the streaming response
                Runnable backgroundTask = new RAGStreamBackgroundTask(ragService, chatState, nearestDocumentChunkEmbeddings, foundDocuments);
                // TODO store active background threads to be able to cancel them if needed
                var backgroundThread = new Thread(backgroundTask);
                backgroundThread.start();

                // Streaming immediately returns the chat id that the caller can use to poll for new messages.
                Map<String, String> returnMap = new HashMap<>();
                returnMap.put("chat_id", chatState.getChatId().toString());
                ctx.json(returnMap);
                return;
            }

            // Now let's ask our rag llm
            String finalPrompt = prompt;
            var answer = ExceptionUtils.tryCatchLog(
                    () -> ragService.postNewRAGPrompt(chatState),
                    (ex) -> logger.error("Error getting the next response from our LLM RAG service. The prompt: " + finalPrompt, ex));
            if (answer == null) {
                var languageResources = LanguageResources.fromRequest(ctx);
                answer = languageResources.get("ragBotErrorMessage");
            }
            var systemResponseMessage = new RAGChatMessage();
            systemResponseMessage.setRole(Roles.ASSISTANT);
            systemResponseMessage.setPrompt(answer);
            systemResponseMessage.setMessage(answer);
            systemResponseMessage.setContextDocument_Ids(nearestDocumentChunkEmbeddings.stream().map(DocumentChunkEmbedding::getDocument_id).toList());
            systemResponseMessage.setContextDocuments(new ArrayList<>(foundDocuments));

            // Add the system response as well
            chatState.addMessage(systemResponseMessage);

            model.put("chatState", chatState);

            // Dont return the template if this is an API request
            var contentType = ctx.header("Accept");
            if (contentType != null && contentType.equals("application/json")) {
                RAGChatStateDTO returnState = RAGChatStateDTO.fromRAGChatState(chatState);
                ctx.json(returnState);
                return;
            }

        } catch (Exception ex) {
            logger.error("Unknown Error getting the response of the ragbot; request body:\n " + ctx.body(), ex);
            ctx.render("defaultError.ftl");
            return;
        }

        ctx.render("ragbot/chatHistory.ftl", model);
    }

    @Authentication(required = Authentication.Requirement.LOGGED_IN,
            route = Authentication.RouteTypes.GET,
            path = "/api/rag/new"
    )
    /**
     * Returns an empty chat alongside its chatHistory view
     */
    public void getNewRAGChat(Context ctx) {
        var model = new HashMap<String, Object>();
        String ragModelId;
        String systemPrompt = null;
        String systemMessage = null;

        // this endpoint is provided as a GET and POST request to handle larger prompts
        // TODO switch to POST only?
        if (ctx.method() == HandlerType.POST) {
            JSONObject requestBody = new JSONObject(ctx.body());

            ragModelId = ExceptionUtils.tryCatchLog(() -> requestBody.getString("model"),
                    (ex) -> logger.error("Error: the chatting requires a 'model' query parameter. ", ex));

            // TODO should we offer this as a parameter? is in use for the TA bot at the moment, but we should discuss it for the future versions
            if (requestBody.has("systemPrompt")) {
                systemPrompt = requestBody.getString("systemPrompt");
            }
            if (requestBody.has("systemMessage")) {
                systemMessage = requestBody.getString("systemMessage");
            }
        }
        else {
            ragModelId = ExceptionUtils.tryCatchLog(() -> ctx.queryParam("model"),
                    (ex) -> logger.error("Error: the chatting requires a 'model' query parameter. ", ex));

            // TODO should we offer this as a parameter? is in use for the TA bot at the moment, but we should discuss it for the future versions
            systemPrompt = ctx.queryParam("systemPrompt");
            systemMessage = ctx.queryParam("systemMessage");
        }

        if (ragModelId == null) {
            ctx.render("defaultError.ftl");
            return;
        }
        var ragModel = SystemStatus.UceConfig.getSettings().getRag().getModels().stream().filter(m -> m.getModel().equals(ragModelId)).findFirst();
        if (ragModel.isEmpty()) {
            ctx.result("The requested model isn't available in this UCE instance: " + ragModelId);
            return;
        }

        try {
            // We need to know the language here
            var languageResources = LanguageResources.fromRequest(ctx);

            var ragState = new RAGChatState();
            ragState.setModel(ragModel.get());
            ragState.setChatId(UUID.randomUUID());
            ragState.setLanguage(languageResources.getSupportedLanguage());

            var startMessage = new RAGChatMessage();
            startMessage.setRole(Roles.SYSTEM);
            // NOTE to prevent inconsistencies, both must be set
            if (systemPrompt != null && systemMessage != null) {
                startMessage.setPrompt(systemPrompt);
                startMessage.setMessage(systemMessage);
            } else {
                startMessage.setPrompt(languageResources.get("ragBotGreetingPrompt"));
                startMessage.setMessage(languageResources.get("ragBotGreetingMessage"));
            }

            ragState.addMessage(startMessage);

            // TODO: Someday it's probably best to cache this in a mongodb or something. -> Yes, it is.
            activeRagChatStates.put(ragState.getChatId(), ragState);
            model.put("chatState", ragState);

            // Dont return the template if this is an API request
            var contentType = ctx.header("Accept");
            if (contentType != null && contentType.equals("application/json")) {
                RAGChatStateDTO returnState = RAGChatStateDTO.fromRAGChatState(ragState);
                ctx.json(returnState);
                return;
            }

        } catch (Exception ex) {
            logger.error("Error creating a new RAGbot chat", ex);
            ctx.render("defaultError.ftl");
            return;
        }

        ctx.render("ragbot/chatHistory.ftl", model);
    }

    public void getSentenceEmbeddings(Context ctx) {
        var documentId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(ctx.queryParam("documentId")),
                (ex) -> logger.error("Error: couldn't determine the documentId for sentence topics with entities. ", ex));

        if (documentId == null) {
            ctx.status(400);
            ctx.render("defaultError.ftl", Map.of("information", "Missing documentId parameter for fetching sentence embeddings"));
            return;
        }

        try {

            ArrayList<DocumentSentenceEmbedding> result =
                    ragService.getDocumentSentenceEmbeddingsOfDocument(documentId);

            List<Map<String, Object>> simplified = result.stream()
                    .map(e -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("sentenceId", e.getSentence_id());
                        map.put("tsne2d", e.getTsne2d());
                        return map;
                    })
                    .toList();

            ctx.json(simplified);
        } catch (Exception ex) {
            logger.error("Error getting sentence embeddings.", ex);
            ctx.status(500);
            ctx.render("defaultError.ftl", Map.of("information", "Error retrieving sentence embeddings."));
        }
    }

}
