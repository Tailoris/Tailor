package com.tailoris.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.merchant.entity.MerchantDashboardStats;
import com.tailoris.merchant.mapper.MerchantDashboardStatsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("商家数据工作台服务单元测试")
@ExtendWith(MockitoExtension.class)
class MerchantDashboardStatsServiceImplTest {

    @Mock
    private MerchantDashboardStatsMapper dashboardMapper;

    private MerchantDashboardStatsServiceImpl dashboardService;

    private MerchantDashboardStats stats;

    @BeforeEach
    void setUp() {
        dashboardService = new MerchantDashboardStatsServiceImpl();
        ReflectionTestUtils.setField(dashboardService, "baseMapper", dashboardMapper);

        stats = new MerchantDashboardStats();
        stats.setId(1L);
        stats.setMerchantId(1L);
        stats.setShopId(1L);
        stats.setStatDate(LocalDate.now().toString());
        stats.setStatType(1);
        stats.setPvCount(1000L);
        stats.setUvCount(500L);
        stats.setProductViewCount(800L);
        stats.setShopFollowCount(50L);
        stats.setCartAddCount(100L);
        stats.setOrderCount(30L);
        stats.setOrderAmount(new BigDecimal("5000.00"));
        stats.setPaidOrderCount(25L);
        stats.setPaidOrderAmount(new BigDecimal("4200.00"));
        stats.setRefundCount(2L);
        stats.setRefundAmount(new BigDecimal("300.00"));
        stats.setConversionRate(new BigDecimal("0.06"));
    }

    @Test
    @DisplayName("获取今日统计：成功返回")
    void testGetTodayStats_Success() {
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(stats);

        MerchantDashboardStats result = dashboardService.getTodayStats(1L);

        assertNotNull(result);
        assertEquals(1L, result.getMerchantId());
        assertEquals(1000L, result.getPvCount());
    }

    @Test
    @DisplayName("获取今日统计（指定店铺）：成功返回")
    void testGetTodayStatsByShop_Success() {
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(stats);

        MerchantDashboardStats result = dashboardService.getTodayStats(1L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getShopId());
    }

