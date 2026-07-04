# Tailor IS 部署后问题清单

> 生成时间：2026-07-02  
> 排查范围：Tailor IS 全量代码库（后端微服务 + 前端多端 + 部署配置 + 文档）  
> 排查方法：基于实际代码静态审查（Grep/Read），结合阶段零已知问题清单

## 概览

- 总问题数：**41** 项
- 阻断（P0）：**4** 项
- 严重（P1）：**18** 项
- 一般（P2）：**14** 项
- 轻微（P3）：**5** 项

> 注：PD-FE-001 与 PD-LD-002 为同一微信登录状态缺陷的功能与逻辑两个视角，去重后独立问题为 40 项。

### 问题分布

| 类别 | 编号前缀 | 数量 |
|------|----------|------|
| 一、功能错误 | PD-FE | 11 |
| 二、类型定义错误 | PD-TD | 3 |
| 三、规则违规项 | PD-RV | 4 |
| 四、性能瓶颈 | PD-PB | 3 |
| 五、安全漏洞 | PD-SV | 11 |
| 六、逻辑缺陷 | PD-LD | 4 |
| 七、文档缺失 | PD-DM | 5 |

### 严重级别说明

- **阻断（P0）**：核心功能完全不可用，阻断主业务流程
- **严重（P1）**：重要功能受损或存在高危安全风险
- **一般（P2）**：次要功能异常或违反规范，需修复但不阻断
- **轻微（P3）**：代码整洁度、文档完善度等优化项

---

## 一、功能错误

### PD-FE-001: 微信登录用户状态校验逻辑反转
- 严重级别：阻断（P0）
- 影响范围：核心功能（微信登录）
- 复现步骤：微信授权登录 → 后端校验 `user.getStatus() == 0` → 抛出"账号已被禁用"异常
- 预期结果：正常用户（status=0）放行，禁用用户（status≠0）拦截
- 实际结果：正常用户被拦截，禁用用户被放行
- 文件位置：`tailor-is/tailor-is-user/src/main/java/com/tailoris/user/service/impl/WechatLoginServiceImpl.java#L106`
- 证据对照：同项目 `AuthController.java#L94` 使用 `user.getStatus() != 0`（正确），且 `WechatLoginServiceImpl.java#L188` 新建微信用户时 `user.setStatus(0)` 证明 0=正常。两处状态判断逻辑不一致，微信登录分支为反转错误。
- 修复建议：将 `== 0` 改为 `!= 0`，并提取状态常量（见 PD-RV-001）。

### PD-FE-002: 订单 API 前后端路径/方法/参数全面不匹配
- 严重级别：阻断（P0）
- 影响范围：核心功能（下单、支付、取消、确认收货、订单查询）
- 复现步骤：前端 `createOrder` 调用 `POST /orders` → 网关无 `/api/orders/**` 路由 → 404
- 预期结果：前端订单接口与后端 `OrderController` 路由一致
- 实际结果：存在五重不匹配：
  1. 路径：前端 `/orders`（复数）vs 后端 `/api/order`（单数）
  2. 列表：前端 `GET /orders` vs 后端 `GET /api/order/list`
  3. 方法：前端 `POST /orders/{orderNo}/pay|confirm|cancel` vs 后端 `PUT`
  4. 参数：后端 `payOrder` 要求 `@RequestParam Integer payType`、`cancelOrder` 要求 `@RequestParam String reason`，前端均未传
  5. 后端 `OrderController` 已标注 `@Deprecated`
- 文件位置：
  - 前端：`tailor-is-frontend/pc-mall/src/api/order.ts#L17-L58`
  - 后端：`tailor-is/tailor-is-order/src/main/java/com/tailoris/order/controller/OrderController.java#L23-L83`
- 修复建议：统一前后端订单接口契约（路径、方法、参数），移除 `@Deprecated` 或迁移前端至 v1 接口。

### PD-FE-003: 购物车结算请求体不匹配且缺少 clearCart 端点
- 严重级别：严重（P1）
- 影响范围：重要功能（购物车结算、清空）
- 复现步骤：`CheckoutView.vue` 调用 `checkoutCart(ids)` 发送 `{ ids: [...] }` → 后端 `batchCheckout` 期望 `List<Long>`（裸数组）→ 反序列化失败
- 预期结果：结算请求体与后端一致
- 实际结果：请求体结构不匹配；且 `clearCart()` 调用 `DELETE /cart/clear` 但后端 `CartController` 无此端点
- 文件位置：
  - 前端：`tailor-is-frontend/pc-mall/src/api/cart.ts#L45-L57`
  - 后端：`tailor-is/tailor-is-order/src/main/java/com/tailoris/order/controller/CartController.java#L70-L76`
- 修复建议：前端 `checkoutCart` 改为发送裸数组；后端补 `clearCart` 端点或前端移除该调用。

