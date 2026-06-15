package com.tailoris.common.lock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.types.Expiration;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * DistributedLock 单元测试 - 验证 B-C07/B-C08 修复
 *
 * <p>Critical Fix 验证：
 * <ul>
 *   <li>B-C07: 订单创建分布式锁</li>
 *   <li>B-C08: 商品创建分布式锁</li>
 * </ul>
 *
 * @author Tailor IS Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DistributedLock 单元测试 - Critical B-C07/B-C08")
class DistributedLockTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private DistributedLock distributedLock;

    @BeforeEach
    void setUp() {
        distributedLock = new DistributedLock(stringRedisTemplate);
    }

    @Test
    @DisplayName("B-C07: tryLock 成功应返回token")
    @SuppressWarnings("unchecked")
    void testTryLock_Success() {
        when(stringRedisTemplate.execute(any(RedisCallback.class))).thenReturn(true);

        String token = distributedLock.tryLock("test:key", 30, TimeUnit.SECONDS);

        assertThat(token).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("B-C07: tryLock 失败应返回null")
    @SuppressWarnings("unchecked")
    void testTryLock_Fail() {
        when(stringRedisTemplate.execute(any(RedisCallback.class))).thenReturn(false);

        String token = distributedLock.tryLock("test:key", 30, TimeUnit.SECONDS);

        assertThat(token).isNull();
    }

    @Test
    @DisplayName("B-C08: 正确的token应能解锁")
    @SuppressWarnings("unchecked")
    void testUnlock_WithValidToken() {
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(1L);

        boolean result = distributedLock.unlock("test:key", "valid-token");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("B-C08: 无效token应解锁失败")
    @SuppressWarnings("unchecked")
    void testUnlock_WithInvalidToken() {
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(0L);

        boolean result = distributedLock.unlock("test:key", "invalid-token");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("B-C07: executeWithLock 成功应执行业务逻辑")
    @SuppressWarnings("unchecked")
    void testExecuteWithLock_Success() {
        when(stringRedisTemplate.execute(any(RedisCallback.class))).thenReturn(true);
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(1L);

        String result = distributedLock.executeWithLock("test:key", 1, 30, TimeUnit.SECONDS, () -> "ok");

        assertThat(result).isEqualTo("ok");
    }

    @Test
    @DisplayName("B-C07: null参数应安全处理")
    void testUnlock_NullArgs() {
        boolean result = distributedLock.unlock(null, "token");
        assertThat(result).isFalse();

        result = distributedLock.unlock("key", null);
        assertThat(result).isFalse();
    }
}
