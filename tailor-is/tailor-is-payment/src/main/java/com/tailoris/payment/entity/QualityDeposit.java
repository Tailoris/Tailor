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
@TableName("quality_deposit")
public class QualityDeposit extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("deposit_amount")
    private BigDecimal depositAmount;

    @TableField("frozen_amount")
    private BigDecimal frozenAmount;

    @TableField("available_amount")
    private BigDecimal availableAmount;

    @TableField("min_deposit")
    private BigDecimal minDeposit;

    @TableField("status")
    private Integer status;

    @TableField("pay_time")
    private LocalDateTime payTime;

    @TableField("refund_time")
    private LocalDateTime refundTime;

    @TableField("version")
    private Integer version;
}
