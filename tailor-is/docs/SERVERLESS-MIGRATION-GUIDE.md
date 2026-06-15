# Tailor IS Serverless 迁移指南

> 社区服务 (tailor-is-community) 与 学堂服务 (tailor-is-academy) 迁移到 Serverless 架构的完整指南。

---

## 目录

1. [迁移概述](#1-迁移概述)
2. [架构设计](#2-架构设计)
3. [迁移准备](#3-迁移准备)
4. [阿里云函数计算 (FC) 部署](#4-阿里云函数计算-fc-部署)
5. [腾讯云 Serverless 云函数 (SCF) 部署](#5-腾讯云-serverless-云函数-scf-部署)
6. [冷启动优化策略](#6-冷启动优化策略)
7. [配置说明](#7-配置说明)
8. [运维与监控](#8-运维与监控)
9. [回滚方案](#9-回滚方案)
10. [常见问题](#10-常见问题)

---

## 1. 迁移概述

### 1.1 迁移目标

将非核心服务（社区、学堂）从传统的容器部署迁移到 Serverless 架构，实现：
- **按需计费**：闲时零实例，降低运营成本
- **自动弹性**：秒级扩容应对流量洪峰
- **简化运维**：无需管理服务器基础设施

### 1.2 迁移范围

| 服务 | 当前端口 | 目标运行时 | 优先级 |
|------|---------|-----------|--------|
| tailor-is-community | 8108 | Aliyun FC 3.0 / Tencent SCF | 高 |
| tailor-is-academy | 8112 | Aliyun FC 3.0 / Tencent SCF | 高 |

### 1.3 不在迁移范围内

核心服务（用户、商户、订单、支付、网关等）保持原有 K8s 容器部署不变。

---

## 2. 架构设计

### 2.1 部署模式

采用 **Custom Container（自定义容器）** 模式：
- 将整个 Spring Boot 应用打包为 Docker 镜像
- 由函数计算平台负责拉取镜像、启动容器、路由 HTTP 请求
- 通过预留实例 + 弹性伸缩实现冷启动优化

### 2.2 架构变更

```
迁移前:
  API Gateway → K8s Ingress → tailor-is-community (Pod)
  API Gateway → K8s Ingress → tailor-is-academy (Pod)

迁移后:
  API Gateway → Aliyun FC HTTP Trigger → Custom Container (Spring Boot)
  API Gateway → Tencent SCF HTTP Trigger → Custom Container (Spring Boot)
```

### 2.3 关键设计决策

- **保留 Spring Boot 全量框架**：通过 `custom-container` 运行，无需大幅重构代码
- **关闭服务注册**：Serverless 环境下 Nacos 服务注册无意义，通过 `application-serverless.yml` 关闭
- **懒加载优先**：启用 `spring.main.lazy-initialization=true` 加速冷启动
- **连接池瘦身**：Druid 初始连接从 5 降至 2，最大连接从 20 降至 10

---

## 3. 迁移准备

### 3.1 前置条件

- [ ] 已开通阿里云函数计算 (FC) 或腾讯云 Serverless 云函数 (SCF)
- [ ] 已配置 VPC 网络（函数需访问内网 RDS/Redis）
- [ ] 已创建容器镜像仓库 (ACR / TCR)
- [ ] MySQL、Redis 等基础设施保持不变，确保函数 VPC 可达

### 3.2 配置文件说明

```
tailor-is-community/
├── src/main/resources/
│   ├── application.yml              # 默认配置 (容器部署)
│   ├── application-serverless.yml   # Serverless 配置 (懒加载、精简组件)
│   └── serverless-config.yml        # FC 函数配置文档 (参考用)
└── src/main/java/.../serverless/
    └── CommunityFunctionHandler.java # Serverless 入口 Handler

deploy/serverless/community/
└── template.yaml                     # SAM 部署模板
```

### 3.3 构建 Docker 镜像

```dockerfile
# Dockerfile (示例)
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 复制 Spring Boot Fat Jar
COPY target/tailor-is-community-1.0.0.jar app.jar

# Serverless 启动脚本
ENV SERVER_PORT=9000
ENV SPRING_PROFILES_ACTIVE=serverless
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+TieredCompilation -XX:TieredStopAtLevel=1"

EXPOSE 9000

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

```bash
# 构建并推送镜像
cd tailor-is-community
mvn clean package -DskipTests
docker build -t registry.cn-shanghai.aliyuncs.com/tailoris/community:latest .
docker push registry.cn-shanghai.aliyuncs.com/tailoris/community:latest
```

---

## 4. 阿里云函数计算 (FC) 部署

### 4.1 使用 SAM 模板部署 (推荐)

```bash
# 安装 Serverless Devs 工具
npm install @serverless-devs/s -g

# 配置访问密钥
s config add --AccountID <account-id> --AccessKeyID <ak> --AccessKeySecret <sk>

# 使用 SAM 模板部署社区服务
cd deploy/serverless/community
s deploy --template-file template.yaml \
  --region cn-shanghai \
  --parameter-overrides \
    VpcId=vpc-xxx \
    VSwitchId=vsw-xxx \
    SecurityGroupId=sg-xxx \
    NasServerAddr=xxx.cn-shanghai.nas.aliyuncs.com
```

### 4.2 使用控制台部署

1. 登录 [阿里云函数计算控制台](https://fcnext.console.aliyun.com/)
2. 创建服务 `tailor-is-community-service`
3. 创建函数 `community-api`，选择 **自定义容器** 运行时
4. 配置镜像地址、VPC、内存、超时
5. 创建 HTTP 触发器
6. 配置预留实例数 ≥ 1

### 4.3 环境变量

| 变量名 | 说明 | 示例 |
|--------|------|------|
| `SPRING_PROFILES_ACTIVE` | Spring Profile | `serverless` |
| `SERVER_PORT` | 服务端口 | `9000` |
| `MYSQL_HOST` | MySQL 地址 | `rm-xxx.mysql.rds.aliyuncs.com` |
| `MYSQL_PASSWORD` | MySQL 密码 | `<your-password>` |
| `REDIS_HOST` | Redis 地址 | `r-xxx.redis.rds.aliyuncs.com` |
| `REDIS_PASSWORD` | Redis 密码 | `<your-password>` |

### 4.4 预留实例配置

| 时段 | 预留实例数 | 说明 |
|------|-----------|------|
| 全天 | 1 | 基础保活，消除首次访问冷启动 |
| 工作日 09:00-12:00 | 3 | 早高峰扩容 |
| 全周 19:00-23:00 | 3 | 晚高峰扩容 |

---

## 5. 腾讯云 Serverless 云函数 (SCF) 部署

### 5.1 使用 Serverless Framework 部署

```yaml
# serverless.yml
component: tencent-scf
name: tailor-is-community

inputs:
  name: community-api
  namespace: tailor-is
  src:
    src: ./target/tailor-is-community-1.0.0.jar
  handler: index.main_handler
  runtime: CustomRuntime
  timeout: 60
  memorySize: 1024
  vpcConfig:
    vpcId: vpc-xxx
    subnetId: subnet-xxx
  environment:
    variables:
      SPRING_PROFILES_ACTIVE: serverless
      SERVER_PORT: 9000
  layers:
    - name: Java17Runtime
      version: 1
```

```bash
# 安装 Serverless Framework
npm install -g serverless

# 配置腾讯云密钥
export TENCENT_SECRET_ID=<your-secret-id>
export TENCENT_SECRET_KEY=<your-secret-key>

# 部署
sls deploy --target deploy/serverless/community
```

### 5.2 使用容器镜像部署

1. 将 Docker 镜像推送到腾讯云容器镜像服务 (TCR)
2. 在 SCF 控制台创建函数，选择 **容器镜像** 运行时
3. 配置 VPC、环境变量、HTTP 触发器
4. 配置 **预置并发** ≥ 1

### 5.3 预置并发配置

腾讯云 SCF 的预置并发等价于阿里云 FC 的预留实例：

```bash
# 通过 API 配置预置并发
scf PutProvisionedConcurrencyConfig \
  --function-name community-api \
  --namespace tailor-is \
  --qualifier LATEST \
  --version-weight 1.0 \
  --provisioned-concurrency-1
```

---

## 6. 冷启动优化策略

### 6.1 代码层优化

| 优化项 | 配置 | 效果 |
|--------|------|------|
| 懒加载 | `spring.main.lazy-initialization=true` | Bean 按需创建，减少启动 Bean 数量 |
| CDS (Class Data Sharing) | `-XX:SharedArchiveFile=app-cds.jsa` | 减少类加载时间 20-40% |
| Tiered Compilation | `-XX:TieredStopAtLevel=1` | 跳过 JIT 优化层级，加速启动 |
| G1 GC | `-XX:+UseG1GC` | 更可控的 GC 停顿 |

### 6.2 框架层优化

| 优化项 | 配置 | 效果 |
|--------|------|------|
| 关闭 Nacos 注册 | `spring.cloud.nacos.discovery.register-enabled=false` | 消除 Nacos 连接等待 |
| 关闭 Nacos 配置 | `spring.cloud.nacos.config.enabled=false` | 消除配置拉取延迟 |
| 关闭 RabbitMQ | 排除 `RabbitAutoConfiguration` | 消除 MQ 连接初始化 |
| 关闭 API 文档 | `knife4j.enable=false` | 减少 Bean 初始化 |
| Druid 瘦身 | `initial-size=2, max-active=10` | 减少数据库连接建立时间 |

### 6.3 基础设施层优化

| 策略 | 说明 | 适用场景 |
|------|------|---------|
| 预留实例 (Reserved Instance) | 始终保持 ≥1 个实例存活 | 所有场景 |
| 定时预留 (Scheduled Provisioning) | 高峰前预扩容 | 可预测的流量模式 |
| 快照加速 (Snapshot Acceleration) | 利用函数计算快照功能 | 大镜像场景 |
| Provisioned Concurrency | 腾讯云预置并发 | 腾讯云场景 |

### 6.4 预期冷启动时间

| 优化阶段 | 冷启动时间 (P99) |
|---------|-----------------|
| 未优化 (默认 Spring Boot) | 15-30s |
| 启用懒加载 | 8-15s |
| 懒加载 + 组件精简 | 5-8s |
| + 预留实例 (生产推荐) | **<1s** (热启动) |

---

## 7. 配置说明

### 7.1 serverless-config.yml

位于各服务的 `src/main/resources/serverless-config.yml`，记录了完整的函数配置参数，包括：
- 内存、超时、磁盘大小
- 环境变量
- VPC 配置
- 触发器配置
- 预留实例策略
- 弹性伸缩策略

此文件作为配置参考文档，实际部署以 SAM 模板 (`deploy/serverless/*/template.yaml`) 为准。

### 7.2 application-serverless.yml

位于各服务的 `src/main/resources/application-serverless.yml`，是 Spring Boot 的 Serverless Profile 配置：
- 懒加载
- 精简数据源连接池
- 关闭非必要组件 (Nacos/RabbitMQ/API文档)
- 精简日志

### 7.3 FunctionHandler

位于各服务的 `src/main/java/.../serverless/*FunctionHandler.java`：
- 实现 Spring Boot 在 Serverless 容器中的入口适配
- 通过 `DispatcherServlet` 转发 HTTP 请求
- 使用延迟初始化避免重复启动

---

## 8. 运维与监控

### 8.1 日志查看

```bash
# 阿里云 FC - 查看函数日志
s logs -f community-api --tail

# 通过 SLS 查询
* | select request_id, duration, status from log order by time desc limit 100
```

### 8.2 关键监控指标

| 指标 | 告警阈值 | 说明 |
|------|---------|------|
| 冷启动次数 | > 10/min | 检查预留实例配置 |
| 函数执行时长 P99 | > 5s | 可能存在慢查询 |
| 错误率 | > 1% | 检查数据库连接、Redis 连通性 |
| 并发实例数 | 接近 maxInstances | 需要调大弹性上限 |
| 内存使用率 | > 80% | 需要增大 memorySize |

### 8.3 健康检查

```bash
# 函数健康检查端点 (需在代码中暴露 /actuator/health)
curl https://<fc-domain>/actuator/health

# 预期响应
{"status":"UP"}
```

---

## 9. 回滚方案

### 9.1 快速回滚到容器部署

```bash
# 1. 修改 API Gateway 路由，将流量切回 K8s 服务
# 2. 删除或暂停函数计算服务
s remove --template-file deploy/serverless/community/template.yaml

# 3. 确认 K8s Pod 正常运行
kubectl get pods -l app=tailor-is-community
```

### 9.2 灰度回滚

如果函数计算出现异常，可以在 API Gateway 层按比例分流：
- 90% 流量 → K8s 容器
- 10% 流量 → 函数计算

逐步降低 Serverless 流量比例直至完全回滚。

---

## 10. 常见问题

### Q1: 冷启动时间长怎么办？

1. 确保预留实例 ≥ 1
2. 检查懒加载是否生效：日志中应看到 `Lazy initialization enabled`
3. 减小镜像体积（使用 JRE 而非 JDK 基础镜像）
4. 考虑 GraalVM Native Image（需要额外改造，不推荐作为首选方案）

### Q2: 数据库连接池连接泄漏？

Serverless 环境下函数实例会被销毁，Druid 连接池的 `destroy()` 方法可能来不及执行。建议：
- 在 `application-serverless.yml` 中配置较短的 `min-evictable-idle-time-millis`
- MySQL 侧配置较短的 `wait_timeout`

### Q3: Nacos 配置中心如何配合？

Serverless 模式下推荐关闭 Nacos 配置中心，改用函数计算的环境变量注入配置。如果必须使用 Nacos，确保：
- `spring.cloud.nacos.config.import-check.enabled=false`
- Nacos 服务器在 VPC 内可达

### Q4: 分布式事务 (Seata) 如何工作？

Serverless 函数实例是无状态的，Seata AT/XA 模式仍然可用，但需注意：
- 函数实例可能被销毁，TCC 模式的 confirm/cancel 需要确保幂等
- 建议非核心服务避免使用 Seata，改用最终一致性方案

### Q5: 文件上传/下载如何处理？

Serverless 函数有临时磁盘限制（通常 512MB），大文件处理建议：
- 上传：直传 OSS，函数仅做元数据校验
- 下载：返回 OSS 签名 URL，不经过函数中转

---

## 附录 A: 部署检查清单

- [ ] Docker 镜像构建并推送成功
- [ ] VPC 网络配置正确，函数可访问 RDS/Redis
- [ ] 环境变量配置完整（数据库、Redis 等）
- [ ] 预留实例 ≥ 1
- [ ] HTTP 触发器配置正确
- [ ] 健康检查通过
- [ ] API Gateway 路由已切换
- [ ] 监控告警已配置
- [ ] 回滚方案已验证

## 附录 B: 相关配置文件路径

| 文件 | 路径 |
|------|------|
| 社区服务 Serverless 配置 | `tailor-is-community/src/main/resources/serverless-config.yml` |
| 社区服务 Serverless Profile | `tailor-is-community/src/main/resources/application-serverless.yml` |
| 社区服务 FunctionHandler | `tailor-is-community/src/main/java/.../CommunityFunctionHandler.java` |
| 社区服务 SAM 模板 | `deploy/serverless/community/template.yaml` |
| 学堂服务 Serverless 配置 | `tailor-is-academy/src/main/resources/serverless-config.yml` |
| 学堂服务 Serverless Profile | `tailor-is-academy/src/main/resources/application-serverless.yml` |
| 学堂服务 FunctionHandler | `tailor-is-academy/src/main/java/.../AcademyFunctionHandler.java` |
| 学堂服务 SAM 模板 | `deploy/serverless/academy/template.yaml` |
