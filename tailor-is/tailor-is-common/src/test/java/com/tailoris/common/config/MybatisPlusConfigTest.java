package com.tailoris.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MybatisPlusConfigTest {

    @Test
    @DisplayName("配置类应能实例化")
    void shouldInstantiateConfig() {
        MybatisPlusConfig config = new MybatisPlusConfig();
        assertNotNull(config);
    }

    @Test
    @DisplayName("配置类不应为空")
    void shouldReturnNonNullConfig() {
        MybatisPlusConfig config = new MybatisPlusConfig();
        assertNotNull(config);
    }
}