package com.tailoris.admin.service;

import com.tailoris.api.admin.dto.DashboardOverviewResponse;
import com.tailoris.api.admin.dto.DashboardStatsResponse;
import com.tailoris.api.admin.dto.DashboardTrendResponse;

public interface AdminDashboardService {

    /**
     * 获取仪表盘核心统计数据（含昨日对比与增长率）。
     *
     * @return 统计数据响应
     */
    DashboardStatsResponse getDashboardStats();

    /**
     * 获取近 N 天营收与订单趋势数据。
     *
     * @param days 天数（1~90，超出区间自动校正）
     * @return 趋势数据响应
     */
    DashboardTrendResponse getDashboardTrend(int days);

    /**
     * 获取仪表盘概览数据（订单状态分布 + 商家/商品排行）。
     *
     * @return 概览数据响应
     */
    DashboardOverviewResponse getDashboardOverview();
}
