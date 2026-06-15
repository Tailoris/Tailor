package com.tailoris.common.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MultiLevelCache 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MultiLevelCache 单元测试")
class MultiLevelCacheTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private MultiLevelCache<String, String> cache;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cache = new MultiLevelCache<>(redisTemplate);
    }

    @Test
    @DisplayName("L1缓存命中应直接返回")
    void get_l1Hit_shouldReturnValue() {
        String key = "test-key";
        String value = "test-value";
        String redisKey = "prefix:" + key;
        
        // First call - cache miss, load from DB
        when(valueOperations.get(redisKey)).thenReturn(null);
        String result1 = cache.get(key, "prefix", Duration.ofMinutes(5), k -> value);
        assertThat(result1).isEqualTo(value);
        
        // Second call - should hit L1 cache
        String result2 = cache.get(key, "prefix", Duration.ofMinutes(5), k -> null);
        assertThat(result2).isEqualTo(value);
    }

    @Test
    @DisplayName("L1未命中L2命中应回写L1并返回")
    void get_l2Hit_shouldWriteBackToL1AndReturn() {
        String key = "test-key";
        String value = "test-value";
        String redisKey = "prefix:" + key;
        
        when(valueOperations.get(redisKey)).thenReturn(value);

        String result = cache.get(key, "prefix", Duration.ofMinutes(5), k -> null);

        assertThat(result).isEqualTo(value);
        
        String result2 = cache.get(key, "prefix", Duration.ofMinutes(5), k -> null);
        assertThat(result2).isEqualTo(value);
    }

    @Test
    @DisplayName("L1L2都未命中应从DB加载并回写")
    void get_cacheMiss_shouldLoadFromDbAndWriteBack() {
        String key = "test-key";
        String value = "db-value";
        String redisKey = "prefix:" + key;
        
        when(valueOperations.get(redisKey)).thenReturn(null);
        Function<String, String> dbLoader = k -> value;

        String result = cache.get(key, "prefix", Duration.ofMinutes(5), dbLoader);

        assertThat(result).isEqualTo(value);
        verify(valueOperations).set(eq(redisKey), eq(value), any(Duration.class));
    }

    @Test
    @DisplayName("Redis读取异常不应影响业务")
    void get_redisReadException_shouldContinueToDb() {
        String key = "test-key";
        String value = "db-value";
        
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis error"));

        String result = cache.get(key, "prefix", Duration.ofMinutes(5), k -> value);

        assertThat(result).isEqualTo(value);
    }

    @Test
    @DisplayName("Redis写入异常不应影响业务")
    void get_redisWriteException_shouldNotAffectBusiness() {
        String key = "test-key";
        String value = "db-value";
        String redisKey = "prefix:" + key;
        
        when(valueOperations.get(redisKey)).thenReturn(null);
        lenient().doThrow(new RuntimeException("Redis write error"))
                .when(valueOperations).set(eq(redisKey), eq(value), any(Duration.class));

        String result = cache.get(key, "prefix", Duration.ofMinutes(5), k -> value);

        assertThat(result).isEqualTo(value);
    }

    @Test
    @DisplayName("DB返回null不应写入缓存")
    void get_dbReturnsNull_shouldNotWriteToCache() {
        String key = "test-key";
        String redisKey = "prefix:" + key;
        
        when(valueOperations.get(redisKey)).thenReturn(null);

        String result = cache.get(key, "prefix", Duration.ofMinutes(5), k -> null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("失效缓存应同时失效L1和L2")
    void invalidate_shouldInvalidateBothLevels() {
        String key = "test-key";
        String redisKey = "prefix:" + key;

        cache.invalidate(key, "prefix");

        verify(redisTemplate).delete(redisKey);
    }

    @Test
    @DisplayName("Redis删除异常不应影响L1失效")
    void invalidate_redisDeleteException_shouldStillInvalidateL1() {
        String key = "test-key";
        String redisKey = "prefix:" + key;
        
        lenient().doThrow(new RuntimeException("Redis error")).when(redisTemplate).delete(redisKey);

        cache.invalidate(key, "prefix");
    }

    @Test
    @DisplayName("按前缀失效应删除匹配的Redis键和清空L1")
    void invalidateByPrefix_shouldDeleteMatchingKeysAndClearL1() {
        String prefix = "user";
        Set<String> keys = Set.of("user:1", "user:2", "user:3");
        
        when(redisTemplate.keys("user:*")).thenReturn(keys);

        cache.invalidateByPrefix(prefix);

        verify(redisTemplate).delete(keys);
    }

    @Test
    @DisplayName("按前缀失效-无匹配键应正常处理")
    void invalidateByPrefix_noMatchingKeys_shouldHandleGracefully() {
        when(redisTemplate.keys("user:*")).thenReturn(null);

        cache.invalidateByPrefix("user");
    }

    @Test
    @DisplayName("按前缀失效-Redis异常应正常处理")
    void invalidateByPrefix_redisException_shouldHandleGracefully() {
        when(redisTemplate.keys(anyString())).thenThrow(new RuntimeException("Redis error"));

        cache.invalidateByPrefix("user");
    }

    @Test
    @DisplayName("获取统计信息应返回非null")
    void getStats_shouldReturnNonNull() {
        MultiLevelCache.CacheStats stats = cache.getStats();

        assertThat(stats).isNotNull();
        assertThat(stats.hitCount()).isGreaterThanOrEqualTo(0);
        assertThat(stats.missCount()).isGreaterThanOrEqualTo(0);
        assertThat(stats.hitRate()).isBetween(0.0, 100.0);
    }
}
