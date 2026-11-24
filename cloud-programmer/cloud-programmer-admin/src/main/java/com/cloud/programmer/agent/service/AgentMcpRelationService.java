package com.cloud.programmer.agent.service;

import java.util.List;

public interface AgentMcpRelationService {

    /**
     * 查询 Agent 关联的 MCP 服务 ID。
     *
     * @param agentId Agent ID
     * @return MCP 服务 ID 列表
     */
    List<Integer> listMcpServerIds(Integer agentId);

    /**
     * 全量更新 Agent 关联的 MCP 服务。
     *
     * @param agentId Agent ID
     * @param mcpServerIds MCP 服务 ID 列表，空列表表示清空关联
     */
    void updateMcpRelations(Integer agentId, List<Integer> mcpServerIds);

    /**
     * 查询所有可供关联的 MCP 服务。
     *
     * @return MCP 服务列表
     */
    List<?> listAvailableMcpServers();
}