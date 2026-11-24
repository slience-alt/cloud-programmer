package com.cloud.programmer.runtime.agent;


import com.cloud.programmer.runtime.skill.Skill;
import lombok.Data;

import java.util.List;

@Data
public class AgentDefinition {

    /**
     * Agent ID
     */
    private Integer id;
    /**
     * Agent名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * skill
     */
    private List<Skill> skills;

    /**
     * 使用模型
     */
    private String modelName;

    /**
     * MCP
     */
    private List<Integer> mcpIds;

}