package com.cloud.programmer.runtime.tool;

import com.cloud.programmer.runtime.agent.AgentDefinition;

import java.util.List;

public interface AgentToolProvider {


    List<AgentTool> getTools(AgentDefinition definition);
}