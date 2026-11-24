package com.cloud.programmer.agent.service;

import com.cloud.programmer.agent.dto.AgentDTO;
import com.cloud.programmer.agent.entity.AgentEntity;

import java.util.List;
import java.util.Map;

/**
 * @author 周梓强 2026/10/13
 */


public interface AgentService {

    AgentDTO selectByAgentId(Integer agentId);

    /**
     * 根据 Agent ID 和所属用户 ID 查询 Agent。
     * 供内部服务调用，不能只根据 Agent ID 查询。
     */
    AgentDTO selectByAgentId(Integer agentId, Long userId);

    List<AgentDTO> list(Map<String, Object> params);

    AgentDTO insert(AgentDTO agentDTO);

    Boolean updateStatus(Integer id, Integer status);

    AgentDTO update(AgentDTO agentDTO);

    Boolean delete(Integer id);

    Boolean batchDelete(List<Integer> ids);
}
