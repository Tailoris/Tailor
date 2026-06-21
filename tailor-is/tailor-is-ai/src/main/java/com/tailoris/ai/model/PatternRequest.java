package com.tailoris.ai.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI版型生成请求")
public class PatternRequest {

    @Schema(description = "服装类型：SHIRT/DRESS/PANTS/JACKET/SKIRT")
    private String garmentType;

    @Schema(description = "身体尺寸数据，key为尺寸名称，value为数值(cm)")
    private Map<String, Double> measurements;

    @Schema(description = "风格偏好：casual/formal/sport/business")
    private String stylePreference;

    @Schema(description = "生成约束条件(JSON)")
    private String constraints;
}