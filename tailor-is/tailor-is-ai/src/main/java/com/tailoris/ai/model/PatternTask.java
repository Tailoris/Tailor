package com.tailoris.ai.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pattern_task")
public class PatternTask extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long taskId;

    @TableField("user_id")
    private Long userId;

    @TableField("pattern_type")
    private Integer patternType;

    @TableField("parameters")
    private String parameters;

    @TableField("status")
    private String status;

    @TableField("progress")
    private Integer progress;

    @TableField("result")
    private String result;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}