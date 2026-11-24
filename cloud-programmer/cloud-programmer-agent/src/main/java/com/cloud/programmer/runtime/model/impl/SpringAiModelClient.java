package com.cloud.programmer.runtime.model.impl;

import com.cloud.programmer.runtime.api.ModelResponse;
import com.cloud.programmer.runtime.message.AgentMessage;
import com.cloud.programmer.runtime.model.ModelClient;
import com.cloud.programmer.runtime.task.AgentTaskContext;
import com.cloud.programmer.runtime.tool.ToolCall;
import com.cloud.programmer.runtime.tool.ToolDefinition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 基于 Spring AI 的大模型客户端实现。
 * <p>
 * 负责：
 * 1. 将 AgentMessage 转换为 Spring AI 消息
 * 2. 将 ToolDefinition 转换为 Spring AI 工具定义
 * 3. 调用 ChatModel
 * 4. 将模型响应转换为 ModelResponse
 * <p>
 * 工具的实际执行由 Agent Runtime 自己负责。
 */
@Component
public class SpringAiModelClient implements ModelClient {

    private final ChatModel chatModel;

    private final ObjectMapper objectMapper;

    public SpringAiModelClient(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = Objects.requireNonNull(chatModel, "ChatModel 不能为空");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper 不能为空");
    }

    /**
     * 调用大模型。
     *
     * @param messages 当前对话上下文
     * @param tools    当前可用的工具定义
     * @return 模型响应
     */
    @Override
    public ModelResponse chat(List<AgentMessage> messages, List<ToolDefinition> tools) {
        Objects.requireNonNull(messages, "消息列表不能为空");
        // 一、转换对话消息
        List<Message> springMessages = new ArrayList<>();
        for (AgentMessage message : messages) {
            springMessages.add(toSpringMessage(message));
        }
        // 二、转换工具定义
        List<ToolCallback> callbacks = new ArrayList<>();
        if (tools != null) {
            for (ToolDefinition tool : tools) {
                callbacks.add(toToolCallback(tool));
            }
        }
        // 三、配置工具调用策略
        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(callbacks)
                // 关闭 Spring AI 自动执行工具
                .internalToolExecutionEnabled(false)
                .build();
        // 四、创建模型请求
        Prompt prompt = new Prompt(springMessages, options);
        // 五、调用大模型
        ChatResponse response = chatModel.call(prompt);
        if (response == null || response.getResult() == null) {
            throw new IllegalStateException("模型没有返回有效结果");
        }
        // 六、获取模型响应内容
        AssistantMessage output = response.getResult().getOutput();
        if (output == null) {
            throw new IllegalStateException("模型响应内容为空");
        }
        // 七、转换模型返回的工具调用
        List<ToolCall> toolCalls = new ArrayList<>();
        if (output.getToolCalls() != null) {
            for (AssistantMessage.ToolCall call : output.getToolCalls()) {
                toolCalls.add(toRuntimeToolCall(call));
            }
        }
        // 八、返回 Agent Runtime 自己的响应对象
        return new ModelResponse(output.getText(), toolCalls);
    }


    @Override
    public ModelResponse chatStream(List<AgentMessage> messages, List<ToolDefinition> tools, Consumer<String> onToken, AgentTaskContext context) {
        context.checkCancelled();
        Prompt prompt = buildPrompt(messages, tools);
        StringBuilder fullContent = new StringBuilder();
        List<ChatResponse> chunks = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        Disposable subscription = null;
        try {
            subscription = chatModel.stream(prompt)
                    .doOnNext(response -> {
                        context.checkCancelled();
                        if (response == null) {
                            return;
                        } else {
                            response.getResult();
                        }
                        chunks.add(response);

                        String text = response.getResult()
                                .getOutput()
                                .getText();

                        if (text != null && !text.isEmpty()) {
                            fullContent.append(text);
                            onToken.accept(text);
                        }
                    })
                    .doOnError(errorRef::set)
                    .doFinally(signalType -> latch.countDown())
                    .subscribe();

            context.setModelSubscription(subscription);

            latch.await();

            context.checkCancelled();

            Throwable error = errorRef.get();
            if (error != null) {
                throw new RuntimeException("模型流式调用失败", error);
            }

            return mergeStreamResponses(chunks, fullContent.toString());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("模型流式调用被中断");

        } finally {
            if (subscription != null) {
                context.clearModelSubscription(subscription);
                subscription.dispose();
            }
        }
    }


    /**
     * 将 AgentMessage 转换为 Spring AI Message。
     */
    private Message toSpringMessage(AgentMessage message) {
        Objects.requireNonNull(message, "消息不能为 null");
        String content = message.getContent() == null ? "" : message.getContent();
        switch (message.getRole()) {
            case "system":
                return new SystemMessage(content);
            case "user":
                return new UserMessage(content);
            case "assistant":
                List<AssistantMessage.ToolCall> springToolCalls = new ArrayList<>();
                if (message.getToolCalls() != null) {
                    for (ToolCall call : message.getToolCalls()) {
                        springToolCalls.add(new AssistantMessage.ToolCall(call.getId(), "function", call.getName(), writeJson(call.getArguments())));
                    }
                }
                return AssistantMessage.builder().content(content).properties(Map.of()).toolCalls(springToolCalls).build();
            case "tool":
                ToolResponseMessage.ToolResponse toolResponse = new ToolResponseMessage.ToolResponse(message.getToolCallId(), message.getToolName(), content);
                return ToolResponseMessage.builder().responses(List.of(toolResponse)).build();
            default:
                throw new IllegalArgumentException("不支持的消息角色：" + message.getRole());
        }
    }

