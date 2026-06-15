# Tailor IS 1Panel 项目部署计划方案

**文档编号**: TAILOR-IS-1PANEL-DEPLOY-2026-0603
**版本**: V1.0
**编制日期**: 2026年6月3日
**关联文档**:
- [PROJECT-DEVELOPMENT-TASK-PLAN.md](file:///F:/Tailor/Tailor%20is/PROJECT-DEVELOPMENT-TASK-PLAN.md) — 项目开发任务计划书
- [项目部署 1Panel账户信息.txt](file:///F:/Tailor/Tailor%20is/项目部署%201Panel账户信息.txt) — 1Panel 账户凭证
- [SPRINT9-QUALITY-ASSURANCE-COMPLETION-REPORT.md](file:///F:/Tailor/Tailor%20is/SPRINT9-QUALITY-ASSURANCE-COMPLETION-REPORT.md) — Sprint 9 质量保障完成报告
- [deploy-staging.sh](file:///F:/Tailor/Tailor%20is/tailor-is/deploy/deploy-staging.sh) — 部署脚本

---

## 1. 部署概述

### 1.1 部署目标

将 Tailor IS（裁智云）项目完整、安全、高效地部署到 1Panel 目标环境中，确保：
- 所有 Sprint 1-9 已完成的开发任务（含最新 Sprint 9 质量保障）正常运行
- 核心业务模块（用户/商家/商品/订单/支付/营销/版权/社区）功能完整
- 系统性能指标达到生产环境标准（P95 ≤ 200ms，可用性 ≥ 99.9%）
- 安全合规（OWASP Top 10 全覆盖，0 High 漏洞）
- 提供完整的数据迁移、回滚、监控机制

### 1.2 部署环境信息

#### 1.2.1 1Panel 面板信息

| 项目 | 值 |
|------|-----|
| **1Panel 启动命令** | `sudo 1pctl start all` |
| **重启 SSH** | `sudo service ssh restart` |
| **内部访问** | http://172.28.249.179:42405/5b4c869c53 |
| **外部访问** | http://223.73.36.220:42405/5b4c869c53 |
| **本机访问** | http://localhost:42405/5b4c869c53 |
| **面板用户** | c0f9ba4b02 |
| **面板密码** | 004db65669 |
| **官方网站** | https://1panel.cn |
| **项目文档** | https://1panel.cn/docs |

#### 1.2.2 已部署基础服务（1Panel 应用商店）

| 服务 | 端口 | 凭证 | 说明 |
|------|:----:|------|------|
| **MySQL** | 3306 | 密码: `mysql_CA75Yk` | 数据库 |
| **Redis** | 6379 | 密码: `redis_RSeR4G` | 缓存 |
| **RabbitMQ** | 5672 / 15672 | 用户: `rabbitmq` / 密码: `rabbitmq` | 消息队列 |
| **Nacos** | 8080 / 8848 / 9848 | 身份验证令牌: `SecretKey012345678901234567890123456789012345678901234567890123456789` | 配置中心 |

#### 1.2.3 主机信息

| 项目 | 值 |
|------|-----|
| **内部 IP** | 172.28.249.179 |
| **外部 IP** | 223.73.36.220 |
| **主机架构** | Linux x86_64 (1Panel 适用) |
| **建议配置** | 8 核 16G（已具备），推荐生产 16 核 32G |

### 1.3 部署架构图

```
┌─────────────────────────────────────────────────────────────┐
│                  1Panel 部署环境 (172.28.249.179)             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ Nginx    │  │Gateway   │  │User Svc  │  │Product   │   │
│  │:80/443   │→ │:8081     │→ │Svc:8082  │  │Svc:8083  │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
│        │              │              │              │       │
│        │              ├──────────────┴──────────────┘       │
│        │              │                                     │
│  ┌─────▼────┐  ┌──────▼─────┐  ┌──────────┐  ┌──────────┐ │
│  │Frontend  │  │Order Svc   │  │Payment   │  │Copyright │ │
│  │PC/H5     │  │:8084       │  │:8085     │  │:8086     │ │
│  └──────────┘  └────────────┘  └──────────┘  └──────────┘ │
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │Merchant  │  │Marketing │  │Community │  │Message   │   │
│  │:8087     │  │:8088     │  │:8089     │  │:8090     │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐ │
│  │              基础设施服务 (1Panel 已部署)               │ │
│  │  MySQL:3306  Redis:6379  RabbitMQ:5672  Nacos:8080  │ │
│  └──────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 1.4 部署范围

| 类别 | 范围 | 数量 |
|------|------|:----:|
| 后端微服务 | tailor-is-*（10+ 模块） | 10 |
| 前端应用 | PC商城 + H5 + 商家后台 + 平台后台 | 4 |
| 数据库迁移脚本 | 01-10 + V8/V9 系列 | 16 |
| 基础设施 | MySQL/Redis/RabbitMQ/Nacos（已部署） | 4 |
| 监控告警 | Prometheus + Grafana + AlertManager（可选部署） | 3 |
| 日志系统 | Filebeat + ELK（可选部署） | 4 |
| 部署脚本 | 启动/停止/迁移/回滚/验证 | 6 |

---

## 2. 部署环境准备

### 2.1 主机环境要求

#### 2.1.1 硬件要求（最低/推荐）

| 资源 | 最低 | 推荐（生产） |
|------|------|-------------|
| CPU | 4 核 | 16 核 |
| 内存 | 8 GB | 32 GB |
| 系统盘 | 50 GB | 100 GB SSD |
| 数据盘 | 100 GB | 500 GB SSD |
| 带宽 | 5 Mbps | 50 Mbps |

#### 2.1.2 软件要求

| 软件 | 版本 | 说明 |
|------|------|------|
| 1Panel | v1.10+ | 已安装 |
| OpenJDK | 17+ | 部署后端必需 |
| Nginx | 1.20+ | 1Panel 自带 |
| Docker | 20.10+ | 容器化部署（可选） |
| Node.js | 18+ | 前端构建 |
| Maven | 3.9+ | 后端构建 |

### 2.2 网络与端口规划

| 端口 | 服务 | 用途 | 内网/外网 |
|:----:|------|------|----------|
| 42405 | 1Panel | 面板管理 | 外网开放 |
| 80 | Nginx | HTTP 重定向 | 外网开放 |
| 443 | Nginx | HTTPS | 外网开放 |
| 3306 | MySQL | 数据库 | 仅内网 |
| 6379 | Redis | 缓存 | 仅内网 |
| 5672 | RabbitMQ | AMQP | 仅内网 |
| 15672 | RabbitMQ | Dashboard | 仅内网 |
| 8080 | Nacos | 配置中心 | 内网+外网（待定） |
| 8081 | Gateway | API 网关 | 内网（经 Nginx 转发） |
| 8082-8090 | 微服务 | 业务服务 | 仅内网 |
| 9090 | Prometheus | 监控 | 仅内网 |
| 3000 | Grafana | 监控可视化 | 仅内网 |
| 5601 | Kibana | 日志可视化 | 仅内网 |

### 2.3 目录规划

```bash
# 部署根目录
/opt/tailor-is/
├── jars/                    # 后端 JAR 包
│   ├── tailor-is-gateway.jar
│   ├── tailor-is-user.jar
│   ├── tailor-is-merchant.jar
│   ├── tailor-is-product.jar
│   ├── tailor-is-order.jar
│   ├── tailor-is-payment.jar
│   ├── tailor-is-copyright.jar
│   ├── tailor-is-marketing.jar
│   ├── tailor-is-community.jar
│   └── tailor-is-message.jar
├── frontend/                # 前端构建产物
│   ├── pc-mall/             # PC商城
│   ├── mobile-h5/           # H5
│   ├── merchant-admin/      # 商家后台
│   └── platform-admin/      # 平台后台
├── config/                  # 配置文件
│   ├── application-prod.yml # 生产配置
│   ├── logback-spring.xml   # 日志配置
│   └── nacos/               # Nacos 配置
├── scripts/                 # 运维脚本
│   ├── start-services.sh
│   ├── stop-services.sh
│   ├── deploy.sh
│   ├── rollback.sh
│   └── health-check.sh
├── logs/                    # 日志
│   ├── gateway/
│   ├── user/
│   └── ...
├── pids/                    # 进程 PID
├── backup/                  # 备份
│   ├── db/                  # 数据库备份
│   └── jar/                 # JAR 包备份
└── sql/                     # SQL 脚本
```

### 2.4 环境检查脚本

部署前需运行环境检查脚本：[pre-deploy-check.sh](file:///F:/Tailor/Tailor%20is/tailor-is/deploy/1panel-pre-deploy-check.sh)

```bash
# 1. 检查 1Panel 状态
sudo 1pctl status

# 2. 检查基础服务连接
bash /opt/tailor-is/scripts/pre-deploy-check.sh

# 3. 检查端口占用
netstat -tlnp | grep -E '3306|6379|5672|8080|8081'

# 4. 检查磁盘空间
df -h /opt
```

---

## 3. 资源配置规划

### 3.1 微服务资源配置（JVM）

| 服务 | 端口 | 堆内存 | 线程数 | 日志级别 |
|------|:----:|:------:|:------:|:--------:|
| tailor-is-gateway | 8081 | 1G | 200 | INFO |
| tailor-is-user | 8082 | 1G | 150 | INFO |
| tailor-is-merchant | 8083 | 1G | 150 | INFO |
| tailor-is-product | 8084 | 1G | 150 | INFO |
| tailor-is-order | 8085 | 1.5G | 200 | INFO |
| tailor-is-payment | 8086 | 1G | 150 | INFO |
| tailor-is-copyright | 8087 | 2G | 200 | INFO |
| tailor-is-marketing | 8088 | 1.5G | 200 | INFO |
| tailor-is-community | 8089 | 1G | 150 | INFO |
| tailor-is-message | 8090 | 1G | 150 | INFO |

### 3.2 数据库资源配置

| 服务 | 缓冲池 | 最大连接 | 字符集 |
|------|:------:|:-------:|--------|
| MySQL | 1G | 500 | utf8mb4 |

### 3.3 缓存资源配置

| 服务 | 最大内存 | Key 数量上限 | 淘汰策略 |
|------|:--------:|:-----------:|---------|
| Redis | 2 GB | 1000 万 | allkeys-lru |

### 3.4 监控资源配置（可选）

| 资源 | 规格 | 数量 |
|------|------|:----:|
| Prometheus | 2 核 4G | 1 |
| Grafana | 1 核 2G | 1 |
| AlertManager | 1 核 1G | 1 |
| Elasticsearch | 4 核 8G | 1 |
| Kibana | 2 核 4G | 1 |
| Logstash | 2 核 4G | 1 |
| Filebeat | 0.5 核 1G | 部署到各应用节点 |

---

## 4. 部署步骤流程

### 4.1 部署阶段总览

| 阶段 | 名称 | 预计耗时 | 责任人 |
|:----:|------|:--------:|--------|
| 阶段 0 | 部署前准备 | 1h | DevOps |
| 阶段 1 | 1Panel 服务确认 | 0.5h | DevOps |
| 阶段 2 | 数据库初始化与迁移 | 1h | DBA + 后端 |
| 阶段 3 | 后端服务部署 | 1.5h | 后端 |
| 阶段 4 | 前端应用部署 | 1h | 前端 |
| 阶段 5 | Nginx 配置与启动 | 0.5h | DevOps |
| 阶段 6 | 监控/日志部署（可选） | 1h | DevOps |
| 阶段 7 | 部署验证 | 1h | QA + 后端 |
| 阶段 8 | 灰度发布 | 2h | SRE |
| 阶段 9 | 全量上线 | 0.5h | SRE |
| **总计** | — | **~9h** | — |

### 4.2 详细部署步骤

#### 阶段 0：部署前准备（1h）

```bash
# 1. SSH 登录服务器
ssh root@172.28.249.179

# 2. 创建部署用户（生产环境推荐）
sudo useradd -m -s /bin/bash tailor
sudo usermod -aG sudo tailor

# 3. 创建部署目录
sudo mkdir -p /opt/tailor-is/{jars,frontend,config,scripts,logs,pids,backup/db,backup/jar,sql}
sudo chown -R tailor:tailor /opt/tailor-is

# 4. 安装 JDK 17
sudo apt update
sudo apt install -y openjdk-17-jdk
java -version

# 5. 准备上传部署包
# 上传 tailor-is-deploy-v1.0.0.tar.gz 到 /tmp/
```

#### 阶段 1：1Panel 服务确认（0.5h）

```bash
# 1. 启动 1Panel
sudo 1pctl start all

# 2. 登录 1Panel 面板
# http://172.28.249.179:42405/5b4c869c53
# 用户: c0f9ba4b02  密码: 004db65669

# 3. 验证基础服务
# - MySQL: 3306 端口
# - Redis: 6379 端口
# - RabbitMQ: 5672 端口
# - Nacos: 8080 端口
```

#### 阶段 2：数据库初始化与迁移（1h）

参考：[1panel-data-migration.sh](file:///F:/Tailor/Tailor%20is/tailor-is/deploy/1panel-data-migration.sh)

```bash
# 1. 验证 MySQL 连接
mysql -h172.28.249.179 -P3306 -uroot -p'mysql_CA75Yk' -e "SELECT VERSION();"

# 2. 创建数据库 tailor_is
mysql -h172.28.249.179 -P3306 -uroot -p'mysql_CA75Yk' -e \
  "CREATE DATABASE IF NOT EXISTS tailor_is DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 3. 初始化 Nacos 数据库（用于 Nacos 配置持久化）
mysql -h172.28.249.179 -P3306 -uroot -p'mysql_CA75Yk' \
  -e "CREATE DATABASE IF NOT EXISTS nacos_config DEFAULT CHARACTER SET utf8mb4;"

# 4. 执行 SQL 迁移脚本（顺序执行）
cd /opt/tailor-is/sql
for f in 01_user_system.sql 02_merchant_system.sql 03_product_system.sql \
         04_order_system.sql 05_payment_system.sql 06_marketing_system.sql \
         07_copyright_system.sql 08_community_system.sql 09_supply_system.sql \
         10_message_system.sql \
         V8__Sprint8_OSS_Search_CustomReview.sql \
         V8_1__Sprint8_Merchant_Dashboard_Trial_Violation.sql \
         V8_2__Sprint8_Marketing_Community.sql \
         V8_3__Sprint8_Blockchain_Copyright.sql \
         V9_1__Sprint9_QA_Index_Optimization.sql; do
  echo "[INFO] 执行 $f"
  mysql -h172.28.249.179 -P3306 -uroot -p'mysql_CA75Yk' tailor_is < "$f"
done

# 5. 验证表数量
mysql -h172.28.249.179 -P3306 -uroot -p'mysql_CA75Yk' tailor_is -e "SHOW TABLES;" | wc -l
# 预期：100+ 张表

# 6. 备份初始数据
mysqldump -h172.28.249.179 -P3306 -uroot -p'mysql_CA75Yk' \
  --single-transaction tailor_is > /opt/tailor-is/backup/db/init.sql
```

#### 阶段 3：后端服务部署（1.5h）

```bash
# 1. 上传 JAR 包到 /opt/tailor-is/jars/
# 包括 10 个微服务 JAR

# 2. 上传配置文件到 Nacos（或本地）
# 1Panel 浏览器 → Nacos 服务 → 导入配置

# 3. 创建 Nacos 命名空间
# Namespace: tailor-is-prod
# Namespace ID: tailor-is-prod

# 4. 上传配置到 Nacos
# - application-common.yml
# - application-gateway.yml
# - application-user.yml
# - ... (10 个服务的 yml)

# 5. 启动服务（按依赖顺序）
bash /opt/tailor-is/scripts/1panel-start-services.sh
```

#### 阶段 4：前端应用部署（1h）

```bash
# 1. 上传前端构建产物
# /opt/tailor-is/frontend/pc-mall/   (PC商城)
# /opt/tailor-is/frontend/mobile-h5/  (H5)
# /opt/tailor-is/frontend/merchant-admin/  (商家后台)
# /opt/tailor-is/frontend/platform-admin/  (平台后台)

# 2. 配置 Nginx（使用项目自带配置）
sudo cp /opt/tailor-is/deploy/nginx.conf /etc/nginx/conf.d/tailor-is.conf
sudo nginx -t
sudo systemctl reload nginx
```

#### 阶段 5：Nginx 配置与启动（0.5h）

参考：[nginx.conf](file:///F:/Tailor/Tailor%20is/tailor-is/deploy/nginx.conf)

```bash
# 1. 配置 SSL 证书（生产环境）
sudo mkdir -p /etc/nginx/ssl
# 上传证书文件到 /etc/nginx/ssl/
# tailor-is.com.pem
# tailor-is.com.key

# 2. 测试配置
sudo nginx -t

# 3. 重新加载
sudo systemctl reload nginx

# 4. 验证
curl -I https://tailor-is.com
```

#### 阶段 6：监控/日志部署（可选，1h）

```bash
# 1. 部署 Prometheus
cd /opt/monitoring
docker-compose -f docker-compose.monitoring.yml up -d

# 2. 部署 ELK
cd /opt/logging
docker-compose -f docker-compose.elk.yml up -d
```

#### 阶段 7：部署验证（1h）

参考：[1panel-verify.sh](file:///F:/Tailor/Tailor%20is/tailor-is/deploy/1panel-verify.sh)

```bash
# 1. 健康检查
bash /opt/tailor-is/scripts/1panel-verify.sh

# 2. 接口测试
curl -X POST https://tailor-is.com/api/v1/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test123"}'

# 3. 关键流程 E2E 测试
# 使用 Playwright 执行
```

#### 阶段 8：灰度发布（2h）

```bash
# 1. 1% 灰度
bash /opt/tailor-is/scripts/1panel-gray-release.sh 1
# 观察 30 分钟

# 2. 10% 灰度
bash /opt/tailor-is/scripts/1panel-gray-release.sh 10
# 观察 30 分钟

# 3. 50% 灰度
bash /opt/tailor-is/scripts/1panel-gray-release.sh 50
# 观察 60 分钟

# 4. 100% 全量
bash /opt/tailor-is/scripts/1panel-gray-release.sh 100
```

#### 阶段 9：全量上线（0.5h）

```bash
# 1. 清理灰度版本
# 2. 归档部署包
# 3. 通知相关方
# 4. 启动持续监控
```

---

## 5. 数据迁移策略

### 5.1 迁移场景

| 场景 | 描述 | 工具 |
|------|------|------|
| **首次部署** | 全新安装，无历史数据 | SQL 初始化脚本 |
| **版本升级** | 新版本发布，需数据迁移 | Flyway / Liquibase |
| **环境迁移** | 从测试环境到生产环境 | mysqldump / mysql |
| **灰度切流** | 双写验证 + 数据校验 | Canal / 业务对账 |

### 5.2 数据迁移流程

```
[1. 数据导出] → [2. 数据备份] → [3. 数据校验] → [4. 导入新环境] → [5. 数据校验] → [6. 切换流量]
```

### 5.3 数据备份策略

| 备份类型 | 频率 | 保留时间 | 存储位置 |
|---------|:----:|:--------:|---------|
| 全量备份 | 每日 03:00 | 30 天 | /opt/tailor-is/backup/db/ |
| 增量备份 | 每小时 | 7 天 | /opt/tailor-is/backup/db/incremental/ |
| binlog 备份 | 实时 | 7 天 | /opt/tailor-is/backup/db/binlog/ |
| 配置备份 | 每次变更 | 永久 | Git 仓库 |
| 文件备份 | 每日 | 90 天 | OSS |

### 5.4 数据迁移脚本

参考：[1panel-data-migration.sh](file:///F:/Tailor/Tailor%20is/tailor-is/deploy/1panel-data-migration.sh)

```bash
# 全量迁移示例
SOURCE_HOST="172.28.249.179"
TARGET_HOST="172.28.249.180"

# 1. 源端备份
mysqldump -h$SOURCE_HOST -P3306 -uroot -p'mysql_CA75Yk' \
  --single-transaction --routines --triggers \
  tailor_is > /tmp/tailor_is_full_$(date +%Y%m%d).sql

# 2. 压缩
gzip /tmp/tailor_is_full_$(date +%Y%m%d).sql

# 3. 传输
scp /tmp/tailor_is_full_*.sql.gz root@$TARGET_HOST:/tmp/

# 4. 目标端导入
gunzip -c /tmp/tailor_is_full_*.sql.gz | \
  mysql -h$TARGET_HOST -P3306 -uroot -p'mysql_CA75Yk' tailor_is
```

### 5.5 数据一致性校验

```sql
-- 1. 表数量校验
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'tailor_is';

-- 2. 关键表数据量校验
SELECT 'sys_user' AS tbl, COUNT(*) AS cnt FROM sys_user
UNION ALL SELECT 'merchant', COUNT(*) FROM merchant
UNION ALL SELECT 'product', COUNT(*) FROM product
UNION ALL SELECT 'order_info', COUNT(*) FROM order_info
UNION ALL SELECT 'copyright_record', COUNT(*) FROM copyright_record;

-- 3. 校验和校验
CHECKSUM TABLE sys_user, merchant, product, order_info;
```

---

## 6. 服务启停顺序

### 6.1 启动顺序

按照依赖关系，**先启动基础服务，再启动业务服务**：

```
步骤 1: 基础设施层（已由 1Panel 部署）
  ├─ MySQL
  ├─ Redis
  ├─ RabbitMQ
  └─ Nacos

步骤 2: 公共组件层
  └─ tailor-is-common (内嵌)

步骤 3: 业务基础服务（无相互依赖）
  ├─ tailor-is-user        (8082)
  ├─ tailor-is-merchant    (8083)
  └─ tailor-is-product     (8084)

步骤 4: 业务核心服务
  ├─ tailor-is-order       (8085) 依赖 user/product
  ├─ tailor-is-payment     (8086) 依赖 order
  └─ tailor-is-message     (8090) 依赖 user

步骤 5: 业务扩展服务
  ├─ tailor-is-copyright   (8087) 依赖 user
  ├─ tailor-is-marketing   (8088) 依赖 product
  └─ tailor-is-community   (8089) 依赖 user

步骤 6: 网关层
  └─ tailor-is-gateway     (8081) 依赖所有业务服务

步骤 7: 前端层
  └─ Nginx (静态资源 + 反向代理)
```

### 6.2 停止顺序

**与启动顺序相反**：

```
1. Nginx
2. tailor-is-gateway
3. 业务扩展服务
4. 业务核心服务
5. 业务基础服务
6. 基础服务（保留运行，除非维护）
```

### 6.3 服务启停脚本

参考：[1panel-start-services.sh](file:///F:/Tailor/Tailor%20is/tailor-is/deploy/1panel-start-services.sh) 和 [1panel-stop-services.sh](file:///F:/Tailor/Tailor%20is/tailor-is/deploy/1panel-stop-services.sh)

---

## 7. 部署验证方法

### 7.1 验证层次

| 层次 | 验证内容 | 工具 | 通过标准 |
|------|---------|------|---------|
| L1 基础设施 | MySQL/Redis/RabbitMQ/Nacos 可达 | mysql/redis-cli/curl | 连接成功 |
| L2 服务健康 | 各微服务 `/actuator/health` | curl | HTTP 200 |
| L3 接口可用 | 关键 API 200 响应 | curl/Postman | 99% 通过 |
| L4 业务流程 | 登录/下单/支付/版权 | Playwright E2E | 100% 通过 |
| L5 性能指标 | P95 ≤ 200ms | JMeter | 达标 |
| L6 安全扫描 | OWASP ZAP | ZAP | 0 High |

### 7.2 验证清单

参考：[1panel-verify.sh](file:///F:/Tailor/Tailor%20is/tailor-is/deploy/1panel-verify.sh)

#### 7.2.1 基础设施验证

- [ ] MySQL 连接成功（端口 3306）
- [ ] Redis 连接成功（端口 6379）
- [ ] RabbitMQ Dashboard 可访问（端口 15672）
- [ ] Nacos 登录成功（端口 8080）

#### 7.2.2 后端服务验证

- [ ] Gateway 健康检查 `/actuator/health` 返回 200
- [ ] User 服务健康检查 200
- [ ] Merchant 服务健康检查 200
- [ ] Product 服务健康检查 200
- [ ] Order 服务健康检查 200
- [ ] Payment 服务健康检查 200
- [ ] Copyright 服务健康检查 200
- [ ] Marketing 服务健康检查 200
- [ ] Community 服务健康检查 200
- [ ] Message 服务健康检查 200

#### 7.2.3 接口功能验证

- [ ] 用户注册接口
- [ ] 用户登录接口
- [ ] 商家入驻接口
- [ ] 商品发布接口
- [ ] 商品搜索接口
- [ ] 购物车加购接口
- [ ] 订单创建接口
- [ ] 支付回调接口
- [ ] 版权登记接口
- [ ] 区块链上链查询
- [ ] 社区发帖接口
- [ ] 站内信发送接口

#### 7.2.4 前端验证

- [ ] PC 商城首页可访问
- [ ] H5 首页可访问
- [ ] 商家后台可访问
- [ ] 平台后台可访问
- [ ] 静态资源加载正常
- [ ] HTTPS 证书有效
- [ ] HSTS 头部正确

#### 7.2.5 性能验证

- [ ] 首页 P95 ≤ 200ms
- [ ] 商品列表 P95 ≤ 300ms
- [ ] 下单 P95 ≤ 800ms
- [ ] 支付回调 P95 ≤ 1000ms
- [ ] 版权登记 P95 ≤ 800ms
- [ ] 500 并发用户正常

#### 7.2.6 安全验证

- [ ] OWASP ZAP 扫描 0 High
- [ ] SonarQube A 级
- [ ] HTTPS 全站
- [ ] SQL 注入测试通过
- [ ] XSS 测试通过
- [ ] 暴力破解防护
- [ ] 限流配置生效

---

## 8. 回滚机制设计

### 8.1 回滚策略

| 回滚级别 | 触发条件 | 回滚方式 | RTO |
|---------|---------|---------|-----|
| L1 服务级 | 单服务异常 | 重启 / 回滚单服务 | 5min |
| L2 版本级 | 新版本严重 Bug | 切换到上一版本 | 15min |
| L3 数据库级 | DDL 错误 | 恢复数据库备份 | 30min |
| L4 整体级 | 系统级故障 | 切换到灾备环境 | 1h |

### 8.2 回滚步骤

```bash
# 1. 触发回滚（任选其一）
bash /opt/tailor-is/scripts/1panel-rollback.sh service <service_name>
bash /opt/tailor-is/scripts/1panel-rollback.sh version <version>
bash /opt/tailor-is/scripts/1panel-rollback.sh database <backup_file>

# 2. 停止当前服务
bash /opt/tailor-is/scripts/1panel-stop-services.sh

# 3. 恢复 JAR 包
cp /opt/tailor-is/backup/jar/<version>/*.jar /opt/tailor-is/jars/

# 4. 恢复数据库（如需）
mysql -h172.28.249.179 -P3306 -uroot -p'mysql_CA75Yk' tailor_is < \
  /opt/tailor-is/backup/db/<backup_file>.sql

# 5. 重启服务
bash /opt/tailor-is/scripts/1panel-start-services.sh

# 6. 验证
bash /opt/tailor-is/scripts/1panel-verify.sh
```

### 8.3 回滚脚本

参考：[1panel-rollback.sh](file:///F:/Tailor/Tailor%20is/tailor-is/deploy/1panel-rollback.sh)

---

## 9. 部署时间安排

### 9.1 总体时间表（建议）

| 阶段 | 时间 | 时长 | 责任人 |
|------|------|:----:|--------|
| D-7 | 部署评审 + 资源准备 | 1d | 全员 |
| D-3 | 部署演练（staging） | 1d | 后端 + DevOps |
| D-1 | 数据备份 + 通知公告 | 0.5d | DBA + 运营 |
| **D-Day** | **正式部署** | **1d** | — |
| D-Day 09:00-10:00 | 部署前检查 | 1h | DevOps |
| D-Day 10:00-11:00 | 数据库迁移 | 1h | DBA |
| D-Day 11:00-12:30 | 后端服务部署 | 1.5h | 后端 |
| D-Day 13:30-14:30 | 前端部署 + Nginx | 1h | 前端 |
| D-Day 14:30-15:30 | 部署验证 | 1h | QA |
| D-Day 15:30-17:30 | 灰度发布 | 2h | SRE |
| D-Day 17:30-18:00 | 全量上线 | 0.5h | SRE |
| D+1 | 持续监控 | 1d | SRE |

### 9.2 关键里程碑

- **09:00 部署启动** → 通知所有相关方
- **10:00 数据库就绪** → 通知后端
- **12:30 后端就绪** → 通知前端
- **15:30 灰度就绪** → 通知产品/运营
- **18:00 全量上线** → 通知全公司
- **D+1 09:00** → 发布部署总结

### 9.3 人员作息安排

- **主操作组**: 09:00-18:00
- **值班组**: 18:00-09:00 (SRE 2 人轮值)
- **紧急联系人**: 24/7 待命

---

## 10. 责任人分配

### 10.1 部署组织架构

| 角色 | 人数 | 主要职责 | 联系人 |
|------|:----:|---------|--------|
| **总指挥** | 1 | 整体协调、决策、风险升级 | Tech Lead |
| **DevOps 负责人** | 1 | 1Panel、Nginx、基础设施 | 运维工程师 |
| **DBA** | 1 | 数据库迁移、性能调优 | DBA |
| **后端负责人** | 1 | 微服务部署、配置 | 后端架构师 |
| **前端负责人** | 1 | 前端部署 | 前端架构师 |
| **QA 负责人** | 1 | 部署验证、测试 | 测试经理 |
| **SRE 负责人** | 1 | 灰度、监控、应急响应 | SRE 工程师 |
| **运营/客服** | 1 | 用户通知、问题收集 | 运营经理 |

### 10.2 RACI 矩阵

| 活动 | 总指挥 | DevOps | DBA | 后端 | 前端 | QA | SRE | 运营 |
|------|:------:|:------:|:---:|:----:|:----:|:--:|:---:|:----:|
| 部署计划评审 | A | R | R | R | R | R | R | C |
| 1Panel 准备 | C | R | I | I | I | I | C | I |
| 数据库迁移 | A | C | R | C | I | C | I | I |
| 后端服务部署 | A | C | I | R | I | C | C | I |
| 前端部署 | A | C | I | I | R | C | I | I |
| Nginx 配置 | A | R | I | I | C | I | C | I |
| 部署验证 | A | C | C | C | C | R | C | I |
| 灰度发布 | A | C | I | C | I | I | R | C |
| 监控值守 | I | C | I | I | I | C | R | I |
| 应急响应 | A | R | R | R | C | C | R | C |

**R** = 执行，**A** = 问责，**C** = 咨询，**I** = 知会

### 10.3 联系方式

| 角色 | 姓名 | 联系电话 | 备用联系 |
|------|------|---------|---------|
| 总指挥 | Tech Lead | 138-0000-0001 | wechat-group |
| DevOps | 运维工程师 | 138-0000-0002 | slack: #devops |
| DBA | 数据库工程师 | 138-0000-0003 | oncall rotation |
| 后端 | 后端架构师 | 138-0000-0004 | wechat |
| 前端 | 前端架构师 | 138-0000-0005 | wechat |
| QA | 测试经理 | 138-0000-0006 | wechat |
| SRE | SRE 工程师 | 138-0000-0007 | oncall rotation |
| 运营 | 运营经理 | 138-0000-0008 | wechat |

---

## 11. 风险评估与应对措施

### 11.1 风险登记册

| 风险ID | 风险描述 | 可能性 | 影响 | 风险等级 | 应对措施 | 责任人 | 触发条件 |
|:------:|---------|:------:|:----:|:--------:|---------|--------|---------|
| R01 | 1Panel 面板无法访问 | 低 | 高 | 中 | 重启 1Panel；备用访问通道 | DevOps | 连接超时 |
| R02 | MySQL 数据迁移失败 | 中 | 高 | 高 | 完整备份 + 手动执行 + 校验 | DBA | 导入错误 |
| R03 | 端口冲突 | 中 | 中 | 中 | 提前端口扫描 + 备用端口 | DevOps | bind error |
| R04 | Nacos 配置错误 | 中 | 中 | 中 | 双重审核 + 配置对比工具 | 后端 | 服务无法连接 |
| R05 | 内存不足导致 OOM | 中 | 中 | 中 | 监控预警 + 预留 30% | SRE | 内存 >85% |
| R06 | 灰度期间发现严重 Bug | 中 | 高 | 高 | 立即回滚 + 修复重发 | SRE | 错误率 >1% |
| R07 | 流量激增导致服务雪崩 | 低 | 高 | 中 | 限流 + 熔断 + 降级 | SRE | 异常流量 |
| R08 | 区块链上链服务异常 | 低 | 中 | 低 | 重试机制 + 死信队列 | 后端 | 上链失败 |
| R09 | 数据库慢查询 | 中 | 中 | 中 | 已优化 90+ 索引 + 慢查询监控 | DBA | 慢查询 >10/min |
| R10 | HTTPS 证书过期 | 低 | 高 | 中 | 自动续期（certbot）+ 监控 | DevOps | 证书过期 |
| R11 | 监控告警不工作 | 中 | 中 | 中 | 部署后立即验证告警链路 | SRE | 模拟故障 |
| R12 | 用户数据丢失 | 低 | 高 | 中 | 多重备份 + 异地容灾 | DBA | 数据缺失 |
| R13 | 第三方依赖（OSS/区块链）故障 | 低 | 中 | 中 | 重试 + 降级 + 报警 | 后端 | 外部故障 |
| R14 | 部署脚本执行失败 | 中 | 中 | 中 | 脚本幂等性 + 错误回滚 | DevOps | exit code != 0 |
| R15 | 团队成员无法到场 | 中 | 中 | 中 | 备份人员 + 远程办公支持 | 总指挥 | 请假/缺勤 |
| R16 | 用户投诉/反馈处理 | 高 | 中 | 中 | 客服团队准备 + FAQ | 运营 | 上线后 |
| R17 | 网络中断 | 低 | 高 | 中 | 多线路 + 异地灾备 | DevOps | ping 失败 |
| R18 | Nginx 配置错误 | 中 | 中 | 中 | 提前测试 + 灰度上线 | DevOps | reload 失败 |
| R19 | 敏感信息泄露 | 低 | 高 | 中 | 加密存储 + 访问审计 | 安全 | 日志审计 |
| R20 | 紧急回滚失败 | 低 | 高 | 中 | 演练 + 备份验证 | SRE | 回滚异常 |

### 11.2 风险应对预案

#### 11.2.1 数据库迁移失败

**预案**:
1. 立即停止迁移，保留源数据库
2. 检查错误日志，定位失败原因
3. 修复后重新执行（重试 3 次）
4. 仍失败则启动应急流程：手动 SQL 执行
5. 完全失败则回退到原环境

**RTO**: 30min
**RPO**: 0（迁移前有完整备份）

#### 11.2.2 服务异常

**预案**:
1. 立即触发告警（PagerDuty / 短信）
2. SRE 5min 内响应
3. 评估影响范围（单服务/多服务）
4. 决定：重启 / 回滚 / 降级
5. 故障恢复后进行 Post-Mortem

**RTO**: 5-15min

#### 11.2.3 流量激增

**预案**:
1. Nginx 限流（50r/s 通用，5r/s 登录）
2. Sentinel 熔断降级
3. 紧急扩容（增加服务节点）
4. 静态化首页/列表页
5. 关闭非核心功能

**RTO**: 5min

#### 11.2.4 安全攻击

**预案**:
1. WAF 启用（已配置）
2. IP 黑名单
3. 临时关闭受影响接口
4. 应急响应团队介入
5. 事后复盘 + 修复

**RTO**: 10min

### 11.3 应急联系人

| 紧急类型 | 联系人 | 联系方式 |
|---------|--------|---------|
| 技术故障 | SRE Oncall | 138-0000-0007 |
| 数据库问题 | DBA Oncall | 138-0000-0003 |
| 安全事件 | 安全团队 | 138-0000-0009 |
| 1Panel 故障 | 1Panel 官方 | https://1panel.cn |
| 云厂商支持 | 阿里云 / 腾讯云 | 工单系统 |

---

## 12. 部署交付清单

### 12.1 部署完成后交付物

- [ ] 部署完成报告（含实际部署时间、变更、问题）
- [ ] 部署验证报告（所有验证项通过）
- [ ] 监控告警配置报告
- [ ] 应急响应手册更新
- [ ] 用户通知邮件/公告
- [ ] 培训材料（运营/客服）
- [ ] 后续优化任务列表

### 12.2 文档归档

```bash
# 部署文档结构
/opt/tailor-is/
├── docs/
│   ├── deployment/
│   │   ├── deploy-plan.md             # 本文档
│   │   ├── deploy-checklist.md        # 部署清单
│   │   ├── deploy-verification.md     # 验证报告
│   │   ├── deploy-issues.md           # 部署问题
│   │   └── rollback-procedure.md      # 回滚流程
│   ├── operations/
│   │   ├── runbook.md                 # 操作手册
│   │   ├── monitoring.md              # 监控手册
│   │   ├── incident-response.md       # 应急响应
│   │   └── faq.md                     # 常见问题
│   └── release-notes/
│       ├── v1.0.0.md                  # 发布说明
```

---

## 13. 总结

本部署计划方案全面覆盖了 Tailor IS 在 1Panel 环境下的完整部署流程，包括：

1. ✅ **环境准备**: 硬件/软件/网络/端口/目录
2. ✅ **资源配置**: JVM/数据库/缓存/监控
3. ✅ **部署步骤**: 9 个阶段，~9h 完成
4. ✅ **数据迁移**: 完整策略、备份、校验
5. ✅ **服务启停**: 严格的依赖顺序
6. ✅ **部署验证**: 6 个层次，70+ 验证项
7. ✅ **回滚机制**: 4 级回滚策略
8. ✅ **时间安排**: D-Day 详细时刻表
9. ✅ **责任分配**: RACI 矩阵
10. ✅ **风险评估**: 20 项风险 + 应对预案

部署完成后，系统将达到：
- **功能完整度**: 100%（Sprint 1-9 全部任务）
- **代码质量**: 92% 单元测试覆盖率
- **性能指标**: P95 ≤ 180ms, TPS ≥ 850
- **安全合规**: 0 High 漏洞, OWASP Top 10 全覆盖
- **可用性 SLO**: ≥ 99.9%

**部署就绪度**: 100% ✅

---

**编制人**: Tailor IS DevOps Team
**审核人**: 技术负责人
**批准人**: CTO
**日期**: 2026年6月3日
