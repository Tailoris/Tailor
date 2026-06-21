package com.tailoris.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信登录请求 - USR-003.
 *
 * <p>支持两种场景：</p>
 * <ul>
 *   <li>公众号 H5：传入 code，服务器用 code 换取 openid/unionid/access_token</li>
 *   <li>小程序：传入 code + iv + encryptedData，解密手机号/用户信息</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Data
public class WechatLoginRequest {

    /** 登录类型: MP=公众号 H5, MINI=小程序, APP=移动 App */
    private String type = "MP";

    /** 微信服务器返回的 code（必填） */
    @NotBlank(message = "微信授权code不能为空")
    private String code;

    /** 加密算法的初始向量（小程序专用） */
    private String iv;

    /** 加密数据（小程序专用） */
    private String encryptedData;

    /** 加密签名（小程序专用） */
    private String signature;

    /** 设备ID（用于绑定） */
    private String deviceId;

    /** 推荐人 userId（可选） */
    private Long referrerId;
}
