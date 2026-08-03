package com.tailoris.common.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.connection.RedisConnection;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
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

    @Mock
    private RedisConnection redisConnection;

    @Mock
    private Cursor<byte[]> mockCursor;

    private MultiLevelCache<String, String> cache;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cache = new MultiLevelCache<>(redisTemplate, 300L, 10000L, 5L);
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

    /**
     * 配置 redisTemplate.execute() 以模拟 SCAN 命令返回指定 keys
     */
    @SuppressWarnings("unchecked")
    private void mockScanKeysReturning(Set<String> keysToReturn) {
        doAnswer(invocation -> {
            RedisCallback<Set<String>> callback = invocation.getArgument(0);
            byte[][] keyBytes = keysToReturn.stream()
                    .map(String::getBytes)
                    .toArray(byte[][]::new);
            // 模拟 Cursor 行为：遍历 keys 中的每个元素
            if (keyBytes.length >= 3) {
                when(mockCursor.hasNext()).thenReturn(true, true, true, false);
                when(mockCursor.next()).thenReturn(keyBytes[0], keyBytes[1], keyBytes[2]);
            } else if (keyBytes.length == 2) {
                when(mockCursor.hasNext()).thenReturn(true, true, false);
                when(mockCursor.next()).thenReturn(keyBytes[0], keyBytes[1]);
            } else if (keyBytes.length == 1) {
                when(mockCursor.hasNext()).thenReturn(true, false);
                when(mockCursor.next()).thenReturn(keyBytes[0]);
            }
            when(redisConnection.scan(any(ScanOptions.class))).thenReturn(mockCursor);
            return callback.doInRedis(redisConnection);
        }).when(redisTemplate).execute(any(RedisCallback.class));
    }

    @Test
    @DisplayName("按前缀失效应通过SCAN删除匹配的Redis键并清空L1")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void invalidateByPrefix_shouldDeleteMatchingKeysAndClearL1() {
        String prefix = "user";
        Set<String> keys = Set.of("user:1", "user:2", "user:3");

        mockScanKeysReturning(keys);

        cache.invalidateByPrefix(prefix);

        ArgumentCaptor<Collection> deletedKeysCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(redisTemplate).delete(deletedKeysCaptor.capture());
        assertThat(deletedKeysCaptor.getValue()).containsExactlyInAnyOrderElementsOf(keys);
    }

    @Test
    @DisplayName("按前缀失效-无匹配键应正常处理")
    @SuppressWarnings("unchecked")
    void invalidateByPrefix_noMatchingKeys_shouldHandleGracefully() {
        // mock 返回空集合
        doAnswer(invocation -> {
            RedisCallback<Set<String>> callback = invocation.getArgument(0);
            when(mockCursor.hasNext()).thenReturn(false);
            when(redisConnection.scan(any(ScanOptions.class))).thenReturn(mockCursor);
            return callback.doInRedis(redisConnection);
        }).when(redisTemplate).execute(any(RedisCallback.class));

        cache.invalidateByPrefix("user");
    }

    @Test
    @DisplayName("按前缀失效-Redis异常应正常处理")
    @SuppressWarnings("unchecked")
    void invalidateByPrefix_redisException_shouldHandleGracefully() {
        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenThrow(new RuntimeException("Redis error"));

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
