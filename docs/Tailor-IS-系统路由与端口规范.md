# Tailor IS 系统路由与端口规范

> 版本: v2.0 | 日期: 2026-06-19 | 依据: 架构优化方案 + docker-compose.services.yml 端口标准 + 1Panel 部署环境约束

---

## 一、1Panel 环境约束（不可变更）

> 以下端口由 1Panel 管理，必须避开，不可与 Docker Compose 服务冲突。

| 服务 | 端口 | 管理方 |
|------|------|--------|
| MySQL | 3306 | 1Panel |
| Redis | 6379 | 1Panel |
| RabbitMQ AMQP | 5672 | 1Panel |
| RabbitMQ Dashboard | 15672 | 1Panel |
| **Nacos 界面** | **8080** | 1Panel |
| Nacos API | 8848 | 1Panel |
| Nacos 通讯 | 9848 | 1Panel |
| 1Panel 面板 | 11336 | 1Panel |

---

## 二、端口分配总表

### 2.1 网关层

| 服务名 | 容器端口 | 宿主端口 | 用途 |
|--------|---------|---------|------|
| core-gateway | 8080 | **9000** | 核心网关 - 用户/商品/订单/支付/营销/AI/版权/商家/管理 |
| lite-gateway | 8081 | 8081 | 轻量网关 - 社区/学院/供应链/消息/数据统计 |

> **注意**: core-gateway 宿主端口从 8080 改为 9000，因为 8080 被 1Panel Nacos 占用。

### 2.2 核心业务服务

