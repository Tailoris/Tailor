# Tailor IS 质量保障长效机制

> 版本: v1.0 | 日期: 2026-06-19

---

## 一、自动化测试体系

### 1.1 测试分层

| 层级 | 覆盖范围 | 工具 | 目标覆盖率 | 当前状态 |
|------|---------|------|-----------|---------|
| 单元测试 | Service/Util/Filter | JUnit 5 + Mockito | ≥80% | 部分实现 |
| 集成测试 | Controller/Repository | Spring Boot Test | ≥60% | 待建设 |
| E2E测试 | 核心业务流程 | Playwright | 核心流程100% | 已有框架 |
| 契约测试 | 网关-服务接口 | Spring Cloud Contract | 关键接口100% | 待建设 |

### 1.2 后端测试规范

```xml
<!-- pom.xml 必须包含 -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
```

### 1.3 前端测试规范

- **组件测试**: Vitest + Vue Test Utils，覆盖率 ≥70%
- **E2E测试**: Playwright，覆盖注册→登录→浏览→下单→支付→售后全流程
- **视觉回归**: Percy/Chromatic，防止UI意外变更

---

## 二、CI/CD 流水线

### 2.1 流水线架构

```
PR 提交
  │
  ├──▶ [PR Check] 代码规范 + 废弃文件检测 + 安全扫描
  │
  ├──▶ [Backend CI] 编译 → 单元测试 → Checkstyle/PMD → JaCoCo → Docker构建
  │
  ├──▶ [Frontend CI] Lint → 类型检查 → 构建 → E2E测试
  │
  └──▶ [Project Cleanup] 每周定期扫描废弃文件
```

### 2.2 质量门禁

| 门禁项 | 工具 | 阈值 | 阻断级别 |
|--------|------|------|---------|
| 代码规范 | Checkstyle | 0 error | 阻断合并 |
| 代码缺陷 | PMD | 0 高优 | 阻断合并 |
| 测试覆盖率 | JaCoCo | ≥80% | 阻断合并 |
| 安全漏洞 | OWASP Dependency Check | 0 Critical/High | 阻断合并 |
| 废弃文件 | 自定义脚本 | 0 个 | 阻断合并 |
| 敏感信息 | TruffleHog/Gitleaks | 0 个 | 阻断合并 |

### 2.3 已有CI工作流

| 工作流 | 文件 | 功能 | 状态 |
|--------|------|------|------|
| Backend CI | backend-ci.yml | 编译/测试/静态分析/Docker构建 | ✅ 已修复 |
| Frontend CI | frontend-ci.yml | Lint/构建/E2E | ✅ 运行中 |
| PR Check | pr-check.yml | 代码规范/废弃文件/编辑器缓存检测 | ✅ 已增强 |
| Project Cleanup | project-cleanup-check.yml | 每周定期扫描 | ✅ 新建 |

### 2.4 待补充CI环节

1. **OWASP Dependency Check**: 扫描 Maven/npm 依赖漏洞
2. **SonarQube**: 代码质量评分门禁
3. **TruffleHog**: 敏感信息扫描
4. **k6 性能测试**: 核心接口压测

---

## 三、代码审查规范

### 3.1 审查流程

1. 开发者提交 PR，填写变更说明
2. CI 自动检查通过后，分配审查者
3. 审查者重点关注: 安全/并发/性能/规范
4. Critical/High 修复需2名审查者批准
5. 审查意见需在1个工作日内响应

### 3.2 审查清单

- [ ] 输入校验: 所有外部输入已校验
- [ ] 权限控制: 接口有适当的权限注解
- [ ] SQL安全: 无SQL拼接，使用参数化查询
- [ ] 并发安全: 共享状态修改有锁保护
- [ ] 异常处理: 无吞异常，错误信息友好
- [ ] 敏感数据: 无硬编码密码/密钥
- [ ] 性能: 无N+1查询，无KEYS命令
- [ ] 日志: 无敏感信息泄露
- [ ] 测试: 附带测试用例

---

## 四、质量指标监控

### 4.1 核心指标

| 指标 | 目标 | 监控方式 |
|------|------|---------|
| 代码覆盖率 | ≥80% | JaCoCo + CI门禁 |
| Critical/High 缺陷数 | 0 | SonarQube + 人工审查 |
| 安全漏洞数 | 0 | OWASP + TruffleHog |
| 接口 P95 响应 | ≤200ms | Prometheus + Grafana |
| 系统可用性 | ≥99.9% | Uptime监控 |
| CI 构建成功率 | ≥95% | GitHub Actions |

### 4.2 月度质量巡检

每月第一周输出《质量巡检报告》，包含:
- 缺陷趋势图（新增/修复/遗留）
- 覆盖率变化
- 安全扫描结果
- 性能指标对比
- CI/CD 统计
- 改进建议

---

## 五、问题跟踪机制

### 5.1 跟踪流程

```
发现问题 → 创建Issue(含严重等级/类型/描述/复现步骤)
    │
    ├── P0: 立即修复 → 修复后交叉测试 → 验收关闭
    ├── P1: 24h内修复 → 修复后专项验收 → 验收关闭
    ├── P2: 3工作日内修复 → 迭代验收
    └── P3: 迭代周期内整改 → 统一验收
```

### 5.2 周报机制

每周五输出《问题修复进度周报》:
- 本周新增问题
- 本周修复问题
- 遗留问题及风险
- 下周计划
