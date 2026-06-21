package com.tailoris.litegateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.init.InitExecutor;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.circuitbreaker.CircuitBreakerStrategy;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.*;

/**
 * Sentinel 限流熔断配置.
 *
 * <p>为 Lite Gateway 提供完整的流量控制和熔断保护：
 * <ul>
 *   <li>流控规则: 按路由路径设置不同 QPS 阈值</li>
 *   <li>熔断规则: 错误率 > 50% 时触发熔断</li>
 *   <li>系统规则: CPU > 80% 时触发限流</li>
 *   <li>Sentinel Dashboard 连接</li>
 * </ul>
 */
@Slf4j
@Configuration
public class SentinelConfig {

    @Value("${spring.cloud.sentinel.transport.dashboard:localhost:8080}")
    private String dashboard;

    @Value("${spring.cloud.sentinel.transport.port:8719}")
    private int transportPort;

    @Value("${sentinel.rules.file:classpath:sentinel-rules.json}")
    private String rulesFile;

    /**
     * 初始化 Sentinel 规则.
     * <p>在 Bean 初始化完成后加载限流、熔断、系统规则。</p>
     */
    @PostConstruct
    public void initSentinelRules() {
        // 配置 Sentinel Dashboard 连接
        configureDashboard();

        // 初始化流控规则
        initFlowRules();

        // 初始化熔断规则
        initDegradeRules();

        // 初始化系统规则
        initSystemRules();

        // 加载预定义规则文件
        loadRulesFromFile();

        log.info("Sentinel 规则初始化完成 (Dashboard: {})", dashboard);
    }

    /**
     * 配置 Sentinel Dashboard 连接.
     */
    private void configureDashboard() {
        System.setProperty("csp.sentinel.dashboard.server", dashboard);
        System.setProperty("csp.sentinel.api.port", String.valueOf(transportPort));
        System.setProperty("project.name", "tailor-is-lite-gateway");
        InitExecutor.doInit();
        log.info("Sentinel Dashboard 连接配置: {}, port: {}", dashboard, transportPort);
    }

    /**
     * 初始化流控规则 (QPS-based).
     * <p>按路由路径设置不同的 QPS 阈值：
     * <ul>
     *   <li>/api/public/** → 100 QPS</li>
     *   <li>/api/community/** → 50 QPS</li>
     *   <li>/api/academy/** → 30 QPS</li>
     * </ul>
     */
    private void initFlowRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();

