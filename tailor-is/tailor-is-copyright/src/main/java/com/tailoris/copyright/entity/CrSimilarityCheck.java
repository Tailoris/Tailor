package com.tailoris.copyright.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cr_similarity_check")
public class CrSimilarityCheck extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("source_record_id")
    private Long sourceRecordId;

    @TableField("target_record_id")
    private Long targetRecordId;

    @TableField("target_url")
    private String targetUrl;

    @TableField("similarity_score")
    private BigDecimal similarityScore;

    @TableField("check_method")
    private String checkMethod;

    @TableField("check_engine")
    private String checkEngine;

    @TableField("check_cost_ms")
    private Long checkCostMs;

    @TableField("evidence_image_url")
    private String evidenceImageUrl;

    @TableField("check_time")
    private LocalDateTime checkTime;

    @TableField("is_infringement")
    private Integer isInfringement;

    @TableField("risk_level")
    private Integer riskLevel;
}
