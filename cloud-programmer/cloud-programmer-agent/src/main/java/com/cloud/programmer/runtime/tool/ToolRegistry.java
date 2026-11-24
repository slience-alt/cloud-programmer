
package com.cloud.programmer.runtime.tool;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Agent 工具注册表。
 */
@Component
public class ToolRegistry {

    /**
     * key：工具名称
     * value：工具实例
     */
    private final Map<String, AgentTool> tools = new LinkedHashMap<>();

    /**
     * 注册工具。
     */
    public synchronized void register(AgentTool tool) {
        Objects.requireNonNull(tool, "工具不能为空");
        ToolDefinition definition = Objects.requireNonNull(tool.getToolDefinition(), "工具定义不能为空");
        String name = definition.getName();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("工具名称不能为空");
        }
        if (tools.containsKey(name)) {
            throw new IllegalArgumentException("工具已经注册，不能重复注册：" + name);
        }
        tools.put(name, tool);
    }

    /**
     * 根据名称查找工具。
     */
    public synchronized AgentTool getTool(String name) {
        AgentTool tool = tools.get(name);
        if (tool == null) {
            throw new IllegalArgumentException("未找到已注册的工具：" + name);
        }
        return tool;
    }

    /**
     * 移除指定工具。
     *
     * 用于 MCP Server 停用、删除或工具刷新。
     */
    public synchronized boolean unregister(String name) {
        return tools.remove(name) != null;
    }

    /**
     * 批量移除工具。
     */
    public synchronized void unregisterAll(
            Collection<String> names) {

        if (names == null || names.isEmpty()) {
            return;
        }

        for (String name : names) {
            tools.remove(name);
        }
    }

    /**
     * 获取全部工具定义，供大模型使用。
     */
    public synchronized List<ToolDefinition> getToolDefinitions() {
        List<ToolDefinition> definitions = new ArrayList<>();

        for (AgentTool tool : tools.values()) {
            definitions.add(tool.getToolDefinition());
        }

        return Collections.unmodifiableList(definitions);
    }

    /**
     * 判断工具是否已经注册。
     */
    public synchronized boolean contains(String name) {
        return tools.containsKey(name);
    }

    /**
     * 获取当前注册的工具数量。
     */
    public synchronized int size() {
        return tools.size();
    }
}
