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
@TableName("pattern_record")
public class PatternRecord extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("pattern_name")
    private String patternName;

    @TableField("pattern_type")
    private Integer patternType;

    @TableField("body_size_id")
    private Long bodySizeId;

    @TableField("parameters")
    private String parameters;

    @TableField("pattern_data")
    private String patternData;

    @TableField("pattern_file_url")
    private String patternFileUrl;

    @TableField("thumbnail_url")
    private String thumbnailUrl;

    @TableField("export_format")
    private String exportFormat;

    @TableField("check_result")
    private String checkResult;

    @TableField("check_status")
    private Integer checkStatus;

    @TableField("status")
    private Integer status;

    @TableField("version")
    private Integer version;
}
