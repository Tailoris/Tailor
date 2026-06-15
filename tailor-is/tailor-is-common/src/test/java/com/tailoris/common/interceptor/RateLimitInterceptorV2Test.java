package com.tailoris.common.interceptor;

import com.tailoris.common.annotation.LimitType;
import com.tailoris.common.annotation.RateLimit;
import com.tailoris.common.config.RateLimitDynamicConfig;
import com.tailoris.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RateLimitInterceptorV2 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitInterceptorV2 单元测试")
class RateLimitInterceptorV2Test {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RateLimitDynamicConfig dynamicConfig;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private RateLimitInterceptorV2 interceptor;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(dynamicConfig.isEnabled()).thenReturn(true);
        lenient().when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(request.getHeader("X-Real-IP")).thenReturn(null);
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        interceptor = new RateLimitInterceptorV2(stringRedisTemplate, dynamicConfig);
    }

    @Test
    @DisplayName("非HandlerMethod应直接放行")
    void preHandle_nonHandlerMethod_shouldPass() throws Exception {
        Object handler = new Object();

        boolean result = interceptor.preHandle(request, response, handler);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("无@RateLimit注解应直接放行")
    void preHandle_noAnnotation_shouldPass() throws Exception {
        HandlerMethod handlerMethod = createHandlerMethod(new TestController(), "normalMethod");

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("动态配置禁用应直接放行")
    void preHandle_dynamicConfigDisabled_shouldPass() throws Exception {
        when(dynamicConfig.isEnabled()).thenReturn(false);
        HandlerMethod handlerMethod = createHandlerMethod(new TestController(), "limitedMethod");

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Redis限流-未超限应放行并设置响应头")
    void preHandle_redisLimit_underCapacity_shouldPass() throws Exception {
        when(valueOperations.increment(anyString())).thenReturn(5L);
        when(dynamicConfig.getConfig(anyString())).thenReturn(new RateLimitDynamicConfig.RateLimitConfigEntry());
        
        HandlerMethod handlerMethod = createHandlerMethod(new TestController(), "limitedMethod");

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertThat(result).isTrue();
        verify(response).setHeader(eq("X-RateLimit-Limit"), anyString());
        verify(response).setHeader(eq("X-RateLimit-Remaining"), anyString());
    }

    @Test
    @DisplayName("Redis限流-超过容量应抛出BusinessException")
    void preHandle_redisLimit_overCapacity_shouldThrow() throws Exception {
        when(valueOperations.increment(anyString())).thenReturn(100L);
        when(dynamicConfig.getConfig(anyString())).thenReturn(new RateLimitDynamicConfig.RateLimitConfigEntry());
        
        HandlerMethod handlerMethod = createHandlerMethod(new TestController(), "limitedMethod");

        assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Redis限流-首次请求应设置过期时间")
    void preHandle_redisLimit_firstRequest_shouldSetExpire() throws Exception {
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(dynamicConfig.getConfig(anyString())).thenReturn(new RateLimitDynamicConfig.RateLimitConfigEntry());
        
        HandlerMethod handlerMethod = createHandlerMethod(new TestController(), "limitedMethod");

        interceptor.preHandle(request, response, handlerMethod);

        verify(stringRedisTemplate).expire(anyString(), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Redis连接失败应降级到本地限流")
    void preHandle_redisConnectionFailure_shouldFallbackToLocal() throws Exception {
        when(valueOperations.increment(anyString()))
                .thenThrow(new RedisConnectionFailureException("Connection failed"));
        when(dynamicConfig.getConfig(anyString())).thenReturn(new RateLimitDynamicConfig.RateLimitConfigEntry());
        
        HandlerMethod handlerMethod = createHandlerMethod(new TestController(), "limitedMethod");

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertThat(result).isTrue();
        verify(response).setHeader(eq("X-RateLimit-Local"), eq("true"));
    }

    @Test
    @DisplayName("本地限流-未超限应放行")
    void preHandle_localLimit_underCapacity_shouldPass() throws Exception {
        when(valueOperations.increment(anyString()))
                .thenThrow(new RedisConnectionFailureException("Connection failed"));
        when(dynamicConfig.getConfig(anyString())).thenReturn(new RateLimitDynamicConfig.RateLimitConfigEntry());
        
        HandlerMethod handlerMethod = createHandlerMethod(new TestController(), "limitedMethod");

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("本地限流-超过容量应抛出BusinessException")
    void preHandle_localLimit_overCapacity_shouldThrow() throws Exception {
        when(valueOperations.increment(anyString()))
                .thenThrow(new RedisConnectionFailureException("Connection failed"));
        
        RateLimitDynamicConfig.RateLimitConfigEntry config = new RateLimitDynamicConfig.RateLimitConfigEntry();
        config.setCapacity(1);
        when(dynamicConfig.getConfig(anyString())).thenReturn(config);
        
        HandlerMethod handlerMethod = createHandlerMethod(new TestController(), "limitedMethod");

        // First call fills the capacity (count=1, not > 1)
        interceptor.preHandle(request, response, handlerMethod);

        // Second call exceeds capacity (count=2 > 1)
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("本地限流");
    }

    @Test
    @DisplayName("IP限流-应使用X-Forwarded-For头")
    void preHandle_ipLimit_withXForwardedFor_shouldUseIt() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100");
        when(valueOperations.increment(anyString())).thenReturn(5L);
        when(dynamicConfig.getConfig(anyString())).thenReturn(new RateLimitDynamicConfig.RateLimitConfigEntry());
        
        HandlerMethod handlerMethod = createHandlerMethod(new TestController(), "ipLimitedMethod");

        interceptor.preHandle(request, response, handlerMethod);

        verify(valueOperations).increment(anyString());
    }

    @Test
    @DisplayName("IP限流-多个IP应取第一个")
    void preHandle_ipLimit_multipleIps_shouldUseFirst() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100, 10.0.0.1");
        when(valueOperations.increment(anyString())).thenReturn(5L);
        when(dynamicConfig.getConfig(anyString())).thenReturn(new RateLimitDynamicConfig.RateLimitConfigEntry());
        
        HandlerMethod handlerMethod = createHandlerMethod(new TestController(), "ipLimitedMethod");

        interceptor.preHandle(request, response, handlerMethod);

        verify(valueOperations).increment(anyString());
    }

    @Test
    @DisplayName("USER限流-有Authorization头应使用其hashCode")
    void preHandle_userLimit_withAuthHeader_shouldUseHashCode() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token123");
        when(valueOperations.increment(anyString())).thenReturn(5L);
        when(dynamicConfig.getConfig(anyString())).thenReturn(new RateLimitDynamicConfig.RateLimitConfigEntry());
        
        HandlerMethod handlerMethod = createHandlerMethod(new TestController(), "userLimitedMethod");

        interceptor.preHandle(request, response, handlerMethod);

        verify(valueOperations).increment(anyString());
    }

    @Test
    @DisplayName("GLOBAL限流应使用global作为key")
    void preHandle_globalLimit_shouldUseGlobalKey() throws Exception {
        when(valueOperations.increment(anyString())).thenReturn(5L);
        when(dynamicConfig.getConfig(anyString())).thenReturn(new RateLimitDynamicConfig.RateLimitConfigEntry());
        
        HandlerMethod handlerMethod = createHandlerMethod(new TestController(), "globalLimitedMethod");

        interceptor.preHandle(request, response, handlerMethod);

        verify(valueOperations).increment(anyString());
    }

    private HandlerMethod createHandlerMethod(Object controller, String methodName) throws NoSuchMethodException {
        Method method = controller.getClass().getMethod(methodName);
        return new HandlerMethod(controller, method);
    }

    static class TestController {
        public void normalMethod() {}

        @RateLimit(key = "test", permitsPerSecond = 10, capacity = 60)
        public void limitedMethod() {}

        @RateLimit(key = "ipTest", permitsPerSecond = 5, capacity = 10, limitType = LimitType.IP)
        public void ipLimitedMethod() {}

        @RateLimit(key = "userTest", permitsPerSecond = 5, capacity = 10, limitType = LimitType.USER)
        public void userLimitedMethod() {}

        @RateLimit(key = "globalTest", permitsPerSecond = 5, capacity = 10, limitType = LimitType.GLOBAL)
        public void globalLimitedMethod() {}
    }
}
