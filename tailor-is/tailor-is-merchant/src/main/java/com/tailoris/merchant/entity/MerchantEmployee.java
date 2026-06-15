package com.tailoris.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_employee")
public class MerchantEmployee extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("shop_id")
    private Long shopId;

    @TableField("user_id")
    private Long userId;

    @TableField("employee_name")
    private String employeeName;

    @TableField("employee_phone")
    private String employeePhone;

    @TableField("role")
    private Integer role;

    @TableField("role_code")
    private String roleCode;

    @TableField("permissions")
    private String permissions;

    @TableField("shop_ids")
    private String shopIds;

    @TableField("status")
    private Integer status;

    @TableField("hire_date")
    private LocalDate hireDate;

    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    @TableField("last_active_time")
    private LocalDateTime lastActiveTime;

    @TableField("login_count")
    private Integer loginCount;
}
