package com.tailoris.user.service;

import com.tailoris.user.dto.LoginResponse;
import com.tailoris.user.dto.WechatLoginRequest;

/**
 * 微信登录服务接口 - USR-003.
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
public interface WechatLoginService {

    /**
     * 微信授权登录.
     *
     * <p>完整流程：</p>
     * <ol>
     *   <li>用 code 调微信接口换 access_token + openid</li>
     *   <li>查询/创建本地用户（按 openid 关联）</li>
     *   <li>Sa-Token 登录签发 JWT</li>
     *   <li>缓存预热 + 审计日志</li>
     * </ol>
     *
     * @param request 微信登录请求
     * @return 登录响应（含 token + 用户信息）
     */
    LoginResponse loginByWechat(WechatLoginRequest request);
}
