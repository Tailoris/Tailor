package com.tailoris.common.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RedisKeyPrefix 测试")
class RedisKeyPrefixTest {

    @Nested
    @DisplayName("常量前缀测试")
    class ConstantTests {

        @Test
        @DisplayName("PROJECT 前缀")
        void testProject() {
            assertEquals("tailor:", RedisKeyPrefix.PROJECT);
        }

        @Test
        @DisplayName("USER 前缀")
        void testUser() {
            assertEquals("tailor:user:", RedisKeyPrefix.USER);
        }

        @Test
        @DisplayName("PRODUCT 前缀")
        void testProduct() {
            assertEquals("tailor:product:", RedisKeyPrefix.PRODUCT);
        }

        @Test
        @DisplayName("ORDER 前缀")
        void testOrder() {
            assertEquals("tailor:order:", RedisKeyPrefix.ORDER);
        }

        @Test
        @DisplayName("PAYMENT 前缀")
        void testPayment() {
            assertEquals("tailor:payment:", RedisKeyPrefix.PAYMENT);
        }

        @Test
        @DisplayName("MERCHANT 前缀")
        void testMerchant() {
            assertEquals("tailor:merchant:", RedisKeyPrefix.MERCHANT);
        }

        @Test
        @DisplayName("TOKEN 前缀")
        void testToken() {
            assertEquals("tailor:token:", RedisKeyPrefix.TOKEN);
        }

        @Test
        @DisplayName("CACHE 前缀")
        void testCache() {
            assertEquals("tailor:cache:", RedisKeyPrefix.CACHE);
        }

        @Test
        @DisplayName("LOCK 前缀")
        void testLock() {
            assertEquals("tailor:lock:", RedisKeyPrefix.LOCK);
        }

        @Test
        @DisplayName("BLOOM 前缀")
        void testBloom() {
            assertEquals("tailor:bloom:", RedisKeyPrefix.BLOOM);
        }
    }

    @Nested
    @DisplayName("动态键生成测试")
    class DynamicKeyTests {

        @Test
        @DisplayName("生成用户键")
        void testUserKey() {
            assertEquals("tailor:user:123", RedisKeyPrefix.user(123L));
        }

        @Test
        @DisplayName("生成商品键")
        void testProductKey() {
            assertEquals("tailor:product:456", RedisKeyPrefix.product(456L));
        }

        @Test
        @DisplayName("生成订单键")
        void testOrderKey() {
            assertEquals("tailor:order:ORD123", RedisKeyPrefix.order("ORD123"));
        }

        @Test
        @DisplayName("生成 Token 键")
        void testTokenKey() {
            assertEquals("tailor:token:user123", RedisKeyPrefix.token("user123"));
        }

        @Test
        @DisplayName("生成缓存键")
        void testCacheKey() {
            assertEquals("tailor:cache:module:key", RedisKeyPrefix.cache("module", "key"));
        }

        @Test
        @DisplayName("生成锁键")
        void testLockKey() {
            assertEquals("tailor:lock:resource", RedisKeyPrefix.lock("resource"));
        }

        @Test
        @DisplayName("生成布隆过滤器键")
        void testBloomKey() {
            assertEquals("tailor:bloom:filter", RedisKeyPrefix.bloom("filter"));
        }
    }
}
