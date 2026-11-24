package com.cloud.programmer.agent.service;

import com.cloud.programmer.agent.dto.SkillDTO;

import java.util.List;

/**
 * @author 周梓强 2026/10/14
 */

public interface AgentSkillRelationService {
    List<SkillDTO> findSkillByAgentId(int agentId);
}