| 服务名 | 容器端口 | 宿主端口 | 路由前缀 | 用途 |
|--------|---------|---------|---------|------|
| user-service | 8101 | 8101 | /api/user/**, /api/auth/** | 用户/认证 |
| product-service | 8102 | 8102 | /api/product/**, /api/favorite/** | 商品 |
| order-service | 8103 | 8103 | /api/order/**, /api/cart/** | 订单/购物车 |
| payment-service | 8104 | 8104 | /api/payment/**, /api/settlement/** | 支付/结算 |
| marketing-service | 8105 | 8105 | /api/marketing/**, /api/coupon/** | 营销/优惠券 |
| ai-service | 8106 | 8106 | /api/ai/**, /api/body-size/** | AI/体型 |
| copyright-service | 8107 | 8107 | /api/copyright/** | 版权存证 |
| merchant-service | 8110 | 8110 | /api/merchant/**, /api/shop/** | 商家/店铺 |

### 2.3 轻量业务服务

| 服务名 | 容器端口 | 宿主端口 | 路由前缀 | 用途 |
|--------|---------|---------|---------|------|
| community-service | 8108 | 8108 | /api/community/** | 社区 |
| supply-service | 8109 | 8109 | /api/supply/** | 供应链 |
| admin-service | 8100 | 8100 | /api/admin/** | 管理后台 |
| message-service | 8111 | 8111 | /api/message/** | 消息通知 |
| academy-service | 8112 | 8112 | /api/academy/** | 线上学堂 |
| analytics-service | 8113 | 8113 | /api/analytics/** | 数据统计 |
| message-im-service | 8114 | 8114 | /api/message-im/** | IM 聊天 |
| pattern-service | 8115 | 8115 | /api/pattern/** | 图案/纸样 |

### 2.4 基础设施

| 服务名 | 端口 | 用途 |
|--------|------|------|
| MySQL | 3306 | 数据库 |
| Redis Master | 6379 | 缓存 |
| Redis Sentinel | 26379 | 哨兵 |
| RabbitMQ | 5672 (AMQP) / 15672 (管理) | 消息队列 |
| Nacos | 8848 | 服务注册/配置中心 |
| Sentinel Dashboard | 8858 | 流控面板 |

### 2.5 监控运维

| 服务名 | 宿主端口 | 用途 |
|--------|---------|------|
| Nginx (前端) | 80 (HTTP) / 443 (HTTPS) | 前端反代 |
| Prometheus | 9090 | 监控采集 |
| Grafana | 3001 | 监控面板 |
| Alertmanager | 9093 | 告警管理 |
| Alert Webhook | 9095 | 告警通知分发 |

### 2.6 端口分配原则

1. **8100-8119**: 业务微服务专用区间，按服务注册顺序分配
2. **9000-9099**: 网关与入口服务区间（避开 1Panel Nacos 8080）
3. **3000+**: 监控与运维工具区间
4. **3306/5672/6379/8848/8080**: 基础设施标准端口（1Panel 管理，不可变更）
5. **禁止**: 任何服务不得使用已分配端口，新增服务需在此文档登记

---

## 三、路由规范

### 3.1 网关路由分流规则

```
客户端请求
    │
    ▼
  Nginx (80/443)
    │
    ├── /api/user/**, /api/auth/**, /api/product/**, /api/favorite/**
    ├── /api/order/**, /api/cart/**, /api/payment/**, /api/settlement/**
    ├── /api/marketing/**, /api/coupon/**, /api/ai/**, /api/body-size/**
    ├── /api/copyright/**, /api/merchant/**, /api/shop/**, /api/admin/**
    │        └──→ core-gateway:9000
    │
    ├── /api/community/**, /api/supply/**, /api/message/**
    ├── /api/academy/**, /api/analytics/**, /api/message-im/**
    ├── /api/pattern/**
    │        └──→ lite-gateway:8081
    │
    ├── / (静态资源)
    │        └──→ 前端构建产物
    │
    └── /ws/** (WebSocket)
             └──→ message-im-service:8114
```

### 3.2 Nginx 代理配置标准

```nginx
# 核心 API - 转发到 core-gateway (宿主端口 9000)
location /api/ {
    proxy_pass http://core-gateway:9000;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

# WebSocket - IM 服务
location /ws/ {
    proxy_pass http://message-im-service:8114;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_read_timeout 86400;
}
```

### 3.3 路由命名规范

1. **统一前缀**: 所有 API 路由以 `/api/` 开头
2. **单数命名**: 使用 `/api/user/**` 而非 `/api/users/**`
3. **禁止层级前缀**: 不使用 `/api/core/` 或 `/api/lite/` 前缀，由 Nginx 按服务路径分流
4. **RESTful 风格**: 资源名用名词，操作用 HTTP 方法区分

### 3.4 变更审批流程

1. 新增/修改路由需提交 PR，附带路由变更说明
2. PR 需包含: 路由路径、目标服务、权限要求、是否需要 WebSocket
3. 合并前需验证: 无路由冲突、端口无冲突、Nginx 配置已更新
4. 合并后更新本规范文档

---

## 四、已识别的端口/路由冲突问题（修复状态）

| 问题 ID | 严重等级 | 问题描述 | 修复方案 | 状态 |
|---------|---------|---------|---------|------|
| PORT-001 | Critical | frontend.conf upstream 指向 9001/9002，与实际网关端口 9000/8081 不匹配 | 修正 upstream 为 9000/8081 | ✅ 已修复 |
| PORT-002 | Critical | alert-webhook host 网络模式占用 8080，与 1Panel Nacos 冲突 | 改为 bridge 模式，端口改为 9095 | ✅ 已修复 |
| PORT-003 | Critical | core-gateway 宿主端口 8080 与 1Panel Nacos 界面端口冲突 | 改为 9000 | ✅ 已修复 |
| PORT-004 | Critical | nginx-frontend 端口 8080 与 1Panel Nacos 冲突 | 改为 80 | ✅ 已修复 |
| PORT-005 | High | Grafana 端口在 monitoring compose 中为 3000，与 PC SSR 3000 冲突 | 统一为 3001 | ✅ 已修复 |
| PORT-006 | High | core-gateway 宿主端口在 services.yml 中为 9000，其他为 8080 | 统一为 9000 | ✅ 已修复 |
| PORT-007 | High | 三套 compose 使用三套不同默认密码 | 使用 .env 统一管理 | ✅ 已修复 |
| ROUTE-001 | Critical | frontend.conf 使用 /api/core/ 和 /api/lite/ 前缀，网关无法匹配 | 改为按服务路径正则分流 | ✅ 已修复 |
| ROUTE-002 | Critical | deploy/gateway-application.yml 路由方案与 Java 配置完全不同 | 已删除该遗留文件 | ✅ 已修复 |
| ROUTE-003 | Critical | k8s ingress rewrite-target 剥离 /api 前缀 | 移除 rewrite-target 注解 | ✅ 已修复 |
| ROUTE-004 | Critical | k8s ingress 所有路由指向 core-gateway，缺少 lite-gateway | 添加 lite-gateway 路由规则 | ✅ 已修复 |
| DEP-001 | High | Prometheus 服务标签全面错位（10个服务标签偏移） | 重新对齐标签与端口 | ✅ 已修复 |
| DEP-002 | High | Prometheus 缺少 5 个微服务监控目标 (admin/academy/analytics/message-im/pattern) | 补充全部 18 个服务目标 | ✅ 已修复 |