# Tailor IS（裁智云）架构优化改造计划与开发方案

## 一、项目概述

### 1.1 项目背景

Tailor IS 平台原采用全量 Java SpringCloud 微服务架构，资源占用过大。本次架构优化通过 "分层轻量化、性能聚焦化、资源弹性化" 三大策略，在保留 "AI + 区块链 + 多商户商城" 核心壁垒的基础上，实现 **性能不降级、资源更高效、成本更可控** 的核心目标。

### 1.2 优化目标量化指标

| 指标维度 | 优化前 | 优化目标 | 行业基准 |
|---------|--------|---------|---------|
| 整体资源占用 | 100% | ≤60%（降低40%+） | 70% |
| AI 制版生成速度 | 基准 | 提升 40%+ | 20% |
| 区块链存证效率 | 基准 | 提升 50%+ | 30% |
| 交易订单处理峰值 | 基准 | 提升 50%+ | 30% |
| 运维成本 | 100% | ≤50%（降低50%+） | 70% |
| 非核心闲置资源 | 100% | ≤20%（降低80%+） | 50% |
| 首屏加载时间 | 基准 | 降低 2s+ | 1s |
| 核心接口响应时间 | ≤500ms | ≤200ms（P99≤500ms） | 300ms |

### 1.3 技术文档索引

| 序号 | 文档名称 | 路径 | 覆盖领域 |
|------|---------|------|---------|
| 1 | DATABASE-SHARDING-GUIDE.md | `tailor-is/docs/` | 数据库分片 + TiDB |
| 2 | CACHE-LAYERING-GUIDE.md | `tailor-is/docs/` | 缓存分层（Cluster/Standalone） |
| 3 | GATEWAY-SPLIT-GUIDE.md | `tailor-is/docs/` | 网关分层拆分 |
| 4 | MESSAGE-QUEUE-GUIDE.md | `tailor-is/docs/` | 消息队列双选型 |
| 5 | AI-PERFORMANCE-GUIDE.md | `tailor-is/docs/` | AI 性能优化 |
| 6 | BLOCKCHAIN-OPTIMIZATION-GUIDE.md | `tailor-is/docs/` | 区块链批量上链 |
| 7 | TRADING-OPTIMIZATION-GUIDE.md | `tailor-is/docs/` | 交易结算优化 |
| 8 | SERVERLESS-MIGRATION-GUIDE.md | `tailor-is/docs/` | Serverless 迁移 |
| 9 | FRONTEND-PERFORMANCE-GUIDE.md | `tailor-is-frontend/docs/` | 前端性能（SSR+GraphQL） |
| 10 | MONITORING-GUIDE.md | `tailor-is/docs/` | 分层监控体系 |
| 11 | K8S-DEPLOYMENT-GUIDE.md | `tailor-is/docs/` | K8s 弹性伸缩 |
| 12 | DATA-SYNC-GUIDE.md | `tailor-is/docs/` | 数据同步分层 |
| 13 | OFFLINE-CAPABILITY-GUIDE.md | `tailor-is-frontend/docs/` | 多端离线能力 |
| 14 | PERFORMANCE-TESTING-GUIDE.md | `tailor-is/docs/` | 全链路性能测试 |
| 15 | ARCHITECTURE.md | `tailor-is/docs/` | 总体架构设计 |

---

## 二、阶段性开发目标与关键里程碑

### 第一阶段：核心架构轻量化改造（Week 1-4）

**目标**：完成后端技术栈升级、数据库分片、缓存分层、网关拆分等核心基础设施改造

