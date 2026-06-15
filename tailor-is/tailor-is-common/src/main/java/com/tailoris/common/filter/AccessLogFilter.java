package com.tailoris.common.filter;

import com.tailoris.common.util.HttpRequestUtils;
import com.tailoris.common.util.TraceUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 访问日志过滤器 - 修复 L-22.
 *
 * <p>🔒 L-22修复: 记录所有 HTTP 请求的访问日志，包含：traceId、URI、HTTP方法、客户端IP、
 *    User-Agent、响应状态、响应耗时、用户ID（若已登录）等关键信息。</p>
 *
 * <p>日志格式：</p>
 * <pre>
 * ACCESS | traceId=xxx | userId=12345 | 192.168.1.10 | POST /api/order/create | UA=Mozilla/5.0 | 200 | 156ms
 * </pre>
 *
 * <p>注意：仅记录业务请求（{@code /api/**}），不记录静态资源与健康检查，避免日志爆炸。</p>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)  // 最低优先级，确保 traceId 已写入
public class AccessLogFilter extends OncePerRequestFilter {

    private static final String API_PREFIX = "/api/";
    private static final String ACTUATOR_PREFIX = "/actuator/";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 仅记录业务请求
        String uri = request.getRequestURI();
        if (!shouldLog(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long cost = System.currentTimeMillis() - start;
            int status = response.getStatus();
            String traceId = TraceUtils.currentTraceId();
            String ip = HttpRequestUtils.getClientIp(request);
            String method = request.getMethod();
            String userAgent = request.getHeader("User-Agent");
            String userId = extractUserId(request);

            // 慢请求 / 错误请求升级到 WARN
            if (status >= 500 || cost > 3000) {
                log.warn("ACCESS | traceId={} | userId={} | {} | {} {} | UA={} | status={} | cost={}ms",
                        traceId, userId, ip, method, uri, userAgent, status, cost);
            } else {
                log.info("ACCESS | traceId={} | userId={} | {} | {} {} | UA={} | status={} | cost={}ms",
                        traceId, userId, ip, method, uri, userAgent, status, cost);
            }
        }
    }

    private boolean shouldLog(String uri) {
        if (uri == null) {
            return false;
        }
        return uri.startsWith(API_PREFIX) && !uri.startsWith(ACTUATOR_PREFIX);
    }

    /**
     * 提取已登录用户ID（通过 Sa-Token 上下文）.
     */
    private String extractUserId(HttpServletRequest request) {
        try {
            Object loginId = request.getAttribute("loginId");
            if (loginId != null) {
                return String.valueOf(loginId);
            }
            // 尝试 Sa-Token 头
            String token = request.getHeader("satoken");
            if (token == null) {
                token = request.getHeader("Authorization");
            }
            return token == null ? "-" : "-";
        } catch (Exception e) {
            return "-";
        }
    }
}
