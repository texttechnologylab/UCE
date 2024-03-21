package org.texttechnologylab.routes;

import com.google.gson.Gson;
import freemarker.template.Configuration;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.config.CommonConfig;
import org.texttechnologylab.models.rag.RAGChatMessage;
import org.texttechnologylab.models.rag.RAGChatState;
import org.texttechnologylab.models.rag.Roles;
import org.texttechnologylab.services.DatabaseService;
import org.texttechnologylab.services.RAGService;
import org.texttechnologylab.utils.SupportedLanguages;
import spark.ModelAndView;
import spark.Route;
import spark.template.freemarker.FreeMarkerEngine;

import javax.management.openmbean.InvalidKeyException;
import java.util.*;

public class RAGApi {

    private Configuration freemakerConfig = Configuration.getDefaultConfiguration();
    private RAGService ragService;
    private DatabaseService db;
    private CommonConfig commonConfig = new CommonConfig();
    private Map<UUID, RAGChatState> activeRagChatStates = new HashMap<>();

    public RAGApi(ApplicationContext serviceContext,
                  Configuration freemakerConfig){
        this.freemakerConfig = freemakerConfig;
        this.db = serviceContext.getBean(DatabaseService.class);
        this.ragService = serviceContext.getBean(RAGService.class);
    }

    /**
     * Receives a user message and handles and returns a new RAGChatState
     */
    public Route postUserMessage = ((request, response) -> {
        var model = new HashMap<String, Object>();
        try {
            var gson = new Gson();
            Map<String, Object> requestBody = gson.fromJson(request.body(), Map.class);
            var userMessage = requestBody.get("userMessage").toString();
            var stateId = UUID.fromString(requestBody.get("stateId").toString());

            if(!activeRagChatStates.containsKey(stateId)) throw new InvalidKeyException("Chat state couldn't be found of key " + stateId.toString());
            // Get the cached state.
            var chatState = activeRagChatStates.get(stateId);

            // First, add the user message to the chat history
            var userRagMessage = new RAGChatMessage();
            userRagMessage.setRole(Roles.USER);
            userRagMessage.setMessage(userMessage);

            // Now fetch some context through embeddings
            var nearestDocumentEmbeddings = ragService.getClosestDocumentEmbeddings(userMessage, 1);
            var prompt = "Context: " + String.join("\n", nearestDocumentEmbeddings.stream().map(e -> e.getCoveredText()).toList())
                    + "\nInstruktion:" + userMessage;
            userRagMessage.setPrompt(prompt);

            // Add the message to the current chat
            chatState.addMessage(userRagMessage);

            // Now let's ask our rag llm
            var answer = ragService.postNewRAGPrompt(chatState.getMessages());
            var systemResponseMessage = new RAGChatMessage();
            systemResponseMessage.setRole(Roles.SYSTEM);
            systemResponseMessage.setPrompt(answer);
            systemResponseMessage.setMessage(answer);
            systemResponseMessage.setContextDocument_Ids(nearestDocumentEmbeddings.stream().map(e -> e.getDocument_id()).toList());

            // Add the system response as well
            chatState.addMessage(systemResponseMessage);

            model.put("chatState", chatState);

        } catch (Exception ex) {
            // TODO: Logging
            model.put("data", "");
        }

        return new FreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "ragbot/chatHistory.ftl"));
    });

    /**
     * Returns an empty chat alongside its chatHistory view
     */
    public Route getNewRAGChat = ((request, response) -> {
        var model = new HashMap<String, Object>();
        try {
            var ragState = new RAGChatState();
            ragState.setModel(commonConfig.getRAGModel());
            ragState.setChatId(UUID.randomUUID());
            // TODO: Add support for more languages
            ragState.setLanguage(SupportedLanguages.GERMAN);

            var startMessage = new RAGChatMessage();
            startMessage.setRole(Roles.SYSTEM);
            // TODO: Add support for more languages
            startMessage.setMessage("Hallo! Ich bin dein virtueller Assistent rund um das Suchportal. " +
                    "Wenn du eine Frage zu einem bestimmten Thema hast, oder du über die Suche nicht die " +
                    "Dokumente findest, die du dir erhofft hattest, dann frag mich gerne.");
            startMessage.setPrompt("Du bist ein Bibliothekar in einer Online-Suchplattform für Bücher und Texte aller Art. " +
                    "Menschen instruieren dich und suchen verschiedenste Dinge über diese Bücher und Artikel, und du hilfst ihnen. " +
                    "Es werden dir Kontexte gegeben, die Auszüge aus Büchern und Artikel der Plattform darstellen. " +
                    "Nutze diese Kontexte um zu antworten, aber erwähne nicht, dass du sie hast - der Mensch sieht sie nicht!" +
                    "Wenn du die Antwort nicht weisst, dann sag das einfach. Halte die Antwort kurz und sei höflich!");
            ragState.addMessage(startMessage);

            // TODO: Someday it's probably best to cache this in a mongodb or something.
            activeRagChatStates.put(ragState.getChatId(), ragState);
            model.put("chatState", ragState);
        } catch (Exception ex) {
            // TODO: Logging
            model.put("data", "");
        }

        return new FreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "ragbot/chatHistory.ftl"));
    });

}
