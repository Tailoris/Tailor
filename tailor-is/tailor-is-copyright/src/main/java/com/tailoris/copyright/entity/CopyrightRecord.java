package com.tailoris.copyright.entity;

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
@TableName("copyright_record")
public class CopyrightRecord extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("author_real_name")
    private String authorRealName;

    @TableField("author_id_card")
    private String authorIdCard;

    @TableField("author_phone")
    private String authorPhone;

    @TableField("product_id")
    private Long productId;

    @TableField("work_name")
    private String workName;

    @TableField("work_type")
    private Integer workType;

    @TableField("file_hash")
    private String fileHash;

    @TableField("file_type")
    private String fileType;

    @TableField("file_size")
    private Long fileSize;

    @TableField("file_url")
    private String fileUrl;

    @TableField("thumbnail_url")
    private String thumbnailUrl;

    @TableField("description")
    private String description;

    @TableField("blockchain_tx_hash")
    private String blockchainTxHash;

    @TableField("blockchain_tx_time")
    private LocalDateTime blockchainTxTime;

    @TableField("blockchain_platform")
    private String blockchainPlatform;

    @TableField("blockchain_cert_no")
    private String blockchainCertNo;

    @TableField("blockchain_block_height")
    private Long blockchainBlockHeight;

    @TableField("blockchain_node")
    private String blockchainNode;

    @TableField("signature")
    private String signature;

    @TableField("certificate_url")
    private String certificateUrl;

    @TableField("status")
    private Integer status;

    @TableField("fail_reason")
    private String failReason;

    @TableField("audit_status")
    private Integer auditStatus;

    @TableField("audit_remark")
    private String auditRemark;

    @TableField("audit_time")
    private LocalDateTime auditTime;

    @TableField("audit_by")
    private Long auditBy;

    @TableField("registered_at")
    private LocalDateTime registeredAt;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("creation_start_time")
    private LocalDateTime creationStartTime;

    @TableField("creation_end_time")
    private LocalDateTime creationEndTime;

    @TableField("evidence_chain")
    private String evidenceChain;

    @TableField("version")
    private Integer version;

    @TableField("parent_id")
    private Long parentId;

    @TableField("is_commercial")
    private Integer isCommercial;

    @TableField("license_type")
    private Integer licenseType;

    @TableField("license_text")
    private String licenseText;

    @TableField("watermark_enabled")
    private Integer watermarkEnabled;
}
