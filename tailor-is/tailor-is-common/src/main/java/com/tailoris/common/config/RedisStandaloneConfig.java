package com.tailoris.common.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;

import java.time.Duration;

/**
 * Redis Standalone 配置 — 为非核心服务使用（community, academy, supply）
 * <p>
 * 通过 {@code spring.profiles.active=lite} 或 {@code tailoris.redis.mode=standalone} 启用。
 * 使用更轻量的资源分配：更小的连接池、更短的超时。
 */
@Configuration
@ConditionalOnProperty(name = "tailoris.redis.mode", havingValue = "standalone", matchIfMissing = false)
public class RedisStandaloneConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.database:0}")
    private int database;

    @Value("${tailoris.redis.standalone.timeout:2000}")
    private long timeoutMs;

    @Value("${tailoris.redis.standalone.pool.max-active:10}")
    private int poolMaxActive;

    @Value("${tailoris.redis.standalone.pool.max-idle:5}")
    private int poolMaxIdle;

    @Value("${tailoris.redis.standalone.pool.min-idle:1}")
    private int poolMinIdle;

    @Value("${tailoris.redis.standalone.pool.max-wait:2000}")
    private long poolMaxWaitMs;

    @Bean(name = "standaloneRedisConnectionFactory")
    public RedisConnectionFactory standaloneRedisConnectionFactory() {
        // ── 1. Standalone config ─────────────────────────────────────────
        RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
        standaloneConfig.setHostName(host);
        standaloneConfig.setPort(port);
        standaloneConfig.setDatabase(database);
        if (password != null && !password.isEmpty()) {
            standaloneConfig.setPassword(RedisPassword.of(password));
        }

        // ── 2. Connection pool (lighter than cluster) ───────────────────
        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(poolMaxActive);
        poolConfig.setMaxIdle(poolMaxIdle);
        poolConfig.setMinIdle(poolMinIdle);
        poolConfig.setMaxWait(Duration.ofMillis(poolMaxWaitMs));

        // ── 3. Socket options ────────────────────────────────────────────
        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();

        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(socketOptions)
                .build();

        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig)
                .clientOptions(clientOptions)
                .commandTimeout(Duration.ofMillis(timeoutMs))
                .shutdownTimeout(Duration.ZERO)
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(standaloneConfig, clientConfig);
        factory.afterPropertiesSet();
        return factory;
    }
}
