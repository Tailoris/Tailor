package com.tailoris.common.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Tailor IS 登录失败限制器
 * 功能:
 *   1. 基于用户ID的失败次数计数 (5次失败锁定15分钟)
 *   2. 基于IP的失败次数计数 (10次失败锁定30分钟)
 *   3. 自动解锁 (时间窗口过期后自动清除计数)
 *   4. 登录成功后清除失败记录
 *
 * Redis Key 结构:
 *   login:fail:user:{userId}         -> 计数(String), TTL 24h
 *   login:fail:ip:{ipAddress}        -> 计数(String), TTL 24h
 *   login:lock:user:{userId}         -> "1" 表示锁定, TTL 15分钟
 *   login:lock:ip:{ipAddress}        -> "1" 表示锁定, TTL 30分钟
 *
 * @author Tailor IS Platform Team
 * @version 1.0
 */
@Component
public class LoginRateLimiter {

    private static final String FAIL_USER_KEY_PREFIX = "login:fail:user:";
    private static final String FAIL_IP_KEY_PREFIX = "login:fail:ip:";
    private static final String LOCK_USER_KEY_PREFIX = "login:lock:user:";
    private static final String LOCK_IP_KEY_PREFIX = "login:lock:ip:";

    private static final int MAX_USER_ATTEMPTS = 5;
    private static final int MAX_IP_ATTEMPTS = 10;
    private static final int USER_LOCK_MINUTES = 15;
    private static final int IP_LOCK_MINUTES = 30;
    private static final int FAIL_RECORD_HOURS = 24;

    private final StringRedisTemplate redisTemplate;

    public LoginRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 检查用户是否被锁定
     */
    public boolean isUserLocked(String userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(LOCK_USER_KEY_PREFIX + userId));
    }

    /**
     * 检查IP是否被锁定
     */
    public boolean isIpLocked(String ipAddress) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(LOCK_IP_KEY_PREFIX + ipAddress));
    }

    /**
     * 记录一次登录失败
     * @return true: 本次失败后被锁定; false: 未锁定
     */
    public boolean recordFailure(String userId, String ipAddress) {
        boolean userLocked = incrementAndCheckUser(userId);
        boolean ipLocked = incrementAndCheckIp(ipAddress);
        return userLocked || ipLocked;
    }

    /**
     * 登录成功后清除失败记录
     */
    public void recordSuccess(String userId, String ipAddress) {
        redisTemplate.delete(FAIL_USER_KEY_PREFIX + userId);
        redisTemplate.delete(LOCK_USER_KEY_PREFIX + userId);
        redisTemplate.delete(FAIL_IP_KEY_PREFIX + ipAddress);
        redisTemplate.delete(LOCK_IP_KEY_PREFIX + ipAddress);
    }

    /**
     * 获取剩余尝试次数 (用户维度)
     */
    public int getRemainingUserAttempts(String userId) {
        String count = redisTemplate.opsForValue().get(FAIL_USER_KEY_PREFIX + userId);
        int attempts = count != null ? Integer.parseInt(count) : 0;
        return Math.max(0, MAX_USER_ATTEMPTS - attempts);
    }

    /**
     * 获取剩余尝试次数 (IP维度)
     */
    public int getRemainingIpAttempts(String ipAddress) {
        String count = redisTemplate.opsForValue().get(FAIL_IP_KEY_PREFIX + ipAddress);
        int attempts = count != null ? Integer.parseInt(count) : 0;
        return Math.max(0, MAX_IP_ATTEMPTS - attempts);
    }

    /**
     * 获取用户锁定剩余时间 (秒)
     */
    public long getUserLockRemainingSeconds(String userId) {
        Long ttl = redisTemplate.getExpire(LOCK_USER_KEY_PREFIX + userId, TimeUnit.SECONDS);
        return ttl != null ? ttl : 0;
    }

    private boolean incrementAndCheckUser(String userId) {
        String key = FAIL_USER_KEY_PREFIX + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, FAIL_RECORD_HOURS, TimeUnit.HOURS);
        }
        if (count != null && count >= MAX_USER_ATTEMPTS) {
            lockUser(userId);
            return true;
        }
        return false;
    }

    private boolean incrementAndCheckIp(String ipAddress) {
        String key = FAIL_IP_KEY_PREFIX + ipAddress;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, FAIL_RECORD_HOURS, TimeUnit.HOURS);
        }
        if (count != null && count >= MAX_IP_ATTEMPTS) {
            lockIp(ipAddress);
            return true;
        }
        return false;
    }

    private void lockUser(String userId) {
        redisTemplate.opsForValue().set(LOCK_USER_KEY_PREFIX + userId, "1",
                USER_LOCK_MINUTES, TimeUnit.MINUTES);
    }

    private void lockIp(String ipAddress) {
        redisTemplate.opsForValue().set(LOCK_IP_KEY_PREFIX + ipAddress, "1",
                IP_LOCK_MINUTES, TimeUnit.MINUTES);
    }
}
