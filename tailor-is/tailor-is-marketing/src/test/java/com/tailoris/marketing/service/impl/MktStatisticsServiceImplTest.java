package com.tailoris.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.marketing.entity.MktPromotionStats;
import com.tailoris.marketing.mapper.MktOrderPromotionMapper;
import com.tailoris.marketing.mapper.MktPromotionStatsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("营销报表服务单元测试")
@ExtendWith(MockitoExtension.class)
class MktStatisticsServiceImplTest {

    @Mock
    private MktPromotionStatsMapper statsMapper;

    @Mock
    private MktOrderPromotionMapper orderPromotionMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private MktStatisticsServiceImpl statisticsService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("记录曝光：成功递增并设置过期时间")
    void testRecordExposure_Success() {
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(stringRedisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        assertDoesNotThrow(() -> statisticsService.recordExposure(1, 100L, "测试优惠券"));
        verify(valueOps).increment(anyString());
    }

    @Test
    @DisplayName("记录点击：成功递增并设置过期时间")
    void testRecordClick_Success() {
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(stringRedisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        assertDoesNotThrow(() -> statisticsService.recordClick(1, 100L, "测试优惠券"));
        verify(valueOps).increment(anyString());
    }

    @Test
    @DisplayName("记录参与：成功递增并设置过期时间")
    void testRecordParticipate_Success() {
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(stringRedisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        assertDoesNotThrow(() -> statisticsService.recordParticipate(1, 100L, "测试优惠券"));
        verify(valueOps).increment(anyString());
    }

    @Test
    @DisplayName("记录订单：统计不存在时创建新记录")
    void testRecordOrder_CreateNew() {
        when(statsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(statsMapper.insert(any(MktPromotionStats.class))).thenReturn(1);
        when(statsMapper.updateById(any(MktPromotionStats.class))).thenReturn(1);
        when(valueOps.get(anyString())).thenReturn(null);

        assertDoesNotThrow(() -> statisticsService.recordOrder(
                1, 100L, "测试优惠券",
                new BigDecimal("100"), new BigDecimal("10")));
        verify(statsMapper).insert(any(MktPromotionStats.class));
    }

    @Test
    @DisplayName("记录订单：统计已存在时更新记录")
    void testRecordOrder_UpdateExisting() {
        MktPromotionStats stats = new MktPromotionStats();
        stats.setId(1L);
        stats.setPromotionType(1);
        stats.setPromotionId(100L);
        stats.setOrderCount(5L);
        stats.setOrderAmount(new BigDecimal("500"));
        stats.setDiscountAmount(new BigDecimal("50"));

        when(statsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(stats);
        when(statsMapper.updateById(any(MktPromotionStats.class))).thenReturn(1);
        when(valueOps.get(anyString())).thenReturn(null);

        assertDoesNotThrow(() -> statisticsService.recordOrder(
                1, 100L, "测试优惠券",
                new BigDecimal("100"), new BigDecimal("10")));
        assertEquals(6L, stats.getOrderCount());
    }

    @Test
    @DisplayName("获取促销统计：成功返回")
    void testGetPromotionStats_Success() {
        MktPromotionStats stats = new MktPromotionStats();
        stats.setId(1L);
        stats.setPromotionType(1);
        stats.setPromotionId(100L);

        when(statsMapper.selectByPromotionRecent(1, 100L, 7))
                .thenReturn(Arrays.asList(stats));

        List<MktPromotionStats> result = statisticsService.getPromotionStats(1, 100L, 7);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("获取营销概览：成功返回转换后的数据")
    void testGetMarketingOverview_Success() {
        Map<String, Object> rawItem = new java.util.HashMap<>();
        rawItem.put("promotion_type", 1);
        rawItem.put("orderCount", 100L);
        rawItem.put("orderAmount", new BigDecimal("10000"));
        rawItem.put("discountAmount", new BigDecimal("1000"));

        when(statsMapper.aggregateByType(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Arrays.asList(rawItem));

        List<Map<String, Object>> result = statisticsService.getMarketingOverview(
                LocalDate.now().minusDays(7), LocalDate.now());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("优惠券", result.get(0).get("typeName"));
    }

    @Test
    @DisplayName("更新ROI：成本为null时直接返回")
    void testUpdateRoi_NullCost() {
        assertDoesNotThrow(() -> statisticsService.updateRoi(1, 100L, null));
        verify(statsMapper, never()).selectByPromotionRecent(anyInt(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("更新ROI：成本为0时直接返回")
    void testUpdateRoi_ZeroCost() {
        assertDoesNotThrow(() -> statisticsService.updateRoi(1, 100L, BigDecimal.ZERO));
        verify(statsMapper, never()).selectByPromotionRecent(anyInt(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("更新ROI：成功计算并更新")
    void testUpdateRoi_Success() {
        MktPromotionStats stats = new MktPromotionStats();
        stats.setId(1L);
        stats.setOrderAmount(new BigDecimal("1000"));

        when(statsMapper.selectByPromotionRecent(1, 100L, 365))
                .thenReturn(Arrays.asList(stats));
        when(statsMapper.updateById(any(MktPromotionStats.class))).thenReturn(1);

        assertDoesNotThrow(() -> statisticsService.updateRoi(1, 100L, new BigDecimal("100")));
        assertEquals(0, new BigDecimal("10.0000").compareTo(stats.getRoi()));
    }

    @Test
    @DisplayName("获取热门促销：成功返回")
    void testGetTopPromotions_Success() {
        MktPromotionStats stats = new MktPromotionStats();
        stats.setId(1L);

        when(statsMapper.selectTopByDateRange(any(LocalDate.class), any(LocalDate.class), anyInt()))
                .thenReturn(Arrays.asList(stats));

        List<MktPromotionStats> result = statisticsService.getTopPromotions(
                LocalDate.now().minusDays(7), LocalDate.now(), 10);

        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
