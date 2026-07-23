# Tailor-IS（裁智云）架构设计文档 ARCHITECTURE.md

> **版本**: v1.1  
> **创建日期**: 2026-07-21  
> **状态**: 已审计（Audited）  
> **审计来源**: VibeCoding Phase 2 — Architect

---

## 1. 系统架构总览

```
┌──────────────────────────────────────────────────────────────────────┐
│                          客户端层 (Client)                            │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐            │
│  │ PC 商城   │  │ 平台管理  │  │ 商户管理  │  │ 移动端    │            │
│  │ pc-mall   │  │ platform │  │ merchant │  │ mobile    │            │
│  │ :5173     │  │ -admin   │  │ -admin   │  │ -app      │            │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘            │
└───────┼─────────────┼─────────────┼─────────────┼───────────────────┘
        │             │             │             │
        └─────────────┼─────────────┼─────────────┘
                      │             │
              ┌───────┴──────┐ ┌───┴────────┐
              │  Nginx :80   │ │  WebSocket  │
              │  (静态资源    │ │  (IM 通信)  │
              │   + API 代理) │ │             │
              └───────┬──────┘ └───┬────────┘
                      │            │
┌─────────────────────┼────────────┼──────────────────────────────────┐
│                     │  网关层 (Gateway)   │                          │
│           ┌─────────┴────────┐ ┌────────┴─────────┐                │
│           │  core-gateway    │ │  lite-gateway    │                │
│           │  :9000           │ │  :8081           │                │
│           │  (核心交易网关)    │ │  (轻量业务网关)    │                │
│           └────────┬────────┘ └────────┬─────────┘                │
└────────────────────┼──────────────────┼────────────────────────────┘
                     │                  │
┌────────────────────┼──────────────────┼────────────────────────────┐
│              微服务层 (Microservices)  │                            │
│                    │                  │                            │
│  ┌─────────────────┴──────┐  ┌───────┴──────────────────┐        │
│  │   核心业务服务 8100-8107 │  │  轻量业务服务 8108-8117   │        │
│  │                        │  │                          │        │
│  │ admin      :8100       │  │ community    :8108       │        │
│  │ user       :8101       │  │ supply       :8109       │        │
│  │ product    :8102       │  │ merchant     :8110       │        │
│  │ order      :8103       │  │ message      :8111       │        │
│  │ payment    :8104       │  │ academy      :8112       │        │
│  │ marketing  :8105       │  │ analytics    :8113       │        │
│  │ ai         :8106       │  │ message-im   :8114       │        │
│  │ copyright  :8107       │  │ pattern      :8115       │        │
│  └──────────┬────────────┘  │ design       :8116       │        │
│             │               │ knowledge    :8117       │        │
│             │               └──────────────┬───────────┘        │
└─────────────┼──────────────────────────────┼─────────────────────┘
              │                              │
┌─────────────┼──────────────────────────────┼─────────────────────┐
│       基础设施层 (Infrastructure)            │                     │
│             │                              │                     │
│  ┌──────────┴──────────┐  ┌───────────────┴───────────┐        │
│  │ MySQL :3306          │  │ Redis :6379               │        │
│  │ (tailor-is-mysql)    │  │ (tailor-is-redis)         │        │
│  └─────────────────────┘  └───────────────────────────┘        │
│  ┌──────────────────────┐ ┌──────────────────────────┐        │
│  │ Nacos :8848          │ │ RabbitMQ :5672           │        │
│  │ (tailor-is-nacos)    │ │ (tailor-is-rabbitmq)     │        │
│  └──────────────────────┘ └──────────────────────────┘        │
│  网络: tailoris-services (bridge, 172.28.0.0/16)              │
└───────────────────────────────────────────────────────────────┘
```

---

## 2. 目录结构

### 2.1 后端目录

