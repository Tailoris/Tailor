package com.tailoris.design.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DesignRequest {

    @NotBlank(message = "设计名称不能为空")
    @Size(max = 100, message = "设计名称不能超过100个字符")
    private String name;

    @Size(max = 2000, message = "设计描述不能超过2000个字符")
    private String description;

    private String coverImage;

    private String designType;

    private String styleCategory;

    private String color;

    private String material;

    private String sizeRange;

    private String designData;

    private String difficulty;

    private Integer workTime;

    private BigDecimal price;

    private Integer isFree;

    private Long patternId;

    private Long merchantId;
}