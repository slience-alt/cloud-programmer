
package com.cloud.programmer.runtime.orchestrator;

import com.cloud.programmer.runtime.agent.AgentConfigLoader;
import com.cloud.programmer.runtime.agent.AgentDefinition;
import com.cloud.programmer.runtime.memory.ConversationMemory;
import com.cloud.programmer.runtime.message.AgentMessage;
import com.cloud.programmer.runtime.skill.Skill;
import com.cloud.programmer.runtime.stream.AgentStreamEvent;
import com.cloud.programmer.runtime.task.AgentTaskContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
public class AgentOrchestrator {

    private final ConversationMemory conversationMemory;
    private final AgentExecutionLoop agentExecutionLoop;
    private final AgentConfigLoader agentConfigLoader;

    public AgentOrchestrator(ConversationMemory conversationMemory, AgentExecutionLoop agentExecutionLoop, AgentConfigLoader agentConfigLoader) {
        this.conversationMemory = conversationMemory;
        this.agentExecutionLoop = agentExecutionLoop;
        this.agentConfigLoader = agentConfigLoader;
    }

    /**
     * 非流式执行
     */
    public String run(String userInput, String conversationId, Long agentId, Long userId, String taskId, AgentTaskContext taskContext) {
        validateUserInput(userInput);
        taskContext.checkCancelled();

        conversationMemory.validateAgent(conversationId, agentId);
        // 1. 加载一次 Agent 配置
        AgentDefinition definition = agentConfigLoader.load(agentId, userId);
        // 2. 初始化对话消息
        List<AgentMessage> messages = prepareMessages(conversationId, definition, userInput);
        // 3. 执行 Agent
        String result = agentExecutionLoop.execute(messages, taskId, definition, taskContext);
        // 4. 保存对话历史
        conversationMemory.saveMessages(conversationId, messages);

        return result;
    }

    /**
     * 流式执行
     */
    public String runStream(String userInput, Consumer<AgentStreamEvent> eventConsumer, String conversationId, Long agentId, Long userId, String taskId, AgentTaskContext taskContext) {
        validateUserInput(userInput);
        taskContext.checkCancelled();
        conversationMemory.validateAgent(conversationId, agentId);
        // 1. 加载一次 Agent 配置
        AgentDefinition definition = agentConfigLoader.load(agentId, userId);
        // 2. 初始化对话消息
        List<AgentMessage> messages = prepareMessages(conversationId, definition, userInput);
        // 3. 执行 Agent
        String result = agentExecutionLoop.executeStream(messages, eventConsumer, taskId, definition, taskContext);
        // 4. 保存对话历史
        conversationMemory.saveMessages(conversationId, messages);
        return result;
    }

    /**
     * 准备对话消息
     */
    private List<AgentMessage> prepareMessages(String conversationId, AgentDefinition definition, String userInput) {

        List<AgentMessage> messages = conversationMemory.getMessages(conversationId);
        // 仅在新会话中初始化 System Prompt
        if (messages.isEmpty()) {
            createSystemPrompt(definition, messages);
        }
        messages.add(AgentMessage.user(userInput));
        return messages;
    }

    /**
     * 构建系统提示词
     */
    private void createSystemPrompt(AgentDefinition definition, List<AgentMessage> messages) {

        StringBuilder prompt = new StringBuilder();
        if (definition.getSystemPrompt() != null) {
            prompt.append(definition.getSystemPrompt());
        }
        List<Skill> skills = definition.getSkills();
        if (skills != null && !skills.isEmpty()) {
            prompt.append("\n\n");
            prompt.append("你当前拥有以下 Skill 能力：\n");
            for (Skill skill : skills) {
                prompt.append("\n");
                prompt.append("Skill 名称：")
                        .append(skill.getName())
                        .append("\n");

                prompt.append(skill.getInstruction());
                prompt.append("\n");
            }
        }
        messages.add(AgentMessage.system(prompt.toString()));
    }

    private void validateUserInput(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            throw new IllegalArgumentException("用户输入不能为空");
        }
    }
}
