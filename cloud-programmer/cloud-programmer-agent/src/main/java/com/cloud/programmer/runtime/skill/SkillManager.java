package com.cloud.programmer.runtime.skill;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SkillManager {

    private final SkillLoader skillLoader;


    public SkillManager(SkillLoader skillLoader){
        this.skillLoader = skillLoader;
    }


    public List<Skill> getSkills(Long agentId){
        return skillLoader.load(agentId);
    }


}