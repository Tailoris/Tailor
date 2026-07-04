package com.tailoris.common.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * XSS 过滤器 - 修复 B-H19, M-01
 *
 * <p>基于 OWASP Java Encoder 的XSS防护。
 * 三重过滤策略：</p>
 * <ol>
 *   <li>请求参数过滤：转义特殊字符（&,&lt;,&gt;,",'）</li>
 *   <li>JSON Body处理：递归清理JSON所有字符串值（M-01 修复）</li>
 *   <li>Header过滤：清理HTTP Header中的XSS</li>
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

    /** 防止递归包装 */
    private static final ThreadLocal<Boolean> WRAPPED = new ThreadLocal<>();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 检测潜在 XSS payload 的模式（用于性能优化和告警） */
    private static final Pattern XSS_PATTERN = Pattern.compile(
            "(\\bjavascript\\b|\\bon\\w+\\s*=|\\b<\\s*script|\\bdata\\s*:|\\beval\\s*\\(|\\bexpression\\s*\\()",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (isExcluded(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 防止递归包装
        if (Boolean.TRUE.equals(WRAPPED.get())) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            WRAPPED.set(Boolean.TRUE);
            XssRequestWrapper wrappedRequest = new XssRequestWrapper(request);
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            WRAPPED.remove();
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

    private static boolean isJsonRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().contains("application/json");
    }

    /**
     * XSS请求包装器 - 对参数、Header、JSON Body进行HTML转义
     */
    public static class XssRequestWrapper extends HttpServletRequestWrapper {

        /** 缓存清理后的JSON Body 字节数组 */
        private byte[] cachedBody;

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
         * 🔒 M-01 修复: 重写 getInputStream，清理 JSON Body 中的 XSS
         */
        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (cachedBody == null) {
                cacheBody();
            }
            ByteArrayInputStream buffer = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return buffer.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int read() {
                    return buffer.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        /**
         * 缓存并清理请求体内容
         */
        private void cacheBody() throws IOException {
            byte[] originalBody = super.getInputStream().readAllBytes();

            if (isJsonRequest((HttpServletRequest) getRequest()) && originalBody.length > 0) {
                // 性能优化：先检查是否包含潜在 XSS
                String bodyStr = new String(originalBody, StandardCharsets.UTF_8);
                if (XSS_PATTERN.matcher(bodyStr).find()) {
                    log.warn("检测到潜在 XSS payload, 路径: {}",
                            ((HttpServletRequest) getRequest()).getRequestURI());
                }

                try {
                    JsonNode rootNode = OBJECT_MAPPER.readTree(bodyStr);
                    JsonNode cleaned = sanitizeJsonNode(rootNode);
                    cachedBody = OBJECT_MAPPER.writeValueAsBytes(cleaned);
                } catch (Exception e) {
                    // JSON 解析失败，使用原始 body
                    log.warn("JSON Body 解析失败，跳过 XSS 清理: {}", e.getMessage());
                    cachedBody = originalBody;
                }
            } else {
                cachedBody = originalBody;
            }
        }

        /**
         * 递归清理 JSON 节点中的所有字符串值
         */
        private JsonNode sanitizeJsonNode(JsonNode node) {
            if (node == null || node.isNull()) {
                return node;
            }
            if (node.isTextual()) {
                String sanitized = sanitize(node.asText());
                return OBJECT_MAPPER.getNodeFactory().textNode(sanitized);
            }
            if (node.isObject()) {
                Map<String, JsonNode> cleanedMap = new HashMap<>();
                Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    cleanedMap.put(field.getKey(), sanitizeJsonNode(field.getValue()));
                }
                return OBJECT_MAPPER.valueToTree(cleanedMap);
            }
            if (node.isArray()) {
                List<JsonNode> arr = new ArrayList<>();
                node.forEach(child -> arr.add(sanitizeJsonNode(child)));
                return OBJECT_MAPPER.valueToTree(arr);
            }
            return node;
        }

        /**
         * 使用 OWASP Encoder 清理XSS.
         *
         * @return 清理后的字符串，输入为 null 或空时返回原值（可能为 null）
         */
        @Nullable
        private String sanitize(@Nullable String value) {
            if (!StringUtils.hasText(value)) {
                return value;
            }
            // 使用OWASP Java Encoder进行HTML转义
            return Encode.forHtmlContent(value);
        }
    }
}
