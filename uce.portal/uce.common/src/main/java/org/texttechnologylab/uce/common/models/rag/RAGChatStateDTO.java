package org.texttechnologylab.uce.common.models.rag;

import lombok.*;
import org.joda.time.DateTime;
import org.texttechnologylab.uce.common.config.uceConfig.RAGModelConfig;
import org.texttechnologylab.uce.common.utils.SupportedLanguages;

import java.util.*;

@Setter
@Getter
public class RAGChatStateDTO {
    private UUID chatId;
    private RAGModelConfig model;
    private DateTime started;
    private ArrayList<RAGChatMessage> messages;
    private SupportedLanguages language;

    public void setModel(RAGModelConfig model) {
        if (model != null) {
            // Dont leak API keys or internal URLs
            this.model = new RAGModelConfig();
            this.model.setModel(model.getModel());
            this.model.setDisplayName(model.getDisplayName());
            this.model.setStreaming(model.isStreaming());
        }
        else {
            this.model = null;
        }
    }

    public void setMessages(ArrayList<RAGChatMessage> messages) {
        this.messages = messages;
        if (this.messages != null) {
            // NOTE remove the documents for now, could be useful later though
            for (RAGChatMessage message : this.messages) {
                message.setContextDocuments(new ArrayList<>());
            }
        }
    }

    public static RAGChatStateDTO fromRAGChatState(RAGChatState ragChatState) {
        RAGChatStateDTO dto = new RAGChatStateDTO();
        dto.setChatId(ragChatState.getChatId());
        dto.setModel(ragChatState.getModel());
        dto.setStarted(ragChatState.getStarted());
        dto.setMessages(ragChatState.getMessages());
        dto.setLanguage(ragChatState.getLanguage());
        return dto;
    }
}
