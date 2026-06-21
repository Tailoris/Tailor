# Tailor IS 商家后台 (merchant-admin)

## 项目简介

Tailor IS 平台的商户管理后台，面向入驻商户，提供商品管理、订单管理、售后处理、营销活动（秒杀/优惠券）、财务结算、店铺设置、员工管理等经营能力。基于 Vue 3 + TypeScript + Vite + Element Plus 构建。

## 技术栈

- **框架**: Vue 3.4 + TypeScript 5.3
- **构建工具**: Vite 6
- **状态管理**: Pinia 2
- **路由**: Vue Router 4
- **UI 组件**: Element Plus 2.4 + @element-plus/icons-vue
- **国际化**: Vue I18n 9
- **HTTP 客户端**: Axios
- **样式**: Sass

## 开发命令

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产包
npm run build

# 预览生产构建
npm run preview
```

## 目录结构

```
merchant-admin/
├── src/
│   ├── api/              # 接口请求层（auth、product、order、aftersale、finance、marketing 等）
│   ├── components/       # 通用组件（AdminLayout、SidebarMenu、OrderTable、ProductTable 等）
│   ├── composables/      # 组合式函数（useCountdown）
│   ├── i18n/             # 国际化配置
│   ├── locales/          # 国际化资源（zh-CN、en-US）
│   ├── router/           # 路由配置
│   ├── store/            # Pinia 状态（app、user）
│   ├── styles/           # 全局样式与响应式
│   ├── types/            # 类型定义
│   ├── utils/            # 工具函数（crypto、storage、validate）
│   ├── views/            # 页面视图
│   │   ├── finance/          # 财务模块（提现）
│   │   ├── marketing/        # 营销模块（秒杀列表）
│   │   ├── DashboardView.vue
│   │   ├── ProductListView.vue / ProductFormView.vue
│   │   ├── OrderListView.vue / OrderDetailView.vue
│   │   ├── AfterSaleListView.vue / AfterSaleDetailView.vue
│   │   └── ...
│   ├── App.vue           # 根组件
│   ├── i18n.ts           # 国际化入口
│   ├── main.ts           # 应用入口
│   └── style.css         # 全局样式
├── .env.development      # 开发环境变量
├── env.d.ts              # 环境变量类型声明
├── index.html            # HTML 入口
├── vite.config.ts        # Vite 配置
└── tsconfig.json         # TypeScript 配置
```
