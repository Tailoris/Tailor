package com.tailoris.payment.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "wechat.pay")
public class WechatPayConfig {

    private String appId;

    private String mchId;

    private String apiKey;

    private String apiV3Key;

    private String certificateSerialNo;

    private String privateKey;

    private String notifyUrl;

    private String tradeType;

    private String platformPublicKey;

    private String signType = "HMAC-SHA256";

    private String unifiedOrderUrl = "https://api.mch.weixin.qq.com/pay/unifiedorder";

    private String orderQueryUrl = "https://api.mch.weixin.qq.com/pay/orderquery";

    private String refundUrl = "https://api.mch.weixin.qq.com/secapi/pay/refund";

    @PostConstruct
    public void validate() {
        if (appId == null || appId.isBlank()) {
            throw new IllegalStateException("wechat.pay.app-id 未配置，服务无法启动");
        }
        if (mchId == null || mchId.isBlank()) {
            throw new IllegalStateException("wechat.pay.mch-id 未配置，服务无法启动");
        }
        if (privateKey == null || privateKey.isBlank()) {
            throw new IllegalStateException("wechat.pay.private-key 未配置，服务无法启动");
        }
        if (apiV3Key == null || apiV3Key.isBlank()) {
            throw new IllegalStateException("wechat.pay.api-v3-key 未配置，服务无法启动");
        }
        if (platformPublicKey == null || platformPublicKey.isBlank()) {
            throw new IllegalStateException("wechat.pay.platform-public-key 未配置，服务无法启动");
        }
        log.info("微信支付配置验证通过: appId={}, mchId={}", appId, mchId);
    }
}