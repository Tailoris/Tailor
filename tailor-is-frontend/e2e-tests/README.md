# Tailor IS E2E Tests

基于 Playwright 的 Tailor IS 端到端自动化测试套件。

## 环境要求

- Node.js >= 18
- npm >= 9

## 安装

```bash
cd e2e-tests
npm install
```

安装完成后，Playwright 会自动下载所需的浏览器（chromium、firefox）。

## 配置

测试默认连接 `http://localhost:5173`。可通过环境变量 `BASE_URL` 自定义：

```bash
# Linux / macOS
export BASE_URL=http://localhost:3000

# Windows PowerShell
$env:BASE_URL="http://localhost:3000"
```

> **注意**：pc-mall 和 merchant-admin 的实际开发服务器端口为 `3000`，建议使用 `BASE_URL=http://localhost:3000` 运行测试。

## 运行测试

```bash
# 运行所有测试（无头模式）
npm test

# 运行所有测试（有头模式，可观察浏览器操作）
npm run test:headed

# 运行单个测试文件
npx playwright test tests/homepage.spec.ts

# 运行特定 describe 块
npx playwright test -g "Product Listing"

# 仅使用 chromium 运行
npx playwright test --project=chromium
```

## 查看测试报告

```bash
npm run test:report
```

测试完成后会自动生成 HTML 报告，包含截图追踪和详细的步骤记录。

## 测试文件说明

```
e2e-tests/
├── tests/
│   ├── homepage.spec.ts    # 首页测试：页面加载、导航栏、登录按钮等
│   ├── auth.spec.ts        # 认证测试：登录表单、表单验证、错误消息
│   └── product.spec.ts     # 商品测试：商品列表、搜索、详情页
├── playwright.config.ts    # Playwright 配置文件
└── package.json            # 项目依赖和脚本
```

## 前提条件

运行测试前，请确保对应的前端开发服务器已启动：

```bash
# 在 pc-mall 目录下
cd pc-mall
npm run dev
```