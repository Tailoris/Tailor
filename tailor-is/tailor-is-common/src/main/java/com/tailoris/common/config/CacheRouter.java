package com.tailoris.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存路由组件 — 根据服务类型自动选择 Redis 实例。
 *
 * <p>核心服务使用 {@code clusterRedisConnectionFactory}，非核心服务使用 {@code standaloneRedisConnectionFactory}。
 * 当只有一个 RedisConnectionFactory 存在时（默认 Spring Boot 自动配置的情况），直接使用该连接工厂。
 */
@Component
public class CacheRouter {

    private final Map<String, RedisTemplate<String, Object>> templateCache = new ConcurrentHashMap<>();

    @Autowired(required = false)
    @Qualifier("clusterRedisConnectionFactory")
    private RedisConnectionFactory clusterConnectionFactory;

    @Autowired(required = false)
    @Qualifier("standaloneRedisConnectionFactory")
    private RedisConnectionFactory standaloneConnectionFactory;

    @Autowired(required = false)
    @Qualifier("redisConnectionFactory")
    private RedisConnectionFactory defaultConnectionFactory;

    /**
     * 获取核心缓存 RedisTemplate（Cluster 模式）
     */
    public RedisTemplate<String, Object> getCoreTemplate() {
        return templateCache.computeIfAbsent("core", k -> {
            RedisConnectionFactory factory = clusterConnectionFactory != null
                    ? clusterConnectionFactory
                    : (defaultConnectionFactory != null ? defaultConnectionFactory : standaloneConnectionFactory);
            return createTemplate(factory);
        });
    }

    /**
     * 获取轻量缓存 RedisTemplate（Standalone 模式）
     */
    public RedisTemplate<String, Object> getLiteTemplate() {
        return templateCache.computeIfAbsent("lite", k -> {
            RedisConnectionFactory factory = standaloneConnectionFactory != null
                    ? standaloneConnectionFactory
                    : (defaultConnectionFactory != null ? defaultConnectionFactory : clusterConnectionFactory);
            return createTemplate(factory);
        });
    }

    /**
     * 根据模式获取对应的 RedisTemplate
     *
     * @param mode "core" 或 "lite"
     */
    public RedisTemplate<String, Object> getTemplate(String mode) {
        if ("core".equalsIgnoreCase(mode)) {
            return getCoreTemplate();
        }
        return getLiteTemplate();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────

    private RedisTemplate<String, Object> createTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new SafeJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new SafeJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
