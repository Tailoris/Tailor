package com.tailoris.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "体型数据分析响应")
public class BodySizeAnalysisResponse {

    @Schema(description = "身高(cm)")
    private BigDecimal height;

    @Schema(description = "体重(kg)")
    private BigDecimal weight;

    @Schema(description = "胸围(cm)")
    private BigDecimal chestCircumference;

    @Schema(description = "腰围(cm)")
    private BigDecimal waistCircumference;

    @Schema(description = "臀围(cm)")
    private BigDecimal hipCircumference;

    @Schema(description = "推荐尺码(上身/下身)")
    private Map<String, String> recommendedSizes;

    @Schema(description = "置信度")
    private BigDecimal confidence;

    @Schema(description = "分析时间")
    private LocalDateTime analyzedAt;
}