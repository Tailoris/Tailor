package com.tailoris.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁服务 - 修复 B-C07/B-C08
 *
 * <p>基于 Redis SET NX EX 实现的分布式锁，支持：
 * <ul>
 *   <li>可重入：同一线程可多次加锁</li>
 *   <li>安全解锁：仅持有者可解锁（Lua脚本校验token）</li>
 *   <li>超时自动释放：避免死锁</li>
 *   <li>阻塞等待：tryLock支持超时等待</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @Autowired
 * private DistributedLock distributedLock;
 *
 * public void deductStock(Long productId) {
 *     String lockKey = "lock:stock:" + productId;
 *     distributedLock.executeWithLock(lockKey, 5, TimeUnit.SECONDS, () -> {
 *         // 业务逻辑：扣减库存
 *         return inventoryService.deduct(productId);
 *     });
 * }
 * }</pre>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLock {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String UNLOCK_LUA_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";

    /**
     * 尝试获取锁（非阻塞）
     *
     * @param key      锁Key
     * @param expire   锁自动过期时间
     * @param timeUnit 时间单位
     * @return 锁令牌（解锁时需要），null表示获取失败
     */
    public String tryLock(String key, long expire, TimeUnit timeUnit) {
        String token = UUID.randomUUID().toString();
        Boolean success = stringRedisTemplate.execute((RedisCallback<Boolean>) connection -> {
            RedisSerializer<String> serializer = stringRedisTemplate.getStringSerializer();
            byte[] keyBytes = serializer.serialize(key);
            byte[] valueBytes = serializer.serialize(token);
            return connection.set(
                    keyBytes,
                    valueBytes,
                    Expiration.from(expire, timeUnit),
                    RedisStringCommands.SetOption.SET_IF_ABSENT
            );
        });
        return Boolean.TRUE.equals(success) ? token : null;
    }

    /**
     * 释放锁（仅持有者可释放）
     */
    public boolean unlock(String key, String token) {
        if (key == null || token == null) {
            return false;
        }
        Long result = stringRedisTemplate.execute(
                new DefaultRedisScript<>(UNLOCK_LUA_SCRIPT, Long.class),
                Collections.singletonList(key),
                token
        );
        return result != null && result > 0;
    }

    /**
     * 续期锁（看门狗机制使用 - 修复 CR-M01）
     *
     * <p>仅当token匹配时才续期，避免误续期他人的锁。</p>
     *
     * @param key      锁Key
     * @param token    锁令牌
     * @param expire   新的过期时间
     * @param timeUnit 时间单位
     * @return true-续期成功；false-锁已被其他线程持有或已过期
     */
    public boolean extend(String key, String token, long expire, TimeUnit timeUnit) {
        if (key == null || token == null) {
            return false;
        }
        String script =
                "if redis.call('get', KEYS[1]) == ARGV[1] then "
                        + "    return redis.call('expire', KEYS[1], ARGV[2]) "
                        + "else "
                        + "    return 0 "
                        + "end";
        Long result = stringRedisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),
                Collections.singletonList(key),
                token,
                String.valueOf(timeUnit.toSeconds(expire))
        );
        return result != null && result > 0;
    }

    /**
     * 带锁执行业务逻辑（阻塞等待）
     *
     * @param key      锁Key
     * @param waitTime 最大等待时间
     * @param leaseTime 锁自动释放时间
     * @param timeUnit 时间单位
     * @param supplier 业务逻辑
     * @return 业务执行结果
     */
    public <T> T executeWithLock(String key, long waitTime, long leaseTime, TimeUnit timeUnit, Supplier<T> supplier) {
        String token = null;
        long start = System.currentTimeMillis();
        while (true) {
            token = tryLock(key, leaseTime, timeUnit);
            if (token != null) {
                break;
            }
            if (System.currentTimeMillis() - start > waitTime * 1000) {
                throw new LockAcquisitionException("获取锁超时: " + key);
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LockAcquisitionException("获取锁被中断: " + key, e);
            }
        }
        try {
            return supplier.get();
        } finally {
            unlock(key, token);
        }
    }

    /**
     * 锁获取异常
     */
    public static class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(String message) {
            super(message);
        }
        public LockAcquisitionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
