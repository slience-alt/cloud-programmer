package com.cloud.programmer.runtime.mcp;

import com.cloud.programmer.enums.McpConnectionStatusEnum;
import com.cloud.programmer.runtime.tool.AgentTool;

import java.util.List;

public interface McpClientManager {

    /**
     * 连接 MCP Server 并发现工具
     */
    void connect(Long serverId);

    /**
     * 断开 MCP Server
     */
    void disconnect(Long serverId);

    /**
     * 重新连接 MCP Server
     */
    void reconnect(Long serverId);

    /**
     * 查询当前连接状态
     */
    McpConnectionStatusEnum getStatus(Long serverId);

    /**
     * 获取指定 MCP Server 提供的工具。
     */
    List<AgentTool> getTools(Long serverId);

}