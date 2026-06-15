package com.tailoris.common.filter;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * TraceIdFilter 单元测试 - L-21 修复验证.
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@DisplayName("TraceIdFilter 链路追踪过滤器测试 (L-21)")
class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void cleanup() {
        MDC.clear();
    }

    @Test
    @DisplayName("请求头已带 X-Trace-Id 时应透传")
    void doFilter_PassThroughExistingTraceId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(TraceIdFilter.HEADER_TRACE_ID, "test-incoming-trace-123");

        AtomicReference<String> capturedTraceId = new AtomicReference<>();
        FilterChain chain = (req, res) -> capturedTraceId.set(MDC.get(TraceIdFilter.MDC_TRACE_ID));

        filter.doFilter(request, response, chain);

        assertEquals("test-incoming-trace-123", capturedTraceId.get());
        assertEquals("test-incoming-trace-123", response.getHeader(TraceIdFilter.HEADER_TRACE_ID));
        assertEquals("test-incoming-trace-123", request.getAttribute(TraceIdFilter.ATTR_TRACE_ID));
    }

    @Test
    @DisplayName("请求头无 traceId 时应自动生成")
    void doFilter_GenerateNewTraceId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> capturedTraceId = new AtomicReference<>();
        FilterChain chain = (req, res) -> capturedTraceId.set(MDC.get(TraceIdFilter.MDC_TRACE_ID));

        filter.doFilter(request, response, chain);

        assertNotNull(capturedTraceId.get());
        // 默认生成的 traceId 是32位UUID
        assertEquals(32, capturedTraceId.get().length());
        assertEquals(capturedTraceId.get(), response.getHeader(TraceIdFilter.HEADER_TRACE_ID));
    }

    @Test
    @DisplayName("超长 traceId (>64) 应被拒绝重新生成")
    void doFilter_RejectOversizedTraceId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String oversized = "a".repeat(100);
        request.addHeader(TraceIdFilter.HEADER_TRACE_ID, oversized);

        AtomicReference<String> capturedTraceId = new AtomicReference<>();
        FilterChain chain = (req, res) -> capturedTraceId.set(MDC.get(TraceIdFilter.MDC_TRACE_ID));

        filter.doFilter(request, response, chain);

        // 超长被拒绝，应生成新的32位ID
        assertNotNull(capturedTraceId.get());
        assertEquals(32, capturedTraceId.get().length());
    }

    @Test
    @DisplayName("含非法字符的 traceId 应被拒绝重新生成")
    void doFilter_RejectInvalidChars() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        // 包含特殊字符
        request.addHeader(TraceIdFilter.HEADER_TRACE_ID, "abc; DROP TABLE users;");

        AtomicReference<String> capturedTraceId = new AtomicReference<>();
        FilterChain chain = (req, res) -> capturedTraceId.set(MDC.get(TraceIdFilter.MDC_TRACE_ID));

        filter.doFilter(request, response, chain);

        assertNotNull(capturedTraceId.get());
        // 非法字符被拒绝，应生成新的
        assertEquals(32, capturedTraceId.get().length());
    }

    @Test
    @DisplayName("MDC 中的 traceId 在请求结束后应被清理")
    void doFilter_CleanupMdcAfterRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(TraceIdFilter.HEADER_TRACE_ID, "cleanup-test");

        FilterChain chain = (req, res) -> {
            // 验证 MDC 中确实有
            assertEquals("cleanup-test", MDC.get(TraceIdFilter.MDC_TRACE_ID));
        };

        filter.doFilter(request, response, chain);

        // 请求结束后 MDC 应被清理
        assertNull(MDC.get(TraceIdFilter.MDC_TRACE_ID));
    }

    @Test
    @DisplayName("请求属性应包含 traceId")
    void doFilter_SetsRequestAttribute() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(TraceIdFilter.HEADER_TRACE_ID, "attr-test");

        FilterChain chain = (req, res) -> {
            // 在过滤器链中 request.getAttribute 应可访问
        };

        filter.doFilter(request, response, chain);

        assertEquals("attr-test", request.getAttribute(TraceIdFilter.ATTR_TRACE_ID));
    }

    @Test
    @DisplayName("合法字符的 traceId 应通过")
    void doFilter_AcceptValidChars() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String validId = "abcXYZ-123_456-test";
        request.addHeader(TraceIdFilter.HEADER_TRACE_ID, validId);

        AtomicReference<String> captured = new AtomicReference<>();
        FilterChain chain = (req, res) -> captured.set(MDC.get(TraceIdFilter.MDC_TRACE_ID));

        filter.doFilter(request, response, chain);

        assertEquals(validId, captured.get());
    }
}
