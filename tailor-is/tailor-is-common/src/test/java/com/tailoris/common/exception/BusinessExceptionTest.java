package com.tailoris.common.exception;

import com.tailoris.common.result.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BusinessException 测试")
class BusinessExceptionTest {

    @Nested
    @DisplayName("构造方法测试")
    class ConstructorTests {

        @Test
        @DisplayName("带消息构造方法")
        void testConstructorWithMessage() {
            BusinessException exception = new BusinessException("测试异常");
            assertEquals("测试异常", exception.getMessage());
            assertEquals(ResultCode.BUSINESS_ERROR.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("带代码和消息构造方法")
        void testConstructorWithCodeAndMessage() {
            BusinessException exception = new BusinessException(404, "资源不存在");
            assertEquals("资源不存在", exception.getMessage());
            assertEquals(404, exception.getCode());
        }

        @Test
        @DisplayName("使用 ResultCode 构造方法")
        void testConstructorWithResultCode() {
            BusinessException exception = new BusinessException(ResultCode.BAD_REQUEST);
            assertEquals("请求参数错误", exception.getMessage());
            assertEquals(400, exception.getCode());
        }

        @Test
        @DisplayName("使用 ResultCode 和自定义消息构造方法")
        void testConstructorWithResultCodeAndMessage() {
            BusinessException exception = new BusinessException(ResultCode.BAD_REQUEST, "自定义消息");
            assertEquals("自定义消息", exception.getMessage());
            assertEquals(400, exception.getCode());
        }
    }

    @Nested
    @DisplayName("继承关系测试")
    class InheritanceTests {

        @Test
        @DisplayName("是 RuntimeException 的子类")
        void testIsRuntimeException() {
            BusinessException exception = new BusinessException("test");
            assertTrue(exception instanceof RuntimeException);
        }

        @Test
        @DisplayName("可以抛出和捕获")
        void testThrowAndCatch() {
            assertThrows(BusinessException.class, () -> {
                throw new BusinessException("test");
            });
        }
    }
}
