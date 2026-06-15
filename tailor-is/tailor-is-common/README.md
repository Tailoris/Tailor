# Common 模块

Tailor IS 公共工具与基础类库

## 功能概述

- 通用工具类 (日期、字符串、加密、ID生成等)
- 统一返回结果封装 (Result/ResultCode)
- 异常处理 (BusinessException)
- 多级缓存封装 (Caffeine + Redis)
- 分布式锁实现
- XSS 过滤与访问日志过滤器
- 统一 MyBatis-Plus 配置
- SA-Token 鉴权基础配置
- 对象存储服务封装 (Aliyun OSS)
- OpenFeign 客户端定义

## 技术栈

- Spring Boot 3.x
- MyBatis-Plus
- Redis (Caffeine 本地缓存 + Redis 分布式缓存)
- SA-Token (鉴权)
- Knife4j (API 文档)
- Hutool (工具库)
- OWASP Encoder (XSS 防护)

## 使用方式

在其他服务的 `pom.xml` 中引入依赖：

```xml
<dependency>
    <groupId>com.tailoris</groupId>
    <artifactId>tailor-is-common</artifactId>
</dependency>
```

## 配置

配置文件位于 `src/main/resources/application.yml`

### 主要组件

| 组件 | 说明 |
|------|------|
| annotation | 自定义注解 (@RateLimit, @Idempotent, @CoreCache, @LiteCache 等) |
| cache | 多级缓存实现 (MultiLevelCache) |
| client | Feign 客户端定义 (UserClient, OrderClient, ProductClient 等) |
| config | 统一配置类 (Redis, MyBatis-Plus, Swagger, CORS 等) |
| constant | 常量定义 (ErrorCode, CommonConstants, RedisKeyPrefix 等) |
| crypto | AES-GCM 加密工具 |
| dto | 通用 DTO (PageRequest, PageResponse) |
| entity | 基础实体类 (BaseEntity) |
| exception | 业务异常类 |
| filter | 过滤器 (XssFilter, TraceIdFilter, AccessLogFilter) |
| lock | 分布式锁 (RedisDistributedLock) |
| oss | 对象存储服务封装 |
| util | 各种工具类 |

## 依赖关系

- 依赖: Spring Boot, MyBatis-Plus, Redis, Hutool 等 (部分为 provided/optional)
- 被依赖: 几乎所有业务微服务
