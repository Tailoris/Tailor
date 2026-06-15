# 双消息队列集成指南 — RabbitMQ + RocketMQ

> Tailor IS 平台消息队列架构优化文档

## 1. 概述

Tailor IS 平台采用 **双消息队列** 架构，根据业务场景特性选择最合适的 MQ 中间件：

| 维度 | RabbitMQ | RocketMQ |
|------|----------|----------|
| **适用场景** | 核心实时场景 | 批量异步场景 |
| **典型用例** | 支付回调、订单处理、即时通知 | AI 纸样生成、离线渲染、批量任务 |
| **延迟** | 毫秒级（低延迟） | 百毫秒级（可接受延迟） |
| **吞吐量** | 中等 | 高 |
| **消息可靠性** | 高（ACK 机制） | 极高（事务消息、消息回溯） |
| **消息顺序** | 单队列有序 | 分区有序 |
| **消息大小** | 建议 < 1MB | 支持最大 4MB（可调） |
| **特色能力** | 灵活路由（Exchange/Routing Key） | 事务消息、定时投递、消息回溯 |

## 2. 选型原则

### 2.1 何时使用 RabbitMQ

满足以下任一条件时选择 RabbitMQ：

- **低延迟要求**：用户操作后需要毫秒级响应
- **简单路由**：需要根据消息内容灵活路由到不同消费者
- **即时通知**：支付回调、订单状态变更等实时性强的场景
- **已有基础设施**：系统已有成熟的 RabbitMQ 部署和运维经验

**现有 RabbitMQ 场景：**
- `payment.callback.queue` — 支付回调处理
- `order.process.queue` — 订单处理流程
- `realtime.notify.queue` — 实时用户通知

### 2.2 何时使用 RocketMQ

满足以下任一条件时选择 RocketMQ：

- **批量处理**：大量消息需要异步处理，不需要即时响应
- **高吞吐**：消息量大，需要更高的吞吐能力
- **事务消息**：需要保证消息与本地事务的一致性
- **消息回溯**：需要重新消费历史消息（如 AI 模型重新训练）
- **定时投递**：延迟消息、定时任务
- **大消息**：消息体较大（如纸样数据、图片元数据）

**新增 RocketMQ 场景（AI 服务）：**
- `ai-pattern-topic` — AI 纸样生成、迭代、检查
- `ai-render-topic` — 离线渲染任务
- `batch-data-topic` — 批量数据处理

## 3. 架构设计

```
┌─────────────────────────────────────────────────────────┐
│                    Tailor IS 平台                        │
│                                                          │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐           │
│  │ 订单服务  │    │ 支付服务  │    │ AI 服务  │           │
│  └────┬─────┘    └────┬─────┘    └────┬─────┘           │
│       │ RabbitMQ      │ RabbitMQ      │ RabbitMQ(可选)   │
│       │               │               │ RocketMQ         │
│       ▼               ▼               ▼                 │
│  ┌─────────────────────────────────────────────────┐    │
│  │           MessageRoutingStrategy                 │    │
│  │  核心实时 → RabbitMQ   批量异步 → RocketMQ       │    │
│  └─────────────────────────────────────────────────┘    │
│       │                               │                 │
│       ▼                               ▼                 │
│  ┌──────────┐                  ┌──────────┐            │
│  │ RabbitMQ │                  │ RocketMQ │            │
│  └──────────┘                  └──────────┘            │
└─────────────────────────────────────────────────────────┘
```

## 4. 配置指南

### 4.1 父 POM 依赖管理

在 `tailor-is/pom.xml` 中统一管理版本：

```xml
<properties>
    <rocketmq-spring-boot-starter.version>2.3.1</rocketmq-spring-boot-starter.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.apache.rocketmq</groupId>
            <artifactId>rocketmq-spring-boot-starter</artifactId>
            <version>${rocketmq-spring-boot-starter.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 4.2 服务 POM 依赖

AI 服务 `tailor-is-ai/pom.xml` 同时引入 RabbitMQ 和 RocketMQ：

```xml
<!-- RabbitMQ: 保留实时场景 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
<!-- RocketMQ: 新增批量异步场景 -->
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
</dependency>
```

### 4.3 application.yml 配置

```yaml
spring:
  # RabbitMQ: 核心实时场景
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:rabbitmq}
    password: ${RABBITMQ_PASSWORD:rabbitmq}
    virtual-host: /

# RocketMQ: 批量异步场景
rocketmq:
  name-server: ${ROCKETMQ_NAME_SERVER:localhost:9876}
  producer:
    group: ai-pattern-producer
    send-message-timeout: 10000
    retry-times-when-send-async-failed: 3
    compress-message-body-threshold: 4096
    max-message-size: 4194304
