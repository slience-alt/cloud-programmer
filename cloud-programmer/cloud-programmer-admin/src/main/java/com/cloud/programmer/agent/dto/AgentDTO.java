package com.cloud.programmer.agent.dto;

import lombok.Data;

import java.util.List;

@Data
public class AgentDTO {
    
    private Integer id;
    
    private String name;

    private String description;
    
    private String systemPrompt;
    
    private String modelName;

    private Integer status;

    private List<Integer> skillIds;

    private List<Integer> mcpIds;
    
}