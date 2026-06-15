# 缓存分层指南 (Cache Layering Guide)

> **版本**: v1.0 | **日期**: 2026-06-11 | **状态**: 已实施

## 概述

Tailor IS 架构采用**双层 Redis 缓存策略**：核心服务使用 Redis Cluster 提供高可用、高并发能力；非核心服务使用 Redis Standalone 实例降低资源开销。

```
┌─────────────────────────────────────────────────────────┐
│                    L1: Caffeine (本地)                   │
│            最大 10000 条目 · TTL 600s                    │
├─────────────────────────────────────────────────────────┤
│                    L2: Redis (分布式)                    │
│  ┌──────────────────────┐    ┌──────────────────────┐   │
│  │   Redis Cluster      │    │   Redis Standalone   │   │
│  │   核心服务 (6个)     │    │   非核心服务 (3个)   │   │
│  │   连接池: 50/20/5    │    │   连接池: 10/5/1     │   │
│  │   超时: 5000ms       │    │   超时: 2000ms       │   │
│  └──────────────────────┘    └──────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

## 服务分类

| 层级 | 模式 | 服务 | 资源特征 |
|------|------|------|----------|
| **核心** | `cluster` | order, payment, ai, copyright, merchant, product | 高连接池 (50/20/5)，5s 超时 |
| **非核心** | `standalone` | community, academy, supply | 轻连接池 (10/5/1)，2s 超时 |

## 配置方式

### 环境变量控制

```bash
# 核心服务
export REDIS_MODE=cluster
export REDIS_CLUSTER_NODES="10.0.1.1:7000,10.0.1.2:7001,10.0.1.3:7002,10.0.1.4:7003,10.0.1.5:7004,10.0.1.6:7005"

# 非核心服务（默认值）
export REDIS_MODE=standalone
```

### application.yml 配置

#### 核心服务（Redis Cluster）

```yaml
tailoris:
  redis:
    mode: ${REDIS_MODE:standalone}
    password: ${REDIS_PASSWORD}
    cluster:
      nodes: ${REDIS_CLUSTER_NODES:localhost:7000,localhost:7001,...}
      max-redirects: 3
      timeout: 5000
      pool:
        max-active: 50
        max-idle: 20
        min-idle: 5
        max-wait: 3000
```

#### 非核心服务（Redis Standalone）

```yaml
tailoris:
  redis:
    mode: ${REDIS_MODE:standalone}
    standalone:
      timeout: 2000
      pool:
        max-active: 10
        max-idle: 5
        min-idle: 1
        max-wait: 2000
```

## 缓存路由策略

### CacheRouter 组件

`CacheRouter` 是缓存路由的核心组件，根据运行模式自动选择 Redis 实例：

```java
@Component
public class CacheRouter {
    // 获取核心缓存模板（Redis Cluster）
    public RedisTemplate<String, Object> getCoreTemplate();

    // 获取轻量缓存模板（Redis Standalone）
    public RedisTemplate<String, Object> getLiteTemplate();

    // 根据模式选择
    public RedisTemplate<String, Object> getTemplate(String mode);
}
```

### 注解标记

```java
// 核心服务类/方法 — 使用 Redis Cluster
@CoreCache(prefix = "order:", ttlSeconds = 3600)
public class OrderService { ... }

// 非核心服务类/方法 — 使用 Redis Standalone
@LiteCache(prefix = "community:", ttlSeconds = 1800)
public class CommunityService { ... }
```

### 路由决策树

```
请求进入
   │
   ▼
是否有 CacheRouter?
   ├── 否 → 使用 Spring Boot 自动配置的默认 Redis
   │
   ▼
是否为 @CoreCache?
   ├── 是 → getCoreTemplate()
   │          ├── clusterConnectionFactory 存在 → Redis Cluster
   │          └── 否则 → defaultConnectionFactory (fallback)
   │
   ▼
是否为 @LiteCache?
   ├── 是 → getLiteTemplate()
              ├── standaloneConnectionFactory 存在 → Redis Standalone
              └── 否则 → defaultConnectionFactory (fallback)
```

## 缓存 Key 设计模式

### 命名规范

```
{业务域}:{实体}:{标识符}:{版本?}
```

### 示例 Key

| Key | 说明 | TTL |
|-----|------|-----|
| `order:detail:123456789` | 订单详情 | 30min |
| `payment:status:ORD123` | 支付状态 | 10min |
| `pattern:base:all` | AI 图案全集元数据 | 60min |
| `pattern:base:type:1` | 按类型分类的图案 | 60min |
| `copyright:hash:abc123` | 版权哈希记录 | 永久 |
| `merchant:info:M001` | 商户信息 | 15min |
| `community:post:hot` | 热门帖子列表 | 5min |
| `course:list:page1` | 课程列表分页 | 10min |

### AI 图案缓存 Key 设计

```
pattern:base:{type}:{size}
```

- `pattern:base:all` — 所有图案元数据
- `pattern:base:type:{typeId}` — 按图案类型缓存

由 `PatternCacheLoader` 在启动时预加载，每 30 分钟自动刷新。

## 技术实现细节

### RedisClusterConfig

- **连接工厂**: Lettuce Cluster Connection
- **拓扑刷新**: 周期性 (5min) + 自适应触发
- **连接池**: Apache Commons Pool2
- **Socket 选项**: Keep-Alive, 可配置超时
- **激活条件**: `@ConditionalOnProperty(name = "tailoris.redis.mode", havingValue = "cluster")`

### RedisStandaloneConfig

- **连接工厂**: Lettuce Standalone Connection
- **资源特征**: 更小的连接池、更短的超时
- **激活条件**: `@ConditionalOnProperty(name = "tailoris.redis.mode", havingValue = "standalone")`

### PatternCacheLoader

- **触发时机**: `@PostConstruct` (应用启动)
- **定时刷新**: `@Scheduled(fixedRate = 30 * 60 * 1000)`
- **数据源**: `pattern_record` 表 (status = 1)
- **缓存目标**: Redis Cluster (核心服务)

## 迁移指南

### 从单一 Redis 迁移到分层缓存

1. **评估服务分类** — 确定核心/非核心服务归属
2. **部署 Redis Cluster** — 至少 6 节点 (3 master + 3 replica)
3. **配置环境变量** — 设置 `REDIS_MODE` 和 `REDIS_CLUSTER_NODES`
4. **验证连接** — 启动服务，检查日志中 CacheRouter 的路由选择
5. **监控指标** — 观察连接池使用率、缓存命中率

### 快速验证

```bash
# 核心服务启动 (cluster 模式)
REDIS_MODE=cluster \
REDIS_CLUSTER_NODES="node1:7000,node2:7001,node3:7002" \
java -jar tailor-is-order.jar

# 非核心服务启动 (standalone 模式，默认)
REDIS_MODE=standalone \
java -jar tailor-is-community.jar
```

## 文件清单

| 文件 | 说明 |
|------|------|
| `tailor-is-common/.../config/RedisClusterConfig.java` | Redis Cluster 连接配置 |
| `tailor-is-common/.../config/RedisStandaloneConfig.java` | Redis Standalone 连接配置 |
| `tailor-is-common/.../config/CacheRouter.java` | 缓存路由组件 |
| `tailor-is-common/.../annotation/CoreCache.java` | 核心缓存注解 |
| `tailor-is-common/.../annotation/LiteCache.java` | 轻量缓存注解 |
| `tailor-is-ai/.../cache/PatternCacheLoader.java` | AI 图案预加载器 |
