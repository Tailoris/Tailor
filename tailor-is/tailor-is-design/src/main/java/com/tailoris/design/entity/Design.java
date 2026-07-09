package com.tailoris.design.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("design")
public class Design extends BaseEntity {

    @TableField("design_no")
    private String designNo;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("cover_image")
    private String coverImage;

    @TableField("design_type")
    private String designType;

    @TableField("style_category")
    private String styleCategory;

    @TableField("color")
    private String color;

    @TableField("material")
    private String material;

    @TableField("size_range")
    private String sizeRange;

    @TableField("design_data")
    private String designData;

    @TableField("difficulty")
    private String difficulty;

    @TableField("work_time")
    private Integer workTime;

    @TableField("price")
    private BigDecimal price;

    @TableField("is_free")
    private Integer isFree;

    @TableField("status")
    private Integer status;

    @TableField("like_count")
    private Long likeCount;

    @TableField("view_count")
    private Long viewCount;

    @TableField("user_id")
    private Long userId;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("pattern_id")
    private Long patternId;

    @TableField("copyright_status")
    private Integer copyrightStatus;
}