### PD-FE-004: 收货地址 API 路径完全不匹配且网关无路由
- 严重级别：阻断（P0）
- 影响范围：核心功能（结算页地址加载/新增/默认设置）
- 复现步骤：`CheckoutView.vue` 调用 `getAddresses()` → 请求 `/api/addresses` → 网关路由表仅含 `/api/user/**`、`/api/auth/**` → 404
- 预期结果：前端地址接口路由至 user-service
- 实际结果：前端用 `/addresses`（无 `/user` 前缀、复数），后端为 `/api/user/address`（单数、含 `/user`），网关无 `/api/addresses/**` 路由
- 文件位置：
  - 前端：`tailor-is-frontend/pc-mall/src/api/address.ts#L24-L58`
  - 后端：`tailor-is/tailor-is-user/src/main/java/com/tailoris/user/controller/AddressController.java#L41-L43`
  - 网关：`tailor-is/tailor-is-core-gateway/src/main/java/com/tailoris/coregateway/config/CoreGatewayRouteConfig.java#L52`
- 修复建议：前端改为 `/user/address` 路径，与后端及 `api-overview.md` 文档一致。

### PD-FE-005: 用户资料/密码/收藏/积分接口缺失或路径不匹配
- 严重级别：严重（P1）
- 影响范围：重要功能（个人中心）
- 复现步骤：`ProfileView` 调用 `updateProfile` → `PUT /user/profile` → 后端为 `PUT /api/user/info` → 405/404
- 预期结果：用户中心接口可用
- 实际结果：
  - `PUT /user/profile` vs 后端 `PUT /api/user/info`（路径不一致）
  - `PUT /user/password` 后端无此端点
  - `GET /user/favorites`、`POST/DELETE /user/favorites/{id}` 后端无此端点（收藏在 `ProductFavoriteController`，路径不同）
  - `GET /user/points` 后端无此端点（积分在 marketing-service）
- 文件位置：
  - 前端：`tailor-is-frontend/pc-mall/src/api/user.ts#L12-L53`
  - 后端：`tailor-is/tailor-is-user/src/main/java/com/tailoris/user/controller/UserController.java#L32-L34`
- 修复建议：统一用户中心接口路径，补齐 password/favorites/points 端点或调整前端至对应服务。

### PD-FE-006: 商品列表分页不一致且搜索路径不匹配
- 严重级别：严重（P1）
- 影响范围：重要功能（商品列表、搜索）
- 复现步骤：`ProductListView` 调用 `getProducts({current, size})` 期望 `PageResponse` → 后端 `PublicProductController.listProducts` 返回 `List<Product>`（无分页）→ 前端 `res.records` 为 undefined
- 预期结果：商品列表分页返回
- 实际结果：后端返回裸 List；且 `searchProducts` 调用 `/products/search`，后端搜索在 `/api/product/search`（单数 product）→ 路径不匹配
- 文件位置：
  - 前端：`tailor-is-frontend/pc-mall/src/api/product.ts#L14-L42`
  - 后端：`tailor-is/tailor-is-product/src/main/java/com/tailoris/product/controller/PublicProductController.java#L58-L77`
  - 搜索：`tailor-is/tailor-is-product/src/main/java/com/tailoris/product/controller/ProductSearchController.java#L23`
- 修复建议：后端 `listProducts` 改为分页返回 `PageResponse`；统一搜索路径为 `/api/products/search`。

### PD-FE-007: 社区帖子 API 路径不匹配
- 严重级别：严重（P1）
- 影响范围：重要功能（社区浏览、发帖、评论、点赞）
- 复现步骤：`CommunityView` 调用 `getPosts()` → `GET /community/posts` → 后端 `PostController` 路径为 `/api/community/post/list` → 404
- 预期结果：社区接口可用
- 实际结果：前端用资源式 `/community/posts`（复数），后端用动作式 `/api/community/post/create`、`/post/list`、`/post/detail/{id}`；点赞 `/posts/{id}/like`、评论 `/posts/{id}/comments` 路径在 `PostController` 中无对应
- 文件位置：
  - 前端：`tailor-is-frontend/pc-mall/src/api/community.ts#L23-L65`
  - 后端：`tailor-is/tailor-is-community/src/main/java/com/tailoris/community/controller/PostController.java#L22-L70`
- 修复建议：统一社区接口为资源式 RESTful 路径，补齐 like/comments 端点。

### PD-FE-008: GraphQL 网关解析器 HTTP 方法与路径错误
- 严重级别：严重（P1）
- 影响范围：重要功能（GraphQL 客户端购物车/订单操作）
- 复现步骤：GraphQL `updateCartItem` mutation → 解析器用 `POST /cart/{id}` → 后端期望 `PUT /api/cart/{id}` → 405
- 预期结果：GraphQL 解析器调用与后端 REST 接口一致
- 实际结果：
  - `updateCartItem` 用 `post` 应为 `put`
  - `deleteCartItem` 用 `post` 应为 `delete`
  - `clearCart` 调用 `/cart/clear`（后端不存在）
  - `order`/`orders` 查询调用 `/orders/{id}`、`/orders`（后端为 `/api/order/{orderNo}`、`/api/order/list`）
