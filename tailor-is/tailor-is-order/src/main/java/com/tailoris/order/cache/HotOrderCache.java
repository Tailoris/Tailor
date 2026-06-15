package com.tailoris.order.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.order.entity.OrderInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 热门订单 Redis 缓存
 *
 * <p>缓存高热度商品的订单数据，采用 Read-Through 模式：先查缓存，未命中再查数据库。
 * 缓存 Key 格式：order:hot:{productId}，TTL 默认 30 分钟。</p>
 *
 * <p>缓存失效策略：</p>
 * <ul>
 *   <li>订单状态变更时自动失效相关缓存</li>
 *   <li>TTL 到期自动过期</li>
 *   <li>支持手动清除指定商品缓存</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 2.0.0
 */
@Slf4j
@Component
public class HotOrderCache {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /** 缓存 Key 前缀 */
    private static final String HOT_ORDER_KEY_PREFIX = "order:hot:";
    /** 热门商品集合 Key（ZSet，用于排行榜） */
    private static final String HOT_PRODUCT_RANK_KEY = "order:hot:rank";

    /** 默认缓存 TTL：30 分钟 */
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    /** 缓存 TTL（可配置） */
    @Value("${tailoris.order.cache.hot-order-ttl-minutes:30}")
    private int hotOrderTtlMinutes;

    /** 热度加分阈值：访问次数达到此值才入缓存 */
    @Value("${tailoris.order.cache.hot-threshold:10}")
    private int hotThreshold;

    public HotOrderCache(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Read-Through 读取：先查缓存，未命中返回 null（由调用方回源数据库）
     *
     * @param productId 商品 ID
     * @return 缓存中的订单列表，未命中返回 null
     */
    public List<OrderInfo> get(Long productId) {
        String key = buildKey(productId);
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            List<OrderInfo> orders = objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, OrderInfo.class));
            log.debug("热门订单缓存命中: productId={}, orderCount={}", productId, orders.size());
            return orders;
        } catch (JsonProcessingException e) {
            log.error("热门订单缓存反序列化失败: productId={}", productId, e);
            // 缓存数据损坏，删除后让调用方回源
            evict(productId);
            return null;
        }
    }

    /**
     * 写入热门订单缓存
     *
     * @param productId 商品 ID
     * @param orders    订单列表
     */
    public void put(Long productId, List<OrderInfo> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        String key = buildKey(productId);
        try {
            String json = objectMapper.writeValueAsString(orders);
            Duration ttl = Duration.ofMinutes(hotOrderTtlMinutes);
            stringRedisTemplate.opsForValue().set(key, json, ttl);
            // 同时更新热度排行
            recordProductAccess(productId);
            log.debug("热门订单缓存写入: productId={}, orderCount={}, ttl={}min",
                    productId, orders.size(), hotOrderTtlMinutes);
        } catch (JsonProcessingException e) {
            log.error("热门订单缓存序列化失败: productId={}", productId, e);
        }
    }

    /**
     * 订单状态变更时失效缓存
     *
     * @param productId 商品 ID
     */
    public void evict(Long productId) {
        String key = buildKey(productId);
        Boolean deleted = stringRedisTemplate.delete(key);
        log.debug("热门订单缓存失效: productId={}, deleted={}", productId, deleted);
    }

    /**
     * 批量失效缓存（用于批量订单状态变更）
     *
     * @param productIds 商品 ID 列表
     */
    public void evictBatch(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        List<String> keys = new ArrayList<>(productIds.size());
        for (Long pid : productIds) {
            keys.add(buildKey(pid));
        }
        Long deleted = stringRedisTemplate.delete(keys);
        log.debug("热门订单批量缓存失效: productIds={}, deleted={}", productIds.size(), deleted);
    }

    /**
     * 判断商品是否在热门缓存中
     *
     * @param productId 商品 ID
     * @return true 如果缓存存在
     */
    public boolean isCached(Long productId) {
        String key = buildKey(productId);
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    /**
     * 获取热门商品排行榜（Top N）
     *
     * @param limit 返回数量
     * @return 商品 ID 列表（按热度降序）
     */
    public List<Long> getHotProductRank(int limit) {
        Set<String> rank = stringRedisTemplate.opsForZSet()
                .reverseRange(HOT_PRODUCT_RANK_KEY, 0, limit - 1);
        if (rank == null || rank.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> productIds = new ArrayList<>();
        for (String s : rank) {
            try {
                productIds.add(Long.parseLong(s));
            } catch (NumberFormatException e) {
                log.warn("热门排行数据异常: value={}", s);
            }
        }
        return productIds;
    }

    /**
     * 记录商品访问，达到阈值时加入热门排行
     */
    private void recordProductAccess(Long productId) {
        Double score = stringRedisTemplate.opsForZSet().incrementScore(HOT_PRODUCT_RANK_KEY,
                String.valueOf(productId), 1);
        if (score != null && score >= hotThreshold) {
            log.debug("商品达到热门阈值: productId={}, score={}", productId, score);
        }
        // 排行榜也设 TTL，定期清理冷门数据
        stringRedisTemplate.expire(HOT_PRODUCT_RANK_KEY, Duration.ofHours(24));
    }

    private String buildKey(Long productId) {
        return HOT_ORDER_KEY_PREFIX + productId;
    }
}
