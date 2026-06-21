# Tailor IS 质量保障长效机制

> 版本: v1.0 | 日期: 2026-06-20 | 等级: P3

---

## 一、自动化测试执行规范

### 1.1 测试分层

| 层级 | 覆盖范围 | 工具 | 目标覆盖率 | 当前状态 |
|------|---------|------|-----------|---------|
| 单元测试 | Service / Util / Filter | JUnit 5 + Mockito | ≥80% | 部分实现 |
| 集成测试 | Controller / Repository | Spring Boot Test | ≥60% | 待建设 |
| E2E 测试 | 核心业务流程 | Playwright | 核心流程 100% | 已有框架 |
| 契约测试 | 网关-服务接口 | Spring Cloud Contract | 关键接口 100% | 待建设 |

### 1.2 后端测试规范

- **命名规范**: `{ClassName}Test.java`，测试方法名使用 `should_{预期行为}_when_{条件}` 格式
- **覆盖要求**: 每个 Service 公开方法至少 1 个正向 + 1 个异常场景测试
- **Mock 规范**: 对外部依赖（Repository、FeignClient、MQ）必须 Mock，禁止 Mock 被测类本身
- **断言规范**: 使用 AssertJ 流式断言，禁止使用 `assertTrue`/`assertFalse` 裸断言

### 1.3 前端测试规范

- **组件测试**: Vitest + Vue Test Utils，覆盖率 ≥70%
- **E2E 测试**: Playwright，覆盖注册→登录→浏览→下单→支付→售后全流程
- **视觉回归**: Percy / Chromatic，防止 UI 意外变更

### 1.4 执行要求

- 每次提交前本地运行 `mvn test` 确保全部通过
- PR 合并前 CI 中 `mvn test` 必须通过
- 新增功能必须附带对应单元测试，否则审查者有权拒绝合并
- 测试覆盖率低于基线（当前 30%，目标 80%）的模块需在后续迭代中补齐

---

## 二、代码审查执行规范

### 2.1 审查流程

```
开发者提交 PR → CI 自动检查通过 → 分配审查者 → 审查反馈 → 修复 → 批准 → 合并
```

1. 开发者提交 PR 并填写 [PR 模板](.github/PULL_REQUEST_TEMPLATE.md) 中的自查清单
2. CI 自动检查（Backend CI / Frontend CI / PR Check）必须全部通过
3. 审查者按审查清单逐项检查，核心模块（user/order/payment/copyright）需 2 名审查者
4. 审查意见需在 1 个工作日内响应
5. 所有审查意见已解决（Resolved）后方可合并

### 2.2 审查者检查清单

- [ ] **安全审查**: 输入校验、认证授权、数据加密
- [ ] **性能审查**: N+1 查询、缓存使用
- [ ] **逻辑审查**: 边界条件、异常处理、事务边界
- [ ] **规范审查**: 命名、结构、重复代码
- [ ] **测试审查**: 覆盖充分、边界场景

### 2.3 审查分级

| 变更类型 | 审查要求 | 响应时限 |
|---------|---------|---------|
| 文档 / 注释 | 1 名审查者 | 2 个工作日 |
| 单模块变更 | 1 名审查者 | 1 个工作日 |
| 跨模块变更 | 2 名审查者 | 1 个工作日 |
| 核心模块变更 | 2 名审查者 | 24 小时内 |
| 安全相关变更 | 2 名审查者 + 安全审查 | 24 小时内 |

---

## 三、CI/CD 门禁执行规范

### 3.1 流水线架构

```
PR 提交
  │
  ├──▶ [PR Check] 代码规范 + 废弃文件检测 + 安全扫描
  │
  ├──▶ [Backend CI] 编译 → 单元测试 → JaCoCo → SonarQube → Checkstyle/PMD → Docker 构建
  │
  ├──▶ [Frontend CI] Lint → 类型检查 → 构建 → E2E 测试
  │
  └──▶ [Security Scan] gitleaks 密钥泄露扫描
```

### 3.2 质量门禁

