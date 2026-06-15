package com.tailoris.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "生成版型请求")
public class PatternGenerateRequest {

    @NotBlank(message = "版型名称不能为空")
    @Schema(description = "版型名称")
    private String patternName;

    @Schema(description = "服装类型：SHIRT/DRESS/PANTS/JACKET")
    private String garmentType;

    @NotNull(message = "体型数据ID不能为空")
    @Schema(description = "体型数据ID")
    private Long bodySizeId;

    @NotNull(message = "版型类型不能为空")
    @Schema(description = "版型类型：1-上衣，2-裤子，3-裙子，4-外套，5-衬衫")
    private Integer patternType;

    @Schema(description = "版型参数(JSON)")
    private String parameters;

    @Schema(description = "导出格式：SVG/PDF/DXF")
    private String exportFormat;

    @Schema(description = "纸样宽度(cm)")
    private BigDecimal width;

    @Schema(description = "纸样高度(cm)")
    private BigDecimal height;

    @Schema(description = "肩宽(cm)")
    private BigDecimal shoulderWidth;

    @Schema(description = "袖长(cm)")
    private BigDecimal sleeveLength;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "备注")
    private String memo;
}
