package com.tailoris.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 定制参数采集请求 DTO - PRD-008.
 *
 * <p>用户下单定制商品时提交的身材数据。
 * 字段范围参考中国成年人体尺寸国家标准 GB/T 10000-1988。</p>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Data
public class CustomMeasurementRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "身高不能为空")
    @DecimalMin(value = "100", message = "身高应≥100cm")
    @DecimalMin(value = "250", message = "身高应≤250cm")
    private BigDecimal height;

    @DecimalMin(value = "20", message = "体重应≥20kg")
    @DecimalMin(value = "200", message = "体重应≤200kg")
    private BigDecimal weight;

    @DecimalMin(value = "60", message = "胸围应≥60cm")
    @DecimalMin(value = "180", message = "胸围应≤180cm")
    private BigDecimal bust;

    @DecimalMin(value = "50", message = "腰围应≥50cm")
    @DecimalMin(value = "180", message = "腰围应≤180cm")
    private BigDecimal waist;

    @DecimalMin(value = "70", message = "臀围应≥70cm")
    @DecimalMin(value = "180", message = "臀围应≤180cm")
    private BigDecimal hip;

    @DecimalMin(value = "30", message = "肩宽应≥30cm")
    @DecimalMin(value = "70", message = "肩宽应≤70cm")
    private BigDecimal shoulder;

    @DecimalMin(value = "40", message = "袖长应≥40cm")
    @DecimalMin(value = "100", message = "袖长应≤100cm")
    private BigDecimal sleeveLength;

    @DecimalMin(value = "60", message = "裤长应≥60cm")
    @DecimalMin(value = "130", message = "裤长应≤130cm")
    private BigDecimal pantsLength;

    @DecimalMin(value = "25", message = "颈围应≥25cm")
    @DecimalMin(value = "60", message = "颈围应≤60cm")
    private BigDecimal neck;

    @DecimalMin(value = "20", message = "臂围应≥20cm")
    @DecimalMin(value = "60", message = "臂围应≤60cm")
    private BigDecimal arm;

    @DecimalMin(value = "30", message = "大腿围应≥30cm")
    @DecimalMin(value = "100", message = "大腿围应≤100cm")
    private BigDecimal thigh;

    /** 偏好版型：SLIM=修身 REGULAR=常规 LOOSE=宽松 */
    private String fitPreference;

    /** 偏好颜色 */
    private String colorPreference;

    /** 备注 */
    private String remark;
}
