package com.tailoris.common.security;

import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.io.IOException;

/**
 * 认证拦截器 - 修复 B-H21
 *
 * <p>基于白名单配置的认证拦截器：</p>
 * <ol>
 *   <li>检查请求路径是否在白名单中</li>
 *   <li>白名单路径直接放行</li>
 *   <li>非白名单路径验证Token</li>
 *   <li>未认证返回401</li>
 * </ol>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthWhitelistProperties whitelistProperties;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String path = request.getRequestURI();

        // OPTIONS请求直接放行（CORS预检）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 检查白名单
        if (whitelistProperties.isWhitelisted(path)) {
            log.debug("白名单放行: path={}", path);
            return true;
        }

        // 验证Token
        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            log.warn("未提供Token: path={}, ip={}", path, getClientIp(request));
            writeUnauthorized(response, "未登录或Token缺失");
            return false;
        }

        // 简化处理：实际应使用Sa-Token验证Token有效性
        // 完整的验证在AuthGlobalFilter中处理
        if (!isValidToken(token)) {
            log.warn("Token无效: path={}, token={}", path, maskToken(token));
            writeUnauthorized(response, "Token无效或已过期");
            return false;
        }

        return true;
    }

    /**
     * 提取Token
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader)) {
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
            return authHeader;
        }

        // 也支持 X-Access-Token
        return request.getHeader("X-Access-Token");
    }

    /**
     * 验证Token是否有效 - 使用Sa-Token进行实际验证
     */
    private boolean isValidToken(String token) {
        if (token == null || token.length() < 16) {
            return false;
        }
        try {
            // 使用 Sa-Token 验证 token 是否有效
            Object loginId = StpUtil.getLoginIdByToken(token);
            return loginId != null;
        } catch (Exception e) {
            log.debug("Sa-Token验证失败: token={}", maskToken(token));
            return false;
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Result<Void> result = Result.fail(ResultCode.UNAUTHORIZED.getCode(), message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }
}
