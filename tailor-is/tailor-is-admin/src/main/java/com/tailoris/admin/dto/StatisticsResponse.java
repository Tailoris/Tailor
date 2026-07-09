package com.tailoris.admin.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StatisticsResponse {

    private Long totalOrders;

    private Long totalUsers;

    private Long totalProducts;

    private BigDecimal totalRevenue;

    private Long activeUsersToday;

    private Long ordersToday;

    private BigDecimal revenueToday;

    private Double orderGrowthRate;

    private Double revenueGrowthRate;

    private Double userGrowthRate;
}