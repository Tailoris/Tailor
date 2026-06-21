# Tailor IS（裁智云）部署执行计划表

> 版本: v2.0 · 生效日期: 2026-06-12 · 依据: COMPREHENSIVE-AUDIT-AND-IMPROVEMENT-REPORT.md
> RTO 目标: ≤ 1800s（实测: 13.87s） · 综合就绪度: 25%（详见核查报告）

---

## 〇、部署前紧急修复（Phase 0 · 必须先完成，阻塞部署）

> **依据**: 2026-06-12 系统性全面核查报告（COMPREHENSIVE-AUDIT-AND-IMPROVEMENT-REPORT.md）
> **当前状态**: 18 个微服务 Docker 镜像不存在，所有微服务均未运行，部署阻塞

| # | 修复项 | 问题 ID | 当前状态 | 目标状态 | 负责人 | 截止日期 |
|---|--------|---------|---------|---------|--------|---------|
| 0.1 | 统一 Spring Boot 版本（独立 tailor-is-user 2.7.18 → 3.3.5） | C-01 | ⬜ 未开始 | 版本一致 | 后端 | 2026-06-13 |
| 0.2 | 为核心 8 个模块构建 Docker 镜像 | C-02, C-03 | ⬜ 未开始 | 镜像可用 | DevOps | 2026-06-13 |
| 0.3 | 清理空模块 common-web 和废弃 gateway | C-06, L-04 | ⬜ 未开始 | 父 POM 干净 | 后端 | 2026-06-13 |
| 0.4 | 确认 .gitignore 排除 .env（敏感信息保护） | C-04 | ⬜ 未开始 | 安全合规 | DevOps | 2026-06-13 |
| 0.5 | 清理根目录临时日志和 PID 文件 | L-06 | ✅ 已完成 | 目录整洁 | DevOps | 2026-06-12 |
| 0.6 | 启动微服务集群并验证 Nacos 服务注册 | C-03 | ⬜ 未开始 | 核心服务可发现 | DevOps | 2026-06-14 |

**Phase 0 完成标准**: 核心 8 个微服务（user/merchant/product/order/payment/marketing/community/ai）全部通过健康检查，Nacos 服务列表可见。

---

## 一、前置检查清单（部署前 24h 必须全通过）

| # | 检查项 | 负责人 | 结果 | 备注 |
|---|--------|--------|------|------|
| 1 | 生产服务器 CPU/内存/磁盘 ≥ 最低规格（8C/16G/200G SSD） | 运维 | ✅ | 实际配置：16C/32G/500G SSD |
| 2 | Docker + Docker Compose 已安装（≥ 24.0） | 运维 | ✅ | |
| 3 | 数据库 MySQL 8.4.9 已就绪（用户/权限/库） | DBA | ✅ | 通过 1Panel 管理 |
| 4 | Redis 8.8.0 已就绪（密码已配置） | DBA | ✅ | 通过 1Panel 管理 |
| 5 | RabbitMQ 4.2.5 已就绪（vhost/user） | 中间件 | ✅ | 通过 1Panel 管理 |
| 6 | Nacos 3.2.2 配置已同步（配置中心 & 注册中心） | 中间件 | ✅ | 通过 1Panel 管理 |
| 7 | Resend 邮件 API Key 已配置在 `.env` | 开发 | ✅ | noreply@tailorbot.top |
| 8 | 域名 / SSL 证书（Let's Encrypt / 自签）已准备 | 运维 | ✅ | |
| 9 | 备份脚本（`deploy/scripts/backup-enhanced.sh`）已验证 | DBA | ✅ | |
| 10 | RTO 演练报告已生成并评审（`rto-drill/*/rto-report.json`） | DBA/运维 | ✅ | 100 万用户规模 |
| 11 | BusinessMetrics 端点 `/actuator/prometheus` 可达 | 开发 | ✅ | 端到端测试 10/10 |
| 12 | 监控栈（Prometheus + Alertmanager + Grafana）配置已落盘 | 运维 | ✅ | `docker-compose.prod.yml` |
| 13 | 告警渠道（钉钉/飞书/企微/邮件）已端到端验证 | 运维 | ✅ | Resend 邮件回执 OK |
| 14 | 部署窗口已通知，相关人员可响应（电话 + IM） | 项目经理 | ✅ | |
| 15 | 回滚镜像/代码备份/数据库快照已保存 | 运维 | ✅ | |
| 16 | **Phase 0 全部完成** | DevOps | ⬜ | **阻塞项 - 必须先完成** |

---

## 二、部署时间窗口

- **部署日期**: 2026-06-12（周五业务低峰）
- **开始时间**: 23:00
- **预计完成时间**: 2026-06-13 01:30
- **不可回滚点**: 2026-06-12 23:30（数据库迁移执行完成前）
- **黄金监控期**: 2026-06-13 01:30 — 2026-06-14 01:30（24h）
- **全面放行**: 2026-06-14 10:00（业务高峰前验证通过）

---

## 三、分阶段部署流程

### 阶段 0 · 准备（T-60min → T-0min）

