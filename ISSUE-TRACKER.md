# Tailor IS 项目问题跟踪表

> 最后更新: 2026-06-13
> 状态说明: ⬜ 未开始 | 🔄 进行中 | ✅ 已完成 | ✔️ 已验证

---

## CRITICAL (阻塞生产部署)

| ID | 状态 | 模块 | 问题描述 | 发现位置 | 解决方案 | 负责人 | 目标日期 |
|----|------|------|---------|---------|---------|--------|---------|
| C-01 | ⬜ | tailor-is-user | 独立 user 模块使用 Spring Boot 2.7.18，与主 POM 3.3.5 不一致 | `/tailor-is-user/pom.xml` L10 | 废弃独立模块，统一使用 tailor-is/tailor-is-user | 待分配 | 2026-06-13 |
| C-02 | ⬜ | 全部微服务 | 18 个微服务 Docker 镜像不存在 | `docker-compose.prod.yml` | `docker compose build` 本地构建所有镜像 | 待分配 | 2026-06-13 |
| C-03 | ⬜ | 全部微服务 | 所有微服务均未运行，仅基础设施在线 | 运行时 `docker ps` | 构建镜像后 `docker compose up -d` | 待分配 | 2026-06-14 |
| C-04 | ⬜ | 全局安全 | `.env` 文件包含所有敏感信息明文 | `/home/tailor/Tailoris/.env` | 确认 .gitignore 排除；长期迁移至 Vault | 待分配 | 2026-06-13 |
| C-05 | ⬜ | 全局 | 无 CI/CD 流水线 | 全局 | 搭建 GitHub Actions 流水线 | 待分配 | 2026-06-20 |
| C-06 | ⬜ | common-web | 模块为空，仅有 pom.xml 无源代码 | `/tailor-is/tailor-is-common-web/` | 从父 POM modules 中移除 | 待分配 | 2026-06-13 |

## HIGH (高优先级)

| ID | 状态 | 模块 | 问题描述 | 发现位置 | 解决方案 | 负责人 | 目标日期 |
|----|------|------|---------|---------|---------|--------|---------|
| H-01 | ⬜ | 全局 | 测试覆盖率严重不足（11/21 模块有测试） | 各模块 src/test | 制定测试编写计划，优先核心交易流程 | 待分配 | 2026-06-27 |
| H-02 | ⬜ | 全局 | 无 SonarQube/Checkstyle/PMD 静态分析 | 全局 | 配置 SonarQube，设置质量门禁 | 待分配 | 2026-06-20 |
| H-03 | ⬜ | 全局 | 无系统架构图 | 全局 | 使用 draw.io 绘制架构图 | 待分配 | 2026-06-20 |
| H-04 | ⬜ | 前端 | 前端未有实际部署和集成测试 | `tailor-is-frontend/` | 构建前端项目，配置 Nginx 反向代理 | 待分配 | 2026-06-27 |
| H-05 | ⬜ | AI | RocketMQ 双选型实际未集成 | `tailor-is-ai/` | 安装 RocketMQ，集成 Starter | 待分配 | 2026-07-04 |
| H-06 | ⬜ | 全局 | Sentinel 限流熔断未见实际配置 | 各模块 | 安装 Sentinel Dashboard，配置流控规则 | 待分配 | 2026-06-27 |
| H-07 | ⬜ | user | 两个 tailor-is-user 模块并存 | 根目录 + tailor-is/ | 废弃独立模块，统一使用子模块 | 待分配 | 2026-06-13 |

## MEDIUM (中优先级)

