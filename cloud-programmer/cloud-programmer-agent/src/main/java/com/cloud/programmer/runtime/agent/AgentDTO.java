package com.cloud.programmer.runtime.agent;

import lombok.Data;

import java.util.List;

/**
 * @author 周梓强 2026/10/13
 */

@Data
public class AgentDTO {

    private Integer id;

    private String name;

    private String description;

    private String systemPrompt;

    private String modelName;

    private List<Integer> mcpIds;

}
