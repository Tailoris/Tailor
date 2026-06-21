# Tailor IS GraphQL 网关 (graphql-gateway)

## 项目简介

Tailor IS 平台的 GraphQL 接口聚合层，基于 GraphQL Yoga 构建。负责将后端 REST API（core-gateway:8080 / lite:8081）聚合为统一的 GraphQL 接口，供前端各端调用。内置 Redis 缓存层与 DataLoader 批量加载能力，有效降低后端压力并提升响应速度。

## 技术栈

- **运行时**: Node.js + TypeScript 5.3
- **GraphQL 服务**: GraphQL Yoga 5
- **缓存**: Redis（ioredis 5）
- **批量加载**: DataLoader 2
- **HTTP 客户端**: Axios
- **执行**: tsx（开发热重载 / 生产运行）

## 开发命令

```bash
# 安装依赖
npm install

# 启动开发服务器（热重载）
npm run dev

# 生产启动
npm run start

# 类型编译构建
npm run build
```

## 目录结构

```
graphql-gateway/
├── src/
│   └── cache/            # 缓存模块
│       ├── cacheConfig.ts    # 缓存配置（TTL 策略）
│       ├── dataLoaderFactory.ts  # DataLoader 工厂
│       └── redisCache.ts     # Redis 缓存实现
├── cache.ts              # 缓存入口
├── index.ts              # 服务入口（GraphQL Yoga 启动）
├── resolvers.ts          # GraphQL 解析器
├── schema.graphql        # GraphQL Schema 定义
├── Dockerfile            # Docker 镜像配置
├── tsconfig.json         # TypeScript 配置
└── package.json
```

## DataLoader 说明

DataLoader 用于解决 GraphQL 查询中的 N+1 问题，通过批量加载与请求合并减少后端调用次数：

- **批量加载**：在同一事件循环周期内的多次单条查询会被合并为一次批量请求发送至后端
- **缓存去重**：同一 key 在单次请求周期内只查询一次，后续命中缓存
- **工厂模式**：`src/cache/dataLoaderFactory.ts` 集中管理各资源的 DataLoader 实例（商品、分类等），每次 GraphQL 请求独立创建实例以避免跨请求缓存污染

## Redis 缓存策略

| 查询 | TTL | 说明 |
|------|-----|------|
| product | 5 min | 商品信息变更频率低 |
| hotProducts | 3 min | 列表页面需较新数据 |
| newProducts | 3 min | 新品推荐 |
| seckillProducts | 3 min | 秒杀活动 |
| categories | 15 min | 分类极少变更 |

缓存失效策略：
- 订单提交时自动失效相关商品缓存
- 基于 TTL 自动过期
- 手动调用 `invalidate()` 方法失效指定缓存
