package com.cloud.programmer.runtime.api;

import com.cloud.programmer.runtime.task.AgentTask;
import com.cloud.programmer.runtime.task.AgentTaskService;
import com.cloud.programmer.security.context.SecurityUtils;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/agent/tasks")
public class AgentTaskController {

    private final AgentTaskService agentTaskService;

    public AgentTaskController(AgentTaskService agentTaskService) {
        this.agentTaskService = agentTaskService;
    }

    /**
     * 提交 Agent 任务。
     */
    @PostMapping
    public ResponseEntity<AgentTaskResponse> submit(@RequestBody(required = false) AgentChatRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            AgentTask task = agentTaskService.submit(request.getConversationId(), request.getMessage().trim(), request.getAgentId());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(AgentTaskResponse.from(task));
        } catch (TaskRejectedException e) {
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .build();
        }
    }

    @PostMapping("/submitStream")
    public ResponseEntity<AgentTaskResponse> submitStream(@RequestBody(required = false) AgentChatRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            AgentTask task = agentTaskService.submitStream(request.getConversationId(), request.getMessage(), request.getAgentId());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(AgentTaskResponse.from(task));
        } catch (TaskRejectedException e) {
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .build();
        }
    }

    @GetMapping(value = "/{taskId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable("taskId") String taskId, @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        Long eventId = null;
        if (lastEventId != null && !lastEventId.isBlank()) {
            try {
                eventId = Long.parseLong(lastEventId);
                if (eventId < 0) {
                    throw new NumberFormatException("事件ID不能为负数");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Last-Event-ID 格式不正确");
            }
        }

        return agentTaskService.connect(taskId, SecurityUtils.getCurrentUserId(), eventId);
    }

    /**
     * 查询任务状态和结果。
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<AgentTaskResponse> getTask(@PathVariable("taskId") String taskId) {
        AgentTask task = agentTaskService.getTask(taskId, SecurityUtils.getCurrentUserId());
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(AgentTaskResponse.from(task));
    }

    @PostMapping("/{taskId}/cancel")
    public ResponseEntity<Boolean> cancel(@PathVariable("taskId") String taskId) {
        return ResponseEntity.ok(agentTaskService.cancelTask(taskId, SecurityUtils.getCurrentUserId()));
    }


}
