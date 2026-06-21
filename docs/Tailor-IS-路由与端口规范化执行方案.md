# Tailor IS 路由与端口规范化执行方案

> 版本: v2.0 | 日期: 2026-06-19 | 依据: 路由端口规范 v1.0 + 1Panel 部署环境

---

## 一、执行背景

### 1.1 依据文档

| 文档 | 路径 | 用途 |
|------|------|------|
| 系统路由与端口规范 | `docs/Tailor-IS-系统路由与端口规范.md` | 端口分配标准、路由分流规则 |
| 1Panel 部署环境信息 | `项目部署 1Panel账户信息.txt` | 宿主机实际端口占用情况 |

### 1.2 1Panel 环境端口占用（不可变更）

| 服务 | 端口 | 管理方 |
|------|------|--------|
| MySQL | 3306 | 1Panel |
| Redis | 6379 | 1Panel |
| RabbitMQ | 5672 / 15672 | 1Panel |
| **Nacos 界面** | **8080** | 1Panel |
| Nacos API | 8848 | 1Panel |
| Nacos 通讯 | 9848 | 1Panel |
| 1Panel 面板 | 11336 | 1Panel |

**关键约束**: Nacos 由 1Panel 管理，端口 8080 不可变更，因此我们的 core-gateway 宿主端口必须避开 8080。

---

## 二、路由命名规范

### 2.1 命名格式

```
/api/{service-name}/{resource}[/{id}]
```

### 2.2 前缀规则

- **统一前缀**: 所有业务 API 以 `/api/` 开头，由 Nginx 统一代理
- **单数命名**: 使用 `/api/user/` 而非 `/api/users/`
- **禁止层级前缀**: 不使用 `/api/core/` 或 `/api/lite/` 前缀
    - 原因: 网关的 Java 路由配置匹配 `/api/user/**` 等路径，不包含 core/lite 前缀
    - 替代方案: Nginx 直接按服务路径分流到对应网关

### 2.3 特殊场景处理

| 场景 | 规则 | 示例 |
|------|------|------|
| 子资源 | 嵌套不超过 2 层 | `/api/order/{orderNo}/items/{itemId}` |
| 批量操作 | 使用 `batch` 子路径 | `/api/product/batch/update-status` |
| 文件上传 | 使用 `upload` 子路径 | `/api/product/upload/image` |
| WebSocket | 使用 `/ws/` 前缀 | `/ws/chat/{roomId}` |
| 健康检查 | 使用 `/actuator/health` | 仅内部网络可达 |
| 管理接口 | 使用 `/api/admin/` 前缀 | `/api/admin/system/config` |

### 2.4 版本控制策略

- 当前: 不加版本号，默认 v1
- 未来: 通过 URL 路径版本 `/api/v2/user/` 或请求头 `Accept-Version: v2` 区分

---

## 三、路径设计原则

### 3.1 层级结构

```
/ (根路径)
├── /api/                          # 所有业务 API
│   ├── /api/auth/**, /api/user/**      → core-gateway (9000)
│   ├── /api/product/**, /api/favorite/**  → core-gateway (9000)
│   ├── /api/order/**, /api/cart/**     → core-gateway (9000)
│   ├── /api/payment/**, /api/settlement/** → core-gateway (9000)
│   ├── /api/marketing/**, /api/coupon/**  → core-gateway (9000)
│   ├── /api/ai/**, /api/body-size/**   → core-gateway (9000)
│   ├── /api/copyright/**               → core-gateway (9000)
│   ├── /api/merchant/**, /api/shop/**  → core-gateway (9000)
│   ├── /api/admin/**                   → core-gateway (9000)
│   ├── /api/community/**               → lite-gateway (8081)
│   ├── /api/supply/**                  → lite-gateway (8081)
│   ├── /api/message/**                 → lite-gateway (8081)
│   ├── /api/academy/**                 → lite-gateway (8081)
│   ├── /api/analytics/**               → lite-gateway (8081)
│   ├── /api/message-im/**              → lite-gateway (8081)
│   └── /api/pattern/**                 → lite-gateway (8081)
├── /graphql                        → core-gateway (9000)
├── /ws/                            → message-im-service (8114)
├── /merchant/                      → 商户后台前端 SPA
├── /admin/                         → 平台管理前端 SPA
├── /healthz                        → Nginx 健康检查
└── /                               → PC 商城前端 SPA
```

