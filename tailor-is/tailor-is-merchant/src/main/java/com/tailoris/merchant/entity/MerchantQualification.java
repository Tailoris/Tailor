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
@TableName("merchant_qualification")
public class MerchantQualification extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("cert_type")
    private Integer certType;

    @TableField("cert_name")
    private String certName;

    @TableField("cert_no")
    private String certNo;

    @TableField("cert_url")
    private String certUrl;

    @TableField("cert_front_url")
    private String certFrontUrl;

    @TableField("cert_back_url")
    private String certBackUrl;

    @TableField("issue_date")
    private LocalDate issueDate;

    @TableField("expire_date")
    private LocalDate expireDate;

    @TableField("audit_status")
    private Integer auditStatus;

    @TableField("audit_remark")
    private String auditRemark;

    @TableField("audit_time")
    private LocalDateTime auditTime;

    @TableField("audit_by")
    private Long auditBy;
}
