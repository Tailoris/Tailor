package com.tailoris.api.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_modify_apply")
public class MerchantModifyApply extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("apply_no")
    private String applyNo;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("shop_id")
    private Long shopId;

    @TableField("apply_type")
    private Integer applyType;

    @TableField("modify_content")
    private String modifyContent;

    @TableField("original_snapshot")
    private String originalSnapshot;

    @TableField("reason")
    private String reason;

    @TableField("attachments")
    private String attachments;

    @TableField("status")
    private Integer status;

    @TableField("audit_remark")
    private String auditRemark;

    @TableField("audit_by")
    private Long auditBy;

    @TableField("audit_time")
    private LocalDateTime auditTime;

    @TableField("submitted_by")
    private Long submittedBy;
}
