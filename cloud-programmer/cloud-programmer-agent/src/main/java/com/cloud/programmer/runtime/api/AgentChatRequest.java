package com.cloud.programmer.runtime.api;

public class AgentChatRequest {

    private String message;

    private String conversationId;

    private Long agentId;

    public AgentChatRequest() {}

    public AgentChatRequest(String message, String conversationId, Long agentId) {
        this.message = message;
        this.conversationId = conversationId;
        this.agentId = agentId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }
}
