package com.tailoris.coregateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreGatewayRouteConfig {

    @Value("${USER_SERVICE_HOST:localhost}")
    private String userServiceHost;

    @Value("${USER_SERVICE_PORT:8101}")
    private String userServicePort;

    @Value("${PRODUCT_SERVICE_HOST:localhost}")
    private String productServiceHost;

    @Value("${PRODUCT_SERVICE_PORT:8102}")
    private String productServicePort;

    @Value("${ORDER_SERVICE_HOST:localhost}")
    private String orderServiceHost;

    @Value("${ORDER_SERVICE_PORT:8103}")
    private String orderServicePort;

    @Value("${PAYMENT_SERVICE_HOST:localhost}")
    private String paymentServiceHost;

    @Value("${PAYMENT_SERVICE_PORT:8104}")
    private String paymentServicePort;

    @Value("${MARKETING_SERVICE_HOST:localhost}")
    private String marketingServiceHost;

    @Value("${MARKETING_SERVICE_PORT:8105}")
    private String marketingServicePort;

    @Value("${AI_SERVICE_HOST:localhost}")
    private String aiServiceHost;

    @Value("${AI_SERVICE_PORT:8106}")
    private String aiServicePort;

    @Value("${COPYRIGHT_SERVICE_HOST:localhost}")
    private String copyrightServiceHost;

    @Value("${COPYRIGHT_SERVICE_PORT:8107}")
    private String copyrightServicePort;

    @Value("${COMMUNITY_SERVICE_HOST:localhost}")
    private String communityServiceHost;

    @Value("${COMMUNITY_SERVICE_PORT:8108}")
    private String communityServicePort;

    @Value("${SUPPLY_SERVICE_HOST:localhost}")
    private String supplyServiceHost;

    @Value("${SUPPLY_SERVICE_PORT:8109}")
    private String supplyServicePort;

    @Value("${MERCHANT_SERVICE_HOST:localhost}")
    private String merchantServiceHost;

    @Value("${MERCHANT_SERVICE_PORT:8110}")
    private String merchantServicePort;

    @Value("${MESSAGE_SERVICE_HOST:localhost}")
    private String messageServiceHost;

    @Value("${MESSAGE_SERVICE_PORT:8111}")
    private String messageServicePort;

    @Value("${ACADEMY_SERVICE_HOST:localhost}")
    private String academyServiceHost;

    @Value("${ACADEMY_SERVICE_PORT:8112}")
    private String academyServicePort;

    @Value("${IM_SERVICE_HOST:localhost}")
    private String imServiceHost;

    @Value("${IM_SERVICE_PORT:8114}")
    private String imServicePort;

    @Value("${ADMIN_SERVICE_HOST:localhost}")
    private String adminServiceHost;

    @Value("${ADMIN_SERVICE_PORT:8100}")
    private String adminServicePort;

    @Value("${PATTERN_SERVICE_HOST:localhost}")
    private String patternServiceHost;

    @Value("${PATTERN_SERVICE_PORT:8115}")
    private String patternServicePort;

    @Value("${ANALYTICS_SERVICE_HOST:localhost}")
    private String analyticsServiceHost;

    @Value("${ANALYTICS_SERVICE_PORT:8116}")
    private String analyticsServicePort;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-route", r -> r.path("/api/user/**", "/api/auth/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://" + userServiceHost + ":" + userServicePort))

                .route("product-route", r -> r.path("/api/product/**", "/api/products/**", "/api/favorite/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://" + productServiceHost + ":" + productServicePort))

                .route("order-route", r -> r.path("/api/order/**", "/api/cart/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://" + orderServiceHost + ":" + orderServicePort))

                .route("payment-route", r -> r.path("/api/payment/**", "/api/settlement/**",
                                "/api/account/**", "/api/sandbox/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://" + paymentServiceHost + ":" + paymentServicePort))

                .route("marketing-route", r -> r.path("/api/marketing/**", "/api/coupon/**",
                                "/api/points/**", "/api/seckill/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://" + marketingServiceHost + ":" + marketingServicePort))

                .route("ai-route", r -> r.path("/api/ai/**", "/api/body-size/**", "/api/v1/ai/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://" + aiServiceHost + ":" + aiServicePort))

                .route("copyright-route", r -> r.path("/api/copyright/**", "/api/v1/copyright/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://" + copyrightServiceHost + ":" + copyrightServicePort))

                .route("community-route", r -> r.path("/api/community/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://" + communityServiceHost + ":" + communityServicePort))

                .route("supply-route", r -> r.path("/api/supply/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://" + supplyServiceHost + ":" + supplyServicePort))

                .route("merchant-route", r -> r.path("/api/merchant/**", "/api/shop/**", "/api/v1/merchant/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://" + merchantServiceHost + ":" + merchantServicePort))

                .route("message-route", r -> r.path("/api/message/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://" + messageServiceHost + ":" + messageServicePort))

                .route("academy-route", r -> r.path("/api/academy/**", "/api/v1/academy/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://" + academyServiceHost + ":" + academyServicePort))

                .route("im-route", r -> r.path("/api/im/**", "/api/v1/im/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://" + imServiceHost + ":" + imServicePort))

                .route("admin-route", r -> r.path("/api/admin/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://" + adminServiceHost + ":" + adminServicePort))

                .route("pattern-route", r -> r.path("/api/pattern/**", "/api/v1/pattern/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://" + patternServiceHost + ":" + patternServicePort))

                .route("analytics-route", r -> r.path("/api/analytics/**", "/api/v1/analytics/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://" + analyticsServiceHost + ":" + analyticsServicePort))

                .build();
    }
}