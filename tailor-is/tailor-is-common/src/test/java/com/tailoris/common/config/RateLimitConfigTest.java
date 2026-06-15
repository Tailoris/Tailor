package com.tailoris.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RateLimitConfig 测试")
class RateLimitConfigTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("默认启用")
        void testDefaultEnabled() {
            RateLimitConfig config = new RateLimitConfig();
            assertTrue(config.isEnabled());
        }

        @Test
        @DisplayName("默认 permitsPerSecond")
        void testDefaultPermitsPerSecond() {
            RateLimitConfig config = new RateLimitConfig();
            assertEquals(10, config.getDefaultPermitsPerSecond());
        }

        @Test
        @DisplayName("默认 redisExpireSeconds")
        void testDefaultRedisExpireSeconds() {
            RateLimitConfig config = new RateLimitConfig();
            assertEquals(60, config.getRedisExpireSeconds());
        }

        @Test
        @DisplayName("默认 message")
        void testDefaultMessage() {
            RateLimitConfig config = new RateLimitConfig();
            assertEquals("请求过于频繁，请稍后再试", config.getMessage());
        }
    }

    @Nested
    @DisplayName("Global 配置测试")
    class GlobalTests {

        @Test
        @DisplayName("Global 默认值")
        void testGlobalDefault() {
            RateLimitConfig config = new RateLimitConfig();
            assertNotNull(config.getGlobal());
            assertEquals(1000, config.getGlobal().getPermitsPerSecond());
        }

        @Test
        @DisplayName("设置 Global")
        void testSetGlobal() {
            RateLimitConfig config = new RateLimitConfig();
            RateLimitConfig.Global global = new RateLimitConfig.Global();
            global.setPermitsPerSecond(2000);
            config.setGlobal(global);
            assertEquals(2000, config.getGlobal().getPermitsPerSecond());
        }
    }

    @Nested
    @DisplayName("IpLevel 配置测试")
    class IpLevelTests {

        @Test
        @DisplayName("IpLevel 默认值")
        void testIpLevelDefault() {
            RateLimitConfig config = new RateLimitConfig();
            assertNotNull(config.getIpLevel());
            assertEquals(20, config.getIpLevel().getPermitsPerSecond());
            assertEquals(60, config.getIpLevel().getCapacitySeconds());
        }

        @Test
        @DisplayName("设置 IpLevel")
        void testSetIpLevel() {
            RateLimitConfig config = new RateLimitConfig();
            RateLimitConfig.IpLevel ipLevel = new RateLimitConfig.IpLevel();
            ipLevel.setPermitsPerSecond(50);
            ipLevel.setCapacitySeconds(120);
            config.setIpLevel(ipLevel);
            assertEquals(50, config.getIpLevel().getPermitsPerSecond());
            assertEquals(120, config.getIpLevel().getCapacitySeconds());
        }
    }

    @Nested
    @DisplayName("UserLevel 配置测试")
    class UserLevelTests {

        @Test
        @DisplayName("UserLevel 默认值")
        void testUserLevelDefault() {
            RateLimitConfig config = new RateLimitConfig();
            assertNotNull(config.getUserLevel());
            assertEquals(50, config.getUserLevel().getPermitsPerSecond());
            assertEquals(60, config.getUserLevel().getCapacitySeconds());
        }

        @Test
        @DisplayName("设置 UserLevel")
        void testSetUserLevel() {
            RateLimitConfig config = new RateLimitConfig();
            RateLimitConfig.UserLevel userLevel = new RateLimitConfig.UserLevel();
            userLevel.setPermitsPerSecond(100);
            userLevel.setCapacitySeconds(180);
            config.setUserLevel(userLevel);
            assertEquals(100, config.getUserLevel().getPermitsPerSecond());
            assertEquals(180, config.getUserLevel().getCapacitySeconds());
        }
    }

    @Nested
    @DisplayName("EndpointLevel 配置测试")
    class EndpointLevelTests {

        @Test
        @DisplayName("EndpointLevel 默认值")
        void testEndpointLevelDefault() {
            RateLimitConfig config = new RateLimitConfig();
            assertNotNull(config.getEndpointLevel());
            assertEquals(30, config.getEndpointLevel().getPermitsPerSecond());
            assertEquals(60, config.getEndpointLevel().getCapacitySeconds());
        }

        @Test
        @DisplayName("设置 EndpointLevel")
        void testSetEndpointLevel() {
            RateLimitConfig config = new RateLimitConfig();
            RateLimitConfig.EndpointLevel endpointLevel = new RateLimitConfig.EndpointLevel();
            endpointLevel.setPermitsPerSecond(60);
            endpointLevel.setCapacitySeconds(240);
            config.setEndpointLevel(endpointLevel);
            assertEquals(60, config.getEndpointLevel().getPermitsPerSecond());
            assertEquals(240, config.getEndpointLevel().getCapacitySeconds());
        }
    }

    @Nested
    @DisplayName("Setter 测试")
    class SetterTests {

        @Test
        @DisplayName("设置 enabled")
        void testSetEnabled() {
            RateLimitConfig config = new RateLimitConfig();
            config.setEnabled(false);
            assertFalse(config.isEnabled());
        }

        @Test
        @DisplayName("设置 defaultPermitsPerSecond")
        void testSetDefaultPermitsPerSecond() {
            RateLimitConfig config = new RateLimitConfig();
            config.setDefaultPermitsPerSecond(20);
            assertEquals(20, config.getDefaultPermitsPerSecond());
        }

        @Test
        @DisplayName("设置 redisExpireSeconds")
        void testSetRedisExpireSeconds() {
            RateLimitConfig config = new RateLimitConfig();
            config.setRedisExpireSeconds(120);
            assertEquals(120, config.getRedisExpireSeconds());
        }

        @Test
        @DisplayName("设置 message")
        void testSetMessage() {
            RateLimitConfig config = new RateLimitConfig();
            config.setMessage("自定义消息");
            assertEquals("自定义消息", config.getMessage());
        }
    }
}
