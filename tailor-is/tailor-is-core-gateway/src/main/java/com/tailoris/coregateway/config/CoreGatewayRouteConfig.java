package com.tailoris.coregateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Core Gateway 路由权威配置.
 *
 * <p>本类为核心业务域服务的唯一路由定义来源, 包含:
 * user, merchant, product, order, payment, marketing, ai, copyright, admin, pattern.
 *
 * <h3>设计决策</h3>
 * <ul>
 *   <li>StripPrefix=0: 下游 Controller 直接使用 /api/xxx/** 映射，保持 path 完整传递</li>
 *   <li>所有 uri 使用 lb:// 服务发现: 禁止写死 http://localhost:port</li>
 * </ul>
 *
 * <p>核心网关服务端口标准:
 * <ul>
 *   <li>core-gateway: 8080</li>
 *   <li>admin: 8100</li>
 *   <li>user: 8101</li>
 *   <li>product: 8102</li>
 *   <li>order: 8103</li>
 *   <li>payment: 8104</li>
 *   <li>marketing: 8105</li>
 *   <li>ai: 8106</li>
 *   <li>copyright: 8107</li>
 *   <li>merchant: 8110</li>
 *   <li>pattern: 8115</li>
 * </ul>
 */
@Configuration
public class CoreGatewayRouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // ===== 核心业务域 =====

                // 用户服务 - /api/user/** 与 /api/auth/**
                .route("user-route", r -> r.path("/api/user/**", "/api/auth/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-user"))

                // 商品服务 - /api/product/**, /api/favorite/**
                .route("product-route", r -> r.path("/api/product/**", "/api/favorite/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-product"))

                // 订单服务 - /api/order/**, /api/cart/**
                .route("order-route", r -> r.path("/api/order/**", "/api/cart/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-order"))

                // 支付服务 - /api/payment/**, /api/settlement/**, /api/account/**, /api/sandbox/**
                .route("payment-route", r -> r.path("/api/payment/**", "/api/settlement/**",
                                "/api/account/**", "/api/sandbox/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-payment"))

                // 营销服务 - /api/marketing/**, /api/coupon/**, /api/points/**, /api/seckill/**
                .route("marketing-route", r -> r.path("/api/marketing/**", "/api/coupon/**",
                                "/api/points/**", "/api/seckill/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-marketing"))

                // AI 服务 - /api/ai/**, /api/body-size/**
                .route("ai-route", r -> r.path("/api/ai/**", "/api/body-size/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-ai"))

                // 版权服务 - /api/copyright/**
                .route("copyright-route", r -> r.path("/api/copyright/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-copyright"))

                // 商家服务 - /api/merchant/**, /api/shop/**
                .route("merchant-route", r -> r.path("/api/merchant/**", "/api/shop/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-merchant"))

                // ===== 管理域 =====

                // 管理后台 - /api/admin/**
                .route("admin-route", r -> r.path("/api/admin/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-admin"))

                // ===== 增值域 (按需启用) =====

                // 图案/纸样 - /api/pattern/**
                .route("pattern-route", r -> r.path("/api/pattern/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://tailor-is-pattern"))

                .build();
    }
}
