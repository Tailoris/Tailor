# Tailor IS 微服务平台

> 基于 Spring Cloud 2023 + Spring Boot 3.2 + Alibaba Cloud 的微服务架构定制设计交易平台后端

## 架构概述

Tailor IS 是一个面向定制设计交易领域的电商平台后端系统，采用微服务架构设计，包含 **16 个独立微服务**。所有服务通过 **API Gateway** 统一路由入口，依赖 **Nacos** 作为服务注册发现与配置中心，形成完整的服务治理体系。

系统涵盖用户管理、商品交易、订单支付、营销推广、AI 辅助设计、版权保护、社区互动、供应链管理、商户管理、消息推送、数据分析、即时通讯等全链路业务场景。

## 技术栈

| 类别 | 技术 |
|------|------|
| 核心框架 | Spring Boot 3.2, Spring Cloud 2023 |
| 微服务治理 | Spring Cloud Alibaba (Nacos 注册中心 & 配置中心) |
| 持久层 | MyBatis-Plus 3.5.7 |
| 认证授权 | Sa-Token |
| 缓存 | Redis |
| 消息队列 | RabbitMQ |
| 数据库 | MySQL 8.0 |
| 搜索引擎 | Elasticsearch 8.x |
| 服务网关 | Spring Cloud Gateway |
| 分布式事务 | Seata |
| 链路追踪 | Apache SkyWalking |
| 监控告警 | Prometheus + Grafana + Alertmanager |
| 日志采集 | ELK (Filebeat + Logstash + Elasticsearch + Kibana) |
| 质量门禁 | SonarQube, PMD, Checkstyle |
| 构建工具 | Maven |
| 容器化 | Docker, Docker Compose |

## 服务端口列表

| 服务名称 | 端口 | 说明 |
|----------|------|------|
| gateway | 8080 | API 网关 — 统一入口，路由分发，鉴权过滤 |
| admin | 8100 | 后台管理 — 平台运营管理、数据看板、审核仲裁 |
| user | 8101 | 用户服务 — 用户注册、登录、权限、个人信息 |
| product | 8102 | 商品服务 — 商品发布、管理、搜索、分类 |
| order | 8103 | 订单服务 — 订单创建、状态流转、售后、购物车 |
| payment | 8104 | 支付服务 — 支付通道、退款、对账、结算 |
| marketing | 8105 | 营销服务 — 优惠券、秒杀、拼团、会员体系 |
| ai | 8106 | AI 服务 — 智能量体、图案生成、图案检查 |
| copyright | 8107 | 版权服务 — 版权登记、区块链存证、侵权仲裁 |
| community | 8108 | 社区服务 — 帖子、评论、点赞、关注、举报 |
| supply | 8109 | 供应链服务 — 供应商管理、采购、物流协同 |
| merchant | 8110 | 商户服务 — 商户入驻、审核、店铺管理、员工管理 |
| message | 8111 | 消息服务 — 系统通知、模板消息、消息推送 |
| academy | 8112 | 学院服务 — 课程管理、章节管理、学习记录 |
| analytics | 8113 | 数据分析服务 — 业务指标、数据看板、报表 |
| message-im | 8114 | 即时通讯服务 — 实时聊天、会话管理 |

## 快速启动指南

### 前置依赖

- JDK 17+
- Maven 3.8+
- MySQL 8.0
- Redis 6+
- RabbitMQ 3.12+（需启用 `rabbitmq_delayed_message_exchange` 插件）
- Nacos 2.x

### 启动步骤

```bash
# 1. 克隆项目
git clone <repository-url>
cd tailor-is

# 2. 初始化数据库
mysql -u root -p < sql/01_user_system.sql
mysql -u root -p < sql/02_merchant_system.sql
# ... 依次执行其余 SQL 文件

# 3. 启动 Nacos（参考 docs/ 目录下的部署文档）

# 4. 编译打包
mvn clean install -DskipTests

# 5. 启动各微服务（按依赖顺序）
# 推荐顺序: gateway → admin → user → product → order → payment → ...
# 各服务启动类位于 tailor-is-*/src/main/java/com/tailoris/*/

# 或使用一键启动脚本
bash deploy/scripts/start-all-services.sh
```

### Docker Compose 方式

```bash
docker compose up -d
```

