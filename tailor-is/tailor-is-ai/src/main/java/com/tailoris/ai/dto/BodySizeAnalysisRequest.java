package com.tailoris.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "体型数据分析请求")
public class BodySizeAnalysisRequest {

    @NotNull(message = "身高不能为空")
    @Schema(description = "身高(cm)")
    private BigDecimal height;

    @NotNull(message = "体重不能为空")
    @Schema(description = "体重(kg)")
    private BigDecimal weight;

    @Schema(description = "胸围(cm)")
    private BigDecimal chestCircumference;

    @Schema(description = "腰围(cm)")
    private BigDecimal waistCircumference;

    @Schema(description = "臀围(cm)")
    private BigDecimal hipCircumference;
}