- 文件位置：`tailor-is-frontend/graphql-gateway/resolvers.ts#L264-L300, L159-L185`
- 修复建议：按后端实际路由修正 GraphQL 解析器的 HTTP 方法和路径。

### PD-FE-009: 网关 user/product 路由硬编码 URI 违反服务发现规范
- 严重级别：严重（P1）
- 影响范围：重要功能（容器化/集群部署下用户与商品服务可达性）
- 复现步骤：Docker/k8s 部署 → `user-route` 指向 `http://localhost:8101`（容器内无此服务）→ 路由失败
- 预期结果：所有路由使用 `lb://` 服务发现
- 实际结果：`user-route` 用 `http://localhost:8101`，`product-route` 用 `http://host.docker.internal:8102`，其余路由用 `lb://`。该类注释（L17）明确要求"禁止写死 http://localhost:port"，属自有规范违规
- 文件位置：`tailor-is/tailor-is-core-gateway/src/main/java/com/tailoris/coregateway/config/CoreGatewayRouteConfig.java#L52-L59`
- 修复建议：改为 `lb://tailor-is-user` 和 `lb://tailor-is-product`。

### PD-FE-010: 支付服务 v1 路径无法经网关可达
- 严重级别：严重（P1）
- 影响范围：重要功能（支付下单）
- 复现步骤：请求 `/api/v1/payment/wechat` → 网关 `payment-route` 谓词为 `/api/payment/**`（无 v1）→ 不匹配 → 404
- 预期结果：支付服务经网关可达
- 实际结果：`PayController` 映射 `/api/v1/payment`，但网关路由谓词仅含 `/api/payment/**`、`/api/settlement/**` 等（均无 v1 段），`/api/v1/payment/**` 不被匹配。同理 merchant/copyright/ai 等 v1 控制器均存在此问题
- 文件位置：
  - 后端：`tailor-is/tailor-is-payment/src/main/java/com/tailoris/payment/controller/PayController.java#L37`
  - 网关：`tailor-is/tailor-is-core-gateway/src/main/java/com/tailoris/coregateway/config/CoreGatewayRouteConfig.java#L67`
- 修复建议：网关路由谓词增加 `/api/v1/payment/**` 等 v1 前缀，或后端统一去掉 v1。

### PD-FE-011: 前端 payOrder 未传后端必需的 payType 参数
- 严重级别：严重（P1）
- 影响范围：重要功能（订单支付触发）
- 复现步骤：前端 `payOrder(orderNo)` → `POST /orders/{orderNo}/pay`（无 body/params）→ 后端 `@RequestParam Integer payType` 必填 → 400
- 预期结果：支付请求携带支付方式参数
- 实际结果：前端未传 `payType`，后端必填，请求被拒
- 文件位置：
  - 前端：`tailor-is-frontend/pc-mall/src/api/order.ts#L40-L45`
  - 后端：`tailor-is/tailor-is-order/src/main/java/com/tailoris/order/controller/OrderController.java#L57-L62`
- 修复建议：前端补传 `payType`，并与 PD-FE-002 一并统一方法与路径。

---

## 二、类型定义错误

### PD-TD-001: GraphQL fetchProducts 期望 records 字段但后端返回裸 List
- 严重级别：严重（P1）
- 影响范围：重要功能（GraphQL 商品列表查询）
- 复现步骤：GraphQL `products` 查询 → `fetchProducts` 执行 `raw.records.map(...)` → 后端返回 `List<Product>`（无 records 字段）→ `raw.records` 为 undefined → `.map` 抛 TypeError
- 预期结果：GraphQL 解析器与后端响应结构一致
- 实际结果：解析器假设返回 `{ records: [...] }` 分页结构，后端 `PublicProductController` 实际返回 `List<Product>`
- 文件位置：`tailor-is-frontend/graphql-gateway/resolvers.ts#L361-L364`
- 修复建议：与 PD-FE-006 一并修复后端分页返回，或解析器适配 List 结构。

### PD-TD-002: 前端 catch 子句使用 any 类型
- 严重级别：轻微（P3）
- 影响范围：界面（类型安全）
- 复现步骤：代码审查发现 `catch (error: any)` 使用
- 预期结果：使用 `unknown` 类型并做类型收窄
- 实际结果：3 处使用 `catch (error: any)`，违背项目已推行的"M-23 使用 unknown 替代 any"规范
- 文件位置：
  - `tailor-is-frontend/pc-mall/src/views/RegisterView.vue#L470`
  - `tailor-is-frontend/platform-admin/src/views/system/UserListView.vue#L312, L339`
- 修复建议：改为 `catch (error: unknown)` 并使用 `instanceof` 或类型守卫。