## 项目结构

```
tailor-is/
├── tailor-is-gateway/          # API 网关服务
├── tailor-is-admin/            # 后台管理服务
├── tailor-is-user/             # 用户服务
├── tailor-is-product/          # 商品服务
├── tailor-is-order/            # 订单服务
├── tailor-is-payment/          # 支付服务
├── tailor-is-marketing/        # 营销服务
├── tailor-is-ai/               # AI 服务
├── tailor-is-copyright/        # 版权服务
├── tailor-is-community/        # 社区服务
├── tailor-is-supply/           # 供应链服务
├── tailor-is-merchant/         # 商户服务
├── tailor-is-message/          # 消息服务
├── tailor-is-academy/          # 学院服务
├── tailor-is-analytics/        # 数据分析服务
├── tailor-is-message-im/       # 即时通讯服务
├── tailor-is-api/              # API 契约模块（实体定义、Feign 接口）
├── tailor-is-common/           # 公共模块（工具类、通用配置、安全组件）
├── tailor-is-common-web/       # Web 公共模块
├── sql/                        # 数据库初始化脚本
├── deploy/                     # 部署脚本与配置
│   ├── scripts/                #   自动化脚本
│   ├── docker-compose.*.yml    #   编排文件
│   └── filebeat/               #   日志采集配置
├── docs/                       # 项目文档
├── scripts/                    # 开发工具脚本
│   └── git-hooks/              #   Git 钩子
├── pom.xml                     # Maven 父 POM
└── docker-compose.yml          # Docker 编排
```

## 开发指南

### 代码规范

- 遵循阿里巴巴 Java 开发手册
- Checkstyle 规则: `checkstyle.xml`
- PMD 规则: `pmd/pmd-ruleset.xml`
- 详细规范请参阅 `docs/CODING_STANDARDS.md`

### 分支策略

```
main          ← 生产环境
  └── develop ← 开发集成
       ├── feature/*   ← 功能分支
       ├── bugfix/*    ← 修复分支
       └── release/*   ← 发布分支
```

### 本地开发

1. 确保 Nacos、MySQL、Redis、RabbitMQ 等基础设施已启动
2. 在 IDE 中导入为 Maven 项目
3. 配置本地 `application.yml` 或连接 Nacos 配置中心
4. 按依赖顺序启动所需服务（无需全量启动）

### API 文档

启动各服务后访问 Swagger UI：

```
http://localhost:<port>/swagger-ui.html
```

## 质量门禁

本项目在 CI/CD 流程中设置了以下质量门禁，任何合并请求须通过全部检查：

| 检查项 | 工具 | 要求 |
|--------|------|------|
| 代码风格 | Checkstyle | 零违规 |
| 静态分析 | PMD | 零 P1/P2 级别问题 |
| 代码质量 | SonarQube | 覆盖率 ≥ 70%，零 Blocker/Critical |
| 安全扫描 | OWASP Dependency-Check | 无高危漏洞 |
| 单元测试 | JUnit 5 | 核心业务模块覆盖率 ≥ 80% |
| 集成测试 | Spring Boot Test | 接口集成测试通过 |
| 性能测试 | JMeter | 核心接口 TPS、RT 达标（详见 `performance-tests/`） |

### 代码提交前检查

```bash
# 运行本地代码质量检查
mvn checkstyle:check
mvn pmd:check

# 运行单元测试
mvn test

# 或使用 Git Hook（已配置 pre-commit）
bash scripts/git-hooks/setup-hooks.sh
```

## 文档索引

- [架构设计](docs/ARCHITECTURE.md)
- [部署指南](docs/DEPLOYMENT-GUIDE.md)
- [API 指南](docs/API_GUIDE.md)
- [编码规范](docs/CODING_STANDARDS.md)
- [用户指南](docs/USER-GUIDE.md)
- [端口规范](docs/ROUTING-PORT-STANDARD.md)
- [Seata 配置](docs/SEATA-SETUP.md)
- [SonarQube 指南](docs/SONARQUBE-GUIDE.md)
- [性能测试方案](performance-tests/PERFORMANCE-TEST-PLAN.md)
- [部署清单](deploy/DEPLOYMENT-CHECKLIST.md)

## 许可证

Copyright © 2024-2026 Tailor IS. All rights reserved.
