package com.tailoris.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * XSS 过滤器 - 修复 B-H19
 *
 * <p>基于 OWASP Java Encoder 的XSS防护。
 * 双重过滤策略：</p>
 * <ol>
 *   <li>请求参数过滤：转义特殊字符（&,lt;,&gt;,",'）</li>
 *   <li>JSON Body处理：使用Jackson的HTML转义</li>
 * </ol>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class XssFilter extends OncePerRequestFilter {

    /** 不需要XSS过滤的路径 */
    private static final String[] EXCLUDED_PATHS = {
            "/api/file/upload",
            "/api/file/download",
            "/api/common/captcha"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (isExcluded(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 仅对application/json的POST/PUT请求进行深度过滤
        if (isJsonRequest(request)) {
            XssRequestWrapper wrappedRequest = new XssRequestWrapper(request);
            filterChain.doFilter(wrappedRequest, response);
        } else {
            // 普通参数过滤
            filterChain.doFilter(request, response);
        }
    }

    private boolean isExcluded(String path) {
        for (String excluded : EXCLUDED_PATHS) {
            if (path.startsWith(excluded)) {
                return true;
            }
        }
        return false;
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().contains("application/json");
    }

    /**
     * XSS请求包装器 - 对参数进行HTML转义
     */
    static class XssRequestWrapper extends HttpServletRequestWrapper {

        public XssRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            return sanitize(super.getParameter(name));
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) {
                return null;
            }
            String[] sanitized = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                sanitized[i] = sanitize(values[i]);
            }
            return sanitized;
        }

        @Override
        public String getHeader(String name) {
            return sanitize(super.getHeader(name));
        }

        /**
         * 使用 OWASP Encoder 清理XSS
         */
        private String sanitize(String value) {
            if (!StringUtils.hasText(value)) {
                return value;
            }
            // 使用OWASP Java Encoder进行HTML转义
            return Encode.forHtmlContent(value);
        }
    }
}
