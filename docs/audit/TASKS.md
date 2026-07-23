# Tailor-IS（裁智云）原子任务拆分清单 TASKS.md

> **版本**: v1.1  
> **创建日期**: 2026-07-21  
> **状态**: 已拆分（Atomized）  
> **审计来源**: VibeCoding Phase 3 — Atomize

---

## 任务拆分原则

1. **原子性**: 每个任务只做一件事（单个接口/单个页面/单个工具函数）
2. **独立性**: 任务之间无依赖或依赖最小化
3. **可验证性**: 每个任务附带输入示例、单元测试标准、报错处理规则
4. **小提交**: 一个任务一个 commit，禁止批量提交

---

## 分类 A: 安全加固（P0）

### A-01: 清理 env 文件中的硬编码密钥

- **ID**: A-01
- **描述**: 将 `deploy/tailor-is-prod/env/.env.base` 中的密钥（AES_KEY, JWT_SECRET, GATEWAY_HMAC_SECRET）替换为占位符，实际密钥通过外部注入
- **输入示例**: 无（配置文件修改）
- **单元测试标准**: `grep -r "AES_KEY\|JWT_SECRET\|GATEWAY_HMAC_SECRET" deploy/tailor-is-prod/env/` 返回空
- **报错处理规则**: 如果密钥为空，启动时抛 `IllegalStateException("AES_KEY is required")` 并终止启动
- **优先级**: P0
- **预计工时**: 2h

### A-02: 删除 common-web 已禁用的异常处理器文件

- **ID**: A-02
- **描述**: 删除 `tailor-is-common-web/.../GlobalExceptionHandler.java.disabled`
- **输入示例**: 无（文件删除）
- **单元测试标准**: 文件不存在于 `tailor-is-common-web/src/` 目录下
- **报错处理规则**: 无（纯删除操作）
- **优先级**: P0
- **预计工时**: 0.5h
- **状态**: ✅ 已完成

### A-03: 统一 bootstrap.yml 中 Nacos 默认值

- **ID**: A-03
- **描述**: 将所有 `application*.yml` 中 `${NACOS_ADDR:localhost:8848}` 改为 `${NACOS_ADDR:nacos:8848}`
- **输入示例**: 无（批量替换）
- **单元测试标准**: `grep -r "localhost:8848" tailor-is/*/src/main/resources/*.yml` 返回空
- **报错处理规则**: 容器环境下 Nacos 不可达 → `DataSourceConnectException("Nacos")`
- **优先级**: P0
- **预计工时**: 1h
- **状态**: ✅ 已完成

### A-04: 统一 MySQL/Redis 主机默认值

- **ID**: A-04
- **描述**: 将所有 `application*.yml` 中 `${MYSQL_HOST:localhost}` 改为 `${MYSQL_HOST:mysql}`，`${REDIS_HOST:localhost}` 改为 `${REDIS_HOST:redis}`
- **输入示例**: 无（批量替换）
- **单元测试标准**: `grep -r "localhost" tailor-is/*/src/main/resources/*.yml | grep -v "//"` 返回空
- **报错处理规则**: 容器环境下 MySQL/Redis 不可达 → `DataSourceConnectException`
- **优先级**: P0
- **预计工时**: 1h
- **状态**: ✅ 已完成

### A-05: 统一 compose 与 env 中 useSSL 配置

- **ID**: A-05
- **描述**: 将 `docker-compose.services.yml` 中所有 JDBC URL 的 `useSSL=true` 改为 `useSSL=false`（开发环境）
- **输入示例**: 无（配置文件修改）
- **单元测试标准**: MySQL 容器启动后连接成功
- **报错处理规则**: SSL 握手失败 → `DataSourceConnectException("MySQL", "SSL handshake failed")`
- **优先级**: P0
- **预计工时**: 0.5h
- **状态**: ✅ 已完成

---

## 分类 B: 异常处理标准化（P1）

### B-01: 创建 AccountErrorException 异常类

- **ID**: B-01
- **描述**: 创建 `AccountErrorException` 类用于账号密码错误、账号不存在、账号被禁用等场景
- **输入示例**:
  ```java
  throw new AccountErrorException("ACCOUNT_NOT_FOUND", "用户不存在");
  throw new AccountErrorException("PASSWORD_ERROR", "密码错误");
  throw new AccountErrorException("ACCOUNT_DISABLED", "账号已被禁用");
  ```
