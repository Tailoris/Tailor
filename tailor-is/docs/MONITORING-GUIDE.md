# Tailor IS 分层监控指南

## 概述

Tailor IS 采用分层监控策略，根据服务的重要程度和流量特征，将服务分为**核心服务**和**轻量级服务**两个层级，分别配置不同的采集频率、指标范围和告警策略，以实现监控资源的优化利用。

### 分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                      Grafana Dashboards                      │
├─────────────────┬─────────────────────┬─────────────────────┤
│  核心服务面板    │  轻量级服务面板      │  资源利用率面板      │
│  (6个服务)       │  (3个服务)          │  (优化对比)         │
├─────────────────┴─────────────────────┴─────────────────────┤
│                    Prometheus                                │
├─────────────────────────┬───────────────────────────────────┤
│  核心服务 Scrape Config  │  轻量级服务 Scrape Config         │
│  10s 采集间隔            │  30s 采集间隔                      │
│  全量指标                │  基础指标                          │
├─────────────────────────┴───────────────────────────────────┤
│              Actuator /metrics Endpoint                     │
└─────────────────────────────────────────────────────────────┘
```

## 服务分级

### 核心服务 (Core Services)

直接影响交易链路和用户核心体验的服务：

| 服务名称 | Job Name | 关键性 | 采集间隔 |
|---------|----------|--------|---------|
| 订单服务 | tailor-is-order | 关键 | 10s |
| 支付服务 | tailor-is-payment | 关键 | 10s |
| AI服务 | tailor-is-ai | 重要 | 10s |
| 版权服务 | tailor-is-copyright | 重要 | 10s |
| 商户服务 | tailor-is-merchant | 重要 | 10s |
| 商品服务 | tailor-is-product | 重要 | 10s |

### 轻量级服务 (Lite Services)

辅助性、低频访问的服务，采用 Serverless 架构以节省资源：

| 服务名称 | Job Name | 关键性 | 采集间隔 |
|---------|----------|--------|---------|
| 社区服务 | tailor-is-community | 一般 | 30s |
| 学院服务 | tailor-is-academy | 一般 | 30s |
| 供应链服务 | tailor-is-supply | 一般 | 30s |

## 文件结构

```
deploy/
├── prometheus.yml                          # Prometheus 主配置
└── monitoring/
    ├── core-services-dashboard.json        # 核心服务 Grafana 面板
    ├── lite-services-dashboard.json        # 轻量级服务 Grafana 面板
    ├── resource-utilization-dashboard.json # 资源利用率面板
    └── core-alerts.yml                     # 告警规则 (核心+轻量+资源)
```

## 部署步骤

### 1. 部署 Prometheus

```bash
cd deploy
docker-compose -f docker-compose-monitoring.yml up -d prometheus
```

### 2. 配置告警规则

将 `monitoring/core-alerts.yml` 挂载到 Prometheus 容器：

```yaml
# docker-compose-monitoring.yml 中添加
volumes:
  - ./prometheus.yml:/etc/prometheus/prometheus.yml
  - ./monitoring/core-alerts.yml:/etc/prometheus/rules/core-alerts.yml
