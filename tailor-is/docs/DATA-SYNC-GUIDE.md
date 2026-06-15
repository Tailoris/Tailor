# Tailor IS 数据同步分层指南

> 版本: 3.0.0 | 最后更新: 2026-06-11

## 目录

- [概述](#概述)
- [同步策略分类](#同步策略分类)
- [数据分类](#数据分类)
- [架构设计](#架构设计)
- [实时同步](#实时同步)
- [近实时同步](#近实时同步)
- [批量同步](#批量同步)
- [同步路由](#同步路由)
- [一致性校验](#一致性校验)
- [配置说明](#配置说明)
- [实时 vs 近实时权衡](#实时-vs-近实时权衡)
- [扩展指南](#扩展指南)
- [故障排查](#故障排查)

---

## 概述

Tailor IS 采用分层数据同步策略，根据数据的重要性和一致性要求，将数据同步分为三个层级：

| 层级 | 延迟 | 同步机制 | 一致性模型 | 适用数据 |
|------|------|----------|------------|----------|
| **实时 (REAL_TIME)** | 秒级 | RabbitMQ + Seata | 强一致性 / 最终一致性 | 订单、商品、资金、用户 |
| **近实时 (NEAR_REAL_TIME)** | 5 分钟 | 定时轮询 | 最终一致性 | 社区帖子、学院课程、供应链 |
| **批量 (BATCH)** | 小时级 | 离线批处理 | 最终一致性 | 报表、归档数据、日志 |

### 设计目标

1. **降低系统负载**：非核心数据不需要实时同步，减少 RabbitMQ 消息量
2. **资源优化**：将数据库连接和计算资源优先分配给核心数据同步
3. **可扩展性**：新数据类型可以明确分类并选择合适的同步策略
4. **可观测性**：提供一致性校验和监控，及时发现同步问题

---

## 同步策略分类

### SyncLevel 枚举

```java
public enum SyncLevel {
    REAL_TIME("实时同步", 0),      // 秒级
    NEAR_REAL_TIME("近实时同步", 300),  // 5 分钟
    BATCH("批量同步", 3600);       // 小时级
}
```

### 分类规则

分类逻辑在 `DataSyncStrategyConfig` 中定义：

- **核心数据** → `REAL_TIME`
- **非核心数据** → `NEAR_REAL_TIME`
- **历史归档数据** → `BATCH`

---

## 数据分类

### 核心数据（实时同步）

| 数据类型 | 标识 | 原因 |
|----------|------|------|
| 订单 | `order` | 订单状态直接影响支付和库存 |
| 商品 | `product` | 商品信息和库存变更需即时反映 |
| 资金 | `fund` | 涉及金额，必须强一致 |
| 支付 | `payment` | 支付结果影响订单状态 |
| 用户 | `user` | 用户身份和权限变更 |
| 商户 | `merchant` | 商户状态影响交易 |
| 结算 | `settlement` | 资金结算数据 |
| 库存 | `inventory` | 库存扣减影响下单 |

### 非核心数据（近实时同步）

| 数据类型 | 标识 | 原因 |
|----------|------|------|
| 社区帖子 | `community_post` | 社区内容可容忍短暂延迟 |
| 社区评论 | `community_comment` | 同上 |
| 社区点赞 | `community_like` | 同上 |
| 学院课程 | `academy_course` | 课程内容更新频率低 |
| 供应链数据 | `supply` | 供应链数据变更不紧急 |
| 分析数据 | `analytics` | 分析数据本身就是统计结果 |
| 消息 | `message` | 消息通知可容忍短延迟 |
| 通知 | `notification` | 通知推送可容忍短延迟 |

---

## 架构设计

```
┌─────────────┐     ┌──────────────────┐
│  写操作服务   │────▶│  DataSyncRouter  │
│  (Order 等)  │     └────────┬─────────┘
└─────────────┘              │
                      ┌───────┴───────┐
                      │               │
                  核心数据?        非核心数据?
                      │               │
                      ▼               ▼
          ┌───────────────┐  ┌──────────────────┐
          │RealTimeDataSync│  │NearRealTimeSync  │
          │               │  │  Scheduler       │
          │  RabbitMQ     │  │  @Scheduled      │
          │  (秒级)       │  │  (5 min)         │
          └───────┬───────┘  └───────┬──────────┘
                  │                  │
                  ▼                  ▼
          ┌───────────────┐  ┌──────────────────┐
          │  目标服务消费者 │  │  目标服务 Provider │
          │  (Consumer)   │  │  (Provider 接口)  │
          └───────────────┘  └──────────────────┘

┌──────────────────────────────────────────┐
│       DataConsistencyValidator            │
│  每小时校验 ─→ 差异报告 ─→ 非核心自动修复   │
└──────────────────────────────────────────┘
```

---

## 实时同步

### 组件: `RealTimeDataSync`

**路径**: `tailor-is-common/src/main/java/com/tailoris/common/sync/RealTimeDataSync.java`

### 工作原理

1. 业务代码在事务中完成数据写操作
2. 通过 `publishAfterCommit()` 注册事务提交后回调
3. 事务提交后，`TransactionSynchronization.afterCommit()` 触发
4. 将同步事件序列化并通过 RabbitMQ 发送
5. 目标服务通过 `@RabbitListener` 消费并应用变更

### Exchange & Routing

- **Exchange**: `tailoris.core-data.sync`
- **RoutingKey**: `{dataType}.{action}`（如 `order.status_change`）

### 使用示例

```java
@Service
public class OrderService {
    private final RealTimeDataSync realTimeDataSync;

    @Transactional
    public void payOrder(String orderNo) {
        orderMapper.updateStatus(orderNo, PAID);

        // 事务提交后发送同步事件
        realTimeDataSync.publishAfterCommit(
            "order",
            "status_change",
            Map.of("orderNo", orderNo, "status", "PAID")
        );
    }
}
```

### 消费者示例

```java
@Component
@Slf4j
public class OrderSyncConsumer {

    @RabbitListener(
        queues = "order.sync.queue",
        bindings = @QueueBinding(
            value = @Queue(value = "order.sync.queue", durable = "true"),
            exchange = @Exchange(value = "tailoris.core-data.sync", durable = "true"),
            key = "order.*"
        )
    )
    public void handleOrderSync(String message) throws Exception {
        SyncEvent event = objectMapper.readValue(message, SyncEvent.class);
        // 应用变更
    }
}
```

### Seata 分布式事务

对于跨库强一致性场景（如订单创建 + 库存扣减），使用 Seata AT 模式：

```java
@GlobalTransactional
public void createOrderWithStock(CreateOrderRequest request) {
    // Order 服务: 创建订单
    orderMapper.insert(order);
    // Product 服务 (通过 Feign): 扣减库存
    productClient.deductStock(productId, quantity);
}
```

---

## 近实时同步

### 组件: `NearRealTimeSyncScheduler`

**路径**: `tailor-is-common/src/main/java/com/tailoris/common/sync/NearRealTimeSyncScheduler.java`

### 工作原理

1. 每 5 分钟（可配置）触发一次调度任务
2. 遍历所有注册的 `NearRealTimeSyncProvider`
3. 查询自上次同步以来变更的记录（基于 `update_time`）
4. 逐条同步变更记录到下游服务
5. 记录同步统计信息

### 扩展方式

实现 `NearRealTimeSyncProvider` 接口：

```java
@Component
public class CommunityPostSyncProvider implements NearRealTimeSyncProvider {

    @Override
    public String getDataType() {
        return "community_post";
    }

    @Override
    public List<String> getChangedRecordIds(LocalDateTime since) {
        // 查询 update_time > since 的记录
        return postMapper.selectChangedIds(since);
    }

    @Override
    public void syncRecord(String recordId) {
        CommunityPost post = postMapper.selectById(recordId);
        // 同步到目标服务（通过 Feign 或 MQ）
        searchClient.indexPost(post);
    }
}
```

### 调度配置

```yaml
tailoris:
  data-sync:
    near-real-time:
      enabled: true
      fixed-rate-ms: 300000  # 5 分钟
```

---

## 批量同步

批量同步适用于报表、归档数据等低频场景，由外部调度系统（如 XXL-JOB、Airflow）触发。

当前分类标记为 `BATCH` 的数据类型：
- `report` — 报表数据
- `archive` — 归档数据
- `log` — 日志
- `audit_trail` — 审计轨迹

---

## 同步路由

### 组件: `DataSyncRouter`

**路径**: `tailor-is-common/src/main/java/com/tailoris/common/sync/DataSyncRouter.java`

统一入口，根据数据类型自动选择同步策略：

```java
@Service
public class CommunityPostService {
    private final DataSyncRouter dataSyncRouter;

    public void createPost(CommunityPost post) {
        postMapper.insert(post);

        // 自动路由: community_post → 近实时同步
        dataSyncRouter.routeSync("community_post", "create",
            Map.of("postId", post.getId()));
    }
}
```

### 强制实时同步

特殊场景下可提升同步级别：

```java
dataSyncRouter.forceRealTimeSync("community_post", "create", payload);
```

---

## 一致性校验

### 组件: `DataConsistencyValidator`

**路径**: `tailor-is-common/src/main/java/com/tailoris/common/sync/DataConsistencyValidator.java`

### 校验机制

1. **定时触发**: 默认每小时整点执行（可配置 cron）
2. **采样比对**: 从源服务和目标服务各抽取样本
3. **差异检测**: 比对关键字段是否一致
4. **自动修复**: 非核心数据差异自动触发重新同步
5. **告警上报**: 核心数据差异记录日志并告警

### 扩展方式

实现 `ConsistencyChecker` 接口：

```java
@Component
public class CommunityPostConsistencyChecker implements ConsistencyChecker {

    @Override
    public String getDataType() {
        return "community_post";
    }

    @Override
    public CheckResult check() {
        // 1. 从源数据库获取样本
        List<CommunityPost> sourcePosts = sourceMapper.sample(100);

        // 2. 从目标服务获取对应数据
        List<SearchDoc> targetDocs = searchClient.batchGet(
            sourcePosts.stream().map(p -> p.getId().toString()).toList()
        );

        // 3. 比对
        List<String> discrepancyIds = new ArrayList<>();
        for (CommunityPost post : sourcePosts) {
            SearchDoc doc = findDoc(targetDocs, post.getId());
            if (doc == null || !post.getTitle().equals(doc.getTitle())) {
                discrepancyIds.add(post.getId().toString());
            }
        }

        return discrepancyIds.isEmpty()
            ? CheckResult.ok(sourcePosts.size())
            : CheckResult.fail(sourcePosts.size(), discrepancyIds, "标题不一致");
    }

    @Override
    public boolean autoRepair(String recordId) {
        // 重新同步单条记录
        CommunityPost post = postMapper.selectById(recordId);
        searchClient.indexPost(post);
        return true;
    }
}
```

### 查询校验报告

```java
// 获取最近的校验报告
List<ValidationReport> reports = validator.getRecentReports(10);

// 获取统计摘要
Map<String, Object> stats = validator.getStats();

// 手动触发校验
validator.validateNow("community_post");
```

---

## 配置说明

### application.yml 完整配置

```yaml
tailoris:
  data-sync:
    # 近实时同步调度器
    near-real-time:
      enabled: true
      fixed-rate-ms: 300000  # 5 分钟

    # 一致性校验
    validation:
      enabled: true
      cron: "0 0 * * * ?"    # 每小时整点
      max-sample-size: 1000   # 单次最大采样数

    # 重试配置
    retry:
      max-attempts: 3          # 最大重试次数
      initial-backoff-ms: 1000 # 初始退避间隔
      max-backoff-ms: 30000    # 最大退避间隔
      multiplier: 2            # 退避倍数

    # 自定义数据分类（追加）
    extra-real-time-types: []
    extra-near-real-time-types: []
    extra-batch-types: []
```

---

## 实时 vs 近实时权衡

| 维度 | 实时同步 | 近实时同步 |
|------|----------|------------|
| **延迟** | 秒级 (< 1s) | 分钟级 (≤ 5min) |
| **一致性** | 强一致性 (Seata) / 最终一致性 (MQ) | 最终一致性 |
| **资源消耗** | 高 (每条变更都发消息) | 低 (批量轮询) |
| **实现复杂度** | 高 (分布式事务、消息可靠性) | 低 (定时任务 + 增量查询) |
| **故障影响** | 消息堆积影响核心业务 | 延迟增大，不影响核心业务 |
| **监控要求** | 高 (需实时告警) | 中 (定期检查) |

### 决策树

```
数据类型?
    │
    ├─ 涉及资金/支付/订单?
    │     └─ 是 → REAL_TIME (RabbitMQ + Seata)
    │
    ├─ 用户可见的业务数据?
    │     └─ 是 → NEAR_REAL_TIME (5 min)
    │
    ├─ 内容社区/学院/分析?
    │     └─ 是 → NEAR_REAL_TIME (5 min)
    │
    └─ 历史归档/报表/日志?
          └─ 是 → BATCH (hourly)
```

---

## 扩展指南

### 添加新的数据类型分类

```java
// 通过 application.yml 追加
tailoris:
  data-sync:
    extra-real-time-types:
      - "new_core_type"
    extra-near-real-time-types:
      - "new_non_core_type"
```

### 添加新的同步 Provider

1. 实现 `NearRealTimeSyncProvider` 接口
2. 标注 `@Component`，Spring 自动发现
3. 调度器自动纳入调度

### 添加新的校验 Checker

1. 实现 `ConsistencyChecker` 接口
2. 标注 `@Component`，Spring 自动发现
3. 校验器自动纳入校验任务

### 禁用同步功能

```yaml
# 禁用近实时同步
tailoris:
  data-sync:
    near-real-time:
      enabled: false

# 禁用一致性校验
tailoris:
  data-sync:
    validation:
      enabled: false
```

---

## 故障排查

### 实时同步消息丢失

1. 检查 RabbitMQ 队列是否正常运行
2. 检查 `publisher-confirm-type` 和 `publisher-returns` 配置
3. 查看日志中的 `B-L10` 标记（消息确认/退回日志）
4. 检查消费者端是否正常消费

### 近实时同步未执行

1. 检查 `@EnableScheduling` 是否在主类上
2. 检查 `tailoris.data-sync.near-real-time.enabled` 是否为 `true`
3. 查看调度日志是否有异常
4. 检查 Provider 是否注册成功

### 一致性校验发现差异

1. 查看 `ValidationReport` 获取差异详情
2. 非核心数据会自动修复，检查修复日志
3. 核心数据需人工介入：
   - 检查差异原因（同步延迟 vs 数据损坏）
   - 手动重新同步或使用 `forceRealTimeSync()`
4. 定期检查 `getStats()` 监控差异趋势

### 性能问题

1. 近实时同步批量过大 → 减小 `max-sample-size`
2. RabbitMQ 消息堆积 → 增加消费者并发
3. 数据库查询慢 → 优化 Provider 的 `getChangedRecordIds` 查询（确保 `update_time` 有索引）

---

## 文件清单

| 文件 | 路径 | 说明 |
|------|------|------|
| DataSyncStrategyConfig | `tailor-is-common/.../config/DataSyncStrategyConfig.java` | 同步策略分类配置 |
| RealTimeDataSync | `tailor-is-common/.../sync/RealTimeDataSync.java` | 实时同步组件 |
| NearRealTimeSyncScheduler | `tailor-is-common/.../sync/NearRealTimeSyncScheduler.java` | 近实时同步调度器 |
| DataSyncRouter | `tailor-is-common/.../sync/DataSyncRouter.java` | 同步路由器 |
| DataConsistencyValidator | `tailor-is-common/.../sync/DataConsistencyValidator.java` | 一致性校验工具 |
| application.yml | `tailor-is-common/.../resources/application.yml` | 同步配置 |
