# 贡献指南

感谢你关注 Tailor IS（裁智云）项目！我们欢迎各种形式的贡献。

## 如何贡献

### 1. Fork 与克隆

```bash
git clone <your-fork-url>
cd Tailoris
```

### 2. 创建分支

以功能或修复描述命名分支：

```bash
git checkout -b feat/add-coupon-validation
git checkout -b fix/order-status-typo
```

### 3. 开发

- **后端**：遵循 [编码规范](tailor-is/docs/CODING_STANDARDS.md)
- **前端**：使用 TypeScript，保持组件单一职责
- 提交前运行 `mvn test` 和 `npm run lint` 确保通过

### 4. 提交信息

遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```
feat: 添加优惠券批量生成功能
fix: 修复订单状态同步延迟
docs: 更新部署指南
refactor: 重构用户权限校验逻辑
```

### 5. 提交 Pull Request

- 在 PR 描述中说明变更目的和影响范围
- 关联相关的 Issue（使用 `Closes #123` 语法）
- 确保 CI 检查通过

## 开发环境搭建

### 后端

```bash
cd tailor-is
mvn clean install -DskipTests
```

需要 JDK 17+、Maven 3.8+。本地开发需要启动 Nacos、Redis、MySQL 等依赖服务。

### 前端

```bash
cd tailor-is-frontend/pc-mall
npm install
npm run dev
```

需要 Node.js 18+。

## 代码审查

所有 PR 至少需要一位维护者审查后合并。审查关注点：

- 功能正确性与完整性
- 代码质量与可读性
- 安全性（SQL 注入、XSS、权限绕过等）
- 性能影响
- 测试覆盖

## 问题报告

提交 Issue 时请包含：

- 问题描述（含复现步骤）
- 预期行为与实际行为
- 环境信息（JDK 版本、Node 版本、浏览器等）
- 相关日志或截图

## 分支策略

- `main` — 稳定发布分支
- `develop` — 开发集成分支
- `feat/*` — 功能开发分支
- `fix/*` — Bug 修复分支
- `release/*` — 发布准备分支

## 许可证

贡献代码即表示你同意将代码以 Apache 2.0 许可证发布。