```

### 3. 导入 Grafana 面板

1. 打开 Grafana UI (默认 http://localhost:3000)
2. 进入 **Dashboard** → **Import**
3. 分别导入以下面板文件：
   - `core-services-dashboard.json` → 核心服务监控
   - `lite-services-dashboard.json` → 轻量级服务监控
   - `resource-utilization-dashboard.json` → 资源利用率监控

### 4. 验证采集状态

访问 Prometheus UI (默认 http://localhost:9090)，在 **Status → Targets** 页面检查各 Target 状态：

- 核心服务状态应为 **UP** (10s 采集间隔)
- 轻量级服务状态应为 **UP** (30s 采集间隔)
- 基础设施 (Redis, RabbitMQ, MySQL) 状态应为 **UP**

## 监控面板说明

### 核心服务面板 (core-services-dashboard.json)

| 区域 | 指标 | 说明 |
|------|------|------|
| 服务概览 | 总QPS、错误率、P99/平均响应时间 | 核心服务整体健康状态 |
| 服务级监控 | QPS趋势、P95响应时间、错误率、HTTP状态码 | 按服务/接口细分 |
| JVM指标 | 堆内存、GC耗时、线程数 | Java运行时状态 |
| 数据库连接池 | HikariCP活跃/空闲连接、连接超时率 | DB连接池健康 |
| Redis延迟 | P95命令延迟、连接数 | 缓存性能 |
| 业务指标 | 订单量、支付成功率、AI生成耗时、版权上链率、商户审核积压、商品上架率 | 业务健康度 |

### 轻量级服务面板 (lite-services-dashboard.json)

| 区域 | 指标 | 说明 |
|------|------|------|
| 服务概览 | 总QPS、可用性、P95响应时间、错误率 | 轻量级服务整体状态 |
| 服务级监控 | 请求数趋势、错误率 | 按服务细分 |
| 基础资源 | CPU使用率、堆内存使用 | 基础资源消耗 |
| 健康状态 | 在线状态、运行时间、实例数 | 服务存活情况 |

### 资源利用率面板 (resource-utilization-dashboard.json)

| 区域 | 指标 | 说明 |
|------|------|------|
| 资源总览 | 集群CPU/内存使用率、优化前基准值 | 整体资源消耗 |
| 对比分析 | 优化前后CPU/内存曲线对比 | 优化效果可视化 |
| Pod级分析 | 各Pod CPU使用率、内存使用量 | 细粒度资源分布 |
| Redis对比 | 集群vs独立内存、命中率、连接数 | Redis架构优化效果 |
| MQ吞吐量 | 生产/消费速率、队列积压、消息处理延迟 | 消息队列性能 |
| 优化总结 | CPU/内存/Redis节省比例、实例数量变化 | 优化效果汇总 |

## 告警规则说明

### 告警分级

| 级别 | 说明 | 通知方式 |
|------|------|---------|
| `critical` | 严重影响业务，需立即处理 | PagerDuty / 短信 / 电话 |
| `warning` | 潜在风险，需要关注 | 企业微信 / Slack / 邮件 |

### 核心服务告警 (core-service-alerts)

| 告警名称 | 条件 | 持续时间 | 级别 | 分类 |
|---------|------|---------|------|------|
| CoreServiceDown | 服务 down | 1m | critical | 可用性 |
| CoreServiceHighResponseTimeP99 | P99 > 1.0s | 3m | warning | 性能 |
| CoreServiceHighResponseTimeP95 | P95 > 0.5s | 5m | warning | 性能 |
| CoreServiceHighErrorRate | 5xx错误率 > 5% | 3m | critical | 可靠性 |
| CoreServiceElevatedErrorRate | 5xx错误率 > 1% | 5m | warning | 可靠性 |
| CoreServiceHighHeapUsage | 堆内存 > 85% | 5m | warning | 资源 |
| CoreServiceCriticalHeapUsage | 堆内存 > 95% | 2m | critical | 资源 |
| CoreServiceHighGCTime | GC占CPU > 10% | 5m | warning | 性能 |
| CoreServiceHighDBConnectionUsage | DB连接 > 80% | 5m | warning | 资源 |
| CoreServiceDBConnectionTimeout | 连接超时 > 0 | 1m | critical | 可靠性 |
| CoreServiceHighRedisLatency | Redis P95 > 50ms | 3m | warning | 性能 |
| PaymentSuccessRateLow | 支付成功率 < 95% | 5m | critical | 业务 |
| AIPatternGenerationSlow | AI生成耗时 > 10s | 5m | warning | 业务 |
| OrderVolumeDrop | 订单量低于均值50% | 10m | warning | 业务 |

### 自动扩容告警 (autoscaling)

| 告警名称 | 触发条件 | 持续时间 | 级别 |
|---------|---------|---------|------|
| AutoScaleTriggerCPU | CPU使用率 > 75% | 5m | warning |
| AutoScaleTriggerQPS | QPS > 500 | 3m | warning |
| AutoScaleTriggerMQBacklog | 队列积压 > 5000 | 5m | warning |

### 轻量级服务告警 (lite-service-alerts)

| 告警名称 | 条件 | 持续时间 | 级别 |
|---------|------|---------|------|
| LiteServiceDown | 服务 down | 3m | warning |
| LiteServiceHighErrorRate | 5xx错误率 > 5% | 5m | warning |
| LiteServiceHighMemoryUsage | 堆内存 > 90% | 5m | warning |

### 资源利用告警 (resource-utilization-alerts)

| 告警名称 | 条件 | 持续时间 | 级别 |
|---------|------|---------|------|
| ClusterCPUHigh | 集群CPU > 85% | 5m | warning |
| ClusterMemoryHigh | 集群内存 > 90% | 5m | warning |
| RedisMemoryHigh | Redis内存 > 8GB | 5m | warning |
| MQConsumerLag | 队列积压 > 10000 | 10m | critical |

## 业务指标自定义

核心服务面板中的业务指标需要在对应服务中暴露自定义 Metrics。以下是推荐的埋点方式：

### Spring Boot Actuator 自定义指标

```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class BusinessMetrics {
    private final Counter orderCreatedCounter;
    private final Counter paymentSuccessCounter;
    private final Counter paymentAttemptCounter;
    private final Timer aiPatternGenerationTimer;

    public BusinessMetrics(MeterRegistry registry) {
        this.orderCreatedCounter = registry.counter("business.order.created");
        this.paymentSuccessCounter = registry.counter("business.payment.success");
        this.paymentAttemptCounter = registry.counter("business.payment.attempt");
        this.aiPatternGenerationTimer = registry.timer("business.ai.pattern.generation");
    }

    public void recordOrderCreated() {
        orderCreatedCounter.increment();
    }

    public void recordPaymentAttempt() {
        paymentAttemptCounter.increment();
    }

    public void recordPaymentSuccess() {
        paymentSuccessCounter.increment();
    }

    public void recordAIPatternGeneration(long durationMs) {
        aiPatternGenerationTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }
}
```

## 资源优化基准值配置

资源利用率面板需要配置优化前的基准值，通过 `resource_utilization_baseline_*` 指标实现：

```promql
# 在 Prometheus 中通过 recording rule 或 pushgateway 注入基准值
resource_utilization_baseline_cpu_percent 75
resource_utilization_baseline_memory_percent 80
resource_utilization_baseline_redis_memory 10737418240
```

## 故障排查指南

### Prometheus 采集不到数据

1. 检查服务 `/actuator/prometheus` 端点是否可访问
   ```bash
   curl http://<service-host>:<port>/actuator/prometheus
   ```
2. 检查 Prometheus Target 状态页面
3. 检查 `prometheus.yml` 中 `kubernetes_sd_configs` 的 namespace 和 label 配置
4. 检查网络策略是否允许 Prometheus Pod 访问目标 Pod

### Grafana 面板无数据

1. 确认 Grafana 数据源配置正确 (Prometheus URL)
2. 检查面板 JSON 中的 `job` 标签是否与 Prometheus 配置一致
3. 在 Prometheus UI 中执行面板中的 PromQL 表达式验证数据
4. 检查面板的 time range 设置

### 告警未触发

1. 检查 `rule_files` 路径是否正确
2. 在 Prometheus UI 的 **Rules** 页面查看告警规则状态
3. 验证告警表达式中的标签选择器
4. 检查告警的 `for` 持续时间是否合理
5. 确认 Alertmanager 配置和通知渠道

### 资源利用率面板基准值不显示

1. 确认 `resource_utilization_baseline_*` 指标已注入 Prometheus
2. 可使用 Pushgateway 或 recording rules 注入基准数据
3. 检查指标名称与面板 PromQL 是否一致

### 核心服务 QPS 突降排查

1. 查看核心服务面板中的 HTTP 5xx 错误率
2. 检查数据库连接池是否饱和
3. 查看 Redis 延迟是否异常
4. 检查网关 (tailor-is-gateway) 是否出现限流
5. 查看自动扩容告警是否已触发

## 监控调优建议

### 采集频率调优

- 核心服务可根据实际流量调整 `scrape_interval`，高峰期可缩短至 5s
- 轻量级服务如流量极低可延长至 60s
- 注意 `scrape_timeout` 应小于 `scrape_interval`

### 告警阈值调优

- 初期建议适当放宽阈值，避免告警风暴
- 根据历史数据使用百分位数动态调整阈值
- 关键告警 (critical) 建议配置升级机制 (5分钟未处理自动升级)

### 存储调优

```yaml
# Prometheus 存储配置
storage:
  tsdb:
    retention.time: 15d      # 数据保留15天
    retention.size: 50GB     # 或限制存储大小
