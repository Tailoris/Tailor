package com.tailoris.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 认证白名单配置测试 - 覆盖 B-H21
 *
 * @author Tailor IS Team
 */
@DisplayName("AuthWhitelistProperties 测试")
class AuthWhitelistPropertiesTest {

    private AuthWhitelistProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AuthWhitelistProperties();
    }

    @Test
    @DisplayName("默认白名单应包含登录注册")
    void shouldIncludeLoginRegisterInDefault() {
        List<String> defaults = AuthWhitelistProperties.getDefaultWhitelist();
        assertNotNull(defaults);
        assertTrue(defaults.contains("/api/auth/login"));
        assertTrue(defaults.contains("/api/auth/register"));
    }

    @Test
    @DisplayName("精确匹配应识别白名单")
    void shouldMatchExactPath() {
        properties.setWhitelist(Arrays.asList("/api/auth/login", "/api/auth/register"));
        assertTrue(properties.isWhitelisted("/api/auth/login"));
        assertTrue(properties.isWhitelisted("/api/auth/register"));
        assertFalse(properties.isWhitelisted("/api/auth/logout"));
    }

    @Test
    @DisplayName("通配符匹配应正确工作")
    void shouldMatchWildcardPath() {
        properties.setWhitelist(Arrays.asList("/api/public/**", "/actuator/**"));
        assertTrue(properties.isWhitelisted("/api/public/info"));
        assertTrue(properties.isWhitelisted("/api/public/config/get"));
        assertTrue(properties.isWhitelisted("/actuator/health"));
        assertFalse(properties.isWhitelisted("/api/private/info"));
    }

    @Test
    @DisplayName("未配置白名单时使用默认白名单")
    void shouldUseDefaultsWhenEmpty() {
        properties.setWhitelist(null);
        List<String> effective = properties.getEffectiveWhitelist();
        assertNotNull(effective);
        assertFalse(effective.isEmpty());
    }

    @Test
    @DisplayName("空字符串路径返回false")
    void shouldReturnFalseForEmptyPath() {
        assertFalse(properties.isWhitelisted(""));
        assertFalse(properties.isWhitelisted(null));
    }

    @Test
    @DisplayName("禁用白名单时所有路径都返回false")
    void shouldReturnFalseWhenDisabled() {
        properties.setEnabled(false);
        properties.setWhitelist(Arrays.asList("/api/auth/login"));
        assertFalse(properties.isWhitelisted("/api/auth/login"));
    }

    @Test
    @DisplayName("默认情况下登录路径在白名单中")
    void shouldMatchLoginByDefault() {
        // 不设置白名单，使用默认
        assertTrue(properties.isWhitelisted("/api/auth/login"));
        assertTrue(properties.isWhitelisted("/api/auth/register"));
    }
}
