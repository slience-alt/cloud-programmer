package com.cloud.programmer.runtime.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@TableName("t_agent_conversation")
@Data
public class ConversationEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    private String title;

    private Integer agentId;

    private Long userId;

    private Date createTime;

    private Date updateTime;


}