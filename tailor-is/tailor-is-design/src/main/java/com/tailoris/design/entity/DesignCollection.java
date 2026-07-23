package com.tailoris.design.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("design_collection")
public class DesignCollection extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("design_id")
    private Long designId;

    @TableField("user_id")
    private Long userId;
}