# Tailor IS 系统路由与端口规范

> **版本**: v3.0 | **日期**: 2026-07-02 | **依据**: 三份历史规范合并 + 实际代码梳理 + 1Panel 部署环境约束
>
> **本版合并来源**:
> - `docs/Tailor-IS-系统路由与端口规范.md` (v2.0, 2026-06-19)
> - `docs/ROUTING-PORT-STANDARD.md` (v1.0, 2026-06-11)
> - `tailor-is/docs/ROUTING-PORT-STANDARD.md` (v1.1, 2026-06-09)
>
> **权威性声明**: 本文档为 Tailor IS 项目路由与端口分配的唯一权威来源。所有 `application.yml`、`docker-compose*.yml`、`GatewayRouteConfig.java`、Nginx 配置必须与本文档对齐，存在冲突以本文档为准。

---

## 目录

1. [1Panel 环境约束](#一1panel-环境约束不可变更)
2. [端口分配总表](#二端口分配总表)
3. [路由命名规范](#三路由命名规范)
4. [后端路由清单](#四后端路由清单)
5. [前端路由清单](#五前端路由清单)
6. [网关路由配置](#六网关路由配置)
7. [Nginx 路由配置](#七nginx-路由配置)
8. [端口占用检测机制](#八端口占用检测机制)
9. [变更审批流程](#九变更审批流程)
10. [版本控制](#十版本控制)
11. [1Panel 端口冲突回避](#十一1panel-端口冲突回避)

---

## 一、1Panel 环境约束（不可变更）

> 以下端口由 1Panel 管理，必须避开，不可与 Docker Compose 服务冲突。

| 服务 | 端口 | 管理方 | 备注 |
|------|------|--------|------|
| MySQL | 3306 | 1Panel | 主数据库 |
| Redis | 6379 | 1Panel | 缓存 |
| RabbitMQ AMQP | 5672 | 1Panel | 消息队列 |
| RabbitMQ Dashboard | 15672 | 1Panel | 管理界面 |
| **Nacos 界面** | **8080** | 1Panel | ⚠️ core-gateway 必须回避此端口 |
| Nacos API | 8848 | 1Panel | 服务注册/配置中心 |
| Nacos gRPC | 9848 | 1Panel | Nacos 2.x 通讯端口 |
| 1Panel 面板 | 11336 | 1Panel | 服务器管理面板 |

> **关键约束**: core-gateway 容器内端口仍为 8080，但宿主端口映射为 **9000**，以避免与 1Panel Nacos 界面端口冲突。

---

## 二、端口分配总表

### 2.1 网关层

| 服务名 | 容器端口 | 宿主端口 | 路由前缀 | 用途 |
|--------|---------|---------|---------|------|
| core-gateway | 8080 | **9000** | /api/{user,auth,product,favorite,order,cart,payment,settlement,account,sandbox,marketing,coupon,points,seckill,ai,body-size,copyright,merchant,shop,admin,pattern}/** | 核心网关 - 核心 C 端 + 管理端 |
| lite-gateway | 8081 | 8081 | /api/{community,post,comment,supply,message,notice,academy,course,analytics,metrics,dashboard,im,im-message}/** | 轻量网关 - 社区/学院/供应链/消息/数据 |

> **决策依据**: core-gateway 宿主端口从 8080 改为 9000（PORT-003 修复），因为 1Panel Nacos 界面占用 8080。

### 2.2 核心业务服务（core-gateway 路由域）

| 服务名 | 容器端口 | 宿主端口 | 路由前缀 | 用途 |
|--------|---------|---------|---------|------|
| admin-service | 8100 | 8100 | /api/admin/** | 平台管理后台 |
| user-service | 8101 | 8101 | /api/user/**, /api/auth/** | 用户/认证 |
| product-service | 8102 | 8102 | /api/product/**, /api/products/**, /api/favorite/** | 商品 |
| order-service | 8103 | 8103 | /api/order/**, /api/cart/** | 订单/购物车 |
| payment-service | 8104 | 8104 | /api/payment/**, /api/settlement/**, /api/account/**, /api/sandbox/** | 支付/结算/账户 |
| marketing-service | 8105 | 8105 | /api/marketing/**, /api/coupon/**, /api/points/**, /api/seckill/** | 营销 |
| ai-service | 8106 | 8106 | /api/ai/**, /api/body-size/** | AI/体型 |
| copyright-service | 8107 | 8107 | /api/copyright/** | 版权存证 |
| merchant-service | 8110 | 8110 | /api/merchant/**, /api/shop/** | 商家/店铺 |
| pattern-service | 8115 | 8115 | /api/pattern/** | 图案/纸样 |

### 2.3 轻量业务服务（lite-gateway 路由域）

| 服务名 | 容器端口 | 宿主端口 | 路由前缀 | 用途 |
|--------|---------|---------|---------|------|
| community-service | 8108 | 8108 | /api/community/**, /api/post/**, /api/comment/** | 社区 |
| supply-service | 8109 | 8109 | /api/supply/** | 供应链 |
| message-service | 8111 | 8111 | /api/message/**, /api/notice/** | 消息通知 |
| academy-service | 8112 | 8112 | /api/academy/**, /api/course/** | 线上学堂 |
| analytics-service | 8113 | 8113 | /api/analytics/**, /api/metrics/**, /api/dashboard/** | 数据统计 |
| message-im-service | 8114 | 8114 | /api/im/**, /api/message-im/**, /ws/** (WebSocket) | IM 聊天 |

### 2.4 公共服务

| 服务名 | 容器端口 | 宿主端口 | 路由前缀 | 用途 |
|--------|---------|---------|---------|------|
| file-upload (tailor-is-common) | 8082 | 8082 | /api/upload/**, /api/file/** | 文件上传（按需启用） |
| graphql-gateway | 4000 | 4000 | /graphql | GraphQL 聚合层 |

### 2.5 前端服务

| 服务名 | 容器端口 | 宿主端口 | 域名 | 用途 |
|--------|---------|---------|------|------|
| pc-mall (SSR) | 3000 | 3000 | www.tailoris.com | PC 商城（SSR） |
| merchant-admin | 3002 | 3002 | merchant.tailoris.com | 商户管理后台 |
| platform-admin | 3003 | 3003 | admin.tailoris.com | 平台管理后台 |
| mobile-app H5 | 3004 | 3004 | m.tailoris.com | 移动端 H5 |

> **冲突说明**: Grafana 在 development.yml 中默认使用 3000，与 pc-mall SSR 冲突。生产环境 Grafana 统一使用 **3001**（PORT-005 修复）。

### 2.6 基础设施

| 服务名 | 端口 | 管理方 | 用途 |
|--------|------|--------|------|
| MySQL | 3306 | 1Panel | 主数据库 |
| Redis Master | 6379 | 1Panel | 缓存主节点 |
| Redis Sentinel | 26379 | 1Panel | Redis 哨兵 |
| RabbitMQ AMQP | 5672 | 1Panel | 消息队列 |
| RabbitMQ Mgmt | 15672 | 1Panel | RabbitMQ 管理界面 |
| Nacos API | 8848 | 1Panel | 服务注册/配置中心 |
| Nacos gRPC | 9848 | 1Panel | Nacos 2.x 通讯 |
| Sentinel Dashboard | 8858 | 业务自管 | 流控面板 |

### 2.7 监控运维

| 服务名 | 宿主端口 | 用途 |
|--------|---------|------|
| Nginx (前端) | 80 (HTTP) / 443 (HTTPS) | 前端反代与 API 网关入口 |
| Prometheus | 9090 | 监控采集 |
| Grafana | 3001 | 监控面板（⚠️ 不使用 3000） |
| Alertmanager | 9093 | 告警管理 |
| Alert Webhook | 9095 | 告警通知分发（钉钉/飞书/邮件） |
| Node Exporter | 9100 | 主机指标采集 |
| MySQLd Exporter | 9104 | MySQL 指标采集 |
| Redis Exporter | 9121 | Redis 指标采集 |
| Blackbox Exporter | 9115 | 黑盒探测 |

### 2.8 端口分配原则

1. **8100-8119**: 业务微服务专用区间，按服务注册顺序分配，已用 11 个端口（8100-8115）
2. **9000-9099**: 网关与入口服务区间（避开 1Panel Nacos 8080）
3. **3000-3099**: 前端 SSR 与监控可视化区间
4. **9090-9099 / 9100-9199**: 监控与 Exporter 区间
5. **3306/5672/6379/8848/8080**: 基础设施标准端口（1Panel 管理，不可变更）
6. **预留扩展**: 8116-8119 留作未来新业务服务
7. **禁止**: 任何服务不得使用已分配端口，新增服务需在本文档登记并经过审批

---

## 三、路由命名规范

### 3.1 URL 结构标准

```
https://api.tailoris.com/api/{service}/{resource}/{action?}/{id?}
                  └─┬─┘ └──┬───┘ └──┬─────┘ └──┬────┘ └─┬──┘
                    │      │        │          │        └─ 资源 ID（路径参数）
                    │      │        │          └────────── 可选动作（如 audit/process）
                    │      │        └───────────────────── 资源名称（单数）
                    │      └────────────────────────────── 服务域（user/product/order...）
                    └───────────────────────────────────── 统一前缀（/api/）
```

### 3.2 强制规范

| 规则 | 要求 | 示例 |
|------|------|------|
| 统一前缀 | 所有 API 必须以 `/api/` 开头 | `/api/user/info` ✅ ; `/user/info` ❌ |
| 版本化（推荐） | 公开 API 使用 `/api/v1/{resource}/**` 形式 | `/api/v1/payment/wechat` |
| 简化路径（容忍） | 内部业务可省略 v1，直接 `/api/{service}/**` | `/api/order/list` |
| 资源命名 | 资源名用名词单数 | `/api/user` ✅ ; `/api/users` ❌ |
| 操作表达 | 操作用 HTTP 方法区分，不写入 URL | `DELETE /api/cart/{id}` ✅ ; `POST /api/cart/delete` ❌ |
| 路径参数 | 资源 ID 用路径参数 `/{id}` | `/api/order/{orderNo}` |
| 查询参数 | 列表/筛选/分页用查询参数 | `/api/product/list?page=1&size=20` |
| 禁止层级前缀 | 不使用 `/api/core/` 或 `/api/lite/` 前缀 | 由 Nginx 按服务路径分流 |
| 管理端前缀 | 平台管理端统一使用 `/api/admin/**` | `/api/admin/users` |
| 商家端前缀 | 商家后台统一使用 `/api/merchant/**` | `/api/merchant/shop` |

### 3.3 权限控制策略

后端使用 Sa-Token 框架进行权限控制，注解策略如下：

| 注解 | 含义 | 适用场景 |
|------|------|---------|
| `@SaCheckLogin` | 必须登录 | 用户中心、订单、购物车、IM 等 C 端业务 |
| `@SaCheckRole("admin")` | 需平台管理员角色 | 平台管理后台 `/api/admin/**` |
| `@SaCheckRole("merchant")` | 需商家角色 | 商家专属操作（如图案发布） |
| `@SaCheckRole("merchant_owner")` | 需商家主角色 | 商家权限管理、角色增删 |
| `@SaIgnore` | 显式跳过鉴权 | 登录、注册、健康检查 |
| 无注解 | 默认放行 | 公开 API（商品列表、商品详情等） |

### 3.4 参数传递规范

| 参数类型 | 传递方式 | 示例 |
|---------|---------|------|
| 资源 ID | 路径参数 | `GET /api/order/{orderNo}` |
| 列表分页 | 查询参数 | `GET /api/product/list?page=1&size=20` |
| 筛选条件 | 查询参数 | `GET /api/product/list?categoryId=1&status=1` |
| 创建/更新 | Request Body (JSON) | `POST /api/order` + JSON body |
| 文件上传 | multipart/form-data | `POST /api/upload/{bizType}` |
| 用户身份 | Header `X-User-Id`（由网关注入） | 网关解析 token 后注入下游服务 |

### 3.5 HTTP 方法语义

| 方法 | 语义 | 幂等性 | 示例 |
|------|------|--------|------|
| GET | 查询资源 | ✅ 幂等 | `GET /api/product/{id}` |
| POST | 创建资源 / 触发动作 | ❌ 非幂等 | `POST /api/order` |
| PUT | 全量更新资源 | ✅ 幂等 | `PUT /api/user/info` |
| DELETE | 删除资源 | ✅ 幂等 | `DELETE /api/cart/{id}` |
| PATCH | 部分更新（本项目暂未使用） | ❌ 非幂等 | - |

---

## 四、后端路由清单

> **数据来源**: 实际扫描 `tailor-is/tailor-is-*/src/main/java/**/*Controller.java` 文件。
> **统计**: 共 30+ Controller，分布于 14 个微服务模块。

### 4.1 user-service (端口 8101)

| Controller | 类级 @RequestMapping | 鉴权 | 方法路由 |
|-----------|---------------------|------|---------|
| AuthController | `/api/auth` | 公开 + RateLimit | `POST /login`, `POST /login/code`, `POST /register`, `POST /register/email` |
| UserController | `/api/user` | `@SaCheckLogin` (类级) | `GET /info`, `PUT /info`, `PUT /real-name-auth`, `GET /list` (`@SaCheckRole("admin")`) |
| RoleController | `/api/user/roles` | 混合 | 角色查询/分配（部分 `@SaCheckRole("admin")`） |
| AddressController | `/api/user/address` | `@SaCheckLogin` (类级) | 地址增删改查 |

### 4.2 product-service (端口 8102)

| Controller | 类级 @RequestMapping | 鉴权 | 方法路由 |
|-----------|---------------------|------|---------|
| ProductController | `/api/product` | Header 鉴权 | `POST /`, `PUT /{id}`, `DELETE /{id}`, `GET /{id}`, `GET /list`, `GET /shop/{shopId}`, `PUT /{id}/status`, `GET /type/{productType}`, `GET /{id}/skus`, `GET /{id}/tags` |
| PublicProductController | `/api/products` | 公开 | `GET /categories`, `GET /hot`, `GET /new`, `GET /{id}`, `GET /seckill` |
| CategoryController | `/api/product/category` | `@SaCheckLogin` (类级) | `GET /tree`, `GET /list`, `PUT /{id}`, `PUT /{id}/status` |
| ProductSkuController | `/api/product/sku` | `@SaCheckLogin` (类级) | SKU 增删改查 |
| ProductSearchController | `/api/product/search` | 公开 | 商品搜索 |
| ProductFavoriteController | `/api/favorite` | 公开 | `POST /{productId}`, `POST /cancel/{productId}`, `GET /check/{productId}`, `GET /list` |
| ReviewController | `/api/product/review` | `@SaCheckLogin` (类级) | `GET /{productId}`, `DELETE /{id}`, `GET /admin/pending`, `PUT /{id}/audit` |
| AdminProductController | `/api/admin/product` | `@SaCheckRole("admin")` (类级) | `GET /list`, `PUT /audit/{id}`, `GET /stats` |

### 4.3 order-service (端口 8103)

| Controller | 类级 @RequestMapping | 鉴权 | 方法路由 |
|-----------|---------------------|------|---------|
| OrderController | `/api/order` | `@SaCheckLogin` (类级) | `GET /{orderNo}`, `GET /list`, `PUT /{orderNo}/pay`, `PUT /{orderNo}/confirm`, `PUT /{orderNo}/cancel`, `POST /payment/status/update` |
| CartController | `/api/cart` | `@SaCheckLogin` (类级) | `PUT /{id}`, `DELETE /{id}`, `POST /checkout` |
| LogisticsController | `/api/order` | `@SaCheckLogin` (类级) | `PUT /{orderId}/logistics`, `GET /{orderId}/logistics`, `GET /logistics/track` |
| AfterSaleController | `/api/order/after-sale` | `@SaCheckLogin` (类级) | `GET /{ticketNo}`, `GET /list`, `PUT /{ticketNo}/process` |
| AdminOrderController | `/api/admin` | `@SaCheckRole("admin")` (类级) | `GET /order/list`, `GET /order/stats`, `GET /after-sale/pending`, `PUT /after-sale/arbitrate` |

### 4.4 payment-service (端口 8104)

| Controller | 类级 @RequestMapping | 鉴权 | 方法路由 |
|-----------|---------------------|------|---------|
| PayController | `/api/v1/payment` | 部分 `@SaCheckLogin` | `POST /wechat`, `POST /alipay`, `POST /wechat/callback`, `POST /alipay/callback`, `GET /status`, `GET /wechat/query`, `GET /alipay/query`, `POST /refund`, `POST /wechat/native`, `POST /alipay/qrcode`, `POST /alipay/app`, `POST /wechat/close`, `GET /refund/query` |
| SettlementController | `/api/settlement` | `@SaCheckLogin` (类级) | `GET /list`, `GET /detail`, `POST /batch`, `POST /order` |
| AccountController | `/api/account` | `@SaCheckLogin` (类级) | `GET /user/info`, `GET /merchant/info`, `POST /merchant/withdraw`, `POST /user/recharge`, `POST /user/recharge/callback`, `GET /merchant/withdraw/detail` |
| EscrowController | `/api/v1/payment/escrow` | 部分 `@SaCheckRole("admin")` | `GET /balance`, `GET /detail`, `POST /deposit`, `POST /release`, `POST /freeze`, `POST /unfreeze` |
| ReconciliationController | `/api/v1/payment/reconciliation` | 部分 `@SaCheckRole("admin")` | `POST /record`, `POST /execute`, `POST /batch-execute`, `GET /unreconciled` |
| SandboxTestController | `/api/sandbox` | 公开（测试） | 沙箱测试接口 |

### 4.5 marketing-service (端口 8105)

| Controller | 类级 @RequestMapping | 鉴权 | 方法路由 |
|-----------|---------------------|------|---------|
| CouponController | `/api/marketing/coupon` | `@SaCheckLogin` (类级) | 优惠券增删改查 |
| SeckillController | `/api/marketing/seckill` | `@SaCheckLogin` (类级) | 秒杀活动 |
| PointsController | `/api/marketing/points` | `@SaCheckLogin` (类级) | 积分系统 |
| MemberController | `/api/marketing/member` | `@SaCheckLogin` (类级) | 会员体系 |
| MktPromotionController | `/api/marketing/promotion` | 公开 | 促销活动 |
| MktGroupBuyController | `/api/marketing/group-buy` | 公开 | 拼团活动 |
| MktOrderMatchController | `/api/marketing/order-match` | 公开 | 订单匹配 |
| MktStatisticsController | `/api/marketing/stats` | 公开 | 营销统计 |

### 4.6 ai-service (端口 8106)

| Controller | 类级 @RequestMapping | 鉴权 | 方法路由 |
|-----------|---------------------|------|---------|
| PatternController | `/api/v1/ai/pattern` | `@SaCheckLogin` (类级) | AI 图案生成 |
| BodySizeController | `/api/v1/ai/size` | `@SaCheckLogin` (类级) | 体型识别 |

### 4.7 copyright-service (端口 8107)

| Controller | 类级 @RequestMapping | 鉴权 | 方法路由 |
|-----------|---------------------|------|---------|
| CopyrightController | `/api/v1/copyright` | `@SaCheckLogin` (类级) | `POST /register`, `POST /register-hash`, `GET /certificate`, `GET /verify`, `POST /verify-hash`, `GET /list`, `GET /detail`, `POST /authorize`, `POST /blockchain/register`, `POST /blockchain/batch-register`, `GET /blockchain/verify`, `GET /blockchain/certificate/{txHash}`, `GET /blockchain/health` |
| InfringementController | `/api/v1/copyright/infringement` | `@SaCheckLogin` (类级) | 侵权举报 |

### 4.8 merchant-service (端口 8110)

| Controller | 类级 @RequestMapping | 鉴权 | 方法路由 |
|-----------|---------------------|------|---------|
| MerchantController | `/api/merchant` | `@SaCheckLogin` (类级) | `POST /apply`, `GET /info`, `GET /info/{id}`, `GET /list`, `PUT /audit` |
| ShopController | `/api/merchant/shop` | `@SaCheckLogin` (类级) | 店铺管理 |
| EmployeeController | `/api/merchant/employee` | `@SaCheckLogin` (类级) | 员工管理 |
| AdminMerchantController | `/api/admin/merchant` | `@SaCheckRole("admin")` (类级) | `GET /pending`, `PUT /audit/{id}`, `PUT /freeze/{id}`, `PUT /unfreeze/{id}`, `GET /list`, `GET /{id}` |
| MerchantDashboardController | `/api/v1/merchant/dashboard` | `@SaCheckLogin` (方法级) | 商家仪表盘（多个查询接口） |
| MerchantRoleController | `/api/v1/merchant/role` | 混合 | 角色管理（部分 `@SaCheckRole("merchant_owner")`） |
| MerchantNoticeController | `/api/v1/merchant/notices` | `@SaCheckLogin` (方法级) | 商家通知 |
| MerchantViolationController | `/api/v1/merchant/violation` | 混合 | 违规管理（部分 `@SaCheckRole("admin")`） |
| MerchantTrialAssessmentController | `/api/v1/merchant/trial` | 部分 `@SaCheckRole("admin")` | 试用期评估 |
| MerchantShopSwitchController | `/api/v1/merchant/shop-switch` | `@SaCheckLogin` (方法级) | 多店铺切换 |

### 4.9 admin-service (端口 8100)

| Controller | 类级 @RequestMapping | 鉴权 | 方法路由 |
|-----------|---------------------|------|---------|
| AdminUserController | `/api/admin/users` | `@SaCheckRole("admin")` (类级) | `GET /{id}`, `PUT /{id}`, `PUT /{id}/status`, `POST /{id}/password/reset`, `PUT /{id}/roles` |
| AdminRoleController | `/api/admin/roles` | `@SaCheckRole("admin")` (类级) | `GET /{id}`, `PUT /{id}`, `DELETE /{id}`, `GET /{id}/permissions`, `PUT /{id}/permissions`, `GET /{id}/users`, `GET /all` |
| AdminAuditLogController | `/api/admin/audit-logs` | `@SaCheckRole("admin")` (类级) | `GET /{id}` 等 |
| AdminDashboardController | `/api/admin/dashboard` | `@SaCheckRole("admin")` (类级) | `GET /stats` |
| AdminOrderController | `/api/admin/order` | `@SaCheckRole("admin")` (类级) | `GET /list`, `GET /{orderNo}`, `PUT /arbitrate` |
| AdminProductController | `/api/admin/product` | `@SaCheckRole("admin")` (类级) | `GET /pending`, `PUT /audit/{id}`, `PUT /reject/{id}` |
| AdminMerchantController | `/api/admin/merchant` | `@SaCheckRole("admin")` (类级) | （与 merchant-service 同路径，admin-service 同样提供） |
| AdminCommunityController | `/api/admin/community` | `@SaCheckRole("admin")` (类级) | `GET /reports`, `PUT /report/process`, `DELETE /post/{id}`, `PUT /post/audit/{id}` |
| AdminSystemSettingsController | `/api/system/settings` | `@SaCheckRole("admin")` (类级) | `PUT /basic`, `PUT /security`, `PUT /third-party` |

### 4.10 community-service (端口 8108)

| Controller | 类级 @RequestMapping | 鉴权 | 方法路由 |
|-----------|---------------------|------|---------|
| PostController | `/api/community/post` | `@SaCheckLogin` (类级) | `POST /create`, `PUT /update/{postId}`, `DELETE /delete/{postId}`, `GET /detail/{postId}`, `GET /list`, `GET /my`, `POST /top/{postId}` (`@SaCheckRole("admin")`), `POST /essence/{postId}` (`@SaCheckRole("admin")`) |
| PublicPostController | `/api/community/posts` | 公开 | `GET /{id}` |
| CommentController | `/api/community/comment` | `@SaCheckLogin` (类级) | `POST /create`, `GET /list/{postId}`, `DELETE /delete/{commentId}`, `GET /my` |
| InteractionController | `/api/community/interaction` | `@SaCheckLogin` (类级) | 点赞/收藏/关注（共 12 个接口） |
| CommunityTopicController | `/api/community/topic` | 公开 | `PUT /{id}`, `DELETE /{id}`, `GET /{id}`, `GET /hot`, `GET /list` |
| CommunityDiscoveryController | `/api/community/discover` | 公开 | `GET /hot`, `GET /latest`, `GET /following`, `GET /topic/{topicId}`, `GET /recommend`, `GET /topics/hot`, `GET /user/{userId}`, `GET /search` |
| CommunityMessageController | `/api/community/message` | Header 鉴权 | `GET /list`, `GET /unread-count`, `POST /{id}/read`, `POST /read-all` |
| CommunityReportController | `/api/community/report` | 公开 | `GET /list`, `POST /{id}/handle`, `GET /{id}/actions` |
| CommunityBlockController | `/api/community/block` | Header 鉴权 | `DELETE /{blockedUserId}`, `GET /list` |
| AdminCommunityController | `/api/admin/community` | `@SaCheckRole("admin")` (类级) | `POST /post/audit/{postId}`, `DELETE /post/delete/{postId}`, `POST /report/create`, `POST /report/process/{reportId}`, `GET /report/list`, `GET /report/detail/{reportId}` |

### 4.11 supply-service (端口 8109)

| Controller | 类级 @RequestMapping | 鉴权 | 方法路由 |
|-----------|---------------------|------|---------|
| SupplyController | `/api/supply` | `@SaCheckLogin` (类级) | `POST /create`, `PUT /update/{postId}`, `DELETE /delete/{postId}`, `GET /detail/{postId}`, `GET /list`, `GET /my` |
| MatchController | `/api/supply/match` | `@SaCheckLogin` (类级) | `GET /find`, `GET /recommend` |
| ContactController | `/api/supply/contact` | `@SaCheckLogin` (类级) | `POST /create`, `GET /list`, `POST /reply/{contactId}` |

### 4.12 message-service (端口 8111)

| Controller | 类级 @RequestMapping | 鉴权 | 方法路由 |
|-----------|---------------------|------|---------|
| MessageController | `/api/message` | `@SaCheckLogin` (类级) | `GET /list`, `POST /read/{messageId}`, `POST /read-all`, `GET /unread-count`, `POST /send-template` |

### 4.13 academy-service (端口 8112)

| Controller | 类级 @RequestMapping | 鉴权 | 方法路由 |
|-----------|---------------------|------|---------|
| CourseController | `/api/v1/academy` | `@SaCheckLogin` (类级) | 课程管理 |

### 4.14 analytics-service (端口 8113)

| Controller | 类级 @RequestMapping | 鉴权 | 方法路由 |
|-----------|---------------------|------|---------|
| MetricsController | `/api/v1/analytics` | `@SaCheckLogin` (类级) | `GET /metrics/{type}`, `POST /metrics` |

### 4.15 message-im-service (端口 8114)

| Controller | 类级 @RequestMapping | 鉴权 | 方法路由 |
|-----------|---------------------|------|---------|
| ImController | `/api/v1/im` | `@SaCheckLogin` (类级) | `GET /conversations`, `GET /messages/{conversationId}` |
| WebSocket 端点 | `/ws/**` | Token 校验（连接时） | IM 实时通讯 |

### 4.16 pattern-service (端口 8115)

| Controller | 类级 @RequestMapping | 鉴权 | 方法路由 |
|-----------|---------------------|------|---------|
| PatternController | `/api/v1/pattern` | 部分 `@SaCheckRole("merchant")` | `GET /list`, `GET /{id}` 等 |

### 4.17 common (端口 8082，按需启用)

| Controller | 类级 @RequestMapping | 鉴权 | 方法路由 |
|-----------|---------------------|------|---------|
| FileUploadController | `/api/upload` | 公开 | `POST /{bizType}`, `POST /{bizType}/batch`, `POST /delete` |

> **路径前缀差异说明**: 部分服务（payment、ai、copyright、academy、analytics、pattern、im、merchant 部分接口）使用 `/api/v1/...` 前缀，其他服务使用 `/api/{service}/...`。两者均合法，但**新增服务推荐统一使用 `/api/v1/{service}/...` 形式**以符合 §3.2 版本化要求。

---

## 五、前端路由清单

### 5.1 pc-mall（PC 商城，域名 www.tailoris.com，base `/`）

| 路径 | 名称 | 鉴权 | 说明 |
|------|------|------|------|
| `/` | Home | 公开 | 首页 |
| `/products` | Products | 公开 | 商品列表 |
| `/product/:id` | ProductDetail | 公开 | 商品详情 |
| `/cart` | Cart | `requiresAuth` | 购物车 |
| `/checkout` | Checkout | `requiresAuth` | 结算 |
| `/orders` | Orders | `requiresAuth` | 订单列表 |
| `/order/:id` | OrderDetail | `requiresAuth` | 订单详情 |
| `/profile` | Profile | `requiresAuth` | 个人中心 |
| `/community` | Community | 公开 | 社区 |
| `/community/:postId` | PostDetail | 公开 | 帖子详情 |
| `/merchant-apply` | MerchantApply | `requiresAuth` | 商家入驻 |
| `/login` | Login | 公开 | 登录 |
| `/register` | Register | 公开 | 注册 |
| `/forgot-password` | ForgotPassword | 公开 | 找回密码 |

> **路由守卫**: 仅做 token 存在性检查，角色/权限校验交由后端处理（C-012 修复）。

### 5.2 merchant-admin（商户后台，域名 merchant.tailoris.com，base `/merchant`）

| 路径 | 名称 | 鉴权 | 说明 |
|------|------|------|------|
| `/login` | Login | 公开 | 商户登录 |
| `/register` | Register | 公开 | 商户注册 |
| `/forgot-password` | ForgotPassword | 公开 | 找回密码 |
| `/dashboard` | Dashboard | `requiresAuth` | 仪表盘 |
| `/product` | ProductList | `requiresAuth` | 商品管理 |
| `/product/create` | ProductCreate | `requiresAuth` | 创建商品 |
| `/product/edit/:id` | ProductEdit | `requiresAuth` | 编辑商品 |
| `/order` | OrderList | `requiresAuth` | 订单管理 |
| `/order/:orderNo` | OrderDetail | `requiresAuth` | 订单详情 |
| `/aftersale` | AfterSaleList | `requiresAuth` | 售后工单 |
| `/aftersale/:ticketNo` | AfterSaleDetail | `requiresAuth` | 工单详情 |
| `/shop/employees` | EmployeeList | `requiresAuth` | 员工管理 |
| `/shop/settings` | ShopSettings | `requiresAuth` | 店铺设置 |
| `/finance/settlement` | FinanceSettlement | `requiresAuth` | 财务结算 |
| `/finance/withdraw` | FinanceWithdraw | `requiresAuth` | 提现管理 |
| `/marketing/coupon` | CouponList | `requiresAuth` | 优惠券 |
| `/marketing/seckill` | SeckillList | `requiresAuth` | 秒杀活动 |

### 5.3 platform-admin（平台后台，域名 admin.tailoris.com，base `/admin`）

| 路径 | 名称 | 鉴权 | 说明 |
|------|------|------|------|
| `/login` | Login | 公开 | 管理员登录 |
| `/dashboard` | Dashboard | `requiresAuth` | 仪表盘 |
| `/users` | UserManagement | `requiresAuth` | 用户管理 |
| `/roles` | RoleManagement | `requiresAuth` | 角色管理 |
| `/audit-logs` | AuditLog | `requiresAuth` | 操作日志 |
| `/settings` | SystemSettings | `requiresAuth` | 系统设置 |

### 5.4 mobile-app（移动端，多端编译：H5/微信/支付宝/百度/抖音小程序）

| 页面路径 | 说明 | Tab 项 |
|---------|------|--------|
| `pages/index/index` | 首页 | ✅ Tab |
| `pages/category/category` | 分类 | ✅ Tab |
| `pages/cart/cart` | 购物车 | ✅ Tab |
| `pages/user/user` | 我的 | ✅ Tab |
| `pages/login/login` | 登录 | - |
| `pages/register/register` | 注册 | - |
| `pages/product/detail` | 商品详情 | - |
| `pages/order/confirm` | 确认订单 | - |
| `pages/order/list` | 我的订单 | - |
| `pages/order/detail` | 订单详情 | - |
| `pages/order/offline-aftersale` | 离线售后工单 | - |
| `pages/address/list` | 地址管理 | - |
| `pages/address/edit` | 编辑地址 | - |
| `pages/community/list` | 社区 | - |
| `pages/community/detail` | 帖子详情 | - |
| `pages/merchant/apply` | 商家入驻 | - |

---

## 六、网关路由配置

### 6.1 网关路由架构

```
客户端请求
    │
    ▼
  Nginx (80/443)
    │
    ├── /api/{user,auth,product,products,favorite,order,cart,
    │        payment,settlement,account,sandbox,marketing,coupon,
    │        points,seckill,ai,body-size,copyright,merchant,shop,
    │        admin,pattern}/**
    │        └──→ core-gateway:9000  (容器内 8080)
    │
    ├── /api/{community,post,comment,supply,message,notice,
    │        academy,course,analytics,metrics,dashboard,
    │        im,im-message}/**
    │        └──→ lite-gateway:8081
    │
    ├── /api/products/   (绕过网关，直连 product-service:8102)
    ├── /api/community/  (绕过网关，直连 community-service:8108)
    │
    ├── /                └──→ 前端静态资源 (pc-mall)
    ├── /merchant/       └──→ 前端静态资源 (merchant-admin)
    ├── /admin/          └──→ 前端静态资源 (platform-admin)
    ├── /graphql         └──→ graphql-gateway:4000
    └── /ws/**           └──→ message-im-service:8114 (WebSocket)
```

### 6.2 core-gateway 路由表（权威配置）

> **来源**: `tailor-is/tailor-is-core-gateway/src/main/java/com/tailoris/coregateway/config/CoreGatewayRouteConfig.java`
>
> **单源约束**: core-gateway 路由**只能**在 `CoreGatewayRouteConfig.java` 中定义，**严禁**在 `application.yml` 中重复定义。

| # | route id | uri | predicates (Path) | filters |
|---|----------|-----|-------------------|---------|
| 1 | user-route | `lb://tailor-is-user` | `/api/user/**`, `/api/auth/**` | StripPrefix=0 |
| 2 | product-route | `http://host.docker.internal:8102` * | `/api/product/**`, `/api/products/**`, `/api/favorite/**` | StripPrefix=0 |
| 3 | order-route | `lb://tailor-is-order` | `/api/order/**`, `/api/cart/**` | StripPrefix=0 |
| 4 | payment-route | `lb://tailor-is-payment` | `/api/payment/**`, `/api/settlement/**`, `/api/account/**`, `/api/sandbox/**` | StripPrefix=0 |
| 5 | marketing-route | `lb://tailor-is-marketing` | `/api/marketing/**`, `/api/coupon/**`, `/api/points/**`, `/api/seckill/**` | StripPrefix=0 |
| 6 | ai-route | `lb://tailor-is-ai` | `/api/ai/**`, `/api/body-size/**` | StripPrefix=0 |
| 7 | copyright-route | `lb://tailor-is-copyright` | `/api/copyright/**` | StripPrefix=0 |
| 8 | community-route | `lb://tailor-is-community` | `/api/community/**` | StripPrefix=0 |
| 9 | merchant-route | `lb://tailor-is-merchant` | `/api/merchant/**`, `/api/shop/**` | StripPrefix=0 |
| 10 | admin-route | `lb://tailor-is-admin` | `/api/admin/**` | StripPrefix=0 |
| 11 | pattern-route | `lb://tailor-is-pattern` | `/api/pattern/**` | StripPrefix=0 |

> *product-route 当前使用 `http://host.docker.internal:8102` 而非 `lb://tailor-is-product`，应规范化为 `lb://` 形式（参见 §10 变更日志 v3.0 待办）。

### 6.3 lite-gateway 路由表（权威配置）

> **来源**: `tailor-is/tailor-is-lite-gateway/src/main/java/com/tailoris/litegateway/config/LiteGatewayRouteConfig.java`

| # | route id | uri | predicates (Path) | filters |
|---|----------|-----|-------------------|---------|
| 1 | community-route | `lb://tailor-is-community` | `/api/community/**`, `/api/post/**`, `/api/comment/**` | StripPrefix=0 |
| 2 | academy-route | `lb://tailor-is-academy` | `/api/academy/**`, `/api/course/**` | StripPrefix=0 |
| 3 | supply-route | `lb://tailor-is-supply` | `/api/supply/**` | StripPrefix=0 |
| 4 | message-route | `lb://tailor-is-message` | `/api/message/**`, `/api/notice/**` | StripPrefix=0 |
| 5 | im-route | `lb://tailor-is-message-im` | `/api/im/**`, `/api/im-message/**` | StripPrefix=0 |
| 6 | analytics-route | `lb://tailor-is-analytics` | `/api/analytics/**`, `/api/metrics/**`, `/api/dashboard/**` | StripPrefix=0 |

### 6.4 网关配置硬性约束

1. **唯一来源**: 路由只能在 `CoreGatewayRouteConfig.java` / `LiteGatewayRouteConfig.java` 中定义，**严禁**在 `application.yml` 重复定义
2. **uri 格式**: 必须使用 `lb://tailor-is-{service-name}` 服务发现形式，**严禁** `http://localhost:PORT`
3. **route id 命名**: 统一使用 `{purpose}-route`（如 `user-route`）
4. **predicates 必填**: 所有路由必须至少一个 Path 谓词
5. **filters**: 内部调用使用 `StripPrefix=0`（保持原 path 完整传递给下游 Controller）
6. **双模策略**: 当前使用 Java Config 硬编码；后续可迁移到 Nacos 动态路由，迁移时需保证 Nacos 配置与本地配置一致

### 6.5 路由冲突说明

> **已知重叠**: `community-route` 在 core-gateway 和 lite-gateway 中**均有定义**（均匹配 `/api/community/**`）。
>
> **当前实际行为**: Nginx `frontend.conf` 通过 `^~` 前缀将 `/api/community/` **直接路由到 community-service:8108**，绕过两个网关。因此实际不会触发网关层冲突。
>
> **建议**: 在下一次清理中，从 core-gateway 移除 `community-route`，统一由 lite-gateway 路由社区流量。

---

## 七、Nginx 路由配置

### 7.1 配置文件清单

| 文件路径 | 用途 |
|---------|------|
| `deploy/nginx/default.conf` | 主配置（HTTP 80 + HTTPS 443） |
| `deploy/nginx/frontend.conf` | 1Panel OpenResty 前端配置（含 upstream 与详细 location） |
| `deploy/nginx/cdn-optimization.conf` | CDN 静态资源加速片段 |
| `deploy/nginx/security-headers.conf` | 安全响应头 |
| `deploy/nginx/rate-limiting.conf` | 速率限流 zone 定义 |
| `deploy/nginx/ssl.conf` | SSL 全局参数 |
| `deploy/nginx/https.conf` | HTTPS server 块 |
| `deploy/nginx/conf.d/cdn-cache.conf` | CDN 缓存配置 |

### 7.2 Nginx upstream 定义（frontend.conf）

| upstream 名称 | 目标地址 | 用途 |
|--------------|---------|------|
| `tailoris_core_gateway` | `host.docker.internal:9000` | 核心网关（宿主端口 9000） |
| `tailoris_product_service` | `192.168.1.12:8102` | 商品服务直连（绕过网关） |
| `tailoris_community_service` | `192.168.1.12:8108` | 社区服务直连（绕过网关） |
| `tailoris_lite_gateway` | `host.docker.internal:8081` | 轻量网关 |
| `tailoris_pc_ssr` | `host.docker.internal:3000` | PC 商城 SSR |

### 7.3 Nginx location 路由表（frontend.conf）

| location 模式 | 优先级 | 转发目标 | 限流 | 说明 |
|--------------|--------|---------|------|------|
| `/` (root) | - | 静态资源 `/usr/share/nginx/html/pc-mall` | - | PC 商城 SPA |
| `/merchant/` | - | 静态资源 `/usr/share/nginx/html/merchant-admin/` | - | 商户后台 SPA |
| `/admin/` | - | 静态资源 `/usr/share/nginx/html/platform-admin/` | - | 平台后台 SPA |
| `^~ /api/products/` | 高 | `tailoris_product_service` (8102) | api burst=20 | 商品列表（绕过网关） |
| `^~ /api/community/` | 高 | `tailoris_community_service` (8108) | api burst=20 | 社区（绕过网关） |
| `^~ /api/auth/login` | 高 | `tailoris_core_gateway` (9000) | login burst=5 | 登录（防暴力破解） |
| `~ ^/api/(user\|auth\|product\|favorite\|order\|cart\|payment\|settlement\|marketing\|coupon\|seckill\|ai\|body-size\|copyright\|merchant\|shop\|admin)(/\|$)` | 正则 | `tailoris_core_gateway` (9000) | api burst=20 | 核心业务 API |
| `~ ^/api/(community\|supply\|message\|message-im\|academy\|analytics\|pattern)(/\|$)` | 正则 | `tailoris_lite_gateway` (8081) | api burst=20 | 轻量业务 API |
| `/api/` | 低 | `tailoris_core_gateway` (9000) | api burst=20 | 默认兜底 |
| `/ws/` | - | `host.docker.internal:8114` | - | WebSocket (IM) |
| `/graphql` | - | `tailoris_core_gateway` (9000) | - | GraphQL API |
| `= /healthz` | - | 返回 200 "ok" | - | 健康检查 |
| `/nginx_status` | - | stub_status | - | 仅内网访问 |
| `~* /actuator/` | - | deny all (403) | - | 禁止外网访问 Actuator |

### 7.4 default.conf 配置要点

| 配置项 | HTTP (80) | HTTPS (443) |
|-------|-----------|-------------|
| 监听 | `listen 80` | `listen 443 ssl http2` |
| 重定向 | `return 301 https://$host$request_uri` | - |
| SSL 证书 | - | `/etc/nginx/ssl/fullchain.pem` + `privkey.pem` |
| SSL 协议 | TLSv1.2 / TLSv1.3 | TLSv1.2 / TLSv1.3 |
| HSTS | - | `max-age=31536000; includeSubDomains` |
| 静态资源缓存 | 1h | 1y (immutable) |
| `/api/auth/login` | core-gateway:8080 (⚠️ 应为 9000) | core-gateway:8080 (⚠️ 应为 9000) |
| `/api/pay/` | core-gateway:8080 (⚠️ 应为 9000) | core-gateway:8080 (⚠️ 应为 9000) |
| `/api/admin/` | core-gateway:8080 (⚠️ 应为 9000) | core-gateway:8080 (⚠️ 应为 9000) |
| `/api/products/` | `192.168.1.12:8102` | - |
| `/api/` | core-gateway:8080 (⚠️ 应为 9000) | core-gateway:8080 (⚠️ 应为 9000) |
| `/ws/` | `localhost:8114` | - |

> **⚠️ 已知问题**: `default.conf` 中所有 `proxy_pass` 仍指向 `core-gateway:8080`，未与 `frontend.conf` 一致更新为 `9000`。生产部署应优先使用 `frontend.conf`，或修复 `default.conf`。详见 §10 变更日志 v3.0 待办 PORT-008。

### 7.5 CDN 静态资源优化（cdn-optimization.conf）

| 资源类型 | 缓存策略 | 说明 |
|---------|---------|------|
| 带 hash 的 JS/CSS (`*.hash.js`) | `expires 365d; immutable` | 永久缓存 |
| 图片 (jpg/jpeg/gif/png/ico/webp/svg) | `expires 90d; immutable` | 长缓存 |
| 字体 (woff/woff2/ttf/otf/eot) | `expires 365d; immutable` | 永久缓存 + CORS |
| HTML 文件 | `no-store, no-cache, must-revalidate` | 不缓存 |
| gzip_static | on | 优先使用预压缩文件 |
| Brotli | 可选（需模块支持） | 比 Gzip 高 20-30% 压缩率 |

---

## 八、端口占用检测机制

### 8.1 检测脚本

**脚本路径**: `deploy/scripts/check-port-conflicts.sh`

**功能**:
1. 扫描宿主机当前监听的 TCP 端口（`ss -tlnp`，回退到 `netstat`）
2. 与本文档 §2 端口分配总表中的"标准登记表"比对
3. 输出三类告警：
   - **冲突告警（CRITICAL）**: 实际占用端口与标准登记的服务不一致
   - **未登记告警（WARNING）**: 实际占用端口不在标准登记表中
   - **缺失告警（INFO）**: 标准登记的服务端口未监听（可能未启动）
4. 退出码：0=无 CRITICAL，1=存在 CRITICAL 冲突

**使用方式**:

```bash
# 检测当前宿主机端口冲突
bash deploy/scripts/check-port-conflicts.sh

# 仅检查不退出非零（用于 CI 展示）
bash deploy/scripts/check-port-conflicts.sh --no-fail

# 指定额外端口白名单（如临时调试端口）
bash deploy/scripts/check-port-conflicts.sh --extra "8086,8087"
```

### 8.2 冲突告警规则

| 告警等级 | 触发条件 | 处理建议 |
|---------|---------|---------|
| 🔴 CRITICAL | 标准端口被非预期服务占用（如 9000 被 Nacos 占用） | 立即停止冲突服务，按 §11 回避策略处理 |
| 🟡 WARNING | 实际监听端口不在登记表中 | 确认服务身份；若为正式服务，按 §9 流程补登记 |
| 🔵 INFO | 登记端口未监听 | 检查服务是否启动；非关键服务可忽略 |

### 8.3 自动回避策略

当检测到端口冲突时，按以下优先级回避：

1. **优先保护 1Panel 端口**: 1Panel 管理的端口（3306/6379/5672/8080/8848/9848/11336/15672）绝对不可侵占
2. **业务服务回避**: 新增服务发现目标端口被占时，从预留区间（8116-8119）选择替代端口
3. **网关端口固定**: core-gateway 宿主端口 9000 不可变更，如被占用需排查占用方
4. **Docker 网络隔离**: 容器间通信通过 `tailoris-services` 网络（172.28.0.0/16）+ `host.docker.internal` 访问宿主服务，避免端口直接暴露

### 8.4 CI/CD 集成

在 `.github/workflows/ci-cd.yml` 中集成端口冲突检测：

```yaml
- name: 端口冲突检测
  run: bash deploy/scripts/check-port-conflicts.sh --no-fail
```

> **注意**: CI 环境中端口状态与生产不同，CI 中使用 `--no-fail` 仅作展示。生产部署前必须在目标主机执行严格检测（不带 `--no-fail`）。

---

## 九、变更审批流程

### 9.1 端口/路由变更申请表模板

```markdown
## 端口/路由变更申请

**申请日期**: YYYY-MM-DD
**申请人**: <姓名>
**变更类型**: [ ] 新增端口  [ ] 修改端口  [ ] 新增路由  [ ] 修改路由  [ ] 删除路由

### 变更内容
- **服务名**: tailor-is-xxx
- **当前端口/路由**: <旧值或"无">
- **目标端口/路由**: <新值>
- **变更原因**: <详细说明>

### 影响评估
- **影响的服务**: <列出受影响的上游/下游服务>
- **影响的配置文件**: <application.yml / docker-compose.yml / Nginx / Gateway Java Config>
- **回滚方案**: <回滚步骤>

### 验证清单
- [ ] 端口无冲突（运行 check-port-conflicts.sh）
- [ ] 路由无冲突（grep 同前缀的 Controller）
- [ ] Nacos 配置已同步
- [ ] Nginx 配置已同步
- [ ] docker-compose.yml 已同步
- [ ] 本规范文档已更新
```

### 9.2 审核人与职责

| 角色 | 职责 | 审核范围 |
|------|------|---------|
| 申请人 | 提交变更申请、实施变更、更新文档 | - |
| 架构师 | 审核架构合理性、路由命名规范、端口分配原则 | 所有路由/端口变更 |
| 运维工程师 | 审核部署影响、Nginx/防火墙/1Panel 影响 | 涉及端口暴露、Nginx 配置的变更 |
| 服务负责人 | 审核业务影响、上下游依赖 | 涉及具体业务服务的变更 |

### 9.3 实施步骤

1. **申请阶段**: 申请人填写 §9.1 申请表，提交 PR（含代码与文档变更）
2. **审核阶段**: 至少 1 名架构师 + 1 名运维工程师 Approve
3. **预发布验证**: 在预发布环境部署，运行 `check-port-conflicts.sh` 确认无冲突
4. **生产实施**: 按变更窗口实施，遵循"先停旧服务 → 修改配置 → 启动新服务 → 健康检查"流程
5. **文档同步**: 合并 PR 后，更新本文档 §2 端口分配总表与 §4 路由清单

### 9.4 验证清单

实施完成后逐项验证：

- [ ] `bash deploy/scripts/check-port-conflicts.sh` 退出码为 0
- [ ] 所有微服务健康检查 UP（`/actuator/health`）
- [ ] Nacos 服务列表显示全部 17 个微服务
- [ ] Nginx 配置语法检查通过（`nginx -t`）
- [ ] 关键路由可达性测试（curl 各服务 `/api/{service}/health` 或代表性接口）
- [ ] WebSocket 端点 `/ws/` 可正常握手
- [ ] 前端三套界面可正常加载（pc-mall / merchant-admin / platform-admin）
- [ ] Actuator 端点外网不可达（`curl https://api.tailoris.com/actuator/` 返回 403）

---

## 十、版本控制

### 10.1 变更日志

#### v3.0 (2026-07-02) - 本次修订

**合并内容**:
- 合并 `docs/Tailor-IS-系统路由与端口规范.md` (v2.0) 为主干
- 整合 `docs/ROUTING-PORT-STANDARD.md` (v1.0) 的网络安全策略、环境变量配置
- 整合 `tailor-is/docs/ROUTING-PORT-STANDARD.md` (v1.1) 的 CI/CD 防护、Gateway 路由权威配置

**新增章节**:
- §3 路由命名规范（含 RESTful 风格、版本化前缀、权限控制策略、参数传递方式）
- §4 后端路由清单（按 14 个微服务分组，列出全部 30+ Controller 路由）
- §5 前端路由清单（4 个子项目的完整路由表）
- §8 端口占用检测机制（含检测脚本说明、告警规则、回避策略）
- §9 变更审批流程（含申请表模板、审核人职责、实施步骤、验证清单）
- §10 版本控制（本章节）

**修订内容**:
- §2 端口分配总表补充公共服务（file-upload 8082 / graphql-gateway 4000）与前端服务端口
- §6 网关路由配置补充 core-gateway 与 lite-gateway 完整路由表（含 StripPrefix=0 约束）
- §7 Nginx 路由配置补充 upstream 定义与完整 location 路由表
- §11 1Panel 端口冲突回避保留并扩展

**待办事项（v3.0 识别但未实施）**:

| ID | 问题描述 | 建议方案 | 优先级 |
|----|---------|---------|--------|
| PORT-008 | `default.conf` 中 `proxy_pass` 仍指向 `core-gateway:8080`，未更新为 9000 | 统一改为 `core-gateway:9000` 或迁移到 `frontend.conf` | High |
| ROUTE-005 | `product-route` 在 `CoreGatewayRouteConfig.java` 中使用 `http://host.docker.internal:8102`，不符合 `lb://` 规范 | 改为 `lb://tailor-is-product` | Medium |
| ROUTE-006 | `community-route` 在 core-gateway 和 lite-gateway 中重复定义 | 从 core-gateway 移除，统一由 lite-gateway 路由 | Medium |
| ROUTE-007 | `/api/products/` 与 `/api/community/` 在 Nginx 中绕过网关直连服务，与"网关收敛"原则不一致 | 评估是否回归网关路由，或显式记录为"性能优化例外" | Low |
| ROUTE-008 | 部分服务使用 `/api/v1/...` 前缀，部分使用 `/api/{service}/...`，前缀不统一 | 新增服务统一使用 `/api/v1/{service}/...`，存量服务逐步迁移 | Low |

#### v2.0 (2026-06-19) - 已合并

- core-gateway 宿主端口从 8080 改为 9000（PORT-003 修复）
- frontend.conf upstream 修正为 9000/8081（PORT-001 修复）
- alert-webhook 改为 bridge 模式，端口改为 9095（PORT-002 修复）
- nginx-frontend 端口改为 80（PORT-004 修复）
- Grafana 统一为 3001（PORT-005 修复）
- 三套 compose 密码使用 .env 统一管理（PORT-007 修复）
- 网关路由方案改为按服务路径正则分流（ROUTE-001 修复）
- 删除遗留 `deploy/gateway-application.yml`（ROUTE-002 修复）
- k8s ingress 修复 rewrite-target 与 lite-gateway 路由（ROUTE-003/004 修复）
- Prometheus 服务标签对齐与补充监控目标（DEP-001/002 修复）

#### v1.1 (2026-06-09) - 已合并

- 16 服务端口分配表（与 `platform-admin/DashboardView.vue` 对齐）
- Gateway 路由权威配置（17 条路由含 pattern）
- pre-commit 钩子与 CI Pipeline 强制门禁

#### v1.0 (2026-06-11) - 已合并

- 端口分配总表（核心服务 8000-8199 / 前端 3000-4999 / 基础设施）
- API 路由设计规范（URL 结构、路由表、前端页面路由）
- 网络安全策略（ufw 防火墙规则、Nginx 访问控制）
- 环境变量配置标准（.env.production）
- 端口变更流程

### 10.2 回滚策略

当路由/端口变更导致生产故障时，按以下步骤回滚：

1. **立即回滚配置**:
   ```bash
   # 回滚 Nginx 配置
   cp /etc/nginx/conf.d/frontend.conf.bak /etc/nginx/conf.d/frontend.conf
   nginx -s reload

   # 回滚 docker-compose
   cd deploy
   git checkout HEAD~1 -- docker-compose.services.yml
   docker compose -f docker-compose.services.yml up -d
   ```

2. **回滚代码**:
   ```bash
   # 回滚 Gateway 路由配置
   git revert <commit-sha>
   # 重新构建并部署 gateway
   docker compose -f docker-compose.services.yml build core-gateway lite-gateway
   docker compose -f docker-compose.services.yml up -d core-gateway lite-gateway
   ```

3. **回滚文档**: 本文档使用 Git 版本控制，回滚到上一个稳定 tag 即可

4. **回滚验证**: 执行 §9.4 验证清单确认回滚成功

### 10.3 文档废弃与归档

合并到 v3.0 后，以下两份历史文档**标记为废弃**，保留但不维护：

- `docs/ROUTING-PORT-STANDARD.md` (v1.0) - 顶部添加 `> ⚠️ 已废弃，请参考 docs/Tailor-IS-系统路由与端口规范.md (v3.0)`
- `tailor-is/docs/ROUTING-PORT-STANDARD.md` (v1.1) - 顶部添加同样的废弃标记

---

## 十一、1Panel 端口冲突回避

### 11.1 已识别的 1Panel 冲突端口

| 端口 | 1Panel 占用方 | 原项目占用 | 修复方案 | 状态 |
|------|--------------|-----------|---------|------|
| 8080 | Nacos 界面 | core-gateway / nginx-frontend / alert-webhook | core-gateway → 9000；nginx-frontend → 80；alert-webhook → 9095 | ✅ 已修复 |
| 3000 | - | Grafana (dev) 与 pc-mall SSR 冲突 | Grafana 生产统一 3001 | ✅ 已修复 |

### 11.2 1Panel 端口回避清单（不可变更）

以下端口**永远不得**用于 Docker Compose 服务映射：

```
3306  - MySQL (1Panel)
5672  - RabbitMQ AMQP (1Panel)
6379  - Redis (1Panel)
8080  - Nacos 界面 (1Panel) ⚠️ 最易冲突
8848  - Nacos API (1Panel)
9848  - Nacos gRPC (1Panel)
15672 - RabbitMQ Dashboard (1Panel)
11336 - 1Panel 面板
```

### 11.3 部署前检查清单

部署任何服务前，必须执行：

```bash
# 1. 检查目标端口是否被 1Panel 占用
bash deploy/scripts/check-port-conflicts.sh

# 2. 确认 docker-compose 端口映射不冲突
grep -E "^\s+-\s+\"[0-9]+:" deploy/docker-compose.*.yml | sort | uniq

# 3. 确认 Nginx upstream 与实际服务端口一致
grep -E "server\s+" deploy/nginx/frontend.conf
```

### 11.4 历史冲突修复记录

| 问题 ID | 严重等级 | 问题描述 | 修复方案 | 状态 |
|---------|---------|---------|---------|------|
| PORT-001 | Critical | frontend.conf upstream 指向 9001/9002，与实际网关端口 9000/8081 不匹配 | 修正 upstream 为 9000/8081 | ✅ 已修复 |
| PORT-002 | Critical | alert-webhook host 网络模式占用 8080，与 1Panel Nacos 冲突 | 改为 bridge 模式，端口改为 9095 | ✅ 已修复 |
| PORT-003 | Critical | core-gateway 宿主端口 8080 与 1Panel Nacos 界面端口冲突 | 改为 9000 | ✅ 已修复 |
| PORT-004 | Critical | nginx-frontend 端口 8080 与 1Panel Nacos 冲突 | 改为 80 | ✅ 已修复 |
| PORT-005 | High | Grafana 端口在 monitoring compose 中为 3000，与 PC SSR 3000 冲突 | 统一为 3001 | ✅ 已修复 |
| PORT-006 | High | core-gateway 宿主端口在 services.yml 中为 9000，其他为 8080 | 统一为 9000 | ✅ 已修复 |
| PORT-007 | High | 三套 compose 使用三套不同默认密码 | 使用 .env 统一管理 | ✅ 已修复 |
| ROUTE-001 | Critical | frontend.conf 使用 /api/core/ 和 /api/lite/ 前缀，网关无法匹配 | 改为按服务路径正则分流 | ✅ 已修复 |
| ROUTE-002 | Critical | deploy/gateway-application.yml 路由方案与 Java 配置完全不同 | 已删除该遗留文件 | ✅ 已修复 |
| ROUTE-003 | Critical | k8s ingress rewrite-target 剥离 /api 前缀 | 移除 rewrite-target 注解 | ✅ 已修复 |
| ROUTE-004 | Critical | k8s ingress 所有路由指向 core-gateway，缺少 lite-gateway | 添加 lite-gateway 路由规则 | ✅ 已修复 |
| DEP-001 | High | Prometheus 服务标签全面错位（10个服务标签偏移） | 重新对齐标签与端口 | ✅ 已修复 |
| DEP-002 | High | Prometheus 缺少 5 个微服务监控目标 | 补充全部 18 个服务目标 | ✅ 已修复 |
| PORT-008 | High | default.conf 中 proxy_pass 仍指向 core-gateway:8080，未更新为 9000 | 统一改为 9000 | ⏳ 待修复（v3.0 识别） |

---

**文档维护**: Tailor IS 架构组
**生效日期**: 2026-07-02
**下次复审**: 2026-10-02
**关联脚本**: `deploy/scripts/check-port-conflicts.sh`
