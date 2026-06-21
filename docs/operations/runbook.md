# Tailor IS 运维应急手册 (Runbook)

> 版本：v1.0 | 更新时间：2026-06-20 | 适用环境：生产环境 (tailor-is-prod)

---

## 1. 服务状态检查

### 1.1 集群健康检查
```bash
# 检查所有 Pod 状态
kubectl get pods -n tailor-is-prod

# 检查所有 Service 状态
kubectl get svc -n tailor-is-prod

# 检查 Deployment 状态
kubectl get deployment -n tailor-is-prod

# 检查 HPA 状态
kubectl get hpa -n tailor-is-prod

# 检查 Ingress 配置
kubectl get ingress -n tailor-is-prod
```

### 1.2 资源使用检查
```bash
# 查看 Pod 资源使用
kubectl top pods -n tailor-is-prod

# 查看节点资源使用
kubectl top nodes

# 查看资源配额
kubectl describe resourcequota -n tailor-is-prod
```

### 1.3 日志查看
```bash
# 查看指定 Pod 实时日志
kubectl logs -f <pod-name> -n tailor-is-prod

# 查看最近 100 行日志
kubectl logs --tail=100 <pod-name> -n tailor-is-prod

# 查看上一个崩溃实例的日志
kubectl logs <pod-name> -n tailor-is-prod --previous

# 查看所有网关日志
kubectl logs -f -l app=core-gateway -n tailor-is-prod
```

---

## 2. 常见故障处理

### 2.1 服务无法启动

**症状**：Pod 处于 `CrashLoopBackOff`、`Error` 或 `ImagePullBackOff` 状态

**排查步骤**：
```bash
# 1. 查看 Pod 详细状态
kubectl describe pod <pod-name> -n tailor-is-prod

# 2. 检查 Events 中的错误信息
# 关注: FailedMount, FailedScheduling, Liveness probe failed

# 3. 检查 Secret/ConfigMap 是否存在
kubectl get secret -n tailor-is-prod
kubectl get configmap -n tailor-is-prod

# 4. 查看容器启动日志
kubectl logs <pod-name> -n tailor-is-prod --previous
```

**常见原因与解决方案**：

| 错误 | 原因 | 解决方案 |
|------|------|----------|
| `ImagePullBackOff` | 镜像拉取失败 | 检查镜像仓库凭证、镜像名称和标签 |
| `CrashLoopBackOff` | 应用启动失败 | 检查启动日志，确认配置正确 |
| `CreateContainerConfigError` | ConfigMap/Secret 不存在或格式错误 | 检查并修复 ConfigMap/Secret |
| `OOMKilled` | 内存不足 | 增加内存限制或排查内存泄漏 |

### 2.2 数据库连接失败

**症状**：服务日志中出现 `CommunicationsException`、`Connection refused`、`SQLException`

**排查步骤**：
```bash
# 1. 检查 MySQL Pod 状态
kubectl get pods -l app=mysql -n tailor-is-prod

# 2. 检查 MySQL Service 和 Endpoint
kubectl describe svc mysql -n tailor-is-prod
kubectl get endpoints mysql -n tailor-is-prod

# 3. 检查 ConfigMap 中的数据库连接 URL
kubectl describe configmap tailor-is-config -n tailor-is-prod

# 4. 测试数据库连通性
kubectl run mysql-test --rm -it --image=mysql:8.0 --restart=Never -- \
  mysql -h <mysql-host> -u <user> -p<password> -e "SELECT 1"
```

**解决方案**：
- 检查 MySQL 连接 URL、用户名、密码是否正确
- 检查数据库连接池配置（最大连接数、超时）
- 如使用分库分表，检查 ShardingSphere 配置
- 如使用 TiDB，检查 TiDB 集群状态

### 2.3 Redis 连接失败

**症状**：服务日志中出现 `RedisConnectionException`、`RedisConnectionFailureException`

**排查步骤**：
```bash
# 1. 检查 Redis Pod 状态
kubectl get pods -l app=redis -n tailor-is-prod

# 2. 检查 Redis 连接
kubectl run redis-test --rm -it --image=redis:7-alpine --restart=Never -- \
  redis-cli -h <redis-host> -a <password> PING
```

**解决方案**：
- 检查 Redis 密码配置
- 重启 Redis Pod：`kubectl rollout restart deployment/redis -n tailor-is-prod`
- 检查 Redis 内存使用，必要时清理或扩容

### 2.4 Nacos 服务发现失败

