package org.texttechnologylab.uce.common.models.rag;

import org.joda.time.DateTime;
import org.texttechnologylab.uce.common.models.corpus.Document;
import org.texttechnologylab.uce.common.models.corpus.Image;

import java.util.ArrayList;
import java.util.List;

public class RAGChatMessage {
    private Roles role;
    private String message;
    private DateTime created;
    private ArrayList<Document> contextDocuments;
    private boolean done;

    // Images that are sent with the message
    // NOTE that they will use the base64 encoded image in the message payload
    private List<Image> images;

    public static String thinkEndTag = "</think>";

    public static String cleanThinkTag(String answer) {
        if (answer == null) return answer;

        if (answer.contains(thinkEndTag)) {
            int thinkEndIndex = answer.lastIndexOf(thinkEndTag) + thinkEndTag.length();
            answer = answer.substring(thinkEndIndex).trim();
        }

        return answer;
    }

    public static boolean hasThinkTag(String answer) {
        return answer != null && answer.contains(thinkEndTag);
    }

    public static String getThinkTag(String answer) {
        if (answer == null) return answer;

        if (answer.contains(thinkEndTag)) {
            int thinkEndIndex = answer.lastIndexOf(thinkEndTag);
            answer = answer.substring(0, thinkEndIndex).trim();
            return answer;
        }

        return "";
    }

    /**
     * This is the prompt we send to our rag webserver. It differs from
     * the actual message we show in the UI, which is the message.
     */
    private String prompt;
    private List<Long> contextDocument_Ids;

    public RAGChatMessage(){
        this.created = DateTime.now();
        this.contextDocuments = new ArrayList<>();
        this.images = new ArrayList<>();
        // by default, we expect the message to already be finished. this is set to "false" for streaming requests only
        this.done = true;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public List<Image> getImages() {
        return images;
    }

    public void setImages(List<Image> images) {
        this.images = images;
    }

    public void addImage(Image image) {
        this.images.add(image);
    }

    public ArrayList<Document> getContextDocuments() {
        return contextDocuments;
    }

    public void setContextDocuments(ArrayList<Document> contextDocuments) {
        this.contextDocuments = contextDocuments;
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

    public String getMessageWithoutThinking() {
        return cleanThinkTag(message);
    }

    public String getMessageOnlyThinking() {
        return getThinkTag(message);
    }

    public boolean hasThinking() {
        return hasThinkTag(message);
    }

    public List<Long> getContextDocument_Ids() {
        return contextDocument_Ids;
    }

    public void setContextDocument_Ids(List<Long> contextDocumentId) {
        this.contextDocument_Ids = contextDocumentId;
    }
}
