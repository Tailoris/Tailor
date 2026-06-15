package com.tailoris.common.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 多级缓存实现 (Caffeine L1 + Redis L2) - Sprint 9 QA-008
 *
 * <p>提供 L1 本地缓存 + L2 分布式缓存的多级缓存方案，
 * 适用于热点数据查询（如首页 Banner、热门商品、用户 Session 等）。</p>
 *
 * <h3>缓存策略</h3>
 * <ul>
 *   <li><b>L1 (Caffeine)</b>: 进程内本地缓存，微秒级响应，容量有限</li>
 *   <li><b>L2 (Redis)</b>: 跨节点共享缓存，毫秒级响应，容量大</li>
 *   <li><b>DB</b>: 数据库，几十毫秒级</li>
 * </ul>
 *
 * <h3>查询流程</h3>
 * <pre>
 * 1. 查询 L1 缓存
 *    ├─ 命中 → 直接返回
 *    └─ 未命中 ↓
 * 2. 查询 L2 缓存（Redis）
 *    ├─ 命中 → 回写 L1，返回
 *    └─ 未命中 ↓
 * 3. 查询数据库
 *    ├─ 写回 L2（Redis）
 *    ├─ 写回 L1（Caffeine）
 *    └─ 返回
 * </pre>
 *
 * @author Tailor IS Team
 * @since Sprint 9
 */
@Slf4j
@Component
public class MultiLevelCache<K, V> {

    private final Cache<K, V> l1Cache;
    private final RedisTemplate<String, V> redisTemplate;

    @Value("${cache.l2.default-ttl-seconds:300}")
    private long defaultTtlSeconds;

    @Value("${cache.l1.maximum-size:10000}")
    private long maximumSize;

    @Value("${cache.l1.expire-minutes:5}")
    private long l1ExpireMinutes;

    public MultiLevelCache(RedisTemplate<String, V> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.l1Cache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    /**
     * 获取缓存值（多级回源）
     *
     * @param key          缓存键
     * @param l2KeyPrefix  Redis 键前缀
     * @param ttl          Redis 过期时间
     * @param dbLoader     数据库加载函数
     * @return 缓存值
     */
    public V get(K key, String l2KeyPrefix, Duration ttl, Function<K, V> dbLoader) {
        // 1. L1 缓存查询
        V value = l1Cache.getIfPresent(key);
        if (value != null) {
            log.debug("L1 cache HIT for key={}", key);
            return value;
        }

        // 2. L2 缓存查询（Redis）
        String redisKey = l2KeyPrefix + ":" + key;
        try {
            value = redisTemplate.opsForValue().get(redisKey);
            if (value != null) {
                log.debug("L2 cache HIT for key={}", redisKey);
                l1Cache.put(key, value);  // 回写 L1
                return value;
            }
        } catch (Exception e) {
            log.error("Redis read failed for key={}", redisKey, e);
            // Redis 异常不影响业务，继续查 DB
        }

        // 3. DB 查询
        log.debug("Cache MISS for key={}, loading from DB", key);
        value = dbLoader.apply(key);
        if (value != null) {
            // 写回 L1
            l1Cache.put(key, value);

            // 写回 L2
            try {
                redisTemplate.opsForValue().set(redisKey, value, ttl);
            } catch (Exception e) {
                log.error("Redis write failed for key={}", redisKey, e);
            }
        }

        return value;
    }

    /**
     * 主动失效缓存
     */
    public void invalidate(K key, String l2KeyPrefix) {
        // 失效 L1
        l1Cache.invalidate(key);

        // 失效 L2
        try {
            redisTemplate.delete(l2KeyPrefix + ":" + key);
        } catch (Exception e) {
            log.error("Redis delete failed for key={}", key, e);
        }
    }

    /**
     * 失效所有相关缓存（按前缀）
     */
    public void invalidateByPrefix(String l2KeyPrefix) {
        try {
            // Redis 模糊删除
            java.util.Set<String> keys = redisTemplate.keys(l2KeyPrefix + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            // L1 全清（简单粗暴）
            l1Cache.invalidateAll();
        } catch (Exception e) {
            log.error("Invalidate by prefix failed: {}", l2KeyPrefix, e);
        }
    }

    /**
     * 获取缓存统计
     */
    public CacheStats getStats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats stats = l1Cache.stats();
        return new CacheStats(
                stats.hitCount(),
                stats.missCount(),
                stats.hitRate() * 100,
                stats.evictionCount()
        );
    }

    /**
     * 缓存统计信息
     */
    public record CacheStats(long hitCount, long missCount, double hitRate, long evictionCount) {
    }
}