    @Test
    @DisplayName("按日期范围查询统计：成功返回")
    void testGetStatsByDateRange_Success() {
        List<MerchantDashboardStats> list = Arrays.asList(stats);
        when(dashboardMapper.selectByDateRange(eq(1L), isNull(), anyString(), anyString())).thenReturn(list);

        List<MerchantDashboardStats> result = dashboardService.getStatsByDateRange(1L, "2026-01-01", "2026-12-31");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("按日期范围查询店铺统计：shopId为null返回全店数据")
    void testGetShopStatsByDateRange_NullShopId() {
        List<MerchantDashboardStats> list = Arrays.asList(stats);
        when(dashboardMapper.selectByDateRange(eq(1L), isNull(), anyString(), anyString())).thenReturn(list);

        List<MerchantDashboardStats> result = dashboardService.getShopStatsByDateRange(1L, null, "2026-01-01", "2026-12-31");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("按日期范围查询店铺统计：指定shopId返回店铺数据")
    void testGetShopStatsByDateRange_WithShopId() {
        List<MerchantDashboardStats> list = Arrays.asList(stats);
        when(dashboardMapper.selectShopByDateRange(eq(1L), eq(1L), anyString(), anyString())).thenReturn(list);

        List<MerchantDashboardStats> result = dashboardService.getShopStatsByDateRange(1L, 1L, "2026-01-01", "2026-12-31");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("获取工作台汇总：今日数据存在")
    void testGetDashboardSummary_WithTodayData() {
        MerchantDashboardStats yesterday = new MerchantDashboardStats();
        yesterday.setOrderCount(20L);
        yesterday.setOrderAmount(new BigDecimal("3000.00"));
        yesterday.setUvCount(400L);
        yesterday.setPaidOrderAmount(new BigDecimal("2500.00"));

        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean()))
            .thenReturn(stats)       // today
            .thenReturn(yesterday)   // yesterday
            .thenReturn(null);       // month aggregate

        // For the fallback month aggregate
        List<MerchantDashboardStats> dayStats = new ArrayList<>();
        MerchantDashboardStats day1 = new MerchantDashboardStats();
        day1.setOrderCount(10L);
        day1.setOrderAmount(new BigDecimal("1000.00"));
        day1.setPaidOrderCount(8L);
        day1.setPaidOrderAmount(new BigDecimal("800.00"));
        day1.setRefundCount(1L);
        dayStats.add(day1);
        when(dashboardMapper.selectByDateRange(eq(1L), isNull(), anyString(), anyString())).thenReturn(dayStats);

        Map<String, Object> result = dashboardService.getDashboardSummary(1L);

        assertNotNull(result);
        assertEquals(1000L, result.get("todayPv"));
        assertEquals(500L, result.get("todayUv"));
        assertEquals(30L, result.get("todayOrders"));
        assertNotNull(result.get("ordersGrowth"));
        assertNotNull(result.get("monthOrders"));
    }

    @Test
    @DisplayName("获取工作台汇总：今日数据不存在返回默认值")
    void testGetDashboardSummary_NoTodayData() {
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(null);
        when(dashboardMapper.selectByDateRange(eq(1L), isNull(), anyString(), anyString())).thenReturn(Collections.emptyList());

        Map<String, Object> result = dashboardService.getDashboardSummary(1L);

        assertNotNull(result);
        assertEquals(0L, result.get("todayPv"));
        assertEquals(0L, result.get("todayUv"));
        assertEquals(0L, result.get("todayOrders"));
        assertEquals(BigDecimal.ZERO, result.get("todayOrderAmount"));
    }

    @Test
    @DisplayName("获取工作台汇总（指定店铺）：成功返回")
    void testGetDashboardSummaryByShop_Success() {
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(stats);

        Map<String, Object> result = dashboardService.getDashboardSummary(1L, 1L);

        assertNotNull(result);
        assertEquals(1000L, result.get("todayPv"));
    }

    @Test
    @DisplayName("获取趋势数据：成功返回")
    void testGetTrendData_Success() {
        List<MerchantDashboardStats> list = Arrays.asList(stats);
        when(dashboardMapper.selectByDateRange(eq(1L), isNull(), anyString(), anyString())).thenReturn(list);

        List<MerchantDashboardStats> result = dashboardService.getTrendData(1L, null, 30);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("获取趋势数据（指定店铺）：成功返回")
    void testGetTrendDataByShop_Success() {
        List<MerchantDashboardStats> list = Arrays.asList(stats);
        when(dashboardMapper.selectShopByDateRange(eq(1L), eq(1L), anyString(), anyString())).thenReturn(list);

        List<MerchantDashboardStats> result = dashboardService.getTrendData(1L, 1L, 7);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("刷新今日统计：已存在则更新")
    void testRefreshTodayStats_Exists() {
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(stats);
        when(dashboardMapper.updateById(any(MerchantDashboardStats.class))).thenReturn(1);

        MerchantDashboardStats result = dashboardService.refreshTodayStats(1L);

        assertNotNull(result);
        assertEquals(1L, result.getMerchantId());
    }

    @Test
    @DisplayName("刷新今日统计：不存在则创建")
    void testRefreshTodayStats_NotExists() {
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(null);
        when(dashboardMapper.insert(any(MerchantDashboardStats.class))).thenReturn(1);

        MerchantDashboardStats result = dashboardService.refreshTodayStats(1L);

        assertNotNull(result);
        assertEquals(1L, result.getMerchantId());
        assertEquals(0L, result.getPvCount());
        assertEquals(BigDecimal.ZERO, result.getOrderAmount());
    }

    @Test
    @DisplayName("刷新统计（指定日期和类型）：成功刷新")
    void testRefreshStats_Success() {
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(stats);
        when(dashboardMapper.updateById(any(MerchantDashboardStats.class))).thenReturn(1);

        MerchantDashboardStats result = dashboardService.refreshStats(1L, 1L, "2026-06-14", 1);

        assertNotNull(result);
        assertEquals("2026-06-14", result.getStatDate());
        assertEquals(1, result.getStatType());
    }

    @Test
    @DisplayName("刷新统计（shopId为null）：成功刷新")
    void testRefreshStats_NullShopId() {
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(null);
        when(dashboardMapper.insert(any(MerchantDashboardStats.class))).thenReturn(1);

        MerchantDashboardStats result = dashboardService.refreshStats(1L, null, "2026-06-14", 1);

        assertNotNull(result);
        assertNull(result.getShopId());
    }

    @Test
    @DisplayName("月度汇总：有stat_type=3的月统计数据")
    void testGetMonthAggregate_WithMonthStats() {
        MerchantDashboardStats monthStats = new MerchantDashboardStats();
        monthStats.setOrderCount(500L);
        monthStats.setOrderAmount(new BigDecimal("80000.00"));
        monthStats.setPaidOrderCount(400L);
        monthStats.setPaidOrderAmount(new BigDecimal("70000.00"));
        monthStats.setRefundCount(20L);

        // today returns null, yesterday returns null, month stats returns data
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean()))
            .thenReturn(null)    // today
            .thenReturn(null)    // yesterday
            .thenReturn(monthStats);  // month aggregate

        Map<String, Object> result = dashboardService.getDashboardSummary(1L);

        assertNotNull(result);
        assertEquals(500L, result.get("monthOrders"));
    }

    @Test
    @DisplayName("环比增长率：正常计算")
    void testGrowthRate_Normal() {
        MerchantDashboardStats today = new MerchantDashboardStats();
        today.setOrderCount(30L);
        today.setUvCount(500L);
        today.setPaidOrderAmount(new BigDecimal("4200.00"));

        MerchantDashboardStats yesterday = new MerchantDashboardStats();
        yesterday.setOrderCount(20L);
        yesterday.setUvCount(400L);
        yesterday.setPaidOrderAmount(new BigDecimal("2500.00"));

        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean()))
            .thenReturn(today)
            .thenReturn(yesterday)
            .thenReturn(null);
        when(dashboardMapper.selectByDateRange(eq(1L), isNull(), anyString(), anyString())).thenReturn(Collections.emptyList());

        Map<String, Object> result = dashboardService.getDashboardSummary(1L);

        assertNotNull(result.get("ordersGrowth"));
        assertNotNull(result.get("uvGrowth"));
    }
}
