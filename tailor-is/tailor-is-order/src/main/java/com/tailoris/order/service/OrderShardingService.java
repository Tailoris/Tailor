package com.tailoris.order.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单分片服务
 *
 * <p>基于 merchant_id 自动选择分片，支持高频商户独立分片路由。
 * 与 ShardingSphere 分片配置（Task 4）配合使用。</p>
 *
 * <p>分片策略：</p>
 * <ul>
 *   <li>高频商户（订单量超过阈值）：分配到独立分片 shard_3</li>
 *   <li>普通商户：按 merchant_id.hashCode() % 3 分配到 shard_0 ~ shard_2</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 2.0.0
 */
@Slf4j
@Service
public class OrderShardingService {

    private final StringRedisTemplate stringRedisTemplate;

    /** Redis 缓存商户分片映射的 Key 前缀 */
    private static final String MERCHANT_SHARD_KEY = "order:shard:merchant:";
    /** 高频商户统计窗口（分钟） */
    private static final Duration STATS_WINDOW = Duration.ofMinutes(60);
    /** 分片映射缓存过期时间（24 小时） */
    private static final Duration SHARD_MAPPING_TTL = Duration.ofHours(24);

    /** 独立高频分片索引 */
    private static final int HIGH_FREQ_SHARD_INDEX = 3;
    /** 普通分片数量 */
    private static final int NORMAL_SHARD_COUNT = 3;

    /** 本地缓存：高频商户集合（内存级快速判断） */
    private final Map<Long, Integer> merchantShardCache = new ConcurrentHashMap<>();

    /** 高频商户订单量阈值（默认 1000 单/小时） */
    @Value("${tailoris.order.sharding.high-freq-threshold:1000}")
    private int highFreqThreshold;

    /** 总分片数 */
    @Value("${tailoris.order.sharding.total-shards:4}")
    private int totalShards;

    public OrderShardingService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 根据 merchant_id 获取目标分片索引
     *
     * @param merchantId 商户 ID
     * @return 分片索引 (0 ~ 3)
     */
    public int getShardIndex(Long merchantId) {
        if (merchantId == null) {
            return 0;
        }

        // 1. 先查本地缓存
        Integer cachedShard = merchantShardCache.get(merchantId);
        if (cachedShard != null) {
            return cachedShard;
        }

        // 2. 查 Redis 分片映射
        String shardKey = MERCHANT_SHARD_KEY + merchantId;
        String cached = stringRedisTemplate.opsForValue().get(shardKey);
        if (cached != null) {
            int shardIndex = Integer.parseInt(cached);
            merchantShardCache.put(merchantId, shardIndex);
            return shardIndex;
        }

        // 3. 判断是否为高频商户
        int shardIndex = determineShard(merchantId);

        // 4. 写入缓存
        stringRedisTemplate.opsForValue().set(shardKey, String.valueOf(shardIndex), SHARD_MAPPING_TTL);
        merchantShardCache.put(merchantId, shardIndex);

        log.debug("商户分片路由: merchantId={}, shardIndex={}, isHighFreq={}",
                merchantId, shardIndex, shardIndex == HIGH_FREQ_SHARD_INDEX);
        return shardIndex;
    }

    /**
     * 记录商户订单量统计（用于动态判断高频商户）
     *
     * @param merchantId 商户 ID
     */
    public void recordOrder(Long merchantId) {
        String statsKey = "order:shard:stats:" + merchantId;
        Long count = stringRedisTemplate.opsForValue().increment(statsKey);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(statsKey, STATS_WINDOW);
        }
    }

    /**
     * 判断商户是否为高频商户
     *
     * @param merchantId 商户 ID
     * @return true 如果当前窗口内订单量超过阈值
     */
    public boolean isHighFrequencyMerchant(Long merchantId) {
        String statsKey = "order:shard:stats:" + merchantId;
        String countStr = stringRedisTemplate.opsForValue().get(statsKey);
        if (countStr == null) {
            return false;
        }
        try {
            return Long.parseLong(countStr) >= highFreqThreshold;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 清除商户分片缓存（用于商户降级或手动迁移）
     *
     * @param merchantId 商户 ID
     */
    public void evictShardCache(Long merchantId) {
        merchantShardCache.remove(merchantId);
        stringRedisTemplate.delete(MERCHANT_SHARD_KEY + merchantId);
        stringRedisTemplate.delete("order:shard:stats:" + merchantId);
        log.info("已清除商户分片缓存: merchantId={}", merchantId);
    }

    /**
     * 内部方法：确定分片索引
     *
     * <p>高频商户路由到 shard_3，普通商户按 hashCode % 3 分配到 shard_0 ~ shard_2。</p>
     */
    private int determineShard(Long merchantId) {
        if (isHighFrequencyMerchant(merchantId)) {
            return HIGH_FREQ_SHARD_INDEX;
        }
        return Math.abs(merchantId.hashCode()) % NORMAL_SHARD_COUNT;
    }
}
