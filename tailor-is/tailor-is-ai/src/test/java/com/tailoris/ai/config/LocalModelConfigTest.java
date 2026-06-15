package com.tailoris.ai.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LocalModelConfig 单元测试")
class LocalModelConfigTest {

    @Test
    @DisplayName("默认配置值")
    void testDefaultValues() {
        LocalModelConfig config = new LocalModelConfig();

        assertEquals("/opt/tailoris/models/pattern-lightweight.onnx", config.getModelPath());
        assertTrue(config.isGpuEnabled());
        assertEquals(0, config.getGpuDeviceId());
        assertEquals(8, config.getBatchSize());
        assertEquals(4, config.getMaxThreads());
        assertEquals(3000, config.getTimeoutMs());
        assertEquals(2048, config.getMaxMemoryMb());
        assertTrue(config.isFallbackToCloud());
        assertEquals(60, config.getHealthCheckIntervalSeconds());
        assertTrue(config.isAvailable());
    }

    @Test
    @DisplayName("标记不可用")
    void testMarkUnavailable() {
        LocalModelConfig config = new LocalModelConfig();
        assertTrue(config.isAvailable());

        config.markUnavailable();
        assertFalse(config.isAvailable());
    }

    @Test
    @DisplayName("标记可用")
    void testMarkAvailable() {
        LocalModelConfig config = new LocalModelConfig();
        config.markUnavailable();
        assertFalse(config.isAvailable());

        config.markAvailable();
        assertTrue(config.isAvailable());
    }

    @Test
    @DisplayName("设置器 - 修改配置")
    void testSetters() {
        LocalModelConfig config = new LocalModelConfig();

        config.setModelPath("/new/path/model.onnx");
        assertEquals("/new/path/model.onnx", config.getModelPath());

        config.setGpuEnabled(false);
        assertFalse(config.isGpuEnabled());

        config.setGpuDeviceId(1);
        assertEquals(1, config.getGpuDeviceId());

        config.setBatchSize(16);
        assertEquals(16, config.getBatchSize());

        config.setMaxThreads(8);
        assertEquals(8, config.getMaxThreads());

        config.setTimeoutMs(5000);
        assertEquals(5000, config.getTimeoutMs());

        config.setMaxMemoryMb(4096);
        assertEquals(4096, config.getMaxMemoryMb());

        config.setFallbackToCloud(false);
        assertFalse(config.isFallbackToCloud());

        config.setHealthCheckIntervalSeconds(120);
        assertEquals(120, config.getHealthCheckIntervalSeconds());
    }

    @Test
    @DisplayName("状态切换 - 多次切换")
    void testMultipleStateChanges() {
        LocalModelConfig config = new LocalModelConfig();

        assertTrue(config.isAvailable());

        config.markUnavailable();
        assertFalse(config.isAvailable());

        config.markAvailable();
        assertTrue(config.isAvailable());

        config.markUnavailable();
        assertFalse(config.isAvailable());
    }
}
