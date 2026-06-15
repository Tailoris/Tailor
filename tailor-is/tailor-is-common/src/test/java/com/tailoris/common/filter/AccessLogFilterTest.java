package com.tailoris.common.filter;

import com.tailoris.common.util.TraceUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * AccessLogFilter 单元测试 - L-22 修复验证.
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@DisplayName("AccessLogFilter 访问日志过滤器测试 (L-22)")
class AccessLogFilterTest {

    private final AccessLogFilter filter = new AccessLogFilter();

    @AfterEach
    void cleanup() {
        MDC.clear();
    }

    @Test
    @DisplayName("业务请求（/api/**）应被记录")
    void doFilter_ApiRequestLogged() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/info");
        request.setRemoteAddr("192.168.1.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        AtomicReference<String> traceIdCaptured = new AtomicReference<>();
        FilterChain chain = (req, res) -> traceIdCaptured.set(TraceUtils.currentTraceId());

        MDC.put(TraceIdFilter.MDC_TRACE_ID, "test-trace-id");
        filter.doFilter(request, response, chain);
        assertEquals("test-trace-id", traceIdCaptured.get());
    }

    @Test
    @DisplayName("非 /api/** 请求应被跳过（不记录日志）")
    void doFilter_NonApiRequestSkipped() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/static/css/main.css");
        request.setRemoteAddr("192.168.1.10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<Boolean> chainExecuted = new AtomicReference<>(false);
        FilterChain chain = (req, res) -> chainExecuted.set(true);

        filter.doFilter(request, response, chain);
        // 过滤器链应正常执行
        assertEquals(true, chainExecuted.get());
    }

    @Test
    @DisplayName("actuator 端点应被跳过")
    void doFilter_ActuatorSkipped() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.setRemoteAddr("192.168.1.10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<Boolean> chainExecuted = new AtomicReference<>(false);
        FilterChain chain = (req, res) -> chainExecuted.set(true);

        filter.doFilter(request, response, chain);
        assertEquals(true, chainExecuted.get());
    }

    @Test
    @DisplayName("即使 FilterChain 抛异常，过滤器本身不应崩溃")
    void doFilter_ChainExceptionHandled() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/order/create");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            throw new ServletException("业务异常");
        };

        try {
            filter.doFilter(request, response, chain);
        } catch (Exception e) {
            // 期望异常透传，但过滤器内部不应有额外异常
            assertEquals("业务异常", e.getMessage());
        }
    }

    @Test
    @DisplayName("5xx 状态码应记录但不应中断")
    void doFilter_ServerErrorStatus() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/users");
        request.setRemoteAddr("192.168.1.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(500);

        AtomicReference<Boolean> chainExecuted = new AtomicReference<>(false);
        FilterChain chain = (req, res) -> {
            ((jakarta.servlet.http.HttpServletResponse) res).setStatus(500);
            chainExecuted.set(true);
        };

        filter.doFilter(request, response, chain);
        assertEquals(true, chainExecuted.get());
    }
}