```
tailor-is/
├── pom.xml                          # 父 POM，统一依赖管理
├── sql/                             # 数据库初始化 SQL
│   ├── 01_user_system.sql
│   ├── 02_merchant_system.sql
│   ├── 03_product_system.sql
│   ├── 04_order_system.sql
│   ├── 05_payment_system.sql
│   ├── 06_marketing_system.sql
│   ├── 07_copyright_system.sql
│   ├── 08_community_system.sql
│   ├── 09_supply_system.sql
│   ├── 10_message_system.sql
│   ├── 11_ai_system.sql
│   ├── 12_admin_system.sql
│   ├── 13_pattern_system.sql
│   ├── 14_design_system.sql
│   ├── 15_academy_system.sql
│   ├── 16_knowledge_system.sql
│   ├── 17_analytics_system.sql
│   └── 18_im_system.sql
├── tailor-is-common/                # 公共模块
│   └── src/main/java/com/tailoris/common/
│       ├── annotation/              # 自定义注解（RateLimit, ApiVersion）
│       ├── aspect/                  # AOP 切面
│       ├── client/                  # Feign 客户端（UserClient, OrderClient...）
│       ├── config/                  # 全局配置（WebMvc, Security, Redis, Cors, Feign）
│       ├── constant/                # 全局常量
│       │   ├── RoutePortConstant.java
│       │   ├── CommonConstants.java
│       │   ├── StatusEnum.java
│       │   ├── ErrorCode.java
│       │   ├── ProductConstants.java
│       │   ├── OrderConstants.java
│       │   ├── PatternConstants.java
│       │   └── RedisKeyPrefix.java
│       ├── dto/                     # 公共 DTO（PageRequest, PageResponse）
│       ├── entity/                  # 基础实体（BaseEntity）
│       ├── exception/               # 全局异常
│       │   ├── GlobalExceptionHandler.java
│       │   ├── BusinessException.java
│       │   ├── AccountErrorException.java
│       │   └── DataSourceConnectException.java
│       ├── filter/                  # 过滤器（XssFilter, TraceIdFilter）
│       ├── result/                  # 统一返回体（Result, ResultCode）
│       ├── security/                # 安全组件（CsrfTokenFilter）
│       └── util/                    # 工具类（StringUtils, EncryptUtils, DateUtils, HttpRequestUtils）
├── tailor-is-common-web/            # Web 公共模块
├── tailor-is-api/                   # API 接口定义模块
├── tailor-is-core-gateway/          # 核心网关
├── tailor-is-lite-gateway/          # 轻量网关
├── tailor-is-user/                  # 用户服务
│   └── src/main/java/com/tailoris/user/
│       ├── controller/              # AuthController, UserController
│       ├── dto/                     # LoginRequest, RegisterRequest, ...
│       ├── entity/                  # SysUser, SysRole, ...
│       ├── filter/                  # BearerTokenFilter, GatewayTokenFilter
│       ├── mapper/                  # MyBatis Mapper
│       └── service/                 # 业务逻辑层
├── tailor-is-admin/                 # 管理服务
├── tailor-is-merchant/              # 商户服务
├── tailor-is-product/               # 商品服务
├── tailor-is-order/                 # 订单服务
├── tailor-is-payment/               # 支付服务
├── tailor-is-marketing/             # 营销服务
├── tailor-is-ai/                    # AI 服务
├── tailor-is-copyright/             # 版权服务
├── tailor-is-community/             # 社区服务
├── tailor-is-supply/                # 供应链服务
├── tailor-is-message/               # 消息服务
├── tailor-is-message-im/            # 即时通讯服务
├── tailor-is-academy/               # 学堂服务
├── tailor-is-analytics/             # 数据分析服务
├── tailor-is-pattern/               # 纸样服务
├── tailor-is-design/                # 设计服务
└── tailor-is-knowledge/             # 知识库服务
```

### 2.2 前端目录

```
tailor-is-frontend/
├── pc-mall/                         # PC 商城
│   └── src/
│       ├── api/                     # request.ts（API 层）
│       ├── components/              # 公共组件
│       ├── i18n/                    # 国际化（6语言）
│       ├── router/                  # 路由配置
│       ├── security/                # xss.ts, csp.ts, csrf.ts
│       ├── store/                   # Pinia 状态管理
│       ├── types/                   # TypeScript 类型定义
│       ├── utils/                   # 工具函数（crypto.ts）
│       └── views/                   # 页面组件
├── platform-admin/                  # 平台管理后台
├── merchant-admin/                  # 商户管理后台
├── mobile-app/                      # 移动端（uni-app）
└── shared/                          # 共享模块
    ├── components/                  # 共享组件
    ├── types/                       # 共享类型
    └── utils/
        └── request-core.ts          # 共享请求核心
```

### 2.3 部署目录

```
deploy/
├── docker-compose.services.yml      # 主编排文件（唯一）
├── .env.example                     # 环境变量模板（版本控制）
├── nginx/                           # Nginx 配置
├── scripts/                         # 部署脚本
└── tailor-is-prod/
    └── env/
        └── .env.base                # 环境变量（不入版本控制）
```

---

## 3. 模块划分与依赖

### 3.1 模块依赖关系

```
tailor-is-common  ←── 所有模块
       ↑
tailor-is-api  ←── 网关模块
       ↑
tailor-is-core-gateway / tailor-is-lite-gateway
       ↑
  各业务微服务模块（user, order, product, ...）
```

### 3.2 分层架构（每个微服务内部）

