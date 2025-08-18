package org.texttechnologylab.models.rag;

import lombok.Getter;
import lombok.Setter;
import org.apache.uima.cas.text.Language;
import org.joda.time.DateTime;
import org.texttechnologylab.config.uceConfig.RAGModelConfig;
import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.utils.SupportedLanguages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class RAGChatState {
    private UUID chatId;
    private RAGModelConfig model;
    private DateTime started;
    private ArrayList<RAGChatMessage> messages;
    private SupportedLanguages language;
    public RAGChatState(){
        this.started = DateTime.now();
    }

    public void addMessage(RAGChatMessage message){
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }
        this.messages.add(message);
    }

    public RAGChatMessage getNewestMessage() {
        return this.messages != null && !this.messages.isEmpty() ? this.messages.getLast() : null;
    }
}
