package com.tailoris.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant")
public class Merchant extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("merchant_type")
    private Integer merchantType;

    @TableField("company_name")
    private String companyName;

    @TableField("license_no")
    private String licenseNo;

    @TableField("contact_name")
    private String contactName;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("contact_email")
    private String contactEmail;

    @TableField("province")
    private String province;

    @TableField("city")
    private String city;

    @TableField("district")
    private String district;

    @TableField("address")
    private String address;

    @TableField("business_scope")
    private String businessScope;

    @TableField("status")
    private Integer status;

    @TableField("audit_status")
    private Integer auditStatus;

    @TableField("audit_remark")
    private String auditRemark;

    @TableField("audit_time")
    private LocalDateTime auditTime;

    @TableField("audit_by")
    private Long auditBy;

    @TableField("join_time")
    private LocalDateTime joinTime;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    /** 是否试运营 0否 1是 */
    @TableField("is_trial")
    private Integer isTrial;

    /** 试运营开始日期 yyyy-MM-dd */
    @TableField("trial_start_date")
    private String trialStartDate;

    /** 试运营结束日期 yyyy-MM-dd */
    @TableField("trial_end_date")
    private String trialEndDate;

    /** 是否已转正 0否 1是 */
    @TableField("is_promoted")
    private Integer isPromoted;

    /** 转正时间 */
    @TableField("promote_time")
    private LocalDateTime promoteTime;

    /** 违规扣分（满分100） */
    @TableField("violation_score")
    private Integer violationScore;

    /** 当前处罚状态 0=正常 1=限流 2=下架 3=封禁 */
    @TableField("punishment_status")
    private Integer punishmentStatus;

    /** 处罚结束时间 */
    @TableField("punishment_end")
    private LocalDateTime punishmentEnd;
}
