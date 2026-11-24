package com.cloud.programmer.runtime.message;

import com.cloud.programmer.runtime.api.ModelResponse;
import com.cloud.programmer.runtime.tool.ToolCall;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author 周梓强 2026/10/9
 */

public class AgentMessage {

    /**
     * 消息角色：system、user、assistant、tool
     */
    private final String role;

    /**
     * 消息文本内容
     */
    private final String content;

    /**
     * 模型请求调用的工具列表
     */
    private final List<ToolCall> toolCalls;

    /**
     * 工具调用 ID，用于关联工具执行结果
     */
    private final String toolCallId;

    /**
     * 工具名称
     */
    private final String toolName;

    @JsonCreator
    public AgentMessage(
            @JsonProperty("role") String role,
            @JsonProperty("content") String content,
            @JsonProperty("toolCalls") List<ToolCall> toolCalls,
            @JsonProperty("toolCallId") String toolCallId,
            @JsonProperty("toolName") String toolName) {
        this.role = Objects.requireNonNull(role, "消息角色不能为空");
        if (!List.of("system", "user", "assistant", "tool").contains(role)) {
            throw new IllegalArgumentException("不支持的消息角色：" + role);
        }
        this.content = content;
        this.toolCalls = toolCalls == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(toolCalls));
        this.toolCallId = toolCallId;
        this.toolName = toolName;
    }

    /**
     * 创建系统消息
     */
    public static AgentMessage system(String content) {
        return new AgentMessage("system", content, null, null, null);
    }

    /**
     * 创建用户消息
     */
    public static AgentMessage user(String content) {
        return new AgentMessage("user", content, null, null, null);
    }

    /**
     * 创建模型回复消息
     */
    public static AgentMessage assistant(ModelResponse response) {
        Objects.requireNonNull(response, "模型响应不能为空");
        return new AgentMessage("assistant", response.getContent(), response.getToolCalls(), null, null);
    }

    /**
     * 创建工具执行结果消息
     */
    public static AgentMessage toolResult(String toolCallId, String toolName, String result) {

        Objects.requireNonNull(toolCallId, "工具调用 ID 不能为空");
        Objects.requireNonNull(toolName, "工具名称不能为空");

        return new AgentMessage("tool", result, null, toolCallId, toolName);
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public String getToolName() {
        return toolName;
    }
}
