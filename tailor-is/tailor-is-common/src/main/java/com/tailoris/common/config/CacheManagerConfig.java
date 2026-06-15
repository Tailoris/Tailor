package com.tailoris.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheManagerConfig {

    @Bean
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(600, TimeUnit.SECONDS)
                .recordStats());
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }

    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new SafeJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    @Bean
    @Primary
    public CacheManager compositeCacheManager(
            @Autowired CacheManager caffeineCacheManager,
            @Autowired(required = false) CacheManager redisCacheManager) {
        if (redisCacheManager != null) {
            return new CompositeCacheManager(caffeineCacheManager, redisCacheManager);
        }
        return caffeineCacheManager;
    }

    @Bean
    public Cache l1Cache(@Autowired CacheManager compositeCacheManager) {
        return compositeCacheManager.getCache("l1-cache");
    }

    @Bean
    @ConditionalOnBean(name = "redisCacheManager")
    public Cache l2Cache(@Autowired CacheManager redisCacheManager) {
        return redisCacheManager.getCache("l2-cache");
    }
}
