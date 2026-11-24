package com.cloud.programmer.runtime.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_agent_conversation_relation")
public class AgentConversationRelationEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String conversationId;

    private Long agentId;

    private Date createTime;

    private Date updateTime;
}