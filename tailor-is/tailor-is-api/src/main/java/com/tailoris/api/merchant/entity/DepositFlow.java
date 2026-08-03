package com.tailoris.api.merchant.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("deposit_flow")
public class DepositFlow implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("flow_no")
    private String flowNo;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("quality_deposit_id")
    private Long qualityDepositId;

    @TableField("flow_type")
    private Integer flowType;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("balance_before")
    private BigDecimal balanceBefore;

    @TableField("balance_after")
    private BigDecimal balanceAfter;

    @TableField("related_type")
    private Integer relatedType;

    @TableField("related_id")
    private Long relatedId;

    @TableField("related_no")
    private String relatedNo;

    @TableField("operator_id")
    private Long operatorId;

    @TableField("operator_type")
    private Integer operatorType;

    @TableField("remark")
    private String remark;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
