package com.tailoris.coregateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;

import java.util.Collections;
import java.util.List;

/**
 * Sentinel 网关流控配置.
 *
 * <p>集成 Sentinel Gateway 限流与熔断，全局流控规则从 Nacos 动态拉取
 * （参见 application.yml 中 spring.cloud.sentinel.datasource 配置）。
 *
 * <h3>设计要点</h3>
 * <ul>
 *   <li>SentinelGatewayFilter 作为全局过滤器，拦截所有请求进行流控检查</li>
 *   <li>SentinelGatewayBlockExceptionHandler 统一处理流控降级异常</li>
 *   <li>自定义 BlockRequestHandler 返回 JSON 格式错误响应，前端可解析</li>
 *   <li>规则持久化至 Nacos，支持运行时动态更新，无需重启网关</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 2.0.0
 */
@Configuration
public class SentinelGatewayConfig {

    private final List<ViewResolver> viewResolvers;
    private final ServerCodecConfigurer serverCodecConfigurer;

    public SentinelGatewayConfig(ObjectProvider<List<ViewResolver>> viewResolversProvider,
                                  ServerCodecConfigurer serverCodecConfigurer) {
        this.viewResolvers = viewResolversProvider.getIfAvailable(Collections::emptyList);
        this.serverCodecConfigurer = serverCodecConfigurer;
    }

    /**
     * Sentinel 网关全局过滤器.
     *
     * <p>拦截所有请求，根据 Nacos 中配置的 gw-flow 规则进行流量控制。
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public GlobalFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter();
    }

    /**
     * Sentinel 流控/降级异常处理器.
     *
     * <p>当请求被 Sentinel 限流或降级时，统一返回 JSON 格式错误响应。
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler() {
        return new SentinelGatewayBlockExceptionHandler(viewResolvers, serverCodecConfigurer);
    }

    /**
     * 自定义流控响应格式.
     *
     * <p>注册自定义 BlockRequestHandler，返回 JSON 格式的错误信息，
     * 包含错误码、错误消息和提示，前端可据此展示友好提示。
     */
    static {
        GatewayCallbackManager.setBlockHandler((exchange, t) ->
                ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(
                                "{\"code\": 429, \"message\": \"请求过于频繁，请稍后重试\", \"hint\": \"rate-limited\"}"
                        ))
        );
    }
}