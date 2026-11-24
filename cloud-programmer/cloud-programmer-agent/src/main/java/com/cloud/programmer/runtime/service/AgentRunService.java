package com.cloud.programmer.runtime.service;

import com.cloud.programmer.runtime.dto.AgentRunDetailDTO;
import com.cloud.programmer.runtime.entity.AgentRun;

public interface AgentRunService {

    /**
     * 创建待执行记录
     */
    void createPending(String taskId, String conversationId, Long agentId, String input, Long userId);
    /**
     * 标记任务开始执行
     */
    void markRunning(String taskId);

    /**
     * 标记任务执行成功
     */
    void markSuccess(String taskId, String result);

    /**
     * 标记任务执行失败
     */
    void markFailed(String taskId, String errorMessage);

    AgentRunDetailDTO getDetailByTaskId(String taskId);

    /**
     * 标记任务已取消
     */
    void markCancelled(String taskId, String reason);

    AgentRun getOwnedRun(String taskId, Long userId);
}