| 里程碑 | 时间节点 | 技术指标 | 业务指标 | 交付物 |
|--------|---------|---------|---------|--------|
| M1: Sentinel 替换完成 | W1 | Resilience4j 完全移除，Sentinel 6个服务生效 | 服务可用性≥99.9% | `SentinelGatewayConfig.java` + 6个服务配置 |
| M2: 数据库分片上线 | W2 | 订单表按商户ID分4片，ShardingSphere-JDBC生效 | 单商户订单查询延迟≤50ms | 分片配置 + `10_sharding_migration.sql` |
| M3: 缓存分层部署 | W2-3 | Redis Cluster 6节点，Standalone 2实例 | 缓存命中率≥85% | `CacheRouter.java` + `PatternCacheLoader.java` |
| M4: 双网关运行 | W3 | core-gateway:8080, lite-gateway:8081 独立运行 | 网关响应时间≤10ms | 2个新模块 + 前端网关配置 |
| M5: 消息队列双选型 | W3-4 | RocketMQ + RabbitMQ 双路并行 | AI任务投递延迟≤100ms | `RocketMqPatternProducer` + `MessageRoutingStrategy` |

**阶段验收标准**：
- 核心服务（order/payment/ai/copyright/merchant/product）全部通过 Sentinel 限流熔断测试
- 订单表分片后，跨分片查询正确，数据不丢失
- 缓存路由按服务类型正确选择 Redis 实例
- 前端请求正确路由至 core-gateway 或 lite-gateway

### 第二阶段：非核心服务轻量化（Week 5-8）

**目标**：完成非核心服务 Serverless 迁移、AI/区块链/交易核心性能优化

| 里程碑 | 时间节点 | 技术指标 | 业务指标 | 交付物 |
|--------|---------|---------|---------|--------|
| M6: AI 分层模型上线 | W5-6 | 本地模型响应≤50ms，云端模型响应≤500ms | 版型生成速度提升40%+ | `PatternGenerationStrategy.java` |
| M7: 批量上链运行 | W6 | 100条批量上链，链上交互减少90% | 存证成本降低98% | `BatchChainScheduler.java` |
| M8: 交易优化完成 | W7 | 热点订单缓存命中≥70%，批量结算每日运行 | 订单处理峰值提升50%+ | `HotOrderCache.java` + `BatchSettlementScheduler` |
| M9: Serverless 部署 | W7-8 | 冷启动≤2s，预留实例≥1 | 非核心闲置资源降低80%+ | SAM 模板 + FunctionHandler |
| M10: 前端 SSR 上线 | W8 | 首屏加载≤1.5s（原3.5s+） | 首屏加载时间降低2s+ | `entry-server.ts` + `server.ts` |

**阶段验收标准**：
- AI 常规体型生成延迟≤50ms，特殊体型≤500ms
- 批量上链定时任务正常触发，存证状态正确更新
- 批量结算每日凌晨2:00执行，结算准确率100%
- Serverless 冷启动≤2s，核心时段预留实例正常运行
- PC Mall SSR 首屏加载≤1.5s

### 第三阶段：全链路性能调优（Week 9-10）

**目标**：完成分层监控、离线能力、K8s 部署、数据同步分层、全链路压测

| 里程碑 | 时间节点 | 技术指标 | 业务指标 | 交付物 |
|--------|---------|---------|---------|--------|
| M11: 监控体系运行 | W9 | 核心服务全维度监控，告警响应≤5min | 故障发现时间≤3min | 3个 Grafana 仪表板 + 告警规则 |
| M12: 离线能力上线 | W9 | 离线工单创建成功，网络恢复自动同步 | 弱网用户操作成功率≥90% | `offlineStorage.ts` + `autoSync.ts` |
| M13: K8s 弹性伸缩 | W9-10 | HPA 自动扩缩容，闲时缩容50%+ | 资源利用率提升40%+ | K8s Deployment/Service/HPA YAML |
| M14: 数据同步分层 | W10 | 核心数据实时，非核心5min间隔 | 同步延迟符合预期 | `DataSyncRouter.java` |
| M15: 全链路压测通过 | W10 | 资源占用≤60%，核心性能达标 | 所有优化目标达成 | JMX 测试计划 + HTML 报告 |

**阶段验收标准**：
- 核心服务异常5分钟内告警
- 离线工单网络恢复后自动同步成功率≥99%
- K8s HPA 在CPU≥70%时自动扩容
- 全链路压测通过，所有量化指标达成

---

## 三、详细任务分解与资源分配

### 3.1 任务分解矩阵

#### Phase 1: 核心架构轻量化（12个子任务）

