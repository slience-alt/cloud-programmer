package com.cloud.programmer.user.model.vo;

import lombok.Data;

/**
*@author 周梓强 2026/9/22
*/

@Data
public class UserVO {
    private Long id;
    private String nickname;
    private Integer role;
    private Integer credit;
    private Boolean isEnable;
}
