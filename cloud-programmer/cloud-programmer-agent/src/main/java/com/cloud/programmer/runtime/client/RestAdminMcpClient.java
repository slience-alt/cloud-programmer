package com.cloud.programmer.runtime.client;

import com.cloud.programmer.runtime.entity.McpServerEntity;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@Primary
public class RestAdminMcpClient implements AdminMcpClient {

    private final RestClient restClient;

    public RestAdminMcpClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<McpServerEntity> findEnabledServers() {
        return restClient.get()
                .uri("/internal/mcp/list")
                .retrieve()
                .body(new ParameterizedTypeReference<List<McpServerEntity>>() {});
    }

    @Override
    public McpServerEntity findByServerId(Long serverId) {
        return restClient.get()
                .uri("/internal/mcp/findById/{id}", serverId)
                .retrieve()
                .body(McpServerEntity.class);
    }
}