package com.tailoris.common.security;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * CSRF 同步令牌过滤器 - 修复 B-H23
 *
 * <p>实现CSRF防护的同步令牌模式（Synchronizer Token Pattern）：</p>
 * <ol>
 *   <li>登录成功后生成CSRF Token并存入会话</li>
 *   <li>前端将Token放在请求头 X-CSRF-Token</li>
 *   <li>服务端验证Token与会话中的Token是否一致</li>
 * </ol>
 *
 * <h3>关键改进</h3>
 * <ul>
 *   <li>替代原CsrfTokenInterceptor的简单逻辑</li>
 *   <li>使用一次性Token（同Token不能重复使用）</li>
 *   <li>集成Sa-Token会话管理</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class CsrfTokenFilter extends OncePerRequestFilter {

    private static final String CSRF_HEADER = "X-CSRF-Token";
    private static final String CSRF_SESSION_KEY = "_csrf_token";
    private static final String CSRF_USED_KEY = "_csrf_used_tokens";

    /** 需要CSRF验证的HTTP方法 */
    private static final Set<String> CSRF_METHODS = new HashSet<>(
            Arrays.asList("POST", "PUT", "DELETE", "PATCH"));

    /** 不需要CSRF的路径 */
    private static final String[] EXCLUDED_PATHS = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/sms-code",
            "/api/auth/refresh",
            "/api/auth/reset-password"
    };

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod();
        String path = request.getRequestURI();

        // 仅对写操作 + 需保护路径生效
        if (!CSRF_METHODS.contains(method.toUpperCase()) || isExcluded(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 已登录用户才需要CSRF验证
        if (!isUserLoggedIn()) {
            filterChain.doFilter(request, response);
            return;
        }

        String csrfToken = request.getHeader(CSRF_HEADER);
        if (!StringUtils.hasText(csrfToken)) {
            rejectCsrf(response, "缺少CSRF Token");
            return;
        }

        // 一次性Token校验
        if (!validateAndConsumeToken(csrfToken)) {
            rejectCsrf(response, "CSRF Token无效或已使用");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 验证并消费Token
     */
    @SuppressWarnings("unchecked")
    private boolean validateAndConsumeToken(String token) {
        try {
            String sessionId = StpUtil.getTokenValue();
            if (sessionId == null || sessionId.isEmpty()) {
                return false;
            }

            // 使用Sa-Token的Session存储CSRF Token
            String storedToken = (String) StpUtil.getSession().get(CSRF_SESSION_KEY);

            if (storedToken == null || !storedToken.equals(token)) {
                log.warn("CSRF Token不匹配: stored={}, received={}", storedToken, maskToken(storedToken));
                return false;
            }

            // 验证通过后立即消费（一次性Token）
            StpUtil.getSession().delete(CSRF_SESSION_KEY);
            return true;
        } catch (Exception e) {
            log.error("CSRF验证异常", e);
            return false;
        }
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }

    /**
     * 检查用户是否已登录
     */
    private boolean isUserLoggedIn() {
        try {
            return StpUtil.isLogin();
        } catch (Exception e) {
            log.warn("检查登录状态异常", e);
            return false;
        }
    }

    private boolean isExcluded(String path) {
        for (String excluded : EXCLUDED_PATHS) {
            if (path.startsWith(excluded)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 生成新的CSRF Token
     */
    public String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void rejectCsrf(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        SaResult result = SaResult.error(message);
        result.setCode(403);

        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
