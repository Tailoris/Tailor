package com.tailoris.ai.entity;

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
@TableName("body_size_data")
public class BodySizeData extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("size_name")
    private String sizeName;

    @TableField("height")
    private BigDecimal height;

    @TableField("weight")
    private BigDecimal weight;

    @TableField("shoulder_width")
    private BigDecimal shoulderWidth;

    @TableField("chest_circumference")
    private BigDecimal chestCircumference;

    @TableField("waist_circumference")
    private BigDecimal waistCircumference;

    @TableField("hip_circumference")
    private BigDecimal hipCircumference;

    @TableField("neck_circumference")
    private BigDecimal neckCircumference;

    @TableField("arm_length")
    private BigDecimal armLength;

    @TableField("sleeve_length")
    private BigDecimal sleeveLength;

    @TableField("waist_length")
    private BigDecimal waistLength;

    @TableField("inseam_length")
    private BigDecimal inseamLength;

    @TableField("body_type")
    private String bodyType;

    @TableField("gender")
    private Integer gender;

    @TableField("is_default")
    private Integer isDefault;
}
