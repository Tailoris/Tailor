package com.tailoris.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * HttpRequestUtils 单元测试 - B-L03 修复验证.
 *
 * <p>覆盖：</p>
 * <ul>
 *   <li>正常场景：单IP、多级代理IP、未代理直接访问</li>
 *   <li>边界条件：null 请求、unknown 占位、空Header</li>
 *   <li>异常情况：所有Header均为unknown</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
@DisplayName("HttpRequestUtils 工具类测试 (B-L03)")
class HttpRequestUtilsTest {

    @Test
    @DisplayName("null 请求应返回 unknown")
    void getClientIp_NullRequest() {
        assertEquals(HttpRequestUtils.UNKNOWN, HttpRequestUtils.getClientIp(null));
    }

    @Test
    @DisplayName("X-Forwarded-For 头优先级最高")
    void getClientIp_XForwardedForHighest() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5");
        when(req.getHeader("X-Real-IP")).thenReturn("10.0.0.1");
        when(req.getHeader("Proxy-Client-IP")).thenReturn("192.168.1.1");
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        assertEquals("203.0.113.5", HttpRequestUtils.getClientIp(req));
    }

    @Test
    @DisplayName("多级代理应取第一个IP")
    void getClientIp_MultipleProxies() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 198.51.100.1, 10.0.0.1");
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        assertEquals("203.0.113.5", HttpRequestUtils.getClientIp(req));
    }

    @Test
    @DisplayName("X-Forwarded-For 为 unknown 时应回退到 X-Real-IP")
    void getClientIp_FallbackToXRealIp() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(req.getHeader("X-Real-IP")).thenReturn("203.0.113.10");
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        assertEquals("203.0.113.10", HttpRequestUtils.getClientIp(req));
    }

    @Test
    @DisplayName("X-Forwarded-For 为空字符串时回退")
    void getClientIp_EmptyHeaderFallback() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("");
        when(req.getHeader("X-Real-IP")).thenReturn("203.0.113.20");
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        assertEquals("203.0.113.20", HttpRequestUtils.getClientIp(req));
    }

    @Test
    @DisplayName("所有代理Header均为unknown时回退到getRemoteAddr")
    void getClientIp_FallbackToRemoteAddr() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(req.getHeader("X-Real-IP")).thenReturn("unknown");
        when(req.getHeader("Proxy-Client-IP")).thenReturn("unknown");
        when(req.getHeader("WL-Proxy-Client-IP")).thenReturn("unknown");
        when(req.getHeader("HTTP_CLIENT_IP")).thenReturn("unknown");
        when(req.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn("unknown");
        when(req.getRemoteAddr()).thenReturn("192.168.1.100");

        assertEquals("192.168.1.100", HttpRequestUtils.getClientIp(req));
    }

    @Test
    @DisplayName("无任何代理Header时直接使用getRemoteAddr")
    void getClientIp_DirectConnection() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRemoteAddr()).thenReturn("192.168.1.50");

        assertEquals("192.168.1.50", HttpRequestUtils.getClientIp(req));
    }

    @Test
    @DisplayName("Apache 代理 Proxy-Client-IP 生效")
    void getClientIp_ProxyClientIp() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getHeader("X-Real-IP")).thenReturn(null);
        when(req.getHeader("Proxy-Client-IP")).thenReturn("203.0.113.30");
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        assertEquals("203.0.113.30", HttpRequestUtils.getClientIp(req));
    }

    @Test
    @DisplayName("WebLogic 代理 WL-Proxy-Client-IP 生效")
    void getClientIp_WlProxyClientIp() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getHeader("X-Real-IP")).thenReturn(null);
        when(req.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(req.getHeader("WL-Proxy-Client-IP")).thenReturn("203.0.113.40");
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        assertEquals("203.0.113.40", HttpRequestUtils.getClientIp(req));
    }

    @Test
    @DisplayName("HTTP_CLIENT_IP 与 HTTP_X_FORWARDED_FOR 兜底")
    void getClientIp_HttpClientIpFallback() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getHeader("X-Real-IP")).thenReturn(null);
        when(req.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(req.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        when(req.getHeader("HTTP_CLIENT_IP")).thenReturn("203.0.113.50");
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        assertEquals("203.0.113.50", HttpRequestUtils.getClientIp(req));
    }

    @Test
    @DisplayName("getUserAgent null 请求")
    void getUserAgent_NullRequest() {
        assertEquals(HttpRequestUtils.UNKNOWN, HttpRequestUtils.getUserAgent(null));
    }

    @Test
    @DisplayName("getUserAgent 正常")
    void getUserAgent_Normal() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        assertEquals("Mozilla/5.0", HttpRequestUtils.getUserAgent(req));
    }

    @Test
    @DisplayName("getRequestUri null 请求")
    void getRequestUri_NullRequest() {
        assertEquals("", HttpRequestUtils.getRequestUri(null));
    }

    @Test
    @DisplayName("getRequestUri 正常")
    void getRequestUri_Normal() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/user/info");
        assertEquals("/api/user/info", HttpRequestUtils.getRequestUri(req));
    }

    @Test
    @DisplayName("getMethod null 请求")
    void getMethod_NullRequest() {
        assertEquals("", HttpRequestUtils.getMethod(null));
    }

    @Test
    @DisplayName("getMethod 正常")
    void getMethod_Normal() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn("POST");
        assertEquals("POST", HttpRequestUtils.getMethod(req));
    }

    @ParameterizedTest
    @CsvSource({
            "203.0.113.1, 203.0.113.1",
            "10.0.0.1,    10.0.0.1",
            "192.168.0.1, 192.168.0.1",
            "127.0.0.1,   127.0.0.1",
            "::1,         ::1"
    })
    @DisplayName("多种合法IP格式应正常返回")
    void getClientIp_VariousIpFormats(String ipValue, String expected) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(ipValue);
        assertEquals(expected, HttpRequestUtils.getClientIp(req));
    }

    @Test
    @DisplayName("返回结果永不为null")
    void getClientIp_AlwaysReturnsNonNull() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRemoteAddr()).thenReturn(null);
        String ip = HttpRequestUtils.getClientIp(req);
        assertNotNull(ip);
        assertEquals(HttpRequestUtils.UNKNOWN, ip);
    }
}
