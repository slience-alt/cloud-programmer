package com.cloud.programmer.user.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * @author 周梓强 2026/11/10
 */

@Data
public class UserInfoVO {

    private String username;
    private String nickname;
    private Integer credit;
    private String email;
    private String role;
    private Date lastLoginTime;
}
