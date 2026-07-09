# Tailor IS 乱码问题预防与处理流程文档

## 一、文档概述

本文档旨在建立 Tailor IS 系统乱码问题的标准化预防与处理流程，确保系统数据展示与传输的准确性和一致性，彻底杜绝乱码问题的频繁发生。

## 二、乱码问题分类与表现形式

### 2.1 乱码类型

| 类型 | 表现形式 | 常见原因 |
|------|----------|----------|
| **UTF-8 解码错误** | 显示 `�` 或 `\uFFFD` | 字节序列不是有效的 UTF-8 |
| **GBK/GB2312 误读为 UTF-8** | 显示 `Ã¥ÂÂ` 等乱码 | 编码声明与实际编码不一致 |
| **ISO-8859-1 误读** | 显示 `Ã©` 等乱码 | Latin-1 字符被当作 UTF-8 |
| **数据库存储乱码** | 查询结果中文显示为乱码 | 数据库连接字符集未设置 |
| **控制台日志乱码** | 终端输出中文乱码 | JVM 默认编码非 UTF-8 |

### 2.2 触发场景

1. **HTTP 请求/响应**：客户端与服务端字符集不一致
2. **数据库读写**：连接字符串未设置 UTF-8 参数
3. **文件读写**：文件编码与读取编码不匹配
4. **日志输出**：JVM 或终端编码非 UTF-8
5. **消息队列**：消息序列化/反序列化编码不一致

## 三、根本原因分析

### 3.1 编码链路分析

```
客户端 → HTTP 请求 → 网关 → 微服务 → 数据库
  │          │          │         │         │
  ▼          ▼          ▼         ▼         ▼
UTF-8     UTF-8     UTF-8    UTF-8    utf8mb4
```

任何一个环节编码不一致都会导致乱码。

### 3.2 常见根因

1. **缺少统一编码配置**：`server.servlet.encoding` 未配置
2. **数据库连接缺少字符集参数**：JDBC URL 未设置 `useUnicode=true&characterEncoding=utf-8`
3. **连接池初始化缺失**：未执行 `SET NAMES utf8mb4`
4. **前端请求头缺失**：未指定 `Content-Type: application/json;charset=UTF-8`
5. **JVM 默认编码非 UTF-8**：Linux 默认可能为 ASCII 或 ISO-8859-1

## 四、标准化预防措施

### 4.1 后端配置规范

#### 4.1.1 Spring Boot 编码配置

所有微服务 `application.yml` 必须包含以下配置：

```yaml
spring:
  servlet:
    encoding:
      charset: UTF-8
      enabled: true
      force: true
```

#### 4.1.2 数据库连接配置

JDBC URL 必须包含以下参数：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db_name?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    druid:
      connection-init-sqls:
        - SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci
```

#### 4.1.3 JVM 启动参数

Dockerfile 或启动脚本必须包含：

```bash
-Dfile.encoding=UTF-8
-Dsun.jnu.encoding=UTF-8
```

#### 4.1.4 日志编码配置

`logback-spring.xml` 所有 appender 必须配置：

```xml
<charset>UTF-8</charset>
```

### 4.2 前端配置规范

#### 4.2.1 Axios 请求配置

所有前端项目必须配置：

```typescript
axios.create({
  responseEncoding: 'utf8',
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
    'Accept': 'application/json;charset=UTF-8'
  }
})
```

#### 4.2.2 HTML 页面声明

所有 HTML 页面必须包含：

```html
<meta charset="UTF-8">
```

### 4.3 数据库配置规范

#### 4.3.1 数据库级别

```sql
ALTER DATABASE tailor_is_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 4.3.2 表级别

```sql
CREATE TABLE table_name (
  ...
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 4.3.3 MySQL 服务器配置

`my.cnf` 或 `my.ini`：

```ini
[mysqld]
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
```

## 五、数据传输校验机制

### 5.1 请求编码校验过滤器

`CharsetValidationFilter` 自动校验并强制 UTF-8 编码：

- 检测请求字符集，非 UTF-8 则强制转换
- 校验 JSON 请求体是否包含非法 UTF-8 字符
- 记录编码异常日志，便于问题追踪

### 5.2 编码工具类

`EncodingUtils` 提供以下功能：

| 方法 | 功能 |
|------|------|
| `isValidUtf8(byte[])` | 验证字节数组是否为有效 UTF-8 |
| `containsMojibake(String)` | 检测字符串是否包含乱码特征 |
| `sanitizeString(String)` | 清理无效字符，保留有效 UTF-8 |
| `detectCharset(byte[])` | 自动检测字节数组编码 |

### 5.3 监控告警

#### 5.3.1 日志监控

在关键业务流程中添加编码校验日志：

```java
if (EncodingUtils.containsMojibake(responseData)) {
    log.warn("Potential encoding issue detected in response");
}
```

#### 5.3.2 指标监控

通过 Prometheus 监控编码异常率：

```java
Counter encodingErrors = Counter.build()
    .name("encoding_errors_total")
    .help("Total encoding errors")
    .labelNames("service", "endpoint")
    .register();
