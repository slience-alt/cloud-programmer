package com.cloud.programmer.runtime.orchestrator;

import com.cloud.programmer.runtime.agent.AgentDefinition;
import com.cloud.programmer.runtime.api.ModelResponse;
import com.cloud.programmer.runtime.message.AgentMessage;
import com.cloud.programmer.runtime.model.ModelClient;
import com.cloud.programmer.runtime.service.AgentRunEventService;
import com.cloud.programmer.runtime.stream.AgentStreamEvent;
import com.cloud.programmer.runtime.task.AgentTaskContext;
import com.cloud.programmer.runtime.tool.ToolCall;
import com.cloud.programmer.runtime.tool.ToolDefinition;
import com.cloud.programmer.runtime.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@Slf4j
public class AgentExecutionLoop {

    private static final int MAX_ITERATIONS = 10;

    private static final String SUCCESS = "SUCCESS";
    private static final String FAILED = "FAILED";

    private final ModelClient modelClient;
    private final ToolExecutor toolExecutor;
    private final AgentRunEventService agentRunEventService;

    public AgentExecutionLoop(ModelClient modelClient, ToolExecutor toolExecutor, AgentRunEventService agentRunEventService) {
        this.modelClient = modelClient;
        this.toolExecutor = toolExecutor;
        this.agentRunEventService = agentRunEventService;
    }

    /**
     * 非流式执行
     */

