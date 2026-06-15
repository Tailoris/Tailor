package com.tailoris.copyright.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "版权登记响应")
public class CopyrightRegisterResponse {

    @Schema(description = "版权记录ID")
    private Long recordId;

    @Schema(description = "存证证书号")
    private String certificateNo;

    @Schema(description = "文件哈希值(SHA-256)")
    private String fileHash;

    @Schema(description = "作品名称")
    private String workTitle;

    @Schema(description = "登记时间")
    private LocalDateTime registeredAt;
}