```

## 六、乱码问题排查流程

### 6.1 快速定位流程图

```
用户报告乱码
     │
     ▼
┌─────────────────────────────────────────┐
│ 1. 确认乱码表现形式                       │
│    - � 替换字符？                        │
│    - Ã¥Â 等多字节乱码？                  │
│    - 控制台/日志/页面？                   │
└─────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────┐
│ 2. 定位乱码发生环节                       │
│    - 前端页面 → 检查 HTML charset        │
│    - HTTP 响应 → 检查响应头              │
│    - 服务端日志 → 检查 JVM 编码           │
│    - 数据库 → 检查连接字符集              │
└─────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────┐
│ 3. 使用编码工具验证                       │
│    - EncodingUtils.isValidUtf8()        │
│    - EncodingUtils.detectCharset()      │
└─────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────┐
│ 4. 应用标准化修复方案                      │
│    - 配置 server.servlet.encoding       │
│    - 更新 JDBC URL 参数                  │
│    - 添加 connection-init-sqls          │
│    - 配置前端 axios 编码                 │
└─────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────┐
│ 5. 验证修复结果                          │
│    - 重新测试业务流程                     │
│    - 检查日志输出                        │
│    - 确认数据库数据                       │
└─────────────────────────────────────────┘
```

### 6.2 各环节排查要点

#### 6.2.1 前端页面乱码

```bash
# 检查 HTML 文件编码
file -I index.html

# 检查 HTTP 响应头
curl -I http://localhost:3000 | grep Content-Type
```

#### 6.2.2 服务端日志乱码

```bash
# 检查 JVM 编码
java -XshowSettings:properties 2>&1 | grep file.encoding

# 检查系统默认编码
echo $LANG
```

#### 6.2.3 数据库乱码

```sql
-- 检查数据库字符集
SHOW VARIABLES LIKE 'character_set_%';

-- 检查表字符集
SHOW CREATE TABLE table_name;

-- 检查连接字符集
SELECT @@character_set_connection;
```

## 七、存量数据修复方案

### 7.1 数据转码

对于已存在的乱码数据，执行以下转码操作：

```sql
-- 备份数据
CREATE TABLE backup_table AS SELECT * FROM original_table;

-- 修复 GBK 误写为 Latin-1 的数据
UPDATE original_table 
SET column_name = CONVERT(CONVERT(column_name USING latin1) USING utf8mb4)
WHERE column_name REGEXP '[\\x80-\\xFF]';
```

### 7.2 数据重灌

对于无法转码的数据，从 UTF-8 编码的 SQL 脚本重新导入：

```bash
mysql -u root -p --default-character-set=utf8mb4 tailor_is_db < data.sql
```

## 八、CI/CD 编码检查

### 8.1 构建检查

在 Maven 构建中添加编码检查：

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <source>17</source>
    <target>17</target>
    <encoding>UTF-8</encoding>
  </configuration>
</plugin>
```

### 8.2 代码检查规则

- 所有 Java 文件必须以 UTF-8 编码保存
- 所有 SQL 文件必须以 UTF-8 编码保存
- 所有配置文件（yml, properties）必须以 UTF-8 编码保存

## 九、质量监控指标

| 指标 | 目标值 | 告警阈值 |
|------|--------|----------|
| 编码异常率 | 0% | > 0.1% |
| 数据库连接字符集错误 | 0 | > 0 |
| 请求编码强制转换次数 | 0 | > 100/天 |
| 日志乱码行数 | 0 | > 0 |

## 十、附录

### 10.1 常用命令

```bash
# 检查文件编码
file -I filename

# 转换文件编码
iconv -f GBK -t UTF-8 input.txt > output.txt

# 查看字节十六进制
xxd filename | head

# 检查 MySQL 字符集
mysql -u root -p -e "SHOW VARIABLES LIKE '%char%'"
```

### 10.2 编码对照表

| 编码 | 别名 | 特点 |
|------|------|------|
| UTF-8 | utf8 | 变长，兼容 ASCII，支持所有 Unicode |
| UTF-8MB4 | utf8mb4 | UTF-8 超集，支持 Emoji |
| GBK | gb2312 | 中文常用，双字节 |
| ISO-8859-1 | latin1 | 单字节，不支持中文 |

### 10.3 参考文档

- [Spring Boot 编码配置官方文档](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto.web.servlet.enforce-encoding)
- [MySQL 字符集官方文档](https://dev.mysql.com/doc/refman/8.0/en/charset.html)
- [UTF-8 编码规范](https://tools.ietf.org/html/rfc3629)