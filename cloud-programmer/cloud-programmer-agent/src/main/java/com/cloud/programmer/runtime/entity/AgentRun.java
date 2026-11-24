package com.cloud.programmer.runtime.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_agent_run")
public class AgentRun {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String taskId;

    private Long agentId;

    private String conversationId;

    private String status;

    private String inputText;

    private String resultText;

    private String errorMessage;

    private Long userId;

    private Date startTime;

    private Date endTime;

    private Long durationMs;

    private Date createTime;

    private Date updateTime;
}