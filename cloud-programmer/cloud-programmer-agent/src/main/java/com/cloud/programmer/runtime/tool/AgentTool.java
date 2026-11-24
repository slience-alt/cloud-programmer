package com.cloud.programmer.runtime.tool;


import java.util.Map;

/**
 * Agent 可调用的工具统一接口。
 */
public interface AgentTool {
    /**
     * 获取工具定义，供大模型识别工具用途和参数。
     */
    ToolDefinition getToolDefinition();

    /**
     * 执行工具。
     *
     * @param arguments 大模型生成的工具参数
     * @return 工具执行结果
     */
    String execute(Map<String, Object> arguments) throws Exception;
}
