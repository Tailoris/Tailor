package com.tailoris.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.merchant.entity.MerchantDashboardStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商家数据工作台统计Mapper - MER-005.
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Mapper
public interface MerchantDashboardStatsMapper extends BaseMapper<MerchantDashboardStats> {

    /**
     * 按日期范围查询全店汇总统计数据.
     */
    @Select("SELECT * FROM merchant_dashboard_stats " +
            "WHERE merchant_id = #{merchantId} " +
            "  AND shop_id IS NULL " +
            "  AND stat_date BETWEEN #{startDate} AND #{endDate} " +
            "  AND stat_type = 1 " +
            "  AND deleted = 0 " +
            "ORDER BY stat_date ASC")
    List<MerchantDashboardStats> selectByDateRange(
            @Param("merchantId") Long merchantId,
            @Param("shopId") Long shopId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    /**
     * 按日期范围查询指定店铺统计数据.
     */
    @Select("SELECT * FROM merchant_dashboard_stats " +
            "WHERE merchant_id = #{merchantId} " +
            "  AND shop_id = #{shopId} " +
            "  AND stat_date BETWEEN #{startDate} AND #{endDate} " +
            "  AND stat_type = 1 " +
            "  AND deleted = 0 " +
            "ORDER BY stat_date ASC")
    List<MerchantDashboardStats> selectShopByDateRange(
            @Param("merchantId") Long merchantId,
            @Param("shopId") Long shopId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    /**
     * 获取店铺最近N天统计.
     */
    @Select("SELECT * FROM merchant_dashboard_stats " +
            "WHERE merchant_id = #{merchantId} " +
            "  AND shop_id = #{shopId} " +
            "  AND stat_type = 1 " +
            "  AND deleted = 0 " +
            "ORDER BY stat_date DESC LIMIT #{limit}")
    List<MerchantDashboardStats> selectRecentByShop(
            @Param("merchantId") Long merchantId,
            @Param("shopId") Long shopId,
            @Param("limit") int limit);
}