```

## 5. 代码使用示例

### 5.1 使用路由策略类

通过 `MessageRoutingStrategy` 统一管理 Topic/Queue 命名：

```java
import com.tailoris.common.mq.MessageRoutingStrategy;

// 路由决策
boolean useRocket = MessageRoutingStrategy.useRocketMQ(
    MessageRoutingStrategy.SceneType.AI_BATCH_PATTERN); // true

// 获取常量
String topic = MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TOPIC;
String consumerGroup = MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_CONSUMER_GROUP;
```

### 5.2 RocketMQ Producer 发送消息

```java
@Service
@RequiredArgsConstructor
public class PatternBatchService {

    private final RocketMqPatternProducer rocketMqProducer;

    public void submitBatchPattern(List<PatternGenerateRequest> requests) {
        for (PatternGenerateRequest req : requests) {
            rocketMqProducer.sendBatchGenerateTask(req);
        }
    }
}
```

### 5.3 RocketMQ Consumer 消费消息

```java
@Component
@RocketMQMessageListener(
    topic = MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TOPIC,
    consumerGroup = MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_CONSUMER_GROUP,
    selectorExpression = "*",
    maxReconsumeTimes = 3
)
public class PatternConsumer implements RocketMQListener<String> {
    @Override
    public void onMessage(String message) {
        // 处理纸样任务
    }
}
```

## 6. 最佳实践

### 6.1 命名规范

- **RabbitMQ**：`业务域.操作.queue` / `业务域.操作.exchange`
- **RocketMQ**：`业务域-操作-topic`，使用 Tag 区分消息子类型

### 6.2 消息设计

- 消息体尽量精简，大文件通过 OSS 存储后传递 URL
- 使用 JSON 序列化，确保可读性和跨语言兼容
- 消息中必须包含 traceId 用于全链路追踪

### 6.3 错误处理

- **RabbitMQ**：使用死信队列处理消费失败消息
- **RocketMQ**：利用 `maxReconsumeTimes` 控制重试次数，超过后进入死信队列

### 6.4 性能优化

- **批量发送**：RocketMQ Producer 支持批量发送，减少网络开销
- **消费并发**：合理设置 `consumeThreadMin` / `consumeThreadMax`
- **消息压缩**：RocketMQ 自动压缩超过 4KB 的消息体

### 6.5 监控告警

- RabbitMQ：通过 Management Plugin 监控队列长度、消费速率
- RocketMQ：通过 Dashboard 监控 TPS、消费延迟、堆积量

## 7. 部署注意事项

### 7.1 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `ROCKETMQ_NAME_SERVER` | RocketMQ NameServer 地址 | `localhost:9876` |
| `RABBITMQ_HOST` | RabbitMQ 主机地址 | `localhost` |
| `RABBITMQ_PORT` | RabbitMQ 端口 | `5672` |
| `RABBITMQ_USERNAME` | RabbitMQ 用户名 | `rabbitmq` |
| `RABBITMQ_PASSWORD` | RabbitMQ 密码 | `rabbitmq` |

### 7.2 Docker Compose 示例

```yaml
services:
  rocketmq-namesrv:
    image: apache/rocketmq:5.3.1
    command: sh mqnamesrv
    ports:
      - "9876:9876"
    environment:
      JAVA_OPT_EXT: "-Xms256m -Xmx256m"

  rocketmq-broker:
    image: apache/rocketmq:5.3.1
    command: sh mqbroker -n rocketmq-namesrv:9876 --enable-proxy
    ports:
      - "10911:10911"
    environment:
      JAVA_OPT_EXT: "-Xms256m -Xmx512m"
    depends_on:
      - rocketmq-namesrv
```

## 8. 版本兼容性

| 组件 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.3.5 | 父工程统一版本 |
| rocketmq-spring-boot-starter | 2.3.1 | 兼容 Spring Boot 3.x |
| Spring Cloud Alibaba | 2023.0.3.2 | 微服务基础设施 |
| RabbitMQ Client | 5.21.0 | spring-boot-starter-amqp 内置 |

## 9. 迁移指南

如果某个服务需要从 RabbitMQ 迁移到 RocketMQ（或相反）：

1. 确认消息场景是否匹配目标 MQ 的优势领域
2. 更新 pom.xml 引入对应依赖
3. 创建对应的 Producer/Consumer 实现
4. 使用 `MessageRoutingStrategy` 统一管理命名
5. 灰度发布：双写双读，确认无误后切换
6. 监控新 MQ 的消费延迟和错误率
