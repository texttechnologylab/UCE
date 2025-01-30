package org.texttechnologylab.routes;

import com.google.gson.Gson;
import freemarker.template.Configuration;
import org.apache.http.annotation.Obsolete;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.CustomFreeMarkerEngine;
import org.texttechnologylab.LanguageResources;
import org.texttechnologylab.config.CommonConfig;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.models.rag.DocumentChunkEmbedding;
import org.texttechnologylab.models.rag.RAGChatMessage;
import org.texttechnologylab.models.rag.RAGChatState;
import org.texttechnologylab.models.rag.Roles;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.services.RAGService;
import spark.ModelAndView;
import spark.Route;

import java.util.*;

public class RAGApi {
    private static final Logger logger = LogManager.getLogger();
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

    @Obsolete
    /**
     * Returns a fully rendered Tsne plot for the given corpus.
     * Update: This was a short-handed implementation because we needed something fast. It is obsolete as off now.
     */
    public Route getTsnePlot = ((request, response) -> {

        var corpusId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(request.queryParams("corpusId")),
                (ex) -> logger.error("Error: the url for the tsne plot requires a 'corpusId' query parameter. ", ex));
        if(corpusId == null) return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));

        try {
            var plotAsHtml = db.getCorpusTsnePlotByCorpusId(corpusId).getPlotHtml();
            //var plotAsHtml = ragService.getCorpusTsnePlot(corpusId);
            return plotAsHtml == null ? "" : plotAsHtml;
        } catch (Exception ex) {
            logger.error("Error fetching the tsne plot of corpus: " + corpusId, ex);
            return "";
        }
    });

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
            if (!activeRagChatStates.containsKey(stateId)){
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
            if(contextNeeded == null) contextNeeded = 1;

            List<DocumentChunkEmbedding> nearestDocumentChunkEmbeddings = new ArrayList<>();
            List<Document> foundDocuments = new ArrayList<Document>();
            if (contextNeeded == 1) {
                nearestDocumentChunkEmbeddings = ragService.getClosestDocumentChunkEmbeddings(userMessage, 3, -1);
                // foreach fetched document embedding, we also fetch the actual documents so the chat can show them
                foundDocuments = db.getManyDocumentsByIds(nearestDocumentChunkEmbeddings.stream().map(d -> Math.toIntExact(d.getDocument_id())).toList());
                prompt = prompt.replace("[NO CONTEXT - USE CONTEXT FROM PREVIOUS QUESTION IF EXIST]",
                        String.join("\n", nearestDocumentChunkEmbeddings.stream().map(UIMAAnnotation::getCoveredText).toList()));
            }
            userRagMessage.setPrompt(prompt);

            // Add the message to the current chat
            chatState.addMessage(userRagMessage);

            // Now let's ask our rag llm
            String finalPrompt = prompt;
            var answer = ExceptionUtils.tryCatchLog(
                    () -> ragService.postNewRAGPrompt(chatState.getMessages()),
                    (ex) -> logger.error("Error getting the next response from our LLM RAG service. The prompt: " + finalPrompt, ex));
            if(answer == null) {
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

    /**
     * Returns an empty chat alongside its chatHistory view
     */
    public Route getNewRAGChat = ((request, response) -> {
        var model = new HashMap<String, Object>();
        try {
            // We need to know the language here
            var languageResources = LanguageResources.fromRequest(request);

            var ragState = new RAGChatState();
            ragState.setModel(commonConfig.getRAGModel());
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

}
