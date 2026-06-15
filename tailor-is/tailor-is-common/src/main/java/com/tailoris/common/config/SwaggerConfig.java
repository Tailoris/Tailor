package com.tailoris.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 配置.
 *
 * <p>🔒 B-L12修复: 生产环境通过配置 {@code tailoris.swagger.enabled=false} 自动禁用，
 *    避免接口文档泄露到外网。开发/测试环境默认开启。</p>
 *
 * <p>配置示例：</p>
 * <pre>
 * # application.yml
 * tailoris:
 *   swagger:
 *     enabled: true   # 开发环境
 *     # enabled: false  # 生产环境
 * </pre>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "tailoris.swagger", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SwaggerConfig {

    @Bean
    public OpenAPI tailorIsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tailor IS API Documentation")
                        .description("裁智云服装全产业平台API文档")
                        .version("v1.0.0")
                        .license(new License()
                                .name("Proprietary")
                                .url("https://tailoris.com")));
    }
}