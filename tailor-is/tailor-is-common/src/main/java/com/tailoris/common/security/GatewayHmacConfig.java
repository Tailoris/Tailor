package com.tailoris.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关 HMAC 签名校验自动配置，供下游微服务使用。
 *
 * <p>在任意下游服务的 classpath 上引入 tailor-is-common 模块后，添加以下配置即可启用：</p>
 * <pre>
 * tailoris:
 *   gateway:
 *     hmac:
 *       enabled: true                          # 启用 HMAC 签名校验（默认 true）
 *       include-paths: /api/**                 # 需要校验的路径（默认 /api/**）
 *       exclude-paths: /api/public/**,/actuator/**  # 排除的路径
 * </pre>
 *
 * <p>同时设置环境变量 GATEWAY_HMAC_SECRET（需与网关 CoreAuthGlobalFilter 中的配置一致）。</p>
 *
 * <p>如需手动控制拦截器注册，可将 enabled 设为 false，然后在自定义 WebMvcConfigurer 中
 * 注入 {@link GatewayHmacInterceptor} 并手动注册。</p>
 *
 * @author Tailor IS Team
 * @since 2.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tailoris.gateway.hmac", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GatewayHmacConfig implements WebMvcConfigurer {

    private final GatewayHmacInterceptor gatewayHmacInterceptor;
    private final HmacPathsProperties hmacPathsProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> includePaths = hmacPathsProperties.getIncludePaths();
        List<String> excludePaths = hmacPathsProperties.getExcludePaths();

        String[] includePatterns = includePaths.isEmpty()
                ? new String[]{"/api/**"}
                : includePaths.toArray(new String[0]);

        InterceptorRegistration registration = registry.addInterceptor(gatewayHmacInterceptor)
                .addPathPatterns(includePatterns);

        if (!excludePaths.isEmpty()) {
            registration.excludePathPatterns(excludePaths.toArray(new String[0]));
        }

        log.info("网关 HMAC 签名校验已启用，拦截路径: {}, 排除路径: {}",
                includePaths.isEmpty() ? "[/api/**]" : includePaths,
                excludePaths.isEmpty() ? "[]" : excludePaths);
    }

    /**
     * HMAC 签名校验的路径配置属性。
     */
    @ConfigurationProperties(prefix = "tailoris.gateway.hmac")
    @Component
    public static class HmacPathsProperties {

        /** 需要校验 HMAC 签名的路径列表（Ant 风格） */
        private List<String> includePaths = new ArrayList<>();

        /** 排除的路径列表（Ant 风格） */
        private List<String> excludePaths = new ArrayList<>();

        public List<String> getIncludePaths() {
            return includePaths;
        }

        public void setIncludePaths(List<String> includePaths) {
            this.includePaths = includePaths;
        }

        public List<String> getExcludePaths() {
            return excludePaths;
        }

        public void setExcludePaths(List<String> excludePaths) {
            this.excludePaths = excludePaths;
        }
    }
}