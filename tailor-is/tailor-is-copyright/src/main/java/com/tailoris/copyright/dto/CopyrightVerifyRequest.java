package com.tailoris.copyright.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "版权验证请求")
public class CopyrightVerifyRequest {

    @NotBlank(message = "文件哈希不能为空")
    @Schema(description = "文件哈希值(SHA-256)")
    private String fileHash;

    @Schema(description = "文件名（可选）")
    private String fileName;
}