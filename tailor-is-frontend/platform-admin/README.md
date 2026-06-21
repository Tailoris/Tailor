# Tailor IS 平台管理后台 (platform-admin)

## 项目简介

Tailor IS 平台的管理后台，面向平台运营人员，提供平台数据看板、系统设置等管理能力。基于 Vue 3 + TypeScript + Vite + Element Plus 构建。

## 技术栈

- **框架**: Vue 3.4 + TypeScript 5.4
- **构建工具**: Vite 6
- **状态管理**: Pinia 2
- **路由**: Vue Router 4
- **UI 组件**: Element Plus 2.6 + @element-plus/icons-vue
- **HTTP 客户端**: Axios
- **其他**: Dayjs（日期处理）、Express（Mock 服务）

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

# 代码检查
npm run lint
```

## 目录结构

```
platform-admin/
├── src/
│   ├── api/              # 接口请求层（auth、dashboard、settings、request）
│   ├── components/       # 通用组件（AdminLayout）
│   ├── composables/      # 组合式函数（useCountdown）
│   ├── router/           # 路由配置
│   ├── utils/            # 工具函数（crypto、storage、validate）
│   ├── views/            # 页面视图
│   │   ├── dashboard/        # 数据看板（DashboardView.vue）
│   │   ├── system/           # 系统设置（SettingsView.vue）
│   │   └── LoginView.vue
│   ├── App.vue           # 根组件
│   ├── main.ts           # 应用入口
│   └── style.css         # 全局样式
├── .env.development      # 开发环境变量
├── env.d.ts              # 环境变量类型声明
├── index.html            # HTML 入口
├── mock-server.js        # 本地 Mock 服务
├── vite.config.ts        # Vite 配置
└── tsconfig.json         # TypeScript 配置
```
