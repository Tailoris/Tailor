package com.tailoris.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 微信开放平台配置 - USR-003.
 *
 * <p>从 application.yml / Nacos 配置中心读取：</p>
 * <pre>
 * tailoris:
 *   wechat:
 *     mp:
 *       app-id: wx...
 *       app-secret: ...
 *     mini:
 *       app-id: wx...
 *       app-secret: ...
 * </pre>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "tailoris.wechat")
public class WechatProperties {

    /** 微信公众号配置 */
    private Mp mp = new Mp();

    /** 微信小程序配置 */
    private Mini mini = new Mini();

    /** 是否启用沙箱环境 */
    private boolean sandbox = false;

    @Data
    public static class Mp {
        private String appId;
        private String appSecret;
    }

    @Data
    public static class Mini {
        private String appId;
        private String appSecret;
    }
}
