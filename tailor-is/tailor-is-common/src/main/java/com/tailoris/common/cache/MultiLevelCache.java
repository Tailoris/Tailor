package com.tailoris.common.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
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
 * <h3>安全修复</h3>
 * <ul>
 *   <li><b>H-21</b>: 修复构造函数注入缺陷，使用构造函数注入配置参数</li>
 *   <li><b>H-22</b>: 使用 SCAN 命令替代 KEYS 命令，避免阻塞 Redis</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since Sprint 9
 */
@Slf4j
@Component
public class MultiLevelCache<K, V> {

    private final Cache<K, V> l1Cache;
    private final RedisTemplate<String, V> redisTemplate;
    // 🔒 L-03修复: 移除未使用的默认 TTL 字段（仅被未被调用的 3 参数 get 方法使用）

    /**
     * 构造函数注入（修复 H-21）
     * 
     * @param redisTemplate Redis 模板
     * @param l2DefaultTtlSeconds 默认过期时间（秒）
     * @param maximumSize L1 缓存最大条目数
     * @param l1ExpireMinutes L1 缓存过期时间（分钟）
     */
    public MultiLevelCache(RedisTemplate<String, V> redisTemplate,
                          @org.springframework.beans.factory.annotation.Value("${cache.l2.default-ttl-seconds:300}") long l2DefaultTtlSeconds,
                          @org.springframework.beans.factory.annotation.Value("${cache.l1.maximum-size:10000}") long maximumSize,
                          @org.springframework.beans.factory.annotation.Value("${cache.l1.expire-minutes:5}") long l1ExpireMinutes) {
        this.redisTemplate = redisTemplate;
        // 🔒 L-03修复: 移除未使用的默认 TTL 字段及其赋值

        // 使用构造函数注入的配置（修复 H-21）
        this.l1Cache = Caffeine.newBuilder()
                .maximumSize(maximumSize > 0 ? maximumSize : 10_000)
                .expireAfterWrite(l1ExpireMinutes > 0 ? l1ExpireMinutes : 5, TimeUnit.MINUTES)
                .recordStats()
                .build();

        log.info("MultiLevelCache initialized: L1 maxSize={}, L1 expire={}min, L2 defaultTtl={}s",
                maximumSize, l1ExpireMinutes, l2DefaultTtlSeconds);
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
     * 
     * 🔒 修复 H-22: 使用 SCAN 命令替代 KEYS 命令，避免阻塞 Redis
     */
    public void invalidateByPrefix(String l2KeyPrefix) {
        try {
            // 使用 SCAN 命令替代 KEYS 命令（修复 H-22）
            String pattern = l2KeyPrefix + ":*";
            Set<String> keys = scanKeys(pattern);
            
            if (keys != null && !keys.isEmpty()) {
                log.debug("Invalidating {} keys with pattern {}", keys.size(), pattern);
                redisTemplate.delete(keys);
            }
            
            // L1 全清
            l1Cache.invalidateAll();
        } catch (Exception e) {
            log.error("Invalidate by prefix failed: {}", l2KeyPrefix, e);
        }
    }

    /**
     * 使用 SCAN 命令扫描匹配的 key（修复 H-22）
     * 
     * @param pattern 匹配模式
     * @return 匹配的 key 集合
     */
    private Set<String> scanKeys(String pattern) {
        return redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> keys = new HashSet<>();
            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(1000)
                    .build();
            
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    byte[] keyBytes = cursor.next();
                    String key = new String(keyBytes, StandardCharsets.UTF_8);
                    keys.add(key);
                    
                    // 防止一次性扫描过多 key
                    if (keys.size() > 10000) {
                        log.warn("SCAN found more than 10000 keys, stopping scan");
                        break;
                    }
                }
            }
            
            return keys;
        });
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
