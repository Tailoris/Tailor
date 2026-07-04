# Tailor IS — 项目后续工作计划

> 版本: v1.0 | 日期: 2026-06-27 | 基于: 四维度系统性全面核查报告 + 综合问题清单 (89项) + 架构优化方案

---

## 一、背景与总体目标

### 1.1 核查核心结论

| 维度 | 健康度 | 核心发现 |
|------|:------:|----------|
| 架构设计 | ⭐⭐⭐⭐⭐ | 21微服务 + 双网关 + 双MQ + GraphQL，架构方案成熟 |
| 代码质量 | ⭐⭐⭐⭐☆ | 工具链完善，20处死代码/Stub（占组件30%）需激活 |
| 测试覆盖 | ⭐⭐⭐⭐☆ | 193单元测试 + 16 E2E + k6，缺契约测试与异常路径 |
| CI/CD | ⭐⭐⭐⭐⭐ | 8个 Actions 工作流，端到端自动化 |
| 安全性 | ⭐⭐⭐⭐⭐ | Gitleaks + OWASP + Trivy + Vault + Nginx |
| 部署 | ⭐⭐⭐⭐☆ | Docker Compose + K8s 双方案，脚本分散需 GitOps 化 |
| 监控 | ⭐⭐⭐⭐⭐ | Prometheus + Grafana + Alertmanager + SkyWalking |
| 文档 | ⭐⭐⭐⭐⭐ | 完备的全流程中英文技术文档 |

### 1.2 待解决问题总览

| 优先级 | 数量 | 定义 | 处理时限 |
|--------|:---:|------|---------|
| **P0 紧急** | 8 | 阻断核心功能/资金安全，修复成本低 | 立即 (1-3天) |
| **P1 高优** | 20 | 架构偏离/功能缺失/数据流断裂 | 本周 (1周内) |
| **P2 中优** | 24 | 优化项/非核心功能/配置完善 | 本月 (1月内) |
| **P3 低优** | 11 | 改进项/文档/代码清理 | 季度规划 (3月内) |

> 合计 63 项待处理（原有89项中，26项已在近期提交中修复）

### 1.3 总体目标

1. **P0 阶段** (3天)：消除所有阻断生产部署的阻断项，恢复核心业务链路
2. **P1 阶段** (1周)：修复架构偏离，激活死代码，打通数据流断裂点
3. **P2 阶段** (1月)：完成优化项，完善部署配置与监控体系
4. **P3 阶段** (季度)：持续改进代码质量、文档、测试覆盖

---

## 二、P0 — 紧急阻断项修复（1-3天）

### 2.1 任务清单

| ID | 问题 | 影响 | 涉及文件 | 修复方案 | 预计工时 |
|----|------|------|---------|---------|:-------:|
| P0-01 | **售后退款不退款不恢复库存** | 用户资金损失，库存永久占用 | `AfterSaleServiceImpl.java` | 注入 PaymentClient 执行退款 + InventoryClient 恢复库存 | 4h |
| P0-02 | **多商户数据隔离完全失效** | 全平台数据泄露，SaaS 安全底线失守 | `DataPermissionHandlerImpl.java` | 实现 `getSelectTable` SQL 解析，通过 ThreadLocal + 注解完成隔离 | 6h |
| P0-03 | **支付回调不更新订单状态** | 支付成功订单仍显示"待支付"，发货阻断 | `PaymentCallbackAsyncHandler.java` | `notifyOrderService` 改为调用订单状态更新接口 | 2h |
| P0-04 | **批量结算 Exchange 未声明，消息丢失** | 商家永远无法收到结算款 | `RabbitMQConfig.java` | 补充 `settlement.batch.exchange` 声明与 Binding | 1h |
| P0-05 | **5 个 MQ 消费者是 Stub（桩代码）** | 结算/库存/推送/通知/同步五大链路断裂 | 5 个消费者类 | 按优先级实现真实业务逻辑 | 8h |
| P0-06 | **K8s Secret 占位符未替换** | 基础服务无法连接，系统不可用 | `deploy/k8s/production/secrets.yaml` | GitHub Secrets + CI/CD 动态注入 | 2h |
| P0-07 | **微信/支付宝支付凭证为空** | 支付功能不可用 | `secrets.yaml` | 配置真实支付凭证（通过 Vault/Secrets） | 2h |
| P0-08 | **Prometheus 端口配置错误** | 全部业务服务监控失效 | `deploy/prometheus/prometheus.yml` | 端口从 18080 改为实际 containerPort | 1h |

### 2.2 依赖关系与执行顺序

