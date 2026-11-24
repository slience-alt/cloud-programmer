package com.cloud.programmer.runtime.api;

import com.cloud.programmer.runtime.tool.ToolCall;

import java.util.List;

/**
 * @author 周梓强 2026/10/9
 */

public class ModelResponse {

    private String content;
    private List<ToolCall> toolCalls;

    public ModelResponse(String content, List<ToolCall> toolCalls) {
        this.content = content;
        this.toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);;
    }

    public String getContent() {
        return content;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }
}
