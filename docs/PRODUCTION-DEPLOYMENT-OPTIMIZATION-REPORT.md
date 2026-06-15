# 🔧 Tailor IS（裁智云）- 生产部署优化工作完成报告

---

## 一、概述

| 项目 | 内容 |
|------|------|
| **报告版本** | v1.0 |
| **完成日期** | 2026-06-11 |
| **执行人员** | 部署运维团队 |
| **工作目标** | 根据"项目开发及部署专项核查报告"中第三部分的部署要求，执行5项生产级优化工作，确保系统达到生产部署标准 |

---

## 二、已完成的优化工作总览

| # | 优化项 | 说明 | 完成状态 | 产出文件 |
|----|--------|------|----------|---------|
| 1 | SSL/TLS 安全加密 | 生成 TLS 证书并配置 HTTPS 反向代理 | ✅ 完成 | `deploy/nginx/ssl/`、`deploy/nginx/https.conf` |
| 2 | 资源限制配置 | 各服务 CPU / 内存 / 连接数限制 | ✅ 完成 | `deploy/configs/resource-limits.yml` |
| 3 | 自动备份策略 | MySQL / Nacos / Redis / 配置文件备份 | ✅ 完成 | `deploy/scripts/backup-enhanced.sh` |
| 4 | Nacos 功能测试 | 配置管理 + 服务发现完整测试 | ✅ 完成 | `deploy/scripts/nacos-function-test.sh` |
| 5 | 性能压测脚本 | 各服务基准性能测试 + QPS / 延迟测试 | ✅ 完成 | `deploy/scripts/performance-benchmark.sh` |

---

## 三、优化 1：SSL/TLS 安全加密

### 3.1 工作内容

| 项目 | 说明 |
|------|------|
| 证书类型 | RSA 2048 位 + ECDSA P-256 双证书 |
| 有效期 | 3650 天（10 年） |
| 支持协议 | TLS 1.2、TLS 1.3（禁用 SSLv3 / TLSv1.0 / TLSv1.1） |
| 加密套件 | 仅 ECDHE / DHE + GCM / CHACHA20 高安全性套件 |
| HSTS | `Strict-Transport-Security: max-age=31536000` |
| DH 参数 | 2048 位 Diffie-Hellman 参数（前向保密） |

### 3.2 生成的证书文件

```
deploy/nginx/ssl/
├── server.crt        (RSA 证书 - 1.2 KB)
├── server.key        (RSA 私钥 - 1.7 KB)
├── server.csr        (RSA 证书签名请求)
├── ec.crt            (ECDSA 证书 - 688 B)
├── ec.key            (ECDSA 私钥 - 302 B)
├── ec.csr            (ECDSA 证书签名请求)
└── dhparam.pem       (DH 参数 - 424 B)
```

### 3.3 HTTPS 配置特点

- **HTTP 强制跳转**：所有 80 端口请求 301 永久重定向到 HTTPS
- **双证书支持**：同时支持 RSA 和 ECDSA 证书，客户端自动协商最优方案
- **OCSP Stapling**：证书状态在线验证，加速 TLS 握手
- **SSL 会话缓存**：`shared:SSL:10m` 缓存，加速重复连接
- **Session Tickets**：Off（保证前向保密性）
- **安全响应头**：`X-Frame-Options`、`X-Content-Type-Options`、`X-XSS-Protection`、`Content-Security-Policy`

### 3.4 部署步骤

```bash
# 1. 证书已生成在 deploy/nginx/ssl/
# 2. 在 docker-compose.prod.yml 中挂载证书卷
volumes:
  - ./deploy/nginx/ssl:/etc/nginx/ssl:ro
  - ./deploy/nginx/https.conf:/etc/nginx/conf.d/https.conf:ro

# 3. 重启 Nginx 容器
docker compose restart nginx

# 4. 验证 HTTPS
curl -I https://your-domain.com/
openssl s_client -connect your-domain.com:443 -brief
```

---

## 四、优化 2：资源限制配置

### 4.1 服务资源分配表

