package com.cloud.programmer.agent.dto;

import lombok.Data;

import java.util.List;

/**
 * @author 周梓强 2026/10/26
 */

@Data
public class McpDTO {

    private Long id;
    private String name;
    private String description;
    private String transportType;
    private Boolean enabled;
    private String serverUrl;
    private String command;
    private String argsJson;
    private String envJson;
    private String headersJson;
    private String remark;

}
