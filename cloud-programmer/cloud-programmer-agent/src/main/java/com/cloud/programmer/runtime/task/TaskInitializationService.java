package com.cloud.programmer.runtime.task;

import com.cloud.programmer.runtime.memory.ConversationMemory;
import com.cloud.programmer.runtime.service.AgentRunService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskInitializationService {

    private final ConversationMemory conversationMemory;
    private final AgentRunService agentRunService;

    public TaskInitializationService(ConversationMemory conversationMemory, AgentRunService agentRunService) {
        this.conversationMemory = conversationMemory;
        this.agentRunService = agentRunService;
    }

    /**
     * 初始化 Agent 任务。
     * 会话绑定和任务记录创建必须在同一个事务中完成。
     */
    @Transactional(rollbackFor = Exception.class)
    public void initializeTask(String taskId, String conversationId, Long agentId, String input, Long userId) {

        // 1. 创建或校验会话与 Agent 的绑定关系
        conversationMemory.bindAgent(conversationId, agentId);

        // 2. 创建 Agent 执行记录
        agentRunService.createPending(taskId, conversationId, agentId, input, userId);
    }
}