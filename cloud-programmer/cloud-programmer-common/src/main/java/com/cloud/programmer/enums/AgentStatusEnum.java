package com.cloud.programmer.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author 周梓强 2026/10/14
 */

@Getter
@AllArgsConstructor
public enum AgentStatusEnum {

    ONLINE("启用", 1),
    OFFLINE("禁用", 0);

    private String name;
    private Integer status;


}