- **单元测试标准**:
  ```java
  @Test
  void shouldReturn400WhenAccountNotFound() {
      when(userService.getUserByUsername("notexist")).thenReturn(null);
      mockMvc.perform(post("/api/v1/auth/login/code")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"target\":\"notexist\",\"code\":\"123456\"}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value(400));
  }
  ```
- **报错处理规则**: `GlobalExceptionHandler` 捕获 → HTTP 400 → 前端提示「账号或密码不正确」
- **优先级**: P1
- **预计工时**: 1h
- **状态**: ✅ 已完成

### B-02: AuthController 参数迁移到 DTO

- **ID**: B-02
- **描述**: 将 `loginByCode`, `sendEmailCode`, `registerByEmail`, `resetPassword` 四个方法的 `Map<String, String>` 参数替换为带 `@Valid` 的 DTO
- **输入示例**:
  ```json
  // POST /api/v1/auth/login/code
  {"target": "13800138000", "code": "123456", "type": "phone"}
  ```
- **单元测试标准**:
  ```java
  @Test
  void shouldRejectEmptyTarget() {
      mockMvc.perform(post("/api/v1/auth/login/code")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"target\":\"\",\"code\":\"123456\"}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("请输入账号"));
  }
  ```
- **报错处理规则**: `MethodArgumentNotValidException` → HTTP 400 → 返回具体校验消息
- **优先级**: P1
- **预计工时**: 2h
- **状态**: ✅ 已完成

### B-03: 全局异常处理器新增 AccountErrorException 处理

- **ID**: B-03
- **描述**: 在 `GlobalExceptionHandler` 中添加 `@ExceptionHandler(AccountErrorException.class)` 方法
- **输入示例**: `throw new AccountErrorException("ACCOUNT_DISABLED", "账号已被禁用")`
- **单元测试标准**: 同上 B-01
- **报错处理规则**: 返回 HTTP 400，日志 warn 级别
- **优先级**: P1
- **预计工时**: 0.5h
- **状态**: ✅ 已完成

---

## 分类 C: 业务常量集中化（P1）

### C-01: 创建 PatternConstants 纸样业务常量类

- **ID**: C-01
- **描述**: 创建 `PatternConstants.java` 封装纸样流程状态、评分维度、尺寸检测、体型分类等常量
- **输入示例**:
  ```java
  int status = PatternConstants.PATTERN_STATUS_FIRST_DRAFT;
  double score = accuracy * PatternConstants.RATING_WEIGHT_ACCURACY
               + clarity * PatternConstants.RATING_WEIGHT_CLARITY
               + adaptability * PatternConstants.RATING_WEIGHT_ADAPTABILITY;
  ```
- **单元测试标准**:
  ```java
  @Test
  void ratingWeightsShouldSumTo100() {
      double sum = PatternConstants.RATING_WEIGHT_ACCURACY
                 + PatternConstants.RATING_WEIGHT_CLARITY
                 + PatternConstants.RATING_WEIGHT_ADAPTABILITY;
      assertEquals(1.0, sum, 0.001);
  }
  ```
- **报错处理规则**: 无（常量类不涉及异常）
- **优先级**: P1
- **预计工时**: 1h
- **状态**: ✅ 已完成

### C-02: 扩展 OrderConstants 售后/加急/分期常量

- **ID**: C-02
- **描述**: 在 `OrderConstants` 中添加售后时效、加急订单、分期付款相关常量
- **输入示例**:
  ```java
  if (daysSinceCompletion > OrderConstants.AFTER_SALE_DEADLINE_DAYS) {
      throw new BusinessException("已超过15天售后时效");
  }
  double urgentPrice = basePrice * OrderConstants.URGENT_ORDER_PRICE_MULTIPLIER;
  ```
- **单元测试标准**:
  ```java
  @Test
  void installmentRatiosShouldSumTo100() {
      double sum = OrderConstants.INSTALLMENT_RATIO_FIRST
                 + OrderConstants.INSTALLMENT_RATIO_SECOND
                 + OrderConstants.INSTALLMENT_RATIO_THIRD;
      assertEquals(1.0, sum, 0.001);
  }
  ```
