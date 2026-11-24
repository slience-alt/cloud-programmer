package com.cloud.programmer.runtime.agent;

import com.cloud.programmer.runtime.client.AdminAgentClient;
import com.cloud.programmer.runtime.skill.Skill;
import com.cloud.programmer.runtime.skill.SkillManager;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatabaseAgentConfigLoader implements AgentConfigLoader {


    private final AdminAgentClient adminAgentClient;

    private final SkillManager skillManager;



    public DatabaseAgentConfigLoader(AdminAgentClient adminAgentClient, SkillManager skillManager){
        this.adminAgentClient = adminAgentClient;
        this.skillManager = skillManager;
    }



    @Override
    public AgentDefinition load(Long agentId, Long userId) {
        // Admin 服务会再次校验 Agent 所属用户
        AgentDTO entity = adminAgentClient.findById(agentId, userId);
        if (entity == null) {
            throw new IllegalStateException("Agent 不存在或无权访问");
        }
        AgentDefinition definition = new AgentDefinition();
        definition.setId(entity.getId());
        definition.setName(entity.getName());
        definition.setSystemPrompt(entity.getSystemPrompt());
        definition.setModelName(entity.getModelName());
        List<Skill> skills = skillManager.getSkills(agentId);
        definition.setSkills(skills);
        definition.setMcpIds(entity.getMcpIds());

        return definition;
    }
}