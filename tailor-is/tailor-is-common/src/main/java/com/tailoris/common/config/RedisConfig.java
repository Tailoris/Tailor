package com.tailoris.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置
 *
 * <p>配置 RedisTemplate 序列化策略和 TTL 策略。
 * 所有通过 RedisTemplate 写入的数据，默认过期时间可通过
 * {@code tailoris.redis.default-ttl-minutes} 配置项控制。</p>
 */
@Configuration
public class RedisConfig {

    /**
     * 创建 RedisTemplate Bean
     *
     * <p>配置要点：</p>
     * <ul>
     *   <li>Key 使用 String 序列化</li>
     *   <li>Value 使用 JSON 序列化（兼容反序列化异常）</li>
     *   <li>Hash Key/Value 同上</li>
     * </ul>
     *
     * @param connectionFactory Redis 连接工厂
     * @return 配置好的 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new SafeJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new SafeJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
