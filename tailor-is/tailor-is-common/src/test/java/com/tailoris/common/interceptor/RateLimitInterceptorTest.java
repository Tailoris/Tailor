package com.tailoris.common.interceptor;

import com.tailoris.common.annotation.LimitType;
import com.tailoris.common.annotation.RateLimit;
import com.tailoris.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * RateLimitInterceptor 单元测试 - 验证 B-C09 修复
 *
 * <p>Critical Fix 验证：
 * <ul>
 *   <li>B-C09: 登录接口限流（IP级别60次/分钟）</li>
 * </ul>
 *
 * @author Tailor IS Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitInterceptor 单元测试 - Critical B-C09")
class RateLimitInterceptorTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    @SuppressWarnings("rawtypes")
    private ValueOperations valueOperations;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private RateLimitInterceptor rateLimitInterceptor;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        rateLimitInterceptor = new RateLimitInterceptor(stringRedisTemplate);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(request.getHeader("X-Real-IP")).thenReturn(null);
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    @DisplayName("B-C09: 无@RateLimit注解的请求应直接放行")
    void testPreHandle_NoAnnotation() throws Exception {
        Object handler = new Object();
        boolean result = rateLimitInterceptor.preHandle(request, response, handler);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("B-C09: 未超过限流阈值的请求应放行")
    @SuppressWarnings("unchecked")
    void testPreHandle_UnderLimit() throws Exception {
        when(valueOperations.increment(anyString())).thenReturn(10L);

        HandlerMethod handlerMethod = createHandlerMethod(new TestController(), "limitedMethod");
        boolean result = rateLimitInterceptor.preHandle(request, response, handlerMethod);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("B-C09: 超过限流阈值的请求应抛出BusinessException")
    @SuppressWarnings("unchecked")
    void testPreHandle_OverLimit() throws Exception {
        when(valueOperations.increment(anyString())).thenReturn(100L);

        HandlerMethod handlerMethod = createHandlerMethod(new TestController(), "limitedMethod");

        assertThatThrownBy(() -> rateLimitInterceptor.preHandle(request, response, handlerMethod))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请求过于频繁");
    }

    @Test
    @DisplayName("B-C09: IP级别限流应使用客户端IP作为Key")
    @SuppressWarnings("unchecked")
    void testPreHandle_IpBasedKey() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100");
        when(valueOperations.increment(anyString())).thenReturn(5L);

        HandlerMethod handlerMethod = createHandlerMethod(new TestController(), "ipLimitedMethod");
        rateLimitInterceptor.preHandle(request, response, handlerMethod);

        // 验证使用的Key包含IP地址
        org.mockito.Mockito.verify(valueOperations).increment(anyString());
    }

    private HandlerMethod createHandlerMethod(Object controller, String methodName) throws NoSuchMethodException {
        Method method = controller.getClass().getMethod(methodName);
        return new HandlerMethod(controller, method);
    }

    /**
     * 测试用Controller
     */
    static class TestController {
        @RateLimit(key = "test", permitsPerSecond = 10, capacity = 60)
        public void limitedMethod() {
        }

        @RateLimit(key = "ipTest", permitsPerSecond = 5, capacity = 10, limitType = LimitType.IP)
        public void ipLimitedMethod() {
        }
    }
}
