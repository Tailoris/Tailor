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
@TableName("pattern_version")
public class PatternVersion extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("pattern_id")
    private Long patternId;

    @TableField("version_no")
    private Integer versionNo;

    @TableField("version_name")
    private String versionName;

    @TableField("pattern_data")
    private String patternData;

    @TableField("change_description")
    private String changeDescription;

    @TableField("parameters_snapshot")
    private String parametersSnapshot;

    @TableField("is_current")
    private Integer isCurrent;
}
