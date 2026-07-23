package com.tailoris.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 邮箱注册请求 DTO.
 */
@Data
public class EmailRegisterRequest {

    @NotBlank(message = "请输入邮箱")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "请输入验证码")
    private String code;

    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 32, message = "密码长度需在6到32位之间")
    private String password;
}