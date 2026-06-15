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
@Schema(description = "版权验证响应")
public class CopyrightVerifyResponse {

    @Schema(description = "是否匹配到已有版权记录")
    private boolean matched;

    @Schema(description = "存证证书号")
    private String certificateNo;

    @Schema(description = "作者名称")
    private String authorName;

    @Schema(description = "作品名称")
    private String workTitle;

    @Schema(description = "登记时间")
    private LocalDateTime registeredAt;
}