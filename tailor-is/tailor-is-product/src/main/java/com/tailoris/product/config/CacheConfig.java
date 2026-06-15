package com.tailoris.product.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.product.entity.Product;
import com.tailoris.product.mapper.ProductMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CacheConfig {

    private final RedissonClient redissonClient;
    private final ProductMapper productMapper;

    private static final String BLOOM_FILTER_NAME = "product:bloom";
    private static final long EXPECTED_INSERTIONS = 10000L;
    private static final double FALSE_PROBABILITY = 0.01;

    private RBloomFilter<Long> bloomFilter;

    @Bean
    public RBloomFilter<Long> productBloomFilter() {
        bloomFilter = redissonClient.getBloomFilter(BLOOM_FILTER_NAME);
        bloomFilter.tryInit(EXPECTED_INSERTIONS, FALSE_PROBABILITY);
        return bloomFilter;
    }

    @PostConstruct
    public void preloadBloomFilter() {
        log.info("Preloading product bloom filter...");
        try {
            if (bloomFilter == null) {
                bloomFilter = productBloomFilter();
            }
            LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
            wrapper.select(Product::getId);
            List<Product> products = productMapper.selectList(wrapper);
            for (Product product : products) {
                bloomFilter.add(product.getId());
            }
            log.info("Bloom filter preloaded with {} product IDs", products.size());
        } catch (Exception e) {
            log.warn("Failed to preload bloom filter, will continue without it: {}", e.getMessage());
        }
    }
}