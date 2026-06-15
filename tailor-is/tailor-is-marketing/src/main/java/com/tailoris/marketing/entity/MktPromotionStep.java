package com.tailoris.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mkt_promotion_step")
public class MktPromotionStep extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("promotion_id")
    private Long promotionId;

    @TableField("threshold_value")
    private BigDecimal thresholdValue;

    @TableField("discount_type")
    private Integer discountType;

    @TableField("discount_value")
    private BigDecimal discountValue;

    @TableField("gift_product_id")
    private Long giftProductId;

    @TableField("gift_quantity")
    private Integer giftQuantity;

    @TableField("max_discount")
    private BigDecimal maxDiscount;

    @TableField("sort_order")
    private Integer sortOrder;
}
