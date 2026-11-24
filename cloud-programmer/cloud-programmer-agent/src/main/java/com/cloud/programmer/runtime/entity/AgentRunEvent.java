package com.cloud.programmer.runtime.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@TableName("t_agent_run_event")
public class AgentRunEvent {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String taskId;

    /**
     * MODEL_CALL、TOOL_CALL、TOOL_RESULT、ERROR
     */
    private String eventType;

    /**
     * 模型名称或工具名称
     */
    private String eventName;

    private String inputContent;

    private String outputContent;

    /**
     * SUCCESS 或 FAILED
     */
    private String status;

    private Long durationMs;

    private Date createTime;
}