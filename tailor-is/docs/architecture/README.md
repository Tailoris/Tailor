# Tailor IS 系统架构图集 (System Architecture Diagrams)

> 对应: Phase 1 / P1-4 / H-03  
> 生成时间: 2026-06-13  
> 格式: PlantUML + Mermaid (兼容 GitHub/GitLab 渲染)  
> 工具: VSCode PlantUML 插件 / IntelliJ PlantUML Integration / 在线 https://www.plantuml.com/plantuml

## 目录

| 编号 | 名称 | 文件 | 用途 |
|------|------|------|------|
| 1 | 逻辑架构图 | [01-logical-architecture.puml](./01-logical-architecture.puml) | 系统分层、服务依赖 |
| 2 | 物理部署图 | [02-physical-deployment.puml](./02-physical-deployment.puml) | 节点/K8s 集群布局 |
| 3 | 模块交互图 | [03-module-interaction.puml](./03-module-interaction.puml) | 4 大核心业务场景时序 |
| 4 | 网络拓扑图 | [04-network-topology.puml](./04-network-topology.puml) | 安全分区/VPC/端口策略 |

## 一、逻辑架构 (01-logical-architecture.puml)

```
┌─────────────────────────────────────────────┐
│   客户端层  (PC/H5/小程序)                  │
└──────────────────┬──────────────────────────┘
                   ↓ HTTPS
┌─────────────────────────────────────────────┐
│   接入层  (OpenResty: 静态/SSL终止)         │
└──────────────────┬──────────────────────────┘
                   ↓
┌─────────────────────────────────────────────┐
│   API 网关层  (Core-GW / Lite-GW)           │
└──────┬──────────────────────┬───────────────┘
       ↓                      ↓
┌──────────────┐      ┌──────────────┐
│ 核心服务 ×9  │      │ 轻量服务 ×7  │
└──────┬───────┘      └──────┬───────┘
       ↓                      ↓
┌─────────────────────────────────────────────┐
│   共享能力  (Common / Common-Web / API)     │
└──────────────────┬──────────────────────────┘
                   ↓
┌─────────────────────────────────────────────┐
│   基础设施  (MySQL/Redis/MQ/Nacos/Sentinel) │
└─────────────────────────────────────────────┘
```

**核心要点**:
- 微服务按业务重要性分为两层网关 (Core/Lite), 实现隔离
- 共享能力下沉到 `common` 模块, 避免重复造轮子
- 服务通过 Nacos 注册发现, Sentinel 统一流控
- 数据访问: 关系数据走 MySQL, 缓存走 Redis, 异步走 MQ

## 二、物理部署 (02-physical-deployment.puml)

```
互联网
  ↓
CDN / WAF
  ↓
OpenResty (DMZ) → HAProxy
  ↓
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ Core Gateway │  │ Core Gateway │  │ Lite Gateway │
│    ×3        │  │    ×3        │  │    ×3        │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       ↓                 ↓                 ↓
┌─────────────────────────────────────────────────┐
│ K8s Node (核心业务 ×9 服务, 轻量 ×7 服务)        │
│ - User/Merchant/Product/Order/Payment/...        │
└────────────────────┬────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│ 数据层: MySQL M/S | Redis Cluster | MinIO | ES │
└─────────────────────────────────────────────────┘
```

**部署资源估算** (生产环境, 单集群):

| 组件 | 节点数 | CPU | 内存 | 磁盘 | 网络 |
|------|-------|-----|------|------|------|
| Core Gateway | 3 | 4核 | 8GB | 50GB | 1Gbps |
| 核心业务 Pod | 6 | 16核 | 32GB | 200GB | 10Gbps |
| 轻量服务 Pod | 2 | 8核 | 16GB | 100GB | 1Gbps |
| MySQL | 3 (1主2从) | 16核 | 64GB | 2TB SSD | 10Gbps |
| Redis | 6 (3主3从) | 8核 | 16GB | 100GB | 10Gbps |
| MinIO | 4 | 8核 | 16GB | 4TB×4 | 10Gbps |
| ES | 3 | 16核 | 32GB | 2TB | 1Gbps |
| Prometheus | 1 | 4核 | 16GB | 500GB | 1Gbps |

## 三、模块交互 (03-module-interaction.puml)

涵盖 4 大核心场景:

1. **用户登录认证**: User Service + Redis (Token) + Sa-Token
2. **下单→支付→回调**: Order + Payment + Marketing + MQ 异步
3. **AI 制版 (RocketMQ 异步)**: AI Service + Copyright + 模型调用
4. **商户入驻**: Merchant + User (角色授权) + Admin 审核

每个场景都标注了:
- 同步调用 (实线箭头)
- 异步消息 (虚线箭头)
- 涉及的服务/中间件
- 时序步骤编号

## 四、网络拓扑 (04-network-topology.puml)

**VPC 划分** (云上生产):
- `vpc-tailoris-prod` (10.0.0.0/16)
  - `subnet-public` (10.10.0.0/24): CDN, WAF, OpenResty
  - `subnet-app` (10.20.0.0/16): K8s 节点
  - `subnet-data` (10.30.0.0/16): MySQL/Redis/MQ
  - `subnet-mgmt` (10.40.0.0/24): Prometheus/Grafana/堡垒机

**安全组规则** (单向访问, 最小权限):
| 源 | 目标 | 协议 | 端口 | 说明 |
|----|------|------|------|------|
| 互联网 | DMZ | TCP | 80/443 | HTTPS 入口 |
| DMZ | App | TCP | 9001/9002 | 网关 |
| App | Data | TCP | 3306/6379/5672/9092/9000 | 业务访问数据 |
| Mgmt | All | TCP | 9090/3001/9093 | 监控拉取 |
| 堡垒机 | All | TCP | 22 | 运维 SSH |

## 五、查看图谱

### 选项 1: VSCode 插件 (推荐)
1. 安装 `PlantUML` 扩展 (作者: jebbs)
2. 安装 Graphviz (`apt install graphviz` 或 `brew install graphviz`)
3. 打开 `.puml` 文件, Alt+D 预览

### 选项 2: IntelliJ IDEA
1. 安装 `PlantUML Integration` 插件
2. 打开 `.puml` 文件, 自动渲染

### 选项 3: 命令行
```bash
# 安装 plantuml
npm install -g node-plantuml
# 或
docker run -d -p 8080:8080 plantuml/plantuml-server

# 渲染为 PNG
plantuml docs/architecture/*.puml
# 输出: docs/architecture/*.png
```

### 选项 4: 在线
访问 https://www.plantuml.com/plantuml/uml/ , 粘贴源码

## 六、版本管理

| 版本 | 日期 | 变更 |
|------|------|------|
| 1.0.0 | 2026-06-13 | Phase 1 初始版本, 4 张核心图 |

每次架构变更 (新增服务 / 调整依赖) 需同步更新本目录并提交 PR。

## 七、相关文档

- [ARCHITECTURE.md](../ARCHITECTURE.md) - 架构文字描述
- [ARCHITECTURE-OPTIMIZATION-PLAN.md](../ARCHITECTURE-OPTIMIZATION-PLAN.md) - 优化方案
- [DEPLOYMENT-GUIDE.md](../DEPLOYMENT-GUIDE.md) - 部署指南
- [K8S-DEPLOYMENT-GUIDE.md](../K8S-DEPLOYMENT-GUIDE.md) - K8s 部署
