package com.cloud.programmer.user.model.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author 周梓强 2026/9/28
 */

@Data
public class RegisterVO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 50, message = "用户名长度为4-50位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度为8-64位")
    private String password;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(max = 50, message = "昵称不能超过50个字符")
    private String nickname;
}
