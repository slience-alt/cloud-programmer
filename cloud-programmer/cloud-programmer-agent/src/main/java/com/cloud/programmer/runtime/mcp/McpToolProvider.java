package com.cloud.programmer.runtime.mcp;

import com.cloud.programmer.enums.McpConnectionStatusEnum;
import com.cloud.programmer.runtime.agent.AgentConfigLoader;
import com.cloud.programmer.runtime.agent.AgentDefinition;
import com.cloud.programmer.runtime.tool.AgentTool;
import com.cloud.programmer.runtime.tool.AgentToolProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class McpToolProvider implements AgentToolProvider {

    private final McpClientManager mcpClientManager;

    public McpToolProvider(AgentConfigLoader agentConfigLoader, McpClientManager mcpClientManager) {
        this.mcpClientManager = mcpClientManager;
    }


    @Override
    public List<AgentTool> getTools(AgentDefinition definition) {

        if (definition == null) {
            throw new IllegalArgumentException("Agent 配置不能为空");
        }

        List<Integer> mcpIds = definition.getMcpIds();

        if (mcpIds == null || mcpIds.isEmpty()) {
            return List.of();
        }

        List<AgentTool> tools = new ArrayList<>();

        for (Integer mcpId : mcpIds) {
            if (mcpId == null) {
                continue;
            }

            long serverId = mcpId.longValue();

            if (mcpClientManager.getStatus(serverId) != McpConnectionStatusEnum.CONNECTED) {
                mcpClientManager.connect(serverId);
            }

            tools.addAll(mcpClientManager.getTools(serverId));
        }

        return List.copyOf(tools);
    }
}