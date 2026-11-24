package com.cloud.programmer.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * @author 周梓强 2026/10/14
 */

@Data
@TableName("t_agent_skill_relation")
public class AgentSkillRelationEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer agentId;

    private Integer skillId;

    private Date createTime;
}
