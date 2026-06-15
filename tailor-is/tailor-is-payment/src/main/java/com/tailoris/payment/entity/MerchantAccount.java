package com.tailoris.payment.entity;

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
@TableName("merchant_account")
public class MerchantAccount extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("balance")
    private BigDecimal balance;

    @TableField("withdrawable_balance")
    private BigDecimal withdrawableBalance;

    @TableField("frozen_amount")
    private BigDecimal frozenAmount;

    @TableField("pending_amount")
    private BigDecimal pendingAmount;

    @TableField("total_income")
    private BigDecimal totalIncome;

    @TableField("total_expense")
    private BigDecimal totalExpense;

    @TableField("total_withdraw")
    private BigDecimal totalWithdraw;

    @TableField("total_settlement")
    private BigDecimal totalSettlement;

    @TableField("version")
    private Integer version;
}
