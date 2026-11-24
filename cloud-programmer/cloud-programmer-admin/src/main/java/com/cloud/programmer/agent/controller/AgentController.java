package com.cloud.programmer.agent.controller;

import com.cloud.programmer.agent.dto.AgentDTO;
import com.cloud.programmer.agent.entity.AgentEntity;
import com.cloud.programmer.agent.service.AgentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author 周梓强 2026/10/13
 */

@RestController
@RequestMapping("/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @RequestMapping("/list")
    @ResponseBody
    public ResponseEntity<List<AgentDTO>> list(@RequestParam Map<String, Object> params) {
        List<AgentDTO> list = agentService.list(params);
        return ResponseEntity.ok(list);
    }

    @RequestMapping("/findById")
    @ResponseBody
    public ResponseEntity<AgentDTO> getAgentById(@RequestParam("id") Integer id) {
       return ResponseEntity.ok(agentService.selectByAgentId(id));
    }

    @RequestMapping("/insert")
    @ResponseBody
    public ResponseEntity<AgentDTO> insertAgent(@RequestBody AgentDTO agentDTO) {
        AgentDTO resultAgentDTO = agentService.insert(agentDTO);
        return ResponseEntity.ok(resultAgentDTO);
    }

    @RequestMapping("/updateStatus")
    @ResponseBody
    public ResponseEntity<Boolean> updateAgentStatus(@RequestParam("id") Integer id, @RequestParam("status") Integer status) {
       return ResponseEntity.ok(agentService.updateStatus(id, status));
    }

    @RequestMapping("/update")
    @ResponseBody
    public ResponseEntity<AgentDTO> updateAgent(@RequestBody AgentDTO agentDTO) {
        return ResponseEntity.ok(agentService.update(agentDTO));
    }

    @RequestMapping("/delete")
    @ResponseBody
    public ResponseEntity<Boolean> deleteAgent(@RequestParam("id") Integer id) {
        return ResponseEntity.ok(agentService.delete(id));
    }

    @PostMapping("/batchDelete")
    public ResponseEntity<Boolean> batchDelete(@RequestBody List<Integer> ids) {
        return ResponseEntity.ok(agentService.batchDelete(ids));
    }
}