### PD-TD-003: GraphQL 解析器 mutation 方法签名与后端契约不符
- 严重级别：严重（P1）
- 影响范围：重要功能（GraphQL 购物车变更）
- 复现步骤：`UpdateCartItemInput` 仅含 `{id, quantity}` → 解析器发 `POST` → 后端 `PUT /api/cart/{id}` 接收 `CartUpdateRequest`（含 `checked` 字段）
- 预期结果：GraphQL Input 类型与后端 DTO 字段、HTTP 方法一致
- 实际结果：方法用 `post`（应为 `put`），且未传递 `checked` 字段
- 文件位置：`tailor-is-frontend/graphql-gateway/resolvers.ts#L273-L280`
- 修复建议：修正为 `put` 方法，Input 类型补齐后端必需字段。

---

## 三、规则违规项

> 对照 `tailor-is/docs/CODING_STANDARDS.md` 检查

### PD-RV-001: 用户状态使用魔法数字 0
- 严重级别：一般（P2）
- 影响范围：次要功能（代码可维护性）
- 复现步骤：审查 `WechatLoginServiceImpl` 与 `AuthController` 状态判断
- 预期结果：按编码规范 §1.3 使用 `UPPER_SNAKE_CASE` 常量
- 实际结果：直接使用魔法数字 `0`（`user.getStatus() == 0`、`user.setStatus(0)`），语义不明且易引发 PD-FE-001 类错误
- 文件位置：
  - `tailor-is/tailor-is-user/src/main/java/com/tailoris/user/service/impl/WechatLoginServiceImpl.java#L106, L188`
  - `tailor-is/tailor-is-user/src/main/java/com/tailoris/user/controller/AuthController.java#L94, L159`
- 修复建议：定义 `UserStatusEnum` 或常量 `USER_STATUS_NORMAL=0`，替换所有魔法数字。

### PD-RV-002: API 路径违反自有 RESTful 规范（单数、无 v1 前缀）
- 严重级别：一般（P2）
- 影响范围：次要功能（API 一致性）
- 复现步骤：对照编码规范 §6.1"使用名词复数表示资源：`/api/v1/users`"
- 预期结果：所有资源路径使用复数 + `/api/v1/` 前缀
- 实际结果：`/api/order`、`/api/product`、`/api/user`、`/api/community/post` 均为单数且无 v1 前缀，与规范冲突，也是 PD-FE-002/005/007 路径不匹配的根因
- 文件位置：各 Controller `@RequestMapping`（OrderController、ProductController、UserController、PostController 等）
- 修复建议：统一迁移至 `/api/v1/{复数资源}` 规范路径。

### PD-RV-003: 已弃用控制器指向不存在的 v1 版本
- 严重级别：一般（P2）
- 影响范围：次要功能（API 治理）
- 复现步骤：`UserController`、`ProductController`、`OrderController` 标注 `@Deprecated`，描述"请使用 /api/v1/user"，但 user/product/order 服务无 v1 控制器
- 预期结果：弃用指向的替代接口实际存在
- 实际结果：v1 控制器仅存在于 merchant/payment/copyright/ai/pattern/academy/im/analytics，user/product/order/community 缺失，弃用标注误导
- 文件位置：
  - `tailor-is/tailor-is-user/src/main/java/com/tailoris/user/controller/UserController.java#L26-L30`
  - `tailor-is/tailor-is-product/src/main/java/com/tailoris/product/controller/ProductController.java#L24-L27`
  - `tailor-is/tailor-is-order/src/main/java/com/tailoris/order/controller/OrderController.java#L18-L21`
- 修复建议：补齐 v1 控制器或移除未落地的弃用标注。

### PD-RV-004: 网关 application.yml 与 Java Config 路由定义重复
- 严重级别：一般（P2）
- 影响范围：次要功能（配置治理）
- 复现步骤：`application.yml` L73-84 定义 product-route、community-route，`CoreGatewayRouteConfig.java` L56-91 也定义同名路由
- 预期结果：路由定义单一来源
- 实际结果：YAML 与 Java Config 重复定义 product/community 路由，且 URI 不一致（YAML 用 `http://host.docker.internal`，Java 用 `lb://`），易引发路由冲突与混淆
- 文件位置：
  - `tailor-is/tailor-is-core-gateway/src/main/resources/application.yml#L73-L84`
  - `tailor-is/tailor-is-core-gateway/src/main/java/com/tailoris/coregateway/config/CoreGatewayRouteConfig.java#L56-L91`
- 修复建议：删除 application.yml 中的重复路由，保留 Java Config 作为唯一权威。

---

## 四、性能瓶颈

### PD-PB-001: vue-vendor chunk 合并 element-plus 易超体积阈值
- 严重级别：一般（P2）
- 影响范围：次要功能（首屏加载性能）
- 复现步骤：构建 pc-mall → `manualChunks` 将 vue/pinia/vue-router/element-plus/@element-plus 全部归入 `vue-vendor` → 单 chunk 体积超 500KB 警告阈值
- 预期结果：vendor 合理拆分，单 chunk 控制在阈值内
- 实际结果：element-plus 体积较大（含图标），与 vue 核心合并后 `vue-vendor` 单 chunk 显著超 500KB，影响首屏加载
- 文件位置：`tailor-is-frontend/pc-mall/vite.config.ts#L93-L104`
- 修复建议：将 element-plus 独立为 `element-vendor` chunk（注意规避已注释说明的循环依赖问题）。

