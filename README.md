# Tailor IS 裁智云 — 服装全产业平台

Tailor IS（裁智云）是一个面向服装产业的全链路数字化平台，提供从商品展示、在线交易、纸样定制、商户入驻到社区交流的一体化服务。

## 项目简介

裁智云平台涵盖以下核心业务模块：

- **多商户商城** — 支持商户入驻、商品管理、订单管理、支付结算
- **纸样定制** — 服装纸样在线定制与协作
- **版权保护** — 基于区块链的版权登记与保护
- **AI 辅助** — AI 驱动的推荐与智能服务
- **社区交流** — 用户社区、内容分享
- **营销体系** — 优惠券、秒杀、拼团等营销工具

## 技术栈

| 层级 | 技术选型 |
|------|----------|
| 后端 | Spring Boot 3.x / Spring Cloud / Nacos / Seata |
| 前端 | Vue 3 + TypeScript / Element Plus / UniApp (移动端) |
| 数据库 | MySQL 8.0 / Redis |
| 消息队列 | RabbitMQ |
| 网关 | Spring Cloud Gateway |
| 监控 | Prometheus + Grafana + Alertmanager |
| 容器化 | Docker / Docker Compose / Kubernetes |

## 项目结构

```
tailoris/
├── tailor-is/                  # 后端微服务（Spring Cloud）
│   ├── tailor-is-gateway/      # API 网关
│   ├── tailor-is-user/         # 用户服务
│   ├── tailor-is-product/      # 商品服务
│   ├── tailor-is-order/        # 订单服务
│   ├── tailor-is-payment/      # 支付服务
│   ├── tailor-is-merchant/     # 商户服务
│   ├── tailor-is-ai/           # AI 服务
│   ├── tailor-is-copyright/    # 版权服务
│   ├── tailor-is-community/    # 社区服务
│   ├── tailor-is-marketing/    # 营销服务
│   ├── tailor-is-pattern/      # 纸样定制服务
│   ├── tailor-is-supply/       # 供应链服务
│   ├── tailor-is-message/      # 消息服务
│   └── docs/                   # 后端文档
├── tailor-is-frontend/         # 前端项目
│   ├── pc-mall/                # PC 端商城（Vue 3）
│   ├── merchant-admin/         # 商户管理后台
│   ├── platform-admin/         # 平台管理后台
│   ├── mobile-app/             # 移动端（UniApp）
│   └── docs/                   # 前端文档
├── deploy/                     # 部署相关（Docker Compose、Nginx 等）
├── docs/                       # 项目文档
├── modules/                    # 模块开发文档
└── docker-compose.yml          # Docker Compose 编排文件
```

## 快速开始

### 前置条件

- Docker 20.10+
- Docker Compose 2.x
- JDK 17+（本地开发）
- Node.js 18+（前端开发）

### 一键启动（Docker Compose）

```bash
# 1. 克隆项目
git clone <repository-url>
cd Tailoris

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env 文件，填写数据库密码等配置

# 3. 启动所有服务
docker-compose up -d

# 4. 查看服务状态
docker-compose ps

# 5. 查看日志
docker-compose logs -f
```

### 前端开发

```bash
cd tailor-is-frontend/pc-mall
npm install
npm run dev
```

### 后端开发

```bash
cd tailor-is
mvn clean install -DskipTests
# 按需启动各个微服务
```

## 文档

更多文档请查看 [docs 目录](tailor-is/docs/)：

- [架构设计](tailor-is/docs/ARCHITECTURE.md)
- [部署指南](tailor-is/docs/K8S-DEPLOYMENT-GUIDE.md)
- [API 文档](tailor-is/docs/API_GUIDE.md)
- [用户指南](tailor-is/docs/USER-GUIDE.md)
- [编码规范](tailor-is/docs/CODING_STANDARDS.md)
- [监控指南](tailor-is/docs/MONITORING-GUIDE.md)
- [消息队列指南](tailor-is/docs/MESSAGE-QUEUE-GUIDE.md)
- [缓存分层指南](tailor-is/docs/CACHE-LAYERING-GUIDE.md)
- [备份与恢复](tailor-is/docs/BACKUP-RECOVERY.md)
- [1Panel 部署指南](tailor-is/docs/1PANEL-DEPLOYMENT.md)

## 许可证

本项目采用 Apache 2.0 许可证，详见 [LICENSE](LICENSE) 文件。
