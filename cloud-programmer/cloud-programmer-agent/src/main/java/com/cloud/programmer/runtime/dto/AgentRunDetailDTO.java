package com.cloud.programmer.runtime.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
public class AgentRunDetailDTO {

    private String taskId;
    private Long agentId;
    private String conversationId;
    private String status;
    private String inputText;
    private String resultText;
    private String errorMessage;
    private Date startTime;
    private Date endTime;
    private Long durationMs;
    private Date createTime;
    private List<AgentRunEventDTO> events;

}