package com.tailoris.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "体型数据请求")
public class SizeDataRequest {

    @NotBlank(message = "体型名称不能为空")
    @Schema(description = "体型名称")
    private String sizeName;

    @NotNull(message = "身高不能为空")
    @DecimalMin(value = "0", message = "身高必须大于0")
    @Schema(description = "身高(cm)")
    private BigDecimal height;

    @Schema(description = "体重(kg)")
    private BigDecimal weight;

    @Schema(description = "肩宽(cm)")
    private BigDecimal shoulderWidth;

    @Schema(description = "胸围(cm)")
    private BigDecimal chestCircumference;

    @Schema(description = "腰围(cm)")
    private BigDecimal waistCircumference;

    @Schema(description = "臀围(cm)")
    private BigDecimal hipCircumference;

    @Schema(description = "颈围(cm)")
    private BigDecimal neckCircumference;

    @Schema(description = "臂长(cm)")
    private BigDecimal armLength;

    @Schema(description = "袖长(cm)")
    private BigDecimal sleeveLength;

    @Schema(description = "腰节长(cm)")
    private BigDecimal waistLength;

    @Schema(description = "内缝长(cm)")
    private BigDecimal inseamLength;

    @Schema(description = "体型分类")
    private String bodyType;

    @Schema(description = "性别：1-男，2-女")
    private Integer gender;

    @Schema(description = "是否设为默认体型")
    private Integer isDefault;
}
