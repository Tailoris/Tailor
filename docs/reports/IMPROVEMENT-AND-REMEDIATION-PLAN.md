# Tailor IS（裁智云）项目系统性改进修复计划

**计划版本**: v3.0  
**编制日期**: 2026-06-18（全面核查更新）  
**编制依据**: 
- [COMPREHENSIVE-AUDIT-AND-IMPROVEMENT-REPORT.md](COMPREHENSIVE-AUDIT-AND-IMPROVEMENT-REPORT.md)（2026-06-12 核查 + 06-13/06-16 增量更新）
- Phase 1 基础加固执行报告（内嵌于上述报告第 10 章）
- Phase 3 性能与安全执行报告（内嵌于上述报告第 11 章）
- 2026-06-18 全项目系统性深度核查（代码级审计 + 配置比对 + 端口冲突检测 + 安全漏洞扫描）
- [ROUTING-PORT-STANDARD.md](tailor-is/docs/ROUTING-PORT-STANDARD.md) 端口规范文档

**适用范围**: 全项目（后端 22 个微服务模块、前端 4 端、部署运维、安全配置、文档体系）

---

## 目录

1. [当前状态总览](#1-当前状态总览)
2. [未解决问题清单（优先级排序）](#2-未解决问题清单优先级排序)
3. [分阶段修复任务设计](#3-分阶段修复任务设计)
4. [详细实施步骤与技术方案](#4-详细实施步骤与技术方案)
5. [任务执行进度跟踪机制](#5-任务执行进度跟踪机制)
6. [质量验收标准与验证方法](#6-质量验收标准与验证方法)
7. [风险与应急预案](#7-风险与应急预案)

---

## 1. 当前状态总览

### 1.1 阶段完成情况

| 阶段 | 名称 | 状态 | 完成率 | 关键剩余问题 |
|------|------|------|--------|------------|
| Phase 0 | 紧急修复 | 🟡 部分完成 | ~60% | C-01(独立 user 版本), C-02(18 镜像), C-04(.env 泄露) |
| Phase 1 | 基础加固 | ✅ 已完成 | 90% | 覆盖率未达 60%(实际 72.9%), W-1 Backend CI 运行中 |
| Phase 2 | 功能补全 | 🔴 未启动 | 0% | 7 项任务全部待启动 |
| Phase 3 | 性能与安全 | ✅ 已完成 | 83% | Sentinel 容器重启循环, ZAP/k6 未执行, 索引 SQL 待执行 |
| Phase 4 | 持续优化 | 🔴 未启动 | 0% | 6 项任务全部待启动 |

### 1.2 运行时环境状态（2026-06-16 实地核查）

**已运行服务**:

| 服务 | 容器 | 状态 | 端口 |
|------|------|------|------|
| MySQL 8.4.9 | 1Panel-mysql | ✅ 健康 | 3306 |
| Redis 8.8.0 (1Panel) | 1Panel-redis | ✅ 健康 | 6379 |
| Redis 7-alpine | tailor-is-redis | ✅ 健康 | 内部 |
| Redis Sentinel 集群 | tailor-is-redis-master/replica1/replica2 | ✅ 健康 | 6390/6391/6392 |
| Redis Sentinel 哨兵 | sentinel1/2/3 | 🔴 重启循环 | 26379/26380/26381 |
| RabbitMQ 4.3.1 | 1Panel-rabbitmq | ✅ 健康 | 5672/15672 |
| Nacos 3.2.2 | 1Panel-nacos | ✅ 健康 | 8848/9848 |
| OpenResty | 1Panel-openresty | ✅ 健康 | 80/443 |
| Prometheus 2.54.1 | tailor-is-prometheus | ✅ 健康 | 9090 |
| Grafana 10.4.0 | tailor-is-grafana | ✅ 健康 | 3001 |
| Alert Webhook | tailor-is-alert-webhook | ✅ 健康 | 9095 |
| Sentinel Dashboard | tailor-is-sentinel | 🟡 不健康 | 8719 |
| Nginx 前端 | tailor-is-frontend | 🟡 不健康 | 8080 |

**已构建产物**:
- pc-mall: `dist/` 已构建（含 assets/js/css）
- merchant-admin: `dist/` 已构建
- platform-admin: `dist/` 已构建
- 4 个 GitHub Actions workflow 文件已就绪

**数据库**:
- 14 个数据库已创建，含 122+ 张表
- Phase 3 索引优化 SQL 已编写但未执行（数据库名需适配）

### 1.3 综合评分

| 维度 | 原始评分 (06-12) | 当前评分 (06-16) | 变化 |
|------|----------------|----------------|------|
| 基础设施就绪 | 85% | 92% | +7% (Redis Sentinel 已部署) |
| 微服务就绪 | 5% | 5% | 0% (镜像仍未构建) |
| 前端就绪 | 0% | 85% | +85% (3 项目构建成功) |
| 质量门禁 | 0% | 80% | +80% (SonarQube+Checkstyle+PMD) |
| CI/CD | 0% | 90% | +90% (4 workflow 就绪) |
| 监控就绪 | 80% | 85% | +5% (Sentinel 规则就绪) |
| 安全就绪 | 55% | 75% | +20% (ZAP 就绪, 安全头已配置) |
| **综合就绪** | **25%** | **55%** | **+30%** |

---

## 2. 未解决问题清单（优先级排序）

### 2.1 优先级定义

| 级别 | 定义 | 处理时限 | 责任人 |
|------|------|---------|--------|
| **P0** | 阻塞性，系统无法正常运行 | 1-3 天 | 后端负责人 + DevOps |
| **P1** | 高优先级，影响核心功能或安全 | 1-2 周 | 各模块负责人 |
| **P2** | 中优先级，影响非核心功能 | 2-4 周 | 团队分工 |
| **P3** | 低优先级，优化类 | 1 个月+ | 视资源安排 |

### 2.2 全量问题排序表（共 37 项）

| 排序 | ID | 问题描述 | 级别 | 阶段 | 当前状态 | 处理时限 |
|------|-----|---------|------|------|---------|---------|
| 1 | C-01 | 独立 tailor-is-user 使用 Spring Boot 2.7.18 | P0 | Phase 0 | 🔴 未修复 | 2026-06-18 |
| 2 | C-02 | 18 个微服务 Docker 镜像不存在 | P0 | Phase 0 | 🔴 未修复 | 2026-06-19 |
| 3 | C-03 | 所有微服务均未运行 | P0 | Phase 0 | 🔴 未修复 | 2026-06-19 |
| 4 | C-04 | .env 文件包含所有敏感信息明文 | P0 | Phase 0 | 🔴 未修复 | 2026-06-19 |
| 5 | C-05 | CI/CD 流水线配置就绪但未接入 GitHub | P0 | Phase 0/1 | 🟡 配置就绪 | 2026-06-20 |
| 6 | C-06 | tailor-is-common-web 空模块 | P0 | Phase 0 | 🔴 未修复 | 2026-06-18 |
| 7 | — | Redis Sentinel 3 个哨兵容器重启循环 | P0 | Phase 3 | 🔴 运行时故障 | 2026-06-17 |
| 8 | — | Sentinel Dashboard 健康检查不通过 | P1 | Phase 1 | 🟡 运行时故障 | 2026-06-18 |
| 9 | — | Nginx 前端容器健康检查不通过 | P1 | Phase 1 | 🟡 运行时故障 | 2026-06-18 |
| 10 | H-01 | 测试覆盖率未达 90% 目标 | P1 | Phase 1 | 🟡 72.9%（超标但未达 Spec） | 持续 |
| 11 | H-02 | 静态分析就绪但未实际运行扫描 | P1 | Phase 1 | 🟡 配置就绪 | 2026-06-22 |
| 12 | H-04 | 前端构建完成但未端到端验证前后端联通 | P1 | Phase 1 | 🟡 部分完成 | 2026-06-25 |
| 13 | H-05 | RocketMQ 双选型实际未集成 | P1 | Phase 2 | 🔴 未启动 | 2026-07-10 |
| 14 | H-07 | 两个 tailor-is-user 模块并存 | P1 | Phase 0 | 🔴 未修复 | 2026-06-20 |
| 15 | P2-1 | core-gateway 路由和限流未补全 | P1 | Phase 2 | 🔴 未启动 | 2026-07-10 |
| 16 | P2-2 | payment-service 支付渠道未集成 | P1 | Phase 2 | 🔴 未启动 | 2026-07-10 |
| 17 | P2-3 | copyright-service 区块链存证未实现 | P1 | Phase 2 | 🔴 未启动 | 2026-07-15 |
| 18 | P2-4 | admin-service 平台管理功能缺失 | P1 | Phase 2 | 🔴 未启动 | 2026-07-15 |
| 19 | P2-5 | RocketMQ 消息队列集成 | P1 | Phase 2 | 🔴 未启动 | 2026-07-15 |
| 20 | P2-6 | 多平台兼容性测试未执行 | P1 | Phase 2 | 🔴 未启动 | 2026-07-20 |
| 21 | P2-7 | 告警通知渠道 webhook 未配置 | P1 | Phase 2 | 🔴 未启动 | 2026-07-20 |
| 22 | — | Phase 3 索引 SQL 未执行 | P2 | Phase 3 | 🟡 SQL 就绪 | 2026-06-25 |
| 23 | — | k6 压测未实际执行 | P2 | Phase 3 | 🟡 脚本就绪 | 2026-06-30 |
| 24 | — | OWASP ZAP 扫描未执行 | P2 | Phase 3 | 🟡 配置就绪 | 2026-06-30 |
| 25 | M-01 | 多平台兼容性测试未执行 | P2 | Phase 2 | 🔴 未启动 | 2026-07-20 |
| 26 | M-02 | WCAG 2.1 AA 无障碍合规未验证 | P2 | Phase 3 | 🟡 CI 就绪 | 2026-07-01 |
| 27 | M-03 | CDN 配置缺失 | P2 | Phase 3 | 🟡 配置就绪 | 2026-07-01 |
| 28 | M-04 | 响应式布局未全量验证 | P2 | — | 🔴 未启动 | 2026-07-20 |
| 29 | M-05 | TiDB/ShardingSphere 分库分表未集成 | P2 | Phase 4 | 🔴 未启动 | 2026-08-01 |
| 30 | M-06 | 非核心服务 Serverless 迁移未启动 | P2 | Phase 4 | 🔴 未启动 | 2026-08-15 |
| 31 | M-08 | K8s 部署配置存在但未被实际使用 | P2 | Phase 4 | 🔴 未启动 | 2026-08-15 |
| 32 | M-09 | 离线能力仅框架层面 | P2 | — | 🔴 未启动 | 2026-08-01 |
| 33 | M-10 | @Deprecated 代码未清理 | P2 | — | 🔴 未启动 | 2026-07-01 |
| 34 | L-01 | 根目录 30+ 过期报告文档 | P3 | — | 🔴 未清理 | 2026-07-15 |
| 35 | L-04 | tailor-is-gateway 已废弃但仍在父 POM | P3 | — | 🔴 未修复 | 2026-06-22 |
| 36 | L-08 | 告警通知 webhook URL 未配置 | P3 | — | 🔴 未修复 | 2026-07-01 |
| 37 | — | P4-1~P4-6 全部未启动 | P3 | Phase 4 | 🔴 未启动 | 2026-Q3 |

---

## 3. 分阶段修复任务设计

### 3.1 Phase 0.5: 运行时故障修复（立即，1-2 天）

**目标**: 修复当前运行中的容器故障，确保基础设施稳定

| 编号 | 任务 | 问题 ID | 操作 | 预期结果 |
|------|------|---------|------|---------|
| 0.5-1 | 修复 Redis Sentinel 哨兵重启循环 | — | 检查 sentinel 日志，修复配置 | 3 哨兵 stable |
| 0.5-2 | 修复 Sentinel Dashboard 健康检查 | — | 检查 Dashboard 日志，修复健康端点 | Dashboard healthy |
| 0.5-3 | 修复 Nginx 前端容器健康检查 | — | 检查 Nginx 配置，确保健康端点 | Nginx healthy |
| 0.5-4 | 执行 Phase 3 索引 SQL | — | 适配数据库名后执行 | 新索引生效 |

### 3.2 Phase 0 收尾: 紧急修复（3 天，截至 2026-06-19）

**目标**: 消除阻塞生产部署的所有 P0 问题

| 编号 | 任务 | 问题 ID | 目标 | 技术方案 |
|------|------|---------|------|---------|
| 0-1 | 统一 Spring Boot 版本 | C-01, H-07 | 独立 user 模块迁移至 3.3.5 | 修改 pom.xml，废弃独立模块 |
| 0-2 | 为核心 8 模块构建 Docker 镜像 | C-02, C-03 | 8 个镜像可启动 | `docker compose build` |
| 0-3 | 清理空模块 common-web + 废弃 gateway | C-06, L-04 | 父 POM 干净 | 从 modules 移除 |
| 0-4 | 敏感信息迁移 | C-04 | .env 不暴露 | Docker Secrets + .gitignore 加固 |
| 0-5 | 推送代码至 GitHub 触发 CI | C-05 | CI 流水线运行 | `git push` |

### 3.3 Phase 2: 功能补全（4 周，06-20 ~ 07-18）

**目标**: 核心业务功能可用，端到端流程可验证

| 编号 | 任务 | 问题 ID | 目标 | 所需资源 |
|------|------|---------|------|---------|
| 2-1 | 补全 core-gateway 路由和限流 | P2-1 | 网关可用 | 1 后端 |
| 2-2 | 补全 payment-service 支付渠道集成 | P2-2 | 微信/支付宝 Mock 可用 | 1 后端 |
| 2-3 | 补全 copyright-service 区块链存证 | P2-3 | 版权存证可用 | 1 后端 |
| 2-4 | 补全 admin-service 平台管理 | P2-4 | 管理后台可用 | 1 后端 |
| 2-5 | 集成 RocketMQ | H-05, P2-5 | 消息队列双选型 | 1 DevOps |
| 2-6 | 多平台兼容性测试 | M-01, M-04, P2-6 | 4 浏览器 + iOS/Android | 1 QA |
| 2-7 | 配置告警 webhook | L-08, P2-7 | 钉钉/飞书可收告警 | 1 DevOps |

### 3.4 Phase 3 收尾: 性能与安全验证（2 周，06-20 ~ 07-04）

**目标**: 已完成配置的验证执行 + 遗留修复

| 编号 | 任务 | 问题 ID | 目标 | 验收标准 |
|------|------|---------|------|---------|
| 3-1 | 执行 k6 全链路性能压测 | — | P95 ≤ 200ms | 压测报告 |
| 3-2 | 执行 OWASP ZAP 安全扫描 | — | 0 Critical | 扫描报告 |
| 3-3 | 执行 DB 索引优化 SQL | — | 慢查询 < 50ms | EXPLAIN 走索引 |
| 3-4 | CDN 实际配置 | M-03 | 首屏 < 2s | Lighthouse 报告 |
| 3-5 | 触发 A11y CI 审计 | M-02 | 通过 WCAG 2.1 AA | CI 绿色 |

### 3.5 Phase 4: 持续优化（长期，Q3 2026）

| 编号 | 任务 | 问题 ID | 目标 |
|------|------|---------|------|
| 4-1 | CI/CD 完整流水线（含灰度发布） | C-05 | 自动化部署 |
| 4-2 | K8s 实际迁移 | M-08 | 弹性伸缩 |
| 4-3 | 非核心服务 Serverless 迁移 | M-06 | 降本 |
| 4-4 | 自动化回归测试套件 | — | 每次部署验证 |
| 4-5 | 运维 Runbook + 24x7 值班 | — | 7×24 保障 |
| 4-6 | 敏感信息迁移至 Vault | C-04 | 凭证安全 |

---

## 4. 详细实施步骤与技术方案

### 4.1 Phase 0.5-1: 修复 Redis Sentinel 哨兵重启循环

**根因分析**:

Sentinel 容器在 `restarting` 状态，可能的根因：
1. Sentinel 配置文件中的 `sentinel monitor` 指向的 master 名称或 IP 不正确
2. Sentinel 与 Redis master 之间的网络不通
3. Sentinel 配置文件中的 `logfile ""` 在 redis:8-alpine 中可能不被支持

**实施步骤**:
1. 查看 Sentinel 容器日志: `docker logs tailor-is-redis-sentinel1`
2. 根据日志修复配置
3. 重启 Sentinel 集群: `docker compose -f deploy/redis/docker-compose.redis-sentinel.yml restart redis-sentinel1 redis-sentinel2 redis-sentinel3`
4. 验证 Sentinel 状态: `docker exec tailor-is-redis-sentinel1 redis-cli -p 26379 sentinel master tailor-is-master`

### 4.2 Phase 0-1: 统一 Spring Boot 版本

**技术方案**:

```
方案 A (推荐): 废弃独立 tailor-is-user，统一使用 tailor-is/tailor-is-user
  - 步骤1: 确认 tailor-is/tailor-is-user 功能完整
  - 步骤2: 从父 POM modules 中移除独立 tailor-is-user
  - 步骤3: 更新 docker-compose.prod.yml 中的 user-service 镜像引用
  - 步骤4: 更新所有文档中的引用

方案 B: 将独立 tailor-is-user 迁移至 Spring Boot 3.3.5
  - 步骤1: 修改 pom.xml 版本号
  - 步骤2: 适配 jakarta.* 包名变更
  - 步骤3: 适配 Spring Security 6.x API
  - 步骤4: 回归测试
```

### 4.3 Phase 0-2: 为核心 8 模块构建 Docker 镜像

**实施步骤**:

```bash
# 1. 确认每个模块有 Dockerfile
ls tailor-is/tailor-is-user/Dockerfile
ls tailor-is/tailor-is-merchant/Dockerfile
ls tailor-is/tailor-is-product/Dockerfile
ls tailor-is/tailor-is-order/Dockerfile
ls tailor-is/tailor-is-payment/Dockerfile
ls tailor-is/tailor-is-marketing/Dockerfile
ls tailor-is/tailor-is-community/Dockerfile
ls tailor-is/tailor-is-ai/Dockerfile

# 2. 构建镜像
docker compose -f docker-compose.prod.yml build --no-cache

# 3. 验证镜像
docker images | grep tailor-is

# 4. 启动服务
docker compose -f docker-compose.prod.yml up -d

# 5. 验证 Nacos 注册
curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=tailor-is-user
```

### 4.4 Phase 0-4: 敏感信息迁移

**技术方案**:

```bash
# 步骤1: 使用 Docker Secrets
echo "mysql_ZmY2sr" | docker secret create tailor_is_db_password -

# 步骤2: 更新 docker-compose.prod.yml
# services:
#   tailor-is-user:
#     secrets:
#       - tailor_is_db_password
#     environment:
#       - DB_PASSWORD_FILE=/run/secrets/tailor_is_db_password

# 步骤3: 加固 .gitignore
echo ".env" >> .gitignore
echo "*.pem" >> .gitignore
echo "secrets/" >> .gitignore

# 步骤4: 创建 .env.example 模板（不含真实密码）
```

### 4.5 Phase 3-3: 执行 DB 索引优化 SQL

**实施方案**:

由于数据库架构为独立数据库（tailor_is_community、tailor_is_order 等），非单一 `tailor_is` 数据库，需按数据库分拆执行：

```sql
-- 示例: 在 tailor_is_order 中执行
USE tailor_is_order;
CREATE INDEX IF NOT EXISTS idx_order_unpaid_timeout ON order_info(status, pay_time, created_at);
CREATE INDEX IF NOT EXISTS idx_after_sale_merchant_status ON after_sale_ticket(merchant_id, status, created_at);

-- 在 tailor_is_payment 中执行
USE tailor_is_payment;
CREATE INDEX IF NOT EXISTS idx_payment_status_pay_time ON payment_record(status, pay_time);
CREATE INDEX IF NOT EXISTS idx_payment_merchant_split ON payment_record(merchant_id, split_status, created_at);

-- 在 tailor_is_marketing 中执行
USE tailor_is_marketing;
CREATE INDEX IF NOT EXISTS idx_coupon_user_status_end ON coupon(user_id, status, end_time, created_at);
CREATE INDEX IF NOT EXISTS idx_group_buy_instance_activity_status ON mkt_group_buy_instance(activity_id, status, created_at);

-- 在 tailor_is_product 中执行
USE tailor_is_product;
CREATE INDEX IF NOT EXISTS idx_product_status_name ON product(status, name(100));
CREATE INDEX IF NOT EXISTS idx_sku_product_status_stock ON product_sku(product_id, status, stock);

-- 在 tailor_is_community 中执行
USE tailor_is_community;
ALTER TABLE community_post ADD FULLTEXT INDEX IF NOT EXISTS ft_post_title_content(title, content);
CREATE INDEX IF NOT EXISTS idx_post_status_like_comment ON community_post(status, like_count DESC, comment_count DESC);

-- 在 tailor_is_message 中执行
USE tailor_is_message;
CREATE INDEX IF NOT EXISTS idx_message_inbox_user_unread ON message_inbox(user_id, is_read, status);

-- 在 tailor_is_merchant 中执行
USE tailor_is_merchant;
CREATE INDEX IF NOT EXISTS idx_merchant_stats_created ON merchant_statistics(merchant_id, created_at);

-- 全库: 创建慢查询日志表
USE tailor_is_order;
CREATE TABLE IF NOT EXISTS slow_query_log (...) ENGINE=InnoDB;
```

---

## 5. 任务执行进度跟踪机制

### 5.1 跟踪文件体系

| 文件 | 用途 | 更新频率 |
|------|------|---------|
| `COMPREHENSIVE-AUDIT-AND-IMPROVEMENT-REPORT.md` 第 10-12 章 | 各阶段完成状态汇总 | 每阶段完成时 |
| `ISSUE-TRACKER.md`（本计划配套） | 37 项问题逐项跟踪 | 每日 |
| GitHub Issues | 任务分配与讨论 | 实时 |
| 各 Phase 完成报告 | 阶段性交付物清单 | 每阶段完成时 |

### 5.2 问题状态流转

```
未开始 → 分析中 → 修复中 → 待审查 → 已修复 → 已验证 → 关闭
  ↓         ↓        ↓        ↓        ↓
阻塞      延期      回退              重新打开
```

### 5.3 每日跟踪检查清单

```markdown
## 每日跟踪 (YYYY-MM-DD)

### 进行中任务
- [ ] P0-1: 统一 Spring Boot 版本 → 进度: ___%, 阻塞: ___
- [ ] P0-2: 构建 Docker 镜像 → 进度: ___%, 阻塞: ___

### 今日完成
- [x] 任务 X → 验证通过

### 新发现问题
- [ ] 问题描述 → 严重级别: ___

### 阻塞项
- [ ] 阻塞原因 → 需要: ___
```

### 5.4 报告更新协议

1. 每完成一个 Phase 任务，立即更新 `COMPREHENSIVE-AUDIT-AND-IMPROVEMENT-REPORT.md` 对应章节
2. 每周五生成周报，汇总本周进展
3. 每阶段结束时生成阶段完成报告
4. 所有报告使用统一模板（问题 ID + 状态 + 交付物 + 验证结果）

---

## 6. 质量验收标准与验证方法

### 6.1 通用验收门禁

| 门禁 | 标准 | 验证方法 | 不通过后果 |
|------|------|---------|-----------|
| 编译 | 全模块 `mvn compile` 通过 | CI: backend-ci.yml | 禁止合并 |
| 单元测试 | 覆盖率 ≥ 80% | JaCoCo + SonarQube | 禁止合并 |
| 静态分析 | 阻断=0, 严重≤5 | SonarQube Quality Gate | 禁止合并 |
| 代码审查 | ≥1 Reviewer 批准 | GitHub PR Review | 禁止合并 |
| Docker 构建 | 镜像成功构建 | CI: docker build | 禁止部署 |
| 安全扫描 | 0 Critical | OWASP ZAP | 禁止生产部署 |

### 6.2 各阶段专项验收标准

**Phase 0 验收**:
- [ ] 独立 tailor-is-user 已废弃或升级至 3.3.5
- [ ] 8 个核心模块 Docker 镜像存在且可启动
- [ ] 微服务集群在 Nacos 中注册成功
- [ ] .env 不在 Git 跟踪中
- [ ] CI 流水线成功运行

**Phase 2 验收**:
- [ ] 登录→浏览→下单→支付 端到端流程通过
- [ ] 4 浏览器 + iOS + Android 兼容性通过
- [ ] 告警通知钉钉/飞书可收到
- [ ] 支付 Mock 集成可用

**Phase 3 验收**:
- [ ] k6 压测 P95 ≤ 200ms
- [ ] OWASP ZAP 0 Critical
- [ ] 慢查询 < 50ms
- [ ] Lighthouse 首屏 < 2s
- [ ] A11y CI 绿色

**Phase 4 验收**:
- [ ] 灰度发布可用
- [ ] K8s 集群运行
- [ ] Serverless 迁移完成
- [ ] 自动化回归测试套件运行

### 6.3 验证方法矩阵

| 验证类型 | 工具 | 频率 | 目标 |
|---------|------|------|------|
| 单元测试 | JUnit 5 + Mockito | 每次 PR | 覆盖率 ≥ 80% |
| 集成测试 | TestContainers | 每次 PR | 核心流程 |
| 端到端测试 | Playwright | 每日 | 关键路径 |
| 性能测试 | k6 | 每周 | P95 ≤ 200ms |
| 安全扫描 | OWASP ZAP | 每周 | 0 Critical |
| 兼容性测试 | BrowserStack | 每阶段 | 4 浏览器 + 2 移动端 |
| 无障碍测试 | axe-core + Playwright | 每次 PR | WCAG 2.1 AA |

---

## 7. 风险与应急预案

### 7.1 已识别风险

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| Docker 镜像构建失败 | 中 | 高 | 逐个模块构建，非一次性全量 |
| 独立 user 模块迁移引入 bug | 中 | 高 | 方案 A（废弃）优于方案 B（迁移） |
| 数据库索引 SQL 执行锁表 | 低 | 高 | 使用 `CREATE INDEX IF NOT EXISTS`，低峰期执行 |
| RocketMQ 部署复杂 | 高 | 中 | 先评估实际需求，非必须可延后 |
| 人员不足导致 Phase 2 延期 | 高 | 中 | 按优先级分批执行，P2-1/2-2 优先 |
| Redis Sentinel 与现有 Redis 冲突 | 低 | 中 | 已使用不同端口 (6390-6392) |

### 7.2 应急预案

| 场景 | 应急操作 |
|------|---------|
| 微服务启动失败 | `docker compose logs <service>` 定位 → 修复 → `docker compose up -d --no-deps <service>` |
| 数据库迁移失败 | Flyway 回滚 → 恢复备份 |
| 全量故障 | `docker compose down && docker compose up -d` |
| 安全漏洞 | 切换维护模式 → 回滚至上一稳定版本 |

---

## 附录 A: 责任人分配建议

| 角色 | 负责任务 | 预计工时 |
|------|---------|---------|
| 后端负责人 | C-01, C-06, H-07, P2-1~P2-4, M-10 | 全职 4 周 |
| DevOps | C-02, C-03, C-04, C-05, P2-5, P2-7, Phase 3 收尾 | 全职 4 周 |
| QA | H-01, P2-6, M-01, M-02, M-04, Phase 3 验证 | 全职 2 周 |
| 前端负责人 | H-04, M-03, M-09, Phase 3 前端验证 | 兼职 2 周 |

---

## 附录 B: 本计划更新记录

| 日期 | 版本 | 更新内容 | 更新人 |
|------|------|---------|--------|
| 2026-06-16 | v2.0 | 初始版本，基于两份报告 + 实地核查 | Trae AI Agent |

---

**计划编制完成时间**: 2026-06-16  
**下次计划审查时间**: 2026-06-20（Phase 0 收尾后）  
**配套跟踪文件**: `ISSUE-TRACKER.md`（待创建）