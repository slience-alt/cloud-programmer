package com.cloud.programmer.runtime.model;

import com.cloud.programmer.runtime.api.ModelResponse;
import com.cloud.programmer.runtime.message.AgentMessage;
import com.cloud.programmer.runtime.task.AgentTaskContext;
import com.cloud.programmer.runtime.tool.ToolDefinition;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author 周梓强 2026/10/9
 */

public interface ModelClient {

    /**
     * 调用大模型。
     *
     * @param messages 当前对话消息
     * @param tools    当前可用的工具定义
     * @return 模型响应，可能包含文本或工具调用
     */
    ModelResponse chat(List<AgentMessage> messages, List<ToolDefinition> tools);

    /**
     * 流式调用模型。
     *
     * @param messages 当前对话消息
     * @param tools 当前可用工具
     * @param onToken 收到文本片段时的回调
     * @return 本轮完整模型响应，包含可能的工具调用
     */
    ModelResponse chatStream(List<AgentMessage> messages, List<ToolDefinition> tools, Consumer<String> onToken, AgentTaskContext context);
}
