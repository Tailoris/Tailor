# Admin 服务

Tailor IS 平台管理微服务

## 功能概述

- 平台管理后台服务
- 用户管理查询
- 商家审核管理
- 商品审核管理
- 订单查询与仲裁
- 佣金配置管理
- 举报处理
- 平台数据看板

## 技术栈

- Spring Boot 3.x
- Spring Cloud Alibaba
- Nacos (服务注册/配置中心)
- MySQL (通过 Feign 调用各业务服务)
- Redis (缓存)

## 启动方式

```bash
# 设置环境变量
export NACOS_ADDR=localhost:8848
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=

# 启动服务
./mvnw spring-boot:run
```

## API 文档

启动后访问: http://localhost:8100/doc.html

## 配置

配置文件位于 `src/main/resources/application.yml`

### 关键配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| server.port | 服务端口 | 8100 |
| spring.cloud.nacos.discovery.server-addr | Nacos地址 | ${NACOS_ADDR} |
| spring.datasource.url | 数据库连接 | jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/tailor_is_admin |

## 健康检查

```bash
curl http://localhost:8100/actuator/health
```

## 日志

日志配置由 `logback-spring.xml` 统一管理。

## 依赖关系

- 依赖服务: Nacos, Redis, MySQL, 各业务微服务 (Feign 调用)
- 被依赖: Gateway, Core Gateway 等服务
