package org.texttechnologylab.uce.common.models.dto;

public class RAGCompleteDto {
    private String message;
    private int status;

    public RAGCompleteDto(){}

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
