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
@TableName("cr_certificate_file")
public class CrCertificateFile extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("record_id")
    private Long recordId;

    @TableField("cert_no")
    private String certNo;

    @TableField("file_url")
    private String fileUrl;

    @TableField("qr_code_url")
    private String qrCodeUrl;

    @TableField("qr_content")
    private String qrContent;

    @TableField("signature")
    private String signature;

    @TableField("signed_at")
    private LocalDateTime signedAt;

    @TableField("file_size")
    private Long fileSize;

    @TableField("page_count")
    private Integer pageCount;

    @TableField("download_count")
    private Integer downloadCount;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("status")
    private Integer status;
}