    public String execute(List<AgentMessage> messages, String taskId, AgentDefinition definition, AgentTaskContext taskContext) {

        // 每次任务只创建一次上下文
        AgentExecutionContext context = toolExecutor.createContext(definition, taskId);
        List<ToolDefinition> tools = toolExecutor.getToolDefinitions(context);
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            taskContext.checkCancelled();
            ModelResponse response = callModel(messages, tools, taskId, false, null, taskContext);
            taskContext.checkCancelled();
            messages.add(AgentMessage.assistant(response));
            // 没有工具调用，返回最终回答
            if (response.getToolCalls() == null || response.getToolCalls().isEmpty()) {
                return response.getContent();
            }
            executeTools(messages, response.getToolCalls(), taskId, context, taskContext);
        }
        taskContext.checkCancelled();
        throw new RuntimeException("Agent执行超限");
    }


    /**
     * 流式执行
     */

    public String executeStream(List<AgentMessage> messages, Consumer<AgentStreamEvent> consumer, String taskId, AgentDefinition definition, AgentTaskContext taskContext) {

        // 每次任务只创建一次上下文
        AgentExecutionContext context = toolExecutor.createContext(definition, taskId);

        List<ToolDefinition> tools = toolExecutor.getToolDefinitions(context);

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            taskContext.checkCancelled();
            StringBuilder answer = new StringBuilder();

            ModelResponse response = callModel(messages, tools, taskId, true,
                    token -> {
                        answer.append(token);
                        consumer.accept(new AgentStreamEvent("token", token));
                    }, taskContext
            );

            taskContext.checkCancelled();
            messages.add(AgentMessage.assistant(response));

            // 没有工具调用，返回最终回答
            if (response.getToolCalls() == null || response.getToolCalls().isEmpty()) {
                return answer.toString();
            }

            executeToolsStream(messages, response.getToolCalls(), consumer, taskId, context, taskContext);
        }

        taskContext.checkCancelled();
        throw new RuntimeException("超过最大执行次数");
    }

    /**
     * 调用大模型，并记录调用事件
     */
    private ModelResponse callModel(List<AgentMessage> messages, List<ToolDefinition> tools, String taskId, boolean stream, Consumer<String> tokenConsumer, AgentTaskContext taskContext) {
        taskContext.checkCancelled();
        long startNanos = System.nanoTime();
        try {
            ModelResponse response;
            if (stream) {
                response = modelClient.chatStream(messages, tools, tokenConsumer, taskContext);
            } else {
                response = modelClient.chat(messages, tools);
            }

            taskContext.checkCancelled();
            long durationMs = elapsedMillis(startNanos);
            recordEventSafely(taskId, "MODEL_CALL", stream ? "chatStream" : "chat", "消息数量：" + messages.size(), response.getContent(), SUCCESS, durationMs);
            return response;

        } catch (Exception e) {
            long durationMs = elapsedMillis(startNanos);
            recordEventSafely(taskId, "ERROR", stream ? "chatStream" : "chat", "消息数量：" + messages.size(), e.getMessage(), FAILED, durationMs);
            throw e;
        }
    }

    /**
     * 非流式工具执行
     */
    private void executeTools(List<AgentMessage> messages, List<ToolCall> toolCalls, String taskId, AgentExecutionContext context, AgentTaskContext taskContext) {

        for (ToolCall toolCall : toolCalls) {

            taskContext.checkCancelled();
            if (toolCall == null || toolCall.getName() == null || toolCall.getName().isBlank()) {
                continue;
            }

            String toolName = toolCall.getName();
            Map<String, Object> arguments = toolCall.getArguments() == null ? Map.of() : toolCall.getArguments();
            long startNanos = System.nanoTime();
            String result;
            String status;
            try {
                result = toolExecutor.execute(context, toolName, arguments);
                taskContext.checkCancelled();
                if (result == null) {
                    result = "";
                }
                status = SUCCESS;
            } catch (Exception e) {
                result = "工具执行失败，工具:" + toolName + ",原因:" + e.getMessage();
                status = FAILED;
                log.error("工具执行失败，taskId={}, toolName={}", taskId, toolName, e);
            }

            recordEventSafely(taskId, status.equals(SUCCESS) ? "TOOL_CALL" : "ERROR", toolName, String.valueOf(arguments), result, status, elapsedMillis(startNanos)
            );

            // 工具结果加入上下文
            messages.add(AgentMessage.toolResult(toolCall.getId(), toolName, result));
        }
    }

    /**
     * 流式工具执行
     */
    private void executeToolsStream(List<AgentMessage> messages, List<ToolCall> toolCalls, Consumer<AgentStreamEvent> consumer, String taskId, AgentExecutionContext context, AgentTaskContext taskContext) {
        for (ToolCall toolCall : toolCalls) {

            taskContext.checkCancelled();
            if (toolCall == null || toolCall.getName() == null || toolCall.getName().isBlank()) {
                continue;
            }

            String toolName = toolCall.getName();
            Map<String, Object> arguments = toolCall.getArguments() == null ? Map.of() : toolCall.getArguments();
            consumer.accept(new AgentStreamEvent("tool_start", toolName));
            long startNanos = System.nanoTime();
            String result;
            String status;
            try {
                taskContext.checkCancelled();
                result = toolExecutor.execute(context, toolName, arguments);
                if (result == null) {
                    result = "";
                }
                status = SUCCESS;

            } catch (Exception e) {
                result = "工具执行失败:" + toolName + ",原因:" + e.getMessage();
                status = FAILED;
                log.error("流式工具执行失败，taskId={}, toolName={}", taskId, toolName, e);
            }

            recordEventSafely(taskId, status.equals(SUCCESS) ? "TOOL_CALL" : "ERROR", toolName, String.valueOf(arguments), result, status, elapsedMillis(startNanos));

            // 工具结果加入上下文
            messages.add(AgentMessage.toolResult(toolCall.getId(), toolName, result)
            );
            taskContext.checkCancelled();
            // 通知前端工具执行结果
            if (SUCCESS.equals(status)) {
                consumer.accept(new AgentStreamEvent("tool_result", result));
            } else {
                consumer.accept(new AgentStreamEvent("tool_error", result));
            }
        }
    }

    /**
     * 安全记录事件，避免事件日志写入失败影响 Agent 主流程
     */
    private void recordEventSafely(String taskId, String eventType, String eventName, String inputContent, String outputContent, String status, Long durationMs) {

        try {
            agentRunEventService.record(taskId, eventType, eventName, inputContent, outputContent, status, durationMs);
        } catch (Exception e) {
            log.error("记录 Agent 执行事件失败，taskId={}, eventType={}, eventName={}", taskId, eventType, eventName, e);
        }
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}