package com.tailoris.common.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 网关 HMAC 签名校验器.
 *
 * <p>校验网关注入的 X-User-Id 头是否携带有效的 HMAC-SHA256 签名，
 * 防止攻击者绕过网关直接伪造 X-User-Id 头。</p>
 *
 * <h3>使用方式</h3>
 * <pre>
 * if (gatewaySignatureValidator.shouldValidate() &amp;&amp;
 *     !gatewaySignatureValidator.validate(request)) {
 *     response.sendError(403, "Invalid gateway signature");
 *     return;
 * }
 * </pre>
 *
 * @author Tailor IS Team
 * @since 1.2.0
 */
@Slf4j
@Component
public class GatewaySignatureValidator {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_SIGNATURE = "X-User-Id-Signature";
    private static final String HEADER_TOKEN_VALIDATED = "X-Token-Validated";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${GATEWAY_HMAC_SECRET:}")
    private String hmacSecret;

    /**
     * 判断是否需要校验签名.
     *
     * <p>仅在以下条件同时满足时才需要校验：
     * <ol>
     *   <li>已配置 GATEWAY_HMAC_SECRET</li>
     *   <li>请求包含 X-User-Id 头</li>
     *   <li>请求包含 X-Token-Validated 头（说明经过网关）</li>
     * </ol>
     */
    public boolean shouldValidate(HttpServletRequest request) {
        return hmacSecret != null && !hmacSecret.isBlank()
                && request.getHeader(HEADER_USER_ID) != null
                && "true".equals(request.getHeader(HEADER_TOKEN_VALIDATED));
    }

    /**
     * 校验签名是否有效.
     *
     * @param request HTTP 请求
     * @return true 表示签名有效；false 表示签名缺失或不匹配
     */
    public boolean validate(HttpServletRequest request) {
        String userId = request.getHeader(HEADER_USER_ID);
        String signature = request.getHeader(HEADER_SIGNATURE);

        if (userId == null || signature == null) {
            log.warn("网关签名校验失败: X-User-Id 或 X-User-Id-Signature 头缺失, uri={}", request.getRequestURI());
            return false;
        }

        String expected = computeSignature(userId);
        if (expected == null) {
            return false;
        }

        boolean valid = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );
        if (!valid) {
            log.warn("网关签名校验失败: 签名不匹配, userId={}, uri={}", userId, request.getRequestURI());
        }
        return valid;
    }

    private String computeSignature(String userId) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(userId.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("网关签名计算失败: {}", e.getMessage());
            return null;
        }
    }
}