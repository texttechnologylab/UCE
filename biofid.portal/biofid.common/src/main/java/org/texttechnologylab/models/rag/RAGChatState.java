package org.texttechnologylab.models.rag;

import org.apache.uima.cas.text.Language;
import org.joda.time.DateTime;
import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.utils.SupportedLanguages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class RAGChatState {
    private UUID chatId;
    private String model;
    private DateTime started;
    private ArrayList<RAGChatMessage> messages;
    private SupportedLanguages language;
    public RAGChatState(){
        this.started = DateTime.now();
    }

    public DateTime getStarted() {
        return started;
    }

    public void setStarted(DateTime started) {
        this.started = started;
    }

    public SupportedLanguages getLanguage() {
        return language;
    }

    public void setLanguage(SupportedLanguages language) {
        this.language = language;
    }

    public UUID getChatId() {
        return chatId;
    }

    public void setChatId(UUID chatId) {
        this.chatId = chatId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<RAGChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(ArrayList<RAGChatMessage> messages) {
        this.messages = messages;
    }

    public void addMessage(RAGChatMessage message){
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }
        this.messages.add(message);
    }
}
