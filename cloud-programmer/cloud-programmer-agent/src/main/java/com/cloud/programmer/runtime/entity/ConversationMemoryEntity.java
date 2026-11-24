package com.cloud.programmer.runtime.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * @author 周梓强 2026/10/21
 */

@Data
@TableName("t_agent_message")
public class ConversationMemoryEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String conversationId;

    private Integer sequenceNo;

    private Integer messageType;

    private String messageContext;

    private Date createTime;
}
