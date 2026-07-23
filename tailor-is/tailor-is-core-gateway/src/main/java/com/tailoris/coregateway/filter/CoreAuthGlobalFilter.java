package com.tailoris.coregateway.filter;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Core Gateway 全局认证过滤器.
 *
 * <p>在核心网关层统一校验 Token，确保下游微服务接收到的请求都是已认证的。
 * 排除白名单路径（如登录、注册、公开接口）。</p>
 *
 * <h3>修复要点</h3>
 * <ul>
 *   <li>网关层强制 Token 验证，无效 Token 直接返回 401</li>
 *   <li>支持 Ant 风格路径匹配的白名单</li>
 *   <li>从 Authorization Header 或请求参数提取 Token</li>
 *   <li>校验通过后将用户信息透传到下游服务</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
@Slf4j
@Component
public class CoreAuthGlobalFilter implements GlobalFilter, Ordered {

    /** 默认白名单路径（不需Token） */
    private static final List<String> DEFAULT_EXCLUDED_PATHS = Arrays.asList(
            // v4.0 标准路径 (/api/v1/ 前缀)
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/sms-code",
            "/api/v1/auth/email-code",
            "/api/v1/auth/register/email",
            "/api/v1/auth/refresh",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/login/code",
            "/api/v1/auth/wechat-login",
            "/api/v1/auth/userinfo",
            // 用户服务完整路径 (/api/v1/user/auth/ 前缀)
            "/api/v1/user/auth/login",
            "/api/v1/user/auth/register",
            "/api/v1/user/auth/sms-code",
            "/api/v1/user/auth/email-code",
            "/api/v1/user/auth/register/email",
            "/api/v1/user/auth/refresh",
            "/api/v1/user/auth/reset-password",
            "/api/v1/user/auth/login/code",
            "/api/v1/user/auth/wechat-login",
            "/api/v1/user/auth/userinfo",
            "/api/v1/products/**",
            "/api/v1/product/**",
            "/api/v1/categories/**",
            "/api/v1/category/**",
            "/api/v1/community/**",
            "/api/v1/merchant/**",
            "/api/v1/public/**",
            "/api/v1/marketing/coupon/list",
            "/api/v1/coupon/list",
            "/api/v1/seckill/list",
            "/api/v1/body-size/template",
            "/api/v1/payment/options",
            "/api/v1/sandbox/**",
            "/api/v1/upload/**",
            "/api/v1/pattern/list",
            "/api/v1/pattern/detail/**",
            // 兼容旧版路径 (v4.1 移除)
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/register/email",
            "/api/auth/sms-code",
            "/api/auth/email-code",
            "/api/auth/refresh",
            "/api/auth/reset-password",
            "/api/auth/login/code",
            "/api/auth/wechat-login",
            "/api/products/**",
            "/api/product/**",
            "/api/categories/**",
            "/api/category/**",
            "/api/community/**",
            "/api/merchant/**",
            "/api/public/**",
            "/api/marketing/coupon/list",
            "/api/coupon/list",
            "/api/seckill/list",
            "/api/body-size/template",
            "/api/payment/options",
            "/api/sandbox/**",
            "/api/upload/**",
            "/api/pattern/list",
            "/api/pattern/detail/**",
            // 基础设施
            "/actuator/health",
            "/doc.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/favicon.ico"
    );

    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Value("${tailoris.gateway.auth.excluded-paths:}")
    private List<String> customExcludedPaths;

    /** 网关 HMAC 签名密钥，用于对 X-User-Id 头签名，防止下游服务被直接访问时伪造该头 */
    @Value("${GATEWAY_HMAC_SECRET:}")
    private String hmacSecret;

    public CoreAuthGlobalFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();

        log.info("Core Gateway - 请求进入: path={}, method={}", path, request.getMethod());

        // 1. 白名单放行
        if (isExcludedPath(path)) {
            log.info("Core Gateway - 白名单路径放行: {}", path);
            return chain.filter(exchange);
        }

        // 2. 非API路径放行（静态资源等）
        if (!path.startsWith("/api/")) {
            log.info("Core Gateway - 非API路径放行: {}", path);
            return chain.filter(exchange);
        }

