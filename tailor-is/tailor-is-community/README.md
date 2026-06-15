# Community 服务

Tailor IS 社区微服务

## 功能概述

- 社区帖子发布与管理
- 评论与互动
- 点赞与收藏
- 话题管理
- 社区举报与内容审核
- Serverless 部署支持

## 技术栈

- Spring Boot 3.x
- Spring Cloud Alibaba
- MyBatis-Plus
- Nacos (服务注册/配置中心)
- MySQL + Druid 连接池
- Redis (缓存)
- Serverless 支持

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

# 启动服务
./mvnw spring-boot:run

# Serverless 模式启动
./mvnw spring-boot:run -Dspring-boot.run.profiles=serverless
```

## API 文档

启动后访问: http://localhost:8108/doc.html

## 配置

配置文件位于 `src/main/resources/application.yml`

### 关键配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| server.port | 服务端口 | 8108 |
| spring.cloud.nacos.discovery.server-addr | Nacos地址 | ${NACOS_ADDR} |
| spring.datasource.url | 数据库连接 | jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/tailor_is_community |

## 健康检查

```bash
curl http://localhost:8108/actuator/health
```

## 日志

日志配置由 `logback-spring.xml` 统一管理。

## 依赖关系

- 依赖服务: Nacos, Redis, MySQL
- 被依赖: Gateway, Lite Gateway 等服务