| 时间 | 操作 | 执行人 | 验证方式 |
|------|------|--------|----------|
| 22:00 | 全员就位，确认前置检查清单 | 项目经理 | 口头 |
| 22:10 | 生产环境快照（VM / 云盘）| 运维 | 控制台显示"完成" |
| 22:20 | 拉取最新 Docker 镜像至生产节点 | 运维 | `docker images` 查看 tag |
| 22:30 | 读取 `deploy/.env.production`，打印关键配置确认 | 运维 | diff 确认 |
| 22:40 | 执行数据库备份（冷备）并校验 SHA256 | DBA | `backup-integrity.sha256` |
| 22:50 | 通知相关业务方「即将进入维护窗口」 | 项目经理 | 飞书/邮件 |

### 阶段 1 · 服务切换到维护页（T+0min）

- **Nginx**: 将所有业务路由 rewrite 到 `/maintenance/index.html`
- **保留端口**: `/actuator/prometheus`, `/health`, Grafana 可访问
- 预计用时: 5 min

### 阶段 2 · 数据库迁移（T+5min → T+25min）

```bash
cd /home/tailor/Tailoris/deploy
# 1) 执行增量迁移脚本（顺序编号保证幂等）
ls sql/0*.sql | sort | while read f; do
  mysql -h$DB_HOST -u$DB_USER -p$DB_PASS tailor_is_prod < "$f"
  echo "[OK] $f" >> migration.log
done
# 2) 数据一致性校验
mysql -e "SELECT 'users',COUNT(*) FROM users UNION ALL SELECT 'orders',COUNT(*) FROM orders"
# 3) 记录迁移耗时到部署报告
```

**回滚条件**: 任何 SQL 报错 / 数据行数异常 → 停止并回到阶段 0 备份恢复

### 阶段 3 · 微服务滚动部署（T+25min → T+80min）

采用 **先监控后业务**、**由下往上** 的部署顺序：

| 顺序 | 服务 | 端口 | 健康检查 | 回滚触发阈值 |
|------|------|------|----------|--------------|
| 1 | Nacos（配置/注册中心） | 8848 | `/nacos/v1/console/health/liveness` | 启动失败 |
| 2 | MySQL（主库） | 3306 | `SELECT 1` + 主从延迟 < 5s | 主从断开 |
| 3 | Redis（缓存） | 6379 | `PING → PONG` | 连不上 |
| 4 | RabbitMQ（消息队列） | 5672/15672 | `GET /api/overview` | 队列消费者数 = 0 |
| 5 | Gateway（API 网关） | 8080 | `/actuator/health` → `UP` | 路由 5xx 率 > 5% |
| 6 | tailor-is-user（用户服务） | 18080 | `/actuator/health` + 业务测试登录 | 失败登录率 30s 上升 2x |
| 7 | tailor-is-order（订单服务） | 18081 | `/actuator/prometheus` 指标存在 | 订单创建失败率 > 3% |
| 8 | 前端静态站 / Nginx 反向代理 | 80/443 | `curl -I https://tailoris.com` → 200 | 响应码非 200 |

**部署命令**（每服务独立执行，间隔 3-5 min 观察）:

```bash
cd /home/tailor/Tailoris/deploy
# 滚动替换：down 旧容器 → up 新容器 → 健康检查
docker compose -f docker-compose.prod.yml up -d --force-recreate <service_name>
sleep 10
# 健康检查
curl -s http://127.0.0.1:<port>/actuator/health | grep -q '"status":"UP"'
# 业务冒烟（用户服务举例）
curl -s -X POST http://127.0.0.1:18080/api/auth/login -H "Content-Type: application/json" \
  -d '{"username":"smoke-test","password":"smoke-test"}' | grep -q "code"
```

### 阶段 4 · 监控/告警栈部署（T+80min → T+95min）

```bash
cd /home/tailor/Tailoris/deploy
docker compose -f docker-compose-monitoring.yml up -d --force-recreate
# 健康检查
sleep 10
curl -s http://127.0.0.1:9090/-/healthy    # Prometheus
curl -s http://127.0.0.1:9093/-/healthy    # Alertmanager
curl -s http://127.0.0.1:3000/api/health   # Grafana
curl -s http://127.0.0.1:8080/health        # alert-webhook（渠道状态）
```

**告警端到端验证**: 由运维 POST 一条模拟告警到 `alert-webhook:8080/api/v1/alerts/critical`，验证钉钉/飞书/邮件 4 渠道均收到。

### 阶段 5 · 业务冒烟测试（T+95min → T+110min）

由测试/产品执行，需全部通过：

| # | 用例 | 验收标准 |
|---|------|----------|
| 1 | 用户注册 + 登录 + 退出 | token 正常返回，`admin_login_count_total` 指标上升 |
| 2 | 密码重置流程 | 邮件收到，`password_reset_requests_total` 上升 |
| 3 | 下单 → 支付（模拟）→ 订单状态流转 | 订单状态从 pending→paid 正常 |
| 4 | 商品浏览 + 搜索 + 分页 | 首屏 < 1.5s（P95） |
| 5 | 多商户后台登录 | 权限隔离正确 |
| 6 | 邮件发送（Resend 集成） | 30s 内收到告警/业务邮件 |

