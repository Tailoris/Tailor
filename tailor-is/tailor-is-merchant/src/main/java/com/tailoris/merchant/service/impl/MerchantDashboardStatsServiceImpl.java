package com.tailoris.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tailoris.merchant.entity.MerchantDashboardStats;
import com.tailoris.merchant.mapper.MerchantDashboardStatsMapper;
import com.tailoris.merchant.service.IMerchantDashboardStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商家数据工作台服务实现 - MER-005.
 *
 * <p>提供商家数据看板的查询与统计功能。</p>
 *
 * <h3>核心能力</h3>
 * <ul>
 *   <li>今日/历史数据查询</li>
 *   <li>数据汇总（核心指标聚合）</li>
 *   <li>趋势图数据（最近30天）</li>
 *   <li>店铺对比数据</li>
 *   <li>实时统计刷新</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantDashboardStatsServiceImpl extends ServiceImpl<MerchantDashboardStatsMapper, MerchantDashboardStats>
        implements IMerchantDashboardStatsService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public MerchantDashboardStats getTodayStats(Long merchantId) {
        return getTodayStats(merchantId, null);
    }

    @Override
    public MerchantDashboardStats getTodayStats(Long merchantId, Long shopId) {
        String today = LocalDate.now().format(DATE_FMT);
        LambdaQueryWrapper<MerchantDashboardStats> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantDashboardStats::getMerchantId, merchantId)
               .eq(MerchantDashboardStats::getStatDate, today)
               .eq(MerchantDashboardStats::getStatType, 1);
        if (shopId != null) {
            wrapper.eq(MerchantDashboardStats::getShopId, shopId);
        } else {
            wrapper.isNull(MerchantDashboardStats::getShopId);
        }
        wrapper.last("LIMIT 1");
        return getOne(wrapper);
    }

    @Override
    public List<MerchantDashboardStats> getStatsByDateRange(Long merchantId, String startDate, String endDate) {
        return baseMapper.selectByDateRange(merchantId, null, startDate, endDate);
    }

    @Override
    public List<MerchantDashboardStats> getShopStatsByDateRange(Long merchantId, Long shopId,
                                                                String startDate, String endDate) {
        if (shopId == null) {
            return baseMapper.selectByDateRange(merchantId, null, startDate, endDate);
        }
        return baseMapper.selectShopByDateRange(merchantId, shopId, startDate, endDate);
    }

    @Override
    public Map<String, Object> getDashboardSummary(Long merchantId) {
        return getDashboardSummary(merchantId, null);
    }

    @Override
    public Map<String, Object> getDashboardSummary(Long merchantId, Long shopId) {
        Map<String, Object> summary = new HashMap<>();

        MerchantDashboardStats today = getTodayStats(merchantId, shopId);
        MerchantDashboardStats yesterday = getYesterdayStats(merchantId, shopId);
        MerchantDashboardStats monthAggregate = getMonthAggregate(merchantId, shopId);

        // 今日数据
        if (today != null) {
            summary.put("todayPv", today.getPvCount());
            summary.put("todayUv", today.getUvCount());
            summary.put("todayProductViews", today.getProductViewCount());
            summary.put("todayFollows", today.getShopFollowCount());
            summary.put("todayCartAdds", today.getCartAddCount());
            summary.put("todayOrders", today.getOrderCount());
            summary.put("todayOrderAmount", today.getOrderAmount());
            summary.put("todayPaidOrders", today.getPaidOrderCount());
            summary.put("todayPaidAmount", today.getPaidOrderAmount());
            summary.put("todayRefundCount", today.getRefundCount());
            summary.put("todayRefundAmount", today.getRefundAmount());
            summary.put("todayConversionRate", today.getConversionRate());
        } else {
            fillEmptyToday(summary);
        }

        // 环比（与昨日对比）
        if (yesterday != null && today != null) {
            summary.put("yesterdayOrders", yesterday.getOrderCount());
            summary.put("yesterdayOrderAmount", yesterday.getOrderAmount());
            summary.put("yesterdayUv", yesterday.getUvCount());
            summary.put("yesterdayPaidAmount", yesterday.getPaidOrderAmount());
            summary.put("ordersGrowth", calcGrowth(
                    BigDecimal.valueOf(num(today.getOrderCount())), BigDecimal.valueOf(num(yesterday.getOrderCount()))));
            summary.put("amountGrowth", calcGrowth(
                    num(today.getPaidOrderAmount()), num(yesterday.getPaidOrderAmount())));
            summary.put("uvGrowth", calcGrowth(
                    BigDecimal.valueOf(num(today.getUvCount())), BigDecimal.valueOf(num(yesterday.getUvCount()))));
        }

        // 月度累计
        if (monthAggregate != null) {
            summary.put("monthOrders", monthAggregate.getOrderCount());
            summary.put("monthOrderAmount", monthAggregate.getOrderAmount());
            summary.put("monthPaidAmount", monthAggregate.getPaidOrderAmount());
            summary.put("monthRefundCount", monthAggregate.getRefundCount());
        }

        return summary;
    }

    @Override
    public List<MerchantDashboardStats> getTrendData(Long merchantId, Long shopId, int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1);
        return getShopStatsByDateRange(merchantId, shopId,
                start.format(DATE_FMT), end.format(DATE_FMT));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantDashboardStats refreshTodayStats(Long merchantId) {
        return refreshStats(merchantId, null, LocalDate.now().format(DATE_FMT), 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantDashboardStats refreshStats(Long merchantId, Long shopId, String statDate, Integer statType) {
        LambdaQueryWrapper<MerchantDashboardStats> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantDashboardStats::getMerchantId, merchantId)
               .eq(MerchantDashboardStats::getStatDate, statDate)
               .eq(MerchantDashboardStats::getStatType, statType);
        if (shopId != null) {
            wrapper.eq(MerchantDashboardStats::getShopId, shopId);
        } else {
            wrapper.isNull(MerchantDashboardStats::getShopId);
        }
        wrapper.last("LIMIT 1");

        MerchantDashboardStats existing = getOne(wrapper);

        // 实际项目中应从订单/支付/浏览等微服务聚合数据
        // 此处提供刷新框架，统计逻辑由后续定时任务调用
        MerchantDashboardStats stats = existing != null ? existing : new MerchantDashboardStats();
        stats.setMerchantId(merchantId);
        stats.setShopId(shopId);
        stats.setStatDate(statDate);
        stats.setStatType(statType);

        if (existing != null) {
            updateById(stats);
        } else {
            stats.setPvCount(0L);
            stats.setUvCount(0L);
            stats.setProductViewCount(0L);
            stats.setShopFollowCount(0L);
            stats.setCartAddCount(0L);
            stats.setOrderCount(0L);
            stats.setOrderAmount(BigDecimal.ZERO);
            stats.setPaidOrderCount(0L);
            stats.setPaidOrderAmount(BigDecimal.ZERO);
            stats.setRefundCount(0L);
            stats.setRefundAmount(BigDecimal.ZERO);
            stats.setConversionRate(BigDecimal.ZERO);
            save(stats);
        }
        return stats;
    }

    // ============================================================
    // 私有方法
    // ============================================================

    private MerchantDashboardStats getYesterdayStats(Long merchantId, Long shopId) {
        String yesterday = LocalDate.now().minusDays(1).format(DATE_FMT);
        LambdaQueryWrapper<MerchantDashboardStats> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantDashboardStats::getMerchantId, merchantId)
               .eq(MerchantDashboardStats::getStatDate, yesterday)
               .eq(MerchantDashboardStats::getStatType, 1);
        if (shopId != null) {
            wrapper.eq(MerchantDashboardStats::getShopId, shopId);
        } else {
            wrapper.isNull(MerchantDashboardStats::getShopId);
        }
        wrapper.last("LIMIT 1");
        return getOne(wrapper);
    }

    private MerchantDashboardStats getMonthAggregate(Long merchantId, Long shopId) {
        String firstDay = LocalDate.now().withDayOfMonth(1).format(DATE_FMT);
        String today = LocalDate.now().format(DATE_FMT);
        // 优先用 stat_type=3 的月统计
        LambdaQueryWrapper<MerchantDashboardStats> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantDashboardStats::getMerchantId, merchantId)
               .eq(MerchantDashboardStats::getStatType, 3)
               .ge(MerchantDashboardStats::getStatDate, firstDay)
               .le(MerchantDashboardStats::getStatDate, today);
        if (shopId != null) {
            wrapper.eq(MerchantDashboardStats::getShopId, shopId);
        } else {
            wrapper.isNull(MerchantDashboardStats::getShopId);
        }
        wrapper.last("LIMIT 1");
        MerchantDashboardStats monthStats = getOne(wrapper);
        if (monthStats != null) {
            return monthStats;
        }
        // 兜底：按日聚合
        List<MerchantDashboardStats> dayStats = getShopStatsByDateRange(merchantId, shopId, firstDay, today);
        if (dayStats.isEmpty()) {
            return null;
        }
        MerchantDashboardStats agg = new MerchantDashboardStats();
        long orders = 0;
        BigDecimal orderAmount = BigDecimal.ZERO;
        long paidOrders = 0;
        BigDecimal paidAmount = BigDecimal.ZERO;
        long refunds = 0;
        for (MerchantDashboardStats s : dayStats) {
            orders += num(s.getOrderCount());
            orderAmount = orderAmount.add(s.getOrderAmount() == null ? BigDecimal.ZERO : s.getOrderAmount());
            paidOrders += num(s.getPaidOrderCount());
            paidAmount = paidAmount.add(s.getPaidOrderAmount() == null ? BigDecimal.ZERO : s.getPaidOrderAmount());
            refunds += num(s.getRefundCount());
        }
        agg.setOrderCount(orders);
        agg.setOrderAmount(orderAmount);
        agg.setPaidOrderCount(paidOrders);
        agg.setPaidOrderAmount(paidAmount);
        agg.setRefundCount(refunds);
        return agg;
    }

    private void fillEmptyToday(Map<String, Object> summary) {
        summary.put("todayPv", 0L);
        summary.put("todayUv", 0L);
        summary.put("todayProductViews", 0L);
        summary.put("todayFollows", 0L);
        summary.put("todayCartAdds", 0L);
        summary.put("todayOrders", 0L);
        summary.put("todayOrderAmount", BigDecimal.ZERO);
        summary.put("todayPaidOrders", 0L);
        summary.put("todayPaidAmount", BigDecimal.ZERO);
        summary.put("todayRefundCount", 0L);
        summary.put("todayRefundAmount", BigDecimal.ZERO);
        summary.put("todayConversionRate", BigDecimal.ZERO);
    }

    private long num(Long v) {
        return v == null ? 0L : v;
    }

    private BigDecimal num(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private BigDecimal calcGrowth(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal("100") : BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .multiply(new BigDecimal("100"))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }
}