| 门禁项 | 工具 | 阈值 | 阻断级别 |
|--------|------|------|---------|
| 代码规范 | Checkstyle | 0 error | 阻断合并 |
| 代码缺陷 | PMD | 0 高优 | 阻断合并 |
| 测试覆盖率 | JaCoCo | ≥30%（Phase 1 基线） | 警告 |
| 代码质量 | SonarQube | Quality Gate passed | 阻断合并 |
| 安全漏洞 | OWASP Dependency Check | 0 Critical/High | 阻断合并 |
| 密钥泄露 | gitleaks | 0 个 | 阻断合并 |
| 编译 | Maven | 编译成功 | 阻断合并 |

### 3.3 已有 CI 工作流

| 工作流 | 文件 | 功能 | 状态 |
|--------|------|------|------|
| Backend CI | `.github/workflows/backend-ci.yml` | 编译/测试/JaCoCo/SonarQube/Checkstyle/PMD/Docker | ✅ 运行中 |
| Frontend CI | `.github/workflows/frontend-ci.yml` | Lint/构建/E2E | ✅ 运行中 |
| PR Check | `.github/workflows/pr-check.yml` | 代码规范/废弃文件检测 | ✅ 运行中 |
| Security Scan | `.github/workflows/backend-ci.yml` (Job 4) | gitleaks 密钥扫描 | ✅ 运行中 |

### 3.4 门禁执行规则

- **阻断合并**: 门禁不通过时，PR 不可合并，必须修复后重新触发 CI
- **警告**: 覆盖率低于基线时发出警告但不阻断，需在 Issue 中跟踪改进
- **豁免**: 紧急热修复（hotfix）经技术负责人批准后可临时豁免非安全门禁，事后 24 小时内补齐

---

## 四、定期评估执行规范

### 4.1 周度评估（每周五）

**执行人**: 技术负责人 / QA 负责人

**执行内容**:
1. 运行 `deploy/scripts/weekly-quality-report.sh` 生成周度质量报告
2. 检查本周 PR 合并数与审查覆盖率
3. 检查本周 CI 构建成功率
4. 检查当前 Firing 告警，评估是否需要调整告警规则
5. 检查安全扫描结果（gitleaks / Trivy / OWASP）
6. 输出《周度质量报告》并归档至 `docs/reports/`

**周报模板**（由脚本自动生成）:
- PR 统计（合并数、最近合并列表）
- 测试覆盖率（按 LINE/BRANCH/INSTRUCTION/METHOD/CLASS 维度）
- 监控告警（本周告警数、当前 Firing 告警）
- 安全扫描（gitleaks 发现数、Trivy 漏洞分布）
- CI/CD 状态（总运行数、成功/失败/取消、成功率）
- 改进建议

### 4.2 月度评估（每月第一周）

**执行人**: 技术负责人

**执行内容**:
1. 汇总本月 4 份周度报告，输出《月度质量巡检报告》
2. 缺陷趋势分析（新增 / 修复 / 遗留）
3. 测试覆盖率变化趋势
4. 安全扫描结果对比
5. 性能指标对比（P50/P95/P99 响应时间、系统可用性）
6. CI/CD 统计（月度构建成功率、平均构建时长）
7. 制定下月质量改进计划

**月度报告额外内容**:
- 缺陷趋势图（按严重等级 P0-P3 分类）
- 模块覆盖率热力图
- 改进措施执行情况跟踪

### 4.3 季度评估（每季度末）

**执行人**: 技术负责人 + 项目经理

**执行内容**:
1. 汇总本季度 3 份月度报告，输出《季度质量总结报告》
2. 质量目标达成情况评估（对比季度初设定的目标）
3. 长期技术债务盘点
4. 质量保障体系有效性评估
5. 工具链评估与升级建议
6. 下季度质量目标与改进计划制定

**季度核心指标**:

| 指标 | 目标值 | 评估方式 |
|------|--------|---------|
| 代码覆盖率 | ≥80% | JaCoCo 报告 |
| Critical/High 缺陷数 | 0 | SonarQube |
| 安全漏洞数 | 0 Critical/High | OWASP + gitleaks |
| 接口 P95 响应时间 | ≤200ms | Prometheus + Grafana |
| 系统可用性 | ≥99.9% | Uptime 监控 |
| CI 构建成功率 | ≥95% | GitHub Actions |
| PR 审查响应时间 | ≤1 工作日 | GitHub PR 统计 |
| 技术债务消除率 | ≥20% | 人工盘点 |