```
Day 1:
  P0-02 (数据隔离) ─────────────────────────────────────┐
  P0-01 (售后退款) ─┐                                    │
  P0-03 (支付回调) ─┼──→  P0-05 (MQ消费者)               │
  P0-04 (结算MQ)  ──┘                                    │
                                                         ├──→ Day 3 集成验证
  Day 2:                                                  │
  P0-06 (Secret注入) ─→ P0-07 (支付凭证) ─→ P0-08 (监控) ┘
```

### 2.3 验收标准

| 验收项 | 标准 |
|--------|------|
| 售后退款 | 审批通过后资金原路退回，库存自动恢复 |
| 数据隔离 | 商户A登录无法查询商户B的任何数据 |
| 支付回调 | 支付成功后订单状态自动变为"已支付" |
| 批量结算 | 结算消息正确投递，消费者正确处理 |
| Secret 注入 | `kubectl get secrets` 所有值非占位符 |
| Prometheus 监控 | `targets` 页面所有服务状态为 UP |

---

## 三、P1 — 架构偏离修复（1周）

### 3.1 后端核心功能修复

| ID | 问题 | 修复方案 | 工时 |
|----|------|---------|:---:|
| P1-01 | **AI 批量任务调度未集成** — RocketMQ 条件 Bean 不满足 | 部署 RocketMQ，移除 `@ConditionalOnBean`，激活 Producer/Consumer | 4h |
| P1-02 | **版权批量上链默认关闭** — `batchChainEnabled=false` | 配置中心设 `batchChainEnabled=true`，并在 `CopyrightServiceImpl` 注入 `addToPendingQueue` | 3h |
| P1-03 | **订单热点缓存未集成** — `HotOrderCache` 无业务调用 | `OrderServiceImpl` 查询前调用 `HotOrderCache.get()`，写入时调用 `put()` | 3h |
| P1-04 | **订单分片路由未集成** — `OrderShardingService` 无调用方 | 激活 `application-sharding.yml`，在订单写入/查询前调用 `getShardIndex` | 4h |
| P1-05 | **Seata 分布式事务禁用** — `seata.enabled=false` | 部署 Seata Server，设 `seata.enabled=true`，补充 `@GlobalTransactional` | 4h |
| P1-06 | **用户注册未分配角色与权限** — 注册后无权限 | `register()` 成功后插入默认角色和权限记录 | 2h |

### 3.2 前端功能修复

| ID | 问题 | 修复方案 | 工时 |
|----|------|---------|:---:|
| P1-07 | **商家后台接口聚合未实现** — 单页请求5-8次 | 实现 BFF 聚合层或 GraphQL 批量 DataLoader | 8h |
| P1-08 | **平台管理后台权限管理不完整** — 缺角色/用户管理 | 补充角色管理、权限分配、用户管理、审计日志页面 | 12h |

### 3.3 数据流转修复

| ID | 问题 | 修复方案 | 工时 |
|----|------|---------|:---:|
| P1-09 | **版权批量上链队列无生产者** | `CopyrightServiceImpl` 创建版权记录时调用 `addToPendingQueue` + 阈值检查 | 2h |
| P1-10 | **AI 制版生产者无调用方** | `PatternServiceImpl` 异步模式调用 `patternTaskProducer` | 2h |
| P1-11 | **3个队列无生产者** | 在支付回调/订单取消/消息触发处注入生产者 | 4h |
| P1-12 | **数据同步框架零业务集成** | 在订单/商品/资金变更处注入 `RealTimeDataSync.publishAfterCommit()` | 6h |
| P1-13 | **MultiLevelCache 未集成** | 在商品详情/用户信息高频读取处注入二级缓存 | 4h |
| P1-14 | **商品创建无 MQ 通知下游** | `ProductServiceImpl` 变更后发送 MQ 消息通知搜索/推荐/分析 | 3h |

### 3.4 部署架构修复

| ID | 问题 | 修复方案 | 工时 |
|----|------|---------|:---:|
| P1-15 | **核心网关缺少 Sentinel 流量管控** | core-gateway POM 添加 Sentinel 依赖并配置网关流控规则 | 3h |
| P1-16 | **动态路由未实现（硬编码路由）** | 迁移至 Nacos 配置中心动态路由 | 6h |
| P1-17 | **K8s 部署目录两套并行** | 合并为单一 K8s 部署目录，统一引用 `deploy/k8s/production/` | 4h |
| P1-18 | **Nacos/RocketMQ K8s 部署缺失** | 补充 StatefulSet/Service/ConfigMap 配置 | 6h |
| P1-19 | **MySQL 单节点无高可用** | 部署 MySQL Group Replication 或 Orchestrator 故障转移，配置 CronJob 备份 | 8h |
| P1-20 | **ShardingSphere 分片覆盖不全** | 扩展分片配置到 product/user/copyright/marketing 服务 | 8h |

