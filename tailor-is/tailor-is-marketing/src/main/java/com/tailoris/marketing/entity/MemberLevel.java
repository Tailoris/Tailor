package com.tailoris.marketing.entity;

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
@TableName("member_level")
public class MemberLevel extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("level_name")
    private String levelName;

    @TableField("level_code")
    private String levelCode;

    @TableField("level_value")
    private Integer levelValue;

    @TableField("min_points")
    private Integer minPoints;

    @TableField("max_points")
    private Integer maxPoints;

    @TableField("discount_rate")
    private BigDecimal discountRate;

    @TableField("privileges")
    private String privileges;

    @TableField("icon")
    private String icon;

    @TableField("color")
    private String color;

    @TableField("description")
    private String description;

    @TableField("sort")
    private Integer sort;

    @TableField("status")
    private Integer status;
}