### 4.4 评估报告归档

| 报告类型 | 归档路径 | 保留期限 |
|---------|---------|---------|
| 周度报告 | `docs/reports/weekly-quality-report-{date}.md` | 3 个月 |
| 月度报告 | `docs/reports/monthly-quality-report-{yyyy-MM}.md` | 1 年 |
| 季度报告 | `docs/reports/quarterly-quality-report-{yyyy}-Q{n}.md` | 永久 |

---

## 五、持续改进机制

### 5.1 改进循环

```
度量（Measure）→ 分析（Analyze）→ 改进（Improve）→ 验证（Verify）→ 标准化（Standardize）
```

### 5.2 问题发现渠道

| 渠道 | 频率 | 负责人 |
|------|------|--------|
| 自动化测试失败 | 每次 CI | 开发者 |
| 代码审查意见 | 每次 PR | 审查者 |
| 周度质量报告 | 每周五 | 技术负责人 |
| 月度质量巡检 | 每月第一周 | 技术负责人 |
| 季度质量总结 | 每季度末 | 技术负责人 + PM |
| 用户反馈 / Bug 报告 | 持续 | 全员 |
| 生产环境告警 | 实时 | 运维 |

### 5.3 改进措施执行

- **P0 级改进**: 立即执行，24 小时内完成，热修复流程
- **P1 级改进**: 当前迭代内完成，纳入 Sprint Backlog
- **P2 级改进**: 下个迭代完成，纳入 Release Plan
- **P3 级改进**: 中长期规划，纳入 Roadmap

### 5.4 改进效果验证

- 每项改进措施执行后，需在下一个评估周期（周度/月度）验证效果
- 验证通过则纳入标准化流程（更新本文档/CI 配置/PR 模板）
- 验证不通过则回滚并重新分析根因

### 5.5 工具链演进

| 阶段 | 工具 | 当前状态 |
|------|------|---------|
| Phase 1 | Checkstyle + PMD + JaCoCo + gitleaks | ✅ 已落地 |
| Phase 2 | SonarQube + OWASP Dependency Check | ✅ 已落地 |
| Phase 3 | Trivy 容器扫描 + k6 性能测试 | 待落地 |
| Phase 4 | Spring Cloud Contract 契约测试 | 待规划 |

### 5.6 知识沉淀

- 每次重大质量事件（P0/P1）必须输出 RCA（根因分析）报告，归档至 `docs/reports/`
- 每季度汇总常见问题，更新编码规范（CODING_STANDARDS.md）和 PR 模板
- 建立质量知识库，记录典型缺陷模式与修复方案

---

## 附录

### A. 相关文件索引

| 文件 | 路径 | 说明 |
|------|------|------|
| PR 模板 | `.github/PULL_REQUEST_TEMPLATE.md` | 提交者自查 + 审查者检查清单 |
| Backend CI | `.github/workflows/backend-ci.yml` | 编译/测试/JaCoCo/SonarQube/静态分析 |
| Frontend CI | `.github/workflows/frontend-ci.yml` | Lint/构建/E2E |
| PR Check | `.github/workflows/pr-check.yml` | 代码规范/废弃文件检测 |
| 周度报告脚本 | `deploy/scripts/weekly-quality-report.sh` | 自动生成周度质量报告 |
| SonarQube 配置 | `tailor-is/sonar-project.properties` | SonarQube 项目配置 |
| 编码规范 | `tailor-is/docs/CODING_STANDARDS.md` | 后端编码规范 |
| 质量保障长效机制 | `docs/Tailor-IS-质量保障长效机制.md` | 中文版长效机制文档 |

### B. 版本历史

| 版本 | 日期 | 变更说明 |
|------|------|---------|
| v1.0 | 2026-06-20 | 初始版本：自动化测试、代码审查、CI/CD 门禁、定期评估、持续改进五大体系 |