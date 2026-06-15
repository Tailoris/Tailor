package com.tailoris.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LoginRateLimiter 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoginRateLimiter 单元测试")
class LoginRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private LoginRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        rateLimiter = new LoginRateLimiter(redisTemplate);
    }

    @Test
    @DisplayName("检查用户锁定-已锁定时应返回true")
    void isUserLocked_locked_shouldReturnTrue() {
        when(redisTemplate.hasKey("login:lock:user:user123")).thenReturn(true);

        boolean locked = rateLimiter.isUserLocked("user123");

        assertThat(locked).isTrue();
    }

    @Test
    @DisplayName("检查用户锁定-未锁定时应返回false")
    void isUserLocked_notLocked_shouldReturnFalse() {
        when(redisTemplate.hasKey("login:lock:user:user123")).thenReturn(false);

        boolean locked = rateLimiter.isUserLocked("user123");

        assertThat(locked).isFalse();
    }

    @Test
    @DisplayName("检查IP锁定-已锁定时应返回true")
    void isIpLocked_locked_shouldReturnTrue() {
        when(redisTemplate.hasKey("login:lock:ip:192.168.1.1")).thenReturn(true);

        boolean locked = rateLimiter.isIpLocked("192.168.1.1");

        assertThat(locked).isTrue();
    }

    @Test
    @DisplayName("检查IP锁定-未锁定时应返回false")
    void isIpLocked_notLocked_shouldReturnFalse() {
        when(redisTemplate.hasKey("login:lock:ip:192.168.1.1")).thenReturn(false);

        boolean locked = rateLimiter.isIpLocked("192.168.1.1");

        assertThat(locked).isFalse();
    }

    @Test
    @DisplayName("记录失败-首次失败应设置TTL")
    void recordFailure_firstAttempt_shouldSetTtl() {
        when(valueOperations.increment("login:fail:user:user123")).thenReturn(1L);
        when(valueOperations.increment("login:fail:ip:192.168.1.1")).thenReturn(1L);

        boolean locked = rateLimiter.recordFailure("user123", "192.168.1.1");

        assertThat(locked).isFalse();
        verify(redisTemplate).expire(eq("login:fail:user:user123"), eq(24L), eq(TimeUnit.HOURS));
        verify(redisTemplate).expire(eq("login:fail:ip:192.168.1.1"), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    @DisplayName("记录失败-用户达到5次应锁定")
    void recordFailure_userReachMaxAttempts_shouldLockUser() {
        when(valueOperations.increment("login:fail:user:user123")).thenReturn(5L);
        when(valueOperations.increment("login:fail:ip:192.168.1.1")).thenReturn(1L);

        boolean locked = rateLimiter.recordFailure("user123", "192.168.1.1");

        assertThat(locked).isTrue();
        verify(valueOperations).set(eq("login:lock:user:user123"), eq("1"), eq(15L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("记录失败-IP达到10次应锁定")
    void recordFailure_ipReachMaxAttempts_shouldLockIp() {
        when(valueOperations.increment("login:fail:user:user123")).thenReturn(1L);
        when(valueOperations.increment("login:fail:ip:192.168.1.1")).thenReturn(10L);

        boolean locked = rateLimiter.recordFailure("user123", "192.168.1.1");

        assertThat(locked).isTrue();
        verify(valueOperations).set(eq("login:lock:ip:192.168.1.1"), eq("1"), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("记录成功-应清除所有失败记录")
    void recordSuccess_shouldClearAllRecords() {
        rateLimiter.recordSuccess("user123", "192.168.1.1");

        verify(redisTemplate).delete("login:fail:user:user123");
        verify(redisTemplate).delete("login:lock:user:user123");
        verify(redisTemplate).delete("login:fail:ip:192.168.1.1");
        verify(redisTemplate).delete("login:lock:ip:192.168.1.1");
    }

    @Test
    @DisplayName("获取用户剩余尝试次数-无记录时应返回5")
    void getRemainingUserAttempts_noRecord_shouldReturn5() {
        when(valueOperations.get("login:fail:user:user123")).thenReturn(null);

        int remaining = rateLimiter.getRemainingUserAttempts("user123");

        assertThat(remaining).isEqualTo(5);
    }

    @Test
    @DisplayName("获取用户剩余尝试次数-有2次失败应返回3")
    void getRemainingUserAttempts_2Failures_shouldReturn3() {
        when(valueOperations.get("login:fail:user:user123")).thenReturn("2");

        int remaining = rateLimiter.getRemainingUserAttempts("user123");

        assertThat(remaining).isEqualTo(3);
    }

    @Test
    @DisplayName("获取用户剩余尝试次数-超过5次应返回0")
    void getRemainingUserAttempts_overMax_shouldReturn0() {
        when(valueOperations.get("login:fail:user:user123")).thenReturn("10");

        int remaining = rateLimiter.getRemainingUserAttempts("user123");

        assertThat(remaining).isEqualTo(0);
    }

    @Test
    @DisplayName("获取IP剩余尝试次数-无记录时应返回10")
    void getRemainingIpAttempts_noRecord_shouldReturn10() {
        when(valueOperations.get("login:fail:ip:192.168.1.1")).thenReturn(null);

        int remaining = rateLimiter.getRemainingIpAttempts("192.168.1.1");

        assertThat(remaining).isEqualTo(10);
    }

    @Test
    @DisplayName("获取IP剩余尝试次数-有3次失败应返回7")
    void getRemainingIpAttempts_3Failures_shouldReturn7() {
        when(valueOperations.get("login:fail:ip:192.168.1.1")).thenReturn("3");

        int remaining = rateLimiter.getRemainingIpAttempts("192.168.1.1");

        assertThat(remaining).isEqualTo(7);
    }

    @Test
    @DisplayName("获取用户锁定剩余时间-有锁定时应返回剩余秒数")
    void getUserLockRemainingSeconds_locked_shouldReturnRemaining() {
        when(redisTemplate.getExpire("login:lock:user:user123", TimeUnit.SECONDS)).thenReturn(600L);

        long remaining = rateLimiter.getUserLockRemainingSeconds("user123");

        assertThat(remaining).isEqualTo(600L);
    }

    @Test
    @DisplayName("获取用户锁定剩余时间-无锁定时应返回0")
    void getUserLockRemainingSeconds_notLocked_shouldReturnZero() {
        when(redisTemplate.getExpire("login:lock:user:user123", TimeUnit.SECONDS)).thenReturn(null);

        long remaining = rateLimiter.getUserLockRemainingSeconds("user123");

        assertThat(remaining).isEqualTo(0L);
    }
}
