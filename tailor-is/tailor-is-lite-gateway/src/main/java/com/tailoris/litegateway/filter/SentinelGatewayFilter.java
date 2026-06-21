package com.tailoris.litegateway.filter;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义 Sentinel Gateway 过滤器.
 *
 * <p>集成 Sentinel 与 Spring Cloud Gateway，根据路由路径应用不同的限流策略。
 * 当请求被限流或熔断时，返回 HTTP 429 (Too Many Requests) 状态码和 JSON 错误响应。</p>
 *
 * <h3>限流策略</h3>
 * <ul>
 *   <li>/api/public/** → 100 QPS</li>
 *   <li>/api/community/** → 50 QPS</li>
 *   <li>/api/academy/** → 30 QPS</li>
 *   <li>/api/message/** → 80 QPS</li>
 * </ul>
 *
 * <h3>错误响应格式</h3>
 * <pre>
 * {
 *   "code": 429,
 *   "message": "Too Many Requests",
 *   "data": {
 *     "reason": "rate_limited",
 *     "path": "/api/community/posts",
 *     "timestamp": 1234567890
 *   }
 * }
 * </pre>
 */
@Slf4j
public class SentinelGatewayFilter implements GatewayFilter, Ordered {

    private static final int ORDER = Ordered.HIGHEST_PRECEDENCE;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String resourceName = resolveResourceName(path);

        Entry entry = null;
        try {
            entry = SphU.entry(resourceName, EntryType.IN, 1);

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                if (entry != null) {
                    entry.exit();
                }
            }));
        } catch (BlockException e) {
            if (entry != null) {
                entry.exit();
            }
            return handleBlockException(exchange, path, e);
        } catch (Exception e) {
            if (entry != null) {
                entry.exit();
            }
            return Mono.error(e);
        }
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    /**
     * 根据请求路径解析 Sentinel 资源名称.
     */
    private String resolveResourceName(String path) {
        if (path == null) {
            return "default";
        }
        if (path.startsWith("/api/public/")) {
            return "public_api";
        }
        if (path.startsWith("/api/community/") || path.startsWith("/api/post/") || path.startsWith("/api/comment/")) {
            return "community_api";
        }
        if (path.startsWith("/api/academy/") || path.startsWith("/api/course/")) {
            return "academy_api";
        }
        if (path.startsWith("/api/message/") || path.startsWith("/api/im/") || path.startsWith("/api/notice/")) {
            return "message_api";
        }
        return "api_paths";
    }

    /**
     * 处理限流/熔断异常，返回 429 JSON 响应.
     */
    private Mono<Void> handleBlockException(ServerWebExchange exchange, String path, BlockException e) {
        String reason;
        if (e instanceof FlowException) {
            reason = "rate_limited";
            log.warn("请求被限流: path={}", path);
        } else if (e instanceof DegradeException) {
            reason = "circuit_broken";
            log.warn("请求被熔断: path={}", path);
        } else if (e instanceof ParamFlowException) {
            reason = "param_limited";
            log.warn("请求参数被限流: path={}", path);
        } else if (e instanceof SystemBlockException) {
            reason = "system_overloaded";
            log.warn("系统过载限流: path={}", path);
        } else {
            reason = "blocked";
            log.warn("请求被拦截: path={}, type={}", path, e.getClass().getSimpleName());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("reason", reason);
        data.put("path", path);
        data.put("timestamp", Instant.now().toEpochMilli());

        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("code", 429);
        errorBody.put("message", "Too Many Requests");
        errorBody.put("data", data);

        String json = buildJson(errorBody);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().setContentLength(bytes.length);

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * 构建简单的 JSON 字符串.
     */
    @SuppressWarnings("unchecked")
    private String buildJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number) {
                sb.append(value);
            } else if (value instanceof Map) {
                sb.append(buildJson((Map<String, Object>) value));
            } else {
                sb.append("\"").append(escapeJson(String.valueOf(value))).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}