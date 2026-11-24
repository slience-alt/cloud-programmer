package com.cloud.programmer.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cloud.programmer.agent.dto.AgentDTO;
import com.cloud.programmer.agent.entity.AgentEntity;
import com.cloud.programmer.agent.entity.AgentMcpRelationEntity;
import com.cloud.programmer.agent.entity.AgentSkillRelationEntity;
import com.cloud.programmer.agent.mapper.AgentMapper;
import com.cloud.programmer.agent.mapper.AgentMcpRelationMapper;
import com.cloud.programmer.agent.mapper.AgentSkillRelationMapper;
import com.cloud.programmer.agent.mapper.SkillMapper;
import com.cloud.programmer.agent.service.AgentService;
import com.cloud.programmer.enums.AgentStatusEnum;
import com.cloud.programmer.enums.ResponseCodeEnum;
import com.cloud.programmer.exception.BizException;
import com.cloud.programmer.security.context.SecurityUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 周梓强 2026/10/13
 */

@Service
public class AgentServiceImpl implements AgentService {

    private final AgentMapper agentMapper;

    private final SkillMapper skillMapper;

    private  final AgentSkillRelationMapper agentSkillRelationMapper;

    private final AgentMcpRelationMapper agentMcpRelationMapper;

    private final Map<Integer, List<Integer>> AGENT_TO_SKILLIDS = new ConcurrentHashMap<>();