```
┌─────────────────────────────────┐
│  Controller 层                   │  ← @RestController + @RequestMapping(RoutePortConstant)
│  - 参数校验 (@Valid)             │
│  - 路由映射                      │
│  - 统一返回 Result<T>            │
├─────────────────────────────────┤
│  Service 层                      │  ← @Service
│  - 业务逻辑                      │
│  - 事务管理 (@Transactional)     │
│  - 异常抛出 (BusinessException)  │
├─────────────────────────────────┤
│  Mapper 层                       │  ← MyBatis-Plus BaseMapper
│  - 数据访问                      │
│  - SQL 映射                      │
├─────────────────────────────────┤
│  Entity 层                       │  ← @TableName
│  - 数据库表映射                  │
│  - 继承 BaseEntity               │
└─────────────────────────────────┘
```

**禁止跨层调用**: Controller → Mapper 直接调用禁止，必须经过 Service 层。

### 3.3 微服务间通信

| 方式 | 用途 | 实现 |
|------|------|------|
| Feign 客户端 | 同步 REST 调用 | `tailor-is-common/client/*.java` |
| RabbitMQ | 异步消息 | 支付回调、订单状态通知 |
| Nacos | 服务发现 + 配置中心 | 所有服务注册 |

---

## 4. 接口规范

### 4.1 路由前缀规范

| 服务 | 一级路由 | 二级路由 | 示例 |
|------|---------|---------|------|
| 认证 | `/api/v1` | `/auth` | `POST /api/v1/auth/login` |
| 用户 | `/api/v1` | `/user` | `GET /api/v1/user/info` |
| 商品 | `/api/v1` | `/product` | `GET /api/v1/product/list` |
| 订单 | `/api/v1` | `/order` | `POST /api/v1/order/create` |
| 支付 | `/api/v1` | `/payment` | `POST /api/v1/payment/pay` |
| 纸样 | `/api/v1` | `/pattern` | `POST /api/v1/pattern/create` |
| 设计 | `/api/v1` | `/design` | `POST /api/v1/design/upload` |
| 版权 | `/api/v1` | `/copyright` | `POST /api/v1/copyright/check` |
| AI | `/api/v1` | `/ai` | `POST /api/v1/ai/size/recommend` |
| 管理 | `/api/v1` | `/admin` | `GET /api/v1/admin/users` |
| 社区 | `/api/v1` | `/community` | `POST /api/v1/community/post` |
| 供应链 | `/api/v1` | `/supply` | `GET /api/v1/supply/list` |
| 商户 | `/api/v1` | `/merchant` | `GET /api/v1/merchant/profile` |
| 消息 | `/api/v1` | `/message` | `GET /api/v1/message/list` |
| 学堂 | `/api/v1` | `/academy` | `GET /api/v1/academy/courses` |
| 分析 | `/api/v1` | `/analytics` | `GET /api/v1/analytics/report` |
| 知识库 | `/api/v1` | `/knowledge` | `GET /api/v1/knowledge/articles` |
| 即时通讯 | `/api/v1` | `/im` | `WS /api/v1/im/ws` |

### 4.2 分页接口规范

**请求**:
```json
{
  "page": 1,
  "size": 20
}
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "pages": 5,
    "current": 1,
    "size": 20,
    "records": []
  }
}
```

### 4.3 错误码规范

| 错误码 | HTTP 状态码 | 异常类型 | 说明 |
|--------|------------|---------|------|
| 200 | 200 | - | 成功 |
| 400 | 400 | BusinessException | 业务异常 |
| 400 | 400 | AccountErrorException | 账号密码错误 |
| 400 | 400 | MethodArgumentNotValidException | 参数校验失败 |
| 401 | 401 | NotLoginException | 未登录/Token无效 |
| 403 | 403 | NotRoleException | 角色权限不足 |
| 403 | 403 | NotPermissionException | 操作权限不足 |
| 404 | 404 | NoHandlerFoundException | 资源不存在 |
| 500 | 500 | RuntimeException | 运行时异常 |
| 500 | 500 | Exception | 系统内部错误 |
| 503 | 503 | DataSourceConnectException | 数据源连接失败 |

---

## 5. 数据模型

### 5.1 数据库分布

| 数据库 | 所属服务 | 核心表 |
|--------|---------|--------|
| tailor_is_user | user | sys_user, sys_role, sys_permission |
| tailor_is_admin | admin | sys_admin, sys_audit_log |
| tailor_is_product | product | product, product_sku, product_category |
| tailor_is_order | order | order, order_item, after_sale |
| tailor_is_payment | payment | payment_record, settlement |
| tailor_is_marketing | marketing | coupon, points_record, seckill |
| tailor_is_copyright | copyright | copyright_record, infringement_report |
| tailor_is_community | community | post, comment, like_record |
| tailor_is_supply | supply | fabric, supplier |
| tailor_is_merchant | merchant | merchant_info, merchant_audit |
| tailor_is_message | message | notification, email_log |
| tailor_is_im | message-im | conversation, message |
| tailor_is_academy | academy | course, chapter, enrollment |
| tailor_is_analytics | analytics | report, data_snapshot |
| tailor_is_pattern | pattern | pattern_order, pattern_file |
| tailor_is_design | design | design_file, design_revision |
| tailor_is_knowledge | knowledge | article, category |
| tailor_is_ai | ai | ai_task, recommendation |

