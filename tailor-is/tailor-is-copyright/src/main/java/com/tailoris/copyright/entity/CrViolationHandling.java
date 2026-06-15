package com.tailoris.copyright.entity;

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
@TableName("cr_violation_handling")
public class CrViolationHandling extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("record_id")
    private Long recordId;

    @TableField("inspection_id")
    private Long inspectionId;

    @TableField("violation_type")
    private Integer violationType;

    @TableField("violation_level")
    private Integer violationLevel;

    @TableField("description")
    private String description;

    @TableField("evidence_urls")
    private String evidenceUrls;

    @TableField("handle_type")
    private Integer handleType;

    @TableField("handle_remark")
    private String handleRemark;

    @TableField("handler_id")
    private Long handlerId;

    @TableField("handle_time")
    private LocalDateTime handleTime;

    @TableField("status")
    private Integer status;
}
