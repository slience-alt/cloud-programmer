package com.cloud.programmer.runtime.api;


import com.cloud.programmer.runtime.dto.ConversationDTO;
import com.cloud.programmer.runtime.dto.ConversationMessageDTO;
import com.cloud.programmer.runtime.entity.ConversationMemoryEntity;
import com.cloud.programmer.runtime.service.ConversationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/agent/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    private final ObjectMapper objectMapper;

    public ConversationController(ConversationService conversationService, ObjectMapper objectMapper) {
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建会话
     */
    @PostMapping
    public ResponseEntity<ConversationDTO> create(@RequestBody(required = false) Map<String, Object> params) {
        Integer agentId = null;

        if (params != null && params.get("agentId") != null) {
            agentId = (Integer) params.get("agentId");
        }
        return ResponseEntity.ok(conversationService.create(agentId));
    }

    /**
     * 查询会话列表
     */
    @GetMapping
    public ResponseEntity<List<ConversationDTO>> list(@RequestParam(required = false, value = "agentId") Long agentId) {
        return ResponseEntity.ok(conversationService.list(agentId));
    }

    /**
     * 重命名会话
     */
    @PutMapping("/{id}/title")
    public ResponseEntity<Void> rename(@PathVariable("id") String id, @RequestBody Map<String, String> params) {
        conversationService.rename(id, params.get("title"));
        return ResponseEntity.noContent().build();
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") String id) {
        conversationService.delete(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<ConversationMessageDTO>> listMessages(@PathVariable("conversationId") String conversationId) {

        List<ConversationMemoryEntity> messages = conversationService.listByConversationId(conversationId);
        List<ConversationMessageDTO> result = messages.stream().map(this::convertToVO).toList();
        return ResponseEntity.ok(result);
    }

    private ConversationMessageDTO convertToVO(ConversationMemoryEntity entity) {
        ConversationMessageDTO conversationMessageDTO = new ConversationMessageDTO();
        conversationMessageDTO.setId(String.valueOf(entity.getId()));
        conversationMessageDTO.setRole(entity.getMessageType() == 1 ? "user" : "assistant");
        conversationMessageDTO.setContent(extractContent(entity.getMessageContext()));
        conversationMessageDTO.setCreateTime(entity.getCreateTime());
        return conversationMessageDTO;
    }

    private String extractContent(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        try {
            JsonNode jsonNode = objectMapper.readTree(content);
            JsonNode contentNode = jsonNode.get("content");
            if (contentNode != null && contentNode.isTextual()) {
                return contentNode.asText();
            }
            return content;
        } catch (Exception e) {
            return content;
        }
    }
}