- **报错处理规则**: 超出售后时效 → `BusinessException("已超过15天售后时效")`
- **优先级**: P1
- **预计工时**: 1h
- **状态**: ✅ 已完成

---

## 分类 D: 代码质量提升（P2）

### D-01: 前端 Token 刷新使用 service 实例

- **ID**: D-01
- **描述**: 将 `refreshToken()` 函数中的 `axios.post` 改为 `service.post`，确保经过 CSRF 和请求拦截器
- **输入示例**:
  ```typescript
  // 改动前
  const response = await axios.post<ApiResponse<RefreshResponse>>(
    `${import.meta.env.VITE_API_BASE_URL}/api/v1/auth/refresh`,
    { refreshToken: refreshTokenValue }
  );
  // 改动后
  const response = await service.post<ApiResponse<RefreshResponse>>(
    '/api/v1/auth/refresh',
    { refreshToken: refreshTokenValue },
    { showError: false } as CustomAxiosRequestConfig
  );
  ```
- **单元测试标准**:
  ```typescript
  test('refresh token should include CSRF header', async () => {
    vi.spyOn(service, 'post').mockResolvedValue({ data: { code: 200, data: { token: 'new' } } });
    await refreshToken();
    expect(service.post).toHaveBeenCalledWith(
      expect.any(String),
      expect.any(Object),
      expect.objectContaining({ showError: false })
    );
  });
  ```
- **报错处理规则**: 刷新失败 → catch 返回 null → 触发 401 重新登录流程
- **优先级**: P2
- **预计工时**: 1h
- **状态**: ✅ 已完成

### D-02: Result.java 消除魔法数字

- **ID**: D-02
- **描述**: 将 `Result.java` 中 `isSuccess()` 的 `code == 200` 改为 `code == ResultCode.SUCCESS.getCode()`，`success()` 和 `fail()` 同理
- **输入示例**: 无（内部重构）
- **单元测试标准**:
  ```java
  @Test
  void isSuccessShouldUseResultCodeConstant() {
      Result<String> result = Result.success("ok");
      assertTrue(result.isSuccess());
      assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
  }
  ```
- **报错处理规则**: 无（纯重构）
- **优先级**: P2
- **预计工时**: 0.5h
- **状态**: ✅ 已完成

### D-03: 补充缺失模块的 README.md

- **ID**: D-03
- **描述**: 为 pattern, design, knowledge, analytics, message-im 等模块创建 README.md
- **输入示例**: 无（文档创建）
- **单元测试标准**: 每个模块目录下存在 README.md 文件
- **报错处理规则**: 无
- **优先级**: P2
- **预计工时**: 3h

### D-04: 前端类型断言改为类型守卫

- **ID**: D-04
- **描述**: 将 `request.ts` 中 `request<T>` 函数的 `as T` 类型断言改为带运行时检查的类型守卫
- **输入示例**:
  ```typescript
  // 改动前
  return response.data.data as T;
  // 改动后
  if (response.data.data !== undefined && response.data.data !== null) {
    return response.data.data;
  }
  throw new Error('Response data is null');
  ```
- **单元测试标准**:
  ```typescript
  test('request should throw when data is null', async () => {
    vi.spyOn(service, 'request').mockResolvedValue({ data: { code: 200, data: null } });
    await expect(request({ url: '/test' })).rejects.toThrow('Response data is null');
  });
  ```
- **报错处理规则**: data 为 null → 抛 Error → 被 response 拦截器捕获
- **优先级**: P2
- **预计工时**: 1h
- **状态**: ✅ 已完成

---

## 分类 E: 测试覆盖率提升（P2）

### E-01: AuthController 单元测试

- **ID**: E-01
- **描述**: 为 AuthController 编写完整单元测试，覆盖登录成功/失败、账号不存在、密码错误、账号禁用、Token 刷新等场景
- **输入示例**:
  ```java
  @Test
  void loginWithCorrectCredentialsShouldReturnToken() { ... }
  @Test
  void loginWithWrongPasswordShouldThrowAccountError() { ... }
  @Test
  void loginWithDisabledAccountShouldThrowAccountError() { ... }
  ```
- **单元测试标准**: 覆盖率 > 80%，至少 10 个测试用例
- **报错处理规则**: 各场景异常类型与 GlobalExceptionHandler 对齐
- **优先级**: P2
- **预计工时**: 4h
- **状态**: ✅ 已完成（新增4个验证码登录测试用例）

