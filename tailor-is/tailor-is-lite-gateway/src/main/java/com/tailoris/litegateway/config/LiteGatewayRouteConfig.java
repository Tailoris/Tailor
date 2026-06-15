package com.tailoris.litegateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Lite Gateway 路由权威配置.
 *
 * <p>本类为轻量级服务域的唯一路由定义来源, 包含:
 * community, academy, supply, message, message-im, analytics.
 *
 * <h3>设计决策</h3>
 * <ul>
 *   <li>StripPrefix=0: 下游 Controller 直接使用 /api/xxx/** 映射，保持 path 完整传递</li>
 *   <li>所有 uri 使用 lb:// 服务发现: 禁止写死 http://localhost:port</li>
 *   <li>Sentinel 限流熔断已集成: QPS 阈值设为核心网关的 1/2</li>
 * </ul>
 *
 * <p>轻量网关服务端口标准:
 * <ul>
 *   <li>lite-gateway: 8081</li>
 *   <li>community: 8108</li>
 *   <li>academy: 8112</li>
 *   <li>supply: 8109</li>
 *   <li>message: 8111</li>
 *   <li>analytics: 8113</li>
 *   <li>message-im: 8114</li>
 * </ul>
 */
@Configuration
public class LiteGatewayRouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // ===== 轻量级业务域 =====

                // 社区服务 - /api/community/**, /api/post/**, /api/comment/**
                .route("community-route", r -> r.path("/api/community/**", "/api/post/**", "/api/comment/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-community"))

                // 学院 - /api/academy/**, /api/course/**
                .route("academy-route", r -> r.path("/api/academy/**", "/api/course/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-academy"))

                // 供应链服务 - /api/supply/**
                .route("supply-route", r -> r.path("/api/supply/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-supply"))

                // 消息服务 - /api/message/**, /api/notice/**
                .route("message-route", r -> r.path("/api/message/**", "/api/notice/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-message"))

                // IM 即时通讯 - /api/im/**, /api/im-message/**
                .route("im-route", r -> r.path("/api/im/**", "/api/im-message/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-message-im"))

                // 数据分析 - /api/analytics/**, /api/metrics/**, /api/dashboard/**
                .route("analytics-route", r -> r.path("/api/analytics/**", "/api/metrics/**", "/api/dashboard/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-analytics"))

                .build();
    }
}
