package com.tailoris.common.exception;

import com.tailoris.common.result.Result;
import com.tailoris.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GlobalExceptionHandler 测试")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("GET", "/api/test");
    }

    @Nested
    @DisplayName("业务异常处理测试")
    class BusinessExceptionTests {

        @Test
        @DisplayName("处理业务异常")
        void testHandleBusinessException() {
            BusinessException ex = new BusinessException(400, "业务异常");
            ResponseEntity<Result<Object>> response = handler.handleBusinessException(ex, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(400, response.getBody().getCode());
            assertEquals("业务异常", response.getBody().getMessage());
        }

        @Test
        @DisplayName("处理带 ResultCode 的业务异常")
        void testHandleBusinessExceptionWithResultCode() {
            BusinessException ex = new BusinessException(ResultCode.BAD_REQUEST);
            ResponseEntity<Result<Object>> response = handler.handleBusinessException(ex, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(400, response.getBody().getCode());
        }
    }

    @Nested
    @DisplayName("参数校验异常处理测试")
    class ValidationExceptionTests {

        @Test
        @DisplayName("处理 MethodArgumentNotValidException")
        void testHandleValidationException() {
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "test");
            bindingResult.addError(new FieldError("test", "field1", "字段1不能为空"));
            bindingResult.addError(new FieldError("test", "field2", "字段2格式不正确"));

            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
            ResponseEntity<Result<Object>> response = handler.handleValidationException(ex, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(ResultCode.BAD_REQUEST.getCode(), response.getBody().getCode());
            assertTrue(response.getBody().getMessage().contains("参数校验失败"));
        }
    }

    @Nested
    @DisplayName("绑定异常处理测试")
    class BindExceptionTests {

        @Test
        @DisplayName("处理 BindException")
        void testHandleBindException() {
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "test");
            bindingResult.addError(new FieldError("test", "field1", "绑定失败"));

            BindException ex = new BindException(bindingResult);
            ResponseEntity<Result<Object>> response = handler.handleBindException(ex, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(ResultCode.BAD_REQUEST.getCode(), response.getBody().getCode());
            assertTrue(response.getBody().getMessage().contains("参数绑定失败"));
        }
    }

    @Nested
    @DisplayName("非法参数异常处理测试")
    class IllegalArgumentExceptionTests {

        @Test
        @DisplayName("处理 IllegalArgumentException")
        void testHandleIllegalArgument() {
            IllegalArgumentException ex = new IllegalArgumentException("参数不合法");
            ResponseEntity<Result<Object>> response = handler.handleIllegalArgument(ex, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(ResultCode.BAD_REQUEST.getCode(), response.getBody().getCode());
            assertEquals("参数不合法", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("空指针异常处理测试")
    class NullPointerExceptionTests {

        @Test
        @DisplayName("处理 NullPointerException")
        void testHandleNullPointer() {
            NullPointerException ex = new NullPointerException("空指针");
            ResponseEntity<Result<Object>> response = handler.handleNullPointer(ex, request);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(ResultCode.INTERNAL_ERROR.getCode(), response.getBody().getCode());
            assertEquals("系统内部错误", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("运行时异常处理测试")
    class RuntimeExceptionTests {

        @Test
        @DisplayName("处理 RuntimeException")
        void testHandleRuntimeException() {
            RuntimeException ex = new RuntimeException("运行时异常");
            ResponseEntity<Result<Object>> response = handler.handleRuntimeException(ex, request);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(ResultCode.INTERNAL_ERROR.getCode(), response.getBody().getCode());
            assertEquals("系统繁忙，请稍后再试", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("兜底异常处理测试")
    class ExceptionTests {

        @Test
        @DisplayName("处理 Exception")
        void testHandleException() {
            Exception ex = new Exception("未知异常");
            ResponseEntity<Result<Object>> response = handler.handleException(ex, request);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(ResultCode.INTERNAL_ERROR.getCode(), response.getBody().getCode());
            assertEquals("系统错误", response.getBody().getMessage());
        }
    }
}
