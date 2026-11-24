package com.cloud.programmer.runtime.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cloud.programmer.runtime.dto.AgentRunDetailDTO;
import com.cloud.programmer.runtime.dto.AgentRunEventDTO;
import com.cloud.programmer.runtime.entity.AgentRun;
import com.cloud.programmer.runtime.entity.AgentRunEvent;
import com.cloud.programmer.runtime.mapper.AgentRunEventMapper;
import com.cloud.programmer.runtime.mapper.AgentRunMapper;
import com.cloud.programmer.runtime.service.AgentRunService;
import com.cloud.programmer.security.context.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AgentRunServiceImpl implements AgentRunService {

    private static final String PENDING = "PENDING";
    private static final String RUNNING = "RUNNING";
    private static final String SUCCESS = "SUCCESS";
    private static final String FAILED = "FAILED";
    private static final String CANCELLED = "CANCELLED";

    private final AgentRunMapper agentRunMapper;
    private final AgentRunEventMapper agentRunEventMapper;

    public AgentRunServiceImpl(AgentRunMapper agentRunMapper, AgentRunEventMapper agentRunEventMapper) {
        this.agentRunMapper = agentRunMapper;
        this.agentRunEventMapper = agentRunEventMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPending(String taskId, String conversationId, Long agentId, String input, Long userId) {
        AgentRun run = new AgentRun();
        run.setTaskId(taskId);
        run.setConversationId(conversationId);
        run.setAgentId(agentId);
        run.setInputText(input);
        run.setUserId(userId);
        run.setStatus(PENDING);
        Date now = new Date();
        run.setCreateTime(now);
        run.setUpdateTime(now);
        int rows = agentRunMapper.insert(run);
        if (rows != 1) {
            throw new IllegalStateException(
                    "创建 Agent 执行记录失败，taskId=" + taskId
            );
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRunning(String taskId) {
        Date now = new Date();

        int rows = agentRunMapper.update(
                null,
                Wrappers.<AgentRun>lambdaUpdate()
                        .eq(AgentRun::getTaskId, taskId)
                        .eq(AgentRun::getStatus, PENDING)
                        .set(AgentRun::getStatus, RUNNING)
                        .set(AgentRun::getStartTime, now)
                        .set(AgentRun::getUpdateTime, now)
        );

        if (rows != 1) {
            throw new IllegalStateException(
                    "更新 Agent 执行状态失败，任务不存在或状态不是 PENDING，taskId=" + taskId
            );
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markSuccess(String taskId, String result) {
        finish(taskId, SUCCESS, result, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markFailed(String taskId, String errorMessage) {
        finish(taskId, FAILED, null, errorMessage);
    }

    private void finish(String taskId, String targetStatus, String result, String errorMessage) {

        AgentRun run = agentRunMapper.selectOne(
                Wrappers.<AgentRun>lambdaQuery()
                        .eq(AgentRun::getTaskId, taskId)
                        .last("LIMIT 1")
        );

        if (run == null) {
            throw new IllegalStateException("Agent 执行记录不存在，taskId=" + taskId);
        }

        // 已经处于终态，不再重复更新
        if (isTerminalStatus(run.getStatus())) {
            return;
        }

        Date now = new Date();
        Date startTime = run.getStartTime();

        Long durationMs = startTime == null ? 0L : Math.max(0L, Duration.between(startTime.toInstant(), now.toInstant()).toMillis());

        int rows = agentRunMapper.update(
                null,
                Wrappers.<AgentRun>lambdaUpdate()
                        .eq(AgentRun::getTaskId, taskId)
                        .in(AgentRun::getStatus, PENDING, RUNNING)
                        .set(AgentRun::getStatus, targetStatus)
                        .set(AgentRun::getResultText, result)
                        .set(AgentRun::getErrorMessage, errorMessage)
                        .set(AgentRun::getEndTime, now)
                        .set(AgentRun::getDurationMs, durationMs)
                        .set(AgentRun::getUpdateTime, now)
        );

        if (rows != 1) {
            // 可能有其他线程抢先完成了状态更新
            AgentRun latest = agentRunMapper.selectOne(
                    Wrappers.<AgentRun>lambdaQuery()
                            .eq(AgentRun::getTaskId, taskId)
                            .last("LIMIT 1")
            );

            if (latest != null && isTerminalStatus(latest.getStatus())) {
                return;
            }

            throw new IllegalStateException(
                    "结束 Agent 执行记录失败，taskId=" + taskId
            );
        }
    }


    @Override
    public AgentRunDetailDTO getDetailByTaskId(String taskId) {
        if (!StringUtils.hasText(taskId)) {
            throw new IllegalArgumentException("taskId不能为空");
        }

        // 1. 查询任务执行记录
        AgentRun run = agentRunMapper.selectOne(
                Wrappers.<AgentRun>lambdaQuery()
                        .eq(AgentRun::getTaskId, taskId)
                        .eq(AgentRun::getUserId, SecurityUtils.getCurrentUserId())
        );

        if (run == null) {
            return null;
        }

        // 2. 查询该任务的所有执行事件，按发生时间升序排列
        List<AgentRunEvent> eventList = agentRunEventMapper.selectList(
                Wrappers.<AgentRunEvent>lambdaQuery()
                        .eq(AgentRunEvent::getTaskId, taskId)
                        .orderByAsc(AgentRunEvent::getCreateTime)
                        .orderByAsc(AgentRunEvent::getId)
        );

        // 3. 转换事件 DTO
        List<AgentRunEventDTO> events = eventList.stream()
                .map(event -> {
                    AgentRunEventDTO dto = new AgentRunEventDTO();
                    dto.setId(event.getId());
                    dto.setEventType(event.getEventType());
                    dto.setEventName(event.getEventName());
                    dto.setInputContent(event.getInputContent());
                    dto.setOutputContent(event.getOutputContent());
                    dto.setStatus(event.getStatus());
                    dto.setDurationMs(event.getDurationMs());
                    dto.setCreateTime(event.getCreateTime());
                    return dto;
                })
                .collect(Collectors.toList());

        // 4. 组装执行详情
        AgentRunDetailDTO detail = new AgentRunDetailDTO();
        detail.setTaskId(run.getTaskId());
        detail.setAgentId(run.getAgentId());
        detail.setConversationId(run.getConversationId());
        detail.setStatus(run.getStatus());
        detail.setInputText(run.getInputText());
        detail.setResultText(run.getResultText());
        detail.setErrorMessage(run.getErrorMessage());
        detail.setStartTime(run.getStartTime());
        detail.setEndTime(run.getEndTime());
        detail.setDurationMs(run.getDurationMs());
        detail.setCreateTime(run.getCreateTime());
        detail.setEvents(events);
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markCancelled(String taskId, String reason) {
        finish(taskId, CANCELLED, null, reason);
    }

    @Override
    public AgentRun getOwnedRun(String taskId, Long userId) {
        if (!StringUtils.hasText(taskId) || userId == null) {
            return null;
        }

        return agentRunMapper.selectOne(
                Wrappers.<AgentRun>lambdaQuery()
                        .eq(AgentRun::getTaskId, taskId)
                        .eq(AgentRun::getUserId, userId)
                        .last("LIMIT 1")
        );
    }

    private boolean isTerminalStatus(String status) {
        return SUCCESS.equals(status)
                || FAILED.equals(status)
                || CANCELLED.equals(status);
    }
}

