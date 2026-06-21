# Pull Request 模板

## PR 描述

<!-- 简要描述本 PR 的目的和主要变更点 -->

### 关联 Issue
<!-- 关联的 Issue ID, 例如: Closes #H-01, Refs #C-05 -->

## 变更类型
- [ ] 新功能
- [ ] Bug 修复
- [ ] 重构
- [ ] 文档更新
- [ ] 安全修复
- [ ] 性能优化
- [ ] Breaking Change

## 实施细节

<!-- 详细描述实施内容, 包括:
- 涉及哪些模块
- 关键代码改动
- 配置文件变更
- 数据库变更 (是否需要执行 SQL 脚本)
- 是否破坏向后兼容
-->

### 涉及模块
- [ ] tailor-is-common
- [ ] tailor-is-user
- [ ] tailor-is-merchant
- [ ] tailor-is-product
- [ ] tailor-is-order
- [ ] tailor-is-payment
- [ ] tailor-is-marketing
- [ ] tailor-is-community
- [ ] tailor-is-ai
- [ ] tailor-is-copyright
- [ ] tailor-is-gateway (core / lite)
- [ ] 前端 (pc-mall / merchant-admin / platform-admin / mobile-app)
- [ ] 部署 / 运维
- [ ] 文档

## 提交者自查清单
- [ ] 代码已通过本地单元测试
- [ ] 代码已通过静态分析（Checkstyle/PMD/ESLint）
- [ ] 未引入新的 `any` 类型（前端）/未引入新的魔法数字（后端）
- [ ] 未硬编码任何密码、密钥、Token
- [ ] 新增功能已编写单元测试
- [ ] Breaking Change 已标注
- [ ] 相关文档已更新

## 审查者检查清单
- [ ] **安全审查**: 输入校验、认证授权、数据加密
- [ ] **性能审查**: N+1 查询、缓存使用
- [ ] **逻辑审查**: 边界条件、异常处理、事务边界
- [ ] **规范审查**: 命名、结构、重复代码
- [ ] **测试审查**: 覆盖充分、边界场景

## 测试

<!-- 描述测试方式和覆盖情况 -->

### 单元测试
- [ ] 新增/修改单元测试
- [ ] 本地运行 `mvn test` 通过
- [ ] 覆盖率 ≥ 30% (Phase 1 基线)

### 集成测试
- [ ] 端到端验证
- [ ] 性能测试 (P95 ≤ 200ms)
- [ ] 兼容性测试 (浏览器 / 设备)

### CI 验证
- [ ] Backend CI 通过
- [ ] Frontend CI 通过
- [ ] PR Check 通过 (无敏感信息)

## 风险评估

<!-- 描述本次变更的潜在风险和回滚方案 -->

### 风险等级
- [ ] 低: 仅文档/注释
- [ ] 中: 单个服务/模块变更
- [ ] 高: 跨服务变更 / 数据库 schema 变更 / 配置变更

### 回滚方案
<!-- 描述如何回滚本次变更 -->

## 截图 / 日志

<!-- 如适用, 附上关键截图或日志输出 -->

## 规范遵循

- 代码遵循 [CODING_STANDARDS.md](./tailor-is/docs/CODING_STANDARDS.md)
- 通过 Checkstyle 检查
- 通过 PMD 检查
- 单元测试覆盖新增/修改代码
- 已与相关模块负责人 Review

---

## Reviewer

<!-- @ 至少 1 名 Reviewer; 核心模块(user/order/payment/copyright) 需 2 名 -->
@reviewer1
@reviewer2
