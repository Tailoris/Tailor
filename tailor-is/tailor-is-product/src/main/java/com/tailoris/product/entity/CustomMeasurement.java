package com.tailoris.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 定制商品参数采集 - PRD-008.
 *
 * <p>定制商品下单时需采集用户的身材数据，存储为快照。
 * 关键字段：身高/体重/三围/肩宽/袖长/裤长/偏好风格等。</p>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Data
@TableName("custom_measurement")
public class CustomMeasurement implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("order_id")
    private Long orderId;

    @TableField("product_id")
    private Long productId;

    /** 身高（cm） */
    @TableField("height")
    private java.math.BigDecimal height;

    /** 体重（kg） */
    @TableField("weight")
    private java.math.BigDecimal weight;

    /** 胸围（cm） */
    @TableField("bust")
    private java.math.BigDecimal bust;

    /** 腰围（cm） */
    @TableField("waist")
    private java.math.BigDecimal waist;

    /** 臀围（cm） */
    @TableField("hip")
    private java.math.BigDecimal hip;

    /** 肩宽（cm） */
    @TableField("shoulder")
    private java.math.BigDecimal shoulder;

    /** 袖长（cm） */
    @TableField("sleeve_length")
    private java.math.BigDecimal sleeveLength;

    /** 裤长/裙长（cm） */
    @TableField("pants_length")
    private java.math.BigDecimal pantsLength;

    /** 颈围（cm） */
    @TableField("neck")
    private java.math.BigDecimal neck;

    /** 臂围（cm） */
    @TableField("arm")
    private java.math.BigDecimal arm;

    /** 大腿围（cm） */
    @TableField("thigh")
    private java.math.BigDecimal thigh;

    /** 偏好版型（修身/常规/宽松） */
    @TableField("fit_preference")
    private String fitPreference;

    /** 偏好颜色 */
    @TableField("color_preference")
    private String colorPreference;

    /** 备注 */
    @TableField("remark")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
