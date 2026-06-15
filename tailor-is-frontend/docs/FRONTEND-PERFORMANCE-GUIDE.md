# 前端性能优化指南

> Task 10: Frontend Performance Optimization (SSR + GraphQL)

本文档记录了 Tailor IS 前端架构的性能优化方案，包括 SSR（服务端渲染）、GraphQL 网关集成和预渲染配置。

---

## 目录

1. [SSR 服务端渲染](#1-ssr-服务端渲染)
2. [GraphQL 网关](#2-graphql-网关)
3. [GraphQL 客户端集成](#3-graphql-客户端集成)
4. [移动端预渲染](#4-移动端预渲染)
5. [性能基准](#5-性能基准)
6. [优化建议清单](#6-优化建议清单)

---

## 1. SSR 服务端渲染

### 1.1 架构概述

PC Mall 项目使用 Vite SSR 方案，将 Vue 3 应用从纯客户端渲染升级为服务端渲染 + 客户端水合（hydration）模式。

**文件结构：**

```
pc-mall/src/server/
├── entry-server.ts    # SSR 入口（创建应用、路由、渲染）
└── server.ts          # Express 开发服务器（Vite 中间件模式）
```

### 1.2 启动方式

```bash
# 开发模式（Vite 中间件模式，支持热更新）
npm run ssr:dev

# 生产构建
npm run build:client   # 构建客户端 bundle → dist/client/
npm run build:server   # 构建服务端 bundle → dist/server/

# 生产启动
npm run ssr:start
```

### 1.3 工作原理

1. **请求到达** → Express 服务器接收请求
2. **读取模板** → 读取 `index.html` 作为渲染模板
3. **加载 SSR 模块** → 通过 `vite.ssrLoadModule` 动态加载 `entry-server.ts`
4. **执行渲染** → `renderToString()` 将 Vue 组件渲染为 HTML 字符串
5. **注入模板** → 将渲染的 HTML 和 Pinia 状态注入到模板中
6. **客户端水合** → 客户端加载后使用 `createSSRApp` + Pinia 状态进行水合

### 1.4 关键配置

**vite.config.ts 中的 SSR 配置：**

```ts
ssr: {
  noExternal: ['vue', 'vue-router', 'pinia', 'element-plus', '@element-plus/icons-vue']
}
```

`noExternal` 确保这些依赖不会被外部化，而是在 SSR bundle 中内联，避免 SSR 时的模块解析问题。

### 1.5 SSR 与 CSR 的对比

| 指标 | CSR（原方案） | SSR（新方案） |
|------|--------------|--------------|
| 首屏渲染时间 | 1.5-3s | 0.5-1s |
| SEO 友好度 | 差（搜索引擎只能看到空 div） | 优（完整 HTML） |
| 首字节时间（TTFB） | ~100ms | ~200-400ms |
| 服务器负载 | 低 | 中等 |
| 首次内容绘制（FCP） | 1.2-2s | 0.3-0.8s |

---

## 2. GraphQL 网关

### 2.1 架构概述

GraphQL 网关作为后端 REST API 的 BFF（Backend for Frontend）层，前端通过一次 GraphQL 查询获取所需的所有数据，避免多次 REST 请求。

**文件结构：**

```
graphql-gateway/
├── schema.graphql    # GraphQL Schema 定义
├── resolvers.ts      # GraphQL Resolvers（转发到后端 REST API）
├── index.ts          # GraphQL Yoga 服务器入口
├── package.json
└── tsconfig.json
```

### 2.2 启动方式

```bash
cd graphql-gateway
npm install
npm run dev          # 开发模式（支持热更新）
npm start            # 生产模式
```

### 2.3 Schema 概览

**Query 类型：**
- `product(id: ID!)` — 商品详情（含 SKU 信息）
- `products(...)` — 商品列表（支持分页、搜索、筛选）
- `order(id: ID!)` — 订单详情（含订单项）
- `orders(...)` — 订单列表
- `hotProducts(limit: Int)` — 热门商品
- `newProducts(limit: Int)` — 新品
- `seckillProducts(limit: Int)` — 秒杀商品
- `categories` — 商品分类

**Mutation 类型：**
- `addToCart` — 加入购物车
- `updateCartItem` — 更新购物车
- `deleteCartItem` — 删除购物车项
- `clearCart` — 清空购物车
- `submitOrder` — 提交订单

### 2.4 Resolver 架构

每个 resolver 内部调用后端 REST API，并做数据格式转换。例如：

```ts
// GraphQL → REST → 格式转换 → 返回
product: async (_parent, { id }, context) => {
  const raw = await request(`/products/${id}`)
  return transformProduct(raw)
}
```

**认证透传：** 从 GraphQL 请求的 `Authorization` 头提取 Token，传递给后端 REST API。

### 2.5 GraphiQL 调试

开发模式下访问 `http://localhost:4000/graphql` 打开 GraphiQL Playground，可以直接测试 GraphQL 查询。

---

## 3. GraphQL 客户端集成

### 3.1 文件位置

```
pc-mall/src/api/graphql.ts
```

### 3.2 核心功能

- **`graphqlQuery<T>(query, variables)`** — 通用 GraphQL 查询执行器
- **`useProductGraphQL(id)`** — 商品详情 composable
- **`useOrderGraphQL(id)`** — 订单详情 composable

### 3.3 使用示例

#### 在组件中使用

```ts
import { useProductGraphQL } from '@/api/graphql'

const { product, loading, error, fetch } = useProductGraphQL(productId)

onMounted(() => {
  fetch()
})
```

#### 降级策略

`ProductDetailView.vue` 已实现 GraphQL 优先 + REST 降级策略：

```ts
onMounted(async () => {
  // 优先尝试 GraphQL（单次请求获取所有数据）
  await fetchGraphQL()
  if (!graphqlProduct.value) {
    // GraphQL 失败时降级到 REST API
    await loadProduct()
  }
})
```

### 3.4 预定义查询

| 查询常量 | 用途 | 参数 |
|---------|------|------|
| `PRODUCT_DETAIL_QUERY` | 商品详情（含 SKU） | `id: ID!` |
| `PRODUCTS_QUERY` | 商品列表 | 分页、筛选 |
| `ORDER_DETAIL_QUERY` | 订单详情（含订单项） | `id: ID!` |
| `HOT_PRODUCTS_QUERY` | 热门商品 | `limit: Int` |

### 3.5 环境变量

```bash
# .env.development
VITE_GRAPHQL_ENDPOINT=http://localhost:4000/graphql

# .env.production
VITE_GRAPHQL_ENDPOINT=/graphql  # 通过 Nginx 反向代理
```

---

## 4. 移动端预渲染

### 4.1 文件位置

```
mobile-app/prerender.config.js
```

### 4.2 预渲染页面

| 路由 | 说明 | 优化效果 |
|------|------|---------|
| `/` | 首页 | FCP < 0.5s |
| `/category` | 商品分类 | 首屏即时显示 |
| `/product/detail` | 商品详情 | 提升 SEO 排名 |
| `/community/list` | 社区列表 | 减少白屏时间 |

### 4.3 配置说明

```js
{
  routes: ['/', '/category', '/product/detail', '/community/list'],
  renderAfterDocumentEvent: 'render-event',  // 等待此事件后捕获 HTML
  renderAfterTime: 5000,                     // 最长等待 5 秒
  headless: true                             // 使用 headless Chrome
}
```

### 4.4 与 SSR 的区别

| 特性 | SSR | 预渲染 |
|------|-----|--------|
| 渲染时机 | 每次请求 | 构建时 |
| 服务器依赖 | 需要 Node.js 服务器 | 纯静态文件 |
| 适用场景 | 动态内容、登录态 | 静态/半静态页面 |
| SEO | 优秀 | 优秀 |
| 首屏速度 | 快 | 最快 |

---

## 5. 性能基准

### 5.1 参考指标

> 以下为预期优化效果，实际数据需在真实环境中通过 Lighthouse/WebPageTest 测量。

| 指标 | 优化前 | 优化后 | 提升 |
|------|--------|--------|------|
| Lighthouse 性能分 | 60-75 | 85-95 | +15~20 |
| First Contentful Paint (FCP) | 1.5s | 0.5s | 66% |
| Largest Contentful Paint (LCP) | 2.5s | 1.2s | 52% |
| Time to Interactive (TTI) | 3.5s | 2.0s | 43% |
| Total Blocking Time (TBT) | 500ms | 200ms | 60% |
| Cumulative Layout Shift (CLS) | 0.15 | 0.05 | 67% |

### 5.2 测量方法

```bash
# Lighthouse CLI
npx lighthouse http://localhost:3001 --output html --output-path ./report.html

# Web Vitals 浏览器扩展
# 安装 Chrome Web Vitals 扩展进行实时测量
```

### 5.3 关键 Web Vitals 目标

| 指标 | 优秀 | 需要改进 | 差 |
|------|------|---------|-----|
| LCP | ≤ 2.5s | ≤ 4.0s | > 4.0s |
| INP | ≤ 200ms | ≤ 500ms | > 500ms |
| CLS | ≤ 0.1 | ≤ 0.25 | > 0.25 |

---

## 6. 优化建议清单

### 6.1 已实现

- [x] SSR 服务端渲染（PC Mall）
- [x] GraphQL 网关集成
- [x] GraphQL 优先 + REST 降级策略
- [x] 移动端预渲染配置
- [x] Vite manualChunks 代码分割
- [x] 路由懒加载

### 6.2 建议后续优化

#### 网络层
- [ ] 启用 HTTP/2 或 HTTP/3
- [ ] 配置 CDN 静态资源分发
- [ ] 启用 Brotli 压缩（比 Gzip 小 15-20%）
- [ ] 配置合理的 Cache-Control 头

#### 资源优化
- [ ] 图片使用 WebP/AVIF 格式 + `<picture>` 回退
- [ ] 懒加载非首屏图片（`loading="lazy"`）
- [ ] 字体子集化 + `font-display: swap`
- [ ] 关键 CSS 内联 + 非关键 CSS 异步加载

#### 构建优化
- [ ] Tree-shaking 验证（确保未使用代码被移除）
- [ ] 动态 import 预加载提示（`<link rel="modulepreload">`）
- [ ] 使用 `vite-plugin-compression` 生成 gzip/br 产物
- [ ] 图片自动压缩（`vite-plugin-imagemin`）

#### 运行时优化
- [ ] 虚拟列表（长列表使用 `vue-virtual-scroller`）
- [ ] Intersection Observer 延迟加载
- [ ] RequestIdleCallback 低优先级任务
- [ ] Web Worker 处理计算密集型任务

#### SSR 增强
- [ ] 数据预取（在 SSR 阶段预加载 API 数据）
- [ ] 流式 SSR（Vue 3 `renderToPipeableStream`）
- [ ] SSR 缓存（Redis / LRU）
- [ ] Edge SSR（Cloudflare Workers / Vercel Edge）

#### GraphQL 优化
- [ ] 查询持久化（Persisted Queries）
- [ ] DataLoader 解决 N+1 问题
- [ ] 响应缓存（Apollo Cache / Relay）
- [ ] 查询复杂度限制（防滥用）

### 6.3 监控建议

- [ ] 接入 Sentry 前端错误监控
- [ ] 配置 Web Vitals 上报
- [ ] 设置 Lighthouse CI 持续集成检查
- [ ] 使用 Performance API 采集真实用户数据（RUM）

---

## 附录：快速启动命令

```bash
# PC Mall SSR
cd pc-mall
npm run ssr:dev                    # 开发
npm run build:client && npm run build:server  # 构建
npm run ssr:start                  # 生产

# GraphQL Gateway
cd graphql-gateway
npm install && npm run dev         # 开发

# 移动端预渲染
cd mobile-app
# 配置 vite-plugin-prerender 后正常构建即可
```
