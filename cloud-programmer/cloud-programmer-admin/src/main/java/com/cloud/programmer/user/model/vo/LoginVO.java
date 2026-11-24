package com.cloud.programmer.user.model.vo;

import lombok.Data;

/**
 * @author 周梓强 2026/9/28
 */

@Data
public class LoginVO {
    private String username;
    private String nickname;
    private String password;
    private String accessToken;
    private String tokenType = "Bearer";
}
