package com.cloud.programmer.runtime.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cloud.programmer.runtime.dto.ConversationDTO;
import com.cloud.programmer.runtime.entity.ConversationEntity;
import com.cloud.programmer.runtime.entity.ConversationMemoryEntity;
import com.cloud.programmer.runtime.mapper.ConversationMapper;
import com.cloud.programmer.runtime.mapper.ConversationMemoryMapper;
import com.cloud.programmer.security.context.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author 11583
 */
@Service
public class ConversationService {

    private final ConversationMapper conversationMapper;

    private final ConversationMemoryMapper conversationMemoryMapper;

    public ConversationService(ConversationMapper conversationMapper, ConversationMemoryMapper conversationMemoryMapper) {
        this.conversationMapper = conversationMapper;
        this.conversationMemoryMapper = conversationMemoryMapper;
    }

    @Transactional
    public ConversationDTO create(Integer agentId) {
        ConversationEntity entity = new ConversationEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setTitle("新聊天");
        entity.setAgentId(agentId);
        entity.setUserId(SecurityUtils.getCurrentUserId());
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        conversationMapper.insert(entity);
        return toDTO(entity);
    }

    public List<ConversationDTO> list(Long agentId) {

        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new IllegalArgumentException("用户未登录");
        }
        List<ConversationEntity> entities =
                conversationMapper.selectList(
                        Wrappers.<ConversationEntity>lambdaQuery()
                                .eq(ConversationEntity::getUserId, userId)
                                .eq(agentId != null, ConversationEntity::getAgentId, agentId)
                                .orderByDesc(ConversationEntity::getUpdateTime)
                );
        return entities.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public void rename(String id, String title) {
        ConversationEntity entity = getOwnedConversation(id);
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("会话标题不能为空");
        }
        entity.setTitle(title.trim());
        entity.setUpdateTime(new Date());
        conversationMapper.updateById(entity);

    }

    @Transactional
    public void delete(String id) {
        ConversationEntity entity = getOwnedConversation(id);

        conversationMapper.delete(
                Wrappers.<ConversationEntity>lambdaQuery()
                        .eq(ConversationEntity::getId, entity.getId())
                        .eq(ConversationEntity::getUserId,
                                SecurityUtils.getCurrentUserId())
        );
    }

    public List<ConversationMemoryEntity> listByConversationId(String conversationId) {

        // 先验证会话归属，再读取消息
        getOwnedConversation(conversationId);

        return conversationMemoryMapper.selectList(
                new LambdaQueryWrapper<ConversationMemoryEntity>()
                        .eq(ConversationMemoryEntity::getConversationId,
                                conversationId)
                        .in(ConversationMemoryEntity::getMessageType, 1, 3)
                        .orderByAsc(ConversationMemoryEntity::getCreateTime)
                        .orderByAsc(ConversationMemoryEntity::getId)
        );
    }

    private ConversationDTO toDTO(ConversationEntity entity) {
        ConversationDTO dto = new ConversationDTO();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setAgentId(entity.getAgentId());
        return dto;
    }


    private ConversationEntity getOwnedConversation(String id) {

        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new IllegalArgumentException("用户未登录");
        }
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("会话ID不能为空");
        }
        ConversationEntity entity = conversationMapper.selectOne(
                Wrappers.<ConversationEntity>lambdaQuery()
                        .eq(ConversationEntity::getId, id)
                        .eq(ConversationEntity::getUserId, userId)
        );

        if (entity == null) {
            // 不区分会话不存在和不属于当前用户
            throw new IllegalArgumentException("会话不存在");
        }

        return entity;
    }
}