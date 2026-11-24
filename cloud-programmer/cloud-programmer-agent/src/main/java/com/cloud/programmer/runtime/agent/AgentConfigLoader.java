package com.cloud.programmer.runtime.agent;


public interface AgentConfigLoader {


    /**
     * 根据 Agent ID 和用户 ID 加载 Agent 配置。
     */
    AgentDefinition load(Long agentId, Long userId);

}