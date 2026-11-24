package com.cloud.programmer.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * @author 周梓强 2026/9/23
 */

@Data
@TableName("sys_plugins")
public class PluginEntity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private Integer type;
    private String content;
    private Boolean isCustom;
    private Boolean isSale;
    private Boolean isDel;
    private Date createTime;
}
