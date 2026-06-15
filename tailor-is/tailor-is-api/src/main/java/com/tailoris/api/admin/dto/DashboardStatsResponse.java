package com.tailoris.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "仪表盘统计响应")
public class DashboardStatsResponse {

    @Schema(description = "用户总数")
    private Long userCount;

    @Schema(description = "商家总数")
    private Long merchantCount;

    @Schema(description = "订单总数")
    private Long orderCount;

    @Schema(description = "今日营收")
    private BigDecimal todayRevenue;

    @Schema(description = "今日订单数")
    private Long todayOrders;

    @Schema(description = "待审核数量")
    private Long pendingAuditCount;
}