---

## 四、P2 — 优化项（1月）

| ID | 问题 | 修复方案 | 优先级 | 工时 |
|----|------|---------|:-----:|:---:|
| P2-01 | **TiDB 配置存在但未激活** | 取消注释 `tidb.enabled`，激活 TiDB profile | High | 1h |
| P2-02 | **GraphQL 缓存层未实现** | resolver 中集成 DataLoader + Redis 缓存 | High | 4h |
| P2-03 | **移动端离线能力未完整实现** | 完成 Service Worker 注册与离线缓存策略 | Medium | 6h |
| P2-04 | **PC 端 SSR 生产构建未完成** | 添加 SSR 构建脚本，配置 Vite SSR 模式 | Medium | 6h |
| P2-05 | **移动端未集成小程序原生组件** | 实现微信小程序原生组件封装 | Low | 8h |
| P2-06 | **轻量服务未按方案采用单体架构** | community/academy/supply 重构为 SpringBoot 单体 | Medium | 24h |
| P2-07 | **批量结算 MQ 选型确认** | 评估 RabbitMQ 是否满足批量需求，否则迁移 RocketMQ | Medium | 4h |
| P2-08 | **Docker Compose 使用 127.0.0.1** | 改为 Docker 服务名 | High | 1h |
| P2-09 | **HTTPS 默认禁用** | 激活 HTTPS 配置，申请正式 SSL 证书 | High | 4h |
| P2-10 | **Redis K8s 单节点无高可用** | 部署 Redis Sentinel 或 Cluster | High | 8h |
| P2-11 | **payment 分片表 DDL 缺失** | 补充 payment_record / refund_record / settlement_record 分片 DDL | High | 2h |
| P2-12 | **CD 流水线覆盖不全 (9/18+)** | 补充全部 18+ 服务的 CD 配置 | High | 6h |
| P2-13 | **核心服务 K8s 部署配置不全** | 补充 user/product/merchant 等缺失服务的部署配置 | High | 6h |
| P2-14 | **Serverless 迁移不完整** | 补充 serverless.yml 框架配置、触发器、冷启动优化 | Medium | 8h |
| P2-15 | **Grafana 数据源未配置认证** | 配置 Prometheus 数据源认证 | Medium | 1h |
| P2-16 | **Vault 部署配置存在但未集成** | 启用 Vault 替代 K8s Secret | Medium | 8h |
| P2-17 | **CDN 加速未实际接入** | 接入 CDN 服务商边缘节点 | Low | 4h |
| P2-18 | **Nacos 认证未启用** | 强制设置 NACOS_AUTH_TOKEN | Medium | 1h |
| P2-19 | **SkyWalking 采样率未配置** | 配置采样率（核心 100%，非核心 10%） | Low | 1h |
| P2-20 | **OWASP Dependency Check 未集成 CI** | `security-scan.yml` 添加 OWASP DC 步骤 | Medium | 2h |
| P2-21 | **SonarQube 未实际接入** | 部署 SonarQube Server 并激活 CI | Medium | 4h |
| P2-22 | **E2E 测试场景覆盖不全** | 补充售后/退款/IM/营销 E2E 测试 | Medium | 8h |
| P2-23 | **k6 压测脚本未执行** | 在预发布环境执行全套压测 | Medium | 4h |
| P2-24 | **运维 Runbook 不完整** | 补充故障应急 SOP、回滚步骤、数据恢复步骤 | Low | 4h |

---

## 五、P3 — 改进项（季度规划）

| ID | 问题 | 修复方案 | 工时 |
|----|------|---------|:---:|
| P3-01 | 过期文档归档 | 历史报告移至 `docs/archive/` | 2h |
| P3-02 | 前端 console.log 未移除 | 构建工具配置移除 console | 1h |
| P3-03 | mock-server.js 未排除 | `.dockerignore` 排除 mock 文件 | 0.5h |
| P3-04 | @Deprecated 方法清理 | 确认无调用方后删除 | 4h |
| P3-05 | placeholder 图片替换 | 外部占位图替换为本地资源 | 3h |
| P3-06 | 全局错误边界 | 添加 errorBoundary 全局组件 | 2h |
| P3-07 | i18n 翻译补充 | 补充缺失的英文翻译 key | 4h |
| P3-08 | CHANGELOG 自动更新 | 配置 release-please 或 standard-version | 2h |
| P3-09 | WCAG 2.1 AA 全面测试 | 执行全站无障碍扫描与修复 | 8h |
| P3-10 | 多平台兼容性测试 | iOS/Android/各小程序全面测试 | 8h |
| P3-11 | TypeScript `any` 类型清理 | 补充具体类型定义 | 6h |

