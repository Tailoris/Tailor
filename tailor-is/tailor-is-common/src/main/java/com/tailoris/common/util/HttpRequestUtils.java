package com.tailoris.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * HTTP请求工具类 - 修复 B-L03.
 *
 * <p>提供获取客户端IP、User-Agent等通用HTTP工具方法。</p>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
public final class HttpRequestUtils {

    /** X-Forwarded-For header name. */
    public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

    /** X-Real-IP header name. */
    public static final String HEADER_X_REAL_IP = "X-Real-IP";

    /** Proxy-Client-IP header name. */
    public static final String HEADER_PROXY_CLIENT_IP = "Proxy-Client-IP";

    /** WL-Proxy-Client-IP header name. */
    public static final String HEADER_WL_PROXY_CLIENT_IP = "WL-Proxy-Client-IP";

    /** HTTP_CLIENT_IP header name. */
    public static final String HEADER_HTTP_CLIENT_IP = "HTTP_CLIENT_IP";

    /** HTTP_X_FORWARDED_FOR header name. */
    public static final String HEADER_HTTP_X_FORWARDED_FOR = "HTTP_X_FORWARDED_FOR";

    /** Unknown IP. */
    public static final String UNKNOWN = "unknown";

    private HttpRequestUtils() {
        throw new IllegalStateException("工具类不允许实例化");
    }

    /**
     * 获取客户端真实IP地址.
     *
     * <p>按以下顺序尝试获取（支持反向代理场景）：</p>
     * <ol>
     *   <li>X-Forwarded-For（最常用）</li>
     *   <li>X-Real-IP（Nginx代理）</li>
     *   <li>Proxy-Client-IP（Apache代理）</li>
     *   <li>WL-Proxy-Client-IP（WebLogic代理）</li>
     *   <li>HTTP_CLIENT_IP</li>
     *   <li>HTTP_X_FORWARDED_FOR</li>
     *   <li>request.getRemoteAddr()（兜底）</li>
     * </ol>
     *
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        String ip = getIpFromHeader(request, HEADER_X_FORWARDED_FOR);
        if (!isValidIp(ip)) {
            ip = getIpFromHeader(request, HEADER_X_REAL_IP);
        }
        if (!isValidIp(ip)) {
            ip = getIpFromHeader(request, HEADER_PROXY_CLIENT_IP);
        }
        if (!isValidIp(ip)) {
            ip = getIpFromHeader(request, HEADER_WL_PROXY_CLIENT_IP);
        }
        if (!isValidIp(ip)) {
            ip = getIpFromHeader(request, HEADER_HTTP_CLIENT_IP);
        }
        if (!isValidIp(ip)) {
            ip = getIpFromHeader(request, HEADER_HTTP_X_FORWARDED_FOR);
        }
        if (!isValidIp(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个IP
        if (StringUtils.hasText(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip == null || ip.isEmpty() ? UNKNOWN : ip;
    }

    private static String getIpFromHeader(HttpServletRequest request, String headerName) {
        return request.getHeader(headerName);
    }

    private static boolean isValidIp(String ip) {
        return StringUtils.hasText(ip) && !UNKNOWN.equalsIgnoreCase(ip);
    }

    /**
     * 获取User-Agent.
     */
    public static String getUserAgent(HttpServletRequest request) {
        return request == null ? UNKNOWN : request.getHeader("User-Agent");
    }

    /**
     * 获取请求URI.
     */
    public static String getRequestUri(HttpServletRequest request) {
        return request == null ? "" : request.getRequestURI();
    }

    /**
     * 获取请求方法.
     */
    public static String getMethod(HttpServletRequest request) {
        return request == null ? "" : request.getMethod();
    }
}
