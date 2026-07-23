package com.tailoris.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 验证码登录请求 DTO.
 */
@Data
public class LoginByCodeRequest {

    @NotBlank(message = "请输入账号")
    private String target;

    @NotBlank(message = "请输入验证码")
    private String code;

    private String type;
}