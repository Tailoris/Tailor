package com.tailoris.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
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
}