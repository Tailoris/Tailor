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
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/sms-code",
            "/api/auth/refresh",
            "/api/auth/reset-password",
            "/api/public/**",
            "/actuator/**",
            "/doc.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/favicon.ico"
    );

    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Value("${tailoris.gateway.auth.excluded-paths:}")
    private List<String> customExcludedPaths;

    public CoreAuthGlobalFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();

        // 1. 白名单放行
        if (isExcludedPath(path)) {
            log.debug("Core Gateway - 白名单路径放行: {}", path);
            return chain.filter(exchange);
        }

        // 2. 非API路径放行（静态资源等）
        if (!path.startsWith("/api/")) {
            return chain.filter(exchange);
        }

        // 3. 提取Token
        String token = extractToken(request);
        if (token == null || token.isBlank()) {
            log.warn("Core Gateway - 缺少Token: path={}, ip={}", path, getClientIp(request));
            return unauthorizedResponse(exchange, "未登录，请先登录");
        }

        // 4. 校验Token
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                log.warn("Core Gateway - Token无效或已过期: path={}, token={}", path, maskToken(token));
                return unauthorizedResponse(exchange, "Token无效或已过期，请重新登录");
            }

            // 5. 透传用户信息到下游
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", String.valueOf(loginId))
                    .header("X-Token-Validated", "true")
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception e) {
            log.warn("Core Gateway - Token验证异常: path={}, error={}", path, e.getMessage());
            return unauthorizedResponse(exchange, "Token验证失败，请重新登录");
        }
    }

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

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 401);
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
