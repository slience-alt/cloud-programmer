package com.cloud.programmer.runtime.tool;

import java.util.Map;

/**
 * 工具调用信息
 *
 * @author 周梓强 2026/10/9
 */
public class ToolCall {

    private String id;
    private String name;
    private Map<String, Object> arguments;

    /**
     * Jackson 反序列化使用
     */
    public ToolCall() {
    }

    public ToolCall(String id, String name, Map<String, Object> arguments) {
        this.id = id;
        this.name = name;
        this.arguments = arguments;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }
}
