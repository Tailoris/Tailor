package com.tailoris.api.admin.entity;

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
@TableName("platform_finance")
public class PlatformFinance extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("finance_type")
    private Integer financeType;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("commission_rate")
    private BigDecimal commissionRate;

    @TableField("commission_amount")
    private BigDecimal commissionAmount;

    @TableField("settlement_amount")
    private BigDecimal settlementAmount;

    @TableField("settlement_status")
    private Integer settlementStatus;

    @TableField("settlement_time")
    private java.time.LocalDateTime settlementTime;

    @TableField("remark")
    private String remark;
}
