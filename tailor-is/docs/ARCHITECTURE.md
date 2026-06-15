# Tailor IS 系统架构设计文档

## 1. 系统概览

Tailor IS（裁智云）是服装全产业数字化平台，采用微服务架构，涵盖设计、版权、交易、营销、社区、供应链全链路。系统基于 Spring Cloud 微服务生态构建，以 Nacos 为注册中心和配置中心，Gateway 为统一入口，RabbitMQ 为异步消息中间件，Redis 为缓存层。

### 核心目标

- 为服装产业链上下游提供一站式数字化解决方案
- 支持从服装设计、版型AI辅助、版权登记保护到商品上架、交易、营销、售后全流程
- 提供社区互动、在线教育、数据分析等增值服务
- 支持PC管理端、商户端、C端用户小程序/移动端多端接入

## 2. 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.2.1 |
| 微服务框架 | Spring Cloud | 2023.0.0 |
| 微服务生态 | Spring Cloud Alibaba | 2022.0.0.0 |
| 注册/配置中心 | Nacos | 2.x |
| API网关 | Spring Cloud Gateway | — |
| ORM | MyBatis-Plus | 3.5.5 |
| 认证授权 | Sa-Token | 1.37.0 |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 7.x |
| 消息队列 | RabbitMQ | 3.12 |
| 连接池 | Druid | 1.2.20 |
| 工具库 | Hutool | 5.8.24 |
| API文档 | Knife4j (Swagger) | 4.4.0 |
| 前端框架 | Vue 3 + Vite + Element Plus | — |
| 跨端方案 | UniApp | — |
| 语言 | Java 17 / TypeScript | — |
| 构建工具 | Maven | — |
| CI/CD | GitHub Actions | — |
| 容器化 | Docker + Docker Compose | — |
| Web服务器 | Nginx | — |

## 3. 系统架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              客户端层 (Client Layer)                          │
│   ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────────┐          │
│   │  PC管理端 │    │  商户端   │    │   H5端   │    │ 小程序/App   │          │
│   └────┬─────┘    └────┬─────┘    └────┬─────┘    └──────┬───────┘          │
└────────┼──────────────┼──────────────┼──────────────────┼───────────────────┘
         │              │              │                  │
         └──────────────┴──────────────┴──────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          网关层 (Gateway Layer) :8080                        │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │              Spring Cloud Gateway + AuthGlobalFilter                 │   │
│   │        路由转发 · 认证鉴权 · 限流 · CORS · 请求日志 · Knife4j聚合       │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         业务服务层 (Business Service Layer)                    │
│                                                                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│  │ user     │ │ merchant │ │ product  │ │ order    │ │ payment  │          │
│  │ 用户服务  │ │ 商户服务  │ │ 商品服务  │ │ 订单服务  │ │ 支付服务  │          │
│  │ :8081    │ │ :8082    │ │ :8083    │ │ :8084    │ │ :8085    │          │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘          │
│       │            │            │            │            │                 │
│  ┌────┴─────┐ ┌────┴─────┐ ┌────┴─────┐ ┌────┴─────┐ ┌────┴─────┐          │
│  │ marketing│ │ ai       │ │ copyright│ │community │ │ supply  │          │
│  │ 营销服务  │ │ AI服务   │ │ 版权服务  │ │ 社区服务  │ │ 供应链   │          │
│  │ :8086    │ │ :8087    │ │ :8088    │ │ :8089    │ │ :8090    │          │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘          │
│       │            │            │            │            │                 │
│  ┌────┴─────┐ ┌────┴─────┐ ┌────┴─────┐ ┌────┴─────┐ ┌────┴─────┐          │
│  │ admin    │ │ message  │ │ pattern  │ │message-im│ │ academy  │          │
│  │ 管理服务  │ │ 消息服务  │ │ 版型服务  │ │ 即时通讯  │ │ 学院服务  │          │
│  │ :8091    │ │ :8092    │ │ :8093    │ │ :8094    │ │ :8095    │          │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘          │
│                                                                              │
│                            ┌──────────┐                                      │
│                            │ analytics│                                      │
│                            │ 数据分析  │                                      │
│                            │ :8096    │                                      │
│                            └──────────┘                                      │
└─────────────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          基础设施层 (Infrastructure Layer)                     │
│                                                                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │  Nacos   │  │  MySQL   │  │  Redis   │  │ RabbitMQ │  │  Nginx   │      │
│  │ :8848    │  │  :3306   │  │  :6379   │  │  :5672   │  │  :80/443 │      │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘  └──────────┘      │
│                                                                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                                  │
│  │  MinIO   │  │  ELK     │  │Prometheus│                                  │
│  │ 对象存储  │  │ 日志收集  │  │  监控    │                                  │
│  └──────────┘  └──────────┘  └──────────┘                                  │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 4. 模块划分

