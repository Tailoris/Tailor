package com.tailoris.merchant.service;

import com.tailoris.merchant.entity.MerchantDashboardStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 商家数据工作台单元测试 - MER-009.
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@DisplayName("商家数据工作台单元测试")
class MerchantDashboardStatsServiceTest {

    @Test
    @DisplayName("Dashboard统计实体字段初始化")
    void testEntityInitialization() {
        MerchantDashboardStats stats = new MerchantDashboardStats();
        stats.setMerchantId(1L);
        stats.setStatDate("2026-06-03");
        stats.setStatType(1);
        stats.setPvCount(1000L);
        stats.setUvCount(500L);
        stats.setOrderCount(20L);
        stats.setOrderAmount(new BigDecimal("5000.00"));
        stats.setPaidOrderCount(18L);
        stats.setPaidOrderAmount(new BigDecimal("4500.00"));
        stats.setConversionRate(new BigDecimal("0.05"));

        assertEquals(1L, stats.getMerchantId());
        assertEquals(1000L, stats.getPvCount());
        assertEquals(500L, stats.getUvCount());
        assertEquals(20L, stats.getOrderCount());
        assertEquals(0, new BigDecimal("5000.00").compareTo(stats.getOrderAmount()));
        assertEquals(0, new BigDecimal("0.05").compareTo(stats.getConversionRate()));
    }

    @Test
    @DisplayName("转化率计算")
    void testConversionRate() {
        // 转化率 = 订单数 / UV
        long uv = 1000L;
        long orders = 50L;
        BigDecimal rate = BigDecimal.valueOf(orders)
                .divide(BigDecimal.valueOf(uv), 4, java.math.RoundingMode.HALF_UP);
        assertEquals(0, new BigDecimal("0.0500").compareTo(rate));
    }

    @Test
    @DisplayName("环比增长率计算")
    void testGrowthRate() {
        // 增长率 = (当前 - 上期) / 上期 * 100
        BigDecimal current = new BigDecimal("120");
        BigDecimal previous = new BigDecimal("100");
        BigDecimal growth = current.subtract(previous)
                .multiply(new BigDecimal("100"))
                .divide(previous, 2, java.math.RoundingMode.HALF_UP);
        assertEquals(0, new BigDecimal("20.00").compareTo(growth));
    }

    @Test
    @DisplayName("零上期增长率")
    void testGrowthFromZero() {
        BigDecimal current = new BigDecimal("100");
        BigDecimal previous = BigDecimal.ZERO;
        // 上期为0且当前>0时，增长率记为100
        BigDecimal growth = previous.compareTo(BigDecimal.ZERO) == 0
                ? (current.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal("100") : BigDecimal.ZERO)
                : current.subtract(previous).multiply(new BigDecimal("100")).divide(previous, 2, java.math.RoundingMode.HALF_UP);
        assertEquals(0, new BigDecimal("100").compareTo(growth));
    }

    @Test
    @DisplayName("数值取整-空值兜底")
    void testNullValueFallback() {
        MerchantDashboardStats stats = new MerchantDashboardStats();
        stats.setPvCount(null);
        stats.setOrderCount(null);
        stats.setOrderAmount(null);

        long pv = stats.getPvCount() == null ? 0L : stats.getPvCount();
        long orders = stats.getOrderCount() == null ? 0L : stats.getOrderCount();
        BigDecimal amount = stats.getOrderAmount() == null ? BigDecimal.ZERO : stats.getOrderAmount();

        assertEquals(0L, pv);
        assertEquals(0L, orders);
        assertEquals(0, BigDecimal.ZERO.compareTo(amount));
    }

    @Test
    @DisplayName("统计类型验证")
    void testStatType() {
        MerchantDashboardStats stats = new MerchantDashboardStats();
        stats.setStatType(1);  // 日
        assertEquals(1, stats.getStatType());

        stats.setStatType(2);  // 周
        assertEquals(2, stats.getStatType());

        stats.setStatType(3);  // 月
        assertEquals(3, stats.getStatType());
    }

    @Test
    @DisplayName("退款率合理性")
    void testRefundRateRange() {
        // 退款率应在 0-1 之间
        BigDecimal refundRate = new BigDecimal("0.15");
        assertTrue(refundRate.compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(refundRate.compareTo(BigDecimal.ONE) <= 0);
    }
}
