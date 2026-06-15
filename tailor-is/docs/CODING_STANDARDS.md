# Tailor IS 平台编码规范

## 目录
1. [命名规范](#1-命名规范)
2. [代码格式](#2-代码格式)
3. [注释要求](#3-注释要求)
4. [文件组织](#4-文件组织)
5. [异常处理](#5-异常处理)
6. [API 设计](#6-api-设计)
7. [数据库规范](#7-数据库规范)
8. [安全规范](#8-安全规范)

---

## 1. 命名规范

### 1.1 类命名
- **类名**使用 `PascalCase`（大驼峰命名法），每个单词首字母大写
- 类名应该是名词或名词短语
- 避免使用缩写，除非是广泛认可的缩写（如 DTO、VO、DAO）

```java
// 正确
public class UserService { }
public class OrderInfo { }
public class ProductDTO { }

// 错误
public class userService { }
public class order_info { }
```

### 1.2 方法和变量命名
- **方法名**和**变量名**使用 `camelCase`（小驼峰命名法），第一个单词首字母小写
- 方法名应该是动词或动词短语
- 布尔类型的变量使用 `is`、`has`、`can`、`should` 等前缀

```java
// 正确
public void getUserInfo() { }
public boolean isActive() { }
public String userName;
public boolean hasPermission;

// 错误
public void GetUser() { }
public void user_info() { }
public String user_name;
```

### 1.3 常量命名
- **常量**使用 `UPPER_SNAKE_CASE`（全大写，下划线分隔）
- 使用 `public static final` 修饰

```java
// 正确
public static final int MAX_RETRY_COUNT = 3;
public static final String DEFAULT_CHARSET = "UTF-8";
public static final long TOKEN_EXPIRE_TIME = 7200L;

// 错误
public static final int maxRetryCount = 3;
public static final String defaultCharset = "UTF-8";
```

### 1.4 包命名
- 包名全部小写，使用点号分隔
- 包名应该是名词或名词短语
- 遵循反向域名约定

```java
// 正确
package com.tailoris.user.service;
package com.tailoris.common.util;

// 错误
package com.tailoris.UserService;
package com.Tailoris.user;
```

---

## 2. 代码格式

### 2.1 缩进
- 使用 **4 个空格** 进行缩进，不使用 Tab
- 所有 IDE 配置统一使用空格缩进

### 2.2 行长度
- 每行最大长度不超过 **120 个字符**
- 超过限制时应适当换行，换行后增加 8 个空格缩进

```java
// 正确
public Result<UserInfo> getUserInfo(@RequestParam String userId,
        @RequestParam(required = false) String includeDetails) {
    return Result.success(userService.getUserInfo(userId, includeDetails));
}

// 错误（超过 120 字符）
public Result<UserInfo> getUserInfo(@RequestParam String userId, @RequestParam(required = false) String includeDetails, @RequestParam(defaultValue = "true") boolean active) {
```

### 2.3 空行
- 方法之间空一行
- 逻辑块之间空一行
- 类的第一个成员前不要空行

### 2.4 大括号
- 左大括号 `{` 不换行，与语句在同一行
- 右大括号 `}` 换行

```java
// 正确
public void doSomething() {
    if (condition) {
        // do something
    }
}

// 错误
public void doSomething()
{
    if (condition)
    {
    }
}
```

---

## 3. 注释要求

### 3.1 Javadoc 注释
- **所有公共方法**必须有 Javadoc 注释
- 包括 `@param`、`@return`、`@throws` 标签

```java
/**
 * 根据用户ID获取用户信息
 *
 * @param userId 用户ID，不能为空
 * @return 用户信息，如果用户不存在则抛出异常
 * @throws BusinessException 当用户不存在时抛出
 */
public UserInfo getUserById(String userId) {
    // implementation
}
```

### 3.2 行内注释
- **复杂业务逻辑**必须有行内注释说明
- 注释应该解释"为什么"而不是"是什么"

```java
// 正确
// 使用逻辑删除而非物理删除，以便审计和数据恢复
entity.setDeleted(true);
entity.setUpdateTime(LocalDateTime.now());

// 错误
// 设置deleted为true
entity.setDeleted(true);
```

### 3.3 TODO 注释
- 使用 `TODO` 标记待完成事项
- 使用 `FIXME` 标记需要修复的问题

```java
// TODO: 实现缓存刷新逻辑
// FIXME: 并发情况下可能出现重复扣减
```

---

## 4. 文件组织

### 4.1 模块结构
每个业务模块遵循以下目录结构：

```
tailor-is-{module}/
├── src/main/java/com/tailoris/{module}/
│   ├── controller/     # 控制器层，处理HTTP请求
│   ├── service/        # 服务层，处理业务逻辑
│   │   └── impl/       # 服务实现类
│   ├── mapper/         # 数据访问层，MyBatis Mapper接口
│   ├── entity/         # 实体类，对应数据库表
│   ├── dto/            # 数据传输对象
│   ├── vo/             # 视图对象
│   ├── config/         # 模块配置类
│   ├── constant/       # 常量定义
│   ├── util/           # 工具类
│   └── {Module}Application.java  # 启动类
└── src/main/resources/
    ├── application.yml # 配置文件
    └── mapper/         # MyBatis XML映射文件
```

### 4.2 分层职责

| 层级 | 职责 | 禁止事项 |
|------|------|----------|
| Controller | 接收请求、参数校验、返回响应 | 不要写业务逻辑 |
| Service | 业务逻辑处理、事务管理 | 不要直接操作数据库 |
| Mapper | 数据库CRUD操作 | 不要写业务逻辑 |
| Entity | 数据库表映射 | 不要包含业务逻辑 |
| DTO | 接口入参数据传输 | 不要直接用于数据库操作 |
| VO | 接口出参数据封装 | 不要包含敏感信息 |

---

## 5. 异常处理

### 5.1 使用 BusinessException
- 所有业务异常统一使用 `BusinessException`
- 不要直接抛出底层异常给调用方

```java
// 正确
if (user == null) {
    throw new BusinessException(ResultCode.USER_NOT_FOUND);
}

// 错误
if (user == null) {
    throw new RuntimeException("User not found");
}
```

### 5.2 禁止吞掉异常
- **永远不要**捕获异常后不做任何处理
- 捕获异常后必须记录日志或重新抛出

```java
// 正确
try {
    processOrder(order);
} catch (Exception e) {
    log.error("订单处理失败, orderId: {}", order.getId(), e);
    throw new BusinessException(ResultCode.ORDER_PROCESS_FAILED);
}

// 错误（吞掉异常）
try {
    processOrder(order);
} catch (Exception e) {
    // 什么都不做
}
```

### 5.3 异常处理层次
- 底层异常 → 转换为业务异常 → 全局异常处理器统一处理
- 使用 `@ControllerAdvice` 进行全局异常处理

---

## 6. API 设计

### 6.1 RESTful 规范
- 使用名词复数表示资源：`/api/v1/users`
- 使用 HTTP 方法表示操作：
  - `GET` - 查询
  - `POST` - 创建
  - `PUT` - 全量更新
  - `PATCH` - 部分更新
  - `DELETE` - 删除

### 6.2 统一响应格式
所有接口统一使用 `Result<T>` 包装：

```java
public class Result<T> {
    private int code;
    private String message;
    private T data;
}

// 成功响应
return Result.success(data);
return Result.success("操作成功", data);

// 失败响应
return Result.error(ResultCode.USER_NOT_FOUND);
return Result.error(400, "参数错误");
```

### 6.3 分页规范
- 入参使用 `PageRequest`：包含 `pageNum`、`pageSize`
- 出参使用 `PageResponse<T>`：包含 `total`、`list`、`pageNum`、`pageSize`

---

## 7. 数据库规范

### 7.1 逻辑删除
- 使用 `deleted` 字段标记删除状态，值为 0/1
- **禁止**物理删除数据，便于审计和数据恢复

```sql
ALTER TABLE `user` ADD COLUMN `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标识: 0-未删除, 1-已删除';
```

### 7.2 审计字段
每个表必须包含以下审计字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT | 主键，使用雪花算法生成 |
| `created_by` | VARCHAR | 创建人ID |
| `created_time` | DATETIME | 创建时间 |
| `updated_by` | VARCHAR | 更新人ID |
| `updated_time` | DATETIME | 更新时间 |
| `deleted` | TINYINT | 逻辑删除标识 |

### 7.3 禁止外键约束
- 不在数据库层面设置外键约束
- 在应用层通过代码维护数据一致性
- 便于分库分表和水平扩展

---

## 8. 安全规范

### 8.1 禁止硬编码密钥
- **绝对不要**在代码中硬编码密码、密钥、Token 等敏感信息
- 敏感配置通过环境变量或配置中心注入

```java
// 正确
@Value("${jwt.secret}")
private String jwtSecret;

// 错误
private String jwtSecret = "my-secret-key-123";
```

### 8.2 参数化查询
- 使用 MyBatis 的 `#{}` 而不是 `${}` 进行参数绑定
- 防止 SQL 注入攻击

```xml
<!-- 正确 -->
<select id="getUserById" resultType="User">
    SELECT * FROM user WHERE id = #{id}
</select>

<!-- 错误（SQL注入风险） -->
<select id="getUserById" resultType="User">
    SELECT * FROM user WHERE id = ${id}
</select>
```

### 8.3 输入验证
- 所有外部输入必须进行校验
- 使用 `@Valid` 和 `@Validated` 注解进行参数校验
- 敏感接口增加频率限制和签名校验

```java
public class UserRegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度必须在2-20之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z\\d]{6,20}$", message = "密码必须包含字母和数字，长度6-20位")
    private String password;
}
```

### 8.4 数据脱敏
- 敏感数据（手机号、身份证号等）在日志和响应中必须脱敏
- 使用 `DesensitizeUtils` 工具类进行脱敏处理

---

## 附录

### 代码审查清单
- [ ] 命名符合规范
- [ ] 代码格式正确
- [ ] Javadoc 注释完整
- [ ] 异常处理完善
- [ ] 没有硬编码密钥
- [ ] SQL 使用参数化查询
- [ ] 输入参数已校验
- [ ] 敏感数据已脱敏
- [ ] 单元测试覆盖

### 工具链
| 工具 | 用途 | 配置 |
|------|------|------|
| Checkstyle | 代码格式检查 | [checkstyle.xml](../checkstyle.xml) |
| SonarQube | 代码质量扫描 | [sonar-project.properties](../sonar-project.properties) |
| JaCoCo | 测试覆盖率 | pom.xml 中配置 |
| Maven | 构建工具 | 统一使用 Maven |