| 序号 | 模块 | ArtifactId | 端口 | 数据库 | 职责描述 | API前缀 |
|------|------|------------|------|--------|----------|---------|
| 1 | 公共模块 | tailor-is-common | — | — | 公共工具类、基础实体、统一返回、全局异常、安全过滤 | — |
| 2 | 网关服务 | tailor-is-gateway | 8080 | — | 统一入口、路由转发、认证鉴权、限流、Knife4j文档聚合 | — |
| 3 | 用户服务 | tailor-is-user | 8081 | tailor_user | 用户注册/登录、角色权限管理、收货地址、实名认证 | /api/user |
| 4 | 商户服务 | tailor-is-merchant | 8082 | tailor_merchant | 商户入驻/审核、店铺管理、员工管理 | /api/merchant |
| 5 | 商品服务 | tailor-is-product | 8083 | tailor_product | 商品发布/审核、SKU管理、分类管理、评价管理 | /api/product |
| 6 | 订单服务 | tailor-is-order | 8084 | tailor_order | 购物车、订单创建/支付/退款、物流跟踪、售后工单 | /api/order |
| 7 | 支付服务 | tailor-is-payment | 8085 | tailor_payment | 支付通道、账户管理、充值/提现、质保金、结算 | /api/payment |
| 8 | 营销服务 | tailor-is-marketing | 8086 | tailor_marketing | 优惠券、积分体系、会员等级、店铺会员 | /api/marketing |
| 9 | AI服务 | tailor-is-ai | 8087 | tailor_ai | 版型生成/检查、人体尺寸数据分析、版型迭代 | /api/ai |
| 10 | 版权服务 | tailor-is-copyright | 8088 | tailor_copyright | 设计版权登记、版权查询、侵权投诉、版权交易 | /api/copyright |
| 11 | 社区服务 | tailor-is-community | 8089 | tailor_community | 帖子发布/评论/点赞、话题管理、内容举报 | /api/community |
| 12 | 供应链服务 | tailor-is-supply | 8090 | tailor_supply | 供需发布、供应商管理、供需匹配、联系方式 | /api/supply |
| 13 | 管理服务 | tailor-is-admin | 8091 | tailor_admin | 平台运营管理、商户审核、商品审核、佣金配置、财务报表 | /api/admin |
| 14 | 消息服务 | tailor-is-message | 8092 | tailor_message | 系统消息推送、消息模板管理、消息收件箱 | /api/message |
| 15 | 版型服务 | tailor-is-pattern | 8093 | tailor_pattern | 版型库管理、版型搜索、版型推荐 | /api/pattern |
| 16 | 即时通讯 | tailor-is-message-im | 8094 | tailor_im | 商家-用户实时聊天、会话管理、消息记录 | /api/im |
| 17 | 学院服务 | tailor-is-academy | 8095 | tailor_academy | 课程管理、章节管理、学习进度 | /api/academy |
| 18 | 数据分析 | tailor-is-analytics | 8096 | tailor_analytics | 平台数据统计、经营分析、用户画像、数据大屏 | /api/analytics |

## 5. 数据流转

### 5.1 用户注册流程

```
客户端 → Gateway(:8080) → tailor-is-user(:8081)
  ├── POST /api/user/register → 验证手机号/邮箱 → 加密密码存储
  ├── 自动创建用户账户 (tailor_payment.user_account)
  └── 返回 JWT Token (Sa-Token)
```

### 5.2 商户入驻流程

```
商户端 → Gateway → tailor-is-merchant(:8082)
  ├── POST /api/merchant/apply → 提交商户资料
  ├── RabbitMQ → tailor-is-admin 审核通知
  ├── 审核通过 → 自动创建店铺 + 质保金账户
  └── 通知商户入驻成功 (tailor-is-message)
```

### 5.3 商品上架流程

```
商户端 → Gateway → tailor-is-product(:8083)
  ├── POST /api/product/create → 商品基本信息 + SKU + 属性
  ├── RabbitMQ → tailor-is-admin 商品审核队列
  ├── 审核通过 → 商品状态更新为上架
  ├── Redis 缓存更新 (商品详情缓存)
  └── 同步到 ES (搜索索引)
```

### 5.4 下单支付流程

```
用户端 → Gateway → tailor-is-order(:8084)
  ├── POST /api/order/create → 验库存 → 锁库存 → 创建订单
  ├── RabbitMQ 延迟队列 → 30分钟未支付自动取消
  │
  └→ tailor-is-payment(:8085)
       ├── POST /api/payment/pay → 调用支付通道
       ├── 支付回调 → 更新订单状态 → 扣库存
       ├── RabbitMQ → tailor-is-marketing 发放积分/更新会员等级
       └── RabbitMQ → tailor-is-message 推送订单通知
```

### 5.5 结算流程

```
tailor-is-admin → tailor-is-payment(:8085)
  ├── 定时任务 → 计算周期内已完成订单
  ├── 扣除平台佣金 → 生成结算单
  ├── 商户提现 → 审核 → 打款
  └── 更新商户账户余额
```

### 5.6 售后流程

