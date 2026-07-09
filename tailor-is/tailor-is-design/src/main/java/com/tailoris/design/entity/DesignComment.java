package com.tailoris.design.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("design_comment")
public class DesignComment extends BaseEntity {

    @TableField("design_id")
    private Long designId;

    @TableField("user_id")
    private Long userId;

    @TableField("content")
    private String content;

    @TableField("parent_id")
    private Long parentId;

    @TableField("like_count")
    private Integer likeCount;

    @TableField("is_top")
    private Integer isTop;
}