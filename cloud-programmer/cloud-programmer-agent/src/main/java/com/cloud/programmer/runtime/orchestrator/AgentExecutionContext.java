
package com.cloud.programmer.runtime.orchestrator;

import com.cloud.programmer.runtime.agent.AgentDefinition;
import com.cloud.programmer.runtime.tool.AgentTool;
import com.cloud.programmer.runtime.tool.ToolDefinition;
import lombok.Data;

import java.util.List;

/**
 * Agent 单次任务执行上下文。
 *
 * 保存本次任务可使用的工具，确保工具定义和工具执行使用同一份集合。
 */
@Data
public class AgentExecutionContext {

    private final AgentDefinition agentDefinition;


    private final String taskId;

    private final List<AgentTool> tools;

    private final List<ToolDefinition> toolDefinitions;

    public AgentExecutionContext(AgentDefinition agentDefinition, String taskId, List<AgentTool> tools) {
        this.agentDefinition = agentDefinition;
        this.taskId = taskId;
        this.tools = tools == null ? List.of() : List.copyOf(tools);
        this.toolDefinitions = this.tools.stream().map(AgentTool::getToolDefinition).toList();
    }


}
