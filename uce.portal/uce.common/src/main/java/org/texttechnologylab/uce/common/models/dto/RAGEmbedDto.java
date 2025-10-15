package org.texttechnologylab.uce.common.models.dto;

public class RAGEmbedDto {
    private int status;
    private float[] message;

    public RAGEmbedDto(){

    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public float[] getMessage() {
        return message;
    }

    public void setMessage(float[] message) {
        this.message = message;
    }
}
