package com.tailoris.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * CORS 配置 - 修复 B-H29
 *
 * <p>替代原WebMvcConfig中宽松的CORS配置，改为白名单模式：</p>
 * <ul>
 *   <li>仅允许配置的域名跨域访问</li>
 *   <li>支持环境变量配置允许的源</li>
 *   <li>不暴露敏感头</li>
 *   <li>不启用通配符凭据</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
@Configuration
public class CorsConfig {

    /** 默认允许的跨域源（生产环境应从配置中心读取） */
    private static final List<String> DEFAULT_ALLOWED_ORIGINS = Arrays.asList(
            "https://www.tailor-is.com",
            "https://admin.tailor-is.com",
            "https://merchant.tailor-is.com",
            "https://m.tailor-is.com"
    );

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // 🔒 B-H29: 明确白名单，不使用 *
        List<String> allowedOrigins = getAllowedOrigins();
        config.setAllowedOrigins(allowedOrigins);

        // 允许的HTTP方法
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 允许的请求头
        config.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "X-Requested-With",
                "X-CSRF-Token", "X-Request-Id", "Accept", "Origin"
        ));

        // 暴露的响应头
        config.setExposedHeaders(Arrays.asList(
                "X-Total-Count", "X-RateLimit-Limit", "X-RateLimit-Remaining"
        ));

        // 不允许携带凭据（除非明确配置）
        config.setAllowCredentials(false);

        // 预检请求缓存时间
        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    /**
     * 获取允许的源（从环境变量覆盖）
     */
    private List<String> getAllowedOrigins() {
        String envOrigins = System.getenv("ALLOWED_ORIGINS");
        if (envOrigins != null && !envOrigins.isEmpty()) {
            return Arrays.asList(envOrigins.split(","));
        }
        return DEFAULT_ALLOWED_ORIGINS;
    }
}