**症状**：服务注册失败、服务发现异常、`NacosException`

**排查步骤**：
```bash
# 1. 检查 Nacos Pod 状态
kubectl get pods -l app=nacos -n tailor-is-prod

# 2. 检查 Nacos 控制台
# 访问 http://<nacos-host>:8848/nacos

# 3. 检查服务注册信息
curl http://<nacos-host>:8848/nacos/v1/ns/service/list?namespaceId=tailor-is
```

**解决方案**：
- 检查 Nacos 认证 Token 是否正确
- 重启 Nacos：`kubectl rollout restart deployment/nacos -n tailor-is-prod`
- 检查 `bootstrap.yml` 中的 Nacos 配置

### 2.5 支付回调失败

**症状**：支付回调返回 500，订单状态未更新

**排查步骤**：
```bash
# 1. 检查支付服务日志
kubectl logs -f -l app=payment -n tailor-is-prod | grep -i callback

# 2. 检查回调 URL 配置
# 支付宝: 检查应用配置中的 notify_url 和 return_url

# 3. 检查签名验证
# 日志中搜索 "sign" 关键词
```

**解决方案**：
- 验证回调 URL 可被外部访问
- 检查支付宝/微信支付证书配置
- 检查签名算法和密钥是否匹配
- 手动查询支付状态并更新订单

### 2.6 AI 制版服务超时

**症状**：版型生成超时、返回 504

**排查步骤**：
```bash
# 1. 检查 AI 服务 Pod 状态
kubectl get pods -l app=ai -n tailor-is-prod

# 2. 检查 GPU 节点资源
kubectl describe nodes | grep -A 10 "nvidia.com/gpu"

# 3. 检查 AI 模型服务日志
kubectl logs -f -l app=ai -n tailor-is-prod

# 4. 检查消息队列积压
kubectl logs -f -l app=ai -n tailor-is-prod | grep "queue"
```

**解决方案**：
- 检查 GPU 节点资源是否充足
- 检查 AI 模型服务（如 Triton Inference Server）是否正常运行
- 调整超时配置（Gateway timeout、AI 服务超时）
- 检查 RocketMQ 消息队列是否积压

### 2.7 网关异常

**症状**：所有请求返回 502/503，路由失败

**排查步骤**：
```bash
# 1. 检查网关 Pod 状态
kubectl get pods -l app=core-gateway -n tailor-is-prod
kubectl get pods -l app=lite-gateway -n tailor-is-prod

# 2. 检查网关日志
kubectl logs -f -l app=core-gateway -n tailor-is-prod

# 3. 检查网关路由配置
kubectl logs -l app=core-gateway -n tailor-is-prod | grep "route"
```

**解决方案**：
- 重启网关 Pod
- 检查 Nacos 中的路由配置
- 检查 Sentinel 限流规则是否误触发
- 检查后端服务是否正常注册

---

## 3. 扩容与缩容

### 3.1 手动扩容
```bash
# 扩容指定 Deployment
kubectl scale deployment <deployment-name> --replicas=<N> -n tailor-is-prod

# 示例：扩容网关服务
kubectl scale deployment core-gateway --replicas=5 -n tailor-is-prod

# 示例：扩容订单服务
kubectl scale deployment order --replicas=3 -n tailor-is-prod
```

### 3.2 HPA 自动扩缩容
```bash
# 查看 HPA 状态
kubectl get hpa -n tailor-is-prod

# 查看 HPA 详情
kubectl describe hpa <hpa-name> -n tailor-is-prod

# 创建 HPA（示例）
kubectl autoscale deployment order --cpu-percent=70 --min=2 --max=10 -n tailor-is-prod
```

### 3.3 定时缩容（低峰期）
```bash
# 查看定时缩容 CronJob
kubectl get cronjob -n tailor-is-prod

# 手动触发缩容
kubectl create job --from=cronjob/scale-down scale-down-manual -n tailor-is-prod
```

---

## 4. 数据备份与恢复

### 4.1 MySQL 备份
```bash
# 备份整个数据库
mysqldump -h <mysql-host> -u <user> -p<password> \
  --single-transaction --routines --triggers \
  --all-databases > backup_$(date +%Y%m%d_%H%M%S).sql

# 备份单个数据库
mysqldump -h <mysql-host> -u <user> -p<password> \
  --single-transaction tailor_is > tailor_is_backup_$(date +%Y%m%d).sql

# 压缩备份
mysqldump -h <mysql-host> -u <user> -p<password> tailor_is | \
  gzip > tailor_is_backup_$(date +%Y%m%d).sql.gz
```

