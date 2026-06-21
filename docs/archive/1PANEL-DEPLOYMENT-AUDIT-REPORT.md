# Tailor IS — 1Panel 面板部署未执行 · 完整排查报告

**日期:** 2026-06-12
**排查目标:** 分析为何"应用未按计划部署到已安装的 1Panel 面板中"
**执行用户:** `tailor` (非 root / 不在 docker 组)

---

## 一、执行摘要

**核心结论:** 1Panel 面板本身、已安装的 MySQL/Redis/RabbitMQ/Nacos/OpenResty **服务均已就绪**，但我们的部署工程（`deploy.sh` + `docker-compose.prod.yml` + `.env.production`）**并未实际执行 1Panel 面板集成部署**，存在 5 类结构性问题导致部署未发生：

| # | 问题 | 严重度 | 影响范围 |
|---|------|--------|----------|
| P-01 | 部署脚本 `deploy.sh` 从未被执行（要求 sudo，当前用户无 docker 权限） | 高 | 全流程 |
| P-02 | `docker-compose.prod.yml` 试图**新建** MySQL/Redis 等容器，但端口 3306/6379/5672/15672/8848 已被 1Panel 容器占用 → `docker compose up` 必挂 | 高 | 基础设施服务 |
| P-03 | `.env.production` 仍使用 **Docker 内网 IP（172.18.0.*）** 和 **占位符 `<...>` 密码**，且凭据与 1Panel 实际分配的不一致 | 高 | 应用连库/连缓存/连 MQ/Nacos |
| P-04 | 部署流程**未将 1Panel 作为基础设施来源**，却试图自己再拉起一套 MySQL/Redis/Nacos（与 1Panel 已安装的重复） | 高 | 架构/资源 |
| P-05 | `PANEL_PASSWORD` 未填 + 1Panel API 接入未验证（部署无法自动上板管理） | 中 | 面板集成 |

**已验证端口/TCP 连通性（✅ 全部正常）:**

| 服务 | 端口 | 连通性 | HTTP 探测 |
|------|------|--------|----------|
| 1Panel 面板 | 11336 | ✅ | HTTP 200（/632ae1b167） |
| MySQL | 3306 | ✅ | —（TCP） |
| Redis | 6379 | ✅ | —（TCP） |
| RabbitMQ | 5672/15672 | ✅ | 15672 HTTP 200 |
| Nacos | 8081/8848/9848 | ✅ | 8081 HTTP 302 · 8848 API 正常 |
| OpenResty | 80/443 | ✅ | 80 HTTP 200 |

---

## 二、详细排查记录

### 2.1 1Panel 面板自身状态

```
1Panel 核心进程（root 拥有）:
   /usr/bin/1panel-agent       (1747)
   /usr/bin/1panel-core        (1748)
1Panel 应用目录:
   /opt/1panel/apps/{mysql,redis,rabbitmq,nacos,openresty}/   ✅ 均存在
1Panel 面板本地访问:
   http://127.0.0.1:11336/632ae1b167  =>  HTTP 200  ✅
```

**问题:**
- `systemctl status 1panel` 报错 *Unit 1panel.service could not be found*（1Panel v2 不再以 systemd 托管自身服务，但已以裸进程启动）。
- 当前用户 `tailor` 不在 docker 组，也无 sudo，`docker ps / docker info` 均返回：
  > `permission denied while trying to connect to docker API at unix:///var/run/docker.sock`
- 部署脚本 `deploy.sh` 第一行 `check_root()` 要求 `id -u == 0`，非 root 直接 `exit 1`。

### 2.2 端口冲突分析（P-02 的根因）

部署工程的 `docker-compose.prod.yml` 试图以下列端口新建容器：
- `mysql:3306`、`redis:6379`、`rabbitmq:5672/15672`、`nacos:8848/9848/8081`

但 TCP 端口探测证明这些端口**已被 1Panel 同名服务占用**：

```
✅ 127.0.0.1:3306  TCP OPEN
✅ 127.0.0.1:6379  TCP OPEN
✅ 127.0.0.1:5672  TCP OPEN
✅ 127.0.0.1:15672 TCP OPEN
✅ 127.0.0.1:8848  TCP OPEN
✅ 127.0.0.1:8081  TCP OPEN
✅ 127.0.0.1:9848  TCP OPEN
✅ 127.0.0.1:80    TCP OPEN
✅ 127.0.0.1:443   TCP OPEN
```

