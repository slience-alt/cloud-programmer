package com.cloud.programmer.runtime.skill;

import com.cloud.programmer.runtime.client.AdminSkillClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FileSkillLoader implements SkillLoader {

    private final AdminSkillClient adminSkillClient;

    public FileSkillLoader(AdminSkillClient adminSkillClient) {
        this.adminSkillClient = adminSkillClient;
    }


    @Override
    public List<Skill> load(Long agentId) {
        return adminSkillClient.findSkillByAgentId(agentId);
    }
}