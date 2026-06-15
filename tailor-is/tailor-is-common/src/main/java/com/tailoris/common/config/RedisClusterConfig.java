package com.tailoris.common.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis Cluster 配置 — 为核心服务使用（order, payment, ai, copyright, merchant, product）
 * <p>
 * 通过 {@code spring.profiles.active=cluster} 或 {@code tailoris.redis.mode=cluster} 启用。
 */
@Configuration
@ConditionalOnProperty(name = "tailoris.redis.mode", havingValue = "cluster", matchIfMissing = false)
public class RedisClusterConfig {

    @Value("${tailoris.redis.cluster.nodes:}")
    private String clusterNodes;

    @Value("${tailoris.redis.cluster.max-redirects:3}")
    private int maxRedirects;

    @Value("${tailoris.redis.password:}")
    private String password;

    @Value("${tailoris.redis.cluster.timeout:5000}")
    private long timeoutMs;

    @Value("${tailoris.redis.cluster.pool.max-active:50}")
    private int poolMaxActive;

    @Value("${tailoris.redis.cluster.pool.max-idle:20}")
    private int poolMaxIdle;

    @Value("${tailoris.redis.cluster.pool.min-idle:5}")
    private int poolMinIdle;

    @Value("${tailoris.redis.cluster.pool.max-wait:3000}")
    private long poolMaxWaitMs;

    @Bean(name = "clusterRedisConnectionFactory")
    public RedisConnectionFactory clusterRedisConnectionFactory() {
        // ── 1. Cluster topology ──────────────────────────────────────────
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration();
        List<RedisNode> nodes = parseClusterNodes(clusterNodes);
        clusterConfig.setClusterNodes(nodes);
        clusterConfig.setMaxRedirects(maxRedirects);
        if (password != null && !password.isEmpty()) {
            clusterConfig.setPassword(RedisPassword.of(password));
        }

        // ── 2. Lettuce client options (socket + topology refresh) ───────
        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .keepAlive(true)
                .build();

        ClusterTopologyRefreshOptions topologyRefresh = ClusterTopologyRefreshOptions.builder()
                .enablePeriodicRefresh(Duration.ofMinutes(5))
                .enableAllAdaptiveRefreshTriggers()
                .build();

        ClientOptions clientOptions = ClusterClientOptions.builder()
                .socketOptions(socketOptions)
                .topologyRefreshOptions(topologyRefresh)
                .build();

        // ── 3. Connection pool ───────────────────────────────────────────
        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(poolMaxActive);
        poolConfig.setMaxIdle(poolMaxIdle);
        poolConfig.setMinIdle(poolMinIdle);
        poolConfig.setMaxWait(Duration.ofMillis(poolMaxWaitMs));
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestWhileIdle(true);

        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig)
                .clientOptions(clientOptions)
                .commandTimeout(Duration.ofMillis(timeoutMs))
                .shutdownTimeout(Duration.ZERO)
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(clusterConfig, clientConfig);
        factory.afterPropertiesSet();
        return factory;
    }

    // ──────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 解析 cluster nodes 字符串，格式: host1:port1,host2:port2,...
     */
    private List<RedisNode> parseClusterNodes(String nodesStr) {
        List<RedisNode> nodes = new ArrayList<>();
        if (nodesStr == null || nodesStr.isBlank()) {
            throw new IllegalArgumentException("tailoris.redis.cluster.nodes 不能为空");
        }
        String[] parts = nodesStr.split(",");
        for (String part : parts) {
            String[] hp = part.trim().split(":");
            if (hp.length == 2) {
                nodes.add(new RedisNode(hp[0].trim(), Integer.parseInt(hp[1].trim())));
            }
        }
        return nodes;
    }
}
