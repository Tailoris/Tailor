package com.tailoris.common.config;

import com.tailoris.common.interceptor.IdempotentInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 幂等性拦截器配置 - L4 幂等性机制
 *
 * <p>注册 IdempotentInterceptor，拦截所有 /api/** 请求。
 * 仅对标注了 @Idempotent 注解的方法生效。</p>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Configuration
public class IdempotentConfig implements WebMvcConfigurer {

    private final IdempotentInterceptor idempotentInterceptor;

    public IdempotentConfig(IdempotentInterceptor idempotentInterceptor) {
        this.idempotentInterceptor = idempotentInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(idempotentInterceptor)
                .addPathPatterns("/api/**");
    }
}
