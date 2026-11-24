package com.cloud.programmer.runtime.skill;

import java.util.List;

public interface SkillLoader {

    List<Skill> load(Long agentId);

}