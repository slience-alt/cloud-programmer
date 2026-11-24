package com.cloud.programmer.runtime.config;

import com.cloud.programmer.runtime.tool.AgentTool;
import com.cloud.programmer.runtime.tool.ToolRegistry;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ToolConfiguration {

    @Bean
    public SmartInitializingSingleton registerAgentTools(ToolRegistry toolRegistry, List<AgentTool> agentTools) {
        return () -> {
            for (AgentTool tool : agentTools) {
                toolRegistry.register(tool);
            }
        };
    }
}