package com.tailoris.pattern.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 版型创建请求DTO
 */
@Data
public class PatternCreateDTO {

    /** 款式类型 */
    private String styleType;

    /** 版型风格 */
    private String patternStyle;

    /** 目标人群 */
    private String targetAudience;

    /** 最小码数 */
    private Integer minSize;

    /** 最大码数 */
    private Integer maxSize;

    /** 工艺要求列表 */
    private List<String> techRequirements;

    /** 详细描述 */
    private String description;

    /** 最小预算 */
    private BigDecimal minBudget;

    /** 最大预算 */
    private BigDecimal maxBudget;

    /** 交付日期 */
    private String deliveryDate;

    /** 是否加急 */
    private Boolean isUrgent;

    /** 设计参考图URL */
    private String imageUrl;
}
