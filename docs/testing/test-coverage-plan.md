# 单元测试覆盖率提升计划

## 1. 目标

在接下来的 3 个月内，将 Tailor IS 后端项目的单元测试覆盖率从当前的 **0%** 提升至 **60%**。

## 2. 当前状态评估

| 模块 | 代码行数 | 测试覆盖率 | 优先级 |
|------|----------|-----------|--------|
| tailor-is-common | 5,000 | 0% | 高 |
| tailor-is-user | 8,000 | 0% | 高 |
| tailor-is-core-gateway | 6,000 | 0% | 高 |
| tailor-is-order | 7,000 | 0% | 中 |
| tailor-is-payment | 6,500 | 0% | 中 |
| tailor-is-product | 5,500 | 0% | 中 |
| tailor-is-merchant | 4,000 | 0% | 低 |
| **总计** | **42,000** | **0%** | |

## 3. 分阶段目标

### 阶段一：第 1 个月（基础建设）
- 目标覆盖率：20%
- 完成核心公共模块的测试（common、user、gateway）
- 建立测试框架和规范

### 阶段二：第 2 个月（核心业务）
- 目标覆盖率：40%
- 完成订单、支付、商品模块的核心测试
- 建立集成测试框架

### 阶段三：第 3 个月（全面覆盖）
- 目标覆盖率：60%
- 完成剩余模块的测试
- 建立端到端测试

## 4. 测试规范

### 4.1 测试框架
- **JUnit 5**: 单元测试框架
- **MockMvc**: Spring MVC 控制器测试
- **Mockito**: Mock 对象框架
- **Testcontainers**: 集成测试容器

### 4.2 测试分层

| 层级 | 测试类型 | 覆盖范围 |
|------|----------|----------|
| Unit | 单元测试 | 单个方法/类 |
| Integration | 集成测试 | 模块内组件交互 |
| Component | 组件测试 | 完整服务接口 |
| E2E | 端到端测试 | 完整业务流程 |

### 4.3 测试命名规范

```java
// 类名：[ClassName]Test
public class UserServiceTest {
    
    // 方法名：test[MethodName]_[Scenario]_[ExpectedResult]
    @Test
    void testCreateUser_ValidInput_ReturnsUser() { }
    
    @Test
    void testCreateUser_InvalidInput_ThrowsException() { }
}
```

## 5. 实施计划

### 5.1 第一阶段：公共模块测试

| 任务 | 预计工时 | 负责人 | 截止日期 |
|------|----------|--------|----------|
| AesGcmCrypto 测试 | 8h | 后端开发 | 第 1 周 |
| JwtTokenUtil 测试 | 8h | 后端开发 | 第 1 周 |
| SaTokenConfig 测试 | 4h | 后端开发 | 第 1 周 |
| ResponseResult 测试 | 4h | 后端开发 | 第 2 周 |
| GlobalExceptionHandler 测试 | 8h | 后端开发 | 第 2 周 |

### 5.2 第二阶段：用户服务测试

| 任务 | 预计工时 | 负责人 | 截止日期 |
|------|----------|--------|----------|
| UserMapper 测试 | 16h | 后端开发 | 第 2 周 |
| UserService 测试 | 24h | 后端开发 | 第 3 周 |
| UserController 测试 | 16h | 后端开发 | 第 3 周 |
| AuthController 测试 | 16h | 后端开发 | 第 4 周 |

### 5.3 第三阶段：网关服务测试

| 任务 | 预计工时 | 负责人 | 截止日期 |
|------|----------|--------|----------|
| GatewayFilter 测试 | 20h | 后端开发 | 第 3 周 |
| RouteConfig 测试 | 8h | 后端开发 | 第 3 周 |
| HmacInterceptor 测试 | 12h | 后端开发 | 第 4 周 |

## 6. 质量保障

### 6.1 CI/CD 集成

```yaml
# GitHub Actions 测试步骤
- name: Run Unit Tests
  run: mvn test -B
  
- name: Run Integration Tests
  run: mvn integration-test -B
  
- name: Generate Coverage Report
  run: mvn jacoco:report -B
  
- name: Upload Coverage Report
  uses: actions/upload-artifact@v4
  with:
    name: coverage-report
    path: target/site/jacoco
```

### 6.2 覆盖率检查

在 CI 中设置覆盖率阈值：
- 新增代码覆盖率 ≥ 80%
- 整体覆盖率 ≥ 60%

### 6.3 测试评审

所有测试代码必须经过代码审查才能合并。

## 7. 工具支持

### 7.1 JaCoCo 配置

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>prepare-package</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### 7.2 SonarQube 集成

配置 SonarQube 进行代码质量和覆盖率分析。

## 8. 成功标准

| 指标 | 目标值 |
|------|--------|
| 单元测试覆盖率 | ≥ 60% |
| 集成测试覆盖率 | ≥ 40% |
| 测试通过率 | 100% |
| 新增代码覆盖率 | ≥ 80% |

## 9. 风险与应对

| 风险 | 应对措施 |
|------|----------|
| 时间不足 | 优先覆盖核心路径，非核心功能延后 |
| 测试环境不稳定 | 使用 Testcontainers 保证环境一致性 |
| 测试数据准备复杂 | 建立测试数据工厂类 |
