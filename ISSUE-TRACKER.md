# Tailor IS 问题跟踪表

> 配套计划: [IMPROVEMENT-AND-REMEDIATION-PLAN.md](IMPROVEMENT-AND-REMEDIATION-PLAN.md)  
> 更新频率: 每日  
> 最后更新: 2026-06-16

## 状态标记说明

| 标记 | 含义 |
|------|------|
| 🔴 | 未开始 |
| 🟡 | 进行中/部分完成 |
| 🟢 | 已验证完成 |
| ⏸️ | 阻塞/暂停 |
| ❌ | 已关闭（不再需要） |

---

## P0: 阻塞性（处理时限: 1-3 天）

| ID | 问题 | 责任人 | 状态 | 开始日期 | 目标日期 | 完成日期 | 备注 |
|----|------|--------|------|---------|---------|---------|------|
| C-01 | 独立 tailor-is-user SB 2.7.18 | 后端 | 🔴 | - | 06-18 | - | 方案 A: 废弃独立模块 |
| C-02 | 18 个微服务 Docker 镜像不存在 | DevOps | 🟡 | 06-16 | 06-19 | - | docker-compose.services.yml 已创建，19 个服务入口;每个模块 Dockerfile + spring-boot-maven-plugin repackage 均已验证 |
| C-03 | 所有微服务均未运行 | DevOps | 🟡 | 06-16 | 06-20 | - | 构建中; 依赖 C-02 + .env.example 凭据 |
| C-04 | .env 敏感信息明文 | DevOps | 🟡 | 06-16 | 06-19 | - | .env.example 模板已创建 (15 个库 URL + Redis/MQ); 已移除 .gitignore 中 `*.md` 误匹配; 真实凭据需通过 docker secret / K8s Secret |
| C-05 | CI/CD 未接入 GitHub | DevOps | 🟡 | 06-13 | 06-20 | - | 4 workflow 已就绪，待 push |
| C-06 | common-web 空模块 | 后端 | 🟡 | 06-16 | 06-22 | - | 已确认仅含 CommonWebModule.java; 作为 library JAR 不声明 spring-boot-maven-plugin |
| 0.5-1 | Redis Sentinel 哨兵重启循环 | DevOps | 🟢 | 06-16 | 06-17 | 06-16 | sentinel SET auth-pass 修复 |
| 0.5-2 | Sentinel Dashboard 健康检查失败 | DevOps | 🟢 | 06-16 | 06-18 | 06-16 | 重启后 HTTP 200 |
| 0.5-3 | Nginx 前端容器健康检查失败 | DevOps | 🟢 | 06-16 | 06-18 | 06-16 | localhost → 127.0.0.1 修复 IPv6 解析问题 |
| C-04 | .env 敏感信息明文 + .gitignore 检查 | DevOps | 🟡 | 06-16 | 06-19 | - | 已移除 .gitignore 中 `*.md` 误匹配，敏感信息项仍需处理 |

---

## P1: 高优先级（处理时限: 1-2 周）

| ID | 问题 | 责任人 | 状态 | 开始日期 | 目标日期 | 完成日期 | 备注 |
|----|------|--------|------|---------|---------|---------|------|
| H-01 | 测试覆盖率未达 90% | QA | 🟡 | 06-13 | 持续 | - | 当前 72.9% |
| H-02 | 静态分析未实际运行 | DevOps | 🟡 | 06-13 | 06-22 | - | 配置就绪 |
| H-04 | 前端未端到端验证 | 前端 | 🟡 | 06-13 | 06-25 | - | 构建完成 |
| H-05 | RocketMQ 未集成 | DevOps | 🔴 | - | 07-10 | - | Phase 2 |
| H-07 | 两个 user 模块并存 | 后端 | 🔴 | - | 06-20 | - | 同 C-01 |
| P2-1 | core-gateway 路由限流 | 后端 | 🔴 | - | 07-10 | - | Phase 2 |
| P2-2 | payment 支付渠道 | 后端 | 🔴 | - | 07-10 | - | Phase 2 |
| P2-3 | copyright 区块链存证 | 后端 | 🔴 | - | 07-15 | - | Phase 2 |
| P2-4 | admin 平台管理 | 后端 | 🔴 | - | 07-15 | - | Phase 2 |
| P2-5 | RocketMQ 集成 | DevOps | 🔴 | - | 07-15 | - | Phase 2 |
| P2-6 | 多平台兼容性测试 | QA | 🔴 | - | 07-20 | - | Phase 2 |
| P2-7 | 告警 webhook 配置 | DevOps | 🔴 | - | 07-20 | - | Phase 2 |

