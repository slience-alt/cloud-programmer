package com.cloud.programmer.runtime.conversation;

import java.time.Instant;

public class AgentConversation {

    private final String conversationId;

    private final Instant createdAt;


    public AgentConversation(String conversationId) {
        this.conversationId = conversationId;
        this.createdAt = Instant.now();
    }


    public String getConversationId() {
        return conversationId;
    }


    public Instant getCreatedAt() {
        return createdAt;
    }
}