### PD-PB-002: 订单热门缓存回写循环内多次 Redis 调用未批量化
- 严重级别：一般（P2）
- 影响范围：次要功能（订单详情查询性能）
- 复现步骤：查询订单详情 → `cacheHotOrder` 对每个 `OrderItem` 循环调用 `hotOrderCache.get` + `hotOrderCache.put` → N 个商品产生 2N 次 Redis 往返
- 预期结果：使用 pipeline 或批量接口
- 实际结果：`recordProductAccessForOrder`（L723）、`cacheHotOrder`（L771）均在循环内逐条调用 Redis；同文件 `evictHotOrderCache`（L752）已用 `evictBatch` 批量化，模式不一致
- 文件位置：`tailor-is/tailor-is-order/src/main/java/com/tailoris/order/service/impl/OrderServiceImpl.java#L718-L786`
- 修复建议：为 `HotOrderCache` 增加批量 put/get 接口，循环改为批处理。

### PD-PB-003: 构建使用 terser 压缩慢于 esbuild
- 严重级别：轻微（P3）
- 影响范围：界面（构建效率）
- 复现步骤：`npm run build` → vite 使用 `minify: 'terser'`
- 预期结果：使用 esbuild 压缩提升构建速度
- 实际结果：terser 较 esbuild 慢数倍；且 `reportCompressedSize: true` 进一步拖慢构建
- 文件位置：`tailor-is-frontend/pc-mall/vite.config.ts#L76, L120`
- 修复建议：评估迁移至 `minify: 'esbuild'`，CI 构建关闭 `reportCompressedSize`。

---

## 五、安全漏洞

### PD-SV-001: 版权服务 AES 密钥硬编码默认值残留
- 严重级别：严重（P1）
- 影响范围：核心功能（版权数据加密）
- 复现步骤：检查 `CopyrightServiceImpl` `@Value` 注解
- 预期结果：密钥从安全配置中心（Vault）注入，无默认值
- 实际结果：`@Value("${copyright.aes.key:t8V4kL0mN2pR6sQ9wX3yZ7aB1cD5eF0hI4jK8lM2nO6pQ0rS4tU8vW0xY2zA4bC}")` 保留 64 字符硬编码默认密钥，若环境变量未设置将使用该明文密钥
- 文件位置：`tailor-is/tailor-is-copyright/src/main/java/com/tailoris/copyright/service/impl/CopyrightServiceImpl.java#L78`
- 修复建议：移除默认值，强制从 Vault 注入，启动时校验密钥已配置。

### PD-SV-002: prod.yml mysqld-exporter 硬编码数据库真实密码
- 严重级别：严重（P1）
- 影响范围：核心功能（数据库凭证泄露）
- 复现步骤：检查 `docker-compose.prod.yml` mysqld-exporter 环境变量
- 预期结果：密码通过 `${MYSQL_PASSWORD}` 注入
- 实际结果：`DATA_SOURCE_NAME: "root:mysql_ZmY2sr@tcp(localhost:3306)/"` 明文真实密码；`deploy/my.cnf#L3` 与 `perf/analyze-slow-queries.sh#L25` 同样硬编码
- 文件位置：
  - `deploy/docker-compose.prod.yml#L980`
  - `deploy/my.cnf#L3`
  - `deploy/perf/analyze-slow-queries.sh#L25`
- 修复建议：改用 `${MYSQL_PASSWORD}` 环境变量，清理 my.cnf 与脚本中的明文密码。

### PD-SV-003: Grafana 管理员密码硬编码
- 严重级别：严重（P1）
- 影响范围：核心功能（监控后台凭证泄露）
- 复现步骤：检查监控 compose 与 task4 脚本
- 预期结果：密码强制通过环境变量注入
- 实际结果：`GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_PASSWORD:-TailorIS2026@Grafana}` 保留默认明文密码；`task4-prometheus.sh`、`task4-fix.sh`、`task4-deploy.sh`、`start-monitoring.sh` 多处硬编码
- 文件位置：
  - `deploy/docker-compose-monitoring.yml#L56`
  - `deploy/task4-prometheus.sh#L326, L364`
  - `deploy/task4-fix.sh#L53, L106`
  - `deploy/start-monitoring.sh#L115`
- 修复建议：移除所有默认值，使用 `${GRAFANA_PASSWORD:?required}` 强制注入。

### PD-SV-004: JDBC 连接串 allowPublicKeyRetrieval=true 残留
- 严重级别：严重（P1）
- 影响范围：核心功能（数据库连接安全）
- 复现步骤：检查 services.yml 与 k8s configmap
- 预期结果：生产环境关闭 allowPublicKeyRetrieval，使用证书认证
- 实际结果：`deploy/docker-compose.services.yml` 全部 17 条 JDBC URL 与 `deploy/k8s/production/configmap.yaml#L61-L76` 全部 16 条 URL 均含 `allowPublicKeyRetrieval=true`，存在中间人攻击风险
- 文件位置：
  - `deploy/docker-compose.services.yml#L84-L718`
  - `deploy/k8s/production/configmap.yaml#L61-L76`
