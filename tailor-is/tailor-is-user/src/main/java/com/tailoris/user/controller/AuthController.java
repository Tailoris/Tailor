package com.tailoris.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.annotation.RateLimit;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.user.dto.LoginRequest;
import com.tailoris.user.dto.LoginResponse;
import com.tailoris.user.dto.RegisterRequest;
import com.tailoris.user.dto.SendSmsCodeRequest;
import com.tailoris.user.dto.WechatLoginRequest;
import com.tailoris.user.service.SysUserService;
import com.tailoris.user.service.WechatLoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证管理Controller.
 *
 * <p>提供用户登录、注册、登出、Token刷新等认证相关接口。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>用户登录：支持用户名或手机号+密码</li>
 *   <li>用户注册：手机号+密码+短信验证码</li>
 *   <li>Token刷新：避免频繁重新登录</li>
 *   <li>短信验证码：发送注册/找回密码验证码</li>
 * </ul>
 *
 * <p>安全加固：</p>
 * <ul>
 *   <li>B-C05: 登录失败锁定（5次/30分钟）</li>
 *   <li>B-C09: 接口限流防暴力破解</li>
 *   <li>B-H23: CSRF Token校验</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Tag(name = "认证管理", description = "用户登录、注册、登出等认证相关接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    /** Bearer Token 前缀长度常量. */
    private static final int BEARER_PREFIX_LENGTH = 7;

    /** Bearer Token 前缀. */
    private static final String BEARER_PREFIX = "Bearer ";

    private final SysUserService sysUserService;
    private final WechatLoginService wechatLoginService;

    /**
     * 用户登录
     *
     * <p>修复 B-C09: 接入限流，IP级别每分钟最多10次</p>
     */
    @Operation(summary = "用户登录", description = "支持用户名或手机号+密码登录")
    @RateLimit(key = "login", permitsPerSecond = 10, capacity = 60, message = "登录请求过于频繁，请稍后再试")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                       HttpServletRequest httpRequest) {
        LoginResponse response = sysUserService.login(request);
        // 🔒 B-L03修复: 使用HttpRequestUtils工具类提取IP
        String ip = com.tailoris.common.util.HttpRequestUtils.getClientIp(httpRequest);
        sysUserService.updateLoginInfo(StpUtil.getLoginIdAsLong(), ip);
        return Result.success(response);
    }

    @Operation(summary = "用户注册", description = "手机号+密码+短信验证码注册")
    @RateLimit(key = "register", permitsPerSecond = 5, capacity = 60, message = "注册请求过于频繁")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        sysUserService.register(request);
        return Result.success();
    }

    /**
     * 发送短信验证码
     */
    @Operation(summary = "发送短信验证码", description = "发送注册/找回密码所需的短信验证码")
    @RateLimit(key = "sms", permitsPerSecond = 1, capacity = 60, message = "验证码请求过于频繁，请1分钟后再试")
    @PostMapping("/sms-code")
    public Result<Void> sendSmsCode(@Valid @RequestBody SendSmsCodeRequest request) {
        sysUserService.sendSmsCode(request.getPhone());
        return Result.success();
    }

    /**
     * 🔒 USR-003: 微信授权登录.
     *
     * <p>支持两种场景：</p>
     * <ul>
     *   <li>type=MP: 微信公众号 H5（网页授权）</li>
     *   <li>type=MINI: 微信小程序登录</li>
     * </ul>
     *
     * <p>前端流程：</p>
     * <pre>
     * 1. 调用 wx.login() 获取 js_code
     * 2. 将 js_code 传入本接口
     * 3. 后端用 js_code 换 openid，自动注册/登录
     * </pre>
     */
    @Operation(summary = "微信授权登录", description = "微信公众号/小程序授权登录，自动注册并签发Token")
    @RateLimit(key = "wechat-login", permitsPerSecond = 5, capacity = 30, message = "微信登录请求过于频繁")
    @PostMapping("/wechat-login")
    public Result<LoginResponse> wechatLogin(@RequestBody WechatLoginRequest request) {
        LoginResponse response = wechatLoginService.loginByWechat(request);
        return Result.success(response);
    }

    @Operation(summary = "用户登出", description = "退出当前登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            sysUserService.logout(userId);
        }
        return Result.success();
    }

    @Operation(
            summary = "刷新Token",
            description = "使用当前有效的Token换取新的Token，延长登录有效期。"
                    + "Token有效期（timeout）配置为1800秒（30分钟），通过此接口可在Token过期前主动刷新，"
                    + "获取新Token，避免频繁重新登录。刷新成功后旧Token立即失效，请使用返回的新Token替换本地存储。"
                    + "活跃超时（active-timeout）配置为-1表示无限制，不会因不活跃而自动退出。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "刷新成功，返回新的LoginResponse包含新Token和用户信息"),
            @ApiResponse(responseCode = "400", description = "Token无效或已过期（超过30分钟），需重新登录")
    })
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(HttpServletRequest httpRequest) {
        String token = extractToken(httpRequest);
        if (token == null || token.isBlank()) {
            throw new BusinessException("未提供认证Token，请先登录后获取Token");
        }

        Object loginId = StpUtil.getLoginIdByToken(token);
        if (loginId == null) {
            throw new BusinessException("Token无效或已过期（超过30分钟未使用），请重新登录");
        }

        Long userId = Long.parseLong(loginId.toString());
        StpUtil.logoutByTokenValue(token);

        LoginResponse response = sysUserService.refresh(userId);
        return Result.success(response);
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX_LENGTH);
        }
        String token = request.getParameter("token");
        if (token != null && !token.isBlank()) {
            return token;
        }
        return StpUtil.getTokenValue();
    }

    /**
     * 🔒 B-L03修复: getClientIp已提取到HttpRequestUtils工具类
     */
    @Deprecated
    private String getClientIp(HttpServletRequest request) {
        return com.tailoris.common.util.HttpRequestUtils.getClientIp(request);
    }
}