若强行执行 `docker compose up -d`，必然得到：
```
ERROR: for tailor-is-mysql  
  Cannot start service mysql: driver failed programming external connectivity on endpoint  
  Bind for 0.0.0.0:3306 failed: port is already allocated
```

### 2.3 配置文件错配（P-03 详细）

`deploy/.env.production` 与实际 1Panel 分配的凭据对比：

| 项 | deploy/.env.production | 1Panel 实际值（用户提供） | 是否匹配 |
|----|------------------------|--------------------------|---------|
| **MySQL host** | `172.18.0.2` （Docker 内网） | `127.0.0.1` (宿主端口映射) | ❌ |
| **MySQL 端口** | `3306` | `3306` | ✅ |
| **MySQL 密码** | `<生产环境请设置强密码>` 占位符 | `mysql_ZmY2sr` | ❌ |
| **Redis host** | `172.18.0.4` | `127.0.0.1` | ❌ |
| **Redis 端口** | `6379` | `6379` | ✅ |
| **Redis 密码** | `<生产环境请设置密码>` 占位符 | `redis_jD2N8n` | ❌ |
| **RabbitMQ host** | `172.18.0.3` | `127.0.0.1` | ❌ |
| **RabbitMQ 账号** | `tailor_is` | `rabbitmq` | ❌ |
| **RabbitMQ 密码** | `<生产环境请设置密码>` 占位符 | `rabbitmq` | ❌ |
| **Nacos host** | `172.19.0.2` | `127.0.0.1` | ❌ |
| **Nacos 端口** | `8848` | `8848` | ✅ |
| **Nacos 密码** | 空（默认 `nacos`） | 需在 1Panel 确认，Nacos server-identity/Token 已匹配 | ⚠️ 待核验 |
| **Nacos identity key** | `serverIdentity` | `serverIdentity` | ✅ |
| **Nacos identity value** | `security` | `security` | ✅ |
| **Nacos auth token** | `SecretKey0123…（64 位）` | `SecretKey0123…（64 位）` | ✅ |
| **PANEL_PASSWORD** | `<生产环境请通过1Panel面板配置>` 占位符 | `a90291cfd8` | ❌ |

**另一类问题:** 文件在部署前为 **Windows CRLF** 换行（此前已在告警测试中发现并修复为 LF），导致 `$RESEND_API_KEY`、`$MYSQL_PASSWORD` 等值末尾可能带 `\r`，被 HTTP/TCP 协议客户端视为非法字符，尤其影响 Resend 邮件 API 的 Authorization 头。

### 2.4 docker-compose.prod.yml 的结构性问题（P-04）

- **基础设施容器与 1Panel 重复定义：** compose 文件在 `services` 中显式声明了 `mysql` / `redis` / `rabbitmq` / `nacos`，意图自己拉起一套。正确做法应改为 `external_links` 或直接将服务 host 设为 `127.0.0.1` / `host.docker.internal`，让业务容器接入 1Panel 已存在的基础设施。
- **compose 内部使用 `SPRING_DATA_REDIS_HOST: redis` 等服务名 DNS 解析**，在 1Panel 独立网络下完全不可用。
- **1Panel 应用容器实际使用的是独立网络**（1Panel 自有 bridge，通常命名为 `1panel_default` 或 `1Panel-net`），而 compose 计划使用 `tailor-is-network (172.18.0.0/24)`、`monitor-network`，两套网络不通。

### 2.5 部署脚本未执行（P-01 复现）

`deploy/scripts/deploy.sh` 的入口：
```bash
check_root() {
    if [ "$(id -u)" -ne 0 ]; then
        log_error "请使用 root 或 sudo 运行此脚本"
        exit 1
    fi
}
# main(): check_root → check_docker → check_env_file → check_ports → ...
```
当前 `id -u = 1000 (tailor)`，首次 `check_root` 直接失败，流程未进入任何一步。

### 2.6 1Panel API 面板登录凭据

用户提供的 `986a2e2333 / a90291cfd8` 已写入 `.env.production` 的前 15 行注释块，但 `PANEL_PASSWORD` 仍为占位符，**部署工程既未读取也未上传到 1Panel API**，导致面板中无"Tailor IS"应用记录。

---

## 三、修复建议（优先级排序）

### 🔴 高优先级（部署前置依赖）

**F-01 · 将当前用户加入 docker 组 & 建立部署执行身份**
```bash
# 以 root 执行
sudo usermod -aG docker tailor
newgrp docker
# 验证
docker ps          # 应看到 1Panel-mysql / 1Panel-redis 等容器
```

