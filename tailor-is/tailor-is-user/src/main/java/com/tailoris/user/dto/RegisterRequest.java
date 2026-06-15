package com.tailoris.user.dto;

import com.tailoris.common.validator.PhoneNumber;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "注册请求")
public class RegisterRequest {

    @Schema(description = "手机号")
    @PhoneNumber(message = "手机号格式不正确")
    private String phone;

    @Schema(description = "密码")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度为6-32位")
    private String password;

    @Schema(description = "短信验证码")
    @NotBlank(message = "验证码不能为空")
    private String smsCode;
}
