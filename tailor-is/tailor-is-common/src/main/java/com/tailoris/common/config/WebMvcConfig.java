package com.tailoris.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

/**
 * Spring MVC 全局配置.
 *
 * <p>🔒 B-L11修复: 配置静态资源缓存策略（CSS/JS/图片等静态资源启用浏览器长缓存，
 *    提升前端访问性能并降低网关带宽压力）。</p>
 *
 * <p>策略说明：</p>
 * <ul>
 *   <li>生产静态资源（/static/**）：缓存 30 天，公共缓存（CDN 可缓存）</li>
 *   <li>Bootstrap CSS/JS：缓存 7 天</li>
 *   <li>WebJars 资源：缓存 30 天</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /** 静态资源默认缓存时间：30 天 */
    private static final int STATIC_RESOURCE_CACHE_DAYS = 30;

    /** 第三方库资源缓存时间：7 天 */
    private static final int WEBJARS_CACHE_DAYS = 7;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. 业务静态资源（图片、字体等）
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(STATIC_RESOURCE_CACHE_DAYS, TimeUnit.DAYS)
                        .cachePublic());

        // 2. WebJars 资源（bootstrap、jquery 等）
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .setCacheControl(CacheControl.maxAge(WEBJARS_CACHE_DAYS, TimeUnit.DAYS)
                        .cachePublic());

        // 3. Swagger UI 等工具资源
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}
