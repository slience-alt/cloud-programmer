package com.cloud.programmer.runtime.service;

import com.cloud.programmer.runtime.entity.AgentRunEvent;

import java.util.List;

public interface AgentRunEventService {

    /**
     * 记录一次执行事件
     */
    Long record(String taskId, String eventType, String eventName, String inputContent, String outputContent, String status, Long durationMs);
    List<AgentRunEvent> listAfterEventId(String taskId, Long lastEventId);


    /**
     * 批量记录模型输出内容（一个批次对应一条数据库记录）
     */
    Long recordTokenBatch(String taskId, String content);
}