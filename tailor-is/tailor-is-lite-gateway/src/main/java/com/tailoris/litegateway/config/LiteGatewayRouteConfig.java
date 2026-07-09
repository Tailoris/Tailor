package com.tailoris.litegateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 轻量网关路由权威配置（v4.0 规范）。
 * 作用：定义 lite-gateway 转发至社区/IM/内容/数据统计等轻量服务的路由规则。
 * 强制约束：
 *   1. 路由唯一权威来源，禁止在 yml 中重复定义；
 *   2. uri 必须使用 lb://tailor-is-{service-name} 服务发现格式，禁止硬编码 IP；
 *   3. 全部使用 /api/v1/ 前缀，废弃旧 /api/ 前缀；
 *   4. 统一 StripPrefix=0，完整传递 /api/v1/xxx 路径至下游服务；
 *   5. WebSocket /ws/** 统一由 lite-gateway 转发至 message-im-service。
 */
@Configuration
public class LiteGatewayRouteConfig {

    /**
     * 功能：构建 lite-gateway 自定义路由表。
     * 入参：RouteLocatorBuilder 路由构造器。
     * 返回值：RouteLocator 路由定位器。
     * 异常场景：Nacos 服务发现失败时路由不可达，由网关熔断兜底。
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // 社区、帖子、评论服务
                .route("community-route", r -> r.path("/api/v1/community/**",
                                "/api/v1/post/**", "/api/v1/comment/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-community"))
                // 面辅料供应链服务
                .route("supply-route", r -> r.path("/api/v1/supply/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-supply"))
                // 消息与通知服务
                .route("message-route", r -> r.path("/api/v1/message/**", "/api/v1/notice/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-message"))
                // 学堂与课程服务
                .route("academy-route", r -> r.path("/api/v1/academy/**", "/api/v1/course/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-academy"))
                // 数据统计、指标、看板服务
                .route("analytics-route", r -> r.path("/api/v1/analytics/**",
                                "/api/v1/metrics/**", "/api/v1/dashboard/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-analytics"))
                // IM 即时通讯服务
                .route("im-route", r -> r.path("/api/v1/im/**", "/api/v1/im-message/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-message-im"))
                // WebSocket 长连接统一经 lite-gateway 转发
                .route("ws-route", r -> r.path("/ws/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-message-im"))

                // ============================================================
                // 兼容旧 /api/ 前缀路由（v4.0 过渡期兼容转发，计划 3 个迭代后下线）
                // 将 /api/xxx/** 重写为 /api/v1/xxx/** 后转发至对应服务
                // ============================================================
                .route("legacy-community-route", r -> r.path("/api/community/**",
                                "/api/post/**", "/api/comment/**")
                        .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/api/v1/${segment}"))
                        .uri("lb://tailor-is-community"))
                .route("legacy-supply-route", r -> r.path("/api/supply/**")
                        .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/api/v1/${segment}"))
                        .uri("lb://tailor-is-supply"))
                .route("legacy-message-route", r -> r.path("/api/message/**", "/api/notice/**")
                        .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/api/v1/${segment}"))
                        .uri("lb://tailor-is-message"))
                .route("legacy-academy-route", r -> r.path("/api/academy/**", "/api/course/**")
                        .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/api/v1/${segment}"))
                        .uri("lb://tailor-is-academy"))
                .route("legacy-analytics-route", r -> r.path("/api/analytics/**",
                                "/api/metrics/**", "/api/dashboard/**")
                        .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/api/v1/${segment}"))
                        .uri("lb://tailor-is-analytics"))
                .route("legacy-im-route", r -> r.path("/api/im/**", "/api/im-message/**")
                        .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/api/v1/${segment}"))
                        .uri("lb://tailor-is-message-im"))
                .build();
    }
}