**F-02 · 重构部署策略：以 1Panel 作为基础设施来源**
- 删除 `docker-compose.prod.yml` 中重复的 `mysql / redis / rabbitmq / nacos` 定义（保留业务微服务与监控）。
- 将业务容器 `network_mode: bridge` 或挂到 `1panel_default` 网络（需 `docker network ls` 确认名称），或用 `extra_hosts` 将服务名映射到 `127.0.0.1`。
- 更稳妥的做法：将所有 `*_HOST` 改为 `127.0.0.1`，业务服务 `network_mode: host`（或用 `host.docker.internal`），直接接入 1Panel 的已映射端口。

**F-03 · 用 1Panel 真实凭据重写 `.env.production`**
```dotenv
# MySQL（1Panel）
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_USERNAME=root                         # 或 1Panel 创建的专用账户
MYSQL_PASSWORD=mysql_ZmY2sr
MYSQL_ROOT_PASSWORD=mysql_ZmY2sr

# Redis（1Panel）
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=redis_jD2N8n

# RabbitMQ（1Panel）
RABBITMQ_HOST=127.0.0.1
RABBITMQ_PORT=5672
RABBITMQ_DASHBOARD_PORT=15672
RABBITMQ_USERNAME=rabbitmq
RABBITMQ_PASSWORD=rabbitmq
RABBITMQ_VHOST=/                             # 1Panel 默认不建 /tailor-is，若业务需要需提前创建

# Nacos（1Panel）
NACOS_ADDR=127.0.0.1:8848
NACOS_GRPC_PORT=9848
NACOS_USERNAME=nacos
NACOS_PASSWORD=（到 1Panel Nacos 控制台确认）
NACOS_NAMESPACE=prod
NACOS_SERVER_IDENTITY_KEY=serverIdentity
NACOS_SERVER_IDENTITY_VALUE=security
NACOS_AUTH_TOKEN=SecretKey012345678901234567890123456789012345678901234567890123456789

# 1Panel 面板（用于自动化上板）
PANEL_URL=http://127.0.0.1:11336/632ae1b167
PANEL_USER=986a2e2333
PANEL_PASSWORD=a90291cfd8
```

**F-04 · 文件强制 Unix LF + 600 权限**
```bash
sed -i 's/\r$//' deploy/.env.production
chmod 600 deploy/.env.production
file deploy/.env.production     # 应输出 "ASCII text"（而非 "with CRLF line terminators"）
```

### 🟡 中优先级

**F-05 · 在 1Panel 控制台为业务创建专用 MySQL 账户（不要用 root）**
- 数据库名：`tailor_is`
- 账户：`tailor_is_app@'%'`（限内部 IP，如 `172.18.0.%` 或 `127.0.0.1`）
- 权限：`SELECT,INSERT,UPDATE,DELETE,CREATE,DROP,INDEX,ALTER`
- 在 `.env.production` 中更新 `MYSQL_USERNAME / MYSQL_PASSWORD` 为专用账户。

**F-06 · 在 1Panel RabbitMQ 建业务 vhost `/tailor-is`**
- 控制台路径：1Panel → RabbitMQ 管理（15672）→ Admin → Virtual Hosts → Add a new virtual host → `/tailor-is`
- 授予 `rabbitmq` 该 vhost 的 `configure / write / read` 权限。

**F-07 · 在 1Panel Nacos 创建 `prod` namespace**
- 控制台：8081 → 命名空间 → 新建 → `prod`，拿到 namespace id 填入 `NACOS_NAMESPACE`。

**F-08 · 让业务应用出现在 1Panel 面板**
- 1Panel v2 支持 "本地应用 / 外部服务"，可：
  - **方式 A（推荐）：** 将业务服务的 compose 模板放到 `/opt/1panel/apps/tailor-is/` 下并走 1Panel 本地应用流程安装；
  - **方式 B（轻量）：** 在 1Panel → 网站 → 反向代理，将网关 (`8080`) 与面板域名绑定。
- 需先 POST `/api/auth/login` 获取 `token`，再调用 `/api/v1/apps/installs` 创建应用实例（1Panel 开放 API 文档参考 `https://github.com/1Panel-dev/1Panel`）。

### 🟢 低优先级（运维提效）