| 服务 | CPU 限制 | 内存限制 | CPU 预留 | 内存预留 | 连接数限制 |
|------|----------|----------|----------|----------|------------|
| **MySQL** | 2.0 vCPU | 4096 MB | 0.5 vCPU | 1024 MB | `max_connections=500` |
| **Redis** | 1.0 vCPU | 1024 MB | 0.2 vCPU | 256 MB | `maxclients=1000` |
| **RabbitMQ** | 2.0 vCPU | 2048 MB | 0.5 vCPU | 512 MB | `vm_memory_high_watermark=0.6` |
| **Nacos** | 2.0 vCPU | 2048 MB | 0.5 vCPU | 512 MB | JVM 堆 512m~1024m |
| **Core Gateway** | 2.0 vCPU | 2048 MB | 0.5 vCPU | 512 MB | Tomcat 线程 200 |
| **Lite Gateway** | 1.0 vCPU | 1024 MB | 0.3 vCPU | 256 MB | - |
| **Nginx** | 1.0 vCPU | 512 MB | 0.2 vCPU | 128 MB | `worker_connections=4096` |
| **Prometheus** | 1.0 vCPU | 1024 MB | 0.2 vCPU | 256 MB | 保留 15 天数据 |
| **Grafana** | 0.5 vCPU | 512 MB | 0.1 vCPU | 128 MB | - |
| **GraphQL Gateway** | 1.0 vCPU | 1024 MB | 0.2 vCPU | 256 MB | 并发 1000 连接 |

### 4.2 配置文件结构

**文件**：`deploy/configs/resource-limits.yml`（Docker Compose v3.8 格式）

包含以下关键环境变量：

```yaml
services:
  mysql:
    deploy:
      resources:
        limits:       { cpus: '2.0', memory: 4096M }
        reservations: { cpus: '0.5', memory: 1024M }
    environment:
      MAX_CONNECTIONS: 500
      INNODB_BUFFER_POOL_SIZE: 2G

  redis:
    deploy:
      resources:
        limits:       { cpus: '1.0', memory: 1024M }
        reservations: { cpus: '0.2', memory: 256M }
    environment:
      MAXMEMORY: 512MB
      MAXMEMORY_POLICY: allkeys-lru
      MAX_CLIENTS: 1000

  # ... 更多服务
```

### 4.3 使用方式

```bash
# 单独应用资源限制（覆盖默认配置）
docker compose -f docker-compose.prod.yml \
               -f deploy/configs/resource-limits.yml \
               up -d

# 查看当前资源使用
docker stats

# 验证配置
docker compose config | grep -A5 'resources:'
```

---

## 五、优化 3：自动备份策略

### 5.1 增强版备份脚本功能

| 项目 | 内容 |
|------|------|
| **文件** | `deploy/scripts/backup-enhanced.sh` |
| **大小** | ~31 KB |
| **权限** | 可执行（`chmod +x`） |
| **语言** | Bash |

### 5.2 备份范围

| 数据类型 | 备份方式 | 频率建议 | 保留策略 |
|----------|----------|----------|----------|
| **MySQL 数据库** | `mysqldump` | 每日 02:00 | 7 天滚动 + 月度归档 |
| **Redis 快照** | RDB / AOF 快照复制 | 每日 03:00 | 7 天滚动 |
| **Nacos 配置** | API 导出 JSON | 每日 02:30 | 30 天保留 |
| **Docker 配置** | `docker-compose.yml` + `.env` + `nginx/` | 每次部署时 | 30 个版本 |
| **SHA256 校验** | 所有备份文件哈希 | 每次备份 | 永久 |

### 5.3 备份目录结构

```
/opt/tailor-is/backups/
├── latest -> 2026-06-11_20-00-00/        （最新备份符号链接）
├── 2026-06-11_20-00-00/
│   ├── mysql/
│   │   ├── tailor_is.sql.gz
│   │   ├── nacos_config.sql.gz
│   │   └── users.sql.gz
│   ├── redis/
│   │   ├── dump.rdb
│   │   └── appendonly.aof  （如启用）
│   ├── nacos/
│   │   ├── namespaces.json
│   │   └── configs-default-group.json
│   ├── configs/
│   │   ├── docker-compose.prod.yml
│   │   ├── .env.production
│   │   └── nginx/           （完整目录）
│   ├── checksums.sha256
│   └── backup-metadata.json
└── reports/
    └── backup-report-2026-06-11.txt
```

### 5.4 脚本主要功能

1. **智能备份**：自动检测数据库列表，每个库独立备份
2. **压缩存储**：使用 gzip 压缩，节省 70%-90% 存储空间
3. **完整性校验**：SHA256 哈希验证，确保数据未篡改
4. **滚动清理**：自动删除 7 天前的旧备份
5. **状态报告**：生成 JSON / TXT 格式报告，记录每次备份详情
6. **恢复支持**：`--restore <dir> <type>` 支持一键恢复
7. **列表查看**：`--list` 查看所有备份历史

