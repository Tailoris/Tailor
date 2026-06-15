package com.tailoris.common.auth;

import com.tailoris.common.metrics.BusinessMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 用户认证服务（演示版本，使用内存用户库）
 *
 *  关键：所有登录成功 / 登录失败分支都调用 BusinessMetrics，
 *       使 Prometheus 能够观测到真实业务指标。
 *
 *  指标采集位置：
 *    - 管理员登录成功 -> recordAdminLogin
 *    - 普通用户登录成功 -> recordUserLogin
 *    - 任意登录失败    -> recordFailedLogin
 */
@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final BusinessMetrics metrics;

    /** 演示用户的用户名、密码、角色（从 application.yml 的 spring.profiles.active 或配置中读取，这里写死作为演示） */
    private final Map<String, DemoUser> userStore = new ConcurrentHashMap<>();

    /** Token -> 过期时间戳（简单实现） */
    private final Map<String, TokenInfo> tokenStore = new ConcurrentHashMap<>();

    @Value("${app.demo-users:}")
    private String demoUsersConfig;  // 备用字段

    public AuthenticationService(BusinessMetrics metrics) {
        this.metrics = metrics;
    }

    @PostConstruct
    public void init() {
        // 初始化演示用户
        userStore.put("admin", new DemoUser("admin", "admin123", "admin"));
        userStore.put("demo-user", new DemoUser("demo-user", "user123", "user"));
        userStore.put("test-merchant", new DemoUser("test-merchant", "merchant123", "merchant"));

        // 为了测试告警阈值（大量失败登录触发告警），增加一批常规用户
        for (int i = 0; i < 10; i++) {
            userStore.put("user" + i, new DemoUser("user" + i, "pwd" + i, "user"));
        }

        log.info("[AuthenticationService] 初始化完成，共有 {} 个内存用户，BusinessMetrics 已接入",
                userStore.size());
    }

    /**
     * 执行登录认证
     *
     * @return Optional<LoginResponse>  空 => 认证失败，详见 reason
     */
    public AuthResult authenticate(String username, String password, String clientIp) {
        // ------- 基本参数校验 -------
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            metrics.recordFailedLogin("unknown", username, "not_found", clientIp);
            return AuthResult.fail("用户名或密码不能为空", 400);
        }
        username = username.trim();

        // ------- 用户查询 -------
        DemoUser user = userStore.get(username);
        if (user == null) {
            metrics.recordFailedLogin("unknown", username, "not_found", clientIp);
            log.warn("[Auth] 登录失败：用户不存在 user={} ip={}", username, clientIp);
            return AuthResult.fail("用户名或密码错误", 401);
        }

        if (user.isLocked()) {
            metrics.recordFailedLogin(user.getRole(), username, "locked", clientIp);
            log.warn("[Auth] 登录失败：账号已锁定 user={} ip={}", username, clientIp);
            return AuthResult.fail("账号已锁定", 403);
        }

        // ------- 密码校验 -------
        if (!password.equals(user.getPassword())) {
            metrics.recordFailedLogin(user.getRole(), username, "wrong_password", clientIp);
            log.warn("[Auth] 登录失败：密码错误 user={} ip={}", username, clientIp);
            return AuthResult.fail("用户名或密码错误", 401);
        }

        // ------- 登录成功 -------
        String token = generateToken(username);
        long expiresAt = System.currentTimeMillis() + 3600 * 1000L;  // 1 小时
        tokenStore.put(token, new TokenInfo(username, user.getRole(), expiresAt));

        // 按角色区分调用 BusinessMetrics
        if ("admin".equalsIgnoreCase(user.getRole())) {
            metrics.recordAdminLogin(username, clientIp);
        } else {
            metrics.recordUserLogin(username, clientIp, user.getRole());
        }

        log.info("[Auth] 登录成功 user={} role={} ip={}", username, user.getRole(), clientIp);
        return AuthResult.success(new LoginResponse(token, username, user.getRole(), expiresAt));
    }

    /** Token 有效性校验（用于活跃用户追踪） */
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) return false;
        TokenInfo info = tokenStore.get(token);
        if (info == null) return false;
        return info.expiresAt > System.currentTimeMillis();
    }

    /** 获取当前活跃 token 数量（用于活跃用户统计） */
    public int getActiveTokenCount() {
        long now = System.currentTimeMillis();
        int active = 0;
        for (TokenInfo info : tokenStore.values()) {
            if (info.expiresAt > now) active++;
        }
        return active;
    }

    /** 获取当前所有 username 集合（用于去重的活跃用户数） */
    public Set<String> getActiveUsernames() {
        long now = System.currentTimeMillis();
        Set<String> users = new HashSet<>();
        for (TokenInfo info : tokenStore.values()) {
            if (info.expiresAt > now) users.add(info.username);
        }
        return users;
    }

    private String generateToken(String username) {
        byte[] random = new byte[16];
        ThreadLocalRandom.current().nextBytes(random);
        StringBuilder sb = new StringBuilder();
        sb.append("tk_").append(username).append("_");
        for (byte b : random) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // ============================================================
    //  结果封装
    // ============================================================

    public static class AuthResult {
        public final boolean success;
        public final LoginResponse data;
        public final String message;
        public final int code;

        private AuthResult(boolean success, LoginResponse data, String message, int code) {
            this.success = success;
            this.data = data;
            this.message = message;
            this.code = code;
        }

        public static AuthResult success(LoginResponse data) {
            return new AuthResult(true, data, "success", 200);
        }
        public static AuthResult fail(String message, int code) {
            return new AuthResult(false, null, message, code);
        }
    }

    static class TokenInfo {
        String username;
        String role;
        long expiresAt;
        TokenInfo(String username, String role, long expiresAt) {
            this.username = username;
            this.role = role;
            this.expiresAt = expiresAt;
        }
    }
}
