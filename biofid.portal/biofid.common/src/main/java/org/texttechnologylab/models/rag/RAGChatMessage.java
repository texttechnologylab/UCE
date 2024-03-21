package org.texttechnologylab.models.rag;

import org.joda.time.DateTime;

import java.util.List;

public class RAGChatMessage {
    private Roles role;
    private String message;
    private DateTime created;

    /**
     * This is the prompt we send to our rag webserver. It differs from
     * the actual message we show in the UI, which is the message.
     */
    private String prompt;
    private List<Long> contextDocument_Ids;

    public RAGChatMessage(){
        this.created = DateTime.now();
    }

    public DateTime getCreated() {
        return created;
    }

    public void setCreated(DateTime created) {
        this.created = created;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Roles getRole() {
        return role;
    }

    public void setRole(Roles role) {
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Long> getContextDocument_Ids() {
        return contextDocument_Ids;
    }

    public void setContextDocument_Ids(List<Long> contextDocumentId) {
        this.contextDocument_Ids = contextDocumentId;
    }
}
