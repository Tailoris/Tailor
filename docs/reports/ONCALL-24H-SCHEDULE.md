# Tailor IS 24 小时黄金监控期值班表

> 适用阶段: 部署完成后 01:30 → 次日 01:30（共 24 小时）
> 目标: 确保新代码上线后第一个完整的"日周期"（夜间低峰 → 早高峰 → 工作峰 → 晚高峰 → 夜间）内无 P0/P1 级告警逃逸

---

## 一、值班表（四班四运转，每班 6 小时）

| 班别 | 时段 | 值班人 A（主） | 值班人 B（备） | 电话 A | 电话 B | 关注重点 |
|------|------|----------------|----------------|--------|--------|----------|
| 夜班 A | 01:30 — 07:30 | 运维-张三 | 开发-李四 | 13x-xxxx-001 | 13x-xxxx-002 | 夜间批处理 / 备份任务 / 磁盘/CPU 突发 |
| 早班 | 07:30 — 13:30 | 开发-王五 | DBA-赵六 | 13x-xxxx-003 | 13x-xxxx-004 | 早高峰登录/下单性能（`admin_login_count_total` / `abnormal_login_rate`） |
| 下午班 | 13:30 — 19:30 | 测试-孙七 | 产品-周八 | 13x-xxxx-005 | 13x-xxxx-006 | 业务高峰期：订单/支付/邮件链路 |
| 夜班 B | 19:30 — 01:30 | 运维-吴九 | 开发-郑十 | 13x-xxxx-007 | 13x-xxxx-008 | 晚高峰 + 夜间平滑过渡到 A 班 |

> **接班要求**: 交接班时间前 30 分钟，交班人在飞书群发布 `[交接班报告]`，包含：
> 当班发生的告警列表、已处理项、遗留项、当前关键指标快照（P95、5xx 率、活跃用户）

---

## 二、告警级别与响应 SOP

### 2.1 严重级别定义

| 级别 | 定义 | 通知渠道 | 响应时限 | 示例 |
|------|------|----------|----------|------|
| **P0** | 全站不可用 / 核心链路中断（登录/下单/支付）/ 数据库主从断开 | 钉钉@所有人 + 飞书@所有人 + 企微 + 所有 oncall 邮件 + 电话（5min 内无响应自动升级） | **5 分钟**响应，**30 分钟**恢复 | `up == 0`、订单创建失败率 > 10% |
| **P1** | 业务子模块不可用 / 性能严重退化 / 登录异常突增 | 钉钉@值班 + 飞书@值班 + 邮件 | **15 分钟**响应，**1 小时**恢复 | `abnormal_login_rate > 50`、P95 延迟 > 3s 持续 5min |
| **P2** | 非核心告警（日志/磁盘/中间件容量）| 飞书 + 邮件 | **2 小时**内处理 | 磁盘剩余 < 20%、Redis 内存使用率 > 80% |
| **P3** | 信息类（容量规划/常规巡检）| 邮件 | 当日内处理 | 定时任务结束、证书距到期 > 30 天 |

### 2.2 P0 告警响应标准操作流程（SOP）

```
收到 P0 告警（<5min）
    ↓
第 1 步 值班人 A 确认
    · 打开 Grafana 大盘（http://grafana.tailoris.internal:3000/d/prod）
    · 打开 Alertmanager（http://alertmanager.tailoris.internal:9093）
    · 打开应用日志（tailor-is-user / tailor-is-order 的最近 500 行）
    · 发飞书："【P0 确认】<告警名> 正在处理"
    ↓
第 2 步 定位（<15min）
    · 判断是"代码问题 / 数据库问题 / 中间件问题 / 外部依赖问题"
    · 若数据库问题 → DBA 直接介入
    · 若代码问题 → 查最近部署变更（`docker history <image>`）
    · 若外部依赖问题 → 查 Resend / Nacos / Redis 状态
    ↓
第 3 步 决策
    · 可定位并 30min 内能修复 → 执行热修复（发 Patch + 重启容器）
    · 不可定位 / 30min 无把握 → **触发回滚**（见下）
    ↓
第 4 步 执行回滚（见 DEPLOYMENT-EXECUTION-PLAN.md 第五章）
    · 双人 Review 回滚命令
    · 回滚后执行 health-check.sh + test-business-metrics.sh 双校验
    · 观察 30 分钟，确认 P95 延迟 / 错误率恢复到基线
    ↓
第 5 步 通报
    · 发飞书全员 + 邮件：告警级别、影响范围、处理动作、当前状态
    · P0 恢复后 2 小时内出具《事故初步报告》
    · 48 小时内举行复盘会议
```

### 2.3 P1/P2/P3 响应

| 级别 | 动作 |
|------|------|
| P1 | 15min 内值班人线上确认 → 1 小时内恢复 / 或降级为 P2 |
| P2 | 2 小时内值班人排查 → 必要时创建 Ticket 分配给对应模块 Owner，下个工作日处理 |
| P3 | 当日归档到《容量/优化事项清单》|

---

## 三、关键监控大盘 & 巡检清单

### 3.1 每小时必看指标（所有值班人）

