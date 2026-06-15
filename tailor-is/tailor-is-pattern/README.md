# Pattern 服务

Tailor IS 版型设计 AI 微服务

## 功能概述

- 版型设计管理
- 版型数据存储与查询
- 与 AI 服务协同工作
- 版型版本管理

## 技术栈

- Spring Boot 3.x
- Spring Cloud Alibaba
- MyBatis-Plus
- Nacos (服务注册/配置中心)
- MySQL

## 启动方式

```bash
# 设置环境变量
export NACOS_ADDR=localhost:8848
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=

# 启动服务
./mvnw spring-boot:run
```

## API 文档

启动后访问: http://localhost:8115/doc.html

## 配置

配置文件位于 `src/main/resources/application.yml`

### 关键配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| server.port | 服务端口 | 8115 |
| spring.cloud.nacos.discovery.server-addr | Nacos地址 | ${NACOS_ADDR} |
| spring.datasource.url | 数据库连接 | jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/tailor_is_pattern |

## 健康检查

```bash
curl http://localhost:8115/actuator/health
```

## 日志

日志配置由 `logback-spring.xml` 统一管理。

## 依赖关系

- 依赖服务: Nacos, MySQL
- 被依赖: Gateway, Core Gateway, AI 等服务