- 修复建议：生产环境移除该参数，配置 MySQL SSL 证书。

### PD-SV-005: dev/staging 环境与脚本 useSSL=false
- 严重级别：一般（P2）
- 影响范围：次要功能（传输安全）
- 复现步骤：检查 development/staging compose 与部署脚本
- 预期结果：所有环境启用 SSL
- 实际结果：`docker-compose.development.yml#L92`、`task5-actuator.sh#L78, L152`、`deploy-services.sh#L59`、`task3-prepare.sh#L131` 均使用 `useSSL=false`
- 文件位置：见上
- 修复建议：统一启用 `useSSL=true` 并配置信任库。

### PD-SV-006: Actuator 暴露 metrics 端点且 health 全量展示
- 严重级别：严重（P1）
- 影响范围：核心功能（信息泄露）
- 复现步骤：访问 `/actuator/metrics`、`/actuator/health`
- 预期结果：仅暴露 health（不展示细节）与 prometheus（需认证）
- 实际结果：`tailor-is-common/src/main/resources/application.yml`（所有服务继承）配置 `include: health,info,prometheus,metrics` 且 `show-details: always`，metrics 端点暴露 JVM/DB/线程池等内部指标，health 暴露组件依赖详情
- 文件位置：`tailor-is/tailor-is-common/src/main/resources/application.yml#L1-L12`
- 修复建议：移除 `metrics`，`show-details` 改为 `when-authorized`，prometheus 端点加认证。

### PD-SV-007: dev/staging 环境默认密码
- 严重级别：一般（P2）
- 影响范围：次要功能（环境安全）
- 复现步骤：检查 staging/development compose
- 预期结果：无默认密码
- 实际结果：staging 含 `mysql_staging`、`tailor_staging`、`redis_staging`、`rabbitmq_staging`、`grafana_staging`；development 含 `mysql_dev`、`tailor_dev`、`redis_dev`、`rabbitmq_dev`、`grafana_dev`；`deploy-services.sh#L11-L20` 硬编码 `redis_RSeR4G`、`rabbitmq`、`mysql_CA75Yk`
- 文件位置：
  - `deploy/docker-compose.staging.yml#L11-L140`
  - `deploy/docker-compose.development.yml#L8-L188`
  - `deploy/deploy-services.sh#L11-L20`
- 修复建议：所有密码使用 `${VAR:?required}` 强制注入。

### PD-SV-008: 管理端口在 staging/development/monitoring 显式暴露
- 严重级别：一般（P2）
- 影响范围：次要功能（端口暴露面）
- 复现步骤：检查 compose ports 映射
- 预期结果：管理端口仅内部访问
- 实际结果：staging/development 暴露 3306、6379、5672、15672、8848、9090；`docker-compose.services.yml#L128` 映射 `9000:8080`；prod.yml 对 node-exporter/mysqld-exporter/redis-exporter 使用 `network_mode: host` 暴露全部端口
- 文件位置：
  - `deploy/docker-compose.staging.yml#L18-L125`
  - `deploy/docker-compose.development.yml`
  - `deploy/docker-compose.prod.yml#L956, L978, L996`
- 修复建议：仅暴露必要端口，生产环境用 `network_mode: bridge` + 内部网络。

### PD-SV-009: prod.yml 监控组件使用 host 网络模式
- 严重级别：一般（P2）
- 影响范围：次要功能（网络隔离）
- 复现步骤：检查 prod.yml node-exporter/mysqld-exporter/redis-exporter
- 预期结果：容器网络隔离
- 实际结果：三个 exporter 均用 `network_mode: host`，容器与宿主共享网络栈，扩大攻击面
- 文件位置：`deploy/docker-compose.prod.yml#L956, L978, L996`
- 修复建议：改用 bridge 网络 + 端口映射。

### PD-SV-010: Nacos 默认密码 nacos 硬编码
- 严重级别：一般（P2）
- 影响范围：次要功能（配置中心安全）
- 复现步骤：检查 `task3-fix-admin.sh`
- 预期结果：Nacos 管理员密码强随机
- 实际结果：`echo 'NACOS_AUTH_ADMIN_PASSWORD=nacos' >> /dest/.env` 写入默认弱密码
- 文件位置：`deploy/task3-fix-admin.sh#L21`
- 修复建议：使用强随机密码并通过环境变量注入。

### PD-SV-011: refresh_token 存储于 sessionStorage 存在 XSS 泄露窗口
- 严重级别：一般（P2）
- 影响范围：次要功能（Token 安全）
- 复现步骤：审查 `request.ts` token 刷新逻辑
- 预期结果：refresh_token 存于 httpOnly cookie
- 实际结果：`refreshToken()` 从 `sessionStorage.getItem('refresh_token')` 读取，代码注释（L87）已自认"应迁移至服务端 httpOnly cookie"
- 文件位置：`tailor-is-frontend/pc-mall/src/api/request.ts#L87-L100`
- 修复建议：refresh_token 迁移至 httpOnly cookie，前端不再持久化。