        // 3. 提取Token
        String token = extractToken(request);
        log.info("Core Gateway - 提取Token: path={}, tokenExists={}", path, token != null && !token.isBlank());
        if (token == null || token.isBlank()) {
            log.warn("Core Gateway - 缺少Token: path={}, ip={}", path, getClientIp(request));
            return unauthorizedResponse(exchange, 401, "NOT_LOGGED_IN", "未登录，请先登录");
        }

        // 4. 校验Token
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            log.info("Core Gateway - Token校验结果: path={}, loginId={}", path, loginId);
            if (loginId == null) {
                log.warn("Core Gateway - Token无效或已过期: path={}, token={}", path, maskToken(token));
                return unauthorizedResponse(exchange, 401, "TOKEN_EXPIRED", "Token无效或已过期，请重新登录");
            }

            // 5. 透传用户信息到下游（BE-H-18: 先删除客户端传入的 X-User-Id 防止伪造）
            //    并对 X-User-Id 计算 HMAC-SHA256 签名，下游服务可据此校验请求确实来自网关
            //    保留 Authorization 头：下游服务 Sa-Token 配置 token-name=Authorization，
            //    需要从 Authorization 头读取 Token 以支持 StpUtil.getLoginIdAsLong() 等 API。
            String userIdStr = String.valueOf(loginId);
            String signature = signUserId(userIdStr);
            ServerHttpRequest mutatedRequest = request.mutate()
                    .headers(headers -> {
                        headers.remove("X-User-Id");
                        headers.remove("X-User-Id-Signature");
                        headers.add("X-User-Id", userIdStr);
                        if (signature != null) {
                            headers.add("X-User-Id-Signature", signature);
                        }
                        headers.add("X-Token-Validated", "true");
                        headers.add("X-Gateway-Token", token);
                    })
                    .build();
            log.info("Core Gateway - 添加X-User-Id请求头: path={}, userId={}", path, userIdStr);
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception e) {
            log.warn("Core Gateway - Token验证异常: path={}, error={}", path, e.getMessage());
            return unauthorizedResponse(exchange, 401, "TOKEN_INVALID", "Token验证失败，请重新登录");
        }
    }

    @Nullable
    private String extractToken(ServerHttpRequest request) {
        // 优先从 Authorization Header 提取
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        // 其次从请求参数提取（WebSocket等场景）
        String paramToken = request.getQueryParams().getFirst("token");
        if (paramToken != null && !paramToken.isBlank()) {
            return paramToken;
        }
        // 最后从Cookie提取
        var cookies = request.getCookies().get("satoken");
        if (cookies != null && !cookies.isEmpty()) {
            return cookies.get(0).getValue();
        }
        return null;
    }

    private boolean isExcludedPath(String path) {
        for (String pattern : DEFAULT_EXCLUDED_PATHS) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        if (customExcludedPaths != null) {
            for (String pattern : customExcludedPaths) {
                if (pathMatcher.match(pattern, path)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getClientIp(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeaders().getFirst("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            var remote = request.getRemoteAddress();
            ip = remote == null ? "unknown" : remote.getAddress().getHostAddress();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }

    /**
     * 对 userId 计算 HMAC-SHA256 签名，防止下游服务被直接访问时伪造 X-User-Id 头.
     *
     * @param userId 用户ID字符串
     * @return Base64 编码的签名，未配置密钥时返回 null（不签名）
     */
    @Nullable
    private String signUserId(String userId) {
        if (hmacSecret == null || hmacSecret.isBlank()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(userId.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.warn("Core Gateway - X-User-Id HMAC签名失败: {}", e.getMessage());
            return null;
        }
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        return unauthorizedResponse(exchange, 401, "UNAUTHORIZED", message);
    }

    /**
     * 返回标准化的未授权响应，包含错误码和错误子类型。
     *
     * @param exchange 当前请求上下文
     * @param code     HTTP 状态码
     * @param subCode  业务错误子类型（NOT_LOGGED_IN / TOKEN_EXPIRED / TOKEN_INVALID）
     * @param message  用户可读的错误消息
     */
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, int code, String subCode, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", code);
        result.put("subCode", subCode);
        result.put("message", message);
        result.put("data", null);
        result.put("timestamp", System.currentTimeMillis());

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(result);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Core Gateway - Failed to write unauthorized response", e);
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
