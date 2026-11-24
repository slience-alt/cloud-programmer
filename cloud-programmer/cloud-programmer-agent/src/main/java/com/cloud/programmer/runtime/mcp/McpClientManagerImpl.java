
package com.cloud.programmer.runtime.mcp;

import com.cloud.programmer.enums.McpConnectionStatusEnum;
import com.cloud.programmer.runtime.client.AdminMcpClient;
import com.cloud.programmer.runtime.entity.McpServerEntity;
import com.cloud.programmer.runtime.tool.AgentTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class McpClientManagerImpl implements McpClientManager {

    private final ObjectMapper objectMapper;
    private final AdminMcpClient adminMcpClient;

    private final ConcurrentMap<Long, McpSyncClient> clients = new ConcurrentHashMap<>();

    /**
     * 每个 MCP Server 对应的工具实例。
     */
    private final ConcurrentMap<Long, List<AgentTool>> serverTools = new ConcurrentHashMap<>();

    private final ConcurrentMap<Long, McpConnectionStatusEnum> statuses = new ConcurrentHashMap<>();

    public McpClientManagerImpl(ObjectMapper objectMapper, AdminMcpClient adminMcpClient) {
        this.objectMapper = objectMapper;
        this.adminMcpClient = adminMcpClient;
    }

    @Override
    public synchronized void connect(Long serverId) {
        if (serverId == null) {
            throw new IllegalArgumentException("MCP Server ID 不能为空");
        }

        if (clients.containsKey(serverId)) {
            return;
        }

        statuses.put(serverId, McpConnectionStatusEnum.CONNECTING);

        McpSyncClient client = null;

        try {
            McpServerEntity server = adminMcpClient.findByServerId(serverId);
            if (server == null) {
                throw new IllegalArgumentException("MCP Server 不存在：" + serverId);
            }
            if (!Boolean.TRUE.equals(server.getEnabled())) {
                throw new IllegalStateException("MCP Server 已禁用：" + serverId);
            }
            if (!"STREAMABLE_HTTP".equalsIgnoreCase(server.getTransportType())) {
                throw new UnsupportedOperationException("当前仅支持 STREAMABLE_HTTP：" + server.getTransportType());
            }
            if (server.getServerUrl() == null || server.getServerUrl().isBlank()) {
                throw new IllegalArgumentException("MCP Server URL 不能为空");
            }
            int connectTimeout = server.getConnectTimeoutMs() == null ? 10000 : server.getConnectTimeoutMs();
            int requestTimeout = server.getRequestTimeoutMs() == null ? 30000 : server.getRequestTimeoutMs();
            var transport = HttpClientStreamableHttpTransport
                    .builder(server.getServerUrl())
                    .clientBuilder(HttpClient.newBuilder()
                            .connectTimeout(
                                    Duration.ofMillis(connectTimeout)))
                    .jsonMapper(new JacksonMcpJsonMapper(objectMapper))
                    .build();

            client = McpClient.sync(transport).clientInfo(new McpSchema.Implementation("cloud-programmer", "1.0.0"))
                    .requestTimeout(Duration.ofMillis(requestTimeout))
                    .build();

            // 初始化 MCP 会话
            client.initialize();
            // 发现服务器提供的工具
            List<McpSchema.Tool> tools = client.listTools().tools();
            List<AgentTool> adapters = new ArrayList<>();

            for (McpSchema.Tool tool : tools) {
                String exposedName = "mcp_" + serverId + "_" + tool.name();
                McpToolAdapter adapter = new McpToolAdapter(exposedName, tool, client, objectMapper);
                adapters.add(adapter);
            }

            clients.put(serverId, client);
            serverTools.put(serverId, List.copyOf(adapters));
            statuses.put(serverId, McpConnectionStatusEnum.CONNECTED);

        } catch (Exception e) {
            if (client != null) {
                try {
                    client.close();
                } catch (Exception closeException) {
                    e.addSuppressed(closeException);
                }
            }
            statuses.put(serverId, McpConnectionStatusEnum.FAILED);
            throw new IllegalStateException("连接 MCP Server 失败，serverId=" + serverId, e);
        }
    }

    @Override
    public synchronized void disconnect(Long serverId) {
        if (serverId == null) {
            return;
        }
        serverTools.remove(serverId);
        McpSyncClient client = clients.remove(serverId);

        try {
            if (client != null) {
                client.close();
            }
        } finally {
            statuses.put(serverId, McpConnectionStatusEnum.DISCONNECTED
            );
        }
    }

    @Override
    public synchronized void reconnect(Long serverId) {
        disconnect(serverId);
        connect(serverId);
    }

    @Override
    public McpConnectionStatusEnum getStatus(Long serverId) {
        if (serverId == null) {
            return McpConnectionStatusEnum.DISCONNECTED;
        }
        return statuses.getOrDefault(serverId, McpConnectionStatusEnum.DISCONNECTED);
    }

    @Override
    public List<AgentTool> getTools(Long serverId) {
        if (serverId == null) {
            return List.of();
        }

        if (getStatus(serverId) != McpConnectionStatusEnum.CONNECTED) {
            throw new IllegalStateException(
                    "MCP Server 尚未连接：" + serverId);
        }

        return serverTools.getOrDefault(serverId, List.of());
    }
}