---

## 六、逻辑缺陷

### PD-LD-001: 订单支付存在双重分歧路径
- 严重级别：严重（P1）
- 影响范围：核心功能（支付链路）
- 复现步骤：分析支付流程
  - 路径 A：前端 `payOrder` → `POST /orders/{orderNo}/pay` → order-service 直接 `order.setStatus(PAID)`（无需真实付款）
  - 路径 B：payment-service `PayController` `/api/v1/payment/wechat` → 调用微信/支付宝 → 回调 `payCallback` 更新
- 预期结果：单一支付链路，先创建支付单再经回调确认
- 实际结果：order-service 的 `payOrder`（L303）直接将订单标记为已支付并扣减库存，绕过真实支付网关；与 payment-service 真实支付流程并存，导致可被绕过付款直接发货

```
流程图：
  前端 payOrder ──► order-service.payOrder ──► 直接标记 PAID + 扣库存（绕过付款）
                    │
  前端 (未调用) ──► payment-service.wechatPay ──► 微信支付 ──► 回调 ──► payCallback
```
- 文件位置：`tailor-is/tailor-is-order/src/main/java/com/tailoris/order/service/impl/OrderServiceImpl.java#L300-L354`
- 修复建议：移除 order-service 直接标记支付的能力，支付状态仅由 payment-service 回调驱动。

### PD-LD-002: 微信登录状态校验反转（逻辑缺陷视角）
- 严重级别：阻断（P0）
- 影响范围：核心功能（登录状态机）
- 复现步骤：同 PD-FE-001
- 预期结果：状态机 `status==0 → 正常 → 放行`，`status!=0 → 禁用 → 拦截`
- 实际结果：`WechatLoginServiceImpl` 实现与状态机语义相反，且与 `AuthController` 验证码登录分支（L94）逻辑矛盾，属状态判断逻辑缺陷
- 文件位置：`tailor-is/tailor-is-user/src/main/java/com/tailoris/user/service/impl/WechatLoginServiceImpl.java#L106`
- 修复建议：同 PD-FE-001，统一状态机常量后修正判断。

### PD-LD-003: GraphQL clearCart 调用不存在的后端端点
- 严重级别：严重（P1）
- 影响范围：重要功能（清空购物车）
- 复现步骤：GraphQL `clearCart` mutation → 解析器 `POST /cart/clear` → 后端 `CartController` 无 `/clear` 端点 → 404，但解析器仍 `return true` 误导前端
- 预期结果：清空购物车成功或返回真实错误
- 实际结果：后端无端点，解析器却返回 `true`，前端误以为清空成功
- 文件位置：`tailor-is-frontend/graphql-gateway/resolvers.ts#L292-L300`
- 修复建议：后端补 `clearCart` 端点，或解析器返回真实状态。

### PD-LD-004: 商品列表与首页数据加载链路类型断点
- 严重级别：严重（P1）
- 影响范围：核心功能（首页商品展示）
- 复现步骤：`HomeView.vue` 调用 `getPosts({current:1, size:3})` → 访问 `res.records` → 后端社区列表返回 `PageResponse`（含 records）；但 `getProducts` 同样期望 `PageResponse` → 后端 `PublicProductController` 返回 `List` → `res.records` undefined → 首页商品列表渲染异常
- 预期结果：所有分页接口返回一致结构
- 实际结果：社区接口返回分页结构（records 可用），商品接口返回裸 List（records 为 undefined），同一前端处理逻辑下行为不一致
- 文件位置：
  - `tailor-is-frontend/pc-mall/src/views/HomeView.vue#L124-L132`
  - `tailor-is/tailor-is-product/src/main/java/com/tailoris/product/controller/PublicProductController.java#L58-L77`
- 修复建议：统一所有列表接口为 `PageResponse` 分页结构。

---

## 七、文档缺失

### PD-DM-001: api-overview.md 接口路径与前端实际调用不一致
- 严重级别：一般（P2）
- 影响范围：次要功能（接口文档可信度）
- 复现步骤：对照 `docs/api/api-overview.md` 与前端 API 文件
- 预期结果：文档路径与前端调用一致
- 实际结果：文档记录 `/api/user/address`、`/api/order`、`/api/community/post`，前端实际调用 `/addresses`、`/orders`、`/community/posts`，文档无法作为前后端对接依据
- 文件位置：`docs/api/api-overview.md#L54-L70`
- 修复建议：以实际后端路由为准修订文档，或推动前端对齐文档路径。

### PD-DM-002: api-overview.md 标注弃用至不存在的 v1 接口
- 严重级别：一般（P2）
- 影响范围：次要功能（文档准确性）
- 复现步骤：文档 L45/L76 标注"已弃用，请使用 /api/v1/user""/api/v1/product"
- 预期结果：弃用指向的 v1 接口存在
- 实际结果：user/product/order/community 服务均无 v1 控制器（见 PD-RV-003），文档误导调用方迁移至不存在接口
- 文件位置：`docs/api/api-overview.md#L45, L76`
- 修复建议：补齐 v1 接口或修正文档移除弃用标注。

