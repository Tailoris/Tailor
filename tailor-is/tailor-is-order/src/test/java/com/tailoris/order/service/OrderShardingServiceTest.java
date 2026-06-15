package com.tailoris.order.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("OrderShardingService 测试")
@ExtendWith(MockitoExtension.class)
class OrderShardingServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private OrderShardingService orderShardingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderShardingService, "highFreqThreshold", 1000);
        ReflectionTestUtils.setField(orderShardingService, "totalShards", 4);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("获取分片-merchantId为null")
    void testGetShardIndex_NullMerchantId() {
        int result = orderShardingService.getShardIndex(null);

        assertEquals(0, result);
    }

    @Test
    @DisplayName("获取分片-本地缓存命中")
    void testGetShardIndex_LocalCacheHit() {
        Long merchantId = 100L;
        ReflectionTestUtils.setField(orderShardingService, "merchantShardCache",
                new java.util.concurrent.ConcurrentHashMap<Long, Integer>() {{
                    put(merchantId, 2);
                }});

        int result = orderShardingService.getShardIndex(merchantId);

        assertEquals(2, result);
    }

    @Test
    @DisplayName("获取分片-Redis缓存命中")
    void testGetShardIndex_RedisCacheHit() {
        Long merchantId = 100L;
        when(valueOperations.get("order:shard:merchant:" + merchantId)).thenReturn("1");

        int result = orderShardingService.getShardIndex(merchantId);

        assertEquals(1, result);
        // Redis缓存命中时不会调用set，只会在未命中时写入缓存
    }

    @Test
    @DisplayName("获取分片-普通商户")
    void testGetShardIndex_NormalMerchant() {
        Long merchantId = 100L;
        when(valueOperations.get("order:shard:merchant:" + merchantId)).thenReturn(null);
        when(valueOperations.get("order:shard:stats:" + merchantId)).thenReturn(null);

        int result = orderShardingService.getShardIndex(merchantId);

        assertTrue(result >= 0 && result < 3);
        verify(valueOperations).set(eq("order:shard:merchant:" + merchantId), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("获取分片-高频商户")
    void testGetShardIndex_HighFreqMerchant() {
        Long merchantId = 100L;
        when(valueOperations.get("order:shard:merchant:" + merchantId)).thenReturn(null);
        when(valueOperations.get("order:shard:stats:" + merchantId)).thenReturn("1500");

        int result = orderShardingService.getShardIndex(merchantId);

        assertEquals(3, result);
    }

    @Test
    @DisplayName("记录订单")
    void testRecordOrder() {
        Long merchantId = 100L;
        when(valueOperations.increment("order:shard:stats:" + merchantId)).thenReturn(1L);
        when(stringRedisTemplate.expire(eq("order:shard:stats:" + merchantId), any(Duration.class))).thenReturn(true);

        orderShardingService.recordOrder(merchantId);

        verify(valueOperations).increment("order:shard:stats:" + merchantId);
        verify(stringRedisTemplate).expire(eq("order:shard:stats:" + merchantId), any(Duration.class));
    }

    @Test
    @DisplayName("判断高频商户-不是高频")
    void testIsHighFrequencyMerchant_False() {
        Long merchantId = 100L;
        when(valueOperations.get("order:shard:stats:" + merchantId)).thenReturn("500");

        boolean result = orderShardingService.isHighFrequencyMerchant(merchantId);

        assertFalse(result);
    }

    @Test
    @DisplayName("判断高频商户-是高频")
    void testIsHighFrequencyMerchant_True() {
        Long merchantId = 100L;
        when(valueOperations.get("order:shard:stats:" + merchantId)).thenReturn("1500");

        boolean result = orderShardingService.isHighFrequencyMerchant(merchantId);

        assertTrue(result);
    }

    @Test
    @DisplayName("判断高频商户-统计不存在")
    void testIsHighFrequencyMerchant_NoStats() {
        Long merchantId = 100L;
        when(valueOperations.get("order:shard:stats:" + merchantId)).thenReturn(null);

        boolean result = orderShardingService.isHighFrequencyMerchant(merchantId);

        assertFalse(result);
    }

    @Test
    @DisplayName("清除分片缓存")
    void testEvictShardCache() {
        Long merchantId = 100L;
        when(stringRedisTemplate.delete("order:shard:merchant:" + merchantId)).thenReturn(true);
        when(stringRedisTemplate.delete("order:shard:stats:" + merchantId)).thenReturn(true);

        orderShardingService.evictShardCache(merchantId);

        verify(stringRedisTemplate).delete("order:shard:merchant:" + merchantId);
        verify(stringRedisTemplate).delete("order:shard:stats:" + merchantId);
    }
}