    private final Map<Integer, List<Integer>> AGENT_TO_MCPIDS = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        initCacheMap(AGENT_TO_SKILLIDS, "skill");
        initCacheMap(AGENT_TO_MCPIDS, "mcp");
    }



    public AgentServiceImpl(AgentMapper agentMapper, SkillMapper skillMapper, AgentSkillRelationMapper agentSkillRelationMapper, AgentMcpRelationMapper agentMcpRelationMapper) {
        this.agentMapper = agentMapper;
        this.skillMapper = skillMapper;
        this.agentSkillRelationMapper = agentSkillRelationMapper;
        this.agentMcpRelationMapper = agentMcpRelationMapper;
    }




    @Override
    public AgentDTO selectByAgentId(Integer agentId) {
        return selectByAgentId(agentId, requireCurrentUserId());
    }

    @Override
    public AgentDTO selectByAgentId(Integer agentId, Long userId) {
        AgentEntity agentEntity = getOwnedAgent(agentId, userId);
        return convertEntityToAgentDTO(agentEntity);
    }



    @Override
    public List<AgentDTO> list(Map<String, Object> params) {
        QueryWrapper<AgentEntity> agentEntityQueryWrapper = new QueryWrapper<>();
        if (params.get("name") != null) {
            agentEntityQueryWrapper.like("name", params.get("name"));
        }

        if (params.get("modelName") != null) {
            agentEntityQueryWrapper.like("model_name", params.get("modelName"));
        }
        agentEntityQueryWrapper.eq("owner_user_id", SecurityUtils.getCurrentUserId());
        List<AgentEntity> agentEntities = agentMapper.selectList(agentEntityQueryWrapper);
        List<AgentDTO> agentList = new ArrayList<>();
        for (AgentEntity agentEntity : agentEntities) {
            AgentDTO agentDTO = convertEntityToAgentDTO(agentEntity);
            List<Integer> skillIds = AGENT_TO_SKILLIDS.get(agentEntity.getId());
            List<Integer> mcpIds = AGENT_TO_MCPIDS.get(agentEntity.getId());
            agentDTO.setMcpIds(mcpIds);
            agentDTO.setSkillIds(skillIds);
            agentList.add(agentDTO);
        }
        return agentList;
    }

    @Override
    @Transactional
    public AgentDTO insert(AgentDTO agentDTO) {
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName(agentDTO.getName());
        agentEntity.setDescription(agentDTO.getDescription());
        agentEntity.setSystemPrompt(agentDTO.getSystemPrompt());
        agentEntity.setModelName(agentDTO.getModelName());
        agentEntity.setStatus(AgentStatusEnum.ONLINE.getStatus());
        agentEntity.setOwnerUserId(SecurityUtils.getCurrentUserId());
        agentEntity.setCreateTime(new Date());
        agentEntity.setUpdateTime(new Date());
        agentMapper.insert(agentEntity);
        List<Integer> skillIds = agentDTO.getSkillIds();
        if (skillIds != null) {
            for (Integer skillId : skillIds) {
                AgentSkillRelationEntity agentSkillRelationEntity = new AgentSkillRelationEntity();
                agentSkillRelationEntity.setAgentId(agentEntity.getId());
                agentSkillRelationEntity.setSkillId(skillId);
                agentSkillRelationMapper.insert(agentSkillRelationEntity);
            }
        }
        List<Integer> mcpIds = agentDTO.getMcpIds();
        if (mcpIds != null) {
            for (Integer mcpId : mcpIds) {
                AgentMcpRelationEntity agentMcpRelationEntity = new AgentMcpRelationEntity();
                agentMcpRelationEntity.setAgentId(agentEntity.getId());
                agentMcpRelationEntity.setMcpServerId(mcpId);
                agentMcpRelationMapper.insert(agentMcpRelationEntity);
            }
        }
        agentDTO = convertEntityToAgentDTO(agentEntity);
        refreshAgentPluginCache(agentEntity.getId());
        return agentDTO;
    }

    @Override
    public Boolean updateStatus(Integer id, Integer status) {
        // 使用当前用户 ID 查询其拥有的 Agent
        AgentEntity agentEntity = getOwnedAgent(id, requireCurrentUserId());
        agentEntity.setStatus(status);
        return agentMapper.updateById(agentEntity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentDTO update(AgentDTO agentDTO) {
        Long userId = requireCurrentUserId();
        // 1. 校验 Agent 所属权
        AgentEntity oldAgent = getOwnedAgent(agentDTO.getId(), userId);
        // 2. 更新 Agent 基础信息
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName(agentDTO.getName());
        agentEntity.setDescription(agentDTO.getDescription());
        agentEntity.setSystemPrompt(agentDTO.getSystemPrompt());
        agentEntity.setModelName(agentDTO.getModelName());
        agentEntity.setUpdateTime(new Date());

        int rows = agentMapper.update(agentEntity,
                Wrappers.<AgentEntity>lambdaUpdate()
                        .eq(AgentEntity::getId, oldAgent.getId())
                        .eq(AgentEntity::getOwnerUserId, userId)
        );
        if (rows <= 0) {
            throw new BizException(ResponseCodeEnum.DATA_NOT_FOUND);
        }
        // 3. 更新 Skill 关联
        List<Integer> skillIds = agentDTO.getSkillIds();
        if (skillIds != null) {
            agentSkillRelationMapper.delete(
                    Wrappers.<AgentSkillRelationEntity>lambdaQuery()
                            .eq(AgentSkillRelationEntity::getAgentId, oldAgent.getId())
            );

            for (Integer skillId : skillIds) {
                AgentSkillRelationEntity relation = new AgentSkillRelationEntity();
                relation.setAgentId(oldAgent.getId());
                relation.setSkillId(skillId);
                agentSkillRelationMapper.insert(relation);
            }
        }

        // 4. 更新 MCP 关联
        List<Integer> mcpIds = agentDTO.getMcpIds();
        if (mcpIds != null) {
            agentMcpRelationMapper.delete(
                    Wrappers.<AgentMcpRelationEntity>lambdaQuery()
                            .eq(AgentMcpRelationEntity::getAgentId, oldAgent.getId())
            );

            for (Integer mcpId : mcpIds) {
                AgentMcpRelationEntity relation = new AgentMcpRelationEntity();
                relation.setAgentId(oldAgent.getId());
                relation.setMcpServerId(mcpId);
                agentMcpRelationMapper.insert(relation);
            }
        }
        // 5. 刷新关联缓存
        refreshAgentPluginCache(oldAgent.getId());
        // 6. 返回最新数据
        return selectByAgentId(oldAgent.getId(), userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Integer id) {
        Long userId = requireCurrentUserId();
        AgentEntity agent = getOwnedAgent(id, userId);
        int rows = agentMapper.delete(
                Wrappers.<AgentEntity>lambdaQuery()
                        .eq(AgentEntity::getId, agent.getId())
                        .eq(AgentEntity::getOwnerUserId, userId)
        );
        if (rows <= 0) {
            throw new BizException(ResponseCodeEnum.DATA_NOT_FOUND);
        }
        agentSkillRelationMapper.delete(
                Wrappers.<AgentSkillRelationEntity>lambdaQuery()
                        .eq(AgentSkillRelationEntity::getAgentId, id)
        );
        agentMcpRelationMapper.delete(
                Wrappers.<AgentMcpRelationEntity>lambdaQuery()
                        .eq(AgentMcpRelationEntity::getAgentId, id)
        );
        refreshAgentPluginCache(id);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchDelete(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }

        Long userId = requireCurrentUserId();

        // 去重，避免重复 ID 影响数量判断
        List<Integer> uniqueIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (uniqueIds.isEmpty()) {
            return false;
        }

        // 查询当前用户实际拥有的 Agent
        List<AgentEntity> ownedAgents = agentMapper.selectList(
                Wrappers.<AgentEntity>lambdaQuery()
                        .select(AgentEntity::getId)
                        .in(AgentEntity::getId, uniqueIds)
                        .eq(AgentEntity::getOwnerUserId, userId)
        );

        if (ownedAgents.size() != uniqueIds.size()) {
            throw new BizException(ResponseCodeEnum.DATA_NOT_FOUND);
        }

        // 删除 Agent 主表
        int rows = agentMapper.delete(
                Wrappers.<AgentEntity>lambdaQuery()
                        .in(AgentEntity::getId, uniqueIds)
                        .eq(AgentEntity::getOwnerUserId, userId)
        );

        if (rows != uniqueIds.size()) {
            throw new BizException(ResponseCodeEnum.DATA_NOT_FOUND);
        }

        // 删除关联数据
        agentSkillRelationMapper.delete(
                Wrappers.<AgentSkillRelationEntity>lambdaQuery()
                        .in(AgentSkillRelationEntity::getAgentId, uniqueIds)
        );

        agentMcpRelationMapper.delete(
                Wrappers.<AgentMcpRelationEntity>lambdaQuery()
                        .in(AgentMcpRelationEntity::getAgentId, uniqueIds)
        );

        // 清理缓存
        for (Integer id : uniqueIds) {
            refreshAgentPluginCache(id);
        }

        return true;
    }

    private AgentDTO convertEntityToAgentDTO(AgentEntity agentEntity) {
        AgentDTO agentDTO = new AgentDTO();
        agentDTO.setId(agentEntity.getId());
        agentDTO.setName(agentEntity.getName());
        agentDTO.setDescription(agentEntity.getDescription());
        agentDTO.setSystemPrompt(agentEntity.getSystemPrompt());
        agentDTO.setModelName(agentEntity.getModelName());
        agentDTO.setStatus(agentEntity.getStatus());
        List<Integer> mcpIds = agentMcpRelationMapper.selectList(
                Wrappers.<AgentMcpRelationEntity>lambdaQuery()
                        .eq(AgentMcpRelationEntity::getAgentId, agentEntity.getId())).stream()
                .map(AgentMcpRelationEntity::getMcpServerId)
                .distinct()
                .toList();
        agentDTO.setMcpIds(mcpIds);
        return agentDTO;
    }

    private void initCacheMap(Map<Integer, List<Integer>> agentToPluginIds, String type) {
        if ("skill".equals(type)) {
            List<AgentSkillRelationEntity> agentSkillRelationEntities = agentSkillRelationMapper.selectList(null);
            for (AgentSkillRelationEntity agentSkillRelationEntity : agentSkillRelationEntities) {
                agentToPluginIds.computeIfAbsent(agentSkillRelationEntity.getAgentId(), key -> new ArrayList<>()).add(agentSkillRelationEntity.getSkillId());
            }
        } else if ("mcp".equals(type)) {
            List<AgentMcpRelationEntity> agentMcpRelationEntities = agentMcpRelationMapper.selectList(null);
            for (AgentMcpRelationEntity agentMcpRelationEntity : agentMcpRelationEntities) {
                agentToPluginIds.computeIfAbsent(agentMcpRelationEntity.getAgentId(), key -> new ArrayList<>()).add(agentMcpRelationEntity.getMcpServerId());
            }
        }

    }


    private void refreshAgentPluginCache(Integer agentId) {
        List<Integer> skillIds = agentSkillRelationMapper.selectList(
                        Wrappers.<AgentSkillRelationEntity>lambdaQuery()
                                .eq(AgentSkillRelationEntity::getAgentId, agentId)
                ).stream()
                .map(AgentSkillRelationEntity::getSkillId)
                .distinct()
                .toList();

        List<Integer> mcpIds = agentMcpRelationMapper.selectList(
                        Wrappers.<AgentMcpRelationEntity>lambdaQuery()
                                .eq(AgentMcpRelationEntity::getAgentId, agentId)
                ).stream()
                .map(AgentMcpRelationEntity::getMcpServerId)
                .distinct()
                .toList();

        updateCache(AGENT_TO_SKILLIDS, agentId, skillIds);
        updateCache(AGENT_TO_MCPIDS, agentId, mcpIds);
    }

    private void updateCache(Map<Integer, List<Integer>> cache,
                             Integer agentId,
                             List<Integer> ids) {
        if (ids.isEmpty()) {
            cache.remove(agentId);
        } else {
            cache.put(agentId, ids);
        }
    }

    /**
     * 获取当前登录用户 ID
     */
    private Long requireCurrentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();

        if (userId == null) {
            throw new BizException(ResponseCodeEnum.USER_NO_LOGIN);
        }

        return userId;
    }

    /**
     * 查询当前用户拥有的 Agent。
     */
    private AgentEntity getOwnedAgent(Integer agentId, Long userId) {
        if (agentId == null || userId == null) {
            throw new BizException(ResponseCodeEnum.DATA_NOT_FOUND);
        }

        AgentEntity agent = agentMapper.selectOne(
                Wrappers.<AgentEntity>lambdaQuery()
                        .eq(AgentEntity::getId, agentId)
                        .eq(AgentEntity::getOwnerUserId, userId)
        );

        if (agent == null) {
            throw new BizException(ResponseCodeEnum.DATA_NOT_FOUND);
        }

        return agent;
    }


}
