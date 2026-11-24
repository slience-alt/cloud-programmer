package com.cloud.programmer.runtime.memory;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cloud.programmer.enums.AgentRoleEnum;
import com.cloud.programmer.runtime.entity.AgentConversationRelationEntity;
import com.cloud.programmer.runtime.entity.ConversationMemoryEntity;
import com.cloud.programmer.runtime.mapper.AgentConversationRelationMapper;
import com.cloud.programmer.runtime.mapper.ConversationMemoryMapper;
import com.cloud.programmer.runtime.message.AgentMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Component
public class DatabaseConversationMemory implements ConversationMemory {

    private final ConversationMemoryMapper conversationMemoryMapper;
    private final ObjectMapper objectMapper;
    private final AgentConversationRelationMapper agentConversationMapper;

    public DatabaseConversationMemory(ConversationMemoryMapper conversationMemoryMapper, ObjectMapper objectMapper, AgentConversationRelationMapper agentConversationMapper) {
        this.conversationMemoryMapper = conversationMemoryMapper;
        this.objectMapper = objectMapper;
        this.agentConversationMapper = agentConversationMapper;
    }

    /**
     * 获取会话历史，按照 sequence_no 升序排列。
     */
    @Override
    public List<AgentMessage> getMessages(String conversationId) {
        validateConversationId(conversationId);

        List<ConversationMemoryEntity> entities = conversationMemoryMapper.selectList(Wrappers.<ConversationMemoryEntity>lambdaQuery()
                                .eq(ConversationMemoryEntity::getConversationId, conversationId)
                                .orderByAsc(ConversationMemoryEntity::getSequenceNo)
                );

        List<AgentMessage> messages = new ArrayList<>(entities.size());

        for (ConversationMemoryEntity entity : entities) {
            AgentMessage message = deserialize(entity.getMessageContext());
            // 校验数据库字典编码与消息 JSON 中的角色一致。
            String role = AgentRoleEnum.getTypeByCode(entity.getMessageType());
            if (!role.equals(message.getRole())) {
                throw new IllegalStateException("会话消息类型不一致，messageId=" + entity.getId());
            }
            messages.add(message);
        }

        return messages;
    }

    /**
     * 整体替换指定会话的历史消息。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMessages(String conversationId, List<AgentMessage> messages) {
        validateConversationId(conversationId);
        List<AgentMessage> safeMessages = messages == null ? List.of() : messages;
        // 先完成序列化和类型转换，避免转换失败后删除原有数据。
        List<ConversationMemoryEntity> entities = new ArrayList<>(safeMessages.size());
        for (int i = 0; i < safeMessages.size(); i++) {
            AgentMessage message = safeMessages.get(i);
            if (message == null) {
                throw new IllegalArgumentException("消息列表不能包含 null");
            }
            ConversationMemoryEntity entity = new ConversationMemoryEntity();
            entity.setConversationId(conversationId);
            entity.setSequenceNo(i + 1);
            entity.setMessageType(AgentRoleEnum.getCodeByType(message.getRole()));
            entity.setMessageContext(serialize(message));
            entity.setCreateTime(new Date());
            entities.add(entity);
        }
        // 删除和插入在同一事务中，失败时回滚。
        conversationMemoryMapper.delete(Wrappers.<ConversationMemoryEntity>lambdaQuery().eq(ConversationMemoryEntity::getConversationId, conversationId));
        for (ConversationMemoryEntity entity : entities) {
            conversationMemoryMapper.insert(entity);
        }
    }

    /**
     * 向会话追加一条消息。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMessage(String conversationId, AgentMessage message) {
        validateConversationId(conversationId);

        if (message == null) {
            throw new IllegalArgumentException("消息不能为空");
        }

        ConversationMemoryEntity entity = new ConversationMemoryEntity();
        entity.setConversationId(conversationId);
        entity.setSequenceNo(conversationMemoryMapper.selectMaxSequenceNo(conversationId) + 1);
        entity.setMessageType(AgentRoleEnum.getCodeByType(message.getRole()));
        entity.setMessageContext(serialize(message));
        entity.setCreateTime(new Date());
        conversationMemoryMapper.insert(entity);
    }

    /**
     * 清除指定会话的历史消息。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clear(String conversationId) {
        validateConversationId(conversationId);
        conversationMemoryMapper.delete(Wrappers.<ConversationMemoryEntity>lambdaQuery().eq(ConversationMemoryEntity::getConversationId, conversationId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindAgent(String conversationId, Long agentId) {

        validateConversationId(conversationId);

        if (agentId == null) {
            throw new IllegalArgumentException("agentId 不能为空");
        }

        AgentConversationRelationEntity conversation = agentConversationMapper.selectOne(
                        Wrappers.<AgentConversationRelationEntity>lambdaQuery()
                                .eq(AgentConversationRelationEntity::getConversationId,
                                        conversationId)
                );

        if (conversation != null) {
            if (!agentId.equals(conversation.getAgentId())) {
                throw new IllegalStateException("当前会话已绑定其他 Agent，不能切换 Agent");
            }
            return;
        }

        Date now = new Date();

        AgentConversationRelationEntity entity = new AgentConversationRelationEntity();
        entity.setConversationId(conversationId);
        entity.setAgentId(agentId);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);

        agentConversationMapper.insert(entity);
    }


    @Override
    public void validateAgent(String conversationId, Long agentId) {

        validateConversationId(conversationId);

        AgentConversationRelationEntity conversation = agentConversationMapper.selectOne(
                        Wrappers.<AgentConversationRelationEntity>lambdaQuery()
                                .eq(AgentConversationRelationEntity::getConversationId,
                                        conversationId)
                );

        if (conversation == null) {
            throw new IllegalStateException("会话绑定关系不存在");
        }

        if (!agentId.equals(conversation.getAgentId())) {
            throw new IllegalStateException("当前会话不属于指定 Agent");
        }
    }



    private String serialize(AgentMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "AgentMessage 序列化失败", e
            );
        }
    }

    private AgentMessage deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, AgentMessage.class);
        } catch (IOException e) {
            throw new IllegalStateException("AgentMessage 反序列化失败", e);
        }
    }

    private void validateConversationId(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            throw new IllegalArgumentException(
                    "conversationId 不能为空"
            );
        }

        if (conversationId.length() > 128) {
            throw new IllegalArgumentException(
                    "conversationId 长度不能超过 128"
            );
        }
    }
}