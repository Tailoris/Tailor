# 测试环境服务器标准化部署与配置指南

## 1. 环境一致性保障

### 1.1 服务器配置标准

| 项目 | 配置 |
|------|------|
| 操作系统 | Ubuntu 22.04 LTS |
| CPU | 8 核 |
| 内存 | 16GB |
| 存储 | 100GB SSD |
| 网络 | 1Gbps |

### 1.2 软件版本标准

| 软件 | 版本 | 用途 |
|------|------|------|
| Docker | 24.0+ | 容器运行时 |
| Docker Compose | 2.20+ | 容器编排 |
| MySQL | 8.0 | 数据库 |
| Redis | 7.x | 缓存 |
| RabbitMQ | 3.12+ | 消息队列 |
| Nacos | 2.3+ | 服务注册/配置中心 |
| OpenJDK | 21 | Java 运行时 |
| Node.js | 18.x | 前端构建 |

## 2. 基础设施部署

### 2.1 1Panel 安装（可选）

```bash
# 安装 1Panel
curl -sSL https://resource.fit2cloud.com/1panel/package/quick_start.sh -o quick_start.sh && bash quick_start.sh

# 访问地址: http://<server-ip>:5080
```

### 2.2 数据库部署

#### 2.2.1 MySQL 配置

```bash
# 创建数据目录
mkdir -p /data/mysql

# 启动 MySQL 容器
docker run -d \
  --name tailor-is-mysql \
  -p 3306:3306 \
  -v /data/mysql:/var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=mysql_staging_password \
  -e TZ=Asia/Shanghai \
  --restart=always \
  mysql:8.0 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci
```

#### 2.2.2 创建业务数据库

```sql
-- 创建所有业务数据库
CREATE DATABASE IF NOT EXISTS tailor_is_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS tailor_is_product DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS tailor_is_order DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS tailor_is_payment DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS tailor_is_marketing DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS tailor_is_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS tailor_is_copyright DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS tailor_is_merchant DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS tailor_is_admin DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS tailor_is_pattern DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS tailor_is_community DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS tailor_is_supply DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS tailor_is_message DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS tailor_is_im DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS tailor_is_academy DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS tailor_is_analytics DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS nacos_config DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建应用用户
CREATE USER 'tailor'@'%' IDENTIFIED BY 'staging_password';
GRANT ALL PRIVILEGES ON tailor_is_*.* TO 'tailor'@'%';
GRANT ALL PRIVILEGES ON nacos_config.* TO 'tailor'@'%';
FLUSH PRIVILEGES;
```

### 2.3 Redis 部署

```bash
# 创建数据目录
mkdir -p /data/redis

# 启动 Redis 容器
docker run -d \
  --name tailor-is-redis \
  -p 6379:6379 \
  -v /data/redis:/data \
  -e TZ=Asia/Shanghai \
  --restart=always \
  redis:7-alpine \
  redis-server --requirepass redis_staging_password --appendonly yes
```

### 2.4 RabbitMQ 部署

```bash
# 创建数据目录
mkdir -p /data/rabbitmq

# 启动 RabbitMQ 容器
docker run -d \
  --name tailor-is-rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -v /data/rabbitmq:/var/lib/rabbitmq \
  -e RABBITMQ_DEFAULT_USER=rabbitmq \
  -e RABBITMQ_DEFAULT_PASS=rabbitmq_staging \
  -e TZ=Asia/Shanghai \
  --restart=always \
  rabbitmq:3-management-alpine
```

### 2.5 Nacos 部署

```bash
# 创建数据目录
mkdir -p /data/nacos

# 启动 Nacos 容器
docker run -d \
  --name tailor-is-nacos \
  -p 8848:8848 \
  -v /data/nacos:/home/nacos/data \
  -e MODE=standalone \
  -e NACOS_AUTH_ENABLE=true \
  -e NACOS_AUTH_TOKEN=SecretKey012345678901234567890123456789012345678901234567890123456789 \
  -e SPRING_DATASOURCE_PLATFORM=mysql \
  -e MYSQL_SERVICE_HOST=localhost \
  -e MYSQL_SERVICE_PORT=3306 \
  -e MYSQL_SERVICE_DB_NAME=nacos_config \
  -e MYSQL_SERVICE_USER=tailor \
  -e MYSQL_SERVICE_PASSWORD=staging_password \
  -e TZ=Asia/Shanghai \
  --restart=always \
  nacos/nacos-server:v2.3.2
```

### 2.6 Nacos 配置初始化

在 Nacos 控制台创建 `tailor-is-common.yml` 共享配置：

