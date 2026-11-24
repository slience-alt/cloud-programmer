package com.cloud.programmer.runtime.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class ConversationMessageDTO {

    private String id;
    private String role;
    private String content;
    private Date createTime;

}