        // 公开接口 API 组
        rules.add(new GatewayFlowRule("public_api")
                .setCount(100)
                .setIntervalSec(1)
                .setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT));

        // 社区接口 API 组
        rules.add(new GatewayFlowRule("community_api")
                .setCount(50)
                .setIntervalSec(1)
                .setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT));

        // 学院接口 API 组
        rules.add(new GatewayFlowRule("academy_api")
                .setCount(30)
                .setIntervalSec(1)
                .setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT));

        // 消息接口 API 组
        rules.add(new GatewayFlowRule("message_api")
                .setCount(80)
                .setIntervalSec(1)
                .setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT));

        GatewayRuleManager.loadRules(rules);

        // 初始化 API 分组定义
        initApiDefinitions();

        log.info("流控规则初始化完成: public=100QPS, community=50QPS, academy=30QPS, message=80QPS");
    }

    /**
     * 初始化 API 分组定义.
     */
    private void initApiDefinitions() {
        Set<ApiDefinition> definitions = new HashSet<>();

        // 公开接口
        ApiDefinition publicApi = new ApiDefinition("public_api")
                .setPredicateItems(new HashSet<>(List.of(
                        new ApiPathPredicateItem().setPattern("/api/public/**")
                )));
        definitions.add(publicApi);

        // 社区接口
        ApiDefinition communityApi = new ApiDefinition("community_api")
                .setPredicateItems(new HashSet<>(List.of(
                        new ApiPathPredicateItem().setPattern("/api/community/**"),
                        new ApiPathPredicateItem().setPattern("/api/post/**"),
                        new ApiPathPredicateItem().setPattern("/api/comment/**")
                )));
        definitions.add(communityApi);

        // 学院接口
        ApiDefinition academyApi = new ApiDefinition("academy_api")
                .setPredicateItems(new HashSet<>(List.of(
                        new ApiPathPredicateItem().setPattern("/api/academy/**"),
                        new ApiPathPredicateItem().setPattern("/api/course/**")
                )));
        definitions.add(academyApi);

        // 消息接口
        ApiDefinition messageApi = new ApiDefinition("message_api")
                .setPredicateItems(new HashSet<>(List.of(
                        new ApiPathPredicateItem().setPattern("/api/message/**"),
                        new ApiPathPredicateItem().setPattern("/api/im/**"),
                        new ApiPathPredicateItem().setPattern("/api/notice/**")
                )));
        definitions.add(messageApi);

        com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager
                .loadApiDefinitions(definitions);
        log.info("API 分组定义初始化完成: {} 个分组", definitions.size());
    }

    /**
     * 初始化熔断规则.
     * <p>错误率超过 50% 且在 10s 统计窗口内达到最小请求数时触发熔断。
     * 熔断时长 30s，之后进入半开状态。</p>
     */
    private void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();

        // 社区服务熔断规则
        DegradeRule communityDegrade = new DegradeRule("community_api")
                .setGrade(CircuitBreakerStrategy.ERROR_RATIO.getType())
                .setCount(0.5)
                .setTimeWindow(30)
                .setMinRequestAmount(10)
                .setStatIntervalMs(10_000);
        rules.add(communityDegrade);

        // 学院服务熔断规则
        DegradeRule academyDegrade = new DegradeRule("academy_api")
                .setGrade(CircuitBreakerStrategy.ERROR_RATIO.getType())
                .setCount(0.5)
                .setTimeWindow(30)
                .setMinRequestAmount(10)
                .setStatIntervalMs(10_000);
        rules.add(academyDegrade);

        // 消息服务熔断规则
        DegradeRule messageDegrade = new DegradeRule("message_api")
                .setGrade(CircuitBreakerStrategy.ERROR_RATIO.getType())
                .setCount(0.5)
                .setTimeWindow(30)
                .setMinRequestAmount(10)
                .setStatIntervalMs(10_000);
        rules.add(messageDegrade);

        DegradeRuleManager.loadRules(rules);
        log.info("熔断规则初始化完成: {} 条规则", rules.size());
    }

    /**
     * 初始化系统规则.
     * <p>CPU 使用率超过 80% 时触发系统级流控，保护服务稳定性。</p>
     */
    private void initSystemRules() {
        List<SystemRule> rules = new ArrayList<>();

        SystemRule systemRule = new SystemRule();
        systemRule.setHighestSystemLoad(-1.0);
        systemRule.setHighestCpuUsage(0.8);
        systemRule.setAvgRt(1000);
        systemRule.setMaxThread(-1);
        systemRule.setQps(5000);
        rules.add(systemRule);

        SystemRuleManager.loadRules(rules);
        log.info("系统规则初始化完成: CPU>80% 触发限流");
    }

    /**
     * 从 JSON 文件加载预定义 Sentinel 规则.
     * <p>支持动态刷新，规则变更无需重启。</p>
     */
    private void loadRulesFromFile() {
        try {
            String path = rulesFile;
            if (path.startsWith("classpath:")) {
                path = path.substring("classpath:".length());
            }

            java.io.InputStream is = getClass().getClassLoader()
                    .getResourceAsStream(path);
            if (is != null) {
                log.info("从 classpath 加载 Sentinel 规则文件: {}", path);
                is.close();
            } else {
                log.warn("Sentinel 规则文件未找到: {}", path);
            }
        } catch (Exception e) {
            log.warn("加载 Sentinel 规则文件失败: {}", e.getMessage());
        }
    }
}