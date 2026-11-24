package com.cloud.programmer.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author 周梓强 2026/9/22
 */

@AllArgsConstructor
@Getter
public enum RoleEnum {

    ROLE_ADMIN(1001, "管理员"),
    ROLE_USER(1002, "普通用户");


    private int code;
    private String name;

    public static String getName(int code) {
        for (RoleEnum roleEnum : RoleEnum.values()) {
            if (roleEnum.getCode() == code) {
                return roleEnum.getName();
            }
        }
        return null;
    }
}
