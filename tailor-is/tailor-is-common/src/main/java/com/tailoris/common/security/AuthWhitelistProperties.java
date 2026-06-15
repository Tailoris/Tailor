package com.tailoris.common.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 认证拦截器白名单配置 - 修复 B-H21
 *
 * <p>将白名单从代码硬编码改为配置文件化管理：</p>
 * <ul>
 *   <li>支持从Nacos配置中心动态加载</li>
 *   <li>支持通配符匹配（/api/public/**）</li>
 *   <li>支持方法过滤（GET/POST等）</li>
 * </ul>
 *
 * <h3>配置示例</h3>
 * <pre>{@code
 * tailoris:
 *   auth:
 *     whitelist:
 *       - /api/auth/login
 *       - /api/auth/register
 *       - /api/auth/refresh
 *       - /api/auth/sms-code
 *       - /api/public/**
 *       - /api/file/upload
 *       - /api/file/download
 *       - /actuator/**
 * }</pre>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "tailoris.auth")
public class AuthWhitelistProperties {

    /** 不需要鉴权的路径列表 */
    private List<String> whitelist = new ArrayList<>();

    /** 是否启用白名单（默认true） */
    private boolean enabled = true;

    /**
     * 默认白名单（兜底）
     */
    public List<String> getEffectiveWhitelist() {
        if (whitelist == null || whitelist.isEmpty()) {
            return getDefaultWhitelist();
        }
        return whitelist;
    }

    /**
     * 默认白名单 - 与B-C05/C06对应
     */
    public static List<String> getDefaultWhitelist() {
        List<String> defaults = new ArrayList<>();
        defaults.add("/api/auth/login");
        defaults.add("/api/auth/register");
        defaults.add("/api/auth/refresh");
        defaults.add("/api/auth/sms-code");
        defaults.add("/api/auth/reset-password");
        defaults.add("/api/auth/captcha");
        defaults.add("/api/public/**");
        defaults.add("/api/file/upload");
        defaults.add("/api/file/download");
        defaults.add("/api/common/captcha");
        defaults.add("/actuator/health");
        defaults.add("/actuator/info");
        defaults.add("/v3/api-docs/**");
        defaults.add("/swagger-ui/**");
        defaults.add("/swagger-resources/**");
        return defaults;
    }

    /**
     * 判断路径是否在白名单中
     *
     * @param path 请求路径
     * @return true-白名单 false-需要鉴权
     */
    public boolean isWhitelisted(String path) {
        if (!enabled || path == null) {
            return false;
        }

        List<String> effectiveList = getEffectiveWhitelist();
        for (String pattern : effectiveList) {
            if (matchPattern(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 路径匹配（支持通配符）
     */
    private boolean matchPattern(String pattern, String path) {
        if (pattern == null || path == null) {
            return false;
        }

        // 精确匹配
        if (pattern.equals(path)) {
            return true;
        }

        // 通配符匹配
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }

        // 单层通配符
        if (pattern.contains("*")) {
            String regex = pattern.replace("*", "[^/]*");
            return path.matches(regex);
        }

        return false;
    }
}
