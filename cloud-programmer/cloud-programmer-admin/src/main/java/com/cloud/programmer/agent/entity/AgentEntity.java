package com.cloud.programmer.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * @author 周梓强 2026/10/13
 */

@TableName("t_agent")
@Data
public class AgentEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    private String description;

    private String systemPrompt;

    private String modelName;

    private Integer status;

    private Long ownerUserId;

    private Date createTime;

    private Date updateTime;
}
