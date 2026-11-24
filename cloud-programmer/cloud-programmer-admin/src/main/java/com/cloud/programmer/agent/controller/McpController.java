package com.cloud.programmer.agent.controller;

import com.cloud.programmer.agent.dto.McpDTO;
import com.cloud.programmer.agent.entity.McpServerEntity;
import com.cloud.programmer.agent.service.AgentMcpRelationService;
import com.cloud.programmer.agent.service.McpServerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mcp")

public class McpController {

    private final AgentMcpRelationService agentMcpRelationService;
    private final McpServerService mcpServerService;

    public McpController(AgentMcpRelationService agentMcpRelationService, McpServerService mcpServerService) {
        this.agentMcpRelationService = agentMcpRelationService;
        this.mcpServerService = mcpServerService;
    }

    @GetMapping("/getMcpServerList")
    @ResponseBody
    public ResponseEntity<List<McpDTO>> getMcpServerList(@RequestParam Map<String, Object> params) {
       return ResponseEntity.ok(mcpServerService.getMcpServerList(params));
    }

    @PostMapping("/updateStatus")
    public ResponseEntity<Void> updateStatus(@RequestParam("id") Long id, @RequestParam("status") Boolean status) {
        mcpServerService.updateEnabled(id, status);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/insert")
    public ResponseEntity<Void> insert(@RequestBody McpDTO mcpDTO) {
        mcpServerService.insert(mcpDTO);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/delete")
    public ResponseEntity<Void> delete(@RequestParam("id") Long id) {
        mcpServerService.delete(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/update")
    public ResponseEntity<Void> update(@RequestBody McpDTO mcpDTO) {
        mcpServerService.update(mcpDTO);
        return ResponseEntity.ok().build();
    }



}