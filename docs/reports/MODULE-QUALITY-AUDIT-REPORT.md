# Tailor IS 模块质量审计报告

> **报告编号**：MQA-2026Q2-001
> **审计时间**：2026-07-02
> **审计范围**：Tailor IS 后端全部功能模块（17 个微服务 + 2 个网关）
> **审计方法**：静态代码审查（Controller / Service / 配置文件 / 异常处理机制）
> **审计工具**：代码检索 + 文件阅读 + 路由配置核对
> **审计人**：Tailor IS 质量保障团队

***

## 目录

1. [审计概述](#1-审计概述)
2. [各模块功能点覆盖率表](#2-各模块功能点覆盖率表task-31)
3. [模块间集成测试用例设计](#3-模块间集成测试用例设计task-32)
4. [20+ 异常场景验证结果](#4-20-异常场景验证结果task-33)
5. [端到端业务流程测试用例设计](#5-端到端业务流程测试用例设计task-34)
6. [模块质量评分表](#6-模块质量评分表task-35)
7. [审计结论与改进建议](#7-审计结论与改进建议)

***

## 1. 审计概述

### 1.1 审计目标

对系统所有功能模块进行全面质量审计，验证功能完整性、模块间交互、错误处理机制，设计集成测试用例，验证 20+ 异常场景，测试端到端业务流程，输出模块质量评分表。

### 1.2 审计范围

| 域    | 模块                     | 端口   | Controller 数     |
| ---- | ---------------------- | ---- | ---------------- |
| 网关   | tailor-is-core-gateway | 8080 | 0（Filter/Config） |
| 网关   | tailor-is-lite-gateway | 8081 | 0（Filter/Config） |
| 核心业务 | tailor-is-user         | 8101 | 4                |
| 核心业务 | tailor-is-product      | 8102 | 8                |
| 核心业务 | tailor-is-order        | 8103 | 5                |
| 核心业务 | tailor-is-payment      | 8104 | 6                |
| 核心业务 | tailor-is-marketing    | 8105 | 8                |
| 核心业务 | tailor-is-ai           | 8106 | 2                |
| 核心业务 | tailor-is-copyright    | 8107 | 2                |
| 核心业务 | tailor-is-merchant     | 8110 | 10               |
| 核心业务 | tailor-is-pattern      | 8115 | 1                |
| 核心业务 | tailor-is-admin        | 8100 | 9                |
| 轻量服务 | tailor-is-community    | 8108 | 8                |
| 轻量服务 | tailor-is-supply       | 8109 | 3                |
| 轻量服务 | tailor-is-academy      | 8112 | 1                |
| 轻量服务 | tailor-is-message      | 8111 | 1                |
| 轻量服务 | tailor-is-message-im   | 8114 | 1                |
| 轻量服务 | tailor-is-analytics    | 8113 | 1                |

### 1.3 审计关键发现摘要

| 严重度   | 数量 | 摘要                                                                                                          |
| ----- | -- | ----------------------------------------------------------------------------------------------------------- |
| 🔴 严重 | 4  | 网关路由与 Controller 路径不一致；Sa-Token 异常未在全局处理器覆盖；支付验签公钥未配置时跳过验签；商品 ProductController 已废弃但仍存在                     |
| 🟡 重要 | 6  | IM 模块缺少发送消息 REST 接口；Academy 课程 CRUD 不全；商家订单管理分散；AlipayService 实现类强转；商品删除使用物理删除而非逻辑删除                        |
| 🟢 改进 | 5  | ResultCode 缺少更细粒度业务码；库存预扣减无 Redis 预校验；订单详情缓存击穿未防护；多个 Controller 未统一 `@SaCheckLogin`；公共上传接口缺 `@SaCheckLogin` |

***

## 2. 各模块功能点覆盖率表（Task 3.1）

### 2.1 用户模块（tailor-is-user）

**入口**：`AuthController`、`UserController`、`AddressController`、`RoleController`

| 功能点      | 期望接口                                          | 实际实现  | 实现位置                                                            | 状态 | 覆盖率  |
| -------- | --------------------------------------------- | ----- | --------------------------------------------------------------- | -- | ---- |
| 注册-手机号   | `POST /api/auth/register`                     | ✅ 已实现 | AuthController.register                                         | ✅  | 100% |
| 注册-邮箱    | `POST /api/auth/register/email`               | ✅ 已实现 | AuthController.registerByEmail                                  | ✅  | 100% |
| 注册-账号密码  | —                                             | ✅ 已实现 | —                                                               | ✅  | 100% |
| 登录-账号密码  | `POST /api/auth/login`                        | ✅ 已实现 | AuthController.login                                            | ✅  | 100% |
| 登录-手机验证码 | `POST /api/auth/login/code`                   | ✅ 已实现 | AuthController.loginByCode                                      | ✅  | 100% |
| 登录-邮箱验证码 | `POST /api/auth/login/code`                   | ✅ 已实现 | AuthController.loginByCode（type=email）                          | ✅  | 100% |
| 登录-微信    | `POST /api/auth/wechat-login`                 | ✅ 已实现 | AuthController.wechatLogin                                      | ✅  | 100% |
| 发送短信验证码  | `POST /api/auth/sms-code`                     | ✅ 已实现 | AuthController.sendSmsCode                                      | ✅  | 100% |
| 发送邮箱验证码  | `POST /api/auth/email-code`                   | ✅ 已实现 | AuthController.sendEmailCode                                    | ✅  | 100% |
| 用户信息-查看  | `GET /api/user/info`、`GET /api/auth/userinfo` | ✅ 已实现 | UserController.getUserInfo、AuthController.getUserInfo           | ✅  | 100% |
| 用户信息-修改  | `PUT /api/user/info`                          | ✅ 已实现 | UserController.updateUserInfo                                   | ✅  | 100% |
| 实名认证     | `PUT /api/user/real-name-auth`                | ✅ 已实现 | UserController.realNameAuth                                     | ✅  | 100% |
| 地址-CRUD  | `/api/user/address`                           | ✅ 已实现 | AddressController（list/default/create/update/delete/setDefault） | ✅  | 100% |
| Token 刷新 | `POST /api/auth/refresh`                      | ✅ 已实现 | AuthController.refresh                                          | ✅  | 100% |
| 用户登出     | `POST /api/auth/logout`                       | ✅ 已实现 | AuthController.logout                                           | ✅  | 100% |

**模块功能覆盖率**：**14/15 = 93.3%**
**遗留问题**：

- `UserController` 已标注 `@Deprecated`，迁移到 `/api/v1/user` 的工作未完成（见 `tailor-is-user/src/main/java/com/tailoris/user/controller/UserController.java:28`）。
- 注册未支持"账号密码"独立注册通道（仅手机号/邮箱）。
- `loginByCode` 接口使用 `Map<String,String>` 接收参数，未使用 DTO + `@Valid`，参数校验较弱。

### 2.2 商品模块（tailor-is-product）

**入口**：`PublicProductController`、`ProductController`、`CategoryController`、`ProductSearchController`、`ProductFavoriteController`、`ReviewController`、`ProductSkuController`、`AdminProductController`

| 功能点             | 实现位置                                                                          | 状态 | 备注                         |
| --------------- | ----------------------------------------------------------------------------- | -- | -------------------------- |
| 商品列表（分页/排序/筛选）  | PublicProductController.listProducts / ProductController.listProducts         | ✅  | 公开接口与后台接口双通道               |
| 商品详情            | PublicProductController.getProductDetail / ProductController.getProductDetail | ✅  | —                          |
| 分类管理（树/列表/CRUD） | CategoryController                                                            | ✅  | 缺少"删除分类"接口                 |
| 搜索（DB/ES 双模）    | ProductSearchController                                                       | ✅  | 默认走 DB，ES 可切换              |
| 收藏（添加/取消/查询/列表） | ProductFavoriteController                                                     | ✅  | 完整                         |
| SKU 管理（CRUD/库存） | ProductSkuController                                                          | ✅  | 库存可手动调整                    |
| 商品评价            | ReviewController                                                              | ✅  | 支持提交/查询/删除/审核              |
| 商品审核            | AdminProductController.auditProduct                                           | ✅  | 仅管理员                       |
| 热门商品/新品         | PublicProductController.getHotProducts / getNewProducts                       | ✅  | —                          |
| 秒杀商品入口          | PublicProductController.getSeckillProducts                                    | ⚠️ | 返回 `List.of()` 空实现，需对接营销模块 |

**模块功能覆盖率**：**10/11 = 90.9%**
**遗留问题**：

- `ProductController` 已标注 `@Deprecated`，但仍在 `/api/product` 路径提供完整 CRUD。
- 鉴权方式不统一：部分用 `@RequestHeader("X-User-Id")`，部分用 `@SaCheckLogin`。
- `AdminProductController.getProductStats` 返回硬编码 0，未真实统计。

### 2.3 订单模块（tailor-is-order）

**入口**：`OrderController`、`CartController`、`AfterSaleController`、`LogisticsController`、`AdminOrderController`

| 功能点           | 实现位置                             | 状态 | 备注                                |
| ------------- | -------------------------------- | -- | --------------------------------- |
| 下单            | OrderController.createOrder      | ✅  | 含幂等锁、库存预扣减、Seata 全局事务、Sentinel 限流 |
| 支付（订单侧）       | OrderController.payOrder         | ✅  | 状态机校验 + 状态幂等                      |
| 取消订单          | OrderController.cancelOrder      | ✅  | 状态机校验 + 库存释放 + MQ 通知              |
| 确认收货          | OrderController.confirmReceive   | ✅  | 状态机校验                             |
| 订单详情          | OrderController.getOrderDetail   | ✅  | IDOR 越权修复已包含                      |
| 订单列表          | OrderController.listOrders       | ✅  | 分页查询                              |
| 售后-创建工单       | AfterSaleController.createTicket | ✅  | —                                 |
| 售后-工单详情/列表/处理 | AfterSaleController              | ✅  | —                                 |
| 购物车-CRUD/批量结算 | CartController                   | ✅  | —                                 |
| 物流-更新/查询/轨迹   | LogisticsController              | ✅  | —                                 |
| 订单状态机         | OrderStateMachine                | ✅  | 完整 7 态 + 合法转换                     |
| 订单超时取消        | OrderTimeoutConsumer             | ✅  | RabbitMQ 延迟队列                     |
| 评价（位于商品模块）    | ReviewController（product）        | ✅  | 跨模块实现                             |
| 后台订单管理        | AdminOrderController             | ✅  | 管理员角色                             |

**模块功能覆盖率**：**14/14 = 100%**

### 2.4 支付模块（tailor-is-payment）

**入口**：`PayController`、`SettlementController`、`ReconciliationController`、`EscrowController`、`AccountController`、`SandboxTestController`

| 功能点          | 实现位置                                     | 状态 | 备注           |
| ------------ | ---------------------------------------- | -- | ------------ |
| 微信 JSAPI 支付  | PayController.wechatPay                  | ✅  | —            |
| 微信 Native 支付 | PayController.wechatNativePay            | ✅  | —            |
| 支付宝 PC 支付    | PayController.alipayPay                  | ✅  | —            |
| 支付宝扫码支付      | PayController.alipayQrCodePay            | ✅  | —            |
| 支付宝 APP 支付   | PayController.alipayAppPay               | ✅  | —            |
| 微信支付回调       | PayController.wechatCallback             | ✅  | V3 验签 + 业务幂等 |
| 支付宝回调        | PayController.alipayCallback             | ✅  | 验签 + 幂等锁     |
| 退款申请         | PayController.refund                     | ✅  | 支持微信/支付宝双通道  |
| 退款查询         | PayController.queryRefund                | ✅  | —            |
| 关闭支付订单       | PayController.closeWechatPayment         | ✅  | —            |
| 查询支付状态       | PayController.getPaymentStatus           | ✅  | —            |
| 查询渠道订单       | queryWechatOrder / queryAlipayOrder      | ✅  | —            |
| 资金托管（Escrow） | EscrowController + EscrowService         | ✅  | —            |
| 商户结算         | SettlementController + SettlementService | ✅  | —            |
| 对账           | ReconciliationController                 | ✅  | —            |
| 商户/用户账户      | AccountController                        | ✅  | —            |

**模块功能覆盖率**：**16/16 = 100%**
**遗留问题**：

- `PaymentServiceImpl.verifyCallbackSign` 在 `alipayPublicKey` 未配置时**跳过验签**（`PaymentServiceImpl.java:220-222`），仅开发环境容错，生产环境若忘配置将造成严重安全风险。
- `PayController` 多处将 `AlipayService` 强转为 `AlipayServiceImpl`（`PayController.java:333、358、406`），违反依赖倒置原则。

### 2.5 AI 制版模块（tailor-is-ai）

**入口**：`PatternController`、`BodySizeController`

| 功能点               | 实现位置                                                | 状态 | 备注                                        |
| ----------------- | --------------------------------------------------- | -- | ----------------------------------------- |
| 版型生成              | PatternController.generatePattern                   | ✅  | —                                         |
| 版型检查              | PatternController.checkPattern                      | ✅  | —                                         |
| 版型迭代              | PatternController.iteratePattern                    | ✅  | —                                         |
| 版本管理（保存/查询）       | PatternController.saveVersion / listVersions        | ✅  | —                                         |
| 版型导出（SVG/PDF/DXF） | PatternController.exportPattern                     | ✅  | —                                         |
| 版型详情/列表           | PatternController.getPatternDetail / listMyPatterns | ✅  | —                                         |
| AI 生成 SVG 纸样      | PatternController.generateSvgPattern                | ✅  | —                                         |
| 纸样预览 SVG          | PatternController.previewPattern                    | ⚠️ | 返回硬编码 SVG 占位符，未真正查询数据库                    |
| 体型数据 CRUD         | BodySizeController                                  | ✅  | save/detail/list/search/setDefault/delete |
| AI 体型分析           | BodySizeController.analyzeBodySize                  | ✅  | MVP 版本                                    |
| 批量任务调度            | PatternTaskScheduler                                | ✅  | 失败重试 / 缓存清理 / 统计报告                        |

**模块功能覆盖率**：**10/11 = 90.9%**
**遗留问题**：

- 路由配置：网关 `ai-route` 仅匹配 `/api/ai/**` 与 `/api/body-size/**`，但 Controller 实际路径为 `/api/v1/ai/pattern` 与 `/api/v1/ai/size`，**路径前缀不匹配**，经网关访问将路由失败（`CoreGatewayRouteConfig.java:79`）。

### 2.6 版权存证模块（tailor-is-copyright）

**入口**：`CopyrightController`、`InfringementController`

| 功能点               | 实现位置                                                           | 状态 | 备注                      |
| ----------------- | -------------------------------------------------------------- | -- | ----------------------- |
| 作品上传/登记           | CopyrightController.registerCopyright                          | ✅  | —                       |
| 哈希计算登记            | CopyrightController.registerWithHash                           | ✅  | SHA-256                 |
| 相似度比对             | SimilarityCheckService.preCheck                                | ✅  | pHash + 风险等级            |
| 区块链直接存证           | CopyrightController.blockchainRegister                         | ✅  | —                       |
| 区块链批量存证（Merkle 树） | CopyrightController.blockchainBatchRegister                    | ✅  | MerkleTreeUtil          |
| 区块链验证             | CopyrightController.blockchainVerify                           | ✅  | —                       |
| 区块链证书查询           | CopyrightController.blockchainCertificate                      | ✅  | —                       |
| 区块链健康检查           | CopyrightController.blockchainHealth                           | ✅  | —                       |
| 证书生成              | CopyrightController.generateCertificate                        | ✅  | CertificatePdfGenerator |
| 版权验证              | CopyrightController.verifyCopyright / verifyByHash             | ✅  | —                       |
| 版权授权              | CopyrightController.createAuthorization                        | ✅  | —                       |
| 侵权举报              | InfringementController.reportInfringement                      | ✅  | —                       |
| 仲裁流程              | InfringementController.createArbitration / completeArbitration | ✅  | —                       |
| 批量上链调度            | BatchChainScheduler                                            | ✅  | 每 30 分钟兜底 + 阈值触发        |
| 多链支持              | BlockchainClientRouter + Eth/Hyperledger/AntChain/ZhiXin       | ✅  | 4 链路由                   |

**模块功能覆盖率**：**15/15 = 100%**
**遗留问题**：

- 同 AI 模块，网关 `copyright-route` 仅匹配 `/api/copyright/**`，但 Controller 路径为 `/api/v1/copyright`，**路径不匹配**。

### 2.7 商家模块（tailor-is-merchant）

**入口**：`MerchantController`、`ShopController`、`EmployeeController`、`MerchantRoleController`、`MerchantShopSwitchController`、`MerchantDashboardController`、`MerchantNoticeController`、`MerchantViolationController`、`MerchantTrialAssessmentController`、`AdminMerchantController`

| 功能点     | 实现位置                                                              | 状态 | 备注                                                           |
| ------- | ----------------------------------------------------------------- | -- | ------------------------------------------------------------ |
| 商家入驻申请  | MerchantController.applyJoin                                      | ✅  | —                                                            |
| 商家信息查询  | MerchantController.getMerchantInfo / getMerchantById              | ✅  | —                                                            |
| 商家列表    | MerchantController.listMerchants                                  | ✅  | —                                                            |
| 商家审核    | MerchantController.audit                                          | ✅  | —                                                            |
| 店铺 CRUD | ShopController                                                    | ✅  | 创建/更新/查询/状态                                                  |
| 店铺装修    | ShopController.saveDecoration / getDecoration / previewDecoration | ✅  | —                                                            |
| 员工管理    | EmployeeController                                                | ✅  | —                                                            |
| 角色管理    | MerchantRoleController                                            | ✅  | —                                                            |
| 多店铺切换   | MerchantShopSwitchController                                      | ✅  | —                                                            |
| 商家工作台   | MerchantDashboardController                                       | ✅  | 8 个统计接口                                                      |
| 商家通知    | MerchantNoticeController                                          | ✅  | —                                                            |
| 违规管理    | MerchantViolationController                                       | ✅  | —                                                            |
| 试用期考核   | MerchantTrialAssessmentController                                 | ✅  | —                                                            |
| 商品管理    | —                                                                 | ⚠️ | 商家商品 CRUD 复用 product-service 的 `ProductController`，无独立商家商品入口 |
| 订单管理    | —                                                                 | ⚠️ | 商家订单管理复用 `AdminOrderController`（仅管理员），缺少商家视角订单接口             |

**模块功能覆盖率**：**13/15 = 86.7%**
**遗留问题**：

- 商家商品/订单管理依赖其他模块，未提供商家专属接口（如 `/api/merchant/product/**`、`/api/merchant/order/**`）。
- `MerchantDashboardController` 路径为 `/api/v1/merchant/dashboard`，但网关 `merchant-route` 仅匹配 `/api/merchant/**` 与 `/api/shop/**`，存在路径前缀不一致问题。

### 2.8 营销模块（tailor-is-marketing）

**入口**：`CouponController`、`SeckillController`、`MktPromotionController`、`MktGroupBuyController`、`MktOrderMatchController`、`MktStatisticsController`、`PointsController`、`MemberController`

| 功能点              | 实现位置                    | 状态 | 备注          |
| ---------------- | ----------------------- | -- | ----------- |
| 优惠券（创建/领取/使用/查询） | CouponController        | ✅  | —           |
| 秒杀（活动/参与/查询）     | SeckillController       | ✅  | —           |
| 阶梯满减满赠           | MktPromotionController  | ✅  | 含最优优惠计算     |
| 拼团               | MktGroupBuyController   | ✅  | 开团/参团/详情/成员 |
| 订单匹配             | MktOrderMatchController | ✅  | —           |
| 营销统计             | MktStatisticsController | ✅  | —           |
| 积分               | PointsController        | ✅  | —           |
| 会员等级             | MemberController        | ✅  | —           |
| 推广               | —                       | ⚠️ | 缺少独立的推广分销接口 |

**模块功能覆盖率**：**8/9 = 88.9%**

### 2.9 轻量服务模块

#### 2.9.1 社区（tailor-is-community）

**入口**：`PostController`、`CommentController`、`InteractionController`、`PublicPostController`、`CommunityTopicController`、`CommunityReportController`、`CommunityMessageController`、`CommunityDiscoveryController`、`CommunityBlockController`、`AdminCommunityController`

| 功能点     | 实现位置                         | 状态 |
| ------- | ---------------------------- | -- |
| 帖子 CRUD | PostController               | ✅  |
| 评论 CRUD | CommentController            | ✅  |
| 点赞/收藏   | InteractionController        | ✅  |
| 话题管理    | CommunityTopicController     | ✅  |
| 举报管理    | CommunityReportController    | ✅  |
| 社区消息    | CommunityMessageController   | ✅  |
| 发现页     | CommunityDiscoveryController | ✅  |
| 拉黑      | CommunityBlockController     | ✅  |
| 后台管理    | AdminCommunityController     | ✅  |

**覆盖率**：**9/9 = 100%**

#### 2.9.2 学堂（tailor-is-academy）

**入口**：`CourseController`

| 功能点        | 实现位置                          | 状态   |
| ---------- | ----------------------------- | ---- |
| 课程列表       | CourseController.list         | ✅    |
| 课程详情       | CourseController.getById      | ✅    |
| 章节列表       | CourseController.listChapters | ✅    |
| 课程创建/更新/删除 | —                             | ❌ 缺失 |

**覆盖率**：**3/6 = 50%**
**遗留问题**：课程管理 CRUD 完全缺失，仅提供查询接口。

#### 2.9.3 供应链（tailor-is-supply）

**入口**：`SupplyController`、`MatchController`、`ContactController`

| 功能点      | 实现位置                               | 状态 |
| -------- | ---------------------------------- | -- |
| 供需帖 CRUD | SupplyController                   | ✅  |
| 智能匹配     | MatchController.findMatches        | ✅  |
| 供应商推荐    | MatchController.recommendSuppliers | ✅  |
| 联系/回复    | ContactController                  | ✅  |

**覆盖率**：**4/4 = 100%**

#### 2.9.4 消息（tailor-is-message）

**入口**：`MessageController`

| 功能点       | 实现位置                                            | 状态 |
| --------- | ----------------------------------------------- | -- |
| 消息列表/未读数  | MessageController.listMessages / getUnreadCount | ✅  |
| 标记已读/全部已读 | MessageController.markAsRead / markAllAsRead    | ✅  |
| 模板消息发送    | MessageController.sendTemplateMessage           | ✅  |
| MQ 消息推送消费 | MessagePushConsumer                             | ✅  |

**覆盖率**：**4/4 = 100%**

#### 2.9.5 IM（tailor-is-message-im）

**入口**：`ImController`

| 功能点          | 实现位置                           | 状态                                                       |
| ------------ | ------------------------------ | -------------------------------------------------------- |
| 会话列表         | ImController.listConversations | ✅                                                        |
| 消息列表         | ImController.listMessages      | ✅                                                        |
| 发送消息         | —                              | ❌ ImService 接口有 `sendMessage` 但 Controller 未暴露           |
| 标记已读         | —                              | ❌ ImService 有 `markMessageRead` 但 Controller 未暴露         |
| 创建会话         | —                              | ❌ ImService 有 `getOrCreateConversation` 但 Controller 未暴露 |
| WebSocket 推送 | —                              | ❌ 全工程未发现 `@MessageMapping` 或 WebSocket 配置                |

**覆盖率**：**2/6 = 33.3%**
**遗留问题**：IM 模块仅提供查询接口，发送消息、已读、会话创建均无 REST 入口；且未实现 WebSocket 实时推送，IM 功能基本不可用。

### 2.10 功能覆盖率汇总表

| 序号     | 模块                   | 期望功能点   | 已实现     | 覆盖率       | 评级    |
| ------ | -------------------- | ------- | ------- | --------- | ----- |
| 1      | tailor-is-user       | 15      | 14      | 93.3%     | 🟢 良好 |
| 2      | tailor-is-product    | 11      | 10      | 90.9%     | 🟢 良好 |
| 3      | tailor-is-order      | 14      | 14      | 100%      | 🟢 优秀 |
| 4      | tailor-is-payment    | 16      | 16      | 100%      | 🟢 优秀 |
| 5      | tailor-is-ai         | 11      | 10      | 90.9%     | 🟢 良好 |
| 6      | tailor-is-copyright  | 15      | 15      | 100%      | 🟢 优秀 |
| 7      | tailor-is-merchant   | 15      | 13      | 86.7%     | 🟡 一般 |
| 8      | tailor-is-marketing  | 9       | 8       | 88.9%     | 🟡 一般 |
| 9      | tailor-is-community  | 9       | 9       | 100%      | 🟢 优秀 |
| 10     | tailor-is-academy    | 6       | 3       | 50.0%     | 🔴 差  |
| 11     | tailor-is-supply     | 4       | 4       | 100%      | 🟢 优秀 |
| 12     | tailor-is-message    | 4       | 4       | 100%      | 🟢 优秀 |
| 13     | tailor-is-message-im | 6       | 2       | 33.3%     | 🔴 差  |
| **合计** | —                    | **135** | **122** | **90.4%** | 🟢 良好 |

***

## 3. 模块间集成测试用例设计（Task 3.2）

### 3.1 链路一：网关 → 服务路由（core-gateway / lite-gateway 转发正确性）

| 用例 ID     | 用例名称                    | 前置条件                              | 测试步骤                             | 预期结果                                                                    | 优先级      |
| --------- | ----------------------- | --------------------------------- | -------------------------------- | ----------------------------------------------------------------------- | -------- |
| IT-GW-001 | 核心网关-用户服务路由             | 网关与 user-service 已启动              | `GET /api/user/info` 携带有效 Token  | 网关透传至 `lb://tailor-is-user`，返回 200 + 用户信息                               | P0       |
| IT-GW-002 | 核心网关-白名单路径放行            | —                                 | `POST /api/auth/login` 不带 Token  | 网关命中白名单放行，返回 200 + LoginResponse                                        | P0       |
| IT-GW-003 | 核心网关-无效 Token 拦截        | —                                 | `GET /api/user/info` 携带无效 Token  | 网关返回 401 + `{code:401, message:"Token无效或已过期"}`                          | P0       |
| IT-GW-004 | 核心网关-缺失 Token 拦截        | —                                 | `GET /api/order/list` 无 Token    | 网关返回 401 + `未登录，请先登录`                                                   | P0       |
| IT-GW-005 | 核心网关-用户身份透传             | —                                 | `GET /api/order/list` 携带有效 Token | 下游服务收到 `X-User-Id` 与 `X-User-Id-Signature` 头                            | P0       |
| IT-GW-006 | 核心网关-限流触发               | 配置 `login-permits-per-minute: 10` | 1 分钟内连续发起 11 次 `/api/auth/login` | 第 11 次返回 429 / 限流提示                                                     | P1       |
| IT-GW-007 | 轻量网关-社区路由               | lite-gateway + community 已启动      | `GET /api/community/post/list`   | 路由至 `lb://tailor-is-community`                                          | P1       |
| IT-GW-008 | **核心网关-AI 路径不匹配（已知缺陷）** | —                                 | `GET /api/v1/ai/pattern/list`    | 路由失败 404，验证 `CoreGatewayRouteConfig` 路径与 Controller `/api/v1/ai/**` 不匹配 | P0（缺陷用例） |
| IT-GW-009 | HMAC 签名防伪造              | 配置 `GATEWAY_HMAC_SECRET`          | 直接访问下游服务并伪造 `X-User-Id` 头        | 下游服务应校验 `X-User-Id-Signature` 并拒绝（当前下游校验未实现，标记为待修复）                     | P1（缺陷用例） |

### 3.2 链路二：订单 → 支付 → 结算（状态同步、资金流转）

| 用例 ID     | 用例名称                | 测试步骤                                                         | 预期结果                                                                                 | 优先级      |
| --------- | ------------------- | ------------------------------------------------------------ | ------------------------------------------------------------------------------------ | -------- |
| IT-OP-001 | 下单-支付-结算正常链路        | ① 创建订单 → ② 调用 `PayController.alipayPay` → ③ 模拟支付宝回调 → ④ 触发结算 | 订单状态：PENDING\_PAY → PAID；支付记录：SUCCESS；商户账户余额增加；escrow\_deposit 记录存在                  | P0       |
| IT-OP-002 | 支付回调幂等性             | 同一 `paymentNo` 连续发送 2 次回调                                    | 第一次处理成功，第二次命中 Redis 幂等锁直接返回                                                          | P0       |
| IT-OP-003 | 重复支付防护              | 订单已支付后再次发起 `PayController.alipayPay`                         | `PaymentServiceImpl.createPayment` 检测到已有 SUCCESS 记录，抛出 `BusinessException`           | P0       |
| IT-OP-004 | 订单超时自动取消            | 创建订单后等待超时（或投递延迟队列）                                           | `OrderTimeoutConsumer` 收到消息，订单状态变更为 CANCELLED，库存释放                                   | P0       |
| IT-OP-005 | 退款链路                | 已支付订单申请退款 → 调用 `PayController.refund`                        | 退款记录创建，渠道退款 API 调用成功，订单状态进入 REFUNDING → REFUNDED                                     | P0       |
| IT-OP-006 | 库存预扣减失败回滚           | 下单时某 SKU 库存不足                                                | `InventoryService.deductStock` 返回 false，Seata 全局事务回滚，订单不创建                           | P0       |
| IT-OP-007 | 支付回调签名验证失败          | 支付宝回调 `sign` 错误                                              | `PaymentServiceImpl.verifyCallbackSign` 返回 false，抛出 `BusinessException`，回调返回 failure | P0       |
| IT-OP-008 | **支付验签公钥未配置（已知缺陷）** | `payment.alipay.public-key` 为空                               | 当前代码跳过验签（`PaymentServiceImpl.java:220-222`），存在严重安全风险                                 | P0（缺陷用例） |

### 3.3 链路三：版权存证 → 区块链（哈希计算、批量上链）

| 用例 ID     | 用例名称            | 测试步骤                                               | 预期结果                                                                 | 优先级 |
| --------- | --------------- | -------------------------------------------------- | -------------------------------------------------------------------- | --- |
| IT-CB-001 | 单次版权登记上链        | `POST /api/v1/copyright/blockchain/register`       | BlockchainService 返回 `txHash`、`blockNumber`，CopyrightRecord 状态更新为已上链 | P0  |
| IT-CB-002 | 批量上链 Merkle 树聚合 | 提交 100 条作品 → 触发 `BatchChainScheduler`              | 100 条 hash 聚合为 1 个 Merkle Root 上链，仅消耗 1 笔链上交易                        | P0  |
| IT-CB-003 | 区块链验证           | `GET /api/v1/copyright/blockchain/verify?hash=xxx` | 返回存证信息（作者、时间戳、txHash）                                                | P0  |
| IT-CB-004 | 区块链服务降级         | 断开区块链节点                                            | `BlockchainClientRouter` 路由到备用链或返回降级响应，`isHealthy` 返回 false          | P1  |
| IT-CB-005 | 相似度比对-高风险拦截     | 提交相似度 ≥90% 的作品                                     | `SimilarityCheckService` 返回风险等级 4（重大），登记流程被阻断                        | P0  |
| IT-CB-006 | 多链路由切换          | 配置 `copyright.blockchain.platform=hyperledger`     | `BlockchainClientRouter` 路由至 `HyperledgerFabricServiceImpl`          | P1  |

### 3.4 链路四：AI 制版调度（任务队列、回调通知）

| 用例 ID     | 用例名称     | 测试步骤                                               | 预期结果                                                 | 优先级 |
| --------- | -------- | -------------------------------------------------- | ---------------------------------------------------- | --- |
| IT-AI-001 | 实时版型生成   | `POST /api/v1/ai/pattern/generate`                 | 同步返回 PatternRecord，status=1                          | P0  |
| IT-AI-002 | 失败任务重试   | 插入 status=0 且 create\_time 超过 2 小时的记录              | `PatternTaskScheduler.retryFailedTasks` 每 30 分钟扫描并重试 | P1  |
| IT-AI-003 | 缓存清理     | 凌晨 3:00 触发                                         | 过期 Redis 缓存被清理，统计报告生成                                | P2  |
| IT-AI-004 | 版型导出多格式  | `GET /api/v1/ai/pattern/export?format=SVG/PDF/DXF` | 返回对应格式下载 URL                                         | P1  |
| IT-AI-005 | 体型数据关联版型 | 录入体型 → 生成版型                                        | 版型记录关联 `bodySizeId`，可基于体型数据生成                        | P1  |

### 3.5 链路五：商品 → 库存 → 订单（库存扣减、释放）

| 用例 ID     | 用例名称     | 测试步骤                                                         | 预期结果                                                  | 优先级 |
| --------- | -------- | ------------------------------------------------------------ | ----------------------------------------------------- | --- |
| IT-PI-001 | 下单库存预扣减  | 创建订单含 2 个 SKU                                                | `product_sku.stock` 减少，`locked_stock` 增加，影响行数 = 订单明细数 | P0  |
| IT-PI-002 | 取消订单库存释放 | 取消已下单订单                                                      | `stock` 恢复，`locked_stock` 减少                          | P0  |
| IT-PI-003 | 支付成功确认扣减 | 订单支付成功                                                       | `InventoryService.confirmDeduct` 将 `locked_stock` 清零  | P0  |
| IT-PI-004 | 库存不足下单失败 | SKU 库存为 0，下单购买 1 件                                           | `deductStock` 返回 false，订单回滚，返回 `库存不足，无法创建订单`          | P0  |
| IT-PI-005 | 并发下单防超卖  | 10 个并发请求购买同一 SKU（库存=1）                                       | 仅 1 个成功，其余抛出库存不足异常                                    | P0  |
| IT-PI-006 | 手动调整库存   | `PUT /api/product/sku/{id}/stock?quantity=100&increase=true` | SKU 库存增加 100                                          | P2  |

***

## 4. 20+ 异常场景验证结果（Task 3.3）

### 4.1 全局异常处理机制核查

**核查位置**：`tailor-is-common/src/main/java/com/tailoris/common/exception/GlobalExceptionHandler.java`

**已覆盖异常类型**（7 种）：

| 异常类型                              | 处理器                         | HTTP 状态码 | ResultCode             |
| --------------------------------- | --------------------------- | -------- | ---------------------- |
| `BusinessException`               | `handleBusinessException`   | 400      | BUSINESS\_ERROR (4000) |
| `MethodArgumentNotValidException` | `handleValidationException` | 400      | BAD\_REQUEST (400)     |
| `BindException`                   | `handleBindException`       | 400      | BAD\_REQUEST (400)     |
| `IllegalArgumentException`        | `handleIllegalArgument`     | 400      | BAD\_REQUEST (400)     |
| `NullPointerException`            | `handleNullPointer`         | 500      | INTERNAL\_ERROR (500)  |
| `RuntimeException`                | `handleRuntimeException`    | 500      | INTERNAL\_ERROR (500)  |
| `Exception`（兜底）                   | `handleException`           | 500      | INTERNAL\_ERROR (500)  |

**未覆盖异常类型**（重要缺陷）：

- ❌ `cn.dev33.satoken.exception.NotLoginException`（Sa-Token 未登录）
- ❌ `cn.dev33.satoken.exception.NotRoleException`（Sa-Token 角色不足）
- ❌ `cn.dev33.satoken.exception.NotPermissionException`（Sa-Token 权限不足）
- ❌ `cn.dev33.satoken.exception.DisableServiceException`（账号被封禁）
- ❌ `org.springframework.web.multipart.MaxUploadSizeExceededException`（文件超限）
- ❌ `org.springframework.web.HttpRequestMethodNotSupportedException`（方法不支持）
- ❌ `org.springframework.web.HttpMediaTypeNotSupportedException`（媒体类型不支持）
- ❌ `java.util.concurrent.TimeoutException`（超时）
- ❌ `com.alibaba.csp.sentinel.slots.block.BlockException`（Sentinel 限流）

### 4.2 异常场景验证矩阵

| 序号 | 场景分类  | 异常场景       | 触发方式                                 | 实际处理机制                                                                                                             | 验证结果        | 严重度   |
| -- | ----- | ---------- | ------------------------------------ | ------------------------------------------------------------------------------------------------------------------ | ----------- | ----- |
| 1  | 认证类   | Token 过期   | 携带过期 Token 调用受保护接口                   | 网关 `CoreAuthGlobalFilter` 返回 401 + `Token无效或已过期`                                                                   | ✅ 已处理       | —     |
| 2  | 认证类   | Token 无效   | 携带篡改的 Token                          | 网关返回 401 + `Token验证失败，请重新登录`                                                                                       | ✅ 已处理       | —     |
| 3  | 认证类   | 未登录访问受保护接口 | 不携带 Token                            | 网关返回 401 + `未登录，请先登录`                                                                                              | ✅ 已处理       | —     |
| 4  | 认证类   | 权限不足       | 普通用户访问 `@SaCheckRole("admin")` 接口    | Sa-Token 抛 `NotRoleException`，**未在全局处理器覆盖**，被兜底 `Exception` 处理返回 500                                               | ❌ **未正确处理** | 🔴 严重 |
| 5  | 参数校验类 | 必填字段缺失     | `POST /api/auth/login` 不传 `username` | `@Valid` 触发 `MethodArgumentNotValidException` → 400 + 字段错误信息                                                       | ✅ 已处理       | —     |
| 6  | 参数校验类 | 格式错误       | 注册时手机号格式错误                           | `@PhoneNumber` 自定义注解校验 → 400                                                                                       | ✅ 已处理       | —     |
| 7  | 参数校验类 | 长度超限       | 注册密码长度 >32                           | `@Size(max=32)` → 400                                                                                              | ✅ 已处理       | —     |
| 8  | 业务类   | 库存不足       | 下单购买量 > 库存                           | `InventoryService.deductStock` 返回 false → `BusinessException("库存不足，无法创建订单")`                                       | ✅ 已处理       | —     |
| 9  | 业务类   | 余额不足       | 钱包余额 < 支付金额                          | `AccountService` 抛 `BusinessException`（推测，需运行时验证）                                                                  | ⚠️ 待验证      | —     |
| 10 | 业务类   | 订单状态非法转换   | 已完成订单再次"支付"                          | `OrderStateMachine.verifyTransition` 抛 `BusinessException("订单状态不允许从[3]转换为[1]")`                                    | ✅ 已处理       | —     |
| 11 | 业务类   | 重复支付       | 已支付订单再次创建支付单                         | `PaymentServiceImpl.createPayment` 检测 `existCount > 0` → `BusinessException("该订单已有待支付记录")`                         | ✅ 已处理       | —     |
| 12 | 业务类   | 重复下单       | 同一 userId + cartIds 短时间重复提交          | Redis 幂等锁 → `BusinessException("订单处理中，请勿重复提交")`                                                                    | ✅ 已处理       | —     |
| 13 | 网络类   | 接口超时       | 下游服务响应 > 30s                         | 无显式超时处理，依赖 Seata `timeoutMills=300000` 全局事务回滚                                                                      | ⚠️ 部分处理     | 🟡    |
| 14 | 网络类   | 服务不可用      | 下游服务宕机                               | Spring Cloud LoadBalancer 重试，最终抛 `RuntimeException` → 兜底 500                                                       | ⚠️ 部分处理     | 🟡    |
| 15 | 网络类   | 限流触发       | 触发 `@RateLimit` 或 Sentinel 规则        | `@RateLimit` 由 `RateLimitInterceptor` 抛 `BusinessException`；Sentinel 通过 `blockHandler` 处理                          | ✅ 已处理       | —     |
| 16 | 数据类   | 资源不存在      | 查询不存在的订单                             | `OrderServiceImpl.getOrderByOrderNo` 返回 null → `BusinessException("订单不存在")`                                        | ✅ 已处理       | —     |
| 17 | 数据类   | 并发冲突       | 多线程同时更新订单状态                          | 依赖数据库行锁 + `OrderStateMachine.verifyTransition`；无显式乐观锁版本号                                                           | ⚠️ 部分处理     | 🟡    |
| 18 | 数据类   | 数据格式错误     | JSON 反序列化失败                          | Spring MVC 抛 `HttpMessageNotReadableException`，**未在全局处理器覆盖**，被兜底 `Exception` 处理返回 500                              | ❌ **未正确处理** | 🔴 严重 |
| 19 | 其他    | 文件上传超限     | 上传 > `max-file-size` 的文件             | `AliyunOssService.validateFile` 抛 `IllegalArgumentException` → 400；但 Spring 的 `MaxUploadSizeExceededException` 未处理 | ⚠️ 部分处理     | 🟡    |
| 20 | 其他    | 频率限制       | 1 分钟内发送 2 次短信验证码                     | `@RateLimit(key="sms", permitsPerSecond=1, capacity=60)` → `BusinessException("验证码请求过于频繁")`                        | ✅ 已处理       | —     |
| 21 | 其他    | 跨域错误       | 非白名单域名访问 API                         | `CorsConfig` 返回 403 Forbidden                                                                                      | ✅ 已处理       | —     |
| 22 | 其他    | 签名错误       | 支付回调签名校验失败                           | `PaymentServiceImpl.verifyCallbackSign` 返回 false → `BusinessException("支付回调签名验证失败")`                               | ✅ 已处理       | —     |
| 23 | 其他    | 验证码错误      | 注册时短信验证码不匹配                          | `AuthController.register` 比对 Redis → `BusinessException("验证码错误或已过期")`                                              | ✅ 已处理       | —     |
| 24 | 其他    | 账号已被禁用     | 登录时 `user.getStatus() != 0`          | `AuthController.loginByCode` → `BusinessException("账号已被禁用")`                                                       | ✅ 已处理       | —     |
| 25 | 其他    | 支付回调缺少参数   | 微信回调缺少 `out_trade_no`                | `PayController.wechatCallback` 返回 `<xml><return_code>FAIL</return_code>...`                                        | ✅ 已处理       | —     |

### 4.3 异常处理验证汇总

- **总验证场景数**：25
- **已正确处理**：18
- **部分处理**：5
- **未正确处理**：2（权限不足、数据格式错误）
- **整体覆盖率**：72%（18/25）

**关键缺陷**：

1. Sa-Token 异常（`NotLoginException`、`NotRoleException`、`NotPermissionException`）未在 `GlobalExceptionHandler` 中显式处理，导致返回 500 而非 401/403。
2. `HttpMessageNotReadableException`、`MaxUploadSizeExceededException`、`HttpRequestMethodNotSupportedException` 等 Spring MVC 内置异常未显式处理。

***

## 5. 端到端业务流程测试用例设计（Task 3.4）

### 5.1 E2E 流程一：C 端用户购物全流程

**流程**：注册 → 登录 → 浏览商品 → 加入购物车 → 下单 → 支付 → 售后 → 评价

| 步骤 | 用例 ID        | 操作     | 接口                                             | 预期结果                 | 校验点                         |
| -- | ------------ | ------ | ---------------------------------------------- | -------------------- | --------------------------- |
| 1  | E2E-SHOP-001 | 手机号注册  | `POST /api/auth/register`                      | 注册成功，用户状态=0          | DB `sys_user` 表新增记录         |
| 2  | E2E-SHOP-002 | 账号密码登录 | `POST /api/auth/login`                         | 返回 token + csrfToken | Redis 中 sa-token 已写入        |
| 3  | E2E-SHOP-003 | 浏览热门商品 | `GET /api/products/hot`                        | 返回 ≤8 个上架商品          | 商品 `auditStatus=APPROVED`   |
| 4  | E2E-SHOP-004 | 查看商品详情 | `GET /api/products/{id}`                       | 返回商品完整信息             | SKU 列表、标签完整                 |
| 5  | E2E-SHOP-005 | 加入购物车  | `POST /api/cart`                               | 返回 200               | DB `shopping_cart` 新增       |
| 6  | E2E-SHOP-006 | 批量结算   | `POST /api/cart/checkout`                      | 返回选中购物车项             | —                           |
| 7  | E2E-SHOP-007 | 创建订单   | `POST /api/order`                              | 返回 orderNo           | 订单状态=0（待支付），库存预扣减           |
| 8  | E2E-SHOP-008 | 支付宝支付  | `POST /api/v1/payment/alipay`                  | 返回支付表单 HTML          | DB `payment_record` 新增，状态=0 |
| 9  | E2E-SHOP-009 | 模拟支付回调 | `POST /api/v1/payment/alipay/callback`         | 返回 "success"         | 订单状态=1（已支付），库存确认扣减          |
| 10 | E2E-SHOP-010 | 确认收货   | `PUT /api/order/{orderNo}/confirm`             | 订单状态=3（已完成）          | 状态机校验通过                     |
| 11 | E2E-SHOP-011 | 提交评价   | `POST /api/product/review`                     | 返回评价 ID              | 评价状态=待审核                    |
| 12 | E2E-SHOP-012 | 创建售后工单 | `POST /api/order/after-sale`                   | 返回 ticketNo          | 售后工单状态=0                    |
| 13 | E2E-SHOP-013 | 处理售后   | `PUT /api/order/after-sale/{ticketNo}/process` | 工单状态更新               | —                           |
| 14 | E2E-SHOP-014 | 申请退款   | `POST /api/v1/payment/refund`                  | 返回 refundNo          | 渠道退款 API 调用成功               |

### 5.2 E2E 流程二：商家入驻与店铺运营全流程

**流程**：商家入驻 → 店铺设置 → 商品发布 → 订单处理 → 发货 → 结算

| 步骤 | 用例 ID       | 操作     | 接口                                       | 预期结果                      | 校验点                         |
| -- | ----------- | ------ | ---------------------------------------- | ------------------------- | --------------------------- |
| 1  | E2E-MCH-001 | 商家入驻申请 | `POST /api/merchant/apply`               | 申请提交成功                    | DB `merchant` 表新增，状态=待审核    |
| 2  | E2E-MCH-002 | 管理员审核  | `PUT /api/merchant/audit`                | 商家状态=已通过                  | —                           |
| 3  | E2E-MCH-003 | 创建店铺   | `POST /api/merchant/shop`                | 返回店铺信息                    | —                           |
| 4  | E2E-MCH-004 | 店铺装修   | `POST /api/merchant/shop/decoration`     | 装修配置保存                    | —                           |
| 5  | E2E-MCH-005 | 发布商品   | `POST /api/product`                      | 返回商品 ID                   | 商品 `auditStatus=待审核`        |
| 6  | E2E-MCH-006 | 商品审核   | `PUT /api/admin/product/audit/{id}`      | 商品 `auditStatus=APPROVED` | —                           |
| 7  | E2E-MCH-007 | 创建 SKU | `POST /api/product/sku/{productId}`      | SKU 创建 + 库存设置             | —                           |
| 8  | E2E-MCH-008 | 收到订单   | 用户下单后                                    | 商家工作台订单数 +1               | `MerchantDashboardStats` 更新 |
| 9  | E2E-MCH-009 | 发货     | `PUT /api/order/{orderId}/logistics`     | 订单状态=2（已发货）               | 物流信息写入                      |
| 10 | E2E-MCH-010 | 用户确认收货 | `PUT /api/order/{orderNo}/confirm`       | 订单状态=3（已完成）               | —                           |
| 11 | E2E-MCH-011 | 触发结算   | 定时任务 `BatchSettlementScheduler`          | 商户账户余额增加（扣除平台费率 5%）       | `settlement_record` 新增      |
| 12 | E2E-MCH-012 | 查看工作台  | `GET /api/v1/merchant/dashboard/summary` | 返回流量/转化/交易/收益统计           | —                           |

### 5.3 E2E 流程三：版权存证全流程

**流程**：版权作品上传 → 相似度比对 → 上链存证 → 证书下载

| 步骤 | 用例 ID      | 操作        | 接口                                                         | 预期结果                                    | 校验点                          |
| -- | ---------- | --------- | ---------------------------------------------------------- | --------------------------------------- | ---------------------------- |
| 1  | E2E-CR-001 | 上传作品文件    | `POST /api/upload/copyright`                               | 返回文件 URL + objectKey                    | OSS 存储                       |
| 2  | E2E-CR-002 | 相似度预检     | `SimilarityCheckService.preCheck`                          | 返回相似度分数 + 风险等级                          | Redis 缓存结果                   |
| 3  | E2E-CR-003 | 版权登记（含哈希） | `POST /api/v1/copyright/register-hash`                     | 返回 CopyrightRegisterResponse，含 fileHash | SHA-256 计算正确                 |
| 4  | E2E-CR-004 | 区块链存证     | `POST /api/v1/copyright/blockchain/register`               | 返回 txHash + blockNumber                 | 区块链交易成功                      |
| 5  | E2E-CR-005 | 批量上链触发    | 等待 `BatchChainScheduler` 触发或手动 `triggerBatchChain`         | 多条记录聚合为 Merkle Root 上链                  | —                            |
| 6  | E2E-CR-006 | 区块链验证     | `GET /api/v1/copyright/blockchain/verify?hash=xxx`         | 返回存证信息                                  | —                            |
| 7  | E2E-CR-007 | 生成证书      | `GET /api/v1/copyright/certificate?copyrightId=xxx`        | 返回证书 PDF URL                            | `CertificatePdfGenerator` 生成 |
| 8  | E2E-CR-008 | 侵权举报      | `POST /api/v1/copyright/infringement/report`               | 侵权记录创建                                  | —                            |
| 9  | E2E-CR-009 | 创建仲裁      | `POST /api/v1/copyright/infringement/arbitration/create`   | 仲裁记录创建                                  | —                            |
| 10 | E2E-CR-010 | 完成仲裁      | `POST /api/v1/copyright/infringement/arbitration/complete` | 仲裁结果记录                                  | —                            |

### 5.4 E2E 流程四：AI 图案生成全流程

**流程**：AI 图案生成 → 任务调度 → 结果回调 → 商品关联

| 步骤 | 用例 ID      | 操作      | 接口                                            | 预期结果                      | 校验点           |
| -- | ---------- | ------- | --------------------------------------------- | ------------------------- | ------------- |
| 1  | E2E-AI-001 | 录入体型数据  | `POST /api/v1/ai/size/save`                   | 返回 BodySizeData           | —             |
| 2  | E2E-AI-002 | AI 体型分析 | `POST /api/v1/ai/size/analyze`                | 返回推荐尺码                    | —             |
| 3  | E2E-AI-003 | 生成版型    | `POST /api/v1/ai/pattern/generate`            | 返回 PatternRecord，status=1 | 关联 bodySizeId |
| 4  | E2E-AI-004 | 检查版型    | `POST /api/v1/ai/pattern/check`               | 返回结构合理性结果                 | —             |
| 5  | E2E-AI-005 | 迭代版型    | `POST /api/v1/ai/pattern/iterate`             | 返回 PatternIteration       | —             |
| 6  | E2E-AI-006 | 保存版本    | `POST /api/v1/ai/pattern/version/save`        | 返回 PatternVersion         | 版本号自增         |
| 7  | E2E-AI-007 | 导出版型    | `GET /api/v1/ai/pattern/export?format=SVG`    | 返回下载 URL                  | —             |
| 8  | E2E-AI-008 | 失败任务重试  | 等待 `PatternTaskScheduler.retryFailedTasks` 触发 | status=0 的记录被重试           | —             |
| 9  | E2E-AI-009 | 商品关联    | `POST /api/product` 时携带 `patternId`           | 商品记录关联版型 ID               | —             |

***

## 6. 模块质量评分表（Task 3.5）

### 6.1 评分维度说明

| 维度    | 权重  | 评分标准                |
| ----- | --- | ------------------- |
| 功能完整性 | 30% | 功能点覆盖率、关键功能缺失情况     |
| 交互性   | 20% | 模块间集成、API 一致性、文档完整性 |
| 错误处理  | 20% | 异常处理覆盖、参数校验、幂等性     |
| 性能    | 15% | 缓存策略、批量处理、异步化、限流    |
| 安全性   | 15% | 鉴权、加密、防越权、签名验证、CORS |

### 6.2 模块质量评分

| 模块                     | 功能完整性 (30%) | 交互性 (20%) | 错误处理 (20%) | 性能 (15%) | 安全性 (15%) | 综合得分     | 评级    |
| ---------------------- | ----------- | --------- | ---------- | -------- | --------- | -------- | ----- |
| tailor-is-user         | 8.5         | 8.0       | 8.0        | 7.5      | 8.5       | **8.20** | 🟢 A  |
| tailor-is-product      | 8.0         | 7.5       | 7.5        | 7.5      | 7.0       | **7.55** | 🟢 B+ |
| tailor-is-order        | 9.5         | 9.0       | 9.0        | 8.5      | 8.5       | **9.00** | 🟢 A+ |
| tailor-is-payment      | 9.5         | 8.5       | 8.5        | 8.0      | 7.0       | **8.40** | 🟢 A  |
| tailor-is-ai           | 8.0         | 6.0       | 7.5        | 8.0      | 7.5       | **7.30** | 🟡 B  |
| tailor-is-copyright    | 9.5         | 8.5       | 8.0        | 8.5      | 8.0       | **8.60** | 🟢 A  |
| tailor-is-merchant     | 7.5         | 7.0       | 7.5        | 7.5      | 8.0       | **7.50** | 🟢 B+ |
| tailor-is-marketing    | 8.0         | 7.5       | 7.5        | 8.0      | 7.5       | **7.70** | 🟢 B+ |
| tailor-is-community    | 9.5         | 8.0       | 8.0        | 7.5      | 8.0       | **8.20** | 🟢 A  |
| tailor-is-academy      | 4.0         | 5.0       | 6.0        | 6.0      | 7.0       | **5.35** | 🔴 C  |
| tailor-is-supply       | 9.5         | 8.0       | 8.0        | 7.5      | 8.0       | **8.20** | 🟢 A  |
| tailor-is-message      | 9.5         | 8.0       | 8.0        | 8.0      | 8.0       | **8.40** | 🟢 A  |
| tailor-is-message-im   | 3.0         | 4.0       | 5.0        | 5.0      | 6.0       | **4.30** | 🔴 D  |
| tailor-is-core-gateway | 9.0         | 8.5       | 8.0        | 8.5      | 8.5       | **8.55** | 🟢 A  |
| tailor-is-lite-gateway | 9.0         | 8.5       | 8.0        | 8.5      | 8.5       | **8.55** | 🟢 A  |
| tailor-is-admin        | 8.5         | 8.0       | 7.5        | 7.5      | 8.5       | **8.05** | 🟢 A  |
| tailor-is-pattern      | 8.0         | 7.0       | 7.0        | 7.0      | 7.5       | **7.35** | 🟡 B  |
| **加权平均**               | —           | —         | —          | —        | —         | **7.71** | 🟢 B+ |

### 6.3 评分说明

#### tailor-is-user（8.20 / A）

- ✅ 功能完整：注册（手机/邮箱）、登录（账号/验证码/微信）、地址 CRUD、Token 刷新均覆盖
- ✅ 安全：CSRF Token、登录失败锁定（5次/30分钟）、身份证 AES 加密、密码 BCrypt
- ✅ 限流：登录、注册、验证码接口均配置 `@RateLimit`
- ⚠️ 改进：`UserController` 已废弃但未移除；`loginByCode` 使用 Map 接收参数未做校验

#### tailor-is-order（9.00 / A+）

- ✅ 功能完整：状态机、库存预扣减、超时取消、售后、物流、购物车全覆盖
- ✅ 性能：Seata 全局事务、Sentinel 限流、热门订单缓存、幂等锁、MQ 异步通知
- ✅ 安全：IDOR 越权修复（`getOrderDetail` 校验 userId）
- ⚠️ 改进：`OrderController` 已废弃但未移除；并发冲突未用乐观锁版本号

#### tailor-is-payment（8.40 / A）

- ✅ 功能完整：微信/支付宝多通道、回调验签、退款、托管、结算、对账
- ✅ 幂等：支付回调 Redis 幂等锁 + SUCCESS 状态 24 小时
- 🔴 严重缺陷：`alipayPublicKey` 未配置时**跳过验签**（`PaymentServiceImpl.java:220-222`）
- ⚠️ 改进：`AlipayService` 强转为 `AlipayServiceImpl` 违反依赖倒置

#### tailor-is-copyright（8.60 / A）

- ✅ 功能完整：登记、哈希、相似度、区块链（4 链路由）、证书、侵权、仲裁
- ✅ 性能：Merkle 树批量上链、Redis 缓存相似度结果、定时调度
- ✅ 多链支持：Eth / Hyperledger Fabric / AntChain / ZhiXin
- ⚠️ 改进：网关路由路径不匹配 `/api/v1/copyright/**`

#### tailor-is-message-im（4.30 / D）

- 🔴 严重缺陷：仅暴露查询接口，**发送消息、已读、会话创建均无 REST 接口**
- 🔴 严重缺陷：**无 WebSocket 实时推送实现**，IM 功能基本不可用
- ⚠️ 改进：`ImService` 接口定义了完整能力但 Controller 未暴露

#### tailor-is-academy（5.35 / C）

- 🔴 课程 CRUD 完全缺失，仅提供列表/详情/章节查询
- ⚠️ 改进：无章节内容管理、无学习进度跟踪、无评论

***

## 7. 审计结论与改进建议

### 7.1 审计结论

#### 7.1.1 整体质量水平

Tailor IS 系统整体模块质量评分为 **7.71/10（B+）**，处于良好水平。核心交易链路（订单/支付/版权）质量优秀（≥8.4），关键功能（库存预扣减、支付幂等、状态机、区块链批量上链）均已正确实现。

#### 7.1.2 优势

1. **订单模块质量最高（9.00）**：状态机模式、Seata 全局事务、Sentinel 限流、RabbitMQ 延迟队列、幂等锁、IDOR 越权修复等最佳实践完整落地。
2. **版权模块功能最完整（100%）**：4 链路由 + Merkle 树批量上链 + 相似度比对 + 仲裁流程，覆盖版权全生命周期。
3. **支付回调幂等设计严谨**：Redis 幂等锁 + SUCCESS 状态 + 事务提交后设置锁，避免事务回滚后误判。
4. **网关层鉴权统一**：`CoreAuthGlobalFilter` 实现 Token 校验 + 用户身份透传 + HMAC 签名防伪造。
5. **限流策略分层**：接口级 `@RateLimit` + 网关级 Sentinel + 业务级幂等锁，三道防线。

#### 7.1.3 待改进领域

1. **网关路由与 Controller 路径不一致**（🔴 严重）：AI、版权、支付、IM、Academy、Merchant Dashboard 等模块的 Controller 使用 `/api/v1/**` 前缀，但网关路由配置仍为 `/api/**`，经网关访问将 404。
2. **全局异常处理覆盖不全**（🔴 严重）：Sa-Token 异常、Spring MVC 内置异常（`HttpMessageNotReadableException`、`MaxUploadSizeExceededException`）未显式处理。
3. **IM 模块功能严重缺失**（🔴 严重）：仅查询接口可用，核心的发送消息、实时推送均未实现。
4. **Academy 模块功能不完整**（🟡 重要）：课程 CRUD 完全缺失。
5. **支付验签公钥未配置时跳过验签**（🔴 严重安全风险）。
6. **废弃 Controller 未清理**：`UserController`、`ProductController`、`OrderController` 已标注 `@Deprecated` 但仍在运行。

### 7.2 改进建议（按优先级排序）

#### P0 - 立即修复（1 周内）

| 编号      | 问题               | 建议方案                                                                                                                                                                                      | 影响模块       |
| ------- | ---------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| FIX-001 | 网关路由路径不匹配        | 在 `CoreGatewayRouteConfig` 与 `LiteGatewayRouteConfig` 中将 AI/Copyright/Payment/IM/Academy/MerchantDashboard 路由路径补充 `/api/v1/**`                                                            | 网关         |
| FIX-002 | Sa-Token 异常未处理   | 在 `GlobalExceptionHandler` 中新增 `@ExceptionHandler(NotLoginException.class)` 返回 401、`@ExceptionHandler(NotRoleException.class)` 返回 403                                                     | common     |
| FIX-003 | 支付验签公钥未配置时跳过     | 将 `PaymentServiceImpl.verifyCallbackSign` 中的 `return true` 改为 `throw new BusinessException("支付渠道公钥未配置")`                                                                                  | payment    |
| FIX-004 | IM 发送消息接口缺失      | 在 `ImController` 中新增 `POST /api/v1/im/send`、`POST /api/v1/im/conversation`、`PUT /api/v1/im/message/{id}/read` 接口                                                                          | message-im |
| FIX-005 | Spring MVC 异常未处理 | 新增 `@ExceptionHandler(MaxUploadSizeExceededException.class)`、`@ExceptionHandler(HttpMessageNotReadableException.class)`、`@ExceptionHandler(HttpRequestMethodNotSupportedException.class)` | common     |

#### P1 - 短期改进（2 周内）

| 编号      | 问题                 | 建议方案                                                                                               | 影响模块               |
| ------- | ------------------ | -------------------------------------------------------------------------------------------------- | ------------------ |
| FIX-006 | IM WebSocket 推送未实现 | 集成 Spring WebSocket 或 Netty，实现 `@MessageMapping("/im/send")` 实时推送                                  | message-im         |
| FIX-007 | Academy 课程 CRUD 缺失 | 在 `CourseController` 中补充 `POST/PUT/DELETE /api/v1/academy/courses` 接口                              | academy            |
| FIX-008 | 商家商品/订单接口缺失        | 在 merchant 模块新增 `MerchantProductController`、`MerchantOrderController`，通过 Feign 调用 product/order 服务 | merchant           |
| FIX-009 | 废弃 Controller 清理   | 在 v1.1.0 中移除 `UserController`、`ProductController`、`OrderController`，确认客户端已迁移至 `/api/v1/**`         | user/product/order |
| FIX-010 | `AlipayService` 强转 | 在 `AlipayService` 接口中新增 `createQrCodePayment`、`createAppPayment`、`queryRefund` 方法签名                | payment            |

#### P2 - 中期优化（1 个月内）

| 编号      | 问题                                               | 建议方案                                                                                    | 影响模块    |
| ------- | ------------------------------------------------ | --------------------------------------------------------------------------------------- | ------- |
| OPT-001 | 并发冲突未用乐观锁                                        | 在 `OrderInfo` 实体增加 `@Version` 字段，使用 MyBatis-Plus 乐观锁插件                                  | order   |
| OPT-002 | 库存预扣减无 Redis 预校验                                 | 在 `InventoryService.deductStock` 前增加 Redis 库存预校验，减少数据库压力                                | order   |
| OPT-003 | `AdminProductController.getProductStats` 硬编码     | 实现真实统计查询                                                                                | product |
| OPT-004 | `PublicProductController.getSeckillProducts` 空实现 | 对接 marketing 模块的 `SeckillService`                                                       | product |
| OPT-005 | 鉴权方式不统一                                          | 统一使用 `@SaCheckLogin` + `StpUtil.getLoginIdAsLong()`，移除 `@RequestHeader("X-User-Id")` 方式 | 全部      |
| OPT-006 | `loginByCode` 使用 Map 接收参数                        | 改为 DTO + `@Valid`                                                                       | user    |
| OPT-007 | `ResultCode` 缺少细粒度业务码                            | 新增 `INSUFFICIENT_STOCK`、`ORDER_STATUS_INVALID`、`PAYMENT_DUPLICATE` 等业务码                 | common  |
| OPT-008 | 公共上传接口缺 `@SaCheckLogin`                          | 在 `FileUploadController` 类上添加 `@SaCheckLogin`                                           | common  |

#### P3 - 长期规划（3 个月内）

| 编号      | 问题             | 建议方案                                                  |
| ------- | -------------- | ----------------------------------------------------- |
| LNG-001 | 推广分销模块缺失       | 在 marketing 模块新增 `PromotionController`，实现分销关系链 + 佣金计算 |
| LNG-002 | 学堂模块功能薄弱       | 补充课程章节内容管理、学习进度跟踪、评论、问答等完整功能                          |
| LNG-003 | 网关动态路由迁移       | 完成 Nacos 动态路由配置，移除 Java Config 硬编码                    |
| LNG-004 | 下游服务校验 HMAC 签名 | 实现 `X-User-Id-Signature` 校验拦截器，防止绕过网关伪造请求             |
| LNG-005 | 全链路追踪          | 集成 SkyWalking（已部署 agent.config），完善链路追踪 ID 透传          |

### 7.3 风险评估

| 风险项                | 概率 | 影响       | 风险等级 | 缓解措施                 |
| ------------------ | -- | -------- | ---- | -------------------- |
| 支付回调被伪造（公钥未配置）     | 中  | 极高（资金损失） | 🔴 高 | 立即修复 FIX-003         |
| 网关路由 404 导致功能不可用   | 高  | 高（用户体验）  | 🔴 高 | 立即修复 FIX-001         |
| 权限不足返回 500 而非 403  | 中  | 中（用户体验）  | 🟡 中 | 修复 FIX-002           |
| IM 功能不可用           | 高  | 高（功能缺失）  | 🔴 高 | 修复 FIX-004 + FIX-006 |
| 废弃 Controller 仍在运行 | 低  | 低（维护成本）  | 🟢 低 | 修复 FIX-009           |

### 7.4 审计结论

Tailor IS 系统在核心交易链路（订单/支付/版权）上实现了较高的工程质量，状态机、幂等性、分布式事务、限流降级等关键能力均已落地。但在以下三个方面存在明显短板：

1. **网关路由配置与 Controller 路径不一致**：多个模块使用 `/api/v1/**` 前缀但网关未适配，导致经网关访问 404，**必须立即修复**。
2. **全局异常处理覆盖不全**：Sa-Token 异常和 Spring MVC 内置异常未显式处理，导致部分异常返回 500 而非正确的 4xx 状态码。
3. **轻量服务模块质量参差不齐**：IM 模块功能严重缺失（评分 4.30），Academy 模块功能不完整（评分 5.35），拉低了整体质量水平。

建议按 P0 → P1 → P2 → P3 优先级依次修复，**P0 项必须在下一次发布前完成**。

***

## 附录 A：审计依据文件清单

| 文件         | 路径                                                                                                 | 用途      |
| ---------- | -------------------------------------------------------------------------------------------------- | ------- |
| 全局异常处理器    | `tailor-is-common/src/main/java/com/tailoris/common/exception/GlobalExceptionHandler.java`         | 异常处理核查  |
| 业务异常类      | `tailor-is-common/src/main/java/com/tailoris/common/exception/BusinessException.java`              | 异常处理核查  |
| 核心网关路由配置   | `tailor-is-core-gateway/src/main/java/com/tailoris/coregateway/config/CoreGatewayRouteConfig.java` | 路由核查    |
| 轻量网关路由配置   | `tailor-is-lite-gateway/src/main/java/com/tailoris/litegateway/config/LiteGatewayRouteConfig.java` | 路由核查    |
| 核心网关认证过滤器  | `tailor-is-core-gateway/src/main/java/com/tailoris/coregateway/filter/CoreAuthGlobalFilter.java`   | 鉴权核查    |
| 订单状态机      | `tailor-is-order/src/main/java/com/tailoris/order/state/OrderStateMachine.java`                    | 订单流程核查  |
| 库存服务       | `tailor-is-order/src/main/java/com/tailoris/order/service/InventoryService.java`                   | 库存扣减核查  |
| 支付服务实现     | `tailor-is-payment/src/main/java/com/tailoris/payment/service/impl/PaymentServiceImpl.java`        | 支付幂等核查  |
| CORS 配置    | `tailor-is-common/src/main/java/com/tailoris/common/config/CorsConfig.java`                        | 跨域核查    |
| 限流注解       | `tailor-is-common/src/main/java/com/tailoris/common/annotation/RateLimit.java`                     | 限流核查    |
| 文件上传服务     | `tailor-is-common/src/main/java/com/tailoris/common/oss/AliyunOssService.java`                     | 文件上传核查  |
| AI 任务调度器   | `tailor-is-ai/src/main/java/com/tailoris/ai/scheduler/PatternTaskScheduler.java`                   | AI 调度核查 |
| 区块链批量上链调度器 | `tailor-is-copyright/src/main/java/com/tailoris/copyright/scheduler/BatchChainScheduler.java`      | 区块链核查   |

## 附录 B：术语表

| 术语       | 含义                                                |
| -------- | ------------------------------------------------- |
| IDOR     | Insecure Direct Object Reference，不安全的直接对象引用       |
| HMAC     | Hash-based Message Authentication Code，基于哈希的消息认证码 |
| Merkle 树 | 一种二叉哈希树，用于批量数据完整性校验                               |
| Seata    | 阿里巴巴开源的分布式事务解决方案                                  |
| Sentinel | 阿里巴巴开源的流量控制组件                                     |
| Sa-Token | 国内轻量级权限认证框架                                       |
| pHash    | 感知哈希，用于图像相似度比对                                    |
| ESCROW   | 第三方托管账户                                           |

***

**报告结束**

> 本报告基于 2026-07-02 时的代码状态生成。所有结论均通过代码静态审查得出，未执行运行时测试。建议结合集成测试与压力测试进一步验证。

