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
@TableName("arbitration_record")
public class ArbitrationRecord extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("arbitration_no")
    private String arbitrationNo;

    @TableField("infringement_id")
    private Long infringementId;

    @TableField("report_no")
    private String reportNo;

    @TableField("arbitrator_id")
    private Long arbitratorId;

    @TableField("arbitrator_name")
    private String arbitratorName;

    @TableField("arbitrator_type")
    private Integer arbitratorType;

    @TableField("result")
    private Integer result;

    @TableField("result_description")
    private String resultDescription;

    @TableField("confidence_level")
    private Integer confidenceLevel;

    @TableField("evidence_analysis")
    private String evidenceAnalysis;

    @TableField("evidence")
    private String evidence;

    @TableField("penalty_recommendation")
    private String penaltyRecommendation;

    @TableField("reporter_appeal")
    private Integer reporterAppeal;

    @TableField("reporter_appeal_reason")
    private String reporterAppealReason;

    @TableField("reported_appeal")
    private Integer reportedAppeal;

    @TableField("reported_appeal_reason")
    private String reportedAppealReason;

    @TableField("final_result")
    private Integer finalResult;

    @TableField("closed_at")
    private LocalDateTime closedAt;
}
