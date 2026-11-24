package com.cloud.programmer.runtime.api;


import com.cloud.programmer.runtime.entity.AgentRun;
import com.cloud.programmer.runtime.task.AgentTask;

import java.time.Instant;

public class AgentTaskResponse {

    private String taskId;
    private String input;
    private String status;
    private String result;
    private String error;
    private Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;

    public AgentTaskResponse() {
    }

    public static AgentTaskResponse from(AgentTask task) {
        AgentTaskResponse response = new AgentTaskResponse();
        response.setTaskId(task.getTaskId());
        response.setInput(task.getInput());
        response.setStatus(task.getStatus());
        response.setResult(task.getResult());
        response.setError(task.getError());
        response.setCreatedAt(task.getCreatedAt());
        response.setStartedAt(task.getStartedAt());
        response.setFinishedAt(task.getFinishedAt());
        return response;
    }

    public static AgentTaskResponse fromRun(AgentRun run) {
        AgentTaskResponse response = new AgentTaskResponse();

        response.setTaskId(run.getTaskId());
        response.setInput(run.getInputText());
        response.setStatus(run.getStatus());
        response.setResult(run.getResultText());
        response.setError(run.getErrorMessage());

        response.setCreatedAt(run.getCreateTime().toInstant());
        response.setStartedAt(run.getStartTime().toInstant());
        response.setFinishedAt(run.getEndTime().toInstant());

        return response;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}
