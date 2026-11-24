package com.cloud.programmer.runtime.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationDTO {

    private String id;
    private String title;
    private Integer agentId;

}