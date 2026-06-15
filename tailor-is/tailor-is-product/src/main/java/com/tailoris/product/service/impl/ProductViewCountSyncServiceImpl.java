package com.tailoris.product.service.impl;

import com.tailoris.product.entity.Product;
import com.tailoris.product.mapper.ProductMapper;
import com.tailoris.product.service.ProductViewCountSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 商品浏览量同步服务实现 - 修复 B-M33
 *
 * <p>将Redis中累积的浏览量批量同步到数据库。</p>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductViewCountSyncServiceImpl implements ProductViewCountSyncService {

    /** Redis键前缀 */
    private static final String VIEW_COUNT_KEY_PREFIX = "product:view:count:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ProductMapper productMapper;

    @Value("${tailoris.product.view-count.sync-batch:100}")
    private int syncBatch;

    @Override
    public int syncAllViewCounts() {
        int total = 0;
        try {
            // 使用SCAN避免阻塞（生产环境大量key时不能用KEYS）
            ScanOptions options = ScanOptions.scanOptions()
                    .match(VIEW_COUNT_KEY_PREFIX + "*")
                    .count(syncBatch)
                    .build();

            Map<Long, Integer> updateMap = new HashMap<>();
            try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    String key = cursor.next();
                    String value = stringRedisTemplate.opsForValue().get(key);
                    if (value == null) {
                        continue;
                    }
                    try {
                        String productIdStr = key.substring(VIEW_COUNT_KEY_PREFIX.length());
                        Long productId = Long.parseLong(productIdStr);
                        Integer viewCount = Integer.parseInt(value);
                        updateMap.put(productId, viewCount);
                    } catch (NumberFormatException e) {
                        log.warn("无效的浏览量key: {}", key);
                    }

                    // 批量更新
                    if (updateMap.size() >= syncBatch) {
                        total += batchUpdate(updateMap);
                        updateMap.clear();
                    }
                }
            }

            // 处理剩余
            if (!updateMap.isEmpty()) {
                total += batchUpdate(updateMap);
            }
        } catch (Exception e) {
            log.error("同步商品浏览量失败", e);
        }
        return total;
    }

    private int batchUpdate(Map<Long, Integer> updateMap) {
        int count = 0;
        for (Map.Entry<Long, Integer> entry : updateMap.entrySet()) {
            try {
                Product product = new Product();
                product.setId(entry.getKey());
                product.setViewCount(entry.getValue());
                productMapper.updateById(product);
                count++;
            } catch (Exception e) {
                log.error("更新商品浏览量失败: productId={}", entry.getKey(), e);
            }
        }
        return count;
    }
}
