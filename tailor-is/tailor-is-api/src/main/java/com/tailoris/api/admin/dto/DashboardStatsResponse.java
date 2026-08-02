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

    @Schema(description = "待审核数量（合并，兼容旧字段）")
    private Long pendingAuditCount;

    @Schema(description = "今日新增用户数")
    private Long todayNewUsers;

    @Schema(description = "昨日订单数")
    private Long yesterdayOrders;

    @Schema(description = "昨日营收")
    private BigDecimal yesterdayRevenue;

    @Schema(description = "昨日新增用户数")
    private Long yesterdayNewUsers;

    @Schema(description = "订单增长率（百分比）")
    private BigDecimal ordersGrowth;

    @Schema(description = "营收增长率（百分比）")
    private BigDecimal revenueGrowth;

    @Schema(description = "用户增长率（百分比）")
    private BigDecimal userGrowth;

    @Schema(description = "待审核商家数")
    private Long pendingMerchantCount;

    @Schema(description = "待审核商品数")
    private Long pendingProductCount;

    @Schema(description = "待处理售后工单数")
    private Long pendingAfterSaleCount;

    @Schema(description = "商品总数")
    private Long totalProducts;

    @Schema(description = "纸样总数")
    private Long totalPatterns;
}
