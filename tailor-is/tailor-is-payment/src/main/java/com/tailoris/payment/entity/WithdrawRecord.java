package com.tailoris.payment.entity;

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
@TableName("withdraw_record")
public class WithdrawRecord extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("withdraw_no")
    private String withdrawNo;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("fee")
    private BigDecimal fee;

    @TableField("actual_amount")
    private BigDecimal actualAmount;

    @TableField("bank_name")
    private String bankName;

    @TableField("bank_branch")
    private String bankBranch;

    @TableField("bank_account")
    private String bankAccount;

    @TableField("account_name")
    private String accountName;

    @TableField("status")
    private Integer status;

    @TableField("fail_reason")
    private String failReason;

    @TableField("channel_transaction_id")
    private String channelTransactionId;

    @TableField("audit_by")
    private Long auditBy;

    @TableField("audit_time")
    private LocalDateTime auditTime;

    @TableField("audit_remark")
    private String auditRemark;

    @TableField("processed_at")
    private LocalDateTime processedAt;
}
