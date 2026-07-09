package com.tailoris.coregateway.monitor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class RouteMatchFailureFilter implements GlobalFilter, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteMatchFailureFilter.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MeterRegistry meterRegistry;
    private Counter routeMatchFailureCounter;
    private Counter routeMatchSuccessCounter;

    public RouteMatchFailureFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        routeMatchFailureCounter = Counter.builder("gateway.route.match.failure")
                .description("路由匹配失败次数")
                .register(meterRegistry);

        routeMatchSuccessCounter = Counter.builder("gateway.route.match.success")
                .description("路由匹配成功次数")
                .register(meterRegistry);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";

        return chain.filter(exchange)
                .onErrorResume(ResponseStatusException.class, e -> {
                    if (e.getStatusCode() != null && e.getStatusCode().value() == 404) {
                        routeMatchFailureCounter.increment();
                        LOGGER.warn("[路由匹配失败] 时间: {}, 方法: {}, 路径: {}, 错误: {}",
                                LocalDateTime.now().format(FORMATTER),
                                method,
                                path,
                                e.getMessage());
                    }
                    return Mono.error(e);
                })
                .doOnSuccess(aVoid -> routeMatchSuccessCounter.increment());
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}