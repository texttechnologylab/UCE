package org.texttechnologylab.uce.common.backgroundtasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.texttechnologylab.uce.common.exceptions.ExceptionUtils;
import org.texttechnologylab.uce.common.models.corpus.Document;
import org.texttechnologylab.uce.common.models.rag.DocumentChunkEmbedding;
import org.texttechnologylab.uce.common.models.rag.RAGChatState;
import org.texttechnologylab.uce.common.services.RAGService;

import java.util.List;

public class RAGStreamBackgroundTask implements Runnable {

    private static final Logger logger = LogManager.getLogger(RAGStreamBackgroundTask.class);

    private final RAGService ragService;
    private final RAGChatState chatState;
    private final List<DocumentChunkEmbedding> nearestDocumentChunkEmbeddings;
    private final List<Document> foundDocuments;

    public RAGStreamBackgroundTask(RAGService ragService, RAGChatState chatState, List<DocumentChunkEmbedding> nearestDocumentChunkEmbeddings, List<Document> foundDocuments) {
        this.ragService = ragService;
        this.chatState = chatState;
        this.nearestDocumentChunkEmbeddings = nearestDocumentChunkEmbeddings;
        this.foundDocuments = foundDocuments;
    }

    public void run() {
        logger.info("Started background task for RAG streaming, chatId: {}", chatState.getChatId());

        // This will internally stream the response from the LLM RAG service and will return the final answer
        ExceptionUtils.tryCatchLog(
                () -> ragService.postNewRAGPromptStreaming(chatState, nearestDocumentChunkEmbeddings, foundDocuments),
                (ex) -> logger.error("Error streaming response from our LLM RAG service for chat " + chatState.getChatId(), ex));

        logger.info("Background task for RAG streaming ended, chatId: {}", chatState.getChatId());
    }

}