### 5.2 基础实体字段

所有实体继承 `BaseEntity`:

```java
public class BaseEntity {
    private Long id;           // 主键（雪花算法）
    private LocalDateTime createTime;  // 创建时间
    private LocalDateTime updateTime;  // 更新时间
    private Integer deleted;   // 逻辑删除（0=未删除, 1=已删除）
}
```

### 5.3 用户状态枚举

| 值 | 含义 | 常量 |
|----|------|------|
| 0 | 禁用 | `CommonConstants.STATUS_DISABLED` |
| 1 | 启用 | `CommonConstants.STATUS_ENABLED` |

---

## 6. 安全架构

### 6.1 认证流程

```
用户 → 前端 → Nginx → Gateway → User Service
                              │
                    1. 验证 Token (Sa-Token + Redis)
                    2. 注入 X-User-Id 头
                    3. 转发到下游微服务
                              │
                    下游微服务读取 X-User-Id
                    执行 RBAC 权限校验
```

### 6.2 加密体系

| 层级 | 算法 | 用途 |
|------|------|------|
| 传输层 | AES-256-GCM | 文件端到端加密 |
| 密码层 | BCrypt-12 | 用户密码哈希 |
| 会话层 | Sa-Token (UUID) | Token 生成与验证 |
| 网关层 | HMAC-SHA256 | 网关签名验证 |
| 前端层 | CSRF Token | 跨站请求伪造防护 |
| 前端层 | XSS Filter | 跨站脚本过滤 |
| 前端层 | CSP Policy | 内容安全策略 |

### 6.3 异常处理架构

```
                      ┌─────────────────────────────┐
                      │  GlobalExceptionHandler      │
                      │  @Order(HIGHEST_PRECEDENCE)  │
                      │  @RestControllerAdvice       │
                      │  (全局生效，无 basePackages)   │
                      └─────────────┬───────────────┘
                                    │
            ┌───────────────────────┼───────────────────────┐
            │                       │                       │
    ┌───────┴───────┐       ┌──────┴──────┐       ┌───────┴───────┐
    │ NotLoginExc   │       │ BusinessExc │       │ Exception     │
    │ → 401         │       │ → 400       │       │ → 500 (兜底)  │
    └───────────────┘       └─────────────┘       └───────────────┘
            │                       │
    ┌───────┴───────┐       ┌──────┴──────┐
    │ NotRoleExc    │       │ AccountErr  │
    │ → 403         │       │ → 400       │
    └───────────────┘       └─────────────┘
            │
    ┌───────┴───────────────┐
    │ NotPermissionExc      │
    │ → 403                 │
    └───────────────────────┘

子模块异常处理器:
  AdminGlobalExceptionHandler
  @Order(LOWEST_PRECEDENCE - 100)
  basePackages = "com.tailoris.admin"
  ❌ 禁止 Exception.class 兜底
```

---

## 7. 部署架构

### 7.1 容器化部署

- **编排工具**: Docker Compose（唯一）
- **网络**: `tailoris-services` bridge（172.28.0.0/16）
- **容器命名**: `tailor-is-{模块名}`（无 `-service` 后缀）
- **重启策略**: `unless-stopped`
- **健康检查**: 30s 间隔，10s 超时，5 次重试，90s 启动等待

### 7.2 启动顺序

```
1. MySQL → 健康检查通过
2. Redis → 健康检查通过
3. Nacos → 健康检查通过
4. RabbitMQ → 健康检查通过
5. core-gateway → lite-gateway
6. 核心业务服务（admin, user, product, order, payment, marketing, ai, copyright）
7. 轻量业务服务（community, supply, merchant, message, academy, analytics, im, pattern, design, knowledge）
```

### 7.3 资源限制

| 服务类型 | CPU Limit | Memory Limit | JVM Xmx |
|---------|-----------|-------------|---------|
| 核心业务 | 0.8 CPU | 384M | 320m |
| 轻量业务 | 0.5 CPU | 256M | 220m |
| 网关 | 1.0 CPU | 512M | 384m |

---

## 8. 变更记录

| 日期 | 版本 | 变更内容 |
|------|------|---------|
| 2026-07-21 | v1.1 | 初始版本 — VibeCoding Phase 2 Architect 输出 |