# Tailor IS 移动端 (mobile-app)

## 项目简介

Tailor IS 平台的移动端应用，基于 UniApp + Vue 3 + TypeScript 构建，支持多端编译（H5、微信小程序）。提供商品浏览、分类、购物车、下单、订单管理、售后、社区、商家入驻等移动端购物体验，并具备离线浏览、弱网适配、Service Worker 缓存等能力。

## 技术栈

- **框架**: UniApp 3 + Vue 3.4 + TypeScript 5.4
- **构建工具**: Vite + @dcloudio/vite-plugin-uni
- **样式**: Sass
- **类型支持**: vue-tsc + @dcloudio/types
- **Service Worker**: 自定义 Vite 插件通过 esbuild 编译 sw.ts

## 开发命令

```bash
# 安装依赖
npm install

# H5 端
npm run dev:h5            # 启动 H5 开发服务器
npm run build:h5          # 构建 H5 生产包

# 微信小程序端
npm run dev:mp-weixin     # 启动微信小程序开发模式
npm run build:mp-weixin   # 构建微信小程序产物

# Service Worker
npm run build:sw          # 编译 Service Worker

# 类型检查
npm run type-check        # 一次性类型检查
npm run type-check:watch  # 监听模式类型检查
```

## 目录结构

```
mobile-app/
├── api/                  # 接口请求层（auth、cart、order、product、aftersale 等）
├── components/           # 通用组件（goods-list、price、skeleton、weak-network-indicator 等）
├── config/               # 应用配置
├── native-components/    # 小程序原生组件（transaction-card、aftersale-form、aftersale-progress）
├── pages/                # 页面（uni-app 页面路由）
│   ├── index/                # 首页
│   ├── category/             # 分类
│   ├── product/              # 商品详情
│   ├── cart/                 # 购物车
│   ├── order/                # 订单（确认/列表/详情/离线售后）
│   ├── address/              # 收货地址
│   ├── community/            # 社区
│   ├── merchant/             # 商家入驻
│   ├── login/ register/      # 登录注册
│   └── user/                 # 个人中心
├── scripts/
│   └── build-sw.mjs          # Service Worker 构建脚本
├── src/
│   ├── components/           # H5 端组件（OfflineIndicator）
│   └── service-worker/       # Service Worker 源码
│       ├── sw.ts                 # SW 主文件
│       ├── register.ts           # SW 注册
│       └── offline-db.ts         # 离线数据库
├── types/                # 类型声明（shims.d.ts）
├── utils/                # 工具函数
│   ├── offlineStorage.ts     # 离线存储（IndexedDB / localStorage）
│   ├── autoSync.ts           # 网络恢复自动同步
│   ├── networkMonitor.ts     # 网络状态监测
│   ├── cartCache.ts          # 购物车缓存
│   ├── productCache.ts       # 商品缓存
│   └── crypto.ts             # 加密工具
├── App.vue               # 根组件
├── main.ts               # 应用入口
├── index.html            # H5 HTML 入口
├── pages.json            # uni-app 页面路由配置
├── manifest.json         # uni-app 应用配置
├── prerender.config.js   # 预渲染配置
├── uni.scss              # 全局样式变量
└── vite.config.ts        # Vite 配置（含 SW 编译插件）
```

## 离线能力说明

移动端具备完整的离线浏览与弱网适配能力：

- **IndexedDB 离线存储**：通过 `utils/offlineStorage.ts` 批量持久化商品、订单等数据，支持离线浏览
- **localStorage 配置存储**：保存用户 Token、应用配置等轻量数据
- **Service Worker 缓存**：`src/service-worker/sw.ts` 经 esbuild 编译为 `sw.js`，实现静态资源 Cache First、API 数据 Network First 的缓存策略
- **自动同步**：`utils/autoSync.ts` 监听网络恢复事件，自动将本地变更同步至服务端
- **弱网监测**：`utils/networkMonitor.ts` 基于 `navigator.connection` API 检测网络质量，弱网时自动降级图片分辨率
- **小程序原生组件**：交易、售后等关键页面在 `pages.json` 中配置 `usingComponents`，小程序端优先使用原生组件，H5 端降级为 Web 组件
