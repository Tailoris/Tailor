package com.tailoris.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 云端分布式模型配置。
 *
 * <p>用于特殊体型和热门款式的高精度纸样生成，通过云端分布式计算集群处理。
 * 支持多端点负载均衡、请求优先级队列和自动重试。</p>
 *
 * <p>配置项说明：</p>
 * <ul>
 *   <li>api-endpoints: 云端API端点列表（支持负载均衡）</li>
 *   <li>api-key: 云端API认证密钥</li>
 *   <li>timeout-ms: 请求超时时间（毫秒）</li>
 *   <li>max-retries: 最大重试次数</li>
 *   <li>retry-delay-ms: 重试延迟（毫秒）</li>
 *   <li>priority-queue-size: 优先级队列容量</li>
 *   <li>batch-size: 云端批量推理大小</li>
 *   <li>circuit-breaker-threshold: 熔断器失败阈值</li>
 *   <li>circuit-breaker-recovery-ms: 熔断器恢复时间（毫秒）</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "tailoris.ai.cloud-model")
public class CloudModelConfig {

    /** 云端API端点列表（支持多节点负载均衡） */
    private List<String> apiEndpoints = new ArrayList<>();

    /** 云端API认证密钥 */
    private String apiKey = "";

    /** 请求超时时间（毫秒） */
    private long timeoutMs = 30000;

    /** 连接超时时间（毫秒） */
    private long connectTimeoutMs = 5000;

    /** 最大重试次数 */
    private int maxRetries = 3;

    /** 重试延迟（毫秒） */
    private long retryDelayMs = 1000;

    /** 重试延迟增长因子（指数退避） */
    private double retryBackoffMultiplier = 2.0;

    /** 优先级队列容量 */
    private int priorityQueueSize = 500;

    /** 云端批量推理大小 */
    private int batchSize = 32;

    /** 熔断器失败阈值（连续失败次数） */
    private int circuitBreakerThreshold = 5;

    /** 熔断器恢复时间（毫秒） */
    private long circuitBreakerRecoveryMs = 60000;

    /** 是否启用负载均衡 */
    private boolean loadBalancingEnabled = true;

    /** 负载均衡策略: ROUND_ROBIN / WEIGHTED / LEAST_CONNECTIONS */
    private String loadBalanceStrategy = "ROUND_ROBIN";

    /**
     * 当前熔断器状态：连续失败计数。
     */
    private volatile int consecutiveFailures = 0;

    /**
     * 熔断器是否已打开（触发熔断）。
     */
    private volatile boolean circuitOpen = false;

    /**
     * 熔断器打开的时间戳。
     */
    private volatile long circuitOpenedAt = 0;

    /**
     * 记录一次请求失败，判断是否需要打开熔断器。
     */
    public synchronized void recordFailure() {
        consecutiveFailures++;
        if (consecutiveFailures >= circuitBreakerThreshold) {
            circuitOpen = true;
            circuitOpenedAt = System.currentTimeMillis();
        }
    }

    /**
     * 记录一次请求成功，重置失败计数。
     */
    public synchronized void recordSuccess() {
        consecutiveFailures = 0;
        circuitOpen = false;
        circuitOpenedAt = 0;
    }

    /**
     * 判断熔断器是否可用（已关闭或已过半恢复期）。
     */
    public boolean isCircuitAvailable() {
        if (!circuitOpen) {
            return true;
        }
        long elapsed = System.currentTimeMillis() - circuitOpenedAt;
        if (elapsed >= circuitBreakerRecoveryMs) {
            // 半开状态：允许一次探测请求
            return true;
        }
        return false;
    }
}
