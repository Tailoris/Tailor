package com.tailoris.litegateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lite Gateway Sentinel 限流熔断配置.
 *
 * <p>为轻量级服务网关(community, academy, supply, message, message-im, analytics)
 * 提供统一的流量控制和熔断保护。QPS 阈值设为核心网关的 1/2，避免过度限流。
 *
 * <h3>限流规则</h3>
 * <ul>
 *   <li>默认 API 组: 100 QPS (核心网关为 200 QPS)</li>
 *   <li>指定路径组: 社区热门接口 50 QPS</li>
 * </ul>
 */
@Configuration
public class SentinelGatewayConfig {

    private final List<ViewResolver> viewResolvers;
    private final ServerCodecConfigurer serverCodecConfigurer;

    public SentinelGatewayConfig(
            ObjectProvider<List<ViewResolver>> viewResolversProvider,
            ServerCodecConfigurer serverCodecConfigurer) {
        this.viewResolvers = viewResolversProvider.getIfAvailable(Collections::emptyList);
        this.serverCodecConfigurer = serverCodecConfigurer;
    }

    /**
     * Sentinel Gateway 限流过滤器，最高优先级.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public GlobalFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter();
    }

    /**
     * Sentinel 限流异常处理器.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler() {
        return new SentinelGatewayBlockExceptionHandler(viewResolvers, serverCodecConfigurer);
    }

    /**
     * 初始化 Sentinel Gateway 限流规则.
     *
     * <p>规则在 Bean 初始化完成后加载，确保 Sentinel 传输层已就绪。
     *
     * <h3>规则设计</h3>
     * <ul>
     *   <li>默认 API 组: QPS 阈值 = 100 (核心网关的 1/2)</li>
     *   <li>api_paths: QPS 阈值 = 50, 针对社区帖子/消息等高频接口</li>
     * </ul>
     */
    @PostConstruct
    public void initGatewayRules() {
        initCustomizedApis();
        initGatewayFlowRules();
    }

    private void initCustomizedApis() {
        Set<ApiDefinition> definitions = new HashSet<>();

        // API 组: api_paths — 需要较低限流的高频接口
        ApiDefinition apiPaths = new ApiDefinition("api_paths")
                .setPredicateItems(new HashSet<>(List.of(
                        new ApiPathPredicateItem().setPattern("/api/community/**"),
                        new ApiPathPredicateItem().setPattern("/api/post/hot/**"),
                        new ApiPathPredicateItem().setPattern("/api/message/**")
                )));
        definitions.add(apiPaths);

        // API 组: default — 所有未被其他 API 组匹配的请求
        ApiDefinition defaultApi = new ApiDefinition("default")
                .setPredicateItems(new HashSet<>(List.of(
                        new ApiPathPredicateItem().setPattern("/**")
                )));
        definitions.add(defaultApi);

        GatewayApiDefinitionManager.loadApiDefinitions(definitions);
    }

    private void initGatewayFlowRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();

        // 规则 1: api_paths — 50 QPS (核心网关 100 QPS)
        GatewayFlowRule apiPathRule = new GatewayFlowRule("api_paths")
                .setCount(50)
                .setIntervalSec(1);
        rules.add(apiPathRule);

        // 规则 2: default — 100 QPS (核心网关 200 QPS)
        GatewayFlowRule defaultRule = new GatewayFlowRule("default")
                .setCount(100)
                .setIntervalSec(1);
        rules.add(defaultRule);

        GatewayRuleManager.loadRules(rules);
    }
}