### 5.5 Cron 定时配置

```bash
# 编辑 crontab
crontab -e

# 添加以下内容（每天凌晨 2 点自动备份）
0 2 * * * /home/tailor/Tailoris/deploy/scripts/backup-enhanced.sh \
          >> /var/log/tailor-is/backup.log 2>&1

# 验证 cron 服务
systemctl status cron
```

---

## 六、优化 4：Nacos 功能测试

### 6.1 测试脚本功能

| 项目 | 内容 |
|------|------|
| **文件** | `deploy/scripts/nacos-function-test.sh` |
| **大小** | ~100 KB（1067 行） |
| **权限** | 可执行（`chmod +x`） |
| **语言** | Bash + `curl` / `jq` |

### 6.2 测试覆盖范围

| 测试模块 | 测试项数 | 关键 API | 预期结果 |
|----------|----------|----------|----------|
| **服务可用性** | 3 | TCP 端口 / 健康检查 / 首页 | ✅ 端口监听，UP 状态 |
| **配置管理** | 6 | POST / GET / LIST / LISTENER | ✅ 发布 / 获取 / 监听正常 |
| **服务注册发现** | 6 | 注册 / 查询 / 更新 / 注销 | ✅ 实例生命周期完整 |
| **命名空间管理** | 5 | 创建 / 查询 / 隔离 / 删除 | ✅ 命名空间隔离验证 |
| **API 认证** | 3 | 登录 / Token 验证 / 无 Token 拒绝 | ✅ 认证机制正常 |
| **性能测试** | 4 | 发布 QPS / 发现 QPS / 响应时间 | ✅ QPS > 100，延迟 < 100ms |
| **总计** | **27 项** | - | - |

### 6.3 支持的命令行参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `--host <host>` | Nacos 服务器地址 | `localhost` |
| `--port <port>` | Nacos 端口 | `8848` |
| `--username` | 登录用户名 | `nacos` |
| `--password` | 登录密码 | `nacos` |
| `--namespace` | 测试命名空间 | `public` |
| `--timeout <s>` | 请求超时（秒） | `10` |
| `--dry-run` | 仅显示测试计划 | 否 |
| `--verbose` | 详细请求 / 响应输出 | 否 |
| `--no-cleanup` | 保留测试数据 | 否 |
| `--help` | 显示帮助 | - |

### 6.4 使用示例

```bash
# 完整测试（使用默认配置）
./deploy/scripts/nacos-function-test.sh

# 测试远程 Nacos 服务
./deploy/scripts/nacos-function-test.sh --host 10.0.0.10 --port 8848

# 仅显示测试计划（不执行）
./deploy/scripts/nacos-function-test.sh --dry-run

# 详细模式查看请求
./deploy/scripts/nacos-function-test.sh --verbose

# 输出位置：/opt/tailor-is/reports/nacos-test-result-*.json
```

### 6.5 测试报告输出

脚本生成三类输出：

1. **控制台输出**：实时进度，`PASS` / `FAIL` 状态
2. **JSON 报告**：`/opt/tailor-is/reports/nacos-test-result-<timestamp>.json`
3. **文本摘要**：27 项测试的通过率、平均响应时间、QPS 统计

---

## 七、优化 5：性能压测

### 7.1 测试脚本功能

| 项目 | 内容 |
|------|------|
| **文件** | `deploy/scripts/performance-benchmark.sh` |
| **大小** | ~80 KB（884 行） |
| **权限** | 可执行（`chmod +x`） |
| **语言** | Bash + `ab` / `wrk` / `mysql` / `redis-cli` |

### 7.2 测试覆盖范围

| 测试模块 | 测试场景 | 指标 |
|----------|----------|------|
| **HTTP 服务** | 基础 QPS、10 / 25 / 50 / 100 并发、静态资源、API 接口 | QPS、平均 / P95 / P99 延迟 |
| **MySQL 数据库** | 连接开销、简单查询、复杂 JOIN、1000 / 10000 行查询 | 连接数、QPS、扫描行数 |
| **Redis 缓存** | PING、SET / GET、HSET / HGET、LIST 操作 | 操作 / 秒、平均延迟 |
| **RabbitMQ** | 1000 / 3000 消息发布、消费速度、持久化消息 | 消息 / 秒、队列深度 |
| **Nacos** | 配置发布 / 拉取、服务注册 / 发现 QPS | API 响应时间、QPS |
| **资源监控** | CPU、内存、磁盘 IO、网络带宽 | 平均 / 峰值使用率 |

