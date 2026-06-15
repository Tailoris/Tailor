package com.tailoris.common.util;

import com.tailoris.common.filter.TraceIdFilter;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 链路追踪工具类 - 修复 L-21.
 *
 * <p>🔒 L-21修复: 提供 traceId 的统一访问入口。
 *    业务代码可通过 {@link #currentTraceId()} 获取当前请求的 traceId 用于关联日志。</p>
 *
 * <p>典型使用场景：</p>
 * <ul>
 *   <li>业务异常日志附加 traceId（{@code log.error("处理失败, traceId={}", TraceUtils.currentTraceId())}）</li>
 *   <li>异步任务 / MQ 消息透传 traceId（保证跨服务追踪）</li>
 *   <li>第三方接口调用时记录 traceId 便于排障</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
public final class TraceUtils {

    private TraceUtils() {
    }

    /**
     * 获取当前请求的 traceId.
     *
     * <p>获取优先级：</p>
     * <ol>
     *   <li>SLF4J MDC（{@link TraceIdFilter} 写入）</li>
     *   <li>HttpServletRequest attribute</li>
     *   <li>生成新的 traceId（异步/无Web上下文场景）</li>
     * </ol>
     *
     * @return 当前 traceId，永不为 null
     */
    public static String currentTraceId() {
        // 1. 优先从 MDC 读取
        String traceId = MDC.get(TraceIdFilter.MDC_TRACE_ID);
        if (traceId != null && !traceId.isEmpty()) {
            return traceId;
        }

        // 2. 从 request attribute 读取
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            Object attr = request.getAttribute(TraceIdFilter.ATTR_TRACE_ID);
            if (attr instanceof String s && !s.isEmpty()) {
                return s;
            }
        }

        // 3. 兜底生成（异步线程等无 Web 上下文场景）
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 将当前 traceId 放入 MDC（用于异步线程 / 定时任务 / MQ 消费者）.
     *
     * <p>使用完毕后请调用 {@link #clear()} 清理，避免线程复用污染。</p>
     *
     * @return 放入的 traceId
     */
    public static String putToMdc() {
        String traceId = currentTraceId();
        MDC.put(TraceIdFilter.MDC_TRACE_ID, traceId);
        return traceId;
    }

    /**
     * 清理当前线程 MDC 中的 traceId.
     */
    public static void clear() {
        MDC.remove(TraceIdFilter.MDC_TRACE_ID);
    }
}
