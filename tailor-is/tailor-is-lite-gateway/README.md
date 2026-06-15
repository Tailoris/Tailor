# Lite Gateway 服务

Tailor IS 轻量级 API 网关微服务

## 功能概述

- 轻量级服务路由 (community, academy, supply, message, message-im, analytics)
- 统一鉴权与全局限流 (50 req/s per IP)
- SA-Token 认证集成
- Prometheus 监控指标暴露
- CORS 跨域配置管理

## 技术栈

- Spring Boot 3.x
- Spring Cloud Gateway (WebFlux)
- Spring Cloud Alibaba
- Nacos (服务注册/配置中心)
- SA-Token (鉴权)
- Redis (限流计数)

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

网关本身不提供 Swagger 文档，各业务服务文档通过网关聚合访问。

## 配置

配置文件位于 `src/main/resources/application.yml`

### 关键配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| server.port | 服务端口 | 8081 |
| spring.cloud.nacos.discovery.server-addr | Nacos地址 | ${NACOS_ADDR} |
| tailoris.gateway.ratelimit.default-permits-per-second | 全局限流 | 50 req/s |

## 健康检查

```bash
curl http://localhost:8081/actuator/health
```

## 日志

日志配置由 `logback-spring.xml` 统一管理。

## 依赖关系

- 依赖服务: Nacos, Redis
- 被依赖: 所有前端/客户端轻量级业务请求入口
- 路由转发: community, academy, supply, message, message-im, analytics
