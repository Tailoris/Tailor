package com.tailoris.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 密码重置请求 DTO.
 */
@Data
public class ResetPasswordRequest {

    @NotBlank(message = "请输入账号")
    private String target;

    @NotBlank(message = "请输入验证码")
    private String code;

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 32, message = "密码长度需在6到32位之间")
    private String newPassword;

    private String type;
}