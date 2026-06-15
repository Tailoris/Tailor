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
@TableName("reconciliation_record")
public class ReconciliationRecord extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("channel")
    private String channel;

    @TableField("channel_trade_no")
    private String channelTradeNo;

    @TableField("status")
    private Integer status;

    @TableField("diff_amount")
    private BigDecimal diffAmount;

    @TableField("reconciled_at")
    private LocalDateTime reconciledAt;
}