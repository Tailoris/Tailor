package com.tailoris.copyright.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "版权授权请求")
public class AuthorizationRequest {

    @NotNull(message = "版权登记ID不能为空")
    @Schema(description = "版权登记ID")
    private Long copyrightId;

    @NotNull(message = "被授权方ID不能为空")
    @Schema(description = "被授权方用户ID")
    private Long licenseeId;

    @NotNull(message = "授权类型不能为空")
    @Schema(description = "授权类型：1-独占授权，2-排他授权，3-普通授权")
    private Integer licenseType;

    @NotNull(message = "授权范围不能为空")
    @Schema(description = "授权范围：1-生产使用权，2-销售权，3-修改权，4-全权")
    private Integer scope;

    @Schema(description = "授权使用的商品范围(JSON数组)")
    private String authorizedProducts;

    @NotNull(message = "授权生效日期不能为空")
    @Schema(description = "授权生效日期")
    private LocalDate startDate;

    @NotNull(message = "授权到期日期不能为空")
    @Schema(description = "授权到期日期")
    private LocalDate endDate;

    @Schema(description = "授权费用(元)")
    private BigDecimal licenseFee;

    @Schema(description = "版税分成比例")
    private BigDecimal royaltyRate;

    @Schema(description = "备注说明")
    private String remark;
}