| 指标 | 来源 | 基线（可接受范围）| 触发关注 |
|------|------|-------------------|----------|
| **服务存活** | `up{job=~"tailor-is-.*"} == 1` | 全部 1 | 任何一个为 0 → P0 |
| **P95 延迟** | `http_request_duration_seconds{quantile="0.95"}` | < 1.5 s | > 3s 持续 5min → P1 |
| **错误率** | `rate(http_requests_total{status=~"5.."}[5m]) / rate(http_requests_total[5m])` | < 1% | > 5% → P1，> 10% → P0 |
| **活跃用户数** | `business_active_users` | 日间 > 1000，夜间 > 100 | 突然为 0 → 检查登录链路 |
| **异常登录比例** | `abnormal_login_rate` | < 20 | > 50 → P1（疑似撞库/密码爆破）|
| **管理员登录次数** | `admin_login_count_total` | 缓慢增长 | 短时间内异常增加 + `failed_login_attempts_total{role="admin"}` 飙升 → P0 |
| **MySQL 连接数** | `mysql_global_status_threads_connected` | < 500 | > 800 → P1 |
| **Redis 内存使用率** | `redis_memory_used_bytes / redis_memory_max_bytes` | < 70% | > 85% → P1 |
| **磁盘剩余** | `node_filesystem_avail_bytes{mountpoint="/"}` | > 20% | < 10% → P1，< 5% → P0 |

### 3.2 每日必做巡检（08:00 / 20:00）

```
# 1. 告警通道活否
curl -s http://127.0.0.1:8080/health | python3 -m json.tool
# 2. Prometheus targets 是否全 UP
curl -s http://127.0.0.1:9090/api/v1/targets | python3 -c "import sys,json;d=json.load(sys.stdin);[print(t['labels']['job'],t['health']) for t in d['data']['activeTargets']]"
# 3. 业务指标端到端测试
bash /home/tailor/Tailoris/deploy/scripts/test-business-metrics.sh
# 4. 数据库备份是否成功（最近 24h）
ls -lh /opt/tailor-is/backups/ | tail -5
# 5. 邮件通道 smoke-test
curl -s http://127.0.0.1:18080/api/auth/password/reset-request -X POST -H "Content-Type: application/json" -d '{"username":"oncall-smoke"}'
# 6. 容器状态
docker compose -f /home/tailor/Tailoris/deploy/docker-compose.prod.yml ps
```

---

## 四、联系方式 & 升级链路

```
主值班链路（飞书群: Tailor-IS-Oncall）
    ｜ 值班人 A （5min 响应）
    ｜    ├─ 无响应 → 值班人 B（电话）
    ｜    └─ 处理中但需要帮助 → 拉对应模块负责人入群
    ｜
升级链路（P0）
    ├─ 项目经理（电话）→ 5min
    ├─ CTO（电话）→ 10min
    └─ 产品负责人（飞书）→ 对外沟通
```

| 角色 | 姓名 | 飞书 | 电话 | 职责 |
|------|------|------|------|------|
| 部署总负责人 | 张三 | @zhangsan | 13x-xxxx-001 | 最终决策 / 升级判断 |
| 开发负责人 | 李四 | @lisi | 13x-xxxx-002 | 代码级问题排查 |
| DBA 负责人 | 赵六 | @zhaoliu | 13x-xxxx-003 | MySQL/Redis 问题 |
| 运维负责人 | 孙七 | @sunqi | 13x-xxxx-004 | 基础设施/容器编排 |
| 产品负责人 | 周八 | @zhouba | 13x-xxxx-005 | 对外口径 |

---

## 五、黄金监控期结束标准（次日 01:30）

满足**全部条件**即可结束黄金监控期，转入常规运维：

1. ✅ 过去 24 小时内 **无 P0** 告警
2. ✅ 过去 6 小时内 **无新的 P1** 告警
3. ✅ 业务核心指标（活跃用户数 / 错误率 / P95 延迟 / 订单量）回归基线（对比上一周同期 ± 10%）
4. ✅ BusinessMetrics 6 个指标持续上报 Prometheus（`admin_login_count_total`、`failed_login_attempts_total`、`total_login_attempts_total`、`password_reset_requests_total`、`business_active_users`、`abnormal_login_rate`）
5. ✅ 最近一次数据库备份成功，且能被 `backup-enhanced.sh` 校验通过

结束时由值班人 A 在飞书群发 `【黄金监控期结束】✅ Tailor IS 部署完成，所有关键指标正常`，并附最后 1 小时的 Grafana 截图。

---

## 六、关联文档

| 文件 | 说明 |
|------|------|
| `deploy/DEPLOYMENT-EXECUTION-PLAN.md` | 部署执行计划表（含回滚方案） |
| `deploy/docker-compose-monitoring.yml` | 监控告警栈 docker compose |
| `deploy/alertmanager.yml` | Alertmanager 路由 / receiver 配置 |
| `deploy/alert-webhook/server.py` | 告警分发中继服务 |
| `deploy/scripts/test-business-metrics.sh` | 业务指标端到端测试 |
| `deploy/scripts/health-check.sh` | 服务健康一键检查 |
| `rto-drill/*/rto-report.json` | 生产级 RTO 演练报告 |
