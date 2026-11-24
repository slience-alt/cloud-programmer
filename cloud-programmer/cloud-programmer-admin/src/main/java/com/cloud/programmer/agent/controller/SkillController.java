package com.cloud.programmer.agent.controller;

import com.cloud.programmer.agent.dto.SkillDTO;
import com.cloud.programmer.agent.service.AgentSkillRelationService;
import com.cloud.programmer.agent.service.SkillService;
import com.cloud.programmer.enums.ResponseCodeEnum;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author 周梓强 2026/10/14
 */

@RestController
@RequestMapping("/skill")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }


    @RequestMapping("/list")
    @ResponseBody
    public ResponseEntity<List<SkillDTO>> list(@RequestParam Map<String, Object> params) {
        return ResponseEntity.ok(skillService.list(params));
    }

    @RequestMapping("/insert")
    @ResponseBody
    public ResponseEntity<SkillDTO> insert(@RequestBody SkillDTO skillDTO) {
        return ResponseEntity.ok(skillService.insert(skillDTO));
    }

    @RequestMapping("/update")
    @ResponseBody
    public ResponseEntity<SkillDTO> update(@RequestBody SkillDTO skillDTO) {
        return ResponseEntity.ok(skillService.update(skillDTO));
    }

    @RequestMapping("/delete")
    @ResponseBody
    public ResponseEntity<Boolean> delete(@RequestParam("id") Integer id) {
        return ResponseEntity.ok(skillService.delete(id));
    }

    @RequestMapping("/updateStatus")
    @ResponseBody
    public ResponseEntity<Boolean> updateStatus(@RequestParam("id") Integer id, @RequestParam("status") Integer status) {
        return ResponseEntity.ok(skillService.updataStatus(id, status));
    }

}
