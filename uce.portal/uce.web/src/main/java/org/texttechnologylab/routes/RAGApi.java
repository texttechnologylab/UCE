package org.texttechnologylab.routes;

import com.google.gson.Gson;
import freemarker.template.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.CustomFreeMarkerEngine;
import org.texttechnologylab.LanguageResources;
import org.texttechnologylab.annotations.auth.Authentication;
import org.texttechnologylab.config.CommonConfig;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.models.rag.*;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.services.RAGService;
import org.texttechnologylab.utils.SystemStatus;
import spark.ModelAndView;
import spark.Route;

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
    public Route getTsnePlot = ((request, response) -> {

        var corpusId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(request.queryParams("corpusId")),
                (ex) -> logger.error("Error: the url for the tsne plot requires a 'corpusId' query parameter. ", ex));
        if (corpusId == null)
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));

        try {
            var plotAsHtml = db.getCorpusTsnePlotByCorpusId(corpusId).getPlotHtml();
            return plotAsHtml == null ? "" : plotAsHtml;
        } catch (Exception ex) {
            logger.error("Error fetching the tsne plot of corpus: " + corpusId, ex);
            return "";
        }
    });

    static final Pattern patternIDGiven = Pattern.compile("ID=(\\d+)");

    @Authentication(required = Authentication.Requirement.LOGGED_IN,
            route = Authentication.RouteTypes.POST,
            path = "/api/rag/postUserMessage"
    )
    /**
     * Receives a user message and handles and returns a new RAGChatState
     */
    public Route postUserMessage = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var gson = new Gson();
        Map requestBody = gson.fromJson(request.body(), Map.class);

        try {
            var userMessage = requestBody.get("userMessage").toString();
            var stateId = UUID.fromString(requestBody.get("stateId").toString());

            // TODO: This also needs some form of periodic cleanup. I could have used websockets, but at the time,
            // noone really knew if this feature is even needed or applicable. Websocket introduced more complexity to client
            // and server so we scraped it. For the future, it may be a good idea though.
            if (!activeRagChatStates.containsKey(stateId)) {
                logger.error("Error fetching the active rag chat states - state not found for stateId: " + stateId);
                return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
            }
            // Get the cached state.
            var chatState = activeRagChatStates.get(stateId);

            // First, add the user message to the chat history
            var userRagMessage = new RAGChatMessage();
            userRagMessage.setRole(Roles.USER);
            userRagMessage.setMessage(userMessage);

            var prompt = "### Context: [NO CONTEXT - USE CONTEXT FROM PREVIOUS QUESTION IF EXIST] \n### Instruktion: " + userMessage;
            // Now fetch some context through embeddings.
            // When do we actually fetch more context? Good paper here: https://arxiv.org/html/2401.06800v1
            // Update: 16.04.2024: I've trained a BERT model that classifies user inputs into context_needed or
            // context_not_needed. So: we ask our webserver: should we fetch context? if yes, do so, if not - then don't.
            // See also: https://www.kaggle.com/models/kevinbnisch/ccc-bert
            var contextNeeded = ExceptionUtils.tryCatchLog(
                    () -> ragService.postRAGContextNeeded(userMessage),
                    (ex) -> logger.error("Error getting the ContextNeeded info from the rag service.", ex));
            if (contextNeeded == null) contextNeeded = 1;

            // Check if the user wants to work with just one document or with multiple documents
            Long documentId = null;
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
            }
            catch (Exception ex) {
                logger.error("Error parsing the user message for a document ID.", ex);
            }

            // Check for the amount of documents the user wants to work with
            String documentTitle = null;
            // Only if there is no document id given
            if (documentId == null) {
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
            if (documentId == null) {
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
            if (contextNeeded == 1) {
                if (documentId != null) {
                    // use a specific document only
                    Document doc = db.getDocumentById(documentId);
                    StringBuilder contextText = new StringBuilder();
                    contextText.append("Provide your answer based on the contents of the following document.\n\n");
                    contextText.append("ID: ").append(doc.getId()).append("\n");
                    contextText.append("Title: ").append(doc.getDocumentTitle()).append("\n");
                    contextText.append("Language: ").append(doc.getLanguage()).append("\n");
                    contextText.append("Content:\n").append(doc.getFullText());
                    contextText.append("\n\n");
                    prompt = prompt.replace("[NO CONTEXT - USE CONTEXT FROM PREVIOUS QUESTION IF EXIST]", contextText);
                }
                else {
                    nearestDocumentChunkEmbeddings = ragService.getClosestDocumentChunkEmbeddings(userMessage, amountOfDocs, -1);
                    // foreach fetched document embedding, we also fetch the actual documents so the chat can show them
                    foundDocuments = db.getManyDocumentsByIds(nearestDocumentChunkEmbeddings.stream().map(d -> Math.toIntExact(d.getDocument_id())).toList());
                    StringBuilder contextText = new StringBuilder();
                    contextText.append("The following documents contain information, ordered by relevance.\n\n");
                    int docInd = 0;
                    for (var nearestDocumentChunkEmbedding : nearestDocumentChunkEmbeddings) {
                        if (docInd >= foundDocuments.size()) break; // TODO this should not happen?!
                        Document doc = foundDocuments.get(docInd);
                        docInd++;
                        contextText.append("Document #").append(docInd).append("\n");
                        contextText.append("ID: ").append(doc.getId()).append("\n");
                        contextText.append("Title: ").append(doc.getDocumentTitle()).append("\n");
                        contextText.append("Language: ").append(doc.getLanguage()).append("\n");
                        contextText.append("Search result:\n").append(nearestDocumentChunkEmbedding.getCoveredText());
                        contextText.append("\n\n");
                    }
                    prompt = prompt.replace("[NO CONTEXT - USE CONTEXT FROM PREVIOUS QUESTION IF EXIST]", contextText);
                }
            }
            userRagMessage.setPrompt(prompt);

            // Add the message to the current chat
            chatState.addMessage(userRagMessage);

            // Now let's ask our rag llm
            String finalPrompt = prompt;
            var answer = ExceptionUtils.tryCatchLog(
                    () -> ragService.postNewRAGPrompt(chatState.getMessages(), chatState.getModel()),
                    (ex) -> logger.error("Error getting the next response from our LLM RAG service. The prompt: " + finalPrompt, ex));
            if (answer == null) {
                var languageResources = LanguageResources.fromRequest(request);
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
        } catch (Exception ex) {
            logger.error("Unknown Error getting the response of the ragbot; request body:\n " + request.body(), ex);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }

        return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "ragbot/chatHistory.ftl"));
    });

    @Authentication(required = Authentication.Requirement.LOGGED_IN,
            route = Authentication.RouteTypes.GET,
            path = "/api/rag/new"
    )
    /**
     * Returns an empty chat alongside its chatHistory view
     */
    public Route getNewRAGChat = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var ragModelId = ExceptionUtils.tryCatchLog(() -> request.queryParams("model"),
                (ex) -> logger.error("Error: the chatting requires a 'model' query parameter. ", ex));
        if (ragModelId == null)
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        var ragModel = SystemStatus.UceConfig.getSettings().getRag().getModels().stream().filter(m -> m.getModel().equals(ragModelId)).findFirst();
        if (ragModel.isEmpty()) return "The requested model isn't available in this UCE instance: " + ragModelId;

        try {
            // We need to know the language here
            var languageResources = LanguageResources.fromRequest(request);

            var ragState = new RAGChatState();
            ragState.setModel(ragModel.get());
            ragState.setChatId(UUID.randomUUID());
            ragState.setLanguage(languageResources.getSupportedLanguage());

            var startMessage = new RAGChatMessage();
            startMessage.setRole(Roles.SYSTEM);
            startMessage.setMessage(languageResources.get("ragBotGreetingMessage"));
            startMessage.setPrompt(languageResources.get("ragBotGreetingPrompt"));

            ragState.addMessage(startMessage);

            // TODO: Someday it's probably best to cache this in a mongodb or something. -> Yes, it is.
            activeRagChatStates.put(ragState.getChatId(), ragState);
            model.put("chatState", ragState);
        } catch (Exception ex) {
            logger.error("Error creating a new RAGbot chat", ex);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }

        return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "ragbot/chatHistory.ftl"));
    });

    public Route getSentenceEmbeddings = ((request, response) -> {
        var documentId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(request.queryParams("documentId")),
                (ex) -> logger.error("Error: couldn't determine the documentId for sentence topics with entities. ", ex));

        if (documentId == null) {
            response.status(400);
            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(Map.of("information", "Missing documentId parameter for fetching sentence embeddings"), "defaultError.ftl"));
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

            response.type("application/json");
            return new Gson().toJson(simplified);
        } catch (Exception ex) {
            logger.error("Error getting sentence embeddings.", ex);
            response.status(500);
            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(Map.of("information", "Error retrieving sentence embeddings."), "defaultError.ftl"));
        }
    });

}
