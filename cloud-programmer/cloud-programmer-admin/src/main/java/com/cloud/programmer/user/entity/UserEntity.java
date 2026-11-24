package com.cloud.programmer.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * @author 周梓强 2026/9/22
 */

@Data
@TableName("sys_user")
public class UserEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String nickname;
    private String password;
    private String email;
    private Integer credit;
    private Integer role;
    private Boolean isEnable;
    private Boolean isDel;
    private Date createTime;
    private Date lastLoginTime;
}
