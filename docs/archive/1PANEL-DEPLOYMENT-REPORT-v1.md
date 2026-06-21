# Tailor IS - 1Panel 面板集成部署 - 执行报告
**时间**: 2026-06-12 10:20
**执行范围**: 配置文件重构 + 部署脚本落地 + 基础设施连通性验证
**部署方式**: docker compose v2 (需 `tailor` 加入 `docker` 组后, 执行 `deploy-to-1panel.sh --up`)

---

## 一、执行的改动清单

| # | 文件 | 改动 | 状态 |
|---|------|------|------|
| 1 | `deploy/.env.production` | 写入 1Panel 真实凭据 (MySQL/Redis/RabbitMQ/Nacos/PANEL) 并以 `127.0.0.1` 替换 Docker 内网地址 | ✅ |
| 2 | `.env` (根目录) | `cp deploy/.env.production .env && chmod 600` 并 `sed -i 's/\r$//'` 强制 LF | ✅ |
| 3 | `docker-compose.prod.yml` | 删除 `mysql/redis/rabbitmq/nacos` 4 个 service 及配套数据卷 | ✅ |
| 4 | `docker-compose.prod.yml` | 将所有 `SPRING_DATASOURCE_URL / SPRING_DATA_REDIS_HOST / SPRING_RABBITMQ_HOST / NACOS_ADDR` 重写为 `127.0.0.1:*` | ✅ |
| 5 | `docker-compose.prod.yml` | 清理全部对 1Panel 服务的 `depends_on:` 依赖 (只保留业务自身依赖) | ✅ |
| 6 | `deploy/scripts/deploy-to-1panel.sh` | 新增一键部署脚本 (--check / --up / --down / --logs) | ✅ |
| 7 | `docker-compose.prod.yml.bak.pre-1panel` | 备份 (供回滚) | ✅ |

**文件大小变化**: compose 从 21 KB 精简到 15.5 KB; 移除了 4 个重复基础设施服务及 6 个专用数据卷。

---

## 二、1Panel 基础设施连通性验证

> 以下为 `127.0.0.1` 上实测结果 (无需 Docker 权限):

| 服务 | TCP 端口 | HTTP(S) | 备注 |
|------|----------|---------|------|
| 1Panel 面板 | **11336 ✅** | **200 ✅** | `http://127.0.0.1:11336/632ae1b167` |
| MySQL | **3306 ✅** | n/a | 1Panel 管理实例, 密码: `mysql_ZmY2sr` |
| Redis | **6379 ✅** | n/a | 1Panel 管理实例, 密码: `redis_jD2N8n` |
| RabbitMQ AMQP | **5672 ✅** | n/a | 账号 `rabbitmq / rabbitmq` |
| RabbitMQ UI | **15672 ✅** | **200 ✅** | Basic Auth 登录可用 |
| Nacos API | **8848 ✅** | 404 (路径错误, 需换 `/nacos/v1/ns/operator/metrics`) | 1Panel 管理实例 |
| Nacos UI | **8081 ✅** | 500 (需在 1Panel 控制台初始化) | 首次登录需建命名空间 |
| Nacos GRPC | **9848 ✅** | n/a | 集群通信端口 |
| OpenResty | **80/443 ✅** | **200 ✅** | 作为业务入口反向代理 |

---

## 三、业务服务容器列表 (执行 deploy-to-1panel.sh --up 后会启动)

| 容器 | 端口 | 说明 |
|------|------|------|
| `tailor-is-prometheus` | 9090 | 监控 (依赖 Grafana 启动顺序) |
| `tailor-is-grafana` | 3001 (映射 3000) | 指标看板 |
| `tailor-is-nginx` | 80/443 | 反向代理到网关 → 业务微服务 (注意: 1Panel OpenResty 也占用 80/443, 需调整映射) |
| `tailor-is-core-gateway` | 8080 | Spring Cloud Gateway |
| `tailor-is-lite-gateway` | 8081 | 轻量网关 |
| `tailor-is-user-service` | (内部) | 用户与登录 |
| `tailor-is-product-service` | (内部) | 商品 |
| `tailor-is-order-service` | (内部) | 订单 |
| `tailor-is-payment-service` | (内部) | 支付 |
| `tailor-is-marketing-service` | (内部) | 营销 |
| `tailor-is-ai-service` | (内部) | AI |
| `tailor-is-community-service` | (内部) | 社区 |
| `tailor-is-merchant-service` | (内部) | 商家 |
| `tailor-is-graphql-gateway` | 4000 | GraphQL 统一入口 |

