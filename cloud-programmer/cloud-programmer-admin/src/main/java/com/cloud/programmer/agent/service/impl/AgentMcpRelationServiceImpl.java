package com.cloud.programmer.agent.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.programmer.agent.entity.AgentMcpRelationEntity;
import com.cloud.programmer.agent.mapper.AgentMcpRelationMapper;
import com.cloud.programmer.agent.mapper.AgentMapper;
import com.cloud.programmer.agent.mapper.McpServerMapper;
import com.cloud.programmer.agent.service.AgentMcpRelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AgentMcpRelationServiceImpl
        extends ServiceImpl<AgentMcpRelationMapper, AgentMcpRelationEntity>
        implements AgentMcpRelationService {

    private final AgentMapper agentMapper;

    private final McpServerMapper mcpServerMapper;

    /**
     * 查询 Agent 关联的 MCP 服务 ID。
     */
    @Override
    public List<Integer> listMcpServerIds(Integer agentId) {
        validateAgentExists(agentId);

        return baseMapper.selectList(
                Wrappers.<AgentMcpRelationEntity>lambdaQuery()
                        .eq(AgentMcpRelationEntity::getAgentId, agentId)
                        .orderByAsc(AgentMcpRelationEntity::getId)
                ).stream()
                .map(AgentMcpRelationEntity::getMcpServerId)
                .toList();
    }

    /**
     * 全量更新 Agent 关联的 MCP 服务。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMcpRelations(Integer agentId, List<Integer> mcpServerIds) {
        validateAgentExists(agentId);
        if (mcpServerIds == null) {
            throw new IllegalArgumentException("MCP 服务 ID 列表不能为空");
        }

        // 去重，并校验 ID 不为空。
        if (mcpServerIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("MCP 服务 ID 不能为 null");
        }

        List<Integer> distinctIds = mcpServerIds.stream().distinct().toList();

        // 批量校验 MCP 服务是否存在。
        if (!distinctIds.isEmpty()) {
            long existingCount = mcpServerMapper.selectByIds(distinctIds).size();
            if (existingCount != distinctIds.size()) {
                throw new IllegalArgumentException("部分 MCP 服务不存在");
            }
        }

        // 删除旧关联。
        baseMapper.delete(
                Wrappers.<AgentMcpRelationEntity>lambdaQuery()
                .eq(AgentMcpRelationEntity::getAgentId, agentId)
        );

        // 插入新关联。
        for (Integer mcpServerId : distinctIds) {
            AgentMcpRelationEntity relation = new AgentMcpRelationEntity();
            relation.setAgentId(agentId);
            relation.setMcpServerId(mcpServerId);
            baseMapper.insert(relation);
        }
    }

    /**
     * 查询所有可供关联的 MCP 服务。
     */
    @Override
    public List<?> listAvailableMcpServers() {
        // TODO: 根据实际 MCP 服务实体、启用状态字段和排序规则实现。
        return List.of();
    }

    /**
     * 校验 Agent 是否存在。
     */
    private void validateAgentExists(Integer agentId) {
        if (agentId == null || agentId <= 0) {
            throw new IllegalArgumentException("Agent ID 不合法");
        }

        if (agentMapper.selectById(agentId) == null) {
            throw new IllegalArgumentException("Agent 不存在，ID：" + agentId);
        }
    }
}