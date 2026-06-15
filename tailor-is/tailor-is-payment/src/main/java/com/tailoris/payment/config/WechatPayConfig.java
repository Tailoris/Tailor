package com.tailoris.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "wechat.pay")
public class WechatPayConfig {

    private String appId;
    
    private String mchId;
    
    private String apiKey;
    
    private String notifyUrl;
    
    private String tradeType;
    
    private String signType = "MD5";
    
    private String unifiedOrderUrl = "https://api.mch.weixin.qq.com/pay/unifiedorder";
    
    private String orderQueryUrl = "https://api.mch.weixin.qq.com/pay/orderquery";
    
    private String refundUrl = "https://api.mch.weixin.qq.com/secapi/pay/refund";
}