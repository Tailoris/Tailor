package com.tailoris.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mkt_order_promotion")
public class MktOrderPromotion extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("promotion_type")
    private Integer promotionType;

    @TableField("promotion_id")
    private Long promotionId;

    @TableField("promotion_name")
    private String promotionName;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @TableField("coupon_id")
    private Long couponId;

    @TableField("group_instance_id")
    private Long groupInstanceId;

    @TableField("seckill_id")
    private Long seckillId;

    @TableField("points_used")
    private Integer pointsUsed;

    @TableField("create_time")
    private LocalDateTime createTime;
}
