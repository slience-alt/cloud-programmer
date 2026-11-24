package com.cloud.programmer.agent.controller;

import com.cloud.programmer.agent.dto.AgentDTO;
import com.cloud.programmer.agent.dto.SkillDTO;
import com.cloud.programmer.agent.entity.McpServerEntity;
import com.cloud.programmer.agent.service.AgentService;
import com.cloud.programmer.agent.service.AgentSkillRelationService;
import com.cloud.programmer.agent.service.McpServerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author 周梓强 2026/11/3
 */

@RestController
@RequestMapping("/internal")
public class InternalController {
    private final McpServerService mcpServerService;
    private final AgentService agentService;
    private final AgentSkillRelationService agentSkillRelationService;

    public InternalController(McpServerService mcpServerService, AgentService agentService, AgentSkillRelationService agentSkillRelationService) {
        this.mcpServerService = mcpServerService;
        this.agentService = agentService;
        this.agentSkillRelationService = agentSkillRelationService;
    }

    @GetMapping("/mcp/list")
    @ResponseBody
    public ResponseEntity<List<McpServerEntity>> list() {
        return ResponseEntity.ok(mcpServerService.list());
    }

    @GetMapping("/mcp/findById/{id}")
    @ResponseBody
    public ResponseEntity<McpServerEntity> findById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(mcpServerService.getById(id));
    }


    @GetMapping("/agent/findById")
    public ResponseEntity<AgentDTO> getAgentById(@RequestParam("id") Integer id, @RequestParam("userId") Long userId) {
        AgentDTO agentDTO = agentService.selectByAgentId(id, userId);
        return ResponseEntity.ok(agentDTO);
    }

    @RequestMapping("/skill/findSkillByAgentId")
    @ResponseBody
    public ResponseEntity<List<SkillDTO>> findSkillByAgentId(@RequestParam("agentId") int agentId) {
        return ResponseEntity.ok(agentSkillRelationService.findSkillByAgentId(agentId));
    }

}
