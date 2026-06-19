# Tailor IS 部署计划与方案 (更新版)

> 版本: v2.0 | 日期: 2026-06-19 | 基于: 专项核查结果更新

---

## 一、部署环境要求

### 1.1 硬件要求

| 环境 | CPU | 内存 | 磁盘 | 用途 |
|------|-----|------|------|------|
| 生产 | 8核+ | 16GB+ | 200GB+ SSD | 1Panel 生产环境 |
| 预发布 | 4核 | 8GB | 100GB SSD | 验收测试 |
| 开发 | 2核 | 4GB | 50GB | 本地开发 |

### 1.2 软件要求

| 组件 | 版本 | 用途 |
|------|------|------|
| 1Panel | 最新稳定版 | 服务器管理面板 |
| Docker | 24.0+ | 容器运行时 |
| Docker Compose | v2.20+ | 容器编排 |
| Java | 17 (Temurin) | 后端运行时 |
| Node.js | 20 LTS | 前端构建 |
| MySQL | 8.0 | 数据库 |
| Redis | 7.0 | 缓存 |
| RabbitMQ | 3.12 | 消息队列 |
| Nacos | 2.3 | 服务注册/配置中心 |

---

## 二、端口与网络规划

> 详见 [Tailor-IS-系统路由与端口规范](../Tailor-IS-系统路由与端口规范.md)

### 2.1 核心端口

| 端口 | 服务 | 说明 |
|------|------|------|
| 80/443 | Nginx | HTTP/HTTPS 入口 |
| 8080 | core-gateway | 核心网关 |
| 8081 | lite-gateway | 轻量网关 |
| 8100-8115 | 业务服务 | 微服务端口区间 |
| 3306 | MySQL | 数据库 |
| 6379 | Redis | 缓存 |
| 5672 | RabbitMQ | 消息队列 |
| 8848 | Nacos | 注册中心 |
| 9090 | Prometheus | 监控 |
| 3001 | Grafana | 监控面板 |

### 2.2 网络隔离

- **tailor-is-network**: 核心业务服务网络
- **monitor-network**: 监控服务网络
- 基础设施通过 `host.docker.internal` 访问宿主机服务

---

## 三、部署步骤

### 3.1 前置准备

```bash
# 1. 克隆代码
git clone git@github.com:Tailoris/Tailor.git
cd Tailor

# 2. 配置环境变量
cp deploy/.env.example deploy/.env
# 编辑 deploy/.env 填入真实凭据

# 3. 确认基础设施已就绪 (1Panel 管理)
#    - MySQL 8.0 运行中
#    - Redis 7.0 运行中
#    - RabbitMQ 3.12 运行中
#    - Nacos 2.3 运行中
```

### 3.2 数据库初始化

```bash
# 执行初始化脚本 (按顺序)
mysql -h <host> -u root -p < deploy/sql/000_init_database.sql
mysql -h <host> -u root -p tailor_is < tailor-is/sql/01_user_system.sql
mysql -h <host> -u root -p tailor_is < tailor-is/sql/02_product_system.sql
# ... 其他SQL脚本
```

### 3.3 后端服务部署

```bash
cd deploy

# 启动核心微服务
docker compose -f docker-compose.services.yml up -d --build

# 验证服务状态
docker compose -f docker-compose.services.yml ps
curl http://localhost:8080/actuator/health
```

### 3.4 前端部署

```bash
# 构建前端
cd tailor-is-frontend/pc-mall
npm ci && npm run build

# 部署到 Nginx
docker compose -f docker-compose.frontend.yml up -d
```

### 3.5 监控部署

```bash
cd deploy
docker compose -f docker-compose-monitoring.yml up -d
```

---

## 四、配置参数清单

### 4.1 必须配置的环境变量