```
用户端 → Gateway → tailor-is-order(:8084)
  ├── POST /api/order/after-sale → 创建售后工单
  ├── 商户处理 → 同意退款/退货退款/拒绝
  ├── RabbitMQ → tailor-is-payment 执行退款
  └── RabbitMQ → tailor-is-message 推送售后进度通知
```

## 6. 安全架构

### 6.1 认证流程

```
客户端请求 → Nginx(SSL终结) → Gateway(:8080)
  ├── AuthGlobalFilter → 白名单路径直接放行
  ├── 非白名单 → 校验 Sa-Token (从 Header/Cookie 读取)
  ├── Token 无效 → 返回 401
  └── Token 有效 → 解析用户信息 → 设置请求头 → 转发到下游服务
```

### 6.2 授权模型

```
Sa-Token RBAC 模型:
  SysUser ──┬── SysUserRole ──── SysRole ──── SysRolePermission ──── SysPermission
            │
            └── @SaCheckRole("merchant") / @SaCheckPermission("product:create")
```

### 6.3 多层安全防护

| 防护层 | 实现方式 | 位置 |
|--------|----------|------|
| 传输安全 | Nginx SSL 终结, HTTPS | Nginx |
| 认证鉴权 | Sa-Token + AuthGlobalFilter | Gateway |
| CSRF防护 | CsrfTokenInterceptor | Common |
| XSS防护 | XssFilter (请求参数清洗) | Common |
| CSP防护 | CspFilter (Content-Security-Policy头) | Common |
| 接口限流 | @RateLimit 注解 + Redis滑动窗口 | Common |
| 签名校验 | @SignatureCheck 注解 (防篡改) | Common |
| 数据权限 | DataPermissionHandlerImpl (MyBatis-Plus插件) | Common |
| 日志脱敏 | LogMaskUtils / DesensitizeUtils | Common |
| SQL注入防护 | MyBatis-Plus 参数化查询 | Common |

### 6.4 安全配置汇总

```
Gateway 全局过滤链:
  CspFilter → XssFilter → CsrfTokenInterceptor → AuthGlobalFilter → RateLimit → 路由转发

敏感数据脱敏规则:
  手机号: 138****1234
  身份证: 320***********1234
  银行卡: 6222 **** **** 1234
  邮箱:   t***@example.com
```

## 7. 服务间通信

### 7.1 同步通信

- **OpenFeign**: 服务间 RESTful API 调用 (声明式HTTP客户端)
- **负载均衡**: Spring Cloud LoadBalancer

### 7.2 异步通信

| 场景 | Exchange | 队列 | 消费者 |
|------|----------|------|--------|
| 订单超时取消 | order.delay.exchange | order.timeout.queue | tailor-is-order |
| 商户审核通知 | merchant.event.exchange | admin.audit.queue | tailor-is-admin |
| 商品审核通知 | product.event.exchange | admin.audit.queue | tailor-is-admin |
| 积分发放 | marketing.event.exchange | marketing.points.queue | tailor-is-marketing |
| 消息推送 | message.event.exchange | message.push.queue | tailor-is-message |
| 退款处理 | payment.event.exchange | payment.refund.queue | tailor-is-payment |

### 7.3 分布式事务

- **Seata AT模式**: 用于跨服务数据一致性要求高的场景 (如订单+支付+库存)
- **最终一致性**: 通过 RabbitMQ 消息 + 本地消息表保证

## 8. 数据存储架构

```
┌─────────────────────────────────────────────┐
│                   MySQL 8.0                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐     │
│  │tailor_user│ │tailor_   │ │tailor_   │     │
│  │          │ │merchant  │ │product   │ ... │
│  └──────────┘ └──────────┘ └──────────┘     │
│  每个微服务独立数据库，避免跨库JOIN             │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│                   Redis 7.x                  │
│  ├── Session/Token 存储 (Sa-Token)           │
│  ├── 商品详情缓存                              │
│  ├── 验证码存储                                │
│  ├── 分布式锁 (Redisson)                      │
│  ├── 接口限流计数器                             │
│  └── 热门数据预热                              │
└─────────────────────────────────────────────┘
```

## 9. 项目结构

```
tailor-is/
├── pom.xml                          # 父POM，统一依赖管理
├── docker-compose.yml               # 本地开发环境
├── docker/                          # Docker相关配置
│   ├── Dockerfile                   # 服务通用Dockerfile
│   └── docker-compose.prod.yml      # 生产环境编排
├── docs/                            # 项目文档
├── sql/                             # 数据库初始化脚本
└── tailor-is-*/                     # 18个微服务模块
    ├── pom.xml
    └── src/main/
        ├── java/com/tailoris/{module}/
        │   ├── {Module}Application.java
        │   ├── controller/          # REST控制器
        │   ├── service/             # 业务服务层
        │   ├── mapper/              # MyBatis数据访问
        │   ├── entity/              # 数据实体
        │   ├── dto/                 # 数据传输对象
        │   ├── config/              # 模块配置
        │   └── mq/                  # 消息队列消费者
        └── resources/
            └── application.yml      # 模块配置
```