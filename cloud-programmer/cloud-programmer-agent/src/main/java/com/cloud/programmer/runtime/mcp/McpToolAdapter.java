
package com.cloud.programmer.runtime.mcp;

import com.cloud.programmer.runtime.tool.AgentTool;
import com.cloud.programmer.runtime.tool.ToolDefinition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;
import java.util.Objects;

/**
 * MCP 工具适配器。
 * 将 MCP Server 提供的工具包装为 AgentTool。
 */
public class McpToolAdapter implements AgentTool {
    /**
     * 注册到 ToolRegistry 中的工具名称。
     * 例如：mcp_1_search
     */
    private final String exposedName;

    /**
     * MCP Server 原始工具定义。
     */
    private final McpSchema.Tool mcpTool;

    /**
     * 对应 MCP Server 的客户端。
     */
    private final McpSyncClient mcpClient;

    private final ObjectMapper objectMapper;

    private final ToolDefinition toolDefinition;

    public McpToolAdapter(String exposedName, McpSchema.Tool mcpTool, McpSyncClient mcpClient, ObjectMapper objectMapper) {
        if (exposedName == null || exposedName.isBlank()) {
            throw new IllegalArgumentException("注册工具名称不能为空");
        }
        this.exposedName = exposedName;
        this.mcpTool = Objects.requireNonNull(mcpTool, "MCP 工具定义不能为空");
        this.mcpClient = Objects.requireNonNull(mcpClient, "MCP Client 不能为空");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper 不能为空");
        this.toolDefinition = new ToolDefinition(exposedName, mcpTool.description() == null || mcpTool.description().isBlank() ? "MCP 工具：" + mcpTool.name() : mcpTool.description(), toJsonSchema(mcpTool));
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public String execute(Map<String, Object> arguments) throws Exception {
        Map<String, Object> safeArguments = arguments == null ? Map.of() : arguments;

        // 调用 MCP Server 时，必须使用服务端原始工具名称，
        // 而不是注册到本地 ToolRegistry 的 exposedName。
        McpSchema.CallToolResult result = mcpClient.callTool(new McpSchema.CallToolRequest(mcpTool.name(), safeArguments));

        if (result == null) {
            throw new IllegalStateException("MCP 工具未返回结果：" + exposedName);
        }

        if (Boolean.TRUE.equals(result.isError())) {
            throw new IllegalStateException("MCP 工具执行失败：" + exposedName + "，结果：" + serializeResult(result));
        }

        return serializeResult(result);
    }

    private String toJsonSchema(McpSchema.Tool tool) {
        try {
            return objectMapper.writeValueAsString(tool.inputSchema());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("转换 MCP 工具参数 Schema 失败：" + tool.name(), e);
        }
    }

    private String serializeResult(McpSchema.CallToolResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化 MCP 工具结果失败", e);
        }
    }
}
