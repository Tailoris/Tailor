package com.tailoris.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 安全配置 - M-003
 *
 * <p>将 BCryptPasswordEncoder 统一在此模块定义，
 * 各业务服务无需重复定义，通过组件扫描自动注入。</p>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
@Configuration
public class SecurityConfig {

    /**
     * 密码编码器
     * 强度 12 位，约 250ms/次，有效抵抗暴力破解
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