### 3.2 参数传递方式

| 方式 | 用途 | 示例 |
|------|------|------|
| 路径参数 | 资源标识 | `/api/user/123` |
| 查询参数 | 筛选/排序/分页 | `/api/product?page=1&size=20&sort=price` |
| 请求体 | 创建/更新数据 | `POST /api/order` body: `{...}` |
| 请求头 | 认证/追踪 | `Authorization: Bearer xxx`, `X-Trace-Id: xxx` |

---

## 四、端口分配策略

### 4.1 1Panel 环境约束下的端口规划

由于 1Panel 管理的 Nacos 占用 **8080** 端口，原规划中 core-gateway 的宿主端口 8080 必须调整。重新规划如下：

| 服务名 | 容器端口 | 宿主端口(新) | 原宿主端口 | 变更原因 |
|--------|---------|-------------|-----------|---------|
| core-gateway | 8080 | **9000** | 8080 | 避开 1Panel Nacos 8080 |
| lite-gateway | 8081 | 8081 | 8081 | 无冲突，不变 |
| Nginx (前端) | 80 | **80** | 8080 | 前端应使用标准 HTTP 端口 |
| alert-webhook | 8080 | **9095** | 8080 | 避开 1Panel Nacos 8080 |
| Grafana | 3000 | **3001** | 3000 | 避免与 PC SSR 3000 冲突 |
| Sentinel | 8858 | 8858 | 8719 | 修正为容器内端口 |

### 4.2 端口范围划分

| 端口区间 | 用途 | 管理方 |
|---------|------|--------|
| 80 / 443 | HTTP / HTTPS 入口 | Nginx |
| 3000-3099 | 监控与运维工具 | Docker Compose |
| 3306 | MySQL | 1Panel |
| 5672 / 15672 | RabbitMQ | 1Panel |
| 6379 | Redis | 1Panel |
| 8080 | Nacos 界面 | 1Panel（不可变） |
| 8081 | lite-gateway | Docker Compose |
| 8100-8115 | 业务微服务 | Docker Compose |
| 8848 / 9848 | Nacos API / 通讯 | 1Panel |
| 9000 | core-gateway | Docker Compose |
| 9090-9095 | 监控(Prometheus/Alertmanager/Webhook) | Docker Compose |
| 11336 | 1Panel 面板 | 1Panel |

### 4.3 服务类型与端口对应规则

1. **基础设施** (1Panel 管理): 使用标准端口，不可变更
2. **网关入口**: 9000 起步，每增加一个网关 +1
3. **业务微服务**: 8100-8115 区间，按服务注册顺序分配
4. **监控运维**: 3000+ 和 9090+ 区间
5. **前端**: 80/443 标准端口

---

## 五、冲突解决机制

### 5.1 端口占用检测流程

```bash
# 1. 检查宿主机端口占用
sudo ss -tlnp | grep -E ":(80|443|3000|3306|5672|6379|8080|8081|8100|8101|8102|8103|8104|8105|8106|8107|8108|8109|8110|8111|8112|8113|8114|8115|8848|9000|9090|9093|9095|9848|15672|26379)"

# 2. 检查 Docker 容器端口映射
docker ps --format "table {{.Names}}\t{{.Ports}}" | grep tailor-is

# 3. 自动化检测脚本
./deploy/scripts/check-ports.sh
```

### 5.2 冲突预警系统

| 预警级别 | 触发条件 | 处理方式 |
|---------|---------|---------|
| Critical | 1Panel 管理端口被占用 | 立即回滚，重新分配 |
| High | 网关端口冲突 | 阻止部署，排查后重新分配 |
| Medium | 业务服务端口冲突 | 告警通知，自动重新分配 |
| Low | 预留端口可能冲突 | 记录日志，下次部署时检查 |

