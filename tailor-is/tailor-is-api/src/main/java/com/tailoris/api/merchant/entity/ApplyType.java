package com.tailoris.api.merchant.entity;

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
@TableName("apply_type")
public class ApplyType extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("type_code")
    private String typeCode;

    @TableField("type_name")
    private String typeName;

    @TableField("required_qualifications")
    private String requiredQualifications;

    @TableField("deposit_amount")
    private BigDecimal depositAmount;

    @TableField("commission_extra_rate")
    private BigDecimal commissionExtraRate;

    @TableField("audit_required")
    private Integer auditRequired;

    @TableField("trial_days")
    private Integer trialDays;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("description")
    private String description;

    @TableField("status")
    private Integer status;
}
