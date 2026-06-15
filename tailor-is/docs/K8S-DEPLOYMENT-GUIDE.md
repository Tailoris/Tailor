# Tailor IS Kubernetes 弹性伸缩部署指南

## 目录

1. [架构概述](#架构概述)
2. [前置条件](#前置条件)
3. [文件结构](#文件结构)
4. [部署步骤](#部署步骤)
5. [HPA 弹性伸缩配置](#hpa-弹性伸缩配置)
6. [闲时缩容策略](#闲时缩容策略)
7. [从 Docker Compose 迁移到 K8s](#从-docker-compose-迁移到-k8s)
8. [监控与告警](#监控与告警)
9. [故障排查](#故障排查)
10. [生产环境 checklist](#生产环境-checklist)

---

## 架构概述

Tailor IS (Tailor Intelligent System) 采用 **双层网关 + 微服务** 架构部署在 Kubernetes 上:

```
                    ┌──────────────────────────────────────┐
                    │         Nginx Ingress Controller     │
                    │         api.tailoris.com             │
                    └──────────────┬───────────────────────┘
                                   │
                    ┌──────────────┴───────────────────────┐
                    │          Core Gateway (8080)         │  <- 核心业务
                    │  /api/auth, /api/order, /api/payment │
                    └────┬─────┬──────┬──────┬─────────────┘
                         │     │      │      │
              ┌──────────┤     │      │      └──────────────┐
              v          v     v      v                      v
          +--------+ +-------+ +------+ +---------+ +---------+
          | Order  | |Payment| |  AI  | |Copyright| |Merchant |
          | :8103  | |:8104  | |:8106 | | :8107   | | :8105   |
          +--------+ +-------+ +------+ +---------+ +---------+

                    ┌──────────────────────────────────────┐
                    │         Lite Gateway (8081)          │  <- 轻量业务
                    │  /api/community, /api/academy        │
                    └────┬──────────┬──────────────────────┘
                         │          │
              ┌──────────┤          └──────────────┐
              v          v                         v
          +-----------+ +---------+           +-----------+
          | Community | | Academy |           | Serverless|
          |  :8108    | | :8112   |           |  (备选)    |
          +-----------+ +---------+           +-----------+
```

### 服务分类

| 分类 | 服务 | 端口 | 说明 |
|------|------|------|------|
| **核心网关** | core-gateway | 8080 | Spring Cloud Gateway, 路由核心业务 |
| **核心服务** | order | 8103 | 订单管理 |
| | payment | 8104 | 支付 (微信/支付宝) |
| | ai | 8106 | AI 纸样生成, 图样处理 |
| | copyright | 8107 | 版权管理 |
| **轻量网关** | lite-gateway | 8081 | Spring Cloud Gateway, 路由轻量业务 |
| **轻量服务** | community | 8108 | 社区 (Serverless 备选) |
| | academy | 8112 | 学院 (Serverless 备选) |

---

## 前置条件

- Kubernetes 集群 >= 1.24 (推荐 >= 1.27 以支持 `timeZone` CronJob)
- kubectl 已配置并连接到目标集群
- metrics-server 已部署 (HPA 必需)
- Nginx Ingress Controller 已部署 (或使用其他 Ingress Controller)
- 镜像仓库已就绪 (Harbor/阿里云 ACR/Docker Hub)
- 基础设施已部署: Nacos, Redis, MySQL, RabbitMQ, RocketMQ, Sentinel

---

## 文件结构

```
deploy/k8s/
├── namespace.yaml                    # 命名空间: tailor-is-prod
├── configmap.yaml                    # 应用配置 (环境变量)
├── secret.yaml                       # 敏感信息 (密码, 密钥)
├── ingress.yaml                      # Ingress 路由规则 + TLS
├── core-services/
│   ├── gateway-deployment.yaml       # Core Gateway 部署
│   ├── gateway-service.yaml          # Core Gateway 服务
│   ├── gateway-hpa.yaml              # Core Gateway HPA (min:2, max:10, CPU:70%)
│   ├── order-deployment.yaml         # Order 服务部署
│   ├── order-service.yaml            # Order 服务
│   ├── order-hpa.yaml                # Order HPA (min:2, max:8, CPU:75%)
│   ├── payment-deployment.yaml       # Payment 服务部署
│   ├── payment-service.yaml          # Payment 服务
│   ├── ai-deployment.yaml            # AI 服务部署
│   ├── ai-service.yaml               # AI 服务
│   ├── ai-hpa.yaml                   # AI HPA (min:1, max:5, CPU:80%)
│   ├── copyright-deployment.yaml     # Copyright 服务部署
│   ├── copyright-service.yaml        # Copyright 服务
│   └── scale-down-cronjob.yaml       # 闲时缩容 CronJob + RBAC
└── lite-services/
    ├── community-deployment.yaml     # Community 服务 (K8s 降级方案)
    └── academy-deployment.yaml       # Academy 服务 (K8s 降级方案)
```

---

## 部署步骤

### Step 1: 创建命名空间

```bash
kubectl apply -f deploy/k8s/namespace.yaml
```

### Step 2: 配置 ConfigMap 和 Secret

```bash
# 1. 编辑 ConfigMap, 将基础设施地址替换为实际值
vim deploy/k8s/configmap.yaml

# 2. 编辑 Secret, 替换所有占位符密码为真实值
# 生成 base64 编码值:
echo -n 'your-actual-mysql-password' | base64
echo -n 'your-actual-redis-password' | base64

vim deploy/k8s/secret.yaml

# 3. 应用配置
kubectl apply -f deploy/k8s/configmap.yaml
kubectl apply -f deploy/k8s/secret.yaml
```

> **安全提示**: Secret 中的占位值仅用于模板演示, 生产部署前 **必须** 替换为真实凭证。建议使用 Sealed Secrets 或 External Secrets Operator 管理生产凭证。

### Step 3: 部署核心服务

```bash
# 部署所有核心服务的 Deployment 和 Service
kubectl apply -f deploy/k8s/core-services/gateway-deployment.yaml
kubectl apply -f deploy/k8s/core-services/gateway-service.yaml
kubectl apply -f deploy/k8s/core-services/order-deployment.yaml
kubectl apply -f deploy/k8s/core-services/order-service.yaml
kubectl apply -f deploy/k8s/core-services/payment-deployment.yaml
kubectl apply -f deploy/k8s/core-services/payment-service.yaml
kubectl apply -f deploy/k8s/core-services/ai-deployment.yaml
kubectl apply -f deploy/k8s/core-services/ai-service.yaml
kubectl apply -f deploy/k8s/core-services/copyright-deployment.yaml
kubectl apply -f deploy/k8s/core-services/copyright-service.yaml

# 或使用通配符
kubectl apply -f deploy/k8s/core-services/*-deployment.yaml
kubectl apply -f deploy/k8s/core-services/*-service.yaml
```

### Step 4: 部署 HPA

```bash
kubectl apply -f deploy/k8s/core-services/gateway-hpa.yaml
kubectl apply -f deploy/k8s/core-services/order-hpa.yaml
kubectl apply -f deploy/k8s/core-services/ai-hpa.yaml
```

### Step 5: 部署闲时缩容 CronJob

```bash
kubectl apply -f deploy/k8s/core-services/scale-down-cronjob.yaml
```

### Step 6: 部署轻量级服务 (可选)

```bash
kubectl apply -f deploy/k8s/lite-services/community-deployment.yaml
kubectl apply -f deploy/k8s/lite-services/academy-deployment.yaml
```

### Step 7: 部署 Ingress

```bash
# 1. 替换 TLS 证书 (生产环境)
# 使用 cert-manager 自动管理:
# kubectl apply -f deploy/k8s/cert-manager-clusterissuer.yaml

# 或手动创建 Secret:
kubectl create secret tls tailor-is-tls-secret \
  --cert=path/to/tls.crt \
  --key=path/to/tls.key \
  -n tailor-is-prod

# 2. 应用 Ingress 配置
kubectl apply -f deploy/k8s/ingress.yaml
```

### Step 8: 验证部署

```bash
# 检查所有 Pod 状态
kubectl get pods -n tailor-is-prod -o wide

# 检查 HPA 状态
kubectl get hpa -n tailor-is-prod

# 检查 Service
kubectl get svc -n tailor-is-prod

# 检查 Ingress
kubectl get ingress -n tailor-is-prod

# 检查 CronJob
kubectl get cronjob -n tailor-is-prod
```

---

## HPA 弹性伸缩配置

### 各服务 HPA 策略

| 服务 | 最小副本 | 最大副本 | CPU 目标 | 扩容策略 | 缩容稳定窗口 |
|------|---------|---------|---------|---------|------------|
| **core-gateway** | 2 | 10 | 70% | 每 60s 最多加 2 Pod 或 50% | 300s |
| **order** | 2 | 8 | 75% | 每 60s 最多加 2 Pod 或 50% | 300s |
| **ai** | 1 | 5 | 80% | 每 120s 最多加 1 Pod 或 100% | 600s |

### 设计考量

- **core-gateway (70% CPU)**: 作为流量入口, 设置较低的 CPU 目标以预留缓冲空间, 快速响突发流量
- **order (75% CPU)**: 订单服务涉及数据库操作, 设置中等 CPU 目标, 平衡性能和成本
- **ai (80% CPU)**: AI 服务计算密集, 单次任务耗时长, 设置较高 CPU 目标。缩容稳定窗口设为 600s (10 分钟), 避免频繁伸缩导致正在运行的任务中断

### 查看 HPA 实时状态

```bash
# 持续监控 HPA
kubectl get hpa -n tailor-is-prod -w

# 查看 HPA 详情和事件
kubectl describe hpa core-gateway-hpa -n tailor-is-prod

# 查看 Pod CPU 使用率
kubectl top pods -n tailor-is-prod
```

### 自定义扩缩容策略

如需调整 HPA 参数:

```bash
# 编辑 HPA
kubectl edit hpa core-gateway-hpa -n tailor-is-prod

# 或直接 patch
kubectl patch hpa order-hpa -n tailor-is-prod \
  -p '{"spec":{"maxReplicas":12}}'
```

### 多指标 HPA (进阶)

当前 HPA 使用 CPU 作为单一伸缩指标。如需更精细的伸缩策略, 可扩展为多指标:

```yaml
metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  # 需要 custom metrics adapter (prometheus-adapter)
  - type: Pods
    pods:
      metric:
        name: http_requests_per_second
      target:
        type: AverageValue
        averageValue: "100"
```

---

## 闲时缩容策略

### 策略说明

系统通过 CronJob 在闲时自动缩减非核心服务的副本数, 节省集群资源。

### 服务优先级

| 优先级 | 服务 | 闲时行为 |
|--------|------|---------|
| **P0 - 始终在线** | core-gateway, order | 由 HPA 管理, 不手动干预 |
| **P1 - 最小保留** | payment, copyright | 缩容至 1 副本 |
| **P2 - 最小保留** | ai | 缩容至 1 副本 |
| **P3 - Serverless** | community, academy | 建议迁移至 Serverless, K8s 为降级方案 |

### CronJob 调度时间

```
# 缩容: 每天 00:00 北京时间 (UTC 16:00)
schedule: "0 16 * * *"

# 扩容: 每天 06:00 北京时间 (UTC 22:00)
schedule: "0 22 * * *"
```

> **注意**: 如果你的 Kubernetes 集群版本 >= 1.27, 可以在 CronJob spec 中添加 `timeZone: "Asia/Shanghai"` 以直接使用本地时间。

### 手动触发缩容/扩容

```bash
# 手动缩容到闲时状态
kubectl scale deployment payment --replicas=1 -n tailor-is-prod
kubectl scale deployment copyright --replicas=1 -n tailor-is-prod
kubectl scale deployment ai --replicas=1 -n tailor-is-prod

# 手动恢复到正常状态
kubectl scale deployment payment --replicas=2 -n tailor-is-prod
kubectl scale deployment copyright --replicas=2 -n tailor-is-prod
kubectl scale deployment ai --replicas=1 -n tailor-is-prod

# 手动触发 CronJob (立即执行)
kubectl create job --from=cronjob/scale-down-idle-hours manual-scale-down -n tailor-is-prod
kubectl create job --from=cronjob/scale-up-peak-hours manual-scale-up -n tailor-is-prod
```

### 查看 CronJob 执行日志

```bash
# 查看最近的 Job 执行
kubectl get jobs -n tailor-is-prod -l job-name=scale-down-idle-hours

# 查看 Job 日志
kubectl logs job/<job-name> -n tailor-is-prod
```

---

## 从 Docker Compose 迁移到 K8s

### 迁移概览

```
Docker Compose              →     Kubernetes
──────────────────────────────────────────────────────
docker-compose.yml          →     deploy/k8s/*.yaml
environment variables       →     ConfigMap / Secret
ports mapping               →     Service (ClusterIP)
depends_on                  →     readinessProbe + startupProbe
restart: always             →     restartPolicy: Always
volumes                     →     PersistentVolumeClaim
networks                    →     Namespace + Service DNS
```

### 迁移步骤

#### 1. 准备基础设施

在 K8s 集群中部署或连接已有基础设施:

- **Nacos**: 服务注册与配置中心
- **Redis**: 缓存层 (standalone 或 cluster)
- **MySQL**: 持久化存储 (各服务独立数据库)
- **RabbitMQ**: 实时消息队列
- **RocketMQ**: 批量异步消息队列 (AI 服务)
- **Sentinel**: 限流熔断

> 基础设施建议部署在独立命名空间 `tailor-is-infra`, 通过 Service DNS 跨命名空间访问。

#### 2. 构建 Docker 镜像

```bash
# 在各服务模块目录执行 Maven 构建
cd tailor-is-core-gateway && mvn clean package -DskipTests
# ... 对所有服务执行

# 构建 Docker 镜像 (需先创建 Dockerfile)
docker build -t tailor-is-core-gateway:1.0.0 -f Dockerfile .
docker build -t tailor-is-order:1.0.0 -f Dockerfile .
# ... 对所有服务执行

# 推送到镜像仓库
docker tag tailor-is-core-gateway:1.0.0 registry.example.com/tailor-is-core-gateway:1.0.0
docker push registry.example.com/tailor-is-core-gateway:1.0.0
```

#### 3. 更新 K8s 配置中的镜像地址

编辑各 `*-deployment.yaml` 文件中的 `image` 字段:

```yaml
image: registry.example.com/tailor-is-core-gateway:1.0.0
```

或使用环境变量:

```bash
export REGISTRY=registry.example.com
export IMAGE_TAG=1.0.0
envsubst < deploy/k8s/core-services/gateway-deployment.yaml | kubectl apply -f -
```

#### 4. 替换配置值

- 编辑 `configmap.yaml`, 将占位地址替换为实际 K8s Service DNS 地址
- 编辑 `secret.yaml`, 将占位密码替换为真实值 (base64 编码)

#### 5. 按顺序部署

```bash
# 1. 命名空间
kubectl apply -f deploy/k8s/namespace.yaml

# 2. 配置
kubectl apply -f deploy/k8s/configmap.yaml
kubectl apply -f deploy/k8s/secret.yaml

# 3. 核心服务
kubectl apply -f deploy/k8s/core-services/

# 4. 轻量服务
kubectl apply -f deploy/k8s/lite-services/

# 5. Ingress
kubectl apply -f deploy/k8s/ingress.yaml
```

#### 6. 验证服务间通信

```bash
# 从 core-gateway Pod 内部测试到 order 服务的连接
kubectl exec -it deploy/core-gateway -n tailor-is-prod -- curl -s http://order:8103/actuator/health

# 测试 Nacos 连接
kubectl exec -it deploy/core-gateway -n tailor-is-prod -- curl -s http://nacos.tailor-is-infra.svc.cluster.local:8848/nacos/

# 测试 Redis 连接
kubectl exec -it deploy/order -n tailor-is-prod -- nc -zv redis-master.tailor-is-infra.svc.cluster.local 6379
```

#### 7. 灰度切换流量

```bash
# 方案 1: DNS 切换
# 将 api.tailoris.com 的 DNS 记录指向 K8s Ingress IP

# 方案 2: Ingress 权重路由 (需要 nginx-ingress annotation)
# 逐步增加 K8s 流量比例, 观察指标

# 方案 3: 蓝绿部署
# 先部署新版本, 确认健康后切换 Ingress 后端
```

### 关键差异对照表

| 特性 | Docker Compose | Kubernetes |
|------|---------------|------------|
| 服务发现 | DNS (docker network) | CoreDNS (kube-dns) |
| 负载均衡 | 无 (需外部 LB) | Service (kube-proxy) |
| 健康检查 | healthcheck (docker) | liveness/readiness/startupProbe |
| 弹性伸缩 | 手动 docker-compose up --scale | HPA (自动) |
| 配置管理 | .env 文件 | ConfigMap + Secret |
| 日志收集 | docker logs | Fluentd/FluentBit + ELK/Loki |
| 监控 | Docker stats | Prometheus + Grafana |
| 网络隔离 | docker networks | Namespace + NetworkPolicy |
| 存储 | docker volumes | PersistentVolume + PVC |

---

## 监控与告警

### Prometheus 采集

各服务已通过 Spring Boot Actuator 暴露 Prometheus 指标。Prometheus 通过 `kubernetes_sd_configs` 自动发现 Pod。

```bash
# 确认 metrics 端点可访问
kubectl exec -it deploy/core-gateway -n tailor-is-prod -- curl -s http://localhost:8080/actuator/prometheus | head -20
```

### Grafana 仪表盘

预置的 Grafana 仪表盘位于 `deploy/monitoring/`:
- `core-services-dashboard.json` - 核心服务监控
- `lite-services-dashboard.json` - 轻量级服务监控
- `resource-utilization-dashboard.json` - 资源利用率监控

### 告警规则

预置的 Prometheus 告警规则位于 `deploy/monitoring/core-alerts.yml`:
- 核心服务宕机 (critical)
- P99 响应时间 > 1s (warning)
- 5xx 错误率 > 5% (critical)
- JVM 堆内存 > 85% (warning)
- 支付成功率 < 95% (critical)
- 自动扩容触发 (warning)

---

## 故障排查

### Pod 无法启动

```bash
# 查看 Pod 状态
kubectl describe pod <pod-name> -n tailor-is-prod

# 查看 Pod 日志
kubectl logs <pod-name> -n tailor-is-prod

# 查看前一个容器的日志 (崩溃重启场景)
kubectl logs <pod-name> -n tailor-is-prod --previous
```

### HPA 不生效

```bash
# 确认 metrics-server 运行正常
kubectl top nodes
kubectl top pods -n tailor-is-prod

# 查看 HPA 事件
kubectl describe hpa core-gateway-hpa -n tailor-is-prod
```

### CronJob 执行失败

```bash
# 查看 CronJob 状态
kubectl get cronjob -n tailor-is-prod

# 查看最近一次执行
kubectl get jobs -n tailor-is-prod --sort-by='.status.startTime'

# 查看 Job 日志
kubectl logs job/<job-name> -n tailor-is-prod
```

### 服务间连接失败

```bash
# 检查 Service 是否存在
kubectl get svc -n tailor-is-prod

# 检查 Endpoint 是否正确
kubectl get endpoints -n tailor-is-prod

# 在 Pod 内部执行 DNS 解析
kubectl exec -it deploy/core-gateway -n tailor-is-prod -- nslookup order.tailor-is-prod.svc.cluster.local
```

---

## 生产环境 Checklist

- [ ] 所有 Secret 中的占位值已替换为真实凭证
- [ ] TLS 证书已配置并验证 (使用 Let's Encrypt 或企业证书)
- [ ] 镜像版本已固定 (不使用 `latest` tag)
- [ ] `imagePullPolicy` 已设置为 `Always` (生产) 或 `IfNotPresent` (稳定版本)
- [ ] Resource limits/requests 已根据实际压测数据调优
- [ ] HPA 的 min/max replicas 已根据业务峰值验证
- [ ] CronJob 调度时间已根据业务峰谷验证
- [ ] NetworkPolicy 已配置 (限制服务间访问)
- [ ] PodDisruptionBudget 已配置 (保障滚动更新可用性)
- [ ] 备份策略已配置 (数据库定时备份)
- [ ] 日志采集已配置 (ELK/Loki)
- [ ] 告警通知已配置 (钉钉/企业微信/邮件)
- [ ] 灾难恢复演练已完成

---

## 附录: 资源配额建议

### 核心服务

| 服务 | CPU Request | CPU Limit | Memory Request | Memory Limit |
|------|------------|-----------|----------------|--------------|
| core-gateway | 500m | 2000m | 512Mi | 1Gi |
| order | 500m | 2000m | 512Mi | 1Gi |
| payment | 500m | 2000m | 512Mi | 1Gi |
| ai | 1000m | 4000m | 1Gi | 2Gi |
| copyright | 250m | 1000m | 512Mi | 1Gi |

### 轻量级服务

| 服务 | CPU Request | CPU Limit | Memory Request | Memory Limit |
|------|------------|-----------|----------------|--------------|
| community | 100m | 500m | 256Mi | 512Mi |
| academy | 100m | 500m | 256Mi | 512Mi |

### 满负载资源估算 (最大副本)

- **核心服务满负载**: 约 33 CPU cores + 17.5 GiB 内存
- **轻量服务**: 约 0.5 CPU cores + 0.5 GiB 内存
- **建议集群规模**: 至少 3 个节点, 每个节点 8C16G 或 4C32G
