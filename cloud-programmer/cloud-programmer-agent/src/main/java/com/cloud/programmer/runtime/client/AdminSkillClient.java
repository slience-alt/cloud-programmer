package com.cloud.programmer.runtime.client;

import com.cloud.programmer.runtime.skill.Skill;

import java.util.List;

public interface AdminSkillClient {

    List<Skill> findSkillByAgentId(Long agentId);
}