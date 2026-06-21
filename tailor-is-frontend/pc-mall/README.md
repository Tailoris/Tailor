# Tailor IS PC 商城 (pc-mall)

## 项目简介

Tailor IS 平台的 PC 端商城应用，面向 C 端消费者，提供商品浏览、搜索、购物车、下单、社区互动、秒杀等完整购物体验。基于 Vue 3 + TypeScript + Vite 构建，支持 SSR 服务端渲染以优化首屏加载性能。

## 技术栈

- **框架**: Vue 3.4 + TypeScript 5.3
- **构建工具**: Vite 6
- **状态管理**: Pinia 2
- **路由**: Vue Router 4
- **UI 组件**: Element Plus 2.5 + @element-plus/icons-vue
- **国际化**: Vue I18n 9
- **HTTP 客户端**: Axios
- **SSR**: Express + tsx（开发环境使用 Vite 中间件）
- **样式**: Sass（sass-embedded）
- **其他**: Swiper（轮播）、DOMPurify（XSS 防护）、vue3-lazyload（图片懒加载）

## 开发命令

```bash
# 安装依赖
npm install

# 启动开发服务器（CSR 模式）
npm run dev

# 构建生产包（CSR）
npm run build

# SSR 相关构建
npm run build:client    # 构建客户端产物到 dist/client
npm run build:server    # 构建 SSR 服务端产物到 dist/server
npm run build:ssr       # 一键构建客户端 + 服务端

# SSR 开发 / 启动
npm run ssr:dev         # 开发模式启动 SSR 服务
npm run ssr:start       # 生产模式启动 SSR 服务

# 生产部署
npm run start:prod      # 启动生产服务
npm run start:pm2       # 通过 PM2 启动

# Docker 部署
npm run docker:ssr:build   # 构建 SSR Docker 镜像
npm run docker:ssr:run     # 运行 SSR 容器
npm run docker:prod:build  # 构建生产 Docker 镜像
npm run docker:prod:run    # 运行生产容器

# 代码检查
npm run lint            # ESLint 检查并自动修复
```

## 目录结构

```
pc-mall/
├── src/
│   ├── api/              # 接口请求层（auth、cart、order、product 等）
│   ├── components/       # 通用组件（AppHeader、ProductCard、SearchBar 等）
│   ├── composables/      # 组合式函数（useCountdown）
│   ├── locales/          # 国际化资源（zh-CN、en-US）
│   ├── router/           # 路由配置
│   ├── server/           # SSR 服务端
│   │   ├── server.ts         # SSR 入口（开发环境使用 Vite 中间件）
│   │   └── entry-server.ts   # 服务端渲染入口
│   ├── store/            # Pinia 状态（app、cart、user）
│   ├── styles/           # 全局样式与响应式断点
│   ├── types/            # 类型定义
│   ├── utils/            # 工具函数（crypto、format、storage、validate）
│   ├── views/            # 页面视图
│   ├── App.vue           # 根组件
│   ├── i18n.ts           # 国际化配置
│   ├── main.ts           # 应用入口
│   └── style.css         # 全局样式
├── Dockerfile.ssr        # SSR Docker 镜像配置
├── Dockerfile.prod       # 生产 Docker 镜像配置
├── ecosystem.config.js   # PM2 配置
├── index.html            # HTML 入口
├── server.prod.ts        # 生产服务入口
├── vite.config.ts        # Vite 配置
└── tsconfig.json         # TypeScript 配置
```

## 环境变量配置

通过 `.env.development` / `.env.production` 文件配置，所有前端变量以 `VITE_` 前缀开头：

| 变量 | 说明 | 示例 |
|------|------|------|
| `VITE_API_BASE_URL` | API 基础路径 | `/api` |
| `VITE_APP_TITLE` | 应用标题 | `Tailor IS PC Mall` |
| `VITE_APP_ENV` | 环境标识 | `development` |
| `VITE_PORT` | 开发服务器端口 | `5173` |
| `VITE_CORE_GATEWAY_URL` | 核心网关地址 | `http://localhost:8080` |
| `VITE_LITE_GATEWAY_URL` | 轻量网关地址 | `http://localhost:8081` |

## SSR 说明

- 开发环境通过 Vite 中间件实现 SSR 热更新
- 生产环境通过 `npm run build:ssr` 构建客户端与服务端产物
- Pinia 状态序列化至 `window.__PINIA_STATE__`，客户端 Hydration 避免重复请求
- 默认服务端口为 3000（生产容器）
