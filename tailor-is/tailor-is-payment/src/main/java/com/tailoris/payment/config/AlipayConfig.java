package com.tailoris.payment.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "alipay")
public class AlipayConfig {

    private String appId;
    
    private String privateKey;
    
    private String publicKey;
    
    private String gatewayUrl = "https://openapi.alipay.com/gateway.do";
    
    private String returnUrl;
    
    private String notifyUrl;
    
    private String signType = "RSA2";
    
    private String charset = "UTF-8";

    @PostConstruct
    public void validate() {
        if (publicKey == null || publicKey.isBlank()) {
            throw new IllegalStateException("alipay.public-key 未配置，服务无法启动");
        }
        if (privateKey == null || privateKey.isBlank()) {
            throw new IllegalStateException("alipay.private-key 未配置，服务无法启动");
        }
        if (appId == null || appId.isBlank()) {
            throw new IllegalStateException("alipay.app-id 未配置，服务无法启动");
        }
        log.info("支付宝配置验证通过: appId={}", appId);
    }
}