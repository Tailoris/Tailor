package com.tailoris.common.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 客户端统一配置.
 *
 * <p>通过 {@link EnableFeignClients} 自动扫描并注册
 * {@code com.tailoris.common.client} 包下的所有 Feign 客户端接口，
 * 避免各业务模块重复声明。</p>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Configuration
@EnableFeignClients(basePackages = "com.tailoris.common.client")
public class FeignConfig {
}
