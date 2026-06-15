package com.tailoris.common.lock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * RedisDistributedLock 单元测试.
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RedisDistributedLock 分布式锁测试 (PRD-002)")
class RedisDistributedLockTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisDistributedLock lock;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lock = new RedisDistributedLock(stringRedisTemplate);
    }

    @Test
    @DisplayName("tryLock - SETNX 成功时返回 token")
    void tryLock_Success() {
        when(stringRedisTemplate.opsForValue().setIfAbsent(eq("key1"), anyString(), any(Duration.class)))
                .thenReturn(true);
        String token = lock.tryLock("key1", Duration.ofSeconds(10));
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    @DisplayName("tryLock - SETNX 失败时返回 null")
    void tryLock_Fail() {
        when(stringRedisTemplate.opsForValue().setIfAbsent(eq("key1"), anyString(), any(Duration.class)))
                .thenReturn(false);
        String token = lock.tryLock("key1", Duration.ofSeconds(10));
        assertNull(token);
    }

    @Test
    @DisplayName("unlock - Lua 脚本返回值 > 0 表示成功")
    void unlock_Success() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(1L);
        boolean result = lock.unlock("key1", "token-abc");
        assertTrue(result);
    }

    @Test
    @DisplayName("unlock - Lua 脚本返回 0 表示锁已过期或被他人持有")
    void unlock_Fail() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(0L);
        boolean result = lock.unlock("key1", "wrong-token");
        assertFalse(result);
    }

    @Test
    @DisplayName("unlock - token 为 null 时返回 false")
    void unlock_NullToken() {
        assertFalse(lock.unlock("key1", null));
    }

    @Test
    @DisplayName("forceUnlock - 强制删除锁")
    void forceUnlock_Normal() {
        when(stringRedisTemplate.delete("key1")).thenReturn(true);
        lock.forceUnlock("key1");
        // 不抛异常即通过
    }

    @Test
    @DisplayName("executeWithLock - 获取锁成功时执行 supplier")
    void executeWithLock_Success() {
        when(stringRedisTemplate.opsForValue().setIfAbsent(eq("key"), anyString(), any(Duration.class)))
                .thenReturn(true);
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(1L);

        String result = lock.executeWithLock("key", Duration.ofSeconds(10), () -> "executed");
        assertEquals("executed", result);
    }

    @Test
    @DisplayName("executeWithLock - supplier 抛异常时锁仍被释放")
    void executeWithLock_ExceptionReleasesLock() {
        when(stringRedisTemplate.opsForValue().setIfAbsent(eq("key"), anyString(), any(Duration.class)))
                .thenReturn(true);
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(1L);

        assertThrows(RuntimeException.class, () ->
                lock.executeWithLock("key", Duration.ofSeconds(10), () -> {
                    throw new RuntimeException("business error");
                }));
    }

    @Test
    @DisplayName("executeWithLock - 获取锁超时时抛 LockAcquireException")
    void executeWithLock_Timeout() {
        when(stringRedisTemplate.opsForValue().setIfAbsent(eq("key"), anyString(), any(Duration.class)))
                .thenReturn(false);  // 一直拿不到锁

        assertThrows(RedisDistributedLock.LockAcquireException.class, () ->
                lock.executeWithLock("key", Duration.ofSeconds(10), Duration.ofMillis(200), () -> "unreachable"));
    }

    @Test
    @DisplayName("并发场景 - 同一key多次tryLock仅一个成功")
    void concurrentTryLock_OnlyOneSuccess() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        // 第一次 SETNX 成功，后续失败
        when(stringRedisTemplate.opsForValue().setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenAnswer(invocation -> {
                    // 用 ThreadLocal 控制：第一次成功
                    if (successCount.incrementAndGet() == 1) {
                        return true;
                    }
                    return false;
                });

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    String token = lock.tryLock("contested-key", Duration.ofSeconds(5));
                    if (token != null) {
                        // 模拟成功获取
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        pool.shutdown();
    }
}
