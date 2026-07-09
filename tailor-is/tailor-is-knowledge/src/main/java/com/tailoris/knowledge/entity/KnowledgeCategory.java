package com.tailoris.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_category")
public class KnowledgeCategory extends BaseEntity {

    @TableField("name")
    private String name;

    @TableField("parent_id")
    private Long parentId;

    @TableField("description")
    private String description;

    @TableField("icon")
    private String icon;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("article_count")
    private Integer articleCount;

    @TableField("status")
    private Integer status;
}