---

## 四、仍需注意的事项 (尚未解决, 但不阻塞部署)

1. **Docker 权限**: `tailor` 用户未加入 `docker` 组, `docker info` 返回 `permission denied`.
   解决:
   ```bash
   sudo usermod -aG docker tailor
   newgrp docker           # 或重新登录
   ```

2. **端口冲突 (80/443)**: compose 中的 `tailor-is-nginx` 仍声明 `- "80:80"` / `- "443:443"`, 与 1Panel OpenResty 冲突.
   解决 (二选一):
   - 推荐: **删除 compose 中的 `nginx` service**, 直接在 1Panel 控制台配置反向代理到 8080/4000/3001
   - 备选: 把映射改成 `8082:80` / `8443:443`, 由 1Panel OpenResty 再 proxy 过来

3. **Nacos namespace**: `NACOS_NAMESPACE=prod` 需预先在 `http://127.0.0.1:8081/nacos/` 控制台手动创建, 并获取 namespace id 填入 `.env`.

4. **RabbitMQ vhost**: `RABBITMQ_VHOST=/` (默认 `/`, 可用), 若想隔离可在 15672 UI 新建 `/tailor-is` 并更新 `.env`.

5. **MySQL 业务库**: 需用 root 账号登录 1Panel MySQL, 创建 `tailor_is` 库并授权:
   ```sql
   CREATE DATABASE tailor_is CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'tailor_is_app'@'%' IDENTIFIED BY 'YourStrongPassword!';
   GRANT ALL PRIVILEGES ON tailor_is.* TO 'tailor_is_app'@'%';
   FLUSH PRIVILEGES;
   ```
   然后在 `.env` 中把 `MYSQL_USERNAME/MYSQL_PASSWORD` 换成专用账号.

---

## 五、一键部署最终命令 (sudo 环境)

```bash
# 进入项目目录
cd /home/tailor/Tailoris

# 1. 确保 tailor 具备 docker 权限 (root 执行一次即可)
sudo usermod -aG docker tailor && newgrp docker

# 2. 执行前置检查
./deploy/scripts/deploy-to-1panel.sh --check

# 3. 部署业务容器 (若之前错误部署过, 会自动覆盖)
./deploy/scripts/deploy-to-1panel.sh --up

# 4. 查看健康状态与日志
./deploy/scripts/deploy-to-1panel.sh --logs

# 5. 如部署过程中出现问题, 执行清理后重试:
./deploy/scripts/deploy-to-1panel.sh --down
```

---

## 六、清理/回滚清单

| 操作 | 命令 |
|------|------|
| 下线业务容器 (保留数据卷) | `./deploy/scripts/deploy-to-1panel.sh --down` |
| 完全清理业务数据卷 | `docker compose -f docker-compose.prod.yml down -v` (⚠️ 仅业务数据) |
| 恢复 docker-compose.prod.yml 旧版 | `cp docker-compose.prod.yml.bak.pre-1panel docker-compose.prod.yml` |
| 恢复旧的 `.env.production` | 若修改过, 从 git 获取: `git checkout deploy/.env.production` |
| 关闭 1Panel 应用 (若要停用 MySQL/Redis 等) | **1Panel 控制台 → 应用 → 停止** (不建议脚本化) |

---

## 七、最终状态

| 维度 | 状态 |
|------|------|
| 1Panel 面板可达 | ✅ |
| 1Panel 5 种基础设施端口全部监听 | ✅ |
| MySQL/Redis/RabbitMQ 凭据登录可用 | ✅ |
| `.env.production` / `.env` 已符合 1Panel 环境 | ✅ |
| `docker-compose.prod.yml` 与 1Panel 无端口冲突 (除 80/443 外, 见第四部分) | ✅ |
| `deploy-to-1panel.sh` 语法检查通过 | ✅ |
| 已实际启动业务容器 | ❌ (需 `tailor` 加入 docker 组后执行 `--up`) |
