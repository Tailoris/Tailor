# 网关分层拆分指南 (Gateway Split Guide)

> 将单体网关 (tailor-is-gateway) 拆分为核心网关 (core-gateway) 和轻量网关 (lite-gateway)，实现服务分层、资源隔离、故障隔离。

## 目录

1. [架构概述](#1-架构概述)
2. [服务路由分配](#2-服务路由分配)
3. [核心网关配置](#3-核心网关配置)
4. [轻量网关配置](#4-轻量网关配置)
5. [前端路由配置](#5-前端路由配置)
6. [部署指南](#6-部署指南)
7. [迁移步骤](#7-迁移步骤)
8. [监控与运维](#8-监控与运维)

---

## 1. 架构概述

### 1.1 拆分原因

| 维度 | 拆分前 (单体网关) | 拆分后 (双网关) |
|------|-------------------|-----------------|
| 路由数量 | 16 个服务共用一个网关 | 核心 10 个 + 轻量 6 个 |
| 资源分配 | 统一配置，无法差异化 | 核心高配、轻量低配 |
| 故障隔离 | 单点故障影响全部服务 | 轻量服务故障不影响核心交易 |
| 限流策略 | 全局统一限流 | 核心 200 req/s，轻量 50 req/s |
| 依赖复杂度 | Sentinel + Redis + SA-Token | 核心完整依赖，轻量无 Sentinel |

### 1.2 架构图

```
                    ┌─────────────────────┐
                    │   Nginx / CDN       │
                    │   (统一入口)         │
                    └──────┬──────────────┘
                           │
              ┌────────────┼────────────┐
              │                         │
       ┌──────▼──────┐          ┌──────▼──────┐
       │ Core Gateway│          │ Lite Gateway│
       │   :8080     │          │   :8081     │
       │ + Sentinel  │          │ 仅 Redis限流│
       │ + SA-Token  │          │ + SA-Token  │
       │ + Redis     │          │ + Redis     │
       └──────┬──────┘          └──────┬──────┘
              │                         │
    ┌─────────┼──────────┐    ┌─────────┼──────────┐
    │         │          │    │         │          │
  user    product   order   community message analytics
  merchant  payment  ai      academy   supply  message-im
  admin   copyright marketing pattern
```

---

## 2. 服务路由分配

### 2.1 Core Gateway (核心网关) - Port 8080

> 路由核心交易链路和高价值服务

| 路由名称 | 路径匹配 | 下游服务 | 说明 |
|----------|----------|----------|------|
| user-route | `/api/user/**`, `/api/auth/**` | tailor-is-user | 用户认证与资料 |
| product-route | `/api/product/**`, `/api/favorite/**` | tailor-is-product | 商品浏览与收藏 |
| order-route | `/api/order/**`, `/api/cart/**` | tailor-is-order | 订单与购物车 |
| payment-route | `/api/payment/**`, `/api/settlement/**`, `/api/account/**`, `/api/sandbox/**` | tailor-is-payment | 支付与结算 |
| marketing-route | `/api/marketing/**`, `/api/coupon/**`, `/api/points/**`, `/api/seckill/**` | tailor-is-marketing | 营销活动 |
| ai-route | `/api/ai/**`, `/api/body-size/**` | tailor-is-ai | AI 量体服务 |
| copyright-route | `/api/copyright/**` | tailor-is-copyright | 版权管理 |
| merchant-route | `/api/merchant/**`, `/api/shop/**` | tailor-is-merchant | 商家管理 |
| admin-route | `/api/admin/**` | tailor-is-admin | 平台管理后台 |
| pattern-route | `/api/pattern/**` | tailor-is-pattern | 图案/纸样 (按需启用) |

**特性**: Sentinel 限流熔断 + SA-Token 鉴权 + Redis 分布式限流

### 2.2 Lite Gateway (轻量网关) - Port 8081

> 路由社区、内容、通讯等非交易链路

| 路由名称 | 路径匹配 | 下游服务 | 说明 |
|----------|----------|----------|------|
| community-route | `/api/community/**`, `/api/post/**`, `/api/comment/**` | tailor-is-community | 社区互动 |
| academy-route | `/api/academy/**`, `/api/course/**` | tailor-is-academy | 学院课程 |
| supply-route | `/api/supply/**` | tailor-is-supply | 供应链 |
| message-route | `/api/message/**`, `/api/notice/**` | tailor-is-message | 消息通知 |
| im-route | `/api/im/**`, `/api/im-message/**` | tailor-is-message-im | IM 即时通讯 |
| analytics-route | `/api/analytics/**`, `/api/metrics/**`, `/api/dashboard/**` | tailor-is-analytics | 数据分析 |

**特性**: 仅 Redis 限流 + SA-Token 鉴权 (无 Sentinel，降低资源占用)

---

## 3. 核心网关配置

### 3.1 关键参数

| 配置项 | 值 | 说明 |
|--------|-----|------|
| 端口 | `8080` | 主网关端口 |
| 默认限流 | `200 req/s per IP` | 高于轻量网关 |
| 登录限流 | `10 req/min per IP` | 防止暴力破解 |
| Redis DB | `0` | 与轻量网关隔离 |
| Redis 连接池 | `max-active: 20` | 高并发场景 |

### 3.2 Sentinel 配置

```yaml
spring:
  cloud:
    sentinel:
      enabled: true
      transport:
        dashboard: ${SENTINEL_DASHBOARD_ADDR:localhost:8080}
        port: 8719
      eager: true
```

---

## 4. 轻量网关配置

### 4.1 关键参数

| 配置项 | 值 | 说明 |
|--------|-----|------|
| 端口 | `8081` | 轻量网关端口 |
| 默认限流 | `50 req/s per IP` | 低于核心网关 |
| Redis DB | `1` | 与核心网关隔离 |
| Redis 连接池 | `max-active: 10` | 轻量级 |

### 4.2 无 Sentinel

轻量网关不依赖 Sentinel，使用 Redis 基础限流即可满足需求，减少资源占用。

---

## 5. 前端路由配置

### 5.1 环境变量

各前端项目需要配置两个网关地址：

| 变量名 | 说明 | 开发环境默认值 | 生产环境默认值 |
|--------|------|----------------|----------------|
| `CORE_GATEWAY_URL` | 核心网关地址 | `http://localhost:8080` | `https://api.tailor-is.com` |
| `LITE_GATEWAY_URL` | 轻量网关地址 | `http://localhost:8081` | `https://api-lite.tailor-is.com` |

### 5.2 各前端项目配置

#### PC Mall (`tailor-is-frontend/pc-mall/`)

```bash
# .env.development
CORE_GATEWAY_URL=http://localhost:8080
LITE_GATEWAY_URL=http://localhost:8081

# .env.production
CORE_GATEWAY_URL=https://api.tailor-is.com
LITE_GATEWAY_URL=https://api-lite.tailor-is.com
```

#### Merchant Admin (`tailor-is-frontend/merchant-admin/`)

```bash
# .env.development
CORE_GATEWAY_URL=http://localhost:8080
LITE_GATEWAY_URL=http://localhost:8081

# .env.production
CORE_GATEWAY_URL=https://api.tailor-is.com
LITE_GATEWAY_URL=https://api-lite.tailor-is.com
```

#### Mobile App (`tailor-is-frontend/mobile-app/`)

```javascript
// config/index.ts
export const GATEWAY_CONFIG = {
  CORE_GATEWAY_URL: import.meta.env.CORE_GATEWAY_URL || 'http://localhost:8080',
  LITE_GATEWAY_URL: import.meta.env.LITE_GATEWAY_URL || 'http://localhost:8081',
}
```

#### Platform Admin (`tailor-is-frontend/platform-admin/`)

```bash
# .env.development
CORE_GATEWAY_URL=http://localhost:8080
LITE_GATEWAY_URL=http://localhost:8081

# .env.production
CORE_GATEWAY_URL=https://api.tailor-is.com
LITE_GATEWAY_URL=https://api-lite.tailor-is.com
```

### 5.3 前端请求分发策略

前端需要根据 API 路径自动选择正确的网关：

```javascript
// utils/gateway-router.js
const CORE_PATHS = [
  '/api/user', '/api/auth', '/api/product', '/api/favorite',
  '/api/order', '/api/cart', '/api/payment', '/api/settlement',
  '/api/account', '/api/sandbox', '/api/marketing', '/api/coupon',
  '/api/points', '/api/seckill', '/api/ai', '/api/body-size',
  '/api/copyright', '/api/merchant', '/api/shop', '/api/admin', '/api/pattern'
];

const LITE_PATHS = [
  '/api/community', '/api/post', '/api/comment',
  '/api/academy', '/api/course',
  '/api/supply',
  '/api/message', '/api/notice',
  '/api/im', '/api/im-message',
  '/api/analytics', '/api/metrics', '/api/dashboard'
];

export function getGatewayUrl(path) {
  const isCore = CORE_PATHS.some(p => path.startsWith(p));
  const isLite = LITE_PATHS.some(p => path.startsWith(p));

  if (isCore) {
    return `${import.meta.env.CORE_GATEWAY_URL}${path}`;
  } else if (isLite) {
    return `${import.meta.env.LITE_GATEWAY_URL}${path}`;
  } else {
    // 默认走核心网关
    console.warn(`[Gateway Router] Unknown path ${path}, routing to core gateway`);
    return `${import.meta.env.CORE_GATEWAY_URL}${path}`;
  }
}
```

### 5.4 Nginx 统一入口配置 (生产环境)

```nginx
upstream core_gateway {
    server 127.0.0.1:8080;
}

upstream lite_gateway {
    server 127.0.0.1:8081;
}

server {
    listen 443 ssl;
    server_name api.tailor-is.com;

    # Core API paths
    location ~ ^/api/(user|auth|product|favorite|order|cart|payment|settlement|account|sandbox|marketing|coupon|points|seckill|ai|body-size|copyright|merchant|shop|admin|pattern) {
        proxy_pass http://core_gateway;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Lite API paths
    location ~ ^/api/(community|post|comment|academy|course|supply|message|notice|im|im-message|analytics|metrics|dashboard) {
        proxy_pass http://lite_gateway;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 6. 部署指南

### 6.1 启动顺序

```bash
# 1. 启动 Nacos
docker start nacos

# 2. 启动 Redis
docker start redis

# 3. 启动 Sentinel Dashboard (仅核心网关需要)
java -jar sentinel-dashboard.jar

# 4. 启动核心网关
cd tailor-is-core-gateway
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 5. 启动轻量网关
cd tailor-is-lite-gateway
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 6.2 Docker 部署

```yaml
# docker-compose.yml
services:
  core-gateway:
    build: ./tailor-is-core-gateway
    ports:
      - "8080:8080"
    environment:
      - NACOS_ADDR=nacos:8848
      - REDIS_HOST=redis
      - SENTINEL_DASHBOARD_ADDR=sentinel:8080
    depends_on:
      - nacos
      - redis

  lite-gateway:
    build: ./tailor-is-lite-gateway
    ports:
      - "8081:8081"
    environment:
      - NACOS_ADDR=nacos:8848
      - REDIS_HOST=redis
    depends_on:
      - nacos
      - redis
```

---

## 7. 迁移步骤

### 7.1 第一阶段：并行运行

1. 保持原有 `tailor-is-gateway` (8080) 正常运行
2. 启动 `tailor-is-core-gateway` (8082 临时端口)
3. 启动 `tailor-is-lite-gateway` (8081)
4. 验证新网关功能

### 7.2 第二阶段：灰度切换

1. Nginx 将 10% 流量路由到 core-gateway
2. 观察日志、错误率、延迟
3. 逐步增加比例: 10% → 25% → 50% → 100%

### 7.3 第三阶段：下线旧网关

1. 确认所有流量已迁移
2. 停止 `tailor-is-gateway` 服务
3. 从 Nacos 注销旧网关实例
4. 后续可从代码库移除 (保留一段时间作为参考)

---

## 8. 监控与运维

### 8.1 健康检查端点

| 网关 | 健康检查 URL |
|------|-------------|
| Core | `http://localhost:8080/actuator/health` |
| Lite | `http://localhost:8081/actuator/health` |

### 8.2 路由查询

| 网关 | 路由查看 URL |
|------|-------------|
| Core | `http://localhost:8080/actuator/gateway/routes` |
| Lite | `http://localhost:8081/actuator/gateway/routes` |

### 8.3 Prometheus 指标

| 网关 | Metrics URL |
|------|-------------|
| Core | `http://localhost:8080/actuator/prometheus` |
| Lite | `http://localhost:8081/actuator/prometheus` |

### 8.4 Sentinel Dashboard

核心网关的流控规则在 Sentinel Dashboard 查看:
`http://localhost:8080` (默认端口, 可能与 core-gateway 冲突, 建议修改 Sentinel dashboard 端口)

---