    /**
     * 将自定义工具定义转换为 Spring AI ToolCallback。
     * <p>
     * 这里仅向模型提供工具描述，不在此处执行工具。
     */
    private ToolCallback toToolCallback(ToolDefinition definition) {
        Objects.requireNonNull(definition, "工具定义不能为空");
        org.springframework.ai.tool.definition.ToolDefinition springDefinition = org.springframework.ai.tool.definition.ToolDefinition.builder()
                .name(definition.getName())
                .description(definition.getDescription())
                .inputSchema(definition.getParametersJsonSchema())
                .build();

        return new ToolCallback() {
            @Override
            public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
                return springDefinition;
            }

            @Override
            public String call(String toolInput) {
                // internalToolExecutionEnabled(false) 时，
                // 工具执行由调用方负责，不应进入这里。
                throw new UnsupportedOperationException("工具应由 Agent Runtime 的 ToolExecutor 执行");
            }
        };
    }

    /**
     * 将 Spring AI 的工具调用转换为 Runtime 工具调用。
     */
    private ToolCall toRuntimeToolCall(AssistantMessage.ToolCall call) {
        try {
            Map<String, Object> arguments = objectMapper.readValue(call.arguments(), new TypeReference<Map<String, Object>>() {
            });
            return new ToolCall(call.id(), call.name(), arguments);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("解析工具参数失败，工具名称：" + call.name(), e);
        }
    }

    /**
     * 将对象转换为 JSON 字符串。
     */
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("工具参数序列化失败", e);
        }
    }

    /**
     * 构建 Spring AI Prompt。
     */
    private Prompt buildPrompt(List<AgentMessage> messages, List<ToolDefinition> tools) {
        Objects.requireNonNull(messages, "消息列表不能为空");
        // 一、转换对话消息
        List<Message> springMessages = new ArrayList<>();
        for (AgentMessage message : messages) {
            springMessages.add(toSpringMessage(message));
        }
        // 二、转换工具定义
        List<ToolCallback> callbacks = new ArrayList<>();
        if (tools != null) {
            for (ToolDefinition tool : tools) {
                callbacks.add(toToolCallback(tool));
            }
        }
        // 三、配置工具调用策略
        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(callbacks)
                .internalToolExecutionEnabled(false)
                .build();
        // 四、构建 Prompt
        return new Prompt(springMessages, options);
    }

    /**
     * 合并 Spring AI 流式响应。
     * <p>
     * 注意：工具参数按增量片段拼接。
     */
    private ModelResponse mergeStreamResponses(List<ChatResponse> chunks, String fullContent) {
        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalStateException("模型未返回有效响应");
        }
        // 按工具调用 ID 保存累积结果，保持原始顺序。
        Map<String, StreamToolCallBuilder> toolCallMap = new java.util.LinkedHashMap<>();
        String activeToolCallId = null;
        for (ChatResponse chunk : chunks) {
            if (chunk == null || chunk.getResult() == null) {
                continue;
            }
            AssistantMessage output = chunk.getResult().getOutput();
            if (output == null || output.getToolCalls() == null) {
                continue;
            }
            for (AssistantMessage.ToolCall call : output.getToolCalls()) {
                if (call == null) {
                    continue;
                }
                String id = call.id();
                String name = call.name();
                String arguments = call.arguments();
                // 流式响应的后续片段可能没有重复提供 ID。
                if (id == null || id.isBlank()) {
                    id = activeToolCallId;
                }
                // 没有可识别的工具调用 ID，无法可靠合并。
                if (id == null || id.isBlank()) {
                    throw new IllegalStateException("流式工具调用缺少 ID，无法合并参数");
                }
                activeToolCallId = id;
                StreamToolCallBuilder builder = toolCallMap.computeIfAbsent(id, key -> new StreamToolCallBuilder());
                // 名称通常只在工具调用的首个片段中出现。
                if (name != null && !name.isBlank()) {
                    if (builder.name != null && !builder.name.equals(name)) {
                        throw new IllegalStateException("同一工具调用出现不一致的名称：" + id);
                    }
                    builder.name = name;
                }

                // 参数可能分散在多个 chunk 中。
                if (arguments != null && !arguments.isEmpty()) {
                    builder.arguments.append(arguments);
                }
            }
        }

        // 将累积的工具调用转换为 Runtime ToolCall。
        List<ToolCall> toolCalls = new ArrayList<>();
        for (Map.Entry<String, StreamToolCallBuilder> entry : toolCallMap.entrySet()) {

            String id = entry.getKey();
            StreamToolCallBuilder builder = entry.getValue();
            if (builder.name == null || builder.name.isBlank()) {
                throw new IllegalStateException("工具调用缺少名称，工具调用 ID：" + id);
            }

            String argumentsJson = builder.arguments.toString();
            try {
                Map<String, Object> arguments;
                if (argumentsJson.isBlank()) {
                    arguments = new java.util.HashMap<>();
                } else {
                    arguments = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
                }
                toolCalls.add(new ToolCall(id, builder.name, arguments));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("合并流式工具参数失败，工具名称：" + builder.name + "，参数：" + argumentsJson, e);
            }
        }
        return new ModelResponse(fullContent, toolCalls);
    }

    /**
     * 暂存一个工具调用的流式片段。
     */
    private static class StreamToolCallBuilder {
        private String name;
        private final StringBuilder arguments = new StringBuilder();
    }
}