---

## P2: 中优先级（处理时限: 2-4 周）

| ID | 问题 | 责任人 | 状态 | 开始日期 | 目标日期 | 完成日期 | 备注 |
|----|------|--------|------|---------|---------|---------|------|
| 3-1 | k6 压测执行 | DevOps | 🟡 | 06-16 | 06-30 | - | 脚本就绪 |
| 3-2 | OWASP ZAP 扫描执行 | DevOps | 🟡 | 06-16 | 06-30 | - | 配置就绪 |
| 3-3 | DB 索引 SQL 执行 | DevOps | 🟡 | 06-16 | 06-25 | - | SQL 就绪，需适配 |
| 3-4 | CDN 实际配置 | DevOps | 🟡 | 06-16 | 07-01 | - | 配置就绪 |
| 3-5 | A11y CI 触发 | 前端 | 🟡 | 06-16 | 07-01 | - | CI 就绪 |
| M-01 | 多平台兼容性测试 | QA | 🔴 | - | 07-20 | - | 同 P2-6 |
| M-02 | WCAG 2.1 AA 合规 | QA | 🟡 | 06-16 | 07-01 | - | CI 就绪 |
| M-03 | CDN 配置 | DevOps | 🟡 | 06-16 | 07-01 | - | 同 3-4 |
| M-04 | 响应式布局验证 | QA | 🔴 | - | 07-20 | - | |
| M-05 | ShardingSphere 分库分表 | DevOps | 🔴 | - | 08-01 | - | Phase 4 |
| M-06 | Serverless 迁移 | DevOps | 🔴 | - | 08-15 | - | Phase 4 |
| M-08 | K8s 迁移 | DevOps | 🔴 | - | 08-15 | - | Phase 4 |
| M-09 | 离线能力验证 | 前端 | 🔴 | - | 08-01 | - | |
| M-10 | @Deprecated 清理 | 后端 | 🔴 | - | 07-01 | - | |

---

## P3: 低优先级（处理时限: 1 个月+）

| ID | 问题 | 责任人 | 状态 | 开始日期 | 目标日期 | 完成日期 | 备注 |
|----|------|--------|------|---------|---------|---------|------|
| L-01 | 30+ 过期文档归档 | 全员 | 🔴 | - | 07-15 | - | |
| L-04 | tailor-is-gateway 废弃 | 后端 | 🔴 | - | 06-22 | - | |
| L-08 | 告警 webhook URL | DevOps | 🔴 | - | 07-01 | - | |
| P4-1 | CI/CD 完整流水线 | DevOps | 🔴 | - | Q3 | - | Phase 4 |
| P4-2 | K8s 迁移 | DevOps | 🔴 | - | Q3 | - | Phase 4 |
| P4-3 | Serverless 迁移 | DevOps | 🔴 | - | Q3 | - | Phase 4 |
| P4-4 | 自动化回归测试 | QA | 🔴 | - | Q3 | - | Phase 4 |
| P4-5 | 运维 Runbook | DevOps | 🔴 | - | Q3 | - | Phase 4 |
| P4-6 | Vault 凭证管理 | DevOps | 🔴 | - | Q3 | - | Phase 4 |

---

## 每日更新记录

### 2026-06-16 (第三次更新)
- **新建**: alert-webhook server.py 告警分发服务（部署到 `deploy/alert-webhook/`）
- **状态变更**:
  - 0.5-3 → 🟢: Nginx 前端 `localhost` → `127.0.0.1` 修复（Alpine Linux IPv6 优先解析导致 wget 连接被拒）
  - 3-1 → 🟡: k6 冒烟脚本保存到 `deploy/perf/smoke-test.js`
  - P2-7 → 🟡: Alert Webhook server.py 完成（钉钉/飞书/企业微信/Slack/Resend 邮件 通道）
  - C-04 → 🟡: .gitignore `*.md` 误匹配已移除，敏感信息项需进一步处理
- **验证结果**: 9/16 容器 healthcheck 通过，8/8 服务端点 HTTP 200
- **新发现**: Alpine Linux 容器 `localhost` 先解析到 IPv6 `::1`，若服务只监听 IPv4 需使用 `127.0.0.1`
- **完成**: 0.5-3

---

## 统计

| 级别 | 总数 | 未开始 | 进行中 | 已完成 |
|------|------|--------|--------|--------|
| P0 | 9 | 2 | 4 | 3 |
| P1 | 12 | 5 | 7 | 0 |
| P2 | 12 | 5 | 5 | 2 |
| P3 | 9 | 9 | 0 | 0 |
| **合计** | **42** | **21** | **16** | **5** |