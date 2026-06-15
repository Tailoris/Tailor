package com.tailoris.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI纸样生成响应")
public class PatternGenerateResponse {

    @Schema(description = "纸样唯一ID")
    private String patternId;

    @Schema(description = "纸样名称")
    private String name;

    @Schema(description = "服装类型")
    private String garmentType;

    @Schema(description = "SVG纸样内容")
    private String svgContent;

    @Schema(description = "预览URL")
    private String previewUrl;

    @Schema(description = "纸样尺寸信息")
    private Map<String, Object> dimensions;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}