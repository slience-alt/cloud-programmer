package com.cloud.programmer.runtime.mcp;

import com.cloud.programmer.runtime.client.AdminMcpClient;
import com.cloud.programmer.runtime.entity.McpServerEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class McpStartupInitializer {

    private final AdminMcpClient adminMcpClient;
    private final McpClientManager mcpClientManager;

    public McpStartupInitializer(AdminMcpClient adminMcpClient, McpClientManager mcpClientManager) {
        this.adminMcpClient = adminMcpClient;
        this.mcpClientManager = mcpClientManager;
    }

    /**
     * Runtime 启动完成后，自动连接所有启用的 MCP Server。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        log.info("开始初始化 MCP Server 连接");
        List<McpServerEntity> servers;

        try {
            servers = adminMcpClient.findEnabledServers();
        } catch (Exception e) {
            // Admin 不可用时，不阻止 Runtime 启动。
            log.error("查询启用的 MCP Server 失败，跳过本次初始化", e);
            return;
        }

        if (servers == null || servers.isEmpty()) {
            log.info("没有启用的 MCP Server，无需初始化");
            return;
        }

        int successCount = 0;
        int failedCount = 0;

        for (McpServerEntity server : servers) {
            if (server == null || server.getId() == null) {
                log.warn("发现无效的 MCP Server 配置，跳过");
                failedCount++;
                continue;
            }
            Long serverId = server.getId();

            try {
                mcpClientManager.connect(serverId);
                successCount++;
                log.info("MCP Server 连接成功，serverId={}, name={}", serverId, server.getName());
            } catch (Exception e) {
                // 单个 MCP Server 连接失败，不影响其他服务。
                failedCount++;
                log.error("MCP Server 连接失败，serverId={}, name={}", serverId, server.getName(), e);
            }
        }

        log.info("MCP Server 初始化完成，total={}, success={}, failed={}", servers.size(), successCount, failedCount);
    }
}