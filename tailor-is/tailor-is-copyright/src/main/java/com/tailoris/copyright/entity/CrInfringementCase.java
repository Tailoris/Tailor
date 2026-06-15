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
@TableName("cr_infringement_case")
public class CrInfringementCase extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("case_no")
    private String caseNo;

    @TableField("record_id")
    private Long recordId;

    @TableField("copyright_user_id")
    private Long copyrightUserId;

    @TableField("infringer_user_id")
    private Long infringerUserId;

    @TableField("infringer_name")
    private String infringerName;

    @TableField("infringer_contact")
    private String infringerContact;

    @TableField("infringement_source")
    private String infringementSource;

    @TableField("discovered_at")
    private LocalDateTime discoveredAt;

    @TableField("infringement_type")
    private Integer infringementType;

    @TableField("similarity_score")
    private BigDecimal similarityScore;

    @TableField("evidence_chain")
    private String evidenceChain;

    @TableField("evidence_files")
    private String evidenceFiles;

    @TableField("encrypted_evidence_key")
    private String encryptedEvidenceKey;

    @TableField("status")
    private Integer status;

    @TableField("arbitration_deadline")
    private LocalDateTime arbitrationDeadline;

    @TableField("arbitration_result")
    private String arbitrationResult;

    @TableField("arbitrator_id")
    private Long arbitratorId;

    @TableField("arbitration_at")
    private LocalDateTime arbitrationAt;

    @TableField("court_name")
    private String courtName;

    @TableField("court_case_no")
    private String courtCaseNo;

    @TableField("lawyer_name")
    private String lawyerName;

    @TableField("lawyer_contact")
    private String lawyerContact;

    @TableField("compensation")
    private BigDecimal compensation;

    @TableField("closed_at")
    private LocalDateTime closedAt;
}
