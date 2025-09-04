package org.texttechnologylab.uce.common.models.util;

public class HealthStatus {

    private boolean isAlive = false;
    private String message;
    private Exception exception;

    public HealthStatus(boolean isAlive, String message, Exception exception) {
        this.isAlive = isAlive;
        this.message = message;
        this.exception = exception;
    }

    public HealthStatus(){

    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        isAlive = alive;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }
}
