package com.cloud.programmer.runtime.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cloud.programmer.runtime.entity.AgentRunEvent;
import com.cloud.programmer.runtime.mapper.AgentRunEventMapper;
import com.cloud.programmer.runtime.service.AgentRunEventService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class AgentRunEventServiceImpl implements AgentRunEventService {

    private final AgentRunEventMapper eventMapper;

    public AgentRunEventServiceImpl(AgentRunEventMapper eventMapper) {
        this.eventMapper = eventMapper;
    }

    @Override
    public Long record(String taskId, String eventType, String eventName, String inputContent, String outputContent, String status, Long durationMs) {

        AgentRunEvent event = new AgentRunEvent();
        event.setTaskId(taskId);
        event.setEventType(eventType);
        event.setEventName(eventName);
        event.setInputContent(inputContent);
        event.setOutputContent(outputContent);
        event.setStatus(status);
        event.setDurationMs(durationMs);
        event.setCreateTime(new Date());
        int rows = eventMapper.insert(event);
        if (rows != 1) {
            throw new IllegalStateException(
                    "保存 Agent 执行事件失败，taskId=" + taskId
            );
        }

        return event.getId();
    }

    @Override
    public List<AgentRunEvent> listAfterEventId(String taskId, Long lastEventId) {
        if (!StringUtils.hasText(taskId)) {
            return Collections.emptyList();
        }

        return eventMapper.selectList(
                Wrappers.<AgentRunEvent>lambdaQuery()
                        .eq(AgentRunEvent::getTaskId, taskId)
                        .gt(lastEventId != null && lastEventId > 0,
                                AgentRunEvent::getId, lastEventId)
                        .orderByAsc(AgentRunEvent::getId)
        );
    }

    @Override
    public Long recordTokenBatch(String taskId, String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        return record(taskId, "token", "模型输出", null, content, "SUCCESS", null);
    }
}