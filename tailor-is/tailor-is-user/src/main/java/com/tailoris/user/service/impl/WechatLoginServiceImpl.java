package com.tailoris.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tailoris.common.constant.RedisKeyPrefix;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.AuditLogUtils;
import com.tailoris.common.util.HttpRequestUtils;
import com.tailoris.common.util.LogMaskUtils;
import com.tailoris.user.config.WechatProperties;
import com.tailoris.user.dto.LoginResponse;
import com.tailoris.user.dto.WechatLoginRequest;
import com.tailoris.user.entity.SysUser;
import com.tailoris.user.entity.SysRole;
import com.tailoris.user.entity.SysPermission;
import com.tailoris.user.mapper.SysUserMapper;
import com.tailoris.user.service.SysRoleService;
import com.tailoris.user.service.SysPermissionService;
import com.tailoris.user.service.WechatLoginService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 微信登录服务实现 - USR-003.
 *
 * <p>支持公众号 H5、小程序两种场景。完整 OAuth2.0 流程：</p>
 * <ol>
 *   <li>前端调 wx.login() 获取 js_code</li>
 *   <li>后端用 js_code + appid/appsecret 调微信接口换 openid</li>
 *   <li>根据 openid 查/建本地用户</li>
 *   <li>Sa-Token 签发 token</li>
 * </ol>
 *
 * <h3>安全要点</h3>
 * <ul>
 *   <li>appsecret 仅在后端使用，绝不暴露给前端</li>
 *   <li>access_token 缓存到 Redis，TTL < 微信官方2小时</li>
 *   <li>openid 全局唯一，作为用户唯一标识</li>
 *   <li>绑定手机号需额外短信验证</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatLoginServiceImpl implements WechatLoginService {

    /** 微信授权码换 access_token 接口 */
    private static final String WECHAT_CODE2TOKEN_URL =
            "https://api.weixin.qq.com/sns/oauth2/access_token"
                    + "?appid={appId}&secret={secret}&code={code}&grant_type=authorization_code";

    /** 微信小程序登录接口 */
    private static final String WECHAT_MINI_LOGIN_URL =
            "https://api.weixin.qq.com/sns/jscode2session"
                    + "?appid={appId}&secret={secret}&js_code={code}&grant_type=authorization_code";

    /** Redis 中 access_token 的 key 前缀 */
    private static final String WECHAT_TOKEN_KEY = "wechat:token:";
    /** access_token 缓存 TTL：微信官方7200秒，提前5分钟过期 */
    private static final Duration WECHAT_TOKEN_TTL = Duration.ofSeconds(7100);

    private final SysUserMapper sysUserMapper;
    private final WechatProperties wechatProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final SysRoleService sysRoleService;
    private final SysPermissionService sysPermissionService;
    private final RestClient restClient = RestClient.builder().build();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse loginByWechat(WechatLoginRequest request) {
        if (request == null || !StringUtils.hasText(request.getCode())) {
            throw new BusinessException("微信授权码不能为空");
        }

        // 1. 用 code 换取 openid / access_token
        WechatTokenResponse tokenResp = exchangeCodeForToken(request);
        if (tokenResp == null || !StringUtils.hasText(tokenResp.getOpenid())) {
            log.warn("微信授权失败: code={}, errcode={}, errmsg={}",
                    LogMaskUtils.maskString(request.getCode()),
                    tokenResp == null ? "null" : tokenResp.getErrcode(),
                    tokenResp == null ? "null" : tokenResp.getErrmsg());
            throw new BusinessException("微信授权失败：" + (tokenResp == null ? "服务异常" : tokenResp.getErrmsg()));
        }

        String openid = tokenResp.getOpenid();
        String unionid = tokenResp.getUnionid();

        // 2. 根据 openid 查/建用户
        SysUser user = findOrCreateWechatUser(openid, unionid);

        // 3. 校验用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }

        // 4. Sa-Token 登录
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        // 5. 缓存 access_token（用于后续主动调用微信接口）
        cacheWechatToken(openid, tokenResp);

        // 6. 审计日志
        AuditLogUtils.login(String.valueOf(user.getId()), user.getUsername(), true, "wechat-oauth");

        log.info("微信登录成功: userId={}, openid={}", user.getId(), LogMaskUtils.maskString(openid));

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserInfo(buildUserInfo(user));
        response.setIsNewUser(user.getLastLoginTime() == null);
        return response;
    }

    /**
     * 用 code 换 access_token.
     */
    private WechatTokenResponse exchangeCodeForToken(WechatLoginRequest request) {
        String appId;
        String appSecret;
        String url;

        if ("MINI".equalsIgnoreCase(request.getType())) {
            // 小程序登录
            appId = wechatProperties.getMini().getAppId();
            appSecret = wechatProperties.getMini().getAppSecret();
            url = WECHAT_MINI_LOGIN_URL;
        } else {
            // 默认公众号 H5
            appId = wechatProperties.getMp().getAppId();
            appSecret = wechatProperties.getMp().getAppSecret();
            url = WECHAT_CODE2TOKEN_URL;
        }

        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
            log.error("微信配置缺失: type={}, appId={}, appSecret={}",
                    request.getType(),
                    LogMaskUtils.maskString(appId),
                    StringUtils.hasText(appSecret) ? "***" : "null");
            throw new BusinessException("微信登录未配置，请联系管理员");
        }

        try {
            return restClient.get()
                    .uri(url, appId, appSecret, request.getCode())
                    .retrieve()
                    .onStatus(status -> status.isError(), (req, res) -> {
                        log.error("微信接口调用失败: status={}", res.getStatusCode());
                    })
                    .body(WechatTokenResponse.class);
        } catch (Exception e) {
            log.error("调用微信接口异常", e);
            return null;
        }
    }

    /**
     * 根据 openid 查/建本地用户.
     */
    private SysUser findOrCreateWechatUser(String openid, String unionid) {
        // 1. 先按 openid 查
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, "wx_" + openid);
        SysUser user = sysUserMapper.selectOne(wrapper);

        if (user != null) {
            return user;
        }

        // 2. 不存在则创建（极简模式：仅绑定 openid）
        user = new SysUser();
        user.setUsername("wx_" + openid);
        user.setNickName("微信用户_" + openid.substring(0, Math.min(6, openid.length())));
        user.setStatus(0);
        // 头像/手机号由后续绑定流程补充
        sysUserMapper.insert(user);
        log.info("微信新用户创建: userId={}, openid={}", user.getId(), LogMaskUtils.maskString(openid));
        return user;
    }

    /**
     * 缓存微信 access_token（避免每次重新获取）.
     */
    private void cacheWechatToken(String openid, WechatTokenResponse tokenResp) {
        if (tokenResp == null || !StringUtils.hasText(tokenResp.getAccessToken())) {
            return;
        }
        String key = WECHAT_TOKEN_KEY + openid;
        stringRedisTemplate.opsForValue().set(key, tokenResp.getAccessToken(),
                WECHAT_TOKEN_TTL.toSeconds(), TimeUnit.SECONDS);
        if (StringUtils.hasText(tokenResp.getRefreshToken())) {
            stringRedisTemplate.opsForValue().set(key + ":refresh", tokenResp.getRefreshToken(),
                    30L * 24 * 3600, TimeUnit.SECONDS);  // refresh_token 30天
        }
    }

    private LoginResponse.UserInfo buildUserInfo(SysUser user) {
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setPhone(user.getPhone());
        userInfo.setEmail(user.getEmail());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setNickName(user.getNickName());
        userInfo.setRealName(user.getRealName());
        userInfo.setGender(user.getGender());
        userInfo.setStatus(user.getStatus());

        // 角色与权限
        try {
            userInfo.setRoles(sysRoleService.listRolesByUserId(user.getId()).stream()
                    .map(SysRole::getRoleCode).collect(java.util.stream.Collectors.toList()));
            userInfo.setPermissions(sysPermissionService.getPermissionsByUserId(user.getId()).stream()
                    .map(SysPermission::getPermissionCode).collect(java.util.stream.Collectors.toList()));
        } catch (Exception e) {
            log.warn("加载用户角色权限失败: userId={}", user.getId(), e);
        }
        return userInfo;
    }

    /**
     * 微信接口响应.
     */
    @lombok.Data
    public static class WechatTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;
        @JsonProperty("expires_in")
        private Integer expiresIn;
        @JsonProperty("refresh_token")
        private String refreshToken;
        @JsonProperty("openid")
        private String openid;
        @JsonProperty("unionid")
        private String unionid;
        @JsonProperty("scope")
        private String scope;
        @JsonProperty("session_key")
        private String sessionKey;  // 小程序专用
        @JsonProperty("errcode")
        private Integer errcode;
        @JsonProperty("errmsg")
        private String errmsg;
    }
}
