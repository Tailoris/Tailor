package com.tailoris.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.common.result.Result;
import com.tailoris.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 网关 HMAC 签名校验拦截器，供下游微服务使用。
 *
 * <p>核心网关 {@code CoreAuthGlobalFilter} 在转发请求时会对 X-User-Id 头计算
 * HMAC-SHA256 签名并通过 X-User-Id-Signature 头透传。下游服务通过此拦截器校验
 * 签名，防止绕过网关直接伪造 X-User-Id 头。</p>
 *
 * <h3>校验逻辑</h3>
 * <ol>
 *   <li>若请求中不存在 X-User-Id-Signature 头，视为内部调用，直接放行（向后兼容）</li>
 *   <li>若存在 X-User-Id-Signature 但 X-User-Id 缺失，返回 403</li>
 *   <li>若存在两者，计算 HMAC-SHA256(X-User-Id, GATEWAY_HMAC_SECRET) 并与签名头比对</li>
 *   <li>签名不匹配时返回 403 Forbidden</li>
 * </ol>
 *
 * <h3>使用方式</h3>
 * 下游服务在 WebMvcConfigurer 中注册此拦截器即可：
 * <pre>{@code
 * @Configuration
 * public class WebMvcConfig implements WebMvcConfigurer {
 *     private final GatewayHmacInterceptor gatewayHmacInterceptor;
 *
 *     @Override
 *     public void addInterceptors(InterceptorRegistry registry) {
 *         registry.addInterceptor(gatewayHmacInterceptor)
 *                 .addPathPatterns("/api/**");
 *     }
 * }
 * }</pre>
 *
 * @author Tailor IS Team
 * @since 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayHmacInterceptor implements HandlerInterceptor {

    /** HMAC 签名算法 */
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /** 网关 HMAC 签名密钥，需与网关 CoreAuthGlobalFilter 中的 GATEWAY_HMAC_SECRET 一致 */
    @Value("${GATEWAY_HMAC_SECRET:}")
    private String hmacSecret;

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String userId = request.getHeader("X-User-Id");
        String signature = request.getHeader("X-User-Id-Signature");

        // 无签名头 → 内部调用或未启用签名，直接放行（向后兼容）
        if (signature == null || signature.isBlank()) {
            return true;
        }

        // 有签名头但缺少 X-User-Id → 拒绝
        if (userId == null || userId.isBlank()) {
            log.warn("HMAC签名校验失败: X-User-Id缺失, path={}, ip={}",
                    request.getRequestURI(), getClientIp(request));
            writeForbidden(response, "请求头中缺少 X-User-Id");
            return false;
        }

        // 未配置密钥 → 跳过校验（兼容开发环境）
        if (hmacSecret == null || hmacSecret.isBlank()) {
            log.debug("GATEWAY_HMAC_SECRET 未配置，跳过 HMAC 签名校验");
            return true;
        }

        // 计算期望签名并比对
        String expectedSignature = computeHmac(userId, hmacSecret);
        if (expectedSignature == null) {
            log.error("HMAC签名计算失败, path={}", request.getRequestURI());
            writeForbidden(response, "签名校验失败");
            return false;
        }

        if (!expectedSignature.equals(signature)) {
            log.warn("HMAC签名不匹配: path={}, expected={}, actual={}, ip={}",
                    request.getRequestURI(), expectedSignature, signature, getClientIp(request));
            writeForbidden(response, "签名校验失败，请求被拒绝");
            return false;
        }

        log.debug("HMAC签名校验通过: path={}, userId={}", request.getRequestURI(), userId);
        return true;
    }

    /**
     * 计算 HMAC-SHA256 签名（与网关 CoreAuthGlobalFilter.signUserId 逻辑一致）。
     *
     * @param data   待签名数据
     * @param secret 签名密钥
     * @return Base64 编码的签名，计算失败返回 null
     */
    private String computeHmac(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("HMAC签名计算异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 写入 403 Forbidden 响应。
     */
    private void writeForbidden(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Result<Void> result = Result.fail(ResultCode.FORBIDDEN.getCode(), message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    /**
     * 获取客户端 IP。
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}