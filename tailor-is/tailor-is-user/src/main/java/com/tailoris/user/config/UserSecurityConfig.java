package com.tailoris.user.config;

import org.springframework.context.annotation.Configuration;

/**
 * 用户服务安全配置
 *
 * <p>BCryptPasswordEncoder 已统一到 tailor-is-common 模块的 SecurityConfig，
 * 本服务通过组件扫描自动注入，无需重复定义。</p>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
@Configuration
public class UserSecurityConfig {
}
