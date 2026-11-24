package com.cloud.programmer.runtime.tool;


import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WeatherTool implements AgentTool {

    private static final ToolDefinition TOOL_DEFINITION =
            new ToolDefinition(
                    "weather_query",
                    "查询指定城市的天气信息。当用户询问天气时调用此工具。",
                    """
                    {
                      "type": "object",
                      "properties": {
                        "city": {
                          "type": "string",
                          "description": "需要查询天气的城市名称，例如北京、上海"
                        }
                      },
                      "required": ["city"],
                      "additionalProperties": false
                    }
                    """
            );

    @Override
    public ToolDefinition getToolDefinition() {
        return TOOL_DEFINITION;
    }

    @Override
    public String execute(Map<String, Object> arguments)
            throws Exception {

        // 1. 获取城市参数
        Object cityValue = arguments.get("city");

        if (cityValue == null
                || cityValue.toString().isBlank()) {
            throw new IllegalArgumentException("城市名称不能为空");
        }

        String city = cityValue.toString().trim();

        // 2. 第一版返回模拟数据
        return "城市：" + city
                + "；天气：晴转多云"
                + "；气温：25-31℃"
                + "；数据来源：模拟数据，仅用于测试";
    }
}