| 任务ID | 任务名称 | 负责人 | 工时 | 前置依赖 | 交付物 |
|--------|---------|--------|------|---------|--------|
| T1.1 | SpringCloud Alibaba 替换 | 后端架构师 | 3d | 无 | Sentinel配置 + POM更新 |
| T1.2 | 数据库分库分表方案设计 | DBA + 后端 | 3d | 无 | 分片方案文档 + 迁移脚本 |
| T1.3 | ShardingSphere-JDBC集成 | 后端开发 | 2d | T1.2 | 分片配置 + 路由组件 |
| T1.4 | Redis Cluster 部署配置 | 运维 | 2d | 无 | Cluster配置 + 连接池 |
| T1.5 | 缓存路由组件开发 | 后端开发 | 2d | T1.4 | `CacheRouter.java` |
| T1.6 | core-gateway 模块创建 | 后端架构师 | 3d | T1.1 | 核心网关模块 |
| T1.7 | lite-gateway 模块创建 | 后端开发 | 2d | T1.1 | 轻量网关模块 |
| T1.8 | 前端网关URL配置 | 前端开发 | 1d | T1.6,T1.7 | 四端环境变量更新 |
| T1.9 | RocketMQ 部署与集成 | 运维 + 后端 | 3d | 无 | Producer/Consumer |
| T1.10 | 消息路由策略实现 | 后端开发 | 2d | T1.9 | `MessageRoutingStrategy` |
| T1.11 | TiDB 连接配置 | DBA | 1d | T1.2 | TiDB配置模板 |
| T1.12 | Phase 1 集成测试 | QA | 2d | T1.1-T1.11 | 集成测试报告 |

#### Phase 2: 非核心服务轻量化（10个子任务）

| 任务ID | 任务名称 | 负责人 | 工时 | 前置依赖 | 交付物 |
|--------|---------|--------|------|---------|--------|
| T2.1 | AI 本地模型配置 | AI工程师 | 3d | 无 | `LocalModelConfig` |
| T2.2 | AI 分层调用策略实现 | AI工程师 | 3d | T2.1 | `PatternGenerationStrategy` |
| T2.3 | AI 批量预生成调度 | 后端开发 | 2d | T2.2 | `OffPeakBatchGenerator` |
| T2.4 | 区块链哈希缓存实现 | 后端开发 | 2d | T1.5 | `HashGenerationService` |
| T2.5 | 批量上链调度器实现 | 后端开发 | 2d | T2.4 | `BatchChainScheduler` |
| T2.6 | 本地相似度比对 | AI工程师 | 3d | 无 | `LocalSimilarityService` |
| T2.7 | 订单分片逻辑实现 | 后端开发 | 2d | T1.3 | `OrderShardingService` |
| T2.8 | 热点订单缓存实现 | 后端开发 | 2d | T1.5 | `HotOrderCache.java` |
| T2.9 | 批量结算调度器 | 后端开发 | 2d | T1.9 | `BatchSettlementScheduler` |
| T2.10 | Phase 2 集成测试 | QA | 3d | T2.1-T2.9 | 集成测试报告 |

#### Phase 3: 全链路调优（8个子任务）

| 任务ID | 任务名称 | 负责人 | 工时 | 前置依赖 | 交付物 |
|--------|---------|--------|------|---------|--------|
| T3.1 | 核心服务 Grafana 仪表板 | 运维 | 2d | 无 | 核心监控JSON |
| T3.2 | 轻量服务 Grafana 仪表板 | 运维 | 1d | 无 | 轻量监控JSON |
| T3.3 | Prometheus 分层配置 | 运维 | 1d | T3.1,T3.2 | prometheus.yml更新 |
| T3.4 | 离线工单编辑开发 | 移动端开发 | 3d | 无 | `offline-aftersale.vue` |
| T3.5 | 自动同步机制实现 | 移动端开发 | 2d | T3.4 | `autoSync.ts` |
| T3.6 | K8s Deployment/HPA 编写 | DevOps | 3d | T1.6 | K8s YAML模板 |
| T3.7 | 数据同步分层实现 | 后端架构师 | 2d | T1.3,T1.5 | `DataSyncRouter.java` |
| T3.8 | 全链路压测执行 | QA + 运维 | 3d | T1-T3全部 | 压测报告 |

