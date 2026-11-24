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
@TableName("t_agent_skill")
public class SkillEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    private String description;

    private String instruction;

    private Integer status;

    private Date createTime;

    private Date updateTime;
}
