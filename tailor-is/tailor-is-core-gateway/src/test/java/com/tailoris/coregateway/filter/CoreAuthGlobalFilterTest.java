package com.tailoris.coregateway.filter;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CoreAuthGlobalFilter 单元测试.
 *
 * <p>测试白名单匹配、Token 提取顺序、客户端 IP 解析、Token 脱敏、
 * filter() 主流程及 unauthorizedResponse() 等关键逻辑。</p>
 */
@DisplayName("CoreAuthGlobalFilter 单元测试")
class CoreAuthGlobalFilterTest {

    private CoreAuthGlobalFilter filter;
    private AntPathMatcher pathMatcher;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        filter = new CoreAuthGlobalFilter(objectMapper);
        pathMatcher = new AntPathMatcher();
    }

    // ========== 辅助方法 ==========

    /** 构造 mock exchange, request 带有指定 path */
    private ServerWebExchange mockExchangeWithPath(String path) {
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        RequestPath requestPath = mock(RequestPath.class);

        when(exchange.getRequest()).thenReturn(request);
        when(request.getPath()).thenReturn(requestPath);
        when(requestPath.toString()).thenReturn(path);
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        when(request.getQueryParams()).thenReturn(new LinkedMultiValueMap<>());
        when(request.getCookies()).thenReturn(new LinkedMultiValueMap<>());

        return exchange;
    }

    /** 为 unauthorizedResponse 设置 response mock */
    private void setupResponseMock(ServerWebExchange exchange) {
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        DataBufferFactory bufferFactory = mock(DataBufferFactory.class);
        DataBuffer buffer = mock(DataBuffer.class);
        HttpHeaders responseHeaders = new HttpHeaders();

        when(exchange.getResponse()).thenReturn(response);
        when(response.getHeaders()).thenReturn(responseHeaders);
        when(response.bufferFactory()).thenReturn(bufferFactory);
        when(bufferFactory.wrap(any(byte[].class))).thenReturn(buffer);
        when(response.writeWith(any())).thenReturn(Mono.empty());
    }

    // ========== 默认白名单 & AntPathMatcher ==========

    @Test
    @DisplayName("默认白名单至少包含 /api/auth/login")
    void testDefaultExcludedPaths() {
        @SuppressWarnings("unchecked")
        List<String> defaultPaths = (List<String>) ReflectionTestUtils.getField(
                filter, "DEFAULT_EXCLUDED_PATHS");
        assertNotNull(defaultPaths);
        assertTrue(defaultPaths.contains("/api/auth/login"));
        assertTrue(defaultPaths.contains("/api/auth/register"));
        assertTrue(defaultPaths.contains("/api/auth/refresh"));
        assertTrue(defaultPaths.contains("/actuator/**"));
    }

    @Test
    @DisplayName("Ant 路径通配符匹配: /api/public/**")
    void testAntPattern_public() {
        assertTrue(pathMatcher.match("/api/public/**", "/api/public/health"));
        assertTrue(pathMatcher.match("/api/public/**", "/api/public/a/b/c"));
        assertFalse(pathMatcher.match("/api/public/**", "/api/private"));
    }

    @Test
    @DisplayName("Ant 路径通配符匹配: /actuator/**")
    void testAntPattern_actuator() {
        assertTrue(pathMatcher.match("/actuator/**", "/actuator/health"));
        assertTrue(pathMatcher.match("/actuator/**", "/actuator/prometheus"));
    }

    // ========== Token 提取 ==========

    @Test
    @DisplayName("从 Authorization Bearer 头提取 Token")
    void testExtractToken_bearer() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer abc123.def456.ghi789");
        when(request.getHeaders()).thenReturn(headers);

        String token = ReflectionTestUtils.invokeMethod(filter, "extractToken", request);
        assertEquals("abc123.def456.ghi789", token);
    }

    @Test
    @DisplayName("从 query 参数提取 Token")
    void testExtractToken_queryParam() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("token", "queryToken123");
        when(request.getQueryParams()).thenReturn(queryParams);

        String token = ReflectionTestUtils.invokeMethod(filter, "extractToken", request);
        assertEquals("queryToken123", token);
    }

    @Test
    @DisplayName("从 Cookie(satoken) 提取 Token")
    void testExtractToken_cookie() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        when(request.getQueryParams()).thenReturn(new LinkedMultiValueMap<>());

        MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
        cookies.add("satoken", new HttpCookie("satoken", "cookie-token-xyz"));
        when(request.getCookies()).thenReturn(cookies);

        String token = ReflectionTestUtils.invokeMethod(filter, "extractToken", request);
        assertEquals("cookie-token-xyz", token);
    }

    @Test
    @DisplayName("无 Token 时返回 null")
    void testExtractToken_null() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        when(request.getQueryParams()).thenReturn(new LinkedMultiValueMap<>());
        when(request.getCookies()).thenReturn(new LinkedMultiValueMap<>());

        String token = ReflectionTestUtils.invokeMethod(filter, "extractToken", request);
        assertNull(token);
    }

    // ========== 客户端 IP ==========

    @Test
    @DisplayName("客户端 IP 提取: X-Forwarded-For 优先(取第一个)")
    void testGetClientIp_xForwardedFor() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Forwarded-For", "192.168.1.100, 10.0.0.1");
        when(request.getHeaders()).thenReturn(headers);

        String ip = ReflectionTestUtils.invokeMethod(filter, "getClientIp", request);
        assertEquals("192.168.1.100", ip);
    }

    @Test
    @DisplayName("客户端 IP 提取: X-Real-IP 兜底")
    void testGetClientIp_xRealIp() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Real-IP", "172.16.0.5");
        when(request.getHeaders()).thenReturn(headers);

        String ip = ReflectionTestUtils.invokeMethod(filter, "getClientIp", request);
        assertEquals("172.16.0.5", ip);
    }

    @Test
    @DisplayName("客户端 IP 提取: 远端地址兜底")
    void testGetClientIp_remote() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        when(request.getRemoteAddress()).thenReturn(
                new InetSocketAddress("203.0.113.42", 54321));

        String ip = ReflectionTestUtils.invokeMethod(filter, "getClientIp", request);
        assertEquals("203.0.113.42", ip);
    }

    @Test
    @DisplayName("客户端 IP 提取: 全部缺失返回 unknown")
    void testGetClientIp_unknown() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        when(request.getRemoteAddress()).thenReturn(null);

        String ip = ReflectionTestUtils.invokeMethod(filter, "getClientIp", request);
        assertEquals("unknown", ip);
    }

    @Test
    @DisplayName("客户端 IP 提取: X-Forwarded-For 为 'unknown' 时降级到 X-Real-IP")
    void testGetClientIp_xForwardedForUnknown() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Forwarded-For", "unknown");
        headers.add("X-Real-IP", "10.0.0.99");
        when(request.getHeaders()).thenReturn(headers);

        String ip = ReflectionTestUtils.invokeMethod(filter, "getClientIp", request);
        assertEquals("10.0.0.99", ip);
    }

    // ========== Token 脱敏 ==========

    @Test
    @DisplayName("Token 脱敏: 长度足够时保留首尾")
    void testMaskToken_long() {
        String masked = ReflectionTestUtils.invokeMethod(filter, "maskToken", "abcdefghij1234567890");
        assertEquals("abcd***7890", masked);
    }

    @Test
    @DisplayName("Token 脱敏: 长度不足时返回 ***")
    void testMaskToken_short() {
        String masked = ReflectionTestUtils.invokeMethod(filter, "maskToken", "abc");
        assertEquals("***", masked);
    }

    @Test
    @DisplayName("Token 脱敏: null 返回 ***")
    void testMaskToken_null() {
        String masked = (String) ReflectionTestUtils.invokeMethod(filter, "maskToken", new Object[]{null});
        assertEquals("***", masked);
    }

    // ========== getOrder ==========

    @Test
    @DisplayName("过滤器顺序为 -100, 保证最先执行")
    void testGetOrder() {
        assertEquals(-100, filter.getOrder());
    }

    // ========== filter() 主流程 ==========

    @Test
    @DisplayName("filter: 白名单路径 /api/auth/login 直接放行")
    void testFilter_whiteListPath() {
        ServerWebExchange exchange = mockExchangeWithPath("/api/auth/login");
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        Mono<Void> result = filter.filter(exchange, chain);

        verify(chain).filter(exchange);
        StepVerifier.create(result).verifyComplete();
    }

    @Test
    @DisplayName("filter: 白名单路径 /api/public/health 直接放行")
    void testFilter_whiteListPublicPath() {
        ServerWebExchange exchange = mockExchangeWithPath("/api/public/health");
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        Mono<Void> result = filter.filter(exchange, chain);

        verify(chain).filter(exchange);
        StepVerifier.create(result).verifyComplete();
    }

    @Test
    @DisplayName("filter: 非 /api/ 路径直接放行")
    void testFilter_nonApiPath() {
        ServerWebExchange exchange = mockExchangeWithPath("/favicon.ico");
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        Mono<Void> result = filter.filter(exchange, chain);

        verify(chain).filter(exchange);
        StepVerifier.create(result).verifyComplete();
    }

    @Test
    @DisplayName("filter: 非 /api/ 路径 /static/js/app.js 直接放行")
    void testFilter_staticResource() {
        ServerWebExchange exchange = mockExchangeWithPath("/static/js/app.js");
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        Mono<Void> result = filter.filter(exchange, chain);

        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("filter: 缺少 Token 返回 401")
    void testFilter_missingToken() {
        ServerWebExchange exchange = mockExchangeWithPath("/api/user/info");
        setupResponseMock(exchange);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        Mono<Void> result = filter.filter(exchange, chain);

        verify(chain, never()).filter(any());
        ServerHttpResponse response = exchange.getResponse();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        StepVerifier.create(result).verifyComplete();
    }

    @Test
    @DisplayName("filter: 有效 Token 透传用户信息到下游")
    void testFilter_validToken() {
        ServerWebExchange exchange = mockExchangeWithPath("/api/user/info");
        ServerHttpRequest request = exchange.getRequest();

        // 设置 Authorization header
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer valid-token-123");
        when(request.getHeaders()).thenReturn(headers);

        // mock request.mutate() 链
        ServerHttpRequest.Builder requestBuilder = mock(ServerHttpRequest.Builder.class);
        ServerHttpRequest mutatedRequest = mock(ServerHttpRequest.class);
        when(request.mutate()).thenReturn(requestBuilder);
        when(requestBuilder.header(anyString(), any())).thenReturn(requestBuilder);
        when(requestBuilder.build()).thenReturn(mutatedRequest);

        // mock exchange.mutate() 链
        ServerWebExchange.Builder exchangeBuilder = mock(ServerWebExchange.Builder.class);
        ServerWebExchange mutatedExchange = mock(ServerWebExchange.class);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(any(ServerHttpRequest.class))).thenReturn(exchangeBuilder);
        when(exchangeBuilder.build()).thenReturn(mutatedExchange);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(mutatedExchange)).thenReturn(Mono.empty());

        try (var mockedStpUtil = mockStatic(StpUtil.class)) {
            mockedStpUtil.when(() -> StpUtil.getLoginIdByToken("valid-token-123"))
                    .thenReturn(42L);

            Mono<Void> result = filter.filter(exchange, chain);

            // 验证透传了 X-User-Id 和 X-Token-Validated
            verify(requestBuilder).header("X-User-Id", "42");
            verify(requestBuilder).header("X-Token-Validated", "true");
            verify(chain).filter(mutatedExchange);
            StepVerifier.create(result).verifyComplete();
        }
    }

    @Test
    @DisplayName("filter: 无效 Token (loginId 为 null) 返回 401")
    void testFilter_invalidToken() {
        ServerWebExchange exchange = mockExchangeWithPath("/api/user/info");
        ServerHttpRequest request = exchange.getRequest();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer expired-token");
        when(request.getHeaders()).thenReturn(headers);

        setupResponseMock(exchange);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        try (var mockedStpUtil = mockStatic(StpUtil.class)) {
            mockedStpUtil.when(() -> StpUtil.getLoginIdByToken("expired-token"))
                    .thenReturn(null);

            Mono<Void> result = filter.filter(exchange, chain);

            verify(chain, never()).filter(any());
            ServerHttpResponse response = exchange.getResponse();
            verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
            StepVerifier.create(result).verifyComplete();
        }
    }

    @Test
    @DisplayName("filter: Token 验证异常返回 401")
    void testFilter_tokenException() {
        ServerWebExchange exchange = mockExchangeWithPath("/api/user/info");
        ServerHttpRequest request = exchange.getRequest();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer bad-token");
        when(request.getHeaders()).thenReturn(headers);

        setupResponseMock(exchange);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        try (var mockedStpUtil = mockStatic(StpUtil.class)) {
            mockedStpUtil.when(() -> StpUtil.getLoginIdByToken("bad-token"))
                    .thenThrow(new RuntimeException("Redis connection failed"));

            Mono<Void> result = filter.filter(exchange, chain);

            verify(chain, never()).filter(any());
            ServerHttpResponse response = exchange.getResponse();
            verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
            StepVerifier.create(result).verifyComplete();
        }
    }

    // ========== isExcludedPath ==========

    @Test
    @DisplayName("isExcludedPath: 默认白名单路径匹配")
    void testIsExcludedPath_defaultPaths() {
        // 未设置 customExcludedPaths
        ReflectionTestUtils.setField(filter, "customExcludedPaths", null);

        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(filter, "isExcludedPath", "/api/auth/login"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(filter, "isExcludedPath", "/api/auth/register"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(filter, "isExcludedPath", "/api/auth/sms-code"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(filter, "isExcludedPath", "/api/auth/refresh"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(filter, "isExcludedPath", "/api/auth/reset-password"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(filter, "isExcludedPath", "/api/public/anything"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(filter, "isExcludedPath", "/actuator/health"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(filter, "isExcludedPath", "/doc.html"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(filter, "isExcludedPath", "/swagger-ui/index.html"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(filter, "isExcludedPath", "/v3/api-docs/swagger"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(filter, "isExcludedPath", "/favicon.ico"));

        // 非白名单
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(filter, "isExcludedPath", "/api/user/info"));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(filter, "isExcludedPath", "/api/order/list"));
    }

    @Test
    @DisplayName("isExcludedPath: 自定义白名单路径匹配")
    void testIsExcludedPath_customPaths() {
        ReflectionTestUtils.setField(filter, "customExcludedPaths",
                List.of("/api/custom/**", "/api/health"));

        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(filter, "isExcludedPath", "/api/custom/test"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(filter, "isExcludedPath", "/api/health"));
        // 默认白名单仍然有效
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(filter, "isExcludedPath", "/api/auth/login"));
        // 非白名单
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(filter, "isExcludedPath", "/api/user/info"));
    }

    @Test
    @DisplayName("isExcludedPath: customExcludedPaths 为空列表时不影响默认白名单")
    void testIsExcludedPath_emptyCustomPaths() {
        ReflectionTestUtils.setField(filter, "customExcludedPaths", List.of());

        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(filter, "isExcludedPath", "/api/auth/login"));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(filter, "isExcludedPath", "/api/user/info"));
    }

    // ========== unauthorizedResponse ==========

    @Test
    @DisplayName("unauthorizedResponse: 返回 401 状态码和 JSON 响应体")
    void testUnauthorizedResponse() {
        ServerWebExchange exchange = mockExchangeWithPath("/api/user/info");
        setupResponseMock(exchange);

        Mono<Void> result = ReflectionTestUtils.invokeMethod(
                filter, "unauthorizedResponse", exchange, "测试消息");

        ServerHttpResponse response = exchange.getResponse();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);

        // 验证 Content-Type 为 application/json
        HttpHeaders responseHeaders = response.getHeaders();
        // response.getHeaders() 在 mock 上默认返回 null, 需要用真实 HttpHeaders
        // 这里通过 verify writeWith 被调用来间接验证
        verify(response).writeWith(any());

        StepVerifier.create(result).verifyComplete();
    }

    @Test
    @DisplayName("unauthorizedResponse: JSON 体包含 code=401 和 message")
    void testUnauthorizedResponse_jsonContent() throws Exception {
        ServerWebExchange exchange = mockExchangeWithPath("/api/user/info");
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        DataBufferFactory bufferFactory = mock(DataBufferFactory.class);
        HttpHeaders responseHeaders = new HttpHeaders();

        when(exchange.getResponse()).thenReturn(response);
        when(response.getHeaders()).thenReturn(responseHeaders);
        when(response.bufferFactory()).thenReturn(bufferFactory);

        // 捕获写入的 byte[] 以验证 JSON 内容
        ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);
        DataBuffer buffer = mock(DataBuffer.class);
        when(bufferFactory.wrap(bytesCaptor.capture())).thenReturn(buffer);
        when(response.writeWith(any())).thenReturn(Mono.empty());

        Mono<Void> result = ReflectionTestUtils.invokeMethod(
                filter, "unauthorizedResponse", exchange, "Token无效");

        StepVerifier.create(result).verifyComplete();

        // 验证 JSON 内容
        byte[] writtenBytes = bytesCaptor.getValue();
        assertNotNull(writtenBytes);
        String json = new String(writtenBytes);
        assertTrue(json.contains("\"code\":401"));
        assertTrue(json.contains("\"message\":\"Token无效\""));
        assertTrue(json.contains("\"data\":null"));
        assertTrue(json.contains("\"timestamp\""));
    }

    // ========== extractToken 边界 ==========

    @Test
    @DisplayName("extractToken: Authorization 头非 Bearer 前缀时跳过")
    void testExtractToken_nonBearerHeader() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz");
        when(request.getHeaders()).thenReturn(headers);
        when(request.getQueryParams()).thenReturn(new LinkedMultiValueMap<>());
        when(request.getCookies()).thenReturn(new LinkedMultiValueMap<>());

        String token = ReflectionTestUtils.invokeMethod(filter, "extractToken", request);
        assertNull(token);
    }

    @Test
    @DisplayName("extractToken: query 参数 token 为空字符串时跳过")
    void testExtractToken_blankQueryParam() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("token", "   ");
        when(request.getQueryParams()).thenReturn(queryParams);
        when(request.getCookies()).thenReturn(new LinkedMultiValueMap<>());

        String token = ReflectionTestUtils.invokeMethod(filter, "extractToken", request);
        assertNull(token);
    }
}
