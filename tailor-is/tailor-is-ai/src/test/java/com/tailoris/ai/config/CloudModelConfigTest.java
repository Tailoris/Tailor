package com.tailoris.ai.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CloudModelConfig 单元测试")
class CloudModelConfigTest {

    @Test
    @DisplayName("默认配置值")
    void testDefaultValues() {
        CloudModelConfig config = new CloudModelConfig();

        assertNotNull(config.getApiEndpoints());
        assertTrue(config.getApiEndpoints().isEmpty());
        assertEquals("", config.getApiKey());
        assertEquals(30000, config.getTimeoutMs());
        assertEquals(5000, config.getConnectTimeoutMs());
        assertEquals(3, config.getMaxRetries());
        assertEquals(1000, config.getRetryDelayMs());
        assertEquals(2.0, config.getRetryBackoffMultiplier());
        assertEquals(500, config.getPriorityQueueSize());
        assertEquals(32, config.getBatchSize());
        assertEquals(5, config.getCircuitBreakerThreshold());
        assertEquals(60000, config.getCircuitBreakerRecoveryMs());
        assertTrue(config.isLoadBalancingEnabled());
        assertEquals("ROUND_ROBIN", config.getLoadBalanceStrategy());
    }

    @Test
    @DisplayName("熔断器 - 初始状态关闭")
    void testCircuitBreaker_InitialState() {
        CloudModelConfig config = new CloudModelConfig();

        assertEquals(0, config.getConsecutiveFailures());
        assertFalse(config.isCircuitOpen());
        assertEquals(0, config.getCircuitOpenedAt());
    }

    @Test
    @DisplayName("熔断器 - 记录失败未达到阈值")
    void testRecordFailure_BelowThreshold() {
        CloudModelConfig config = new CloudModelConfig();
        config.setCircuitBreakerThreshold(5);

        config.recordFailure();
        assertEquals(1, config.getConsecutiveFailures());
        assertFalse(config.isCircuitOpen());

        config.recordFailure();
        assertEquals(2, config.getConsecutiveFailures());
        assertFalse(config.isCircuitOpen());
    }

    @Test
    @DisplayName("熔断器 - 记录失败达到阈值打开")
    void testRecordFailure_ReachesThreshold() {
        CloudModelConfig config = new CloudModelConfig();
        config.setCircuitBreakerThreshold(3);

        config.recordFailure();
        config.recordFailure();
        config.recordFailure();

        assertEquals(3, config.getConsecutiveFailures());
        assertTrue(config.isCircuitOpen());
        assertTrue(config.getCircuitOpenedAt() > 0);
    }

    @Test
    @DisplayName("熔断器 - 记录成功重置状态")
    void testRecordSuccess_ResetsState() {
        CloudModelConfig config = new CloudModelConfig();
        config.setCircuitBreakerThreshold(3);

        config.recordFailure();
        config.recordFailure();
        config.recordFailure();
        assertTrue(config.isCircuitOpen());

        config.recordSuccess();
        assertEquals(0, config.getConsecutiveFailures());
        assertFalse(config.isCircuitOpen());
        assertEquals(0, config.getCircuitOpenedAt());
    }

    @Test
    @DisplayName("熔断器 - 关闭时可用")
    void testIsCircuitAvailable_Closed() {
        CloudModelConfig config = new CloudModelConfig();

        assertTrue(config.isCircuitAvailable());
    }

    @Test
    @DisplayName("熔断器 - 打开且未过恢复期不可用")
    void testIsCircuitAvailable_OpenNotRecovered() {
        CloudModelConfig config = new CloudModelConfig();
        config.setCircuitBreakerThreshold(1);
        config.setCircuitBreakerRecoveryMs(60000);

        config.recordFailure();
        assertTrue(config.isCircuitOpen());

        // 刚打开，还没过恢复期
        assertFalse(config.isCircuitAvailable());
    }

    @Test
    @DisplayName("熔断器 - 打开且已过恢复期可用")
    void testIsCircuitAvailable_OpenRecovered() {
        CloudModelConfig config = new CloudModelConfig();
        config.setCircuitBreakerThreshold(1);
        config.setCircuitBreakerRecoveryMs(100);

        config.recordFailure();
        assertTrue(config.isCircuitOpen());

        // 等待超过恢复期
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertTrue(config.isCircuitAvailable());
    }

    @Test
    @DisplayName("设置器 - 修改配置")
    void testSetters() {
        CloudModelConfig config = new CloudModelConfig();

        config.setApiKey("test-key");
        assertEquals("test-key", config.getApiKey());

        config.setTimeoutMs(5000);
        assertEquals(5000, config.getTimeoutMs());

        config.setMaxRetries(5);
        assertEquals(5, config.getMaxRetries());
    }

    @Test
    @DisplayName("并发 - 记录失败同步")
    void testRecordFailure_Synchronized() throws InterruptedException {
        CloudModelConfig config = new CloudModelConfig();
        config.setCircuitBreakerThreshold(100);

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                config.recordFailure();
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                config.recordFailure();
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertEquals(100, config.getConsecutiveFailures());
    }
}
