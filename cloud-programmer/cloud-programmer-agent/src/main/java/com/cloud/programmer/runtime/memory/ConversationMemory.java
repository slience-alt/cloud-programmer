package com.cloud.programmer.runtime.memory;

import com.cloud.programmer.runtime.message.AgentMessage;

import java.util.List;

public interface ConversationMemory {
    /**
     * 获取会话历史消息
     *
     * @param conversationId 会话ID
     */
    List<AgentMessage> getMessages(String conversationId);

    /**
     * 保存消息
     *
     * @param conversationId 会话ID
     * @param messages 消息列表
     */
    void saveMessages(String conversationId, List<AgentMessage> messages);

    /**
     * 添加单条消息
     */
    void addMessage(String conversationId, AgentMessage message);
    /**
     * 清理会话
     */
    void clear(String conversationId);

    /**
     * 校验会话是否属于指定 Agent。
     */
    void validateAgent(String conversationId, Long agentId);

    /**
     * 初始化会话与 Agent 的绑定关系。
     */
    void bindAgent(String conversationId, Long agentId);

}