### 3.2 资源分配

| 角色 | 人数 | 职责 | 参与阶段 |
|------|------|------|---------|
| 后端架构师 | 1 | 架构设计、核心模块开发 | Phase 1-3 |
| 后端开发 | 3 | 服务改造、缓存/消息/同步 | Phase 1-3 |
| AI工程师 | 1 | 模型分层、相似度比对 | Phase 2 |
| 前端开发 | 2 | SSR、GraphQL、离线能力 | Phase 2-3 |
| 移动端开发 | 1 | 离线工单、弱网适配 | Phase 3 |
| DBA | 1 | 分片方案、TiDB迁移 | Phase 1 |
| 运维/DevOps | 1 | K8s、监控、Serverless | Phase 1-3 |
| QA | 2 | 测试方案、压测、安全测试 | Phase 1-3 |

### 3.3 任务依赖图

```
Phase 1:
T1.1 ─┬─→ T1.6 ──→ T1.8
      ├─→ T1.7 ──┘
      └─→ T1.2 ──→ T1.3 ──→ T2.7
                └─→ T1.11

T1.4 ──→ T1.5 ──┬─→ T2.4 ──→ T2.5
                ├─→ T2.8
                └─→ T3.7

T1.9 ──→ T1.10
T1.10 ──→ T2.9
T1.1-T1.11 ──→ T1.12

Phase 2:
T2.1 ──→ T2.2 ──→ T2.3
T2.4 ──→ T2.5
T2.6 ──┘
T2.7,T2.8,T2.9 ──→ T2.10

Phase 3:
T3.1,T3.2 ──→ T3.3
T3.4 ──→ T3.5
T1.6 ──→ T3.6
T1.3,T1.5 ──→ T3.7
全部任务 ──→ T3.8
```

---

## 四、核查验证要求

### 4.1 单元测试

| 模块 | 范围 | 工具 | 通过标准 | 负责人 |
|------|------|------|---------|--------|
| Sentinel 限流 | Gateway + 6核心服务限流规则 | JUnit 5 + Mockito | 覆盖率≥90%，限流触发准确 | QA |
| 缓存路由 | `CacheRouter`、`@CoreCache`/`@LiteCache` | JUnit 5 + Redis Embedded | 覆盖率≥90%，路由准确 | QA |
| 分片路由 | `OrderShardingService`、ShardingSphere | JUnit 5 + H2 | 覆盖率≥90%，分片正确 | QA |
| 消息路由 | `MessageRoutingStrategy`、Producer/Consumer | JUnit 5 + TestContainers | 覆盖率≥85%，路由准确 | QA |
| AI 分层策略 | `PatternGenerationStrategy` | JUnit 5 + Mockito | 覆盖率≥90%，路由准确 | QA |
| 批量上链 | `BatchChainScheduler`、`HashGenerationService` | JUnit 5 + Redis Embedded | 覆盖率≥85%，批量正确 | QA |
| 热点缓存 | `HotOrderCache` | JUnit 5 + Redis Embedded | 覆盖率≥90%，命中有效 | QA |
| 数据同步 | `DataSyncRouter`、`DataConsistencyValidator` | JUnit 5 + Mockito | 覆盖率≥85% | QA |

**执行频率**：每次 PR 合并前自动执行（CI 流水线）

### 4.2 集成测试

| 场景 | 范围 | 工具 | 通过标准 | 负责人 |
|------|------|------|---------|--------|
| 网关路由 | core/lite-gateway 路由转发 | Playwright + TestContainers | 路由准确率100% | QA |
| 订单流程 | 创建→支付→结算→分账 | TestContainers + RabbitMQ | 端到端成功率100% | QA |
| AI 制版 | 本地/云端模型调用切换 | TestContainers + Mock | 模型切换准确率100% | QA |
| 区块链存证 | 上传→哈希→批量上链 | TestContainers + Mock Chain | 存证成功率100% | QA |
| 离线同步 | 离线创建→联网同步 | Playwright + Network Throttling | 同步成功率≥99% | QA |

