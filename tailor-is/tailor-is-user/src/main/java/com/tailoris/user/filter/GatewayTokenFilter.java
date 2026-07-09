package com.tailoris.user.filter;

import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.security.GatewaySignatureValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(-100)
public class GatewayTokenFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final GatewaySignatureValidator signatureValidator;

    public GatewayTokenFilter(GatewaySignatureValidator signatureValidator) {
        this.signatureValidator = signatureValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 校验网关 HMAC 签名，防止绕过网关伪造 X-User-Id
        if (signatureValidator.shouldValidate(request) && !signatureValidator.validate(request)) {
            log.warn("网关签名校验失败，拒绝请求: uri={}, ip={}", request.getRequestURI(), request.getRemoteAddr());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid gateway signature");
            return;
        }

        String token = extractToken(request);
        if (token != null && !token.isBlank()) {
            try {
                Object loginId = StpUtil.getLoginIdByToken(token);
                if (loginId != null && !StpUtil.isLogin()) {
                    StpUtil.login(loginId);
                    log.debug("Token 自动登录成功: userId={}", loginId);
                }
            } catch (Exception e) {
                log.warn("Token 自动登录失败: {}", e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        String gatewayToken = request.getHeader("X-Gateway-Token");
        if (gatewayToken != null && !gatewayToken.isBlank()) {
            return gatewayToken;
        }
        return request.getParameter("token");
    }
}