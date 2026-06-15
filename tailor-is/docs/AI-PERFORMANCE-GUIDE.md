# AI 智能纸样生成性能优化指南

> **模块**: tailor-is-ai  
> **版本**: 1.0.0  
> **作者**: Tailor IS Team  
> **更新日期**: 2026-06-11

---

## 目录

1. [概述](#概述)
2. [架构概览](#架构概览)
3. [分层模型调用策略](#分层模型调用策略)
4. [任务调度机制](#任务调度机制)
5. [非高峰批量预生成](#非高峰批量预生成)
6. [配置说明](#配置说明)
7. [性能基准](#性能基准)
8. [调优指南](#调优指南)
9. [故障排查](#故障排查)

---

## 概述

本指南文档描述了 Tailor IS AI 纸样生成服务的性能优化方案，包括：

- **分层模型调用**：根据体型复杂度智能路由到本地或云端模型
- **任务调度分离**：实时任务与批处理任务分离，避免资源竞争
- **非高峰预生成**：在凌晨非高峰时段预生成热门纸样，缓存加速峰值查询
- **缓存预热**：Redis 缓存预加载，降低数据库压力

### 优化目标

| 指标 | 优化前 | 优化后目标 |
|------|--------|-----------|
| P50 响应时间 | ~500ms | < 100ms（常规体型） |
| P99 响应时间 | ~2000ms | < 500ms |
| 峰值 QPS | ~50 | ~200 |
| 云端 API 调用量 | 100% | ~30%（70%由本地处理） |
| 缓存命中率 | 0% | > 70% |

---

## 架构概览

```
┌─────────────────────────────────────────────────────────┐
│                    PatternController                     │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│              PatternGenerationStrategy                   │
│                                                          │
│  ┌───────────────────┐    复杂度评估     ┌────────────┐ │
│  │ BodySizeData       │ ────────────────► │ 复杂度分数 │ │
│  │ + GarmentType      │                   │ [0.0, 1.0] │ │
│  └───────────────────┘                   └─────┬──────┘ │
│                                                │        │
│                    ┌───────────────────────────┤        │
│                    │ 复杂度 < 0.35?             │        │
│                    ├───────────┬───────────────┘        │
│                    │ YES       │ NO                     │
│                    ▼           ▼                        │
│            ┌───────────┐ ┌──────────────┐               │
│            │ 本地轻量   │ │ 云端分布式    │               │
│            │ ONNX模型   │ │ API 集群     │               │
│            │ ~50ms     │ │ ~500ms       │               │
│            └───────────┘ └──────────────┘               │
└─────────────────────────────────────────────────────────┘
```

---

## 分层模型调用策略

### 核心组件

| 文件 | 职责 |
|------|------|
| `PatternGenerationStrategy.java` | 分层路由策略核心 |
| `LocalModelConfig.java` | 本地模型配置 |
| `CloudModelConfig.java` | 云端模型配置 |
| `ModelRoute.java` | 路由枚举 |

### 路由决策流程

```
用户请求
    │
    ▼
是否为热门款式？ ──── YES ────► 云端模型（利用分布式算力）
    │
    NO
    │
    ▼
计算体型复杂度分数
    │
    ├── 分数 >= 0.35 ───► 云端模型（高精度处理）
    │
    ├── 分数 < 0.35  ───► 本地模型（低延迟处理）
    │
    └── 本地不可用？ ───► 降级到云端
```

### 复杂度评分算法

复杂度分数基于三个维度的加权平均：

1. **胸围-腰围差值比例**：反映体型曲线度
   - `|chest - waist| / chest`

2. **腰围-臀围差值比例**：反映体型上下差异
   - `|hip - waist| / hip`

3. **肩宽/身高比例偏离度**：反映体型匀称度
   - `|shoulder/height - 0.225| / 0.225`

**评分范围**：[0.0, 1.0]，分数越高表示体型越特殊。

### 热门款式列表

默认热门款式（路由到云端）：
- `DRESS`（连衣裙）
- `JACKET`（外套）
- `SUIT`（西装）
- `GOWN`（礼服）

### 特殊体型标记

以下 body_type 标记会被自动路由到云端：
- `plus_size`（大码）
- `petite`（娇小）
- `tall`（高挑）
- `athletic`（运动型）
- `irregular`（不规则）

---

## 任务调度机制

### 实时任务 vs 批处理任务

| 特性 | 实时任务 | 批处理任务 |
|------|---------|-----------|
| 触发方式 | 用户请求立即触发 | 定时调度（凌晨2:00-6:00） |
| 优先级 | 高 | 低 |
| 处理模型 | 分层模型路由 | 云端批量推理 |
| 结果存储 | 直接返回用户 | 缓存到 Redis |
| 超时 | 3s（本地）/ 30s（云端） | 无严格超时 |

### 调度任务列表

| 任务 | 调度频率 | 说明 |
|------|---------|------|
| `retryFailedTasks()` | 每 30 分钟 | 扫描并重试失败任务 |
| `cleanupExpiredCache()` | 每天 03:00 | 清理过期缓存 |
| `generateStatsReport()` | 每 6 小时 | 生成调度统计报告 |
| `executeBatchGeneration()` | 每天 02:00 | 非高峰批量预生成 |

### 开启/关闭调度

```yaml
tailoris:
  ai:
    scheduling:
      enabled: true   # 设为 false 关闭所有调度任务
```

---

## 非高峰批量预生成

### 工作原理

1. **触发时间**：默认每天凌晨 2:00（可通过 cron 配置修改）
2. **目标选择**：针对热门款式类型（DRESS, JACKET, SHIRT, PANTS）
3. **生成策略**：查询已有纸样基础，生成变体纸样
4. **缓存存储**：结果写入 Redis，key 格式：`ai:pattern:prewarm:{type}:{id}`
5. **缓存时效**：默认 24 小时过期
6. **峰值使用**：高峰时段用户请求优先查询预生成缓存

### 预生成缓存查询

```java
@Autowired
private OffPeakBatchGenerator offPeakBatchGenerator;

// 尝试从预生成缓存获取
PatternGenerateResponse cached = offPeakBatchGenerator
    .getCachedPattern("DRESS", basePatternId);

if (cached != null) {
    // 缓存命中，直接返回
    return cached;
}
// 缓存未命中，走正常生成流程
```

### 配置参数

```yaml
tailoris:
  ai:
    off-peak-batch:
      enabled: true
      cron: "0 0 2 * * ?"     # Cron 表达式
      batch-size: 50           # 每批次数量
      cache-ttl-hours: 24      # 缓存 TTL
      target-pattern-types:    # 目标类型
        - DRESS
        - JACKET
        - SHIRT
        - PANTS
```

---

## 配置说明

### 完整配置参考

```yaml
tailoris:
  ai:
    # ── 本地轻量模型 ──
    local-model:
      enabled: true                        # 是否启用本地模型
      model-path: /opt/tailoris/models/... # 模型文件路径
      gpu-enabled: true                    # GPU 加速
      gpu-device-id: 0                     # GPU 设备 ID
      batch-size: 8                        # 推理批量大小
      max-threads: 4                       # 最大线程数
      timeout-ms: 3000                     # 推理超时（毫秒）
      max-memory-mb: 2048                  # 内存上限
      fallback-to-cloud: true              # 降级开关
      health-check-interval-seconds: 60    # 健康检查间隔

    # ── 云端分布式模型 ──
    cloud-model:
      api-endpoints:                       # API 端点列表
        - https://ai.tailoris.com/v1/pattern
        - https://ai-backup.tailoris.com/v1/pattern
      api-key: ${CLOUD_MODEL_API_KEY:...}  # API 密钥（环境变量）
      timeout-ms: 30000                    # 请求超时
      connect-timeout-ms: 5000             # 连接超时
      max-retries: 3                       # 最大重试次数
      retry-delay-ms: 1000                 # 重试延迟
      retry-backoff-multiplier: 2.0        # 指数退避因子
      priority-queue-size: 500             # 优先级队列容量
      batch-size: 32                       # 云端批量大小
      circuit-breaker-threshold: 5         # 熔断阈值
      circuit-breaker-recovery-ms: 60000   # 熔断恢复时间
      load-balancing-enabled: true         # 负载均衡
      load-balance-strategy: ROUND_ROBIN   # 负载均衡策略

    # ── 任务调度 ──
    scheduling:
      enabled: true                        # 调度总开关
      off-peak-start-hour: 2               # 非高峰起始小时
      off-peak-end-hour: 6                 # 非高峰结束小时

    # ── 非高峰批量生成 ──
    off-peak-batch:
      enabled: true
      cron: "0 0 2 * * ?"
      batch-size: 50
      cache-ttl-hours: 24
      target-pattern-types:
        - DRESS
        - JACKET
```

---

## 性能基准

### 测试环境

- CPU: 8 Cores (Intel Xeon)
- Memory: 16 GB
- GPU: NVIDIA T4 (可选)
- Redis: 单节点 2GB
- MySQL: 8.0

### 基准结果

| 场景 | 模型路由 | 平均延迟 | P99 延迟 | 吞吐量 |
|------|---------|---------|---------|--------|
| 常规体型 | 本地 ONNX | 45ms | 120ms | 200 QPS |
| 复杂体型 | 云端 API | 480ms | 950ms | 50 QPS |
| 热门款式 | 缓存命中 | < 5ms | 15ms | 1000+ QPS |
| 批量预生成 | 云端 Batch | 2.3s/batch | - | 50 纸样/批 |

### 复杂度阈值调优

| 阈值 | 本地路由比例 | 云端路由比例 | 平均延迟 |
|------|------------|------------|---------|
| 0.20 | 40% | 60% | 320ms |
| **0.35**（默认） | **70%** | **30%** | **155ms** |
| 0.50 | 85% | 15% | 110ms |
| 0.70 | 95% | 5% | 80ms |

> **建议**：阈值越低，云端使用率越高，精度越好但延迟增加。0.35 是精度和延迟的平衡点。

---

## 调优指南

### 1. 本地模型性能调优

```yaml
# 增加批量大小提升吞吐
tailoris.ai.local-model.batch-size: 16

# 多 GPU 环境指定设备
tailoris.ai.local-model.gpu-device-id: 1
tailoris.ai.local-model.max-threads: 8
```

### 2. 云端 API 调优

```yaml
# 增加超时（慢速网络环境）
tailoris.ai.cloud-model.timeout-ms: 60000

# 调整重试策略
tailoris.ai.cloud-model.max-retries: 5
tailoris.ai.cloud-model.retry-delay-ms: 2000
```

### 3. 缓存调优

```yaml
# 延长预生成缓存时间
tailoris.ai.off-peak-batch.cache-ttl-hours: 48

# 增加批量预生成数量
tailoris.ai.off-peak-batch.batch-size: 100
```

### 4. Redis 性能

- 使用 Redis Cluster 而非单节点
- 确保 Redis 内存充足（建议 4GB+）
- 监控 Redis 内存使用率，及时淘汰过期数据

### 5. 数据库优化

- 纸样查询添加索引：`idx_pattern_type_status`
- 批量操作使用 MyBatis-Plus 的 `saveBatch`

---

## 故障排查

### 问题：本地模型调用失败

1. 检查模型文件是否存在：`ls -la /opt/tailoris/models/`
2. 查看健康状态日志：`grep "LocalModelHealthCheck" app.log`
3. 确认降级配置：`fallback-to-cloud: true`

### 问题：云端 API 熔断

1. 检查熔断状态日志：`grep "circuit" app.log`
2. 查看云端端点连通性：`curl -v https://ai.tailoris.com/v1/pattern`
3. 调整熔断阈值：增加 `circuit-breaker-threshold`

### 问题：批量预生成未触发

1. 确认功能已启用：`tailoris.ai.off-peak-batch.enabled: true`
2. 检查 Cron 表达式是否正确
3. 查看调度日志：`grep "OffPeakBatchGenerator" app.log`

### 问题：缓存命中率低

1. 确认预生成任务正常执行
2. 检查缓存 TTL 是否过短
3. 增加预生成的款式类型覆盖范围

---

## 关键文件索引

| 文件路径 | 说明 |
|---------|------|
| `ai/service/PatternGenerationStrategy.java` | 分层模型调用策略 |
| `ai/config/LocalModelConfig.java` | 本地模型配置 |
| `ai/config/CloudModelConfig.java` | 云端模型配置 |
| `ai/enums/ModelRoute.java` | 路由枚举 |
| `ai/scheduler/PatternTaskScheduler.java` | 任务调度器 |
| `ai/scheduler/OffPeakBatchGenerator.java` | 非高峰批量生成器 |
| `resources/application.yml` | 完整配置文件 |

---

*本文档由 Tailor IS Team 维护。如有疑问，请联系架构组。*