### 4.2 MySQL 恢复
```bash
# 恢复数据库
mysql -h <mysql-host> -u <user> -p<password> tailor_is < backup.sql

# 从压缩文件恢复
gunzip < backup.sql.gz | mysql -h <mysql-host> -u <user> -p<password> tailor_is
```

### 4.3 Redis 备份
```bash
# 手动触发 RDB 快照
redis-cli -h <redis-host> -a <password> BGSAVE

# 检查备份状态
redis-cli -h <redis-host> -a <password> LASTSAVE

# 备份 RDB 文件
kubectl cp tailor-is-prod/<redis-pod>:/data/dump.rdb ./redis_backup_$(date +%Y%m%d).rdb
```

### 4.4 Redis 恢复
```bash
# 停止 Redis
kubectl scale deployment redis --replicas=0 -n tailor-is-prod

# 复制备份文件到 Pod
kubectl cp ./redis_backup.rdb tailor-is-prod/<redis-pod>:/data/dump.rdb

# 启动 Redis
kubectl scale deployment redis --replicas=1 -n tailor-is-prod
```

### 4.5 备份计划
| 备份类型 | 频率 | 保留策略 |
|----------|------|----------|
| MySQL 全量备份 | 每日凌晨 2:00 | 保留 7 天 |
| MySQL 增量备份 | 每小时 | 保留 24 小时 |
| Redis 快照 | 每日凌晨 3:00 | 保留 7 天 |
| 配置文件备份 | 每次变更后 | 永久保留 |

---

## 5. 证书与密钥管理

### 5.1 SSL 证书
```bash
# 证书路径
/etc/nginx/ssl/

# 查看证书有效期
openssl x509 -in /etc/nginx/ssl/tailor-is.crt -noout -dates

# 验证证书链
openssl s_client -connect tailor-is.com:443 -showcerts

# 更新 Ingress 证书
kubectl create secret tls tailor-is-tls \
  --cert=tailor-is.crt \
  --key=tailor-is.key \
  -n tailor-is-prod --dry-run=client -o yaml | kubectl apply -f -
```

### 5.2 Kubernetes Secret 更新
```bash
# 更新数据库密码
kubectl create secret generic tailor-is-db-secret \
  --from-literal=username=tailor \
  --from-literal=password=<new-password> \
  -n tailor-is-prod --dry-run=client -o yaml | kubectl apply -f -

# 滚动重启使用新 Secret 的服务
kubectl rollout restart deployment -n tailor-is-prod
```

---

## 6. 日志收集与分析

### 6.1 应用日志
```bash
# 实时查看网关日志
kubectl logs -f -l app=core-gateway -n tailor-is-prod

# 查看指定时间范围的日志
kubectl logs -l app=order --since=1h -n tailor-is-prod

# 导出日志到文件
kubectl logs -l app=payment -n tailor-is-prod --tail=1000 > payment_logs.txt
```

### 6.2 审计日志路径
```
应用审计日志：/var/log/tailor-is/audit/
安全审计日志：/var/log/tailor-is/security/
操作审计日志：/var/log/tailor-is/operation/
```

### 6.3 链路追踪
```bash
# SkyWalking 控制台
# 访问 http://<skywalking-host>:8080

# 查看 Trace ID 对应的链路
# 在日志中搜索 TraceId 字段
```

---

## 7. 紧急联系人与升级流程

| 角色 | 姓名 | 联系方式 | 备用联系方式 |
|------|------|----------|--------------|
| 技术负责人 | [待填写] | [待填写] | [待填写] |
| 运维负责人 | [待填写] | [待填写] | [待填写] |
| 安全负责人 | [待填写] | [待填写] | [待填写] |
| DBA | [待填写] | [待填写] | [待填写] |
| 产品经理 | [待填写] | [待填写] | [待填写] |

### 升级流程
1. **Level 1（15 分钟内）**：运维值班人员初步排查
2. **Level 2（30 分钟内）**：升级至技术负责人
3. **Level 3（1 小时内）**：升级至技术总监/CTO
4. **Level 4（2 小时内）**：启动应急响应小组

### 升级条件
- 核心业务中断超过 10 分钟
- 支付功能异常
- 数据丢失或泄露
- 安全漏洞被利用

---

## 8. 监控告警处理