### 阶段 6 · 关闭维护页，正式开放（T+110min → T+130min）

1. Nginx 恢复生产路由
2. 灰度 5% → 10% → 30% → 100% 流量（每步观察 5 min）
3. 监控 30 min，观察错误率/延迟/P95，确认无异常
4. 邮件/飞书通知全员「部署完成 + 进入黄金监控期」

---

## 四、各阶段健康检查指标

每个阶段结束前必须满足以下**全部条件**才能进入下一阶段：

| 维度 | 告警规则 | 严重级别 |
|------|----------|----------|
| 服务可用 | `up == 0` 持续 1 分钟 | P0 |
| 响应延迟 | `http_request_duration_seconds{quantile="0.95"} > 3s` 持续 5min | P1 |
| 错误率 | `rate(http_requests_total{status=~"5.."}[5m]) / rate(http_requests_total[5m]) > 0.05` | P1 |
| MySQL | `mysql_global_status_threads_connected > 800` / 主从延迟 > 30s | P1 |
| Redis | `redis_connected_clients > 500` / `redis_memory_used_bytes > 0.8 * maxmemory` | P1 |
| 业务异常 | `abnormal_login_rate > 50`（异常登录比例过高）| P1 |
| 业务登录异常 | `failed_login_attempts_total{role="admin"} 涨幅 > 3x` | P0 |
| 磁盘 | `node_filesystem_avail_bytes{mountpoint="/"} < 10%` | P1 |

触发 P0 告警 → **按第五章节执行回滚**

---

## 五、回滚方案（Decision Tree）

```
              +-----------------------+
              | 出现 P0 / 重大业务故障 |
              +-----------+-----------+
                          |
         +----------------+----------------+
         |                |                |
    [仅代码/配置变更]  [数据库无变更]   [数据库已变更 / 不可回退]
         |                |                |
  docker compose up -d   同上        1. 维护页 ON
  --force-recreate     （更快）      2. 恢复数据库快照 (T0 备份)
  <service_name>                      3. 恢复应用
         |                |           4. 数据核对（行数/主键连续性）
         v                v           5. 业务冒烟测试
  5min 健康检查通过 → 继续观察
                       30min 无异常 → 回滚完成，邮件通报
```

### 回滚命令速查（所有操作均须双人 Review）

```bash
# 1) 应用代码快速回滚
cd /home/tailor/Tailoris/deploy
# 切回上一个已知稳定 tag
docker compose -f docker-compose.prod.yml up -d --force-recreate <service_name>

# 2) 配置文件回滚（从 backup_raw/configs 恢复）
cp -r $DRILL_DIR/backup_raw/configs/.env.production deploy/.env.production

# 3) 数据库回滚（演练已验证：100 万用户规模 ≈ 12s）
gunzip -c $DRILL_DIR/backup_raw/mysql.sql.gz | mysql -h$DB_HOST -u$DB_USER -p$DB_PASS tailor_is_prod

# 4) Redis 回滚
#    在演练中：redis-cli --pipe < <(cat backup_raw/dump.rdb)
#    在生产：redis-cli SHUTDOWN NOSAVE && cp dump.rdb /data/ && redis-server

# 5) 回滚后校验
bash deploy/scripts/health-check.sh        # 服务健康
bash deploy/scripts/test-business-metrics.sh  # 业务指标链路
bash deploy/scripts/verify-e2e.sh            # 端到端
```

### 回滚升级 / 通报机制

- **回滚启动** 5 分钟内 → 项目经理电话通知 CTO + 产品负责人
- **回滚完成** 30 分钟后 → 邮件 + 飞书发布《部署回滚报告》，附监控截图
- **失败回滚**（数据库回滚后仍异常）→ 升级为 P0 事故，全团队待命，最长容忍 60 分钟，仍失败 → 切回上一版本全部组件

---

## 六、变更记录

| 版本 | 日期 | 修改人 | 说明 |
|------|------|--------|------|
| v1.0 | 2026-06-12 | Tailor IS Team | 初版发布 |

---

## 七、相关文档

| 文件 | 说明 |
|------|------|
| `COMPREHENSIVE-AUDIT-AND-IMPROVEMENT-REPORT.md` | 系统性全面核查与改进报告（2026-06-12） |
| `ISSUE-TRACKER.md` | 结构化问题跟踪表（31 个问题，4 级分类） |
| `deploy/scripts/backup-enhanced.sh` | 增强版备份脚本（MySQL+Redis+配置） |
| `deploy/scripts/test-business-metrics.sh` | BusinessMetrics 端到端测试 |
| `docker-compose.prod.yml` | 生产环境 Docker Compose 配置（19 个服务） |
| `deploy/alertmanager.yml` | 告警路由（P0/P1/P2/P3） |
| `deploy/alert-webhook/server.py` | 告警分发中继（钉钉/飞书/企微/邮件） |
| `deploy/ONCALL-24H-SCHEDULE.md` | 24h 黄金监控期值班表 |
| `rto-drill/*/rto-report.json` | 生产级 RTO 演练报告 |
