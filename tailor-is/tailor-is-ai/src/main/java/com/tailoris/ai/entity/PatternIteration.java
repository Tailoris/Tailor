package com.tailoris.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pattern_iteration")
public class PatternIteration extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("pattern_id")
    private Long patternId;

    @TableField("iteration_type")
    private Integer iterationType;

    @TableField("old_parameters")
    private String oldParameters;

    @TableField("new_parameters")
    private String newParameters;

    @TableField("change_reason")
    private String changeReason;

    @TableField("change_result")
    private String changeResult;
}