**F-09 · 新增 deploy/1panel-integration.sh** — 一次性完成 F-05 / F-06 / F-07。
**F-10 · 接入 1Panel 日志面板** — 将业务 stdout/stderr / filebeat 索引到 1Panel 日志面板（OpenSearch 可选）。
**F-11 · 1Panel 计划任务接管备份** — 将 `deploy/scripts/backup-enhanced.sh` 注册为面板 cron。

---

## 四、推荐的"重跑部署"一次性命令清单

> ⚠️ 需先以 root / sudo 执行 F-01，再执行下列命令。

```bash
# 1) 清理旧容器（只清我们自己的业务容器，不动 1Panel 容器！）
docker compose -f /home/tailor/Tailoris/docker-compose.prod.yml down -v

# 2) 重新生成生产环境变量（覆盖为 1Panel 真实值）
cp /home/tailor/Tailoris/deploy/.env.production /opt/tailor-is/.env
# 在此文件中：将 *_HOST 改为 127.0.0.1，密码按第 三 节 F-03 填入
# 将 CRLF 转 LF
sed -i 's/\r$//' /opt/tailor-is/.env
chmod 600 /opt/tailor-is/.env

# 3) 验证 1Panel 基础设施可连通（用 curl + mysql-cli / redis-cli）
mysql -h 127.0.0.1 -P 3306 -u root -p'mysql_ZmY2sr' -e "SHOW DATABASES;"
redis-cli -h 127.0.0.1 -p 6379 -a 'redis_jD2N8n' PING
curl -u 'rabbitmq:rabbitmq' http://127.0.0.1:15672/api/overview | head -c 200

# 4) 仅启动业务容器与监控（去掉 mysql/redis/rabbitmq/nacos 声明）
cd /opt/tailor-is
docker compose -f docker-compose.prod.yml up -d --remove-orphans

# 5) 健康检查
sleep 30
docker compose ps
curl http://127.0.0.1:8080/actuator/health
curl http://127.0.0.1:8101/actuator/health

# 6) 登录 1Panel 面板（http://{IP}:11336/632ae1b167）
#    - 在 MySQL / Redis / RabbitMQ / Nacos 应用界面创建所需的 database/vhost/namespace
#    - 在"网站"页面新增反向代理，指向网关 8080
```

---

## 五、异常行为日志摘录（供复现）

```
# 1Panel systemd 单元缺失（已知 v2 特性）
$ systemctl status 1panel
Unit 1panel.service could not be found.

# 当前用户无 Docker 权限
$ docker info
permission denied while trying to connect to docker API at unix:///var/run/docker.sock

# 若强制拉起 compose，预期端口冲突
$ docker compose -f docker-compose.prod.yml up -d
ERROR: for tailor-is-mysql
  Bind for 0.0.0.0:3306 failed: port is already allocated

# .env.production 旧版换行符导致凭证被污染（已修复）
$ file deploy/.env.production
deploy/.env.production: ASCII text, with CRLF line terminators
```

---

## 六、最终状态

| 检查项 | 状态 | 备注 |
|--------|------|------|
| 1Panel 面板可访问 | ✅ | `http://127.0.0.1:11336/632ae1b167` |
| 1Panel 应用目录齐全 | ✅ | `/opt/1panel/apps/{mysql,redis,rabbitmq,nacos,openresty}/` |
| 1Panel 容器端口占用 | ✅ | 3306/6379/5672/15672/8848/8081 全部 OPEN |
| Docker daemon 可用 | ❌ | tailor 不在 docker 组 |
| `.env.production` 密码字段 | ❌ | 仍含 `<...>` 占位符 |
| `.env.production` 换行符 | ✅（已修复） | Unix LF |
| 业务容器已按计划启动 | ❌ | 未执行 |
| 服务出现在 1Panel 面板 | ❌ | 未集成 |

### 下一步建议（按顺序）

1. **申请 root / sudo** → 执行 `usermod -aG docker tailor` + 重新登录  
2. **执行本节 "四 · 重跑部署"**（先修改 `docker-compose.prod.yml` 删除重复的基础设施 service）  
3. **按 F-05 / F-06 / F-07** 在 1Panel 控制台手工建业务库 / vhost / namespace  
4. **将业务 compose 上板**（1Panel → 应用商店 → 本地应用 → 导入）  
5. **运行 `deploy/scripts/health-check.sh`** 与 `deploy/scripts/nacos-function-test.sh` 做回归验证  
6. **生成最终部署报告** → `/home/tailor/Tailoris/deploy/DEPLOYMENT-REPORT-<date>.md`