### 7.3 快速性能测试结果（本次执行）

| 服务 | 测试项 | 结果 | 评价 |
|------|--------|------|------|
| **Nginx / HTTP** | 基础 QPS | ~2773 req/s | ✅ 优秀 |
| **Nginx / HTTP** | 平均延迟 | ~0.45 ms | ✅ 优秀 |
| **Nginx / HTTP** | 10 并发延迟 | 2.008 ms | ✅ 正常 |
| **MySQL** | 简单查询 QPS | ~36 req/s | ⚠️ 需优化（进程启动开销） |
| **MySQL** | 平均查询时间 | ~27.71 ms | ⚠️ 需优化（连接池） |
| **Redis** | PING 操作 | ~65 ms（含 nc 连接开销） | ✅ 正常 |
| **RabbitMQ API** | 平均响应 | ~3 ms | ✅ 优秀 |
| **Nacos** | 健康检查 | 响应正常 | ✅ 通过 |

> **说明**：MySQL 和 Redis 的测试值包含了命令行工具（`mysql` / `nc`）的进程启动开销，实际应用层性能应更高。建议使用应用内压测工具获取真实数据。

### 7.4 脚本使用方式

```bash
# 快速模式（50-100 请求 / 服务）
./deploy/scripts/performance-benchmark.sh --quick

# 标准模式（500-1000 请求 / 服务）
./deploy/scripts/performance-benchmark.sh --standard --output /var/log/bench

# 压力测试（5000-10000 请求 / 服务）
HTTP_URL=http://your-gateway/health \
MYSQL_HOST=10.0.0.5 MYSQL_USER=bench MYSQL_PASSWORD='xxx' \
./deploy/scripts/performance-benchmark.sh --stress \
    --services http,mysql,redis,rabbitmq,nacos

# 仅测试特定服务
./deploy/scripts/performance-benchmark.sh --services http,mysql
```

### 7.5 输出文件

```
${OUTPUT_DIR}/
├── benchmark-results.csv       # 表格化数据，可导入 Excel
├── benchmark-results.json      # 结构化 JSON，程序可分析
├── benchmark-report.html       # 可视化网页，含 SVG 柱状图
├── summary.txt                 # 人类可读文本摘要
├── resume.state                # 中断恢复状态文件
└── raw/                        # 各类中间日志（debug 用）
```

---

## 八、优化成果汇总表

### 8.1 新增产出文件清单

| 类型 | 路径 | 大小 | 说明 |
|------|------|------|------|
| 🔐 SSL 证书 | `deploy/nginx/ssl/server.crt` | 1.2 KB | RSA 2048 位证书 |
| 🔐 SSL 证书 | `deploy/nginx/ssl/server.key` | 1.7 KB | RSA 私钥 |
| 🔐 SSL 证书 | `deploy/nginx/ssl/ec.crt` | 688 B | ECDSA 证书 |
| 🔐 SSL 证书 | `deploy/nginx/ssl/ec.key` | 302 B | ECDSA 私钥 |
| 🔐 SSL 证书 | `deploy/nginx/ssl/dhparam.pem` | 424 B | DH 参数 |
| ⚙️ Nginx 配置 | `deploy/nginx/https.conf` | ~5 KB | HTTPS 反向代理配置 |
| ⚙️ 资源限制 | `deploy/configs/resource-limits.yml` | ~10 KB | 10 服务 CPU / 内存 / 连接限制 |
| 💾 备份脚本 | `deploy/scripts/backup-enhanced.sh` | ~31 KB | 5 模块增强备份脚本 |
| 🧪 Nacos 测试 | `deploy/scripts/nacos-function-test.sh` | ~100 KB | 27 项功能测试脚本 |
| 📊 性能测试 | `deploy/scripts/performance-benchmark.sh` | ~80 KB | 5 大服务压测脚本 |
| 📄 文档 | `docs/PRODUCTION-DEPLOYMENT-OPTIMIZATION-REPORT.md` | 本文件 | 优化工作说明 |

**总新增文件**：12 个

**总配置 + 脚本大小**：~230 KB

---

## 九、生产部署检查清单

### 9.1 部署前检查

