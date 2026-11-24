package com.cloud.programmer.runtime.api;

public class AgentChatResponse {

    private boolean success;

    private String message;

    private String error;

    public AgentChatResponse() {}

    public AgentChatResponse(boolean success, String message, String error) {
        this.success = success;
        this.message = message;
        this.error = error;
    }

    public static AgentChatResponse success(String answer) {
        return new AgentChatResponse(true, answer, null);
    }

    public static AgentChatResponse failure(String error) {
        return new AgentChatResponse(false, null, error);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getError() {
        return error;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setError(String error) {
        this.error = error;
    }
}