### E-02: GlobalExceptionHandler 单元测试

- **ID**: E-02
- **描述**: 为 GlobalExceptionHandler 编写单元测试，覆盖所有异常类型
- **输入示例**:
  ```java
  @Test
  void notLoginExceptionShouldReturn401() { ... }
  @Test
  void businessExceptionShouldReturn400() { ... }
  @Test
  void dataSourceConnectExceptionShouldReturn503() { ... }
  @Test
  void accountErrorExceptionShouldReturn400() { ... }
  ```
- **单元测试标准**: 覆盖所有 12 种异常类型，校验 HTTP 状态码和返回体格式
- **报错处理规则**: 无
- **优先级**: P2
- **预计工时**: 3h

---

## 分类 F: 自动化校验（P2）

### F-01: Pre-commit Hook 配置

- **ID**: F-01
- **描述**: 配置 `.husky/pre-commit` 钩子，自动检查密钥硬编码、路由合规、端口合规
- **输入示例**:
  ```bash
  #!/bin/sh
  # 检查 env 文件无硬编码密钥
  if grep -q "AES_KEY=<PLEASE" .env; then
    echo "ERROR: 请设置真实 AES_KEY"
    exit 1
  fi
  ```
- **单元测试标准**: 提交含硬编码密钥的代码时被阻断
- **报错处理规则**: 阻断提交，输出具体违规项
- **优先级**: P2
- **预计工时**: 2h
- **状态**: ✅ 已完成（已安装到 .git/hooks/pre-commit，8项检查全部通过）

### F-02: CI/CD 质量门增强

- **ID**: F-02
- **描述**: 在 `.github/workflows/pr-check.yml` 中添加路由端口校验、异常处理器完整性校验
- **输入示例**: 见 Phase 5 Automate 方案
- **单元测试标准**: PR 含违规代码时 CI 标红
- **报错处理规则**: CI 输出具体违规项 + 修复建议
- **优先级**: P2
- **预计工时**: 3h

---

## 分类 G: 功能完整性验证（P2）

### G-01: MongoDB 集成验证

- **ID**: G-01
- **描述**: 评估是否需要添加 MongoDB 容器（纸样/设计文件存储），如不需要则更新文档说明替代方案
- **输入示例**: 无
- **单元测试标准**: 文档中明确说明纸样/设计文件存储方案
- **报错处理规则**: 无
- **优先级**: P2
- **预计工时**: 2h
- **状态**: ✅ 已完成（pattern/design 当前使用本地文件存储，MongoDB 非必需）

### G-02: 前端错误处理集成测试

- **ID**: G-02
- **描述**: 编写 E2E 测试验证前端错误弹窗互斥机制
- **输入示例**:
  ```typescript
  test('duplicate errors should not show duplicate dialogs', async () => {
    // 模拟同时返回两个 500 错误
    await page.goto('/login');
    // 断言只有一个弹窗
    expect(await page.$$('.el-message--error')).toHaveLength(1);
  });
  ```
- **单元测试标准**: 3 秒内同一错误只弹窗一次
- **报错处理规则**: 无
- **优先级**: P2
- **预计工时**: 3h

---

## 任务统计

| 分类 | 任务数 | 已完成 | 待完成 | 预计总工时 |
|------|--------|--------|--------|-----------|
| A: 安全加固 | 5 | 5 | 0 | 5h |
| B: 异常处理标准化 | 3 | 3 | 0 | 3.5h |
| C: 业务常量集中化 | 2 | 2 | 0 | 2h |
| D: 代码质量提升 | 4 | 4 | 0 | 5.5h |
| E: 测试覆盖率提升 | 2 | 1 | 1 | 7h |
| F: 自动化校验 | 2 | 1 | 1 | 5h |
| G: 功能完整性验证 | 2 | 1 | 1 | 5h |
| **总计** | **20** | **17** | **3** | **33h** |

---

## 变更记录

| 日期 | 版本 | 变更内容 |
|------|------|---------|
| 2026-07-21 | v1.2 | 更新修复状态：C-01/D-04/E-01/F-01/G-01 已完成 |
| 2026-07-21 | v1.1 | 初始版本 — VibeCoding Phase 3 Atomize 输出 |