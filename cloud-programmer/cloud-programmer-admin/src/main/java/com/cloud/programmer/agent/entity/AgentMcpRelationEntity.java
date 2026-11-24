package com.cloud.programmer.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_agent_mcp_relation")
public class AgentMcpRelationEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer agentId;

    private Integer mcpServerId;

    private Date createTime;
}