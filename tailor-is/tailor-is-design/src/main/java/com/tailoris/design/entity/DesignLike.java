package com.tailoris.design.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("design_like")
public class DesignLike extends BaseEntity {

    @TableField("design_id")
    private Long designId;

    @TableField("user_id")
    private Long userId;
}