```yaml
# 数据ID: tailor-is-common.yml
# 分组: DEFAULT_GROUP

# 日志配置
logging:
  level:
    root: INFO
    com.tailoris: DEBUG

# 线程池配置
spring:
  task:
    execution:
      pool:
        core-size: 10
        max-size: 50
        queue-capacity: 1000

# 限流配置
resilience4j:
  rate-limiter:
    instances:
      default:
        limit-for-period: 100
        limit-refresh-period: 1s
        timeout-duration: 0
```

## 3. 网络安全配置

### 3.1 防火墙配置

```bash
# 允许必要端口
ufw allow 22/tcp         # SSH
ufw allow 80/tcp         # HTTP
ufw allow 443/tcp        # HTTPS
ufw allow 3306/tcp       # MySQL
ufw allow 6379/tcp       # Redis
ufw allow 5672/tcp       # RabbitMQ
ufw allow 8848/tcp       # Nacos
ufw allow 9090/tcp       # Prometheus
ufw allow 3000/tcp       # Grafana
ufw allow 9000/tcp       # Core Gateway
ufw enable
```

### 3.2 SSL 证书配置

```bash
# 使用 Let's Encrypt 获取证书
apt-get update && apt-get install -y certbot

# 获取证书
certbot certonly --standalone -d staging.tailoris.com

# 证书路径
# /etc/letsencrypt/live/staging.tailoris.com/fullchain.pem
# /etc/letsencrypt/live/staging.tailoris.com/privkey.pem
```

## 4. 应用部署流程

### 4.1 部署步骤

```bash
# 1. 克隆代码
cd /opt
git clone https://github.com/tailoris/tailoris.git
cd tailoris

# 2. 配置环境变量
cp deploy/.env.example deploy/.env
# 编辑 .env 文件，配置正确的数据库连接等

# 3. 启动基础设施
cd deploy
docker compose -f docker-compose.staging.yml up -d mysql redis rabbitmq nacos

# 4. 等待基础设施就绪
sleep 60

# 5. 初始化数据库
docker exec -i tailor-is-mysql mysql -u root -p < tailor-is/sql/000_init_database.sql

# 6. 启动应用服务
docker compose -f docker-compose.staging.yml up -d
```

### 4.2 冒烟测试

```bash
# 检查服务状态
docker compose -f docker-compose.staging.yml ps

# 测试核心网关
curl -s http://localhost:9000/api/health

# 测试用户服务
curl -s http://localhost:8101/actuator/health

# 测试 Nacos 服务注册
curl -s http://localhost:8848/nacos/v1/ns/instance/list?serviceName=tailor-is-user
```

## 5. 环境验收标准

### 5.1 功能完整性测试

| 测试项 | 测试方法 | 预期结果 |
|--------|----------|----------|
| 服务注册 | 检查 Nacos 控制台 | 所有服务注册成功 |
| API 网关 | 访问 /api/health | 返回 200 OK |
| 数据库连接 | 检查服务日志 | 无连接错误 |
| 缓存连接 | 检查服务日志 | 无连接错误 |
| 消息队列 | 发送测试消息 | 消息正常消费 |

### 5.2 性能基准测试

| 指标 | 目标值 |
|------|--------|
| API 响应时间 | P95 ≤ 200ms |
| 数据库查询时间 | ≤ 100ms |
| 服务启动时间 | ≤ 60s |
| CPU 使用率 | ≤ 70% |
| 内存使用率 | ≤ 80% |

## 6. 维护指南

### 6.1 日常维护

| 任务 | 频率 | 操作 |
|------|------|------|
| 日志检查 | 每日 | `docker logs <container>` |
| 服务状态 | 每日 | `docker compose ps` |
| 资源监控 | 每日 | `top`, `df -h` |
| 数据库备份 | 每日 | `mysqldump` |
| 安全更新 | 每周 | `apt update && apt upgrade` |

### 6.2 故障处理

| 故障类型 | 处理步骤 |
|----------|----------|
| 服务宕机 | 检查日志 → 分析原因 → 重启服务 |
| 数据库连接失败 | 检查网络 → 检查凭证 → 重启数据库 |
| 内存不足 | 增加资源限制 → 优化 JVM 参数 |
| CPU 过高 | 分析进程 → 优化代码 → 增加 CPU 限制 |

### 6.3 紧急联系人

| 角色 | 联系方式 |
|------|----------|
| DevOps 负责人 | devops@tailoris.com |
| 后端负责人 | backend@tailoris.com |
| 前端负责人 | frontend@tailoris.com |

## 7. 部署文档交付清单

- [ ] 服务器配置清单
- [ ] 部署流程图
- [ ] 环境变量配置说明
- [ ] 服务端口映射表
- [ ] 数据库初始化脚本
- [ ] 安全配置说明
- [ ] 维护指南
- [ ] 故障处理预案
- [ ] 验收测试报告
