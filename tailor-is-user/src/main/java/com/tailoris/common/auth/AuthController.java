package com.tailoris.common.auth;

import com.tailoris.common.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 登录认证接口
 *
 *  POST /api/auth/login      - 登录 (需 application/json)
 *  POST /api/auth/logout     - 登出 (需 application/json)
 *  GET  /api/auth/health     - 健康检查
 *
 *  说明：
 *   - 生产环境通过反向代理 (Nginx/1Panel Openresty) 转发，真实客户端 IP 由
 *     X-Forwarded-For / X-Real-IP 头携带；本控制器按优先级解析并用该 IP 写入
 *     登录事件/告警，避免仅记录到反向代理的内网 IP。
 *   - 对认证/登录相关的 POST 接口强制 Content-Type=application/json，避免通过
 *     application/x-www-form-urlencoded 等提交绕过请求体结构校验。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationService authService;

    public AuthController(AuthenticationService authService) {
        this.authService = authService;
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<LoginResponse> login(
            @RequestBody LoginRequest req,
            HttpServletRequest httpRequest) {
        String clientIp = resolveIp(httpRequest);
        log.info("[Auth] login user={} from={} contentType={}",
                req == null ? null : req.getUsername(),
                clientIp,
                httpRequest.getContentType());
        AuthenticationService.AuthResult result = authService.authenticate(
                req.getUsername(), req.getPassword(), clientIp);
        if (result.success) {
            return ApiResponse.ok(result.data);
        }
        return ApiResponse.error(result.code, result.message);
    }

    @PostMapping(value = "/logout", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String token,
            HttpServletRequest httpRequest) {
        log.info("[Auth] 用户登出 ip={} token_prefix={}",
                resolveIp(httpRequest),
                token == null ? "null" : token.substring(0, Math.min(10, token.length())));
        return ApiResponse.ok();
    }

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.ok("auth-service:ok");
    }

    /**
     * 解析真实客户端 IP。
     * 优先级：X-Forwarded-For (第一个非空的逗号分隔项) -> X-Real-IP -> RemoteAddr
     */
    private String resolveIp(HttpServletRequest req) {
        try {
            String xff = req.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isEmpty()) {
                int comma = xff.indexOf(',');
                String ip = (comma > 0 ? xff.substring(0, comma) : xff).trim();
                if (!ip.isEmpty()) return ip;
            }
            String realIp = req.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isEmpty()) return realIp.trim();
            return req.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
