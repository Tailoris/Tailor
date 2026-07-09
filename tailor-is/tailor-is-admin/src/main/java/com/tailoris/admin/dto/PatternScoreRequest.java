package com.tailoris.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PatternScoreRequest {

    @NotNull(message = "版型精准度不能为空")
    @Min(value = 0, message = "评分最小为0")
    @Max(value = 100, message = "评分最大为100")
    private Integer accuracyScore;

    @NotNull(message = "工艺清晰度不能为空")
    @Min(value = 0, message = "评分最小为0")
    @Max(value = 100, message = "评分最大为100")
    private Integer clarityScore;

    @NotNull(message = "适配性不能为空")
    @Min(value = 0, message = "评分最小为0")
    @Max(value = 100, message = "评分最大为100")
    private Integer adaptabilityScore;

    private String scoreRemark;
}