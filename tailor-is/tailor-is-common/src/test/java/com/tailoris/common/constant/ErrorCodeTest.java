package com.tailoris.common.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ErrorCode 测试")
class ErrorCodeTest {

    @Nested
    @DisplayName("枚举值测试")
    class EnumValueTests {

        @Test
        @DisplayName("SUCCESS 枚举值")
        void testSuccess() {
            assertEquals(0, ErrorCode.SUCCESS.getCode());
            assertEquals("操作成功", ErrorCode.SUCCESS.getDefaultMessage());
        }

        @Test
        @DisplayName("SYSTEM_ERROR 枚举值")
        void testSystemError() {
            assertEquals(1000, ErrorCode.SYSTEM_ERROR.getCode());
            assertEquals("系统内部错误", ErrorCode.SYSTEM_ERROR.getDefaultMessage());
        }

        @Test
        @DisplayName("PARAM_INVALID 枚举值")
        void testParamInvalid() {
            assertEquals(1001, ErrorCode.PARAM_INVALID.getCode());
            assertEquals("参数校验失败", ErrorCode.PARAM_INVALID.getDefaultMessage());
        }

        @Test
        @DisplayName("UNAUTHORIZED 枚举值")
        void testUnauthorized() {
            assertEquals(2000, ErrorCode.UNAUTHORIZED.getCode());
            assertEquals("未登录或Token已过期", ErrorCode.UNAUTHORIZED.getDefaultMessage());
        }

        @Test
        @DisplayName("FORBIDDEN 枚举值")
        void testForbidden() {
            assertEquals(2001, ErrorCode.FORBIDDEN.getCode());
            assertEquals("权限不足", ErrorCode.FORBIDDEN.getDefaultMessage());
        }

        @Test
        @DisplayName("ORDER_NOT_FOUND 枚举值")
        void testOrderNotFound() {
            assertEquals(3001, ErrorCode.ORDER_NOT_FOUND.getCode());
            assertEquals("订单不存在", ErrorCode.ORDER_NOT_FOUND.getDefaultMessage());
        }

        @Test
        @DisplayName("PRODUCT_NOT_FOUND 枚举值")
        void testProductNotFound() {
            assertEquals(3002, ErrorCode.PRODUCT_NOT_FOUND.getCode());
            assertEquals("商品不存在", ErrorCode.PRODUCT_NOT_FOUND.getDefaultMessage());
        }

        @Test
        @DisplayName("STOCK_INSUFFICIENT 枚举值")
        void testStockInsufficient() {
            assertEquals(3003, ErrorCode.STOCK_INSUFFICIENT.getCode());
            assertEquals("库存不足", ErrorCode.STOCK_INSUFFICIENT.getDefaultMessage());
        }
    }

    @Nested
    @DisplayName("枚举遍历测试")
    class EnumIterationTests {

        @Test
        @DisplayName("枚举值数量")
        void testEnumCount() {
            ErrorCode[] values = ErrorCode.values();
            assertTrue(values.length > 0);
        }

        @Test
        @DisplayName("valueOf 方法")
        void testValueOf() {
            assertEquals(ErrorCode.SUCCESS, ErrorCode.valueOf("SUCCESS"));
            assertEquals(ErrorCode.SYSTEM_ERROR, ErrorCode.valueOf("SYSTEM_ERROR"));
        }
    }

    @Nested
    @DisplayName("代码范围测试")
    class CodeRangeTests {

        @Test
        @DisplayName("成功代码为 0")
        void testSuccessCode() {
            assertEquals(0, ErrorCode.SUCCESS.getCode());
        }

        @Test
        @DisplayName("系统错误代码在 1000-1999 范围")
        void testSystemErrorRange() {
            assertTrue(ErrorCode.SYSTEM_ERROR.getCode() >= 1000);
            assertTrue(ErrorCode.SYSTEM_ERROR.getCode() < 2000);
            assertTrue(ErrorCode.PARAM_INVALID.getCode() >= 1000);
            assertTrue(ErrorCode.PARAM_INVALID.getCode() < 2000);
        }

        @Test
        @DisplayName("认证错误代码在 2000-2999 范围")
        void testAuthErrorRange() {
            assertTrue(ErrorCode.UNAUTHORIZED.getCode() >= 2000);
            assertTrue(ErrorCode.UNAUTHORIZED.getCode() < 3000);
            assertTrue(ErrorCode.FORBIDDEN.getCode() >= 2000);
            assertTrue(ErrorCode.FORBIDDEN.getCode() < 3000);
        }

        @Test
        @DisplayName("业务错误代码在 3000-3999 范围")
        void testBusinessErrorRange() {
            assertTrue(ErrorCode.ORDER_NOT_FOUND.getCode() >= 3000);
            assertTrue(ErrorCode.ORDER_NOT_FOUND.getCode() < 4000);
            assertTrue(ErrorCode.PRODUCT_NOT_FOUND.getCode() >= 3000);
            assertTrue(ErrorCode.PRODUCT_NOT_FOUND.getCode() < 4000);
        }
    }
}
