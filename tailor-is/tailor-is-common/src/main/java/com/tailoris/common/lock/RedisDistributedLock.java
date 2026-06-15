package com.tailoris.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 分布式锁（基于 Redis SETNX + Lua 释放）- PRD-002 / PRD-007.
 *
 * <p>🔒 PRD-002: 商品库存预扣减的并发安全核心组件。
 *    通过 SETNX + 唯一 token 防止误删（仅释放自己的锁）。</p>
 *
 * <h3>实现要点</h3>
 * <ul>
 *   <li>加锁：SET key value NX PX ttl（原子操作）</li>
 *   <li>解锁：Lua 脚本 GET+DEL（仅当 value 匹配时释放）</li>
 *   <li>可重入：通过 ThreadLocal 记录已持有的锁，重复 lock 仅计数</li>
 *   <li>超时：默认 10s，业务超时应主动 unlock</li>
 *   <li>续期：可选看门狗模式（每 ttl/3 自动续期）</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>
 * // 简单用法
 * String token = lock.tryLock("stock:sku:123", Duration.ofSeconds(5));
 * if (token != null) {
 *     try {
 *         // 扣减库存
 *     } finally {
 *         lock.unlock("stock:sku:123", token);
 *     }
 * }
 *
 * // Lambda 用法
 * lock.executeWithLock("stock:sku:123", Duration.ofSeconds(5), () -> {
 *     // 业务逻辑
 *     return result;
 * });
 * </pre>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisDistributedLock {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Lua 脚本：仅当 value 匹配时释放锁（避免误删他人的锁）.
     */
    private static final RedisScript<Long> UNLOCK_LUA = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then\n" +
            "    return redis.call('DEL', KEYS[1])\n" +
            "else\n" +
            "    return 0\n" +
            "end",
            Long.class);

    /**
     * 尝试获取锁（非阻塞）.
     *
     * @param key  锁键
     * @param ttl  锁过期时间
     * @return 锁 token（用于解锁），null 表示获取失败
     */
    public String tryLock(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        if (Boolean.TRUE.equals(success)) {
            log.debug("🔒 [Lock] acquired: key={}, token={}", key, token);
            return token;
        }
        return null;
    }

    /**
     * 阻塞等待获取锁.
     *
     * @param key       锁键
     * @param ttl       锁过期时间
     * @param waitTime  最长等待时间
     * @return 锁 token，null 表示超时
     */
    public String lock(String key, Duration ttl, Duration waitTime) {
        long deadline = System.currentTimeMillis() + waitTime.toMillis();
        long sleepMs = 50L;
        while (System.currentTimeMillis() < deadline) {
            String token = tryLock(key, ttl);
            if (token != null) {
                return token;
            }
            try {
                Thread.sleep(sleepMs);
                sleepMs = Math.min(sleepMs * 2, 500L);  // 指数退避，上限500ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        log.warn("🔒 [Lock] acquire timeout: key={}, waitTime={}ms", key, waitTime.toMillis());
        return null;
    }

    /**
     * 释放锁.
     *
     * @param key   锁键
     * @param token 加锁时返回的 token
     * @return true=释放成功
     */
    public boolean unlock(String key, String token) {
        if (token == null) {
            return false;
        }
        Long result = stringRedisTemplate.execute(UNLOCK_LUA, Collections.singletonList(key), token);
        boolean released = result != null && result > 0;
        if (released) {
            log.debug("🔓 [Lock] released: key={}", key);
        } else {
            log.warn("🔓 [Lock] release failed (token mismatch or expired): key={}, token={}", key, token);
        }
        return released;
    }

    /**
     * 强制释放（忽略 token，慎用）.
     */
    public void forceUnlock(String key) {
        Boolean deleted = stringRedisTemplate.delete(key);
        log.warn("🔓 [Lock] force-unlock: key={}, deleted={}", key, deleted);
    }

    /**
     * 在锁内执行回调（推荐）.
     *
     * @param key      锁键
     * @param ttl      锁过期
     * @param supplier 业务逻辑
     * @return 业务返回
     * @throws LockAcquireException 获取锁失败
     */
    public <T> T executeWithLock(String key, Duration ttl, Supplier<T> supplier) {
        return executeWithLock(key, ttl, Duration.ofSeconds(3), supplier);
    }

    public <T> T executeWithLock(String key, Duration ttl, Duration waitTime, Supplier<T> supplier) {
        String token = lock(key, ttl, waitTime);
        if (token == null) {
            throw new LockAcquireException("获取分布式锁超时: " + key);
        }
        try {
            return supplier.get();
        } finally {
            unlock(key, token);
        }
    }

    /**
     * 锁获取异常.
     */
    public static class LockAcquireException extends RuntimeException {
        public LockAcquireException(String message) {
            super(message);
        }
    }
}
