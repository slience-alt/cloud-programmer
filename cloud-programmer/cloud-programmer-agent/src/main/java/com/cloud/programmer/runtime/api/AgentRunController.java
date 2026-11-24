package com.cloud.programmer.runtime.api;

import com.cloud.programmer.runtime.dto.AgentRunDetailDTO;
import com.cloud.programmer.runtime.service.AgentRunService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent/runs")
public class AgentRunController {

    private final AgentRunService agentRunService;

    public AgentRunController(AgentRunService agentRunService) {
        this.agentRunService = agentRunService;
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<?> getDetail(@PathVariable("taskId") String taskId) {
        AgentRunDetailDTO detail = agentRunService.getDetailByTaskId(taskId);
        if (detail == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("未找到对应的 Agent 执行记录");
        }
        return ResponseEntity.ok(detail);
    }
}