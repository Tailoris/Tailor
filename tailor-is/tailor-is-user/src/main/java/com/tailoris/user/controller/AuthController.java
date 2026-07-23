package com.tailoris.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.annotation.RateLimit;
import com.tailoris.common.exception.AccountErrorException;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.constant.CommonConstants;
import com.tailoris.common.result.Result;
import com.tailoris.common.security.CsrfTokenFilter;
import com.tailoris.user.dto.LoginRequest;
import com.tailoris.user.dto.LoginByCodeRequest;
import com.tailoris.user.dto.EmailCodeRequest;
import com.tailoris.user.dto.EmailRegisterRequest;
import com.tailoris.user.dto.ResetPasswordRequest;
import com.tailoris.user.dto.LoginResponse;
import com.tailoris.user.dto.RegisterRequest;
import com.tailoris.user.dto.SendSmsCodeRequest;
import com.tailoris.user.dto.WechatLoginRequest;
import com.tailoris.user.entity.SysUser;
import com.tailoris.user.mapper.SysUserMapper;
import com.tailoris.user.service.MailService;
import com.tailoris.user.service.SysUserService;
import com.tailoris.user.service.WechatLoginService;
import com.tailoris.common.annotation.ApiVersion;
import com.tailoris.common.constant.RoutePortConstant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@ApiVersion("1.0")
@Tag(name = "认证管理", description = "用户登录、注册、登出等认证相关接口")
@RestController
@RequestMapping(RoutePortConstant.ROUTE_AUTH)
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private static final int BEARER_PREFIX_LENGTH = 7;
    private static final String BEARER_PREFIX = "Bearer ";
    /**
     * ThreadLocal 包装的 SecureRandom，避免多线程争用静态字段导致的性能瓶颈.
     * 每个线程独立持有 SecureRandom 实例，消除 synchronized 锁竞争.
     */
    private static final ThreadLocal<SecureRandom> SECURE_RANDOM =
            ThreadLocal.withInitial(SecureRandom::new);

    private final SysUserService sysUserService;
    private final WechatLoginService wechatLoginService;
    private final CsrfTokenFilter csrfTokenFilter;
    private final StringRedisTemplate stringRedisTemplate;
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Operation(summary = "用户登录", description = "支持用户名或手机号+密码登录")
    @RateLimit(key = "login", permitsPerSecond = 10, capacity = 60, message = "登录请求过于频繁，请稍后再试")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                       HttpServletRequest httpRequest) {
        LoginResponse response = sysUserService.login(request);
        String ip = com.tailoris.common.util.HttpRequestUtils.getClientIp(httpRequest);
        sysUserService.updateLoginInfo(StpUtil.getLoginIdAsLong(), ip);

        String csrfToken = csrfTokenFilter.generateToken();
        StpUtil.getSession().set("_csrf_token", csrfToken);
        response.setCsrfToken(csrfToken);

        return Result.success(response);
    }

    @Operation(summary = "验证码登录", description = "支持手机号或邮箱验证码登录")
    @RateLimit(key = "login-code", permitsPerSecond = 5, capacity = 60, message = "验证码登录请求过于频繁")
    @PostMapping("/login/code")
    public Result<LoginResponse> loginByCode(@Valid @RequestBody LoginByCodeRequest request,
                                             HttpServletRequest httpRequest) {
        String target = request.getTarget();
        String code = request.getCode();
        String type = request.getType();

        if (!StringUtils.hasText(target) || !StringUtils.hasText(code)) {
            throw new BusinessException("请输入账号和验证码");
        }

        SysUser user = null;
        if ("phone".equals(type)) {
            user = sysUserService.getUserByPhone(target);
        } else if ("email".equals(type)) {
            user = sysUserService.getUserByUsername(target);
        }

        if (user == null) {
            throw new AccountErrorException("ACCOUNT_NOT_FOUND", "用户不存在");
        }

        if (user.getStatus() != CommonConstants.STATUS_ENABLED) {
            throw new AccountErrorException("ACCOUNT_DISABLED", "账号已被禁用");
        }

        String cacheKey = "sms:code:" + target;
        if ("email".equals(type)) {
            cacheKey = "email:code:" + target;
        }

        String cachedCode = stringRedisTemplate.opsForValue().get(cacheKey);
        if (!code.equals(cachedCode)) {
            throw new BusinessException("验证码错误或已过期");
        }

        stringRedisTemplate.delete(cacheKey);

        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        String ip = com.tailoris.common.util.HttpRequestUtils.getClientIp(httpRequest);
        sysUserService.updateLoginInfo(user.getId(), ip);

        String csrfToken = csrfTokenFilter.generateToken();
        StpUtil.getSession().set("_csrf_token", csrfToken);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserInfo(sysUserService.getUserInfo(user.getId()));
        response.setCsrfToken(csrfToken);

        return Result.success(response);
    }

    @Operation(summary = "用户注册", description = "手机号+密码+短信验证码注册")
    @RateLimit(key = "register", permitsPerSecond = 5, capacity = 60, message = "注册请求过于频繁")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        sysUserService.register(request);
        return Result.success();
    }

    @Operation(summary = "邮箱注册", description = "邮箱+密码+邮箱验证码注册")
    @RateLimit(key = "register-email", permitsPerSecond = 5, capacity = 60, message = "注册请求过于频繁")
    @PostMapping("/register/email")
    public Result<Void> registerByEmail(@Valid @RequestBody EmailRegisterRequest request) {
        String email = request.getEmail();
        String code = request.getCode();
        String password = request.getPassword();

        if (!StringUtils.hasText(email) || !StringUtils.hasText(code) || !StringUtils.hasText(password)) {
            throw new BusinessException("请填写完整信息");
        }

        String cacheKey = "email:code:" + email;
        String cachedCode = stringRedisTemplate.opsForValue().get(cacheKey);
        if (!code.equals(cachedCode)) {
            throw new BusinessException("验证码错误或已过期");
        }

        stringRedisTemplate.delete(cacheKey);

        if (sysUserService.getUserByUsername(email) != null) {
            throw new BusinessException("邮箱已注册");
        }

        SysUser user = new SysUser();
        user.setUsername(email);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setStatus(1);
        user.setNickName("用户_" + email.substring(0, email.indexOf("@")));
        sysUserMapper.insert(user);

        return Result.success();
    }

    @Operation(summary = "发送短信验证码", description = "发送注册/找回密码所需的短信验证码")
    @RateLimit(key = "sms", permitsPerSecond = 1, capacity = 60, message = "验证码请求过于频繁，请1分钟后再试")
    @PostMapping("/sms-code")
    public Result<Void> sendSmsCode(@Valid @RequestBody SendSmsCodeRequest request) {
        sysUserService.sendSmsCode(request.getPhone());
        return Result.success();
    }

    @Operation(summary = "发送邮箱验证码", description = "发送注册/找回密码所需的邮箱验证码")
    @RateLimit(key = "email", permitsPerSecond = 1, capacity = 60, message = "验证码请求过于频繁，请1分钟后再试")
    @PostMapping("/email-code")
    public Result<Void> sendEmailCode(@Valid @RequestBody EmailCodeRequest request) {
        String email = request.getEmail();
        if (!StringUtils.hasText(email)) {
            throw new BusinessException("请输入邮箱");
        }

        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new BusinessException("邮箱格式不正确");
        }

        String code = String.valueOf(100000 + SECURE_RANDOM.get().nextInt(900000));
        String cacheKey = "email:code:" + email;
        stringRedisTemplate.opsForValue().set(cacheKey, code, 5, TimeUnit.MINUTES);

        try {
            mailService.sendVerificationCode(email, code);
            log.info("邮箱验证码发送成功: email={}", email);
        } catch (Exception e) {
            stringRedisTemplate.delete(cacheKey);
            log.error("邮箱验证码发送失败: email={}, error={}", email, e.getMessage(), e);
            throw new BusinessException("邮件发送失败，请稍后重试");
        }

        return Result.success();
    }

    @Operation(summary = "密码重置", description = "通过验证码重置密码")
    @RateLimit(key = "reset-password", permitsPerSecond = 5, capacity = 60, message = "重置密码请求过于频繁")
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String target = request.getTarget();
        String code = request.getCode();
        String newPassword = request.getNewPassword();
        String type = request.getType();

        if (!StringUtils.hasText(target) || !StringUtils.hasText(code) || !StringUtils.hasText(newPassword)) {
            throw new BusinessException("请填写完整信息");
        }

        String cacheKey = "sms:code:" + target;
        if ("email".equals(type)) {
            cacheKey = "email:code:" + target;
        }

        String cachedCode = stringRedisTemplate.opsForValue().get(cacheKey);
        if (!code.equals(cachedCode)) {
            throw new BusinessException("验证码错误或已过期");
        }

        SysUser user = null;
        if ("phone".equals(type)) {
            user = sysUserService.getUserByPhone(target);
        } else if ("email".equals(type)) {
            user = sysUserService.getUserByUsername(target);
        }

        if (user == null) {
            throw new AccountErrorException("ACCOUNT_NOT_FOUND", "用户不存在");
        }

        if (user.getStatus() != CommonConstants.STATUS_ENABLED) {
            throw new AccountErrorException("ACCOUNT_DISABLED", "账号已被禁用");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);

        stringRedisTemplate.delete(cacheKey);

        log.info("密码重置成功: target={}, userId={}", target, user.getId());
        return Result.success();
    }

    @Operation(summary = "微信授权登录", description = "微信公众号/小程序授权登录，自动注册并签发Token")
    @RateLimit(key = "wechat-login", permitsPerSecond = 5, capacity = 30, message = "微信登录请求过于频繁")
    @PostMapping("/wechat-login")
    public Result<LoginResponse> wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
        LoginResponse response = wechatLoginService.loginByWechat(request);

        String csrfToken = csrfTokenFilter.generateToken();
        StpUtil.getSession().set("_csrf_token", csrfToken);
        response.setCsrfToken(csrfToken);

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

    @Operation(summary = "获取用户信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/userinfo")
    public Result<LoginResponse.UserInfo> getUserInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        LoginResponse.UserInfo userInfo = sysUserService.getUserInfo(userId);
        return Result.success(userInfo);
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
}