### PD-DM-003: 各服务 README 仅含启动说明，缺少 API 契约细节
- 严重级别：轻微（P3）
- 影响范围：界面（开发者体验）
- 复现步骤：审查各服务 README
- 预期结果：README 含 API 契约、依赖服务、环境变量说明
- 实际结果：README 仅含功能概述、技术栈、启动命令，无接口清单、无字段契约、无异常码说明
- 文件位置：`tailor-is/tailor-is-user/README.md`、`tailor-is/tailor-is-order/README.md` 等
- 修复建议：补充各服务 API 契约摘要或链接至 Knife4j。

### PD-DM-004: 缺少前后端 API 契约对齐文档
- 严重级别：轻微（P3）
- 影响范围：界面（协作效率）
- 复现步骤：检索文档目录
- 预期结果：存在前后端接口对齐/契约文档
- 实际结果：无独立文档记录前端调用与后端路由的映射关系，导致 PD-FE-002~008 类批量不匹配问题未在开发阶段发现
- 文件位置：`docs/` 目录
- 修复建议：新增前后端 API 契约矩阵文档，CI 中校验前端调用路径在后端有对应路由。

### PD-DM-005: 服务 README 含空密码占位符
- 严重级别：轻微（P3）
- 影响范围：界面（文档安全）
- 复现步骤：审查 README 启动示例
- 预期结果：示例使用占位变量而非空值
- 实际结果：`export REDIS_PASSWORD=`、`export MYSQL_PASSWORD=` 直接留空，可能误导开发者以空密码启动生产
- 文件位置：`tailor-is/tailor-is-user/README.md#L29-L33`、`tailor-is/tailor-is-order/README.md#L34-L38`
- 修复建议：改为 `export MYSQL_PASSWORD=<your-password>` 占位提示。

---

## 附录：问题优先级矩阵

### P0 阻断级（4 项，需立即修复）
| 编号 | 问题 | 文件 |
|------|------|------|
| PD-FE-001 | 微信登录状态校验反转 | WechatLoginServiceImpl.java#L106 |
| PD-FE-002 | 订单 API 全面不匹配 | order.ts / OrderController.java |
| PD-FE-004 | 地址 API 路径不匹配且无网关路由 | address.ts / AddressController.java |
| PD-LD-002 | 微信登录状态机逻辑缺陷（同 PD-FE-001） | WechatLoginServiceImpl.java#L106 |

### P1 严重级（18 项，需本迭代修复）
| 编号 | 问题 |
|------|------|
| PD-FE-003 | 购物车结算请求体不匹配 + 缺 clearCart |
| PD-FE-005 | 用户资料/密码/收藏/积分接口缺失 |
| PD-FE-006 | 商品列表分页不一致 + 搜索路径不匹配 |
| PD-FE-007 | 社区帖子 API 路径不匹配 |
| PD-FE-008 | GraphQL 解析器方法/路径错误 |
| PD-FE-009 | 网关 user/product 路由硬编码 |
| PD-FE-010 | 支付服务 v1 路径网关不可达 |
| PD-FE-011 | 前端 payOrder 未传 payType |
| PD-TD-001 | GraphQL fetchProducts 类型断言错误 |
| PD-TD-003 | GraphQL mutation 方法签名不符 |
| PD-SV-001 | AES 密钥硬编码 |
| PD-SV-002 | mysqld-exporter 硬编码密码 |
| PD-SV-003 | Grafana 硬编码密码 |
| PD-SV-004 | allowPublicKeyRetrieval 残留 |
| PD-SV-006 | Actuator metrics 端点暴露 |
| PD-LD-001 | 订单支付双重分歧路径 |
| PD-LD-003 | GraphQL clearCart 调用不存在端点 |
| PD-LD-004 | 首页商品列表类型断点 |

### P2 一般级（14 项）与 P3 轻微级（5 项）
详见正文各条目。

---

## 修复建议执行顺序

1. **第一优先（P0）**：修复微信登录状态校验（PD-FE-001/LD-002）、订单 API 契约（PD-FE-002）、地址 API 路由（PD-FE-004）——恢复核心登录与下单链路。
2. **第二优先（P1 功能）**：统一购物车、用户中心、商品、社区、GraphQL 解析器接口契约（PD-FE-003~011、PD-TD-001/003、PD-LD-003/004）。
3. **第三优先（P1 安全）**：清理所有硬编码密钥/密码、收敛 Actuator 端点、修复 JDBC 连接串（PD-SV-001~006）。
4. **第四优先（P1 逻辑）**：消除订单支付双重路径（PD-LD-001）、修复网关路由（PD-FE-009/010）。
5. **第五优先（P2/P3）**：规则违规、性能优化、文档完善。
