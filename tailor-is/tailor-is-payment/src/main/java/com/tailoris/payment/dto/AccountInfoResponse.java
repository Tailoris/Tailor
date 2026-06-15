package com.tailoris.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "账户信息响应")
public class AccountInfoResponse {

    @Schema(description = "用户ID/商家ID")
    private Long accountId;

    @Schema(description = "账户类型：1-用户，2-商家")
    private Integer accountType;

    @Schema(description = "可用余额")
    private BigDecimal balance;

    @Schema(description = "可提现余额")
    private BigDecimal withdrawableBalance;

    @Schema(description = "冻结金额")
    private BigDecimal frozenAmount;

    @Schema(description = "待结算金额")
    private BigDecimal pendingAmount;

    @Schema(description = "累计收入")
    private BigDecimal totalIncome;

    @Schema(description = "累计支出")
    private BigDecimal totalExpense;

    @Schema(description = "累计提现")
    private BigDecimal totalWithdraw;

    @Schema(description = "累计结算")
    private BigDecimal totalSettlement;

    @Schema(description = "积分")
    private Integer points;
}
