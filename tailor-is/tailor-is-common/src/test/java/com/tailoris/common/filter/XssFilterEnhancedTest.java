package com.tailoris.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * XssFilter 单元测试 - 增强版
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("XssFilter 单元测试")
class XssFilterEnhancedTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private XssFilter filter;

    @BeforeEach
    void setUp() {
        filter = new XssFilter();
    }

    @Test
    @DisplayName("排除路径应直接放行")
    void doFilterInternal_excludedPath_shouldPassThrough() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/file/upload");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("非JSON请求应使用普通参数过滤")
    void doFilterInternal_nonJsonRequest_shouldUseNormalFilter() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getContentType()).thenReturn("text/html");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("JSON请求应使用XssRequestWrapper")
    void doFilterInternal_jsonRequest_shouldWrapRequest() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getContentType()).thenReturn("application/json");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(XssFilter.XssRequestWrapper.class), any());
    }

    @Test
    @DisplayName("XssRequestWrapper-getParameter应转义HTML")
    void xssRequestWrapper_getParameter_shouldEscapeHtml() {
        when(request.getParameter("name")).thenReturn("<script>alert('xss')</script>");

        XssFilter.XssRequestWrapper wrapper = new XssFilter.XssRequestWrapper(request);
        String result = wrapper.getParameter("name");

        verify(request).getParameter("name");
    }

    @Test
    @DisplayName("XssRequestWrapper-getParameterValues应转义所有值")
    void xssRequestWrapper_getParameterValues_shouldEscapeAll() {
        when(request.getParameterValues("tags")).thenReturn(new String[]{"<b>bold</b>", "<i>italic</i>"});

        XssFilter.XssRequestWrapper wrapper = new XssFilter.XssRequestWrapper(request);
        String[] results = wrapper.getParameterValues("tags");

        verify(request).getParameterValues("tags");
    }

    @Test
    @DisplayName("XssRequestWrapper-getParameterValues-null应返回null")
    void xssRequestWrapper_getParameterValues_null_shouldReturnNull() {
        when(request.getParameterValues("missing")).thenReturn(null);

        XssFilter.XssRequestWrapper wrapper = new XssFilter.XssRequestWrapper(request);
        String[] results = wrapper.getParameterValues("missing");

        verify(request).getParameterValues("missing");
    }

    @Test
    @DisplayName("XssRequestWrapper-getHeader应转义HTML")
    void xssRequestWrapper_getHeader_shouldEscapeHtml() {
        when(request.getHeader("X-Custom")).thenReturn("<img src=x onerror=alert(1)>");

        XssFilter.XssRequestWrapper wrapper = new XssFilter.XssRequestWrapper(request);
        String result = wrapper.getHeader("X-Custom");

        verify(request).getHeader("X-Custom");
    }
}
