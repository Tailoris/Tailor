package com.tailoris.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "提现请求")
public class WithdrawRequest {

    @NotNull(message = "提现金额不能为空")
    @DecimalMin(value = "0.01", message = "提现金额必须大于0")
    @Schema(description = "提现金额")
    private BigDecimal amount;

    @NotBlank(message = "开户银行不能为空")
    @Schema(description = "开户银行名称")
    private String bankName;

    @Schema(description = "开户支行")
    private String bankBranch;

    @NotBlank(message = "银行账号不能为空")
    @Schema(description = "银行账号")
    private String bankAccount;

    @NotBlank(message = "开户人姓名不能为空")
    @Schema(description = "开户人姓名")
    private String accountName;
}