| ID | 状态 | 模块 | 问题描述 | 发现位置 | 解决方案 | 负责人 | 目标日期 |
|----|------|------|---------|---------|---------|--------|---------|
| M-01 | ⬜ | 全局 | 多平台兼容性测试未执行 | 全局 | 执行 Chrome/Safari/Firefox/Edge + iOS/Android 测试 | 待分配 | 2026-07-11 |
| M-02 | ⬜ | 前端 | WCAG 2.1 AA 无障碍合规未验证 | 前端各项目 | 执行 Playwright a11y 测试 | 待分配 | 2026-07-11 |
| M-03 | ⬜ | 前端 | CDN 配置缺失 | deploy/ | 配置 CDN 静态资源加速 | 待分配 | 2026-07-11 |
| M-04 | ⬜ | 前端 | 响应式布局未全量验证 | 前端 | 执行响应式断点测试（320px~2560px） | 待分配 | 2026-07-11 |
| M-05 | ⬜ | 数据库 | TiDB/ShardingSphere 分库分表未实际集成 | tailor-is/ | 评估实际需要，或先实现 MySQL 主从 | 待分配 | 2026-07-11 |
| M-06 | ⬜ | 非核心服务 | Serverless 迁移仅为纸上规划 | academy, community | 评估 Serverless 实际 ROI | 待分配 | 2026-07-11 |
| M-07 | ⬜ | Redis | Cluster 配置缺失，仅单机 | deploy/ | 配置 Redis Sentinel 或 Cluster | 待分配 | 2026-07-11 |
| M-08 | ⬜ | K8s | K8s 部署配置存在但未被使用 | `tailor-is/deploy/k8s/` | 将 docker-compose 迁移至 K8s | 待分配 | 2026-07-11 |
| M-09 | ⬜ | 移动端 | 离线能力仅框架层面，未端到端验证 | mobile-app/utils/ | 执行弱网环境端到端测试 | 待分配 | 2026-07-11 |
| M-10 | ⬜ | 全局 | @Deprecated 代码未清理 | 6 个文件 | 清理废弃方法或标注迁移路径 | 待分配 | 2026-07-11 |

## LOW (低优先级)

| ID | 状态 | 模块 | 问题描述 | 发现位置 | 解决方案 | 负责人 | 目标日期 |
|----|------|------|---------|---------|---------|--------|---------|
| L-01 | ⬜ | 根目录 | 30+ 过期报告文档 | 根目录 | 归档至 docs/archive/ | 待分配 | 2026-07-18 |
| L-02 | ⬜ | 配置 | 多个 prometheus.yml 副本 | deploy/, tailor-is/deploy/ | 统一为 deploy/prometheus/prometheus.yml | 待分配 | 2026-07-18 |
| L-03 | ⬜ | 前端 | 缺少统一的 monorepo 管理 | tailor-is-frontend/ | 引入 pnpm workspaces | 待分配 | 2026-07-18 |
| L-04 | ⬜ | 废弃模块 | tailor-is-gateway 已废弃但仍在父 POM | tailor-is/pom.xml L26 | 从 modules 中移除 | 待分配 | 2026-06-13 |
| L-05 | ⬜ | 文档 | 部分模块 README.md 为空 | academy/admin/analytics/pattern/supply | 补充模块说明 | 待分配 | 2026-07-18 |
| L-06 | ⬜ | 根目录 | 临时日志和 PID 文件 | 根目录 | 删除并统一由 Docker 管理 | 待分配 | 2026-06-13 |
| L-07 | ⬜ | 文档 | modules/ 需求文档与实现存在偏差 | modules/ | 更新或标注需求实现状态 | 待分配 | 2026-07-18 |
| L-08 | ⬜ | 告警 | 告警通知渠道 webhook URL 未配置 | .env L142-145 | 补充 DINGTALK_WEBHOOK, FEISHU_WEBHOOK | 待分配 | 2026-07-04 |

---

## 统计

| 级别 | 总数 | 未开始 | 进行中 | 已完成 | 已验证 |
|------|------|--------|--------|--------|--------|
| Critical | 6 | 1 | 1 | 4 | 0 |
| High | 7 | 7 | 0 | 0 | 0 |
| Medium | 10 | 10 | 0 | 0 | 0 |
| Low | 8 | 6 | 0 | 2 | 0 |
| **合计** | **31** | **24** | **1** | **6** | **0** |