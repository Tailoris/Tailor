# Marketing 服务

Tailor IS 营销管理微服务

## 功能概述

- 优惠券模板与用户优惠券管理
- 会员等级与积分体系
- 秒杀活动管理
- 团购活动管理

## 技术栈

- Spring Boot 3.x
- Spring Cloud Alibaba
- MyBatis-Plus
- Nacos (服务注册/配置中心)
- Sentinel (限流熔断)
- MySQL + Druid 连接池
- Redis (缓存)
- RabbitMQ (消息队列)

## 启动方式

```bash
# 设置环境变量
export NACOS_ADDR=localhost:8848
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=
export RABBITMQ_HOST=localhost
export RABBITMQ_PORT=5672

# 启动服务
./mvnw spring-boot:run
```

## API 文档

启动后访问: http://localhost:8105/doc.html

## 配置

配置文件位于 `src/main/resources/application.yml`

### 关键配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| server.port | 服务端口 | 8105 |
| spring.cloud.nacos.discovery.server-addr | Nacos地址 | ${NACOS_ADDR} |
| spring.datasource.url | 数据库连接 | jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/tailor_is_marketing |

## 健康检查

```bash
curl http://localhost:8105/actuator/health
```

## 日志

日志配置由 `logback-spring.xml` 统一管理。

## 依赖关系

- 依赖服务: Nacos, Redis, MySQL, RabbitMQ
- 被依赖: Gateway, Order, Product 等服务
