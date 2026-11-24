package com.cloud.programmer.agent.service;

import com.cloud.programmer.agent.dto.SkillDTO;

import java.util.List;
import java.util.Map;

/**
 * @author 周梓强 2026/10/14
 */

public interface SkillService {
    void findSkillByAgentId(int agentId);

    List<SkillDTO> list(Map<String, Object> params);

    SkillDTO insert(SkillDTO skillDTO);

    SkillDTO update(SkillDTO skillDTO);

    Boolean delete(Integer id);

    Boolean updataStatus(Integer id, Integer status);
}
