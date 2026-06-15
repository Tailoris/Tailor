package com.tailoris.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "实名认证请求")
public class RealNameAuthRequest {

    @Schema(description = "真实姓名")
    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    @Schema(description = "身份证号码")
    @NotBlank(message = "身份证号码不能为空")
    private String idCard;

    @Schema(description = "身份证正面照片URL")
    @NotBlank(message = "身份证正面照片不能为空")
    private String idCardFront;

    @Schema(description = "身份证反面照片URL")
    @NotBlank(message = "身份证反面照片不能为空")
    private String idCardBack;
}
