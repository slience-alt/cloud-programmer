package com.cloud.programmer.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cloud.programmer.agent.dto.SkillDTO;
import com.cloud.programmer.agent.entity.AgentSkillRelationEntity;
import com.cloud.programmer.agent.entity.SkillEntity;
import com.cloud.programmer.agent.mapper.AgentSkillRelationMapper;
import com.cloud.programmer.agent.mapper.SkillMapper;
import com.cloud.programmer.agent.service.SkillService;
import com.cloud.programmer.contant.EnableContant;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author 周梓强 2026/10/14
 */

@Service
public class SkillServiceImpl implements SkillService {

    private final SkillMapper skillMapper;

    private final AgentSkillRelationMapper agentSkillRelationMapper;


    public SkillServiceImpl(SkillMapper skillMapper, AgentSkillRelationMapper agentSkillRelationMapper) {
        this.skillMapper = skillMapper;
        this.agentSkillRelationMapper = agentSkillRelationMapper;
    }


    @Override
    public void findSkillByAgentId(int agentId) {
        QueryWrapper<SkillEntity> skillEntityQueryWrapper = new QueryWrapper<>();
        skillMapper.selectList(skillEntityQueryWrapper.eq("agent_id", agentId));
    }

    @Override
    public List<SkillDTO> list(Map<String, Object> params) {
        QueryWrapper<SkillEntity> skillEntityQueryWrapper = new QueryWrapper<>();
        if (params.get("name") != null) {
            skillEntityQueryWrapper.like("name", params.get("name"));
        }
        skillEntityQueryWrapper.eq("status", EnableContant.ENABLE);
        List<SkillEntity> skillEntities = skillMapper.selectList(skillEntityQueryWrapper);
        List<SkillDTO> skillDTOList = new ArrayList<>();
        for (SkillEntity skillEntity : skillEntities) {
            SkillDTO skillDTO = convertEntityToSkillDTO(skillEntity);
            skillDTOList.add(skillDTO);
        }
        return skillDTOList;
    }

    @Override
    public SkillDTO insert(SkillDTO skillDTO) {
        SkillEntity skillEntity = new SkillEntity();
        skillEntity.setName(skillDTO.getName());
        skillEntity.setDescription(skillDTO.getDescription());
        skillEntity.setInstruction(skillDTO.getInstruction());
        skillEntity.setStatus(EnableContant.ENABLE);
        skillEntity.setCreateTime(new Date());
        skillEntity.setUpdateTime(new Date());
        skillMapper.insert(skillEntity);
        return convertEntityToSkillDTO(skillEntity);
    }

    @Override
    public SkillDTO update(SkillDTO skillDTO) {
        SkillEntity skillEntity = skillMapper.selectById(skillDTO.getId());
        if (skillEntity != null) {
            skillEntity.setName(skillDTO.getName());
            skillEntity.setDescription(skillDTO.getDescription());
            skillEntity.setInstruction(skillDTO.getInstruction());
            skillEntity.setUpdateTime(new Date());
            skillMapper.updateById(skillEntity);
            return convertEntityToSkillDTO(skillEntity);
        }
        return null;

    }

    @Override
    public Boolean delete(Integer id) {
        List<AgentSkillRelationEntity> agentSkillRelationEntities = agentSkillRelationMapper.selectList(new QueryWrapper<AgentSkillRelationEntity>().eq("skill_id", id));
        if (agentSkillRelationEntities != null && agentSkillRelationEntities.size() > 0) {
            return false;
        }
        return skillMapper.deleteById(id) > 0;
    }

    @Override
    public Boolean updataStatus(Integer id, Integer status) {

        if (!agentSkillRelationMapper.selectList(new QueryWrapper<AgentSkillRelationEntity>().eq("skill_id", id)).isEmpty()) {
            return false;
        }

        SkillEntity skillEntity = skillMapper.selectById(id);
        if (skillEntity != null) {
            skillEntity.setStatus(status);
            return skillMapper.updateById(skillEntity) > 0;
        }
        return false;
    }


    private SkillDTO convertEntityToSkillDTO(SkillEntity skillEntity) {
        SkillDTO skillDTO = new SkillDTO();
        skillDTO.setId(skillEntity.getId());
        skillDTO.setName(skillEntity.getName());
        skillDTO.setDescription(skillEntity.getDescription());
        skillDTO.setInstruction(skillEntity.getInstruction());
        skillDTO.setStatus(skillEntity.getStatus());
        return skillDTO;
    }
}
