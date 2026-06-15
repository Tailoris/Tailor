package com.tailoris.payment.entity;

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
@TableName("settlement_record")
public class SettlementRecord extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("settlement_no")
    private String settlementNo;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("shop_id")
    private Long shopId;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("settlement_type")
    private Integer settlementType;

    @TableField("order_amount")
    private BigDecimal orderAmount;

    @TableField("platform_fee")
    private BigDecimal platformFee;

    @TableField("platform_fee_rate")
    private BigDecimal platformFeeRate;

    @TableField("coupon_subsidy")
    private BigDecimal couponSubsidy;

    @TableField("merchant_amount")
    private BigDecimal merchantAmount;

    @TableField("status")
    private Integer status;

    @TableField("settled_at")
    private LocalDateTime settledAt;

    @TableField("settlement_cycle")
    private Integer settlementCycle;

    @TableField("remark")
    private String remark;
}
