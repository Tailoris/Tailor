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
@TableName("user_account")
public class UserAccount extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("balance")
    private BigDecimal balance;

    @TableField("frozen_amount")
    private BigDecimal frozenAmount;

    @TableField("total_recharge")
    private BigDecimal totalRecharge;

    @TableField("total_consume")
    private BigDecimal totalConsume;

    @TableField("total_refund")
    private BigDecimal totalRefund;

    @TableField("points")
    private Integer points;

    @TableField("total_points_earned")
    private Integer totalPointsEarned;

    @TableField("total_points_spent")
    private Integer totalPointsSpent;

    @TableField("version")
    private Integer version;
}
