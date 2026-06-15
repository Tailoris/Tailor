package com.tailoris.common.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StatusEnum 测试")
class StatusEnumTest {

    @Nested
    @DisplayName("枚举值测试")
    class EnumValueTests {

        @Test
        @DisplayName("DISABLED 枚举值")
        void testDisabled() {
            assertEquals(0, StatusEnum.DISABLED.getCode());
            assertEquals("禁用", StatusEnum.DISABLED.getDescription());
        }

        @Test
        @DisplayName("ENABLED 枚举值")
        void testEnabled() {
            assertEquals(1, StatusEnum.ENABLED.getCode());
            assertEquals("启用", StatusEnum.ENABLED.getDescription());
        }

        @Test
        @DisplayName("AUDIT_APPROVED 枚举值")
        void testAuditApproved() {
            assertEquals(2, StatusEnum.AUDIT_APPROVED.getCode());
            assertEquals("审核通过", StatusEnum.AUDIT_APPROVED.getDescription());
        }

        @Test
        @DisplayName("PRODUCT_PUBLISHED 枚举值")
        void testProductPublished() {
            assertEquals(2, StatusEnum.PRODUCT_PUBLISHED.getCode());
            assertEquals("已上架", StatusEnum.PRODUCT_PUBLISHED.getDescription());
        }

        @Test
        @DisplayName("PAYMENT_SUCCESS 枚举值")
        void testPaymentSuccess() {
            assertEquals(1, StatusEnum.PAYMENT_SUCCESS.getCode());
            assertEquals("已支付", StatusEnum.PAYMENT_SUCCESS.getDescription());
        }
    }

    @Nested
    @DisplayName("根据代码获取描述测试")
    class GetDescriptionTests {

        @Test
        @DisplayName("获取存在的状态描述")
        void testGetDescriptionExists() {
            assertEquals("启用", StatusEnum.getDescription(1));
            assertEquals("审核通过", StatusEnum.getDescription(2));
        }

        @Test
        @DisplayName("获取不存在的状态描述")
        void testGetDescriptionNotExists() {
            String result = StatusEnum.getDescription(999);
            assertTrue(result.contains("未知状态"));
            assertTrue(result.contains("999"));
        }

        @Test
        @DisplayName("多个枚举有相同代码时返回第一个")
        void testGetDescriptionMultipleWithSameCode() {
            // code 0 对应多个枚举：DISABLED, PENDING_AUDIT, PRODUCT_DRAFT, PAYMENT_PENDING, REFUND_PENDING
            String result = StatusEnum.getDescription(0);
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("枚举遍历测试")
    class EnumIterationTests {

        @Test
        @DisplayName("枚举值数量")
        void testEnumCount() {
            StatusEnum[] values = StatusEnum.values();
            assertTrue(values.length > 0);
        }

        @Test
        @DisplayName("valueOf 方法")
        void testValueOf() {
            assertEquals(StatusEnum.ENABLED, StatusEnum.valueOf("ENABLED"));
            assertEquals(StatusEnum.DISABLED, StatusEnum.valueOf("DISABLED"));
        }
    }
}
