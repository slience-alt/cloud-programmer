package com.cloud.programmer.runtime.tool;

/**
 * @author 周梓强 2026/10/9
 */

public class ToolDefinition {

    /**
     * 工具名称，例如 weather、web_search
     */
    private final String name;

    /**
     * 工具描述，帮助大模型判断什么时候使用该工具
     */
    private final String description;

    /**
     * 工具参数的 JSON Schema 字符串
     */
    private final String parametersJsonSchema;

    public ToolDefinition(
            String name,
            String description,
            String parametersJsonSchema) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("工具名称不能为空");
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("工具描述不能为空");
        }

        if (parametersJsonSchema == null
                || parametersJsonSchema.isBlank()) {
            throw new IllegalArgumentException("工具参数 Schema 不能为空");
        }

        this.name = name;
        this.description = description;
        this.parametersJsonSchema = parametersJsonSchema;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getParametersJsonSchema() {
        return parametersJsonSchema;
    }
}
