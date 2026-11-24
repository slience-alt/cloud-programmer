package com.cloud.programmer.runtime.client;

import com.cloud.programmer.runtime.agent.AgentDTO;

public interface AdminAgentClient {

    AgentDTO findById(Long agentId, Long userId);
}