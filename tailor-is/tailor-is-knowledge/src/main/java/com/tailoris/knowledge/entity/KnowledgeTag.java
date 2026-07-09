package com.tailoris.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_tag")
public class KnowledgeTag extends BaseEntity {

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("article_count")
    private Integer articleCount;

    @TableField("status")
    private Integer status;
}