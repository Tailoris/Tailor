# Tailor IS 代码审查指南

## 1. 目的

本指南旨在规范 Tailor IS 项目的代码审查流程，确保代码质量、安全性和可维护性，促进团队协作和知识共享。

## 2. 适用范围

- 所有提交到 `main` 和 `develop` 分支的代码变更
- 所有新增功能、bug 修复、重构和配置变更
- 前端、后端和 DevOps 代码

## 3. 审查流程

### 3.1 提交前检查

开发者在提交 Pull Request 前必须完成：

| 检查项 | 说明 |
|--------|------|
| 代码编译 | 确保项目能正常编译通过 |
| 单元测试 | 新增/修改的代码必须有对应的单元测试 |
| 代码格式化 | 使用项目规定的格式化工具 |
| 静态分析 | 无新增的 lint/type 错误 |
| 文档更新 | 相关文档已同步更新 |

### 3.2 Pull Request 提交

PR 标题格式：
```
[type]: [short description]

例如：
feat: 添加用户注册接口
fix: 修复订单状态更新bug
refactor: 重构支付服务代码
docs: 更新 API 文档
```

PR 描述必须包含：
- 变更目的
- 实现方案
- 测试方法
- 相关 Issue 链接

### 3.3 审查阶段

| 角色 | 职责 |
|------|------|
| 作者 | 提交 PR、响应反馈、修复问题 |
| 审查者 | 检查代码质量、安全性、规范性 |
| 维护者 | 最终批准并合并 |

### 3.4 审查标准

#### 3.4.1 安全性审查

| 检查项 | 说明 |
|--------|------|
| SQL 注入 | 使用参数化查询，禁止字符串拼接 |
| XSS 防护 | 输入验证和输出转义 |
| CSRF 防护 | 表单和 API 的 CSRF 保护 |
| 敏感数据 | 密码使用 BCrypt，敏感信息加密存储 |
| 权限控制 | 接口访问权限校验 |
| 日志安全 | 避免记录敏感信息（密码、token、身份证） |

#### 3.4.2 代码质量审查

| 检查项 | 说明 |
|--------|------|
| 可读性 | 变量/函数命名清晰，代码结构合理 |
| 复杂度 | 避免过长的函数和嵌套 |
| 重复代码 | 提取公共方法，避免重复 |
| 错误处理 | 异常捕获和处理完善 |
| 测试覆盖 | 关键路径有单元测试覆盖 |

#### 3.4.3 架构一致性审查

| 检查项 | 说明 |
|--------|------|
| 设计模式 | 遵循项目统一的设计模式 |
| 依赖管理 | 合理使用依赖，避免循环依赖 |
| 接口设计 | RESTful 风格，返回格式统一 |
| 代码风格 | 遵循项目代码规范 |

## 4. 审查检查清单

### 4.1 后端 Java 代码审查清单

- [ ] 方法参数和返回值使用 `@Valid` 注解
- [ ] 异常处理使用统一的异常处理器
- [ ] 数据库操作使用 MyBatis Plus 参数绑定
- [ ] Redis 操作避免使用 KEYS 命令
- [ ] 日志使用 SLF4J，级别正确
- [ ] 配置使用 `@Value` 或 `@ConfigurationProperties`
- [ ] 事务注解使用正确（`@Transactional`）

### 4.2 前端 TypeScript 代码审查清单

- [ ] 使用 TypeScript 严格模式
- [ ] 避免 `any` 类型
- [ ] React 组件使用 hooks 正确
- [ ] 状态管理符合规范（Pinia/Zustand）
- [ ] API 调用使用统一的请求封装
- [ ] 样式使用 CSS Modules 或 Tailwind
- [ ] 错误边界处理

### 4.3 DevOps 配置审查清单

- [ ] Dockerfile 使用多阶段构建
- [ ] 容器资源限制配置合理
- [ ] 环境变量敏感信息不硬编码
- [ ] 健康检查配置完善
- [ ] 日志配置合理

## 5. 审查反馈规范

### 5.1 反馈标签

| 标签 | 含义 |
|------|------|
| `blocker` | 阻止合并的严重问题 |
| `critical` | 安全或功能缺陷 |
| `major` | 重要改进建议 |
| `minor` | 小改进建议 |
| `nit` | 格式或风格问题 |

### 5.2 反馈模板

```
**问题类型**: blocker/critical/major/minor/nit

**问题描述**: 清晰描述问题

**位置**: 文件路径:行号

**建议修复**: 具体建议

**影响**: 说明问题的影响范围
```

## 6. 合并标准

PR 必须满足以下条件才能合并：

- [ ] 至少 1 位审查者批准
- [ ] 所有 blocker 和 critical 问题已修复
- [ ] CI 构建通过
- [ ] 单元测试覆盖率不低于阈值
- [ ] 没有未解决的讨论

## 7. 审查时间规范

| 变更类型 | 目标响应时间 |
|----------|-------------|
| blocker | 2 小时内 |
| critical | 4 小时内 |
| 普通 PR | 1 个工作日内 |
| 大型特性 | 3 个工作日内 |

## 8. 附录

### 8.1 常用审查工具

- **后端**: SonarQube, Checkstyle, SpotBugs
- **前端**: ESLint, Prettier, TypeScript
- **安全**: OWASP Dependency Check, Snyk
- **代码覆盖率**: JaCoCo, Istanbul

### 8.2 参考资源

- [OWASP Top 10](https://owasp.org/www-community/attacks/Top_10_2021)
- [Spring Security Best Practices](https://spring.io/guides/topicals/spring-security-architecture)
- [React Security Checklist](https://github.com/cypress-io/react-security-checklist)
