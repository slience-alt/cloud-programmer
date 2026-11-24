package com.cloud.programmer.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cloud.programmer.agent.dto.SkillDTO;
import com.cloud.programmer.agent.entity.AgentSkillRelationEntity;
import com.cloud.programmer.agent.entity.SkillEntity;
import com.cloud.programmer.agent.mapper.AgentSkillRelationMapper;
import com.cloud.programmer.agent.mapper.SkillMapper;
import com.cloud.programmer.agent.service.AgentSkillRelationService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 周梓强 2026/10/14
 */

@Service
public class AgentSkillRelationServiceImpl implements AgentSkillRelationService {

    private final AgentSkillRelationMapper agentSkillRelationMapper;
    private final SkillMapper skillMapper;

    public AgentSkillRelationServiceImpl(AgentSkillRelationMapper agentSkillRelationMapper, SkillMapper skillMapper) {
        this.agentSkillRelationMapper = agentSkillRelationMapper;
        this.skillMapper = skillMapper;
    }

    @Override
    public List<SkillDTO> findSkillByAgentId(int agentId) {
        List<SkillDTO> skillDTOS = new ArrayList<>();
        QueryWrapper<AgentSkillRelationEntity> agentSkillRelationEntityQueryWrapper = new QueryWrapper<>();
        List<AgentSkillRelationEntity> agentToSkills = agentSkillRelationMapper.selectList(agentSkillRelationEntityQueryWrapper.eq("agent_id", agentId));
        for (AgentSkillRelationEntity agentToSkill : agentToSkills) {
            Integer skillId = agentToSkill.getSkillId();
            SkillEntity skillEntity = skillMapper.selectById(skillId);
            SkillDTO skillDTO = convertEntityToSkillDTO(skillEntity);
            skillDTOS.add(skillDTO);
        }
        return skillDTOS;

    }

    private SkillDTO convertEntityToSkillDTO(SkillEntity skillEntity) {
        SkillDTO skillDTO = new SkillDTO();
        skillDTO.setId(skillEntity.getId());
        skillDTO.setName(skillEntity.getName());
        skillDTO.setDescription(skillEntity.getDescription());
        skillDTO.setInstruction(skillEntity.getInstruction());
        return skillDTO;
    }

}
