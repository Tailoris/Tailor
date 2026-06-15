package com.tailoris.merchant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tailoris.merchant.entity.MerchantDashboardStats;

import java.util.List;
import java.util.Map;

/**
 * 商家数据工作台服务接口 - MER-005.
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
public interface IMerchantDashboardStatsService extends IService<MerchantDashboardStats> {

    /**
     * 获取今日统计（汇总）
     */
    MerchantDashboardStats getTodayStats(Long merchantId);

    /**
     * 获取今日统计（指定店铺）
     */
    MerchantDashboardStats getTodayStats(Long merchantId, Long shopId);

    /**
     * 按日期范围查询（汇总）
     */
    List<MerchantDashboardStats> getStatsByDateRange(Long merchantId, String startDate, String endDate);

    /**
     * 按日期范围查询（指定店铺）
     */
    List<MerchantDashboardStats> getShopStatsByDateRange(Long merchantId, Long shopId,
                                                          String startDate, String endDate);

    /**
     * 看板汇总数据（汇总）
     */
    Map<String, Object> getDashboardSummary(Long merchantId);

    /**
     * 看板汇总数据（指定店铺）
     */
    Map<String, Object> getDashboardSummary(Long merchantId, Long shopId);

    /**
     * 趋势图数据（最近days天）
     */
    List<MerchantDashboardStats> getTrendData(Long merchantId, Long shopId, int days);

    /**
     * 刷新今日统计
     */
    MerchantDashboardStats refreshTodayStats(Long merchantId);

    /**
     * 刷新指定日期统计
     */
    MerchantDashboardStats refreshStats(Long merchantId, Long shopId, String statDate, Integer statType);
}