| 变量名 | 说明 | 示例 |
|--------|------|------|
| MYSQL_HOST | MySQL 主机 | host.docker.internal |
| MYSQL_PORT | MySQL 端口 | 3306 |
| MYSQL_USER | MySQL 用户 | tailor_is |
| MYSQL_PASSWORD | MySQL 密码 | (强密码) |
| REDIS_HOST | Redis 主机 | host.docker.internal |
| REDIS_PORT | Redis 端口 | 6379 |
| REDIS_PASSWORD | Redis 密码 | (强密码) |
| RABBITMQ_HOST | RabbitMQ 主机 | host.docker.internal |
| RABBITMQ_USER | RabbitMQ 用户 | tailor_is |
| RABBITMQ_PASSWORD | RabbitMQ 密码 | (强密码) |
| NACOS_ADDR | Nacos 地址 | host.docker.internal:8848 |
| SENTINEL_DASHBOARD | Sentinel 面板 | host.docker.internal:8858 |

### 4.2 安全配置要求

1. **Actuator 端点**: 仅暴露 `health,info,prometheus,metrics`
2. **Health 详情**: `show-details: when-authorized`
3. **CORS**: 明确列出允许的域名，禁止 `*`
4. **SSL**: 全站 HTTPS，使用有效证书
5. **密码策略**: 所有密码使用强密码，通过环境变量注入

---

## 五、回滚机制

### 5.1 回滚策略

| 场景 | 策略 | 回滚时间 |
|------|------|---------|
| 代码缺陷 | Git revert + 重新构建部署 | ≤10分钟 |
| 配置错误 | 恢复 .env 配置 + 重启服务 | ≤5分钟 |
| 数据库问题 | 恢复数据库备份 | ≤30分钟 |
| 容器故障 | Docker rollback 到上一版本镜像 | ≤5分钟 |

### 5.2 回滚步骤

```bash
# 1. 确认回滚版本
git log --oneline -10

# 2. 回滚代码
git revert <commit-hash>

# 3. 重新构建并部署
cd deploy
docker compose -f docker-compose.services.yml up -d --build

# 4. 验证
curl http://localhost:8080/actuator/health
```

---

## 六、部署验证方案

### 6.1 验证清单

| 验证项 | 验证方法 | 通过标准 |
|--------|---------|---------|
| 服务启动 | `docker compose ps` | 所有服务 running |
| 健康检查 | `curl /actuator/health` | 返回 `{"status":"UP"}` |
| 网关路由 | `curl /api/user/list` | 返回有效响应 |
| 前端访问 | 浏览器访问 | 页面正常加载 |
| 数据库连接 | 检查日志 | 无连接错误 |
| Redis 连接 | 检查日志 | 无连接错误 |
| RabbitMQ 连接 | 检查日志 | 无连接错误 |
| Nacos 注册 | Nacos 控制台 | 所有服务已注册 |
| 监控 | Grafana 面板 | 指标正常采集 |
| SSL | `curl -v https://...` | 证书有效 |

### 6.2 性能验证

| 指标 | 工具 | 目标 |
|------|------|------|
| 接口 P95 响应 | k6 | ≤200ms |
| 首屏加载 | Lighthouse | ≤2s |
| 5000并发 | k6 | 无超时 |
| 1000并发订单 | k6 | 无数据错乱 |

---

## 七、运维配置

### 7.1 日志采集

- 所有服务日志输出到 stdout，由 Docker 收集
- 通过 Filebeat 采集到 ELK/Loki
- 日志保留: 生产30天，预发布14天，开发7天

### 7.2 备份策略

| 数据 | 备份频率 | 保留时长 | 存储位置 |
|------|---------|---------|---------|
| MySQL | 每日全量 + 实时binlog | 30天 | 本地 + 远程 |
| Redis | 每日 RDB | 7天 | 本地 |
| Nacos 配置 | 每日 | 30天 | 本地 |
| Docker 镜像 | 每次构建 | 最近5个版本 | 镜像仓库 |

### 7.3 告警规则

| 告警项 | 条件 | 级别 |
|--------|------|------|
| 服务宕机 | health check 失败 | Critical |
| 接口响应慢 | P95 > 500ms 持续5分钟 | High |
| CPU 使用率 | > 80% 持续10分钟 | High |
| 内存使用率 | > 85% 持续10分钟 | High |
| 磁盘使用率 | > 90% | Critical |
| 数据库连接数 | > 400 | High |
