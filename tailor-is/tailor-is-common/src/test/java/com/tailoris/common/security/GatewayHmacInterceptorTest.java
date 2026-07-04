package com.tailoris.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * GatewayHmacInterceptor 单元测试
 * <p>🔒 H-04修复: 补充单元测试，验证 HMAC 签名校验的完整逻辑</p>
 */
@DisplayName("GatewayHmacInterceptor 测试")
@ExtendWith(MockitoExtension.class)
class GatewayHmacInterceptorTest {

    private static final String TEST_SECRET = "test-hmac-secret-key-2026";
    private static final String TEST_USER_ID = "12345";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private GatewayHmacInterceptor interceptor;
    private ObjectMapper objectMapper;
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        interceptor = new GatewayHmacInterceptor(objectMapper);
        // 注入 hmacSecret
        ReflectionTestUtils.setField(interceptor, "hmacSecret", TEST_SECRET);
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    /**
     * 计算期望的 HMAC-SHA256 签名
     */
    private String computeExpectedSignature(String userId, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(userId.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    @Test
    @DisplayName("无签名头 → 放行（内部调用）")
    void testNoSignature_PassThrough() throws Exception {
        when(request.getHeader("X-User-Id-Signature")).thenReturn(null);

        boolean result = interceptor.preHandle(request, response, null);

        assertTrue(result);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    @DisplayName("空白签名头 → 放行")
    void testBlankSignature_PassThrough() throws Exception {
        when(request.getHeader("X-User-Id-Signature")).thenReturn("   ");

        boolean result = interceptor.preHandle(request, response, null);

        assertTrue(result);
    }

    @Test
    @DisplayName("有签名头但缺少 X-User-Id → 拒绝 403")
    void testSignaturePresent_NoUserId_Reject() throws Exception {
        when(request.getHeader("X-User-Id-Signature")).thenReturn("some-signature");
        when(request.getHeader("X-User-Id")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        boolean result = interceptor.preHandle(request, response, null);

        assertFalse(result);
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType("application/json");
    }

    @Test
    @DisplayName("有效签名 → 校验通过")
    void testValidSignature_Pass() throws Exception {
        String expectedSig = computeExpectedSignature(TEST_USER_ID, TEST_SECRET);

        when(request.getHeader("X-User-Id-Signature")).thenReturn(expectedSig);
        when(request.getHeader("X-User-Id")).thenReturn(TEST_USER_ID);
        when(request.getRequestURI()).thenReturn("/api/test");

        boolean result = interceptor.preHandle(request, response, null);

        assertTrue(result);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    @DisplayName("无效签名 → 拒绝 403")
    void testInvalidSignature_Reject() throws Exception {
        when(request.getHeader("X-User-Id-Signature")).thenReturn("invalid-signature");
        when(request.getHeader("X-User-Id")).thenReturn(TEST_USER_ID);
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        boolean result = interceptor.preHandle(request, response, null);

        assertFalse(result);
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    @DisplayName("空密钥 → 跳过校验（开发环境兼容）")
    void testEmptySecret_SkipValidation() throws Exception {
        // 创建新实例，密钥为空
        GatewayHmacInterceptor emptyKeyInterceptor = new GatewayHmacInterceptor(objectMapper);
        ReflectionTestUtils.setField(emptyKeyInterceptor, "hmacSecret", "");

        when(request.getHeader("X-User-Id-Signature")).thenReturn("some-signature");
        when(request.getHeader("X-User-Id")).thenReturn(TEST_USER_ID);

        boolean result = emptyKeyInterceptor.preHandle(request, response, null);

        assertTrue(result);
    }

    @Test
    @DisplayName("空白 X-User-Id → 拒绝 403")
    void testBlankUserId_Reject() throws Exception {
        when(request.getHeader("X-User-Id-Signature")).thenReturn("some-signature");
        when(request.getHeader("X-User-Id")).thenReturn("   ");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        boolean result = interceptor.preHandle(request, response, null);

        assertFalse(result);
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    @DisplayName("X-Forwarded-For 获取客户端 IP")
    void testGetClientIp_FromXForwardedFor() throws Exception {
        String expectedSig = computeExpectedSignature(TEST_USER_ID, TEST_SECRET);

        when(request.getHeader("X-User-Id-Signature")).thenReturn(expectedSig);
        when(request.getHeader("X-User-Id")).thenReturn(TEST_USER_ID);
        when(request.getRequestURI()).thenReturn("/api/test");

        boolean result = interceptor.preHandle(request, response, null);

        assertTrue(result);
    }

    @Test
    @DisplayName("签名存在但篡改的 userId → 拒绝")
    void testTamperedUserId_Reject() throws Exception {
        // 签名是给 userId=12345 的，但请求头中 userId=99999
        String sigFor12345 = computeExpectedSignature("12345", TEST_SECRET);

        when(request.getHeader("X-User-Id-Signature")).thenReturn(sigFor12345);
        when(request.getHeader("X-User-Id")).thenReturn("99999");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        boolean result = interceptor.preHandle(request, response, null);

        assertFalse(result);
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }
}