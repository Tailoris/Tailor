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
@TableName("commission_config")
public class CommissionConfig extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("merchant_type")
    private Integer merchantType;

    @TableField("commission_rate")
    private BigDecimal commissionRate;

    @TableField("min_settlement_amount")
    private BigDecimal minSettlementAmount;

    @TableField("settlement_cycle")
    private Integer settlementCycle;

    @TableField("status")
    private Integer status;

    @TableField("remark")
    private String remark;
}
