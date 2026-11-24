package com.cloud.programmer.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author 周梓强 2026/10/21
 */

@AllArgsConstructor
@Getter
public enum AgentRoleEnum {

    SYSTEM(0, "系统消息", "system"),
    USER(1, "用户消息", "user"),
    TOOL(2, "工具返回消息", "tool"),
    ASSISTANT(3, "大模型返回消息", "assistant"),
    UNKNOWN(99, "未知类型", "unknown");

    private int code;
    private String name;
    private String type;

    public static String getTypeByCode(int code) {
        for (AgentRoleEnum role : AgentRoleEnum.values()) {
            if (role.getCode() == code) {
                return role.getType();
            }
        }
        return UNKNOWN.getType();
    }

    public static Integer getCodeByType(String type) {
        for (AgentRoleEnum role : AgentRoleEnum.values()) {
            if (role.getType().equals(type)) {
                return role.getCode();
            }
        }
        return UNKNOWN.getCode();
    }

}
