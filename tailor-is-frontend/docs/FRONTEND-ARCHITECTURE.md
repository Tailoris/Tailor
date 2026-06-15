# Tailor IS 前端架构优化设计文档

## 1. 概述

Tailor IS 前端采用多端架构，覆盖 PC 商城、商户管理后台、平台管理后台和移动端四个应用场景。本文档描述前端架构优化设计，包括 SSR、GraphQL、预渲染、离线能力、小程序原生组件等关键技术方案。

## 2. 多端架构

### 2.1 项目矩阵

| 项目 | 技术栈 | 端口 | 说明 |
|------|--------|------|------|
| pc-mall | Vue 3 + Vite 6 + Pinia | 3001 | PC 端商城，含 SSR |
| merchant-admin | Vue 3 + Vite + Element Plus | 3002 | 商户管理后台 |
| platform-admin | Vue 3 + Vite + Element Plus | 3003 | 平台管理后台 |
| mobile-app | uni-app + Vue 3 | 5173 | 移动端 / 小程序 |
| graphql-gateway | Node.js + GraphQL Yoga | 4000 | GraphQL 接口聚合层 |

### 2.2 共享模块

```
@shared/
├── composables/        # useCountdown, useDebounce 等
├── utils/              # storage, validate, format 等
├── components/         # a11y 无障碍组件
└── types/              # 公共类型定义
```

## 3. SSR 服务端渲染

### 3.1 架构设计

```
用户请求 → Nginx → Express (SSR) → Vite SSR → Vue App
                                      ↓
                              Pinia 状态序列化 → 客户端 Hydration
```

### 3.2 关键文件

- [server.ts](file:///home/tailor/Tailoris/tailor-is-frontend/pc-mall/src/server/server.ts) — SSR 入口，开发环境使用 Vite 中间件
- [entry-server.ts](file:///home/tailor/Tailoris/tailor-is-frontend/pc-mall/src/server/entry-server.ts) — 服务端渲染入口
- [Dockerfile.ssr](file:///home/tailor/Tailoris/tailor-is-frontend/pc-mall/Dockerfile.ssr) — 生产环境 Docker 镜像

### 3.3 构建命令

```bash
npm run build:client    # 构建客户端
npm run build:server    # 构建 SSR 服务端
npm run build:ssr       # 一键构建
npm run docker:ssr:build  # Docker 镜像构建
```

### 3.4 性能优化

- Pinia 状态序列化到 `window.__PINIA_STATE__`
- 客户端 Hydration 避免重复请求
- Nginx 缓存 SSR 输出（静态页面缓存 5 分钟）

## 4. GraphQL 接口聚合

### 4.1 架构

```
客户端 → GraphQL Gateway (4000) → REST API (8080/8081)
              ↓
          Redis 缓存层
```

### 4.2 缓存策略

| 查询 | TTL | 说明 |
|------|-----|------|
| product | 5 min | 商品信息变更频率低 |
| hotProducts | 3 min | 列表页面需较新数据 |
| newProducts | 3 min | 新品推荐 |
| seckillProducts | 3 min | 秒杀活动 |
| categories | 15 min | 分类极少变更 |

### 4.3 缓存失效

- 订单提交时自动失效相关商品缓存
- 基于 TTL 自动过期
- 手动调用 `invalidate()` 方法

## 5. 移动端预渲染

### 5.1 配置

使用 `prerender-spa-plugin` 对关键页面进行预渲染：
- 首页 (/)
- 分类页 (/category)
- 商品详情热门页

### 5.2 离线能力

- [IndexedDB](file:///home/tailor/Tailoris/tailor-is-frontend/mobile-app/utils/offlineStorage.ts) — 批量数据离线存储
- [localStorage](file:///home/tailor/Tailoris/tailor-is-frontend/mobile-app/utils/offlineStorage.ts) — 配置/Token 存储
- [autoSync](file:///home/tailor/Tailoris/tailor-is-frontend/mobile-app/utils/autoSync.ts) — 网络恢复自动同步
- [Service Worker](file:///home/tailor/Tailoris/tailor-is-frontend/mobile-app/src/static/sw.js) — 离线缓存

### 5.3 缓存策略

- 静态资源: Cache First
- API 数据: Network First
- 页面 HTML: Network First

## 6. 小程序原生组件集成

### 6.1 配置页面

在 [pages.json](file:///home/tailor/Tailoris/tailor-is-frontend/mobile-app/src/pages.json) 中为交易/售后页面配置 `usingComponents`：

- `pages/order/confirm` — 确认订单
- `pages/order/list` — 订单列表
- `pages/order/detail` — 订单详情
- `pages/cart/cart` — 购物车

### 6.2 兼容降级

- H5 端：使用 Web 组件替代
- 小程序端：优先使用原生组件
- 自动检测运行环境，动态选择组件

## 7. 弱网适配

### 7.1 网络状态监测

- 使用 `navigator.connection` API（含 Safari 降级）
- 监听 `online`/`offline` 事件
- 自动切换图片质量（低分辨率 / 高分辨率）

### 7.2 UI 降级策略

- 弱网时显示低分辨率图片
- 离线时显示缓存数据 + 离线提示
- 网络恢复后自动同步数据

## 8. 无障碍访问 (A11y)

### 8.1 组件

- `A11ySkipLink` — 跳过导航链接
- `A11yAnnouncer` — 屏幕阅读器通知
- `A11yFocusTrap` — 焦点陷阱

### 8.2 标准

- 目标: WCAG 2.1 AA 级
- 工具: axe DevTools 自动检测

## 9. 性能目标

| 指标 | 目标 | 当前状态 |
|------|------|---------|
| PC 首屏加载 | ≤ 1.5s | SSR 已实现 |
| 移动端首屏加载 | ≤ 2s | 预渲染已实现 |
| GraphQL 响应 | ≤ 200ms (缓存命中) | Redis 缓存已实现 |
| 离线可用性 | 3G 网络核心功能可用 | 离线存储 + SW 已实现 |
| 小程序性能 | 原生组件渲染 | usingComponents 已配置 |

## 10. 部署架构

```
                    ┌─────────────┐
                    │   Nginx     │
                    │  (反向代理)  │
                    └──────┬──────┘
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
    ┌─────────────┐ ┌──────────────┐ ┌──────────────┐
    │ SSR Server  │ │ GraphQL GW   │ │ Static Files │
    │  (3000)     │ │  (4000)      │ │  (5173)      │
    └─────────────┘ └──────────────┘ └──────────────┘
           │               │
           ▼               ▼
    ┌─────────────────────────────────────────┐
    │           Backend API Gateway            │
    │     core-gateway:8080 / lite:8081        │
    └─────────────────────────────────────────┘
```