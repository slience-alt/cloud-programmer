package com.cloud.programmer.runtime.task;

import java.time.Instant;

public class AgentTask {

    private final String taskId;
    private final String input;
    private final String conversationId;
    private final Long agentId;
    private final Instant createdAt;
    private final AgentTaskContext context;


    private final Long ownerUserId;

    private volatile String status;
    private volatile String result;
    private volatile String error;
    private volatile Instant startedAt;
    private volatile Instant finishedAt;

    public AgentTask(String taskId, String input, String conversationId, Long agentId, Long ownerUserId) {
        this.taskId = taskId;
        this.input = input;
        this.conversationId = conversationId;
        this.agentId = agentId;
        this.ownerUserId = ownerUserId;
        this.createdAt = Instant.now();
        this.status = "PENDING";
        this.context = new AgentTaskContext(taskId);
    }

    public synchronized boolean start() {
        if (!"PENDING".equals(status) || context.isCancelled()) {
            return false;
        }

        status = "RUNNING";
        startedAt = Instant.now();
        return true;
    }

    public synchronized boolean complete(String result) {
        if (!"RUNNING".equals(status) || context.isCancelled()) {
            return false;
        }

        this.result = result;
        this.status = "SUCCESS";
        this.finishedAt = Instant.now();
        return true;
    }

    public synchronized boolean fail(String error) {
        if (!"RUNNING".equals(status) || context.isCancelled()) {
            return false;
        }

        this.error = error;
        this.status = "FAILED";
        this.finishedAt = Instant.now();
        return true;
    }
    /**
     * 请求取消任务。
     *
     * PENDING 状态下直接进入 CANCELLED；
     * RUNNING 状态下只设置取消标记，由执行线程完成取消。
     */
    public synchronized boolean requestCancel() {
        if (isTerminal()) {
            return false;
        }

        if (!context.cancel()) {
            return false;
        }

        if ("PENDING".equals(status)) {
            this.status = "CANCELLED";
            this.error = "任务已取消";
            this.finishedAt = Instant.now();
        }

        return true;
    }
    /**
     * 将任务状态更新为 CANCELLED。
     */
    public synchronized boolean cancel() {
        if (isTerminal()) {
            return false;
        }

        context.cancel();
        this.status = "CANCELLED";
        this.error = "任务已取消";
        this.finishedAt = Instant.now();
        return true;
    }

    public boolean isTerminal() {
        return "SUCCESS".equals(status)
                || "FAILED".equals(status)
                || "CANCELLED".equals(status);
    }

    public AgentTaskContext getContext() {
        return context;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getInput() {
        return input;
    }

    public String getConversationId() {
        return conversationId;
    }

    public Long getAgentId() {
        return agentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getStatus() {
        return status;
    }

    public String getResult() {
        return result;
    }

    public String getError() {
        return error;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }


}