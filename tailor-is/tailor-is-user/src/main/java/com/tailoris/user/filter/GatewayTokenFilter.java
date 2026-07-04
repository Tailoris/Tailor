package com.tailoris.user.filter;

import cn.dev33.satoken.stp.StpUtil;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = request.getHeader("X-Gateway-Token");
        if (token != null && !token.isBlank()) {
            try {
                Object loginId = StpUtil.getLoginIdByToken(token);
                if (loginId != null && !StpUtil.isLogin()) {
                    StpUtil.login(loginId);
                    log.debug("Gateway Token 自动登录成功: userId={}", loginId);
                }
            } catch (Exception e) {
                log.warn("Gateway Token 自动登录失败: {}", e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}