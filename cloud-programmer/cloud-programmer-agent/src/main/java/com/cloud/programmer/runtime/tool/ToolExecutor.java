package com.cloud.programmer.runtime.tool;

import com.cloud.programmer.runtime.agent.AgentDefinition;
import com.cloud.programmer.runtime.orchestrator.AgentExecutionContext;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Agent 工具执行器。
 */
@Component
public class ToolExecutor {

    private final AgentToolProvider agentToolProvider;

    public ToolExecutor(AgentToolProvider agentToolProvider) {
        this.agentToolProvider = Objects.requireNonNull(agentToolProvider, "AgentToolProvider 不能为空");
    }

    /**
     * 创建本次 Agent 任务的工具上下文。
     * 每次任务只获取一次工具集合。
     */
    public AgentExecutionContext createContext(AgentDefinition definition, String taskId) {
        List<AgentTool> tools = agentToolProvider.getTools(definition);
        return new AgentExecutionContext(definition, taskId, tools);
    }

    /**
     * 获取本次任务的工具定义。
     */
    public List<ToolDefinition> getToolDefinitions(AgentExecutionContext context) {
        return context.getToolDefinitions();
    }

    /**
     * 使用本次任务上下文执行工具。
     */
    public String execute(AgentExecutionContext context, String toolName, Map<String, Object> arguments) throws Exception {

        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("工具名称不能为空");
        }

        Map<String, Object> safeArguments = arguments == null ? Map.of() : new LinkedHashMap<>(arguments);

        AgentTool tool = context.getTools()
                .stream()
                .filter(item -> toolName.equals(item.getToolDefinition().getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("当前 Agent 无权使用该工具：" + toolName));
        return tool.execute(safeArguments);
    }

}