```

### Grafana 面板优化

- 使用模板变量实现服务/接口快速切换
- 复杂查询使用 Recording Rules 预计算
- 面板刷新率建议：核心服务 10s，轻量级服务 30s

## 扩展

### 添加新服务到监控

**核心服务：**

1. 在 `prometheus.yml` 的 core services 区域添加 job 配置
2. 在 `core-services-dashboard.json` 的 PromQL 中添加 `job=~` 匹配
3. 在 `core-alerts.yml` 的告警规则中添加新 job

**轻量级服务：**

1. 在 `prometheus.yml` 的 lite services 区域添加 job 配置
2. 在 `lite-services-dashboard.json` 中添加对应 PromQL

### 集成 Alertmanager

```yaml
# alertmanager.yml
route:
  receiver: 'default'
  group_by: ['alertname', 'severity', 'job']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h
  routes:
    - match:
        severity: critical
      receiver: 'pagerduty'
    - match:
        severity: warning
      receiver: 'wechat'

receivers:
  - name: 'default'
    webhook_configs:
      - url: 'http://notification-service:8080/alerts'
  - name: 'pagerduty'
    pagerduty_configs:
      - service_key: '<your-pagerduty-key>'
  - name: 'wechat'
    wechat_configs:
      - corp_id: '<your-corp-id>'
        to_party: '<your-party>'
        agent_id: '<your-agent-id>'
        api_secret: '<your-api-secret>'
```
