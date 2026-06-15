package com.tailoris.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("message_template")
public class MessageTemplate extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("code")
    private String code;

    @TableField("type")
    private Integer type;

    @TableField("title_template")
    private String titleTemplate;

    @TableField("content_template")
    private String contentTemplate;

    @TableField("params")
    private String params;

    @TableField("channels")
    private String channels;

    @TableField("scene")
    private String scene;

    @TableField("status")
    private Integer status;

    @TableField("sort")
    private Integer sort;
}