### 8.1 监控访问
| 服务 | 地址 | 说明 |
|------|------|------|
| Prometheus | `http://<prometheus-host>:9090` | 指标查询与告警规则 |
| Grafana | `http://<grafana-host>:3000` | 仪表盘可视化 |
| Alertmanager | `http://<alertmanager-host>:9093` | 告警管理 |
| SkyWalking | `http://<skywalking-host>:8080` | 链路追踪 |
| Sentinel | `http://<sentinel-host>:8080` | 流量控制面板 |

### 8.2 告警规则

| 告警名称 | 触发条件 | 严重级别 | 处理建议 |
|----------|----------|----------|----------|
| PodDown | Pod 异常退出 | Critical | 立即检查 Pod 状态和日志 |
| HighCPUUsage | CPU > 80% 持续 5 分钟 | Warning | 检查是否需要扩容 |
| HighMemoryUsage | 内存 > 85% 持续 5 分钟 | Warning | 检查内存泄漏，考虑扩容 |
| HighErrorRate | 错误率 > 5% | Critical | 检查错误日志，排查根因 |
| SlowResponse | P99 延迟 > 3s | Warning | 检查慢查询和外部依赖 |
| DiskFull | 磁盘使用 > 90% | Critical | 清理日志或扩容磁盘 |
| CertificateExpiring | 证书 30 天内过期 | Warning | 更新 SSL 证书 |

### 8.3 告警通知渠道
- 钉钉群：Tailor IS 运维告警群
- 飞书群：Tailor IS 技术团队
- 邮件：ops@tailor-is.com

---

## 9. 常用运维命令速查

### 9.1 Kubernetes 常用命令
```bash
# 重启 Deployment
kubectl rollout restart deployment/<name> -n tailor-is-prod

# 查看 Deployment 滚动更新状态
kubectl rollout status deployment/<name> -n tailor-is-prod

# 回滚 Deployment
kubectl rollout undo deployment/<name> -n tailor-is-prod

# 进入 Pod 容器
kubectl exec -it <pod-name> -n tailor-is-prod -- /bin/bash

# 查看 Pod 资源限制
kubectl describe pod <pod-name> -n tailor-is-prod | grep -A 5 "Limits"

# 查看事件
kubectl get events -n tailor-is-prod --sort-by='.lastTimestamp'
```

### 9.2 网络诊断
```bash
# 测试服务连通性
kubectl run net-test --rm -it --image=busybox --restart=Never -- \
  wget -qO- http://<service-name>:<port>/actuator/health

# DNS 解析测试
kubectl run dns-test --rm -it --image=busybox --restart=Never -- \
  nslookup <service-name>.tailor-is-prod.svc.cluster.local
```

### 9.3 Docker Compose（本地环境）
```bash
# 查看服务状态
docker-compose -f docker-compose-services.yml ps

# 重启服务
docker-compose -f docker-compose-services.yml restart <service-name>

# 查看服务日志
docker-compose -f docker-compose-services.yml logs -f <service-name>
```

---

## 10. 灾备切换

### 10.1 数据库主从切换
```bash
# 1. 检查主从同步状态
# 在从库执行: SHOW SLAVE STATUS\G

# 2. 停止主库写入
# 在主库执行: SET GLOBAL read_only = ON;

# 3. 确认从库数据一致
# 在从库执行: SHOW SLAVE STATUS\G
# 确认 Seconds_Behind_Master = 0

# 4. 提升从库为主库
# 在从库执行: STOP SLAVE; RESET SLAVE ALL;
# 在从库执行: SET GLOBAL read_only = OFF;

# 5. 更新应用连接配置
kubectl edit configmap tailor-is-config -n tailor-is-prod
```

### 10.2 多活切换
```bash
# 切换流量到备用集群
# 修改 DNS 解析或负载均衡配置

# 验证备用集群服务状态
kubectl get pods --context=backup-cluster -n tailor-is-prod
```

---

## 附录：服务端口清单

| 服务 | 端口 | 协议 |
|------|------|------|
| Core Gateway | 8080 | HTTP |
| Lite Gateway | 8081 | HTTP |
| User Service | 8082 | HTTP |
| Product Service | 8083 | HTTP |
| Order Service | 8084 | HTTP |
| Payment Service | 8085 | HTTP |
| AI Service | 8086 | HTTP |
| Copyright Service | 8087 | HTTP |
| MySQL | 3306 | TCP |
| Redis | 6379 | TCP |
| Nacos | 8848 | HTTP |
| RocketMQ | 9876 | TCP |
| Sentinel | 8719 | HTTP |