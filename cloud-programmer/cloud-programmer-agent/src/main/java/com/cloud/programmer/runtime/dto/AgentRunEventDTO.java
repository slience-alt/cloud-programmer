package com.cloud.programmer.runtime.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class AgentRunEventDTO {

    private Long id;
    private String eventType;
    private String eventName;
    private String inputContent;
    private String outputContent;
    private String status;
    private Long durationMs;
    private Date createTime;

}