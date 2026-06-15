package com.tailoris.common.auth;

import com.tailoris.common.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 密码重置接口
 *   POST /api/auth/password/reset-request  - 请求重置验证码 (需 application/json)
 *   POST /api/auth/password/reset          - 验证码+新密码 (需 application/json)
 */
@RestController
@RequestMapping("/api/auth/password")
public class PasswordResetController {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetController.class);

    private final PasswordResetService resetService;

    public PasswordResetController(PasswordResetService resetService) {
        this.resetService = resetService;
    }

    @PostMapping(value = "/reset-request", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Map<String, Object>> requestReset(
            @RequestBody Map<String, String> body,
            HttpServletRequest httpRequest) {
        String clientIp = resolveIp(httpRequest);
        String username = body.get("username");
        log.info("[Auth] reset-request user={} from={}", username, clientIp);
        if (username == null || username.trim().isEmpty()) {
            return ApiResponse.error("用户名不能为空");
        }
        String code = resetService.requestReset(username.trim());
        Map<String, Object> data = new HashMap<>();
        data.put("message", "验证码已生成（demo 环境直接返回，生产环境将通过邮件发送）");
        data.put("username", username);
        data.put("code", code);
        data.put("ttl_seconds", 300);
        return ApiResponse.ok(data);
    }

    @PostMapping(value = "/reset", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<String> doReset(
            @RequestBody Map<String, String> body,
            HttpServletRequest httpRequest) {
        String clientIp = resolveIp(httpRequest);
        String username = body.get("username");
        String code = body.get("code");
        String newPassword = body.get("newPassword");
        log.info("[Auth] reset user={} from={}", username, clientIp);
        if (username == null || code == null || newPassword == null) {
            return ApiResponse.error("用户名、验证码或新密码不能为空");
        }
        boolean ok = resetService.verifyAndReset(username.trim(), code.trim(), newPassword);
        return ok ? ApiResponse.ok("密码重置成功") : ApiResponse.error(400, "验证码错误或已过期");
    }

    /** 反向代理链路真实 IP 解析 */
    private String resolveIp(HttpServletRequest req) {
        try {
            String xff = req.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isEmpty()) {
                int comma = xff.indexOf(',');
                return (comma > 0 ? xff.substring(0, comma) : xff).trim();
            }
            String realIp = req.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isEmpty()) return realIp.trim();
            return req.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }
}

