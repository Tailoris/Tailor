package com.tailoris.product.service.impl;

import com.tailoris.common.exception.BusinessException;
import com.tailoris.product.entity.Product;
import com.tailoris.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheEnhancedProductService {

    private final RedissonClient redissonClient;
    private final RBloomFilter<Long> productBloomFilter;
    private final ProductMapper productMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PRODUCT_CACHE_KEY = "product:detail:";
    private static final String LOCK_KEY_PREFIX = "lock:product:";
    private static final long CACHE_EXPIRE_SECONDS = 1800;
    private static final long LOCK_WAIT_SECONDS = 5;

    public Product getProductDetail(Long id) {
        if (!productBloomFilter.contains(id)) {
            log.info("Bloom filter miss for product id: {}", id);
            throw new BusinessException("商品不存在");
        }

        String cacheKey = PRODUCT_CACHE_KEY + id;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cached)) {
            try {
                return objectMapper.readValue(cached, Product.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize product cache, key: {}, error: {}", cacheKey, e.getMessage());
                stringRedisTemplate.delete(cacheKey);
            }
        }

        String lockKey = LOCK_KEY_PREFIX + id;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(LOCK_WAIT_SECONDS, TimeUnit.SECONDS)) {
                try {
                    cached = stringRedisTemplate.opsForValue().get(cacheKey);
                    if (StringUtils.hasText(cached)) {
                        try {
                            return objectMapper.readValue(cached, Product.class);
                        } catch (Exception e) {
                            log.warn("Failed to deserialize product cache after lock, key: {}", cacheKey);
                            stringRedisTemplate.delete(cacheKey);
                        }
                    }

                    Product product = productMapper.selectById(id);
                    if (product == null) {
                        throw new BusinessException("商品不存在");
                    }

                    product.setViewCount(product.getViewCount() + 1);
                    productMapper.updateById(product);

                    try {
                        stringRedisTemplate.opsForValue().set(
                                cacheKey,
                                objectMapper.writeValueAsString(product),
                                CACHE_EXPIRE_SECONDS,
                                TimeUnit.SECONDS);
                    } catch (Exception e) {
                        log.warn("Failed to write product cache, key: {}", cacheKey);
                    }

                    return product;
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("Failed to acquire lock for product id: {}", id);
                throw new BusinessException("系统繁忙，请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("系统繁忙，请稍后重试");
        }
    }

    public void addToBloomFilter(Long id) {
        productBloomFilter.add(id);
        log.info("Added product id {} to bloom filter", id);
    }

    public void removeFromBloomFilter(Long id) {
        log.warn("Bloom filter does not support item removal, product id: {}", id);
    }
}