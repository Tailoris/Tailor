package com.tailoris.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 支付渠道统一配置.
 *
 * <p>集中管理微信支付和支付宝的配置参数，支持从配置中心动态获取。
 * 包含 API 密钥、证书、回调地址等支付渠道核心配置。</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "payment")
public class PaymentConfig {

    /** 微信支付配置 */
    private Wechat wechat = new Wechat();

    /** 支付宝配置 */
    private Alipay alipay = new Alipay();

    @Data
    public static class Wechat {
        /** 微信支付 AppID */
        private String appId;

        /** 微信支付商户号 */
        private String mchId;

        /** API V3 密钥（32位） */
        private String apiV3Key;

        /** 商户私钥（PEM格式，Base64编码） */
        private String privateKey;

        /** 微信支付平台证书序列号 */
        private String certificateSerialNo;

        /** 支付回调通知地址 */
        private String notifyUrl;

        /** 退款回调通知地址 */
        private String refundNotifyUrl;
    }

    @Data
    public static class Alipay {
        /** 支付宝 AppID */
        private String appId;

        /** 应用私钥（RSA2） */
        private String privateKey;

        /** 支付宝公钥 */
        private String alipayPublicKey;

        /** 支付回调通知地址 */
        private String notifyUrl;

        /** 页面跳转同步通知地址 */
        private String returnUrl;
    }
}