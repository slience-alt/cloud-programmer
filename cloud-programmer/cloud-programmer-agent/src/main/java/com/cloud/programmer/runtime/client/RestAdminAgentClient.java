package com.cloud.programmer.runtime.client;

import com.cloud.programmer.runtime.agent.AgentDTO;
import com.cloud.programmer.runtime.client.AdminAgentClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestAdminAgentClient implements AdminAgentClient {

    private final RestClient restClient;

    public RestAdminAgentClient(
            @Qualifier("adminRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public AgentDTO findById(Long agentId, Long userId) {
        return restClient.get()
                .uri("/internal/agent/findById?id={id}&userId={userId}",
                        agentId, userId)
                .retrieve()
                .body(AgentDTO.class);
    }
}