**执行频率**：每日夜间自动执行（CI 定时任务）

### 4.3 性能测试

| 场景 | 工具 | 并发量 | 目标指标 | 负责人 |
|------|------|--------|---------|--------|
| AI 制版生成 | JMeter | 50/100/200 | P95≤200ms（常规），P95≤500ms（复杂） | QA |
| 高并发交易 | JMeter | 100/500/1000 | TPS≥1000，P99≤500ms | QA |
| 区块链存证 | JMeter | 50/100 | 批量上链耗时≤30s（100条） | QA |
| 网关响应 | JMeter | 1000 | P95≤10ms | QA |
| 缓存命中率 | 自定义脚本 | - | ≥85% | QA |
| 首屏加载 | Lighthouse | - | ≤1.5s | 前端QA |
| 资源占用 | monitor-resources.sh | - | CPU≤60%，内存≤60% | 运维 |

**执行频率**：Phase 3 集中执行，此后每月定期执行

### 4.4 安全测试

| 测试项 | 工具 | 通过标准 | 负责人 |
|--------|------|---------|--------|
| SQL注入防护 | SQLMap + 手动验证 | 0个可注入点 | 安全团队 |
| XSS防护 | Burp Suite + 手动验证 | 0个XSS漏洞 | 安全团队 |
| CSRF防护 | Burp Suite | Token验证100%生效 | 安全团队 |
| JWT鉴权 | OWASP ZAP | 双Token刷新正常 | 安全团队 |
| 数据加密 | 手动验证 | 敏感字段AES-256加密 | 安全团队 |
| 接口限流 | JMeter | Sentinel限流生效 | QA |
| 渗透测试 | 第三方安全团队 | 无高危漏洞 | 安全团队 |

**通过标准**：安全防护达到 **行业企业级标准**，通过第三方安全渗透测试，无高危/中危漏洞。

### 4.5 UI/UX 验证

| 验证项 | 方法 | 通过标准 | 负责人 |
|--------|------|---------|--------|
| 界面完整性 | UI走查 | 所有功能模块完整呈现 | 前端QA |
| 响应式适配 | BrowserStack | 320px~2560px全尺寸适配 | 前端QA |
| 无障碍访问 | axe DevTools | WCAG 2.1 AA级 | 前端QA |
| 操作路径 | 用户测试 | 核心流程≤3步完成 | UX设计师 |
| 弱网体验 | Chrome DevTools | 3G网络下核心功能可用 | 移动端QA |
| 用户满意度 | 问卷调研 | ≥4.5/5分 | 产品团队 |

---

## 五、风险识别与应对策略

### 5.1 技术风险

| 风险项 | 影响等级 | 概率 | 预防措施 | 回滚方案 | 负责人 |
|--------|---------|------|---------|---------|--------|
| Sentinel 替换 Resilience4j 导致限流失效 | 高 | 中 | 灰度发布，双限流并存1周 | 回退至 Resilience4j | 后端架构师 |
| ShardingSphere 分片后跨分片查询性能下降 | 高 | 中 | 分片前完成SQL审计，优化查询语句 | 回退至不分片模式 | DBA |
| Redis Cluster 脑裂导致数据不一致 | 高 | 低 | 配置合理的cluster-node-timeout，监控节点状态 | 降级为单机Redis | 运维 |
| 双网关拆分后前端路由错误 | 中 | 中 | 前端增加URL回退机制，统一网关发现服务 | 回退至单网关 | 前端开发 |
| RocketMQ 部署失败影响 AI 任务 | 中 | 低 | 保留 RabbitMQ 作为 AI 任务 fallback | 切换回 RabbitMQ | 后端开发 |
| TiDB 与 MySQL 兼容性差异 | 中 | 低 | 充分测试 TiDB 特有语法和限制 | 回退至 MySQL | DBA |
| Serverless 冷启动延迟影响用户体验 | 中 | 高 | 核心时段预留实例≥1，前端加载态优化 | 回退至容器部署 | 运维 |
| SSR 首屏渲染服务器压力增大 | 低 | 中 | SSR 层增加缓存，配置 Nginx 缓存策略 | 回退至 CSR | 前端开发 |

### 5.2 业务风险

| 风险项 | 影响等级 | 概率 | 预防措施 | 回滚方案 | 负责人 |
|--------|---------|------|---------|---------|--------|
| 架构升级期间服务中断 | 高 | 低 | 蓝绿部署，服务零停机切换 | 立即回滚至旧版本 | 运维 |
| 批量结算延迟导致商户投诉 | 中 | 中 | 商户提前公告，提供手动触发入口 | 切换回实时结算 | 后端开发 |
| 缓存分层导致数据读取异常 | 中 | 低 | 缓存预热充分，读取异常自动降级查DB | 统一使用Redis | 后端开发 |
| 数据同步分层导致数据延迟 | 低 | 中 | 前端增加"数据更新中"提示 | 切换为实时同步 | 后端架构师 |

### 5.3 回滚策略总览

| 阶段 | 回滚粒度 | 回滚时间 | 数据影响 |
|------|---------|---------|---------|
| Phase 1 | 服务级别 | ≤30min/服务 | 无数据丢失（分片需数据迁移回滚） |
| Phase 2 | 功能级别 | ≤15min/功能 | 无数据丢失 |
| Phase 3 | 配置级别 | ≤10min/配置 | 无数据丢失 |

**通用回滚流程**：
1. 发现异常 → 触发监控告警
2. 评估影响范围 → 决策是否回滚
3. 执行回滚 → 验证回滚结果
4. 数据校验 → 确认数据一致性
5. 记录回滚原因 → 问题修复后重新上线

---

## 六、交付验收标准

### 6.1 业务需求满足度

- **核心功能完整度**：系统核心功能模块（AI制版、区块链存证、多商户交易、用户管理、商户管理、商品管理、订单管理、支付结算、营销管理、社区、学堂、供应链、私信）完整无缺
- **功能测试覆盖率**：100% 功能测试用例通过
- **验收方式**：基于 `checklist.md` 逐项验证，所有检查项通过

### 6.2 性能与安全性

| 指标 | 优化前 | 目标 | 验证方式 |
|------|--------|------|---------|
| 核心接口响应时间 | ≤500ms | ≤200ms（P99≤500ms） | JMeter 压测 |
| 订单处理峰值TPS | 基准 | 提升≥50% | JMeter 交易测试 |
| AI 制版生成速度 | 基准 | 提升≥40% | JMeter AI测试 |
| 区块链存证效率 | 基准 | 提升≥50% | JMeter 区块链测试 |
| 整体资源占用 | 100% | ≤60% | monitor-resources.sh |
| 安全防护等级 | 基础 | 企业级标准 | 第三方渗透测试 |
| 高危漏洞数 | - | 0 | 安全扫描 |

### 6.3 UI/UX 质量标准

| 指标 | 标准 | 验证方式 |
|------|------|---------|
| 界面设计 | Element Plus 设计规范 | UI走查 |
| 响应式适配 | 320px~2560px全尺寸 | BrowserStack |
| 无障碍访问 | WCAG 2.1 AA级 | axe DevTools |
| 操作路径 | 核心流程≤3步 | 用户测试 |
| 首屏加载 | ≤1.5s | Lighthouse |
| 用户满意度 | ≥4.5/5分 | 问卷调研 |
| 弱网可用性 | 3G网络核心功能可用 | Chrome DevTools |

### 6.4 验收流程

```
开发完成 → 自测通过 → 代码审查 → 单元测试 → 集成测试
    ↓
性能测试 → 安全测试 → UI/UX验收 → 用户验收测试(UAT)
    ↓
上线评审 → 灰度发布(10%→50%→100%) → 全量上线
    ↓
上线后监控(7天) → 验收报告 → 项目归档
```

---

## 七、项目时间线总览

```
Week 1    Week 2    Week 3    Week 4    Week 5    Week 6    Week 7    Week 8    Week 9    Week 10
│─────────┤         │         │         │         │         │         │         │         │
│ Phase 1: 核心架构轻量化改造                                                        │
│  M1     M2        M3        M4        M5                                          │
│─────────┼─────────┼─────────┼─────────┼─────────┤                                   │
│                                     Phase 2: 非核心服务轻量化                      │
│                                     M6        M7        M8        M9        M10     │
│                                               │         │         │         │       │
│                                               └─────────┴─────────┴─────────┤       │
│                                                             Phase 3: 全链路调优  │
│                                                             M11       M12  M13 M14 M15
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 八、沟通与汇报机制

| 会议类型 | 频率 | 参与人 | 内容 |
|---------|------|--------|------|
| 每日站会 | 每日15min | 全体开发 | 进度同步、阻塞问题 |
| 周评审会 | 每周五 | 全体 + 产品 | 本周成果、下周计划 |
| 里程碑评审 | 每阶段末 | 全体 + 管理层 | 阶段验收、指标达成 |
| 风险评审 | 每两周 | 架构师 + 运维 + QA | 风险评估、应对策略 |

## 九、附录

### 9.1 关键配置文件清单

| 文件 | 路径 | 用途 |
|------|------|------|
| 架构优化 Spec | `.trae/specs/tailor-is-arch-optimization/spec.md` | 需求规格 |
| 任务清单 | `.trae/specs/tailor-is-arch-optimization/tasks.md` | 任务分解 |
| 质量检查清单 | `.trae/specs/tailor-is-arch-optimization/checklist.md` | 验收标准 |
| 父 POM | `tailor-is/pom.xml` | 依赖管理 |
| core-gateway | `tailor-is/tailor-is-core-gateway/` | 核心网关 |
| lite-gateway | `tailor-is/tailor-is-lite-gateway/` | 轻量网关 |
| K8s 模板 | `tailor-is/deploy/k8s/` | 容器编排 |
| 监控配置 | `tailor-is/deploy/monitoring/` | Grafana/Prometheus |
| 性能测试 | `tailor-is/deploy/performance-test/` | JMeter 测试计划 |
| Serverless | `tailor-is/deploy/serverless/` | 函数计算模板 |

### 9.2 技术文档清单

| 序号 | 文档 | 路径 |
|------|------|------|
| 1 | 数据库分片指南 | `tailor-is/docs/DATABASE-SHARDING-GUIDE.md` |
| 2 | 缓存分层指南 | `tailor-is/docs/CACHE-LAYERING-GUIDE.md` |
| 3 | 网关拆分指南 | `tailor-is/docs/GATEWAY-SPLIT-GUIDE.md` |
| 4 | 消息队列指南 | `tailor-is/docs/MESSAGE-QUEUE-GUIDE.md` |
| 5 | AI 性能优化指南 | `tailor-is/docs/AI-PERFORMANCE-GUIDE.md` |
| 6 | 区块链优化指南 | `tailor-is/docs/BLOCKCHAIN-OPTIMIZATION-GUIDE.md` |
| 7 | 交易优化指南 | `tailor-is/docs/TRADING-OPTIMIZATION-GUIDE.md` |
| 8 | Serverless 迁移指南 | `tailor-is/docs/SERVERLESS-MIGRATION-GUIDE.md` |
| 9 | 前端性能指南 | `tailor-is-frontend/docs/FRONTEND-PERFORMANCE-GUIDE.md` |
| 10 | 监控体系指南 | `tailor-is/docs/MONITORING-GUIDE.md` |
| 11 | K8s 部署指南 | `tailor-is/docs/K8S-DEPLOYMENT-GUIDE.md` |
| 12 | 数据同步指南 | `tailor-is/docs/DATA-SYNC-GUIDE.md` |
| 13 | 离线能力指南 | `tailor-is-frontend/docs/OFFLINE-CAPABILITY-GUIDE.md` |
| 14 | 性能测试指南 | `tailor-is/docs/PERFORMANCE-TESTING-GUIDE.md` |
| 15 | 总体架构设计 | `tailor-is/docs/ARCHITECTURE.md` |
