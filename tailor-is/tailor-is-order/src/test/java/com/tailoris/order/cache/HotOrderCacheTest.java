package com.tailoris.order.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.order.entity.OrderInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("HotOrderCache 测试")
@ExtendWith(MockitoExtension.class)
class HotOrderCacheTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private ObjectMapper objectMapper;

    @InjectMocks
    private HotOrderCache hotOrderCache;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ReflectionTestUtils.setField(hotOrderCache, "hotOrderTtlMinutes", 30);
        ReflectionTestUtils.setField(hotOrderCache, "hotThreshold", 10);
        ReflectionTestUtils.setField(hotOrderCache, "objectMapper", objectMapper);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    @DisplayName("读取缓存-缓存未命中")
    void testGet_CacheMiss() {
        Long productId = 1L;
        when(valueOperations.get("order:hot:" + productId)).thenReturn(null);

        List<OrderInfo> result = hotOrderCache.get(productId);

        assertNull(result);
    }

    @Test
    @DisplayName("读取缓存-缓存命中")
    void testGet_CacheHit() throws JsonProcessingException {
        Long productId = 1L;
        String json = "[{\"id\":1,\"orderNo\":\"ORD123\"}]";
        
        when(valueOperations.get("order:hot:" + productId)).thenReturn(json);
        
        // Use real ObjectMapper to deserialize - it will work with valid JSON
        List<OrderInfo> result = hotOrderCache.get(productId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ORD123", result.get(0).getOrderNo());
    }

    @Test
    @DisplayName("读取缓存-反序列化失败")
    void testGet_DeserializationFailed() throws JsonProcessingException {
        Long productId = 1L;
        String invalidJson = "invalid json";

        when(valueOperations.get("order:hot:" + productId)).thenReturn(invalidJson);
        // Let it actually fail to deserialize - will throw exception internally
        when(stringRedisTemplate.delete("order:hot:" + productId)).thenReturn(true);

        List<OrderInfo> result = hotOrderCache.get(productId);

        assertNull(result);
        verify(stringRedisTemplate).delete("order:hot:" + productId);
    }

    @Test
    @DisplayName("写入缓存-订单列表为空")
    void testPut_EmptyOrders() {
        Long productId = 1L;
        List<OrderInfo> orders = Collections.emptyList();

        hotOrderCache.put(productId, orders);

        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("写入缓存-订单列表为null")
    void testPut_NullOrders() {
        Long productId = 1L;

        hotOrderCache.put(productId, null);

        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("写入缓存成功")
    void testPut_Success() throws JsonProcessingException {
        Long productId = 1L;
        List<OrderInfo> orders = new ArrayList<>();
        OrderInfo order = new OrderInfo();
        order.setId(1L);
        orders.add(order);

        String json = "[{\"id\":1}]";
        // ObjectMapper is real object, so we need to spy it or use doReturn
        ObjectMapper spyMapper = spy(objectMapper);
        ReflectionTestUtils.setField(hotOrderCache, "objectMapper", spyMapper);
        doReturn(json).when(spyMapper).writeValueAsString(orders);
        when(zSetOperations.incrementScore(anyString(), anyString(), anyDouble())).thenReturn(1.0);
        when(stringRedisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);

        hotOrderCache.put(productId, orders);

        verify(valueOperations).set(eq("order:hot:" + productId), eq(json), any(Duration.class));
    }

    @Test
    @DisplayName("失效缓存")
    void testEvict() {
        Long productId = 1L;
        when(stringRedisTemplate.delete(eq("order:hot:" + productId))).thenReturn(true);

        hotOrderCache.evict(productId);

        verify(stringRedisTemplate).delete(eq("order:hot:" + productId));
    }

    @Test
    @DisplayName("批量失效缓存-列表为空")
    void testEvictBatch_EmptyList() {
        hotOrderCache.evictBatch(Collections.emptyList());

        verify(stringRedisTemplate, never()).delete(anyList());
    }

    @Test
    @DisplayName("批量失效缓存-列表为null")
    void testEvictBatch_NullList() {
        hotOrderCache.evictBatch(null);

        verify(stringRedisTemplate, never()).delete(anyList());
    }

    @Test
    @DisplayName("批量失效缓存成功")
    void testEvictBatch_Success() {
        List<Long> productIds = Arrays.asList(1L, 2L, 3L);
        List<String> keys = Arrays.asList("order:hot:1", "order:hot:2", "order:hot:3");
        when(stringRedisTemplate.delete(keys)).thenReturn(3L);

        hotOrderCache.evictBatch(productIds);

        verify(stringRedisTemplate).delete(keys);
    }

    @Test
    @DisplayName("判断是否缓存-存在")
    void testIsCached_True() {
        Long productId = 1L;
        when(stringRedisTemplate.hasKey("order:hot:" + productId)).thenReturn(true);

        boolean result = hotOrderCache.isCached(productId);

        assertTrue(result);
    }

    @Test
    @DisplayName("判断是否缓存-不存在")
    void testIsCached_False() {
        Long productId = 1L;
        when(stringRedisTemplate.hasKey("order:hot:" + productId)).thenReturn(false);

        boolean result = hotOrderCache.isCached(productId);

        assertFalse(result);
    }

    @Test
    @DisplayName("获取热门排行-为空")
    void testGetHotProductRank_Empty() {
        when(zSetOperations.reverseRange("order:hot:rank", 0, 9)).thenReturn(Collections.emptySet());

        List<Long> result = hotOrderCache.getHotProductRank(10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("获取热门排行成功")
    void testGetHotProductRank_Success() {
        Set<String> rank = new LinkedHashSet<>(Arrays.asList("1", "2", "3"));
        when(zSetOperations.reverseRange("order:hot:rank", 0, 9)).thenReturn(rank);

        List<Long> result = hotOrderCache.getHotProductRank(10);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(1L, result.get(0));
        assertEquals(2L, result.get(1));
        assertEquals(3L, result.get(2));
    }

    @Test
    @DisplayName("获取热门排行-数据异常")
    void testGetHotProductRank_InvalidData() {
        Set<String> rank = new LinkedHashSet<>(Arrays.asList("1", "invalid", "3"));
        when(zSetOperations.reverseRange("order:hot:rank", 0, 9)).thenReturn(rank);

        List<Long> result = hotOrderCache.getHotProductRank(10);

        assertNotNull(result);
        assertEquals(2, result.size());
    }
}