### 5.3 动态调整方案

当检测到端口冲突时，按以下优先级处理：

1. **首选**: 调整 Docker Compose 宿主端口映射（容器内端口不变）
2. **次选**: 通过环境变量 `PORT_OVERRIDE_xxx` 覆盖端口
3. **最后**: 修改容器内服务端口 + 更新 Nacos 注册信息

---

## 六、本次修正涉及的配置文件

| 文件 | 修正项 | 严重等级 | 状态 |
|------|--------|---------|------|
| `docker-compose.yml` (root) | core-gateway 8080 → 9000, Grafana 3000 → 3001 | Critical | ✅ 已修复 |
| `deploy/docker-compose.prod.yml` | core-gateway 8080 → 9000, 127.0.0.1 → host.docker.internal | Critical | ✅ 已修复 |
| `deploy/docker-compose.frontend.yml` | nginx-frontend 8080:80 → 80:80 | Critical | ✅ 已修复 |
| `deploy/docker-compose-monitoring.yml` | alert-webhook 8080 → 9095, Grafana 3000 → 3001 | Critical | ✅ 已修复 |
| `deploy/docker-compose.services.yml` | core-gateway 8080 → 9000, 统一默认密码 | Critical | ✅ 已修复 |
| `deploy/nginx/frontend.conf` | upstream 端口修正 + 移除 /api/core/ /api/lite/ 前缀 | Critical | ✅ 已修复 |
| `deploy/prometheus.yml` | 服务标签对齐实际端口 + 补充 5 个缺失服务 | High | ✅ 已修复 |
| `deploy/.env.example` | 统一为 1Panel 实际凭据 (mysql_ZmY2sr / redis_jD2N8n / rabbitmq) | High | ✅ 已修复 |
| `.env.example` (root) | GATEWAY_PORT 8080 → 9000, GRAFANA_PORT 3000 → 3001 | High | ✅ 已修复 |
| `tailor-is/deploy/k8s/ingress.yaml` | 移除 rewrite-target + 添加 lite-gateway 路由 | Critical | ✅ 已修复 |
| `deploy/gateway-application.yml` | 标记为废弃 + 删除 | High | ✅ 已修复 |
| `docs/Tailor-IS-系统路由与端口规范.md` | 更新端口总表增加 1Panel 约束 | Medium | ✅ 已修复 |

---

## 七、修正后验证

### 7.1 验证清单

- [x] `docker compose config` 无语法错误
- [x] 所有宿主端口不与 1Panel 服务冲突
- [x] Nginx upstream 端口与网关实际端口一致
- [x] Nginx 路由能正确转发到网关
- [x] 网关 Java 路由配置能匹配 Nginx 转发的路径
- [x] Prometheus 服务标签与实际端口一致
- [x] 无遗留的 gateway-application.yml 配置
- [x] K8s ingress 无 rewrite-target 剥离 /api 前缀
- [x] K8s ingress 包含 lite-gateway 路由规则
- [x] 三套 compose 默认密码统一为 1Panel 凭据
- [x] Prometheus 覆盖全部 18 个微服务监控目标

### 7.2 验证命令

```bash
# 端口占用检查
sudo ss -tlnp | grep -E ":(80|8080|8081|9000|9095)"

# Docker Compose 配置检查
docker compose -f docker-compose.yml config --quiet
docker compose -f deploy/docker-compose.prod.yml config --quiet
docker compose -f deploy/docker-compose.frontend.yml config --quiet
docker compose -f deploy/docker-compose.services.yml config --quiet
docker compose -f deploy/docker-compose-monitoring.yml config --quiet

# Nginx 配置测试
docker run --rm -v $(pwd)/deploy/nginx/frontend.conf:/etc/nginx/conf.d/default.conf:ro nginx:1.27-alpine nginx -t
```