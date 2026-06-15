package com.tailoris.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 限流动态配置 - 修复 B-H10
 *
 * <p>支持从Nacos配置中心动态调整限流阈值，无需重启服务。</p>
 *
 * <h3>配置示例</h3>
 * <pre>{@code
 * tailoris:
 *   ratelimit:
 *     configs:
 *       login: {permitsPerSecond: 10, capacity: 60}
 *       register: {permitsPerSecond: 5, capacity: 30}
 *       sms: {permitsPerSecond: 1, capacity: 60}
 * }</pre>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "tailoris.ratelimit")
public class RateLimitDynamicConfig {

    /** 各接口的限流配置 */
    private Map<String, RateLimitConfigEntry> configs = new HashMap<>();

    /** 全局开关 */
    private boolean enabled = true;

    @Data
    public static class RateLimitConfigEntry {
        /** 每秒允许的请求数 */
        private int permitsPerSecond = 10;
        /** 令牌桶容量 */
        private int capacity = 60;
        /** 限流提示信息 */
        private String message = "请求过于频繁，请稍后再试";
        /** 限流维度 */
        private String limitType = "IP";
    }

    /**
     * 获取指定key的配置
     */
    public RateLimitConfigEntry getConfig(String key) {
        return configs.getOrDefault(key, defaultConfig());
    }

    private RateLimitConfigEntry defaultConfig() {
        RateLimitConfigEntry entry = new RateLimitConfigEntry();
        return entry;
    }
}