---

## 六、里程碑与时间线

### 6.1 总路线图

```
Day 1    Day 2    Day 3    Week 2    Week 3    Week 4    Month 2-3
│        │        │        │         │         │         │
├─ P0 ───┤        │        │         │         │         │
│ 紧急阻断       │        │         │         │         │
├─────────── P1 ──┼────────┼─────────┤         │         │
│        架构偏离修复       │         │         │         │
│                       ├──── P2 ────┼─────────┤         │
│                            优化项与配置完善     │         │
│                                            ├── P3 ────┤
│                                              持续改进项  │
└────────────────────────────────────────────────────────┘
```

### 6.2 里程碑定义

| 里程碑 | 时间 | 交付目标 | 验收标准 |
|--------|:---:|---------|---------|
| **M0** | Day 3 | P0 全部修复 | 8 项阻断验收全部通过 |
| **M1** | W2 | P1 后端核心 6 项完成 | AI/版权/缓存/分片/Seata/权限全部就绪 |
| **M2** | W2 | P1 数据流 6 项完成 | 5 生产者 + 数据同步 + 多级缓存全部激活 |
| **M3** | W3 | P1 部署 6 项完成 | Sentinel/动态路由/K8s 合并/Nacos-MQ-K8s/MySQL HA/分片扩展 |
| **M4** | W4 | P2 全部完成 | 24 项优化验收通过 |
| **M5** | M2 | P3 全部完成 | 11 项改进验收通过，综合达成率 > 95% |
| **M6** | M2 | 全链路压测通过 | 架构方案 10 项核心指标全部达标 |

### 6.3 关键性能指标目标

| 指标 | 当前 | 目标 | 测量方式 |
|------|:---:|:---:|---------|
| AI 版型生成速度 | 基准 | 提升 40%+ | k6 压测对比 |
| 区块链存证效率 | 基准 | 提升 50%+ | 批量 vs 单条上链耗时 |
| 订单处理峰值 TPS | 基准 | 提升 50%+ | k6 交易压测 |
| 核心接口响应 P99 | ≤500ms | ≤200ms | JMeter/Prometheus |
| 缓存命中率 | — | ≥85% | Redis 监控 |
| 整体资源占用 | 100% | ≤60% | Prometheus CPU/内存 |
| 首屏加载时间 | 3.5s+ | ≤1.5s | Lighthouse |

---

## 七、任务依赖矩阵

```
Week 1 (W1):
P0-02(数据隔离) ──────→ P1-05(Seata) ─────→ P1-09~14(数据流)
P0-01(售后) ───┐
P0-03(支付回调) ┼────→ P0-05(MQ消费者)
P0-04(结算MQ) ─┘

Week 2 (W2):
P1-01(AI调度) ──→ P1-10(生产者调用)
P1-02(批量上链) ──→ P1-09(队列生产者)
P1-03(热点缓存) ──→ P1-13(MultiLevelCache)
P1-04(分片路由) ──→ P1-20(分片扩展)
P1-06(注册权限)
P1-07~08(前端)

Week 3 (W3):
P1-15(Sentinel)
P1-16(动态路由)
P1-17~18(K8s目录/Nacos-MQ)
P1-19(MySQL HA)
P1-20(分片扩展) ←── 延续

Week 4 (W4):
P2-01~24(全部优化项) ──→ M4验收
P2-06(单体重构) ←── 大工时项，贯穿W3-W4
```

---

## 八、资源建议

| 角色 | 建议人数 | P0 | P1 | P2 | P3 |
|:----:|:-------:|:--:|:--:|:--:|:--:|
| 后端开发 | 3 | 3 | 3 | 2 | 1 |
| 前端开发 | 2 | — | 2 | 1 | 1 |
| DevOps | 1 | 1 | 1 | 1 | — |
| QA | 1 | — | 1 | 1 | 1 |
| **合计** | **7** | **4** | **7** | **5** | **3** |

---

## 九、风险与应对

