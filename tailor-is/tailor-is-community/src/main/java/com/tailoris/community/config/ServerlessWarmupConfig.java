package com.tailoris.community.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Serverless 冷启动预热配置.
 *
 * <p>在 Serverless 环境下，函数实例首次启动（冷启动）需要加载 Spring 上下文、
 * 初始化数据库连接池、预热缓存等。本配置类在应用启动后立即执行预热操作，
 * 确保首次请求的响应时间接近热启动。</p>
 *
 * <h3>预热策略</h3>
 * <ul>
 *   <li>数据库连接池预热：执行一次简单查询，初始化连接</li>
 *   <li>Redis 连接预热：PING 检查 Redis 连接可用性</li>
 *   <li>预加载热门数据：从数据库加载高频访问数据到本地缓存</li>
 *   <li>定时自检：定期检查服务健康状态，保持实例活跃</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ServerlessWarmupConfig {

    private final ApplicationContext applicationContext;
    private final DataSource dataSource;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    private final AtomicBoolean warmedUp = new AtomicBoolean(false);

    /**
     * 应用启动后立即执行预热.
     */
    @PostConstruct
    public void warmup() {
        if (!"serverless".equals(activeProfile)) {
            log.info("非 Serverless 环境，跳过预热");
            return;
        }

        log.info("开始 Serverless 冷启动预热...");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 数据库连接池预热
            warmupDatabase();

            // 2. Redis 连接预热
            warmupRedis();

            // 3. 预加载频繁访问数据
            preloadFrequentData();

            warmedUp.set(true);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Serverless 冷启动预热完成，耗时: {}ms", elapsed);
        } catch (Exception e) {
            log.error("Serverless 预热过程中出现异常，应用仍可正常服务", e);
        }
    }

    /**
     * 数据库连接池预热.
     * <p>执行一次简单查询，触发连接池初始化。
     * 避免首次业务请求时等待连接建立。</p>
     */
    private void warmupDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().executeQuery("SELECT 1");
            log.info("数据库连接池预热完成");
        } catch (Exception e) {
            log.warn("数据库连接池预热失败: {}", e.getMessage());
        }
    }

    /**
     * Redis 连接预热.
     * <p>发送 PING 命令验证 Redis 连接可用。</p>
     */
    private void warmupRedis() {
        try {
            String pong = stringRedisTemplate.getConnectionFactory()
                    .getConnection().ping();
            if ("PONG".equals(pong)) {
                log.info("Redis 连接预热完成");
            }
        } catch (Exception e) {
            log.warn("Redis 连接预热失败: {}", e.getMessage());
        }
    }

    /**
     * 预加载频繁访问的数据.
     * <p>将热门帖子、配置等高频数据提前加载到本地缓存或 Redis，减少首次请求延迟。</p>
     */
    private void preloadFrequentData() {
        try {
            // 预热常用配置缓存
            stringRedisTemplate.opsForValue().get("community:config:warmup");

            // 触发应用上下文中的缓存加载器
            applicationContext.getBeansOfType(Object.class).forEach((name, bean) -> {
                // 如有实现 WarmupCapable 接口的 Bean，自动触发预热
                if (bean instanceof WarmupCapable wc) {
                    try {
                        wc.warmup();
                    } catch (Exception e) {
                        log.warn("Bean {} 预热失败: {}", name, e.getMessage());
                    }
                }
            });

            log.info("频繁访问数据预加载完成");
        } catch (Exception e) {
            log.warn("预加载频繁数据失败: {}", e.getMessage());
        }
    }

    /**
     * 定时自检 - 保持实例活跃.
     * <p>在 Serverless 环境下，定时发送健康检查请求可以防止实例被回收。
     * 每 4 分钟执行一次，确保在实例空闲超时前保持活跃。</p>
     */
    @Scheduled(fixedRate = 240_000, initialDelay = 120_000)
    public void keepAlive() {
        if (!warmedUp.get()) {
            return;
        }

        try {
            // 简单自检，保持连接活跃
            stringRedisTemplate.opsForValue().get("community:heartbeat:ts");
        } catch (Exception e) {
            log.warn("定时自检异常: {}", e.getMessage());
        }
    }

    /**
     * 注册 JVM 关闭钩子.
     * <p>在函数实例被销毁前执行清理操作：关闭连接池、释放资源。</p>
     */
    @PreDestroy
    public void shutdown() {
        log.info("Serverless 实例即将销毁，执行清理操作...");
        try {
            warmedUp.set(false);
            // 连接池由 Spring 自动管理，此处仅记录日志
            log.info("Serverless 实例清理完成");
        } catch (Exception e) {
            log.warn("清理操作异常: {}", e.getMessage());
        }
    }

    /**
     * 预热能力接口.
     * <p>需要预热的 Bean 实现此接口，将在冷启动时自动调用。</p>
     */
    public interface WarmupCapable {
        void warmup();
    }
}