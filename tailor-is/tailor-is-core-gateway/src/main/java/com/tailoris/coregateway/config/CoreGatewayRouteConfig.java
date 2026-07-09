package com.tailoris.coregateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 核心网关路由权威配置（v4.0 规范）。
 * 作用：定义 core-gateway 转发至核心业务微服务的路由规则。
 * 强制约束：
 *   1. 路由唯一权威来源，禁止在 yml 中重复定义；
 *   2. uri 必须使用 lb://tailor-is-{service-name} 服务发现格式，禁止硬编码 IP；
 *   3. 全部使用 /api/v1/ 前缀，废弃旧 /api/ 前缀；
 *   4. 统一 StripPrefix=0，完整传递 /api/v1/xxx 路径至下游服务；
 *   5. community/supply/message/academy/analytics/im 路由由 lite-gateway 承载。
 */
@Configuration
public class CoreGatewayRouteConfig {

    /**
     * 功能：构建 core-gateway 自定义路由表。
     * 入参：RouteLocatorBuilder 路由构造器。
     * 返回值：RouteLocator 路由定位器。
     * 异常场景：Nacos 服务发现失败时路由不可达，由网关熔断兜底。
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-route", r -> r.path("/api/v1/user/**", "/api/v1/auth/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-user"))
                .route("product-route", r -> r.path("/api/v1/product/**", "/api/v1/products/**", "/api/v1/favorite/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-product"))
                .route("order-route", r -> r.path("/api/v1/order/**", "/api/v1/cart/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-order"))
                .route("payment-route", r -> r.path("/api/v1/payment/**", "/api/v1/settlement/**",
                                "/api/v1/account/**", "/api/v1/sandbox/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-payment"))
                .route("marketing-route", r -> r.path("/api/v1/marketing/**", "/api/v1/coupon/**",
                                "/api/v1/points/**", "/api/v1/seckill/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-marketing"))
                .route("ai-route", r -> r.path("/api/v1/ai/**", "/api/v1/body-size/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-ai"))
                .route("copyright-route", r -> r.path("/api/v1/copyright/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-copyright"))
                .route("merchant-route", r -> r.path("/api/v1/merchant/**", "/api/v1/shop/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-merchant"))
                .route("admin-route", r -> r.path("/api/v1/admin/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-admin"))
                .route("pattern-route", r -> r.path("/api/v1/pattern/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-pattern"))
                .route("design-route", r -> r.path("/api/v1/design/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-design"))
                .route("knowledge-route", r -> r.path("/api/v1/knowledge/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-knowledge"))
                // ============================================================
                // community/supply/message/academy/analytics/im 路由已迁移至
                // LiteGatewayRouteConfig，由 lite-gateway（端口 8081）承载。
                // WebSocket /ws/** 也由 lite-gateway 统一处理。
                // ============================================================

                .route("legacy-user-route", r -> r.path("/api/user/**", "/api/auth/**")
                        .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/api/v1/${segment}"))
                        .uri("lb://tailor-is-user"))
                .route("legacy-product-route", r -> r.path("/api/product/**", "/api/products/**",
                                "/api/favorite/**")
                        .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/api/v1/${segment}"))
                        .uri("lb://tailor-is-product"))
                .route("legacy-order-route", r -> r.path("/api/order/**", "/api/cart/**")
                        .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/api/v1/${segment}"))
                        .uri("lb://tailor-is-order"))
                .route("legacy-payment-route", r -> r.path("/api/payment/**", "/api/settlement/**",
                                "/api/account/**", "/api/sandbox/**")
                        .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/api/v1/${segment}"))
                        .uri("lb://tailor-is-payment"))
                .route("legacy-marketing-route", r -> r.path("/api/marketing/**", "/api/coupon/**",
                                "/api/points/**", "/api/seckill/**")
                        .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/api/v1/${segment}"))
                        .uri("lb://tailor-is-marketing"))
                .route("legacy-ai-route", r -> r.path("/api/ai/**", "/api/body-size/**")
                        .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/api/v1/${segment}"))
                        .uri("lb://tailor-is-ai"))
                .route("legacy-copyright-route", r -> r.path("/api/copyright/**")
                        .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/api/v1/${segment}"))
                        .uri("lb://tailor-is-copyright"))
                .route("legacy-merchant-route", r -> r.path("/api/merchant/**", "/api/shop/**")
                        .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/api/v1/${segment}"))
                        .uri("lb://tailor-is-merchant"))
                .route("legacy-admin-route", r -> r.path("/api/admin/**")
                        .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/api/v1/${segment}"))
                        .uri("lb://tailor-is-admin"))
                .route("legacy-pattern-route", r -> r.path("/api/pattern/**")
                        .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/api/v1/${segment}"))
                        .uri("lb://tailor-is-pattern"))
                .route("legacy-design-route", r -> r.path("/api/design/**")
                        .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/api/v1/${segment}"))
                        .uri("lb://tailor-is-design"))
                .route("legacy-knowledge-route", r -> r.path("/api/knowledge/**")
                        .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/api/v1/${segment}"))
                        .uri("lb://tailor-is-knowledge"))
                .build();
    }
}
