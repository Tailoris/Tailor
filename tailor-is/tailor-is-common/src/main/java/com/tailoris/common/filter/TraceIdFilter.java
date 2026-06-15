package com.tailoris.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 链路追踪过滤器 - 修复 L-21.
 *
 * <p>🔒 L-21修复: 通过 MDC 传递 traceId，支持分布式链路追踪。
 *    优先级在 XssFilter 之后、业务过滤器之前，确保 traceId 在整个请求生命周期内可用。</p>
 *
 * <p>工作流程：</p>
 * <ol>
 *   <li>读取请求头 {@code X-Trace-Id}（上游服务透传）</li>
 *   <li>若不存在则生成 UUID 作为新 traceId</li>
 *   <li>写入 SLF4J MDC，日志格式 {@code %X{traceId}} 自动引用</li>
 *   <li>将 traceId 写入响应头 {@code X-Trace-Id} 供客户端排障</li>
 *   <li>请求结束后清理 MDC，避免线程复用导致污染</li>
 * </ol>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)  // 比 XssFilter 更高优先级
public class TraceIdFilter extends OncePerRequestFilter {

    /** 请求头常量：透传 traceId */
    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    /** MDC 键名（与 logback-spring.xml 中 %X{traceId} 对应） */
    public static final String MDC_TRACE_ID = "traceId";

    /** 请求 attribute 键名（业务代码可读取） */
    public static final String ATTR_TRACE_ID = "tailoris.traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String traceId = resolveTraceId(request);
            MDC.put(MDC_TRACE_ID, traceId);
            request.setAttribute(ATTR_TRACE_ID, traceId);
            response.setHeader(HEADER_TRACE_ID, traceId);
            filterChain.doFilter(request, response);
        } finally {
            // 必须清理，避免 Tomcat 线程复用导致日志错乱
            MDC.remove(MDC_TRACE_ID);
        }
    }

    /**
     * 解析 traceId：优先取请求头，缺失则生成新ID.
     */
    private String resolveTraceId(HttpServletRequest request) {
        String incoming = request.getHeader(HEADER_TRACE_ID);
        if (StringUtils.hasText(incoming)
                && incoming.length() <= 64   // 限制长度，防止恶意超长header
                && incoming.matches("^[A-Za-z0-9_\\-]+$")) {  // 仅允许安全字符
            return incoming;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
