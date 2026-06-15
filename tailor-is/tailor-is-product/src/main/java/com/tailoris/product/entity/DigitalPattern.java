package com.tailoris.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 数字纸样实体 - PRD-008.
 *
 * <p>数字纸样是服装样板文件，用户购买后可下载使用。
 * 关键字段：文件URL、文件大小、格式（SVG/PDF/DXF）、授权类型。</p>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Data
@TableName("digital_pattern")
public class DigitalPattern implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("product_id")
    private Long productId;

    @TableField("file_key")
    private String fileKey;

    @TableField("file_url")
    private String fileUrl;

    @TableField("preview_url")
    private String previewUrl;

    @TableField("file_size")
    private Long fileSize;

    @TableField("file_format")
    private String fileFormat;   // SVG/PDF/DXF/AI

    @TableField("pattern_type")
    private String patternType;  // 裤子/上衣/裙子/连衣裙/...

    @TableField("difficulty")
    private Integer difficulty;  // 1=入门 2=中级 3=高级

    @TableField("fabric_requirement")
    private String fabricRequirement;

    @TableField("license_type")
    private String licenseType;  // PERSONAL=个人使用 / COMMERCIAL=商业使用

    @TableField("license_duration_days")
    private Integer licenseDurationDays;  // 0=永久

    @TableField("version")
    private String version;     // v1.0/v1.1

    @TableField("download_count")
    private Integer downloadCount;

    @TableField("preview_count")
    private Integer previewCount;

    @TableField("design_price")
    private BigDecimal designPrice;  // 设计版权费

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