| 风险 | 概率 | 影响 | 应对策略 |
|------|:---:|:----:|---------|
| P0 修复引入新缺陷 | 中 | 高 | 每项 P0 修复后执行对应 E2E 测试用例 |
| RocketMQ 部署失败影响 AI 调度 | 低 | 高 | 保留 RabbitMQ 作为 fallback |
| Seata Server 部署复杂度 | 中 | 中 | 先启用 AT 模式，后续升级 TCC |
| MySQL HA 切换导致数据不一致 | 低 | 高 | 配置半同步复制 + 定期校验 |
| 前端接口聚合重构工作量大 | 中 | 中 | 分批进行，先聚合 dashboard 核心页面 |
| K8s 目录合并导致配置遗漏 | 低 | 中 | 合并后执行 dry-run + diff 校验 |

---

## 十、验收流程

### 10.1 阶段验收 Checklist

```
P0 验收 (Day 3):
  [ ] 售后退款资金原路退回 + 库存恢复
  [ ] 商户A/B 数据完全隔离
  [ ] 支付回调订单状态正确更新
  [ ] 批量结算消息正确投递与消费
  [ ] K8s Secret 全部替换为真实值
  [ ] 支付凭证配置完成，支付功能正常
  [ ] Prometheus 全部 target UP

P1 验收 (Week 3):
  [ ] AI 批量制版通过 MQ 异步处理
  [ ] 版权批量上链累计100条自动触发
  [ ] 热点订单缓存命中率 ≥ 70%
  [ ] 订单分片路由正确
  [ ] Seata 分布式事务生效
  [ ] 用户注册自动分配角色权限
  [ ] 前端接口聚合后单页请求 ≤ 2 次
  [ ] 数据同步框架在核心业务中生效
  [ ] Core Gateway Sentinel 限流生效
  [ ] 动态路由通过 Nacos 配置生效
  [ ] K8s 目录统一，Nacos/RocketMQ 可部署
  [ ] MySQL HA + 自动备份就绪

P2 验收 (Week 4):
  [ ] TiDB profile 可切换
  [ ] GraphQL 缓存层生效
  [ ] 移动端离线可用
  [ ] PC Mall SSR 构建通过
  [ ] 24 项优化全部验收完成

P3 验收 (Month 2):
  [ ] 代码质量无遗留问题
  [ ] 测试覆盖率达目标值
  [ ] 全链路压测指标达标
  [ ] 综合架构方案达成率 > 95%
```

### 10.2 持续集成要求

- 每项修复提交前：通过对应模块单元测试
- 每日：执行 E2E 核心流程测试
- 每周：执行 k6 性能回归测试
- 每阶段末：执行全链路压测与安全扫描

---

## 十一、沟通机制

| 频率 | 形式 | 参与人 | 内容 |
|:----:|------|--------|------|
| 每日 | 站会 (15min) | 全体 | 进度同步、阻塞问题 |
| 每周五 | 周评审 | 全体 + PM | 本周成果、下周计划 |
| 里程碑 | 阶段评审 | 全体 + 管理层 | 阶段验收、指标达成 |
| 按需 | 风险评审 | 架构 + DevOps + QA | 风险评估与应对 |

---

## 附录：问题编号映射

| 本计划编号 | 对应问题清单编号 | 类型 |
|:---------:|:--------------:|:----:|
| P0-01 | C-DATA-03 | Critical |
| P0-02 | C-DATA-04 | Critical |
| P0-03 | C-DATA-06 | Critical |
| P0-04 | C-DATA-07 | Critical |
| P0-05 | C-DATA-08 | Critical |
| P0-06 | C-DEP-01 | Critical |
| P0-07 | C-DEP-02 | Critical |
| P0-08 | C-DEP-04 | Critical |
| P1-01 | C-BE-01 / C-DATA-01 | Critical |
| P1-02 | C-BE-02 | Critical |
| P1-03 | C-BE-03 | Critical |
| P1-04 | C-BE-04 | Critical |
| P1-05 | C-BE-05 | Critical |
| P1-06 | C-DATA-05 | Critical |
| P1-07 | H-FE-01 | High |
| P1-08 | H-FE-02 | High |
| P1-09 | H-DATA-01 | High |
| P1-10 | H-DATA-02 | High |
| P1-11 | H-DATA-03 | High |
| P1-12 | H-DATA-02 (data sync) | High |
| P1-13 | H-DATA-05 | High |
| P1-14 | H-DATA-06 | High |
| P1-15 | H-BE-02 | High |
| P1-16 | H-BE-03 | High |
| P1-17 | H-DEP-04 | High |
| P1-18 | H-DEP-10 | High |
| P1-19 | C-DEP-05 | Critical |
| P1-20 | C-DEP-06 | Critical |