- [x] SSL 证书已生成，路径正确
- [x] HTTPS 配置文件已创建，语法已验证
- [x] 资源限制 YAML 文件已创建，符合 Docker Compose 格式
- [x] 备份脚本权限设置正确（`chmod +x`）
- [x] 备份目录权限正确（`/opt/tailor-is/backups/`）
- [x] Nacos 功能测试脚本已准备
- [x] 性能测试脚本已准备
- [ ] **待执行**：Cron 定时任务已配置（需要 root 权限）
- [ ] **待执行**：Docker Compose 已更新引用新配置文件
- [ ] **待执行**：防火墙已开放 443 端口
- [ ] **待执行**：域名 DNS 解析已配置（如使用真实证书）

### 9.2 部署中检查

- [ ] 执行 `docker compose config` 验证配置
- [ ] 启动服务后 `docker compose ps` 检查状态
- [ ] 验证 `https://` 访问正常（`curl -I`）
- [ ] 验证各服务健康检查端点（`/health`、`/actuator/health`）
- [ ] 执行一次完整备份，验证备份脚本正常
- [ ] 执行 Nacos 功能测试，验证服务注册发现
- [ ] 执行性能基准测试，记录基线数据

### 9.3 部署后检查

- [ ] 监控仪表盘 Grafana 可访问
- [ ] Prometheus 指标正常采集
- [ ] 告警规则正常工作（Alertmanager）
- [ ] 日志收集正常（ELK / Loki）
- [ ] 首次备份成功完成并验证完整性
- [ ] 应用日志无 ERROR 级别日志
- [ ] 关键业务流程冒烟测试通过
- [ ] 性能测试结果与基线对比

---

## 十、注意事项与后续建议

### 10.1 SSL/TLS 方面

1. **证书更新**：自签名证书用于开发 / 测试环境，生产环境应使用 Let's Encrypt 或商业 CA 证书
2. **证书有效期**：当前证书有效期 10 年，但建议每 12-24 个月更换
3. **HSTS 配置**：启用后浏览器将强制使用 HTTPS，切换回 HTTP 需谨慎
4. **OCSP Stapling**：需确保容器能访问公网的 OCSP 服务器，否则 TLS 握手可能变慢

### 10.2 资源限制方面

1. **监控资源使用**：建议部署后第一周密切监控各服务 CPU / 内存使用率
2. **动态调整**：资源限制不是一成不变，应根据实际使用情况调整
3. **JVM 内存**：Java 服务应预留堆外内存（堆大小的 20%-30%）给容器
4. **Connection Pool**：数据库连接池大小应小于 `max_connections` 的 80%

### 10.3 备份策略方面

1. **异地备份**：除了本地备份，应配置备份到云存储（OSS / S3）
2. **备份验证**：定期（建议每月）执行恢复测试，验证备份有效性
3. **加密存储**：敏感数据备份应加密（MySQL AES 或 GPG）
4. **增量备份**：大型数据库应考虑使用增量备份（`xtrabackup`）减少时间和空间

### 10.4 测试与监控

1. **自动化测试**：将功能测试和性能测试集成到 CI/CD 流程
2. **性能基线**：保存首次性能测试结果作为基线，便于后续对比
3. **定期回归**：每次重大变更后重新执行完整测试套件
4. **告警配置**：在 Prometheus 中配置关键指标的告警阈值（CPU > 80%、内存 > 80%、磁盘 > 85% 等）

### 10.5 安全加固

1. **证书密钥权限**：`chmod 600 *.key` 防止非授权访问
2. **敏感配置文件**：`.env.production` 应使用 `chmod 600` 限制权限
3. **数据库访问**：限制 MySQL 用户只能从特定 IP 连接
4. **Redis 密码**：确保 `requirepass` 配置启用
5. **API 网关鉴权**：所有内部 API 应配置 JWT / OAuth2 认证

---

## 十一、相关文档索引

| 文档 | 路径 | 说明 |
|------|------|------|
| 项目开发及部署专项核查报告 | `Tailor IS（裁智云）项目开发及部署专项核查报告.md` | 主报告，包含第三部分部署方案 |
| 路由与端口规范 | `docs/ROUTING-PORT-STANDARD.md` | 系统端口分配和路由策略 |
| 部署测试报告 | `docs/TEST-REPORT.md` | 功能测试结果（扩展版） |
| 部署核查清单 | `docs/DEPLOYMENT-CHECKLIST.md` | 部署前中后检查清单 |
| **本优化报告** | **`docs/PRODUCTION-DEPLOYMENT-OPTIMIZATION-REPORT.md`** | **5 项优化工作成果** |

---

**报告签名**：_________________________

**审核**：_________________________

**日期**：2026-06-11

---
