# Tailor IS 项目问题清单

> 本文档为 Tailor IS（裁智云）服装全产业平台的完整问题清单，按 **3 大模块域**（后端、前端、部署与基础设施）及其下属 **子类别** 与 **4 个严重级别**（Critical / High / Medium / Low）分类编排。

| 项目 | 内容 |
| --- | --- |
| 文档名称 | Tailor IS 项目问题清单（PROJECT-ISSUE-INVENTORY） |
| 编制日期 | 2026-06-20 |
| 版本号 | v1.0 |
| 编制依据 | 项目代码审计、安全扫描、部署配置核查、前端代码审查、CI/CD 工作流检查、数据库 SQL 审查；参考已有报告 `docs/reports/PROJECT-COMPREHENSIVE-AUDIT-FINAL.md` |
| 问题总数 | 105 项（后端 44 / 前端 27 / 部署与基础设施 34） |
| 严重级别 | Critical / High / Medium / Low |

---

## 一、统计汇总表

### 1.1 按模块域与严重级别统计

| 模块域 | Critical | High | Medium | Low | 合计 |
| --- | ---: | ---: | ---: | ---: | ---: |
| 后端问题 | 11 | 14 | 17 | 2 | 44 |
| 前端问题 | 3 | 6 | 11 | 7 | 27 |
| 部署与基础设施 | 3 | 12 | 17 | 2 | 34 |
| **合计** | **17** | **32** | **45** | **11** | **105** |

### 1.2 按子类别统计

| 模块域 | 子类别 | Critical | High | Medium | Low | 合计 |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| 后端 | 逻辑缺陷 | 6 | 3 | 0 | 0 | 9 |
| 后端 | 安全漏洞 | 5 | 6 | 0 | 0 | 11 |
| 后端 | 性能瓶颈 | 0 | 3 | 2 | 0 | 5 |
| 后端 | 类型定义问题 | 0 | 0 | 3 | 0 | 3 |
| 后端 | 编码规范违规 | 0 | 0 | 5 | 0 | 5 |
| 后端 | 事务与并发问题 | 0 | 2 | 1 | 0 | 3 |
| 后端 | 文档与测试 | 0 | 0 | 3 | 1 | 4 |
| 后端 | 其他问题 | 0 | 0 | 3 | 1 | 4 |
| 前端 | 安全漏洞 | 3 | 4 | 0 | 0 | 7 |
| 前端 | TypeScript 类型问题 | 0 | 2 | 2 | 0 | 4 |
| 前端 | 性能问题 | 0 | 0 | 4 | 1 | 5 |
| 前端 | 逻辑缺陷 | 0 | 0 | 5 | 1 | 6 |
| 前端 | 编码规范问题 | 0 | 0 | 0 | 3 | 3 |
| 前端 | 文档缺失 | 0 | 0 | 0 | 2 | 2 |
| 部署 | Docker/部署配置 | 2 | 3 | 3 | 0 | 8 |
| 部署 | Kubernetes 配置 | 1 | 3 | 3 | 0 | 7 |
| 部署 | Nginx 配置 | 0 | 2 | 4 | 0 | 6 |
| 部署 | 监控配置 | 0 | 2 | 2 | 0 | 4 |
| 部署 | CI/CD 工作流 | 0 | 2 | 2 | 0 | 4 |
| 部署 | 数据库/SQL 配置 | 0 | 0 | 2 | 2 | 4 |

---

## 二、后端问题（44 项）

### 2.1 逻辑缺陷

#### Critical

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| B-L-1 | Critical | `tailor-is/tailor-is-user/src/main/java/com/tailoris/user/service/impl/SysUserServiceImpl.java` | 405 | 用户刷新 Token 时状态判断逻辑反转：`if (user.getStatus() != USER_STATUS_DISABLED)` 抛出"用户已被禁用"，导致正常用户无法刷新 Token，被禁用用户反而可以刷新。 | 将 `!=` 改为 `==` |
| B-L-2 | Critical | `tailor-is/tailor-is-user/src/main/java/com/tailoris/user/service/impl/WechatLoginServiceImpl.java` | 104 | 微信登录用户状态校验反转：`if (user.getStatus() != null && user.getStatus() == 0)` 抛出"账号已被禁用"，但状态 0 表示正常，状态 1 才是禁用。 | 将 `== 0` 改为 `!= 0` |
| B-L-3 | Critical | `tailor-is/tailor-is-user/src/main/java/com/tailoris/user/service/impl/SysUserServiceImpl.java` | 222 | 新注册用户被创建为禁用状态：`user.setStatus(USER_STATUS_DISABLED)`，注册后无法登录。 | 改为 `USER_STATUS_NORMAL` |
| B-L-4 | Critical | `tailor-is/tailor-is-user/src/main/java/com/tailoris/user/service/impl/WechatLoginServiceImpl.java` | 186 | 新微信用户被创建为禁用状态：`user.setStatus(1)`，微信注册后无法登录。 | 改为 `0` |
| B-L-5 | Critical | `tailor-is/tailor-is-payment/src/main/java/com/tailoris/payment/service/impl/PaymentServiceImpl.java` | 232 | 支付回调 `getMerchantIdFromRecord()` 直接 `return record.getOrderId()`，将订单 ID 作为商家 ID 返回，导致资金入账到错误的商家账户。 | 实现正确的商家 ID 查询逻辑 |
| B-L-6 | Critical | `tailor-is/tailor-is-copyright/src/main/java/com/tailoris/copyright/service/impl/CopyrightServiceImpl.java` | 98-102 | 版权高风险作品未拦截：相似度 ≥90% 仅执行 `log.warn` 记录日志，未抛出异常拦截上传，存在版权侵权风险。 | 抛出异常拦截上传 |

#### High

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| B-L-7 | High | `tailor-is/tailor-is-order/src/main/java/com/tailoris/order/service/impl/OrderServiceImpl.java` | 409-427 | 订单取消未释放库存：`cancelOrder()` 仅修改订单状态，未调用 `inventoryService.releaseStock()`，导致库存永久占用。 | 添加库存释放调用 |
| B-L-8 | High | `tailor-is/tailor-is-payment/src/main/java/com/tailoris/payment/service/impl/PaymentServiceImpl.java` | 256-263 | 支付状态查询缓存逻辑无效：`getPaymentStatus()` 检查缓存后无论是否命中都执行 `selectById` 查询数据库，缓存形同虚设。 | 缓存命中时直接返回 |
| B-L-9 | High | `tailor-is/tailor-is-payment/src/main/java/com/tailoris/payment/service/impl/PaymentServiceImpl.java` | 199-203 | 支付回调验签失败处理不当：`alipayPublicKey` 未配置时返回 false，注释说明为"开发环境容错"但实际阻断所有回调。 | 配置缺失时应启动失败 |

### 2.2 安全漏洞

#### Critical

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| B-S-1 | Critical | `tailor-is/tailor-is-copyright/src/main/resources/application.yml` | 125 | 版权服务 AES 密钥硬编码：`key: t8V4kL0mN2pR6sQ9wX3yZ7aB1cD5eF0hI4jK8lM2nO6pQ0rS4tU8vW0xY2zA4bC` 生产加密密钥直接提交到代码库。 | 迁移至环境变量 |
| B-S-2 | Critical | `tailor-is/tailor-is-copyright/src/main/java/com/tailoris/copyright/service/impl/CopyrightServiceImpl.java` | 381 | 版权服务使用不安全的 AES-ECB 模式：`Cipher.getInstance("AES")` 默认使用 ECB 模式，相同明文产生相同密文。 | 改用 AES-256-GCM（项目已有 AesGcmCrypto） |
| B-S-3 | Critical | `tailor-is/tailor-is-copyright/src/main/java/com/tailoris/copyright/service/impl/CopyrightServiceImpl.java` | 389 | 加密失败时返回明文：`encrypt()` catch 异常后 `return data` 返回原始明文，敏感数据以明文落库。 | 加密失败应抛异常 |
| B-S-4 | Critical | `tailor-is/deploy/k8s/secret.yaml` | 16-28 | K8s Secret 文件包含可解码的占位凭证：MySQL 密码以 base64 编码直接提交至代码库。 | 改为占位符并添加替换说明 |
| B-S-5 | Critical | `tailor-is/tailor-is-payment/src/main/java/com/tailoris/payment/controller/SandboxTestController.java` | - | 沙箱测试控制器在生产代码中暴露：`/api/sandbox/**` 路径暴露测试接口且无认证。 | 添加 `@Profile("dev,test")` 注解 |

#### High

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| B-S-6 | High | 所有模块 `application.yml` | - | 所有模块 Nacos 默认凭证为 `nacos/nacos`：`username: ${NACOS_USERNAME:nacos}` / `password: ${NACOS_PASSWORD:nacos}`。 | 移除默认值，强制使用环境变量 |
| B-S-7 | High | `tailor-is/tailor-is-ai/src/main/resources/application.yml` | 154 | AI 模块云端模型 API Key 默认占位符：`api-key: ${CLOUD_MODEL_API_KEY:your-api-key-here}`。 | 移除默认占位符 |
| B-S-8 | High | 所有模块 `application.yml` 的 `datasource.url` | - | 所有模块 MySQL 启用 `allowPublicKeyRetrieval=true`，允许中间人攻击。 | 移除此参数 |
| B-S-9 | High | 所有模块 `application.yml` | - | 所有模块生产代码启用 debug 日志：`logging.level.com.tailoris.*: debug`，生产环境输出敏感信息。 | 改为 info |
| B-S-10 | High | `tailor-is/tailor-is-core-gateway/src/main/java/com/tailoris/coregateway/filter/CoreAuthGlobalFilter.java` | 105-110 | 网关 `X-User-Id` 头未签名：网关设置 `X-User-Id` 头透传给下游但未签名/HMAC，下游服务无法验证该头的真实性。 | 添加 HMAC 签名 |
| B-S-11 | High | `tailor-is/tailor-is-user/src/main/java/com/tailoris/user/controller/AuthController.java` | 121 | 微信登录接口缺少 `@Valid` 校验：`wechatLogin(@RequestBody WechatLoginRequest request)` 未加 `@Valid`。 | 添加 `@Valid` 注解 |

### 2.3 性能瓶颈

#### High

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| B-P-1 | High | `tailor-is/tailor-is-ai/src/main/java/com/tailoris/ai/cache/PatternCacheLoader.java` | 99-125 | AI 图案缓存加载 N+1 查询：`loadPatternsByType()` 先查询所有记录获取 `patternType`，再逐类型查询。 | 使用 `SELECT DISTINCT pattern_type` |
| B-P-2 | High | `tailor-is/tailor-is-user/src/main/java/com/tailoris/user/service/impl/SysUserServiceImpl.java` | 430-438 | 用户信息构建产生多次 DB 查询：`buildUserInfo()` 每次调用分别查询 `listRolesByUserId` 和 `getPermissionsByUserId`，分页场景下 N 条记录产生 2N 次查询。 | 批量查询 |
| B-P-3 | High | `tailor-is/tailor-is-order/src/main/java/com/tailoris/order/mapper/OrderInfoMapper.java` | 15-23 | 订单详情查询使用懒加载引发 N+1：`selectOrderDetailWithItems` 使用 `@Many` 和 `@One` 注解触发嵌套查询。 | 使用 JOIN 或 resultMap |

#### Medium

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| B-P-4 | Medium | `tailor-is/tailor-is-product/src/main/java/com/tailoris/product/service/impl/ProductServiceImpl.java` | 537-546 | 商品浏览量更新同步执行：方法名 `asyncIncrementViewCount` 但实际同步执行 SQL 更新，高并发下成为瓶颈。 | 使用消息队列或批量异步更新 |
| B-P-5 | Medium | `tailor-is/tailor-is-copyright/src/main/java/com/tailoris/copyright/service/impl/CopyrightServiceImpl.java` | 318-324 | 版权服务缓存仅存 `fileHash` 未存实体：`getCopyrightDetail()` 缓存仅存 fileHash 字符串，下次查询仍需查库。 | 缓存完整实体 |

### 2.4 类型定义问题

#### Medium

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| B-T-1 | Medium | `SysUser.java`、`OrderInfo.java`、`PaymentRecord.java` 等 | 43 / 41 / - | 实体类状态字段使用 `Integer` 而非枚举：状态字段使用魔法数字（如 0、1、2），可读性差且易出错。 | 推广使用枚举（如 `ProductStatusEnum`） |
| B-T-2 | Medium | `CoreAuthGlobalFilter.java`、`AuthController.java` | 99 / 154 | Sa-Token `loginId` 使用 `Object` 类型：`loginId` 返回 `Object` 需手动转换，存在类型安全风险。 | 封装类型转换工具 |
| B-T-3 | Medium | `tailor-is/tailor-is-payment/src/main/java/com/tailoris/payment/controller/SandboxTestController.java` | 33,50,66,82,95,130,138 | `SandboxTestController` 使用原始类型转换：多处强制类型转换。 | 使用 `Boolean.TRUE.equals()` |

### 2.5 编码规范违规

#### Medium

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| B-C-1 | Medium | `tailor-is/tailor-is-order/src/main/java/com/tailoris/order/service/impl/OrderServiceImpl.java` | - | 方法长度接近超限：`createOrder` 及辅助方法约 100 行，checkstyle 限制 200 行。 | 拆分方法 |
| B-C-2 | Medium | `tailor-is/tailor-is-payment/src/main/java/com/tailoris/payment/service/impl/PaymentServiceImpl.java` | 81,96,147,153,158,162 | 魔法数字使用：`in(PaymentRecord::getPayStatus, 0, 1)`、`record.setPayStatus(2)` 等。 | 替换为枚举或常量 |
| B-C-3 | Medium | `tailor-is/tailor-is-user/src/main/java/com/tailoris/user/service/impl/WechatLoginServiceImpl.java` | 224,226 | Lambda 可替换为方法引用：`r -> r.getRoleCode()` 应为 `SysRole::getRoleCode`。 | 使用方法引用 |
| B-C-4 | Medium | `tailor-is/tailor-is-copyright/src/main/java/com/tailoris/copyright/service/impl/CopyrightServiceImpl.java` | 322-323 | 空 catch 块：`catch (Exception ignored) {}` 空捕获，异常被静默吞掉。 | 补充日志或处理 |
| B-C-5 | Medium | `CopyrightServiceImpl.java`、`PatternServiceImpl.java` | 397-416,270-276 / 190-193 | 手动构建 JSON 字符串：手动拼接 JSON 易出错且不可维护。 | 使用 ObjectMapper |

### 2.6 事务与并发问题

#### High

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| B-TX-1 | High | `tailor-is/tailor-is-payment/src/main/java/com/tailoris/payment/service/impl/PaymentServiceImpl.java` | 115-182 | 支付回调事务与幂等锁边界不一致：同时使用 `@GlobalTransactional` 和 `@Transactional`，Redis 幂等锁在方法内管理，事务回滚但 Redis 锁已删除可能导致重复处理。 | 修正锁生命周期与事务边界一致性 |
| B-TX-2 | High | `tailor-is/tailor-is-payment/src/main/java/com/tailoris/payment/service/impl/EscrowServiceImpl.java` | 30-38 | 担保账户 `deposit` 非原子操作：`deposit()` 先尝试原子更新失败后 `getOrCreateAccount` 创建账户再重试，两次调用之间无锁，存在并发竞态。 | 使用 `INSERT ON DUPLICATE KEY` 或分布式锁 |

#### Medium

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| B-TX-3 | Medium | `tailor-is/tailor-is-order/src/main/java/com/tailoris/order/service/impl/OrderServiceImpl.java` | 359-380 | 订单创建事务内调用远程结算服务：`confirmReceive` 在 `afterCommit` 中调用 `settlementClient.settleOrder()`，远程调用失败仅 `log.error` 不重试。 | 引入消息队列保证最终一致性 |

### 2.7 文档与测试

#### Medium

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| B-D-1 | Medium | `PaymentServiceImpl.java`、`PatternGenerationStrategy.java` | 226 / 231,261 | 多处 TODO 标记未完成功能：核心功能未实现。 | 实现或明确标注为占位 |
| B-D-2 | Medium | `tailor-is/tailor-is-user/src/main/java/com/tailoris/user/service/impl/SysUserServiceImpl.java` | 232-238 | 短信验证码未实际发送：`sendSmsCode()` 仅将验证码存入 Redis 未调用短信网关。 | 接入短信网关 |
| B-D-3 | Medium | `tailor-is/tailor-is-ai/src/main/java/com/tailoris/ai/service/impl/PatternServiceImpl.java` | 168 | `exportPattern` 返回硬编码 URL：返回不存在的域名。 | 实现导出功能 |

#### Low

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| B-D-4 | Low | 所有 8 个核心模块 `README.md` | - | 模块 README 文档覆盖良好（**正面发现**）。 | 无需修复 |
| B-D-5 | Low | `tailor-is/tailor-is-payment`、`tailor-is/tailor-is-ai` 测试目录 | - | 测试覆盖情况：user/order/copyright/gateway/common 覆盖较好，payment/ai 覆盖不足。 | 补充 `PaymentService`/`EscrowService`/`PatternService` 测试 |

### 2.8 其他问题

#### Medium

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| B-O-1 | Medium | `tailor-is/tailor-is-user/src/main/resources/application.yml` | 41-54 | 用户模块 Redis 配置重复：同时配置 `spring.data.redis` 和 `spring.redis`。 | 仅保留 `spring.data.redis` |
| B-O-2 | Medium | `tailor-is/tailor-is-user/src/main/java/com/tailoris/user/controller/AuthController.java` | 181-184 | `AuthController` 存在废弃方法未删除：`@Deprecated getClientIp()` 方法仍存在。 | 删除 |
| B-O-3 | Medium | `tailor-is/tailor-is-copyright/src/main/resources/application.yml` | 93,172 | 版权服务 YAML 重复定义 `tailoris` 键：`tailoris:` 键定义两次。 | 合并 |

#### Low

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| B-O-4 | Low | - | - | （与 B-D-4 合并记录的正面发现，此处保留编号占位以与原清单对齐） | - |

---

## 三、前端问题（27 项）

### 3.1 安全漏洞

#### Critical

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| F-S-1 | Critical | `tailor-is-frontend/pc-mall/src/utils/crypto.ts`、`tailor-is-frontend/merchant-admin/src/utils/crypto.ts` | 97-119 / 75-106 | `crypto.ts` 使用可逆 XOR 伪装加密：`encryptSync`/`decryptSync` 使用 `simpleEncrypt` 做 XOR 运算，密钥与密文同存 localStorage，等同明文存储。 | 移除 XOR 伪加密，改用 Web Crypto API |
| F-S-2 | Critical | `tailor-is-frontend/platform-admin/src/api/request.ts` | - | `platform-admin` 完全无加密、无 CSRF、无错误处理：L9 直接读取明文 token，无 CSRF，响应拦截器不处理业务 code/401/网络错误。 | 添加 token 加密、CSRF 防护、完整错误处理 |
| F-S-3 | Critical | `tailor-is-frontend/pc-mall/src/api/request.ts` | 95 | `refresh_token` 明文存储于 localStorage：未加密直接存储，XSS 可读取。 | 改为加密存储或 httpOnly cookie |

#### High

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| F-S-4 | High | `tailor-is-frontend/mobile-app/utils/crypto.ts` | 190-264 | `mobile-app crypto.ts` 降级加密使用 `Math.random()` 和弱哈希：`fallbackEncrypt` 使用 `Math.random()` 生成 salt，`hashString` 使用 DJB2 变种。 | 使用密码学安全随机数 |
| F-S-5 | High | `pc-mall/src/api/request.ts`、`merchant-admin/src/api/request.ts` | 31-39 / 32-40 | CSRF token 存于 localStorage，依赖 XSS 可读：CSRF token 存于 localStorage，一旦发生 XSS 即可被窃取。 | 改用 httpOnly cookie |
| F-S-6 | High | `tailor-is-frontend/graphql-gateway/resolvers.ts` | 14-17 | GraphQL 网关无鉴权层，透传任意 token：`getAuth` 仅从请求头提取 `authorization` 原样转发，未校验有效性。 | 网关层添加鉴权校验 |
| F-S-7 | High | `tailor-is-frontend/pc-mall/src/server/server.ts` | - | SSR 服务缺少安全头与速率限制：未使用 helmet，无 rate limiting，Pinia 状态序列化仅转义 `<`。 | 添加 helmet、rate limiting、完善转义 |

### 3.2 TypeScript 类型问题

#### High

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| F-T-1 | High | `pc-mall` 和 `merchant-admin` 全部 API 文件 | - | `request<any, T>` 模式泛滥：共 105 处，axios `request<T, R>` 第一个泛型 `T` 被一律设为 `any`，类型安全形同虚设。 | 统一为 `mobile-app` 的 `post<T>` 模式 |
| F-T-2 | High | `tailor-is-frontend/platform-admin/src/api/auth.ts` | 13-18 | `platform-admin` API 完全无类型：`login` 等函数不带任何泛型返回 `any`。 | 添加泛型类型标注 |

#### Medium

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| F-T-3 | Medium | `pc-mall/src/types/index.ts`、`merchant-admin/src/types/index.ts`、`mobile-app/api/types.ts` | 180-184 / 150-162 / 1-32 | 公共类型重复定义未真正共享：`ApiResponse`/`PageResponse` 三处定义存在字段可选性差异。 | 抽取到 `shared/types/` |
| F-T-4 | Medium | `pc-mall`、`merchant-admin` 类型定义 | - | 状态字段类型不一致：pc-mall 用数字状态码，merchant-admin 用字符串字面量联合，pc-mall `User` 同时存在 `nickname` 和 `nickName`。 | 统一类型定义 |

### 3.3 性能问题

#### Medium

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| F-P-1 | Medium | `graphql-gateway/resolvers.ts` | 235-254 | GraphQL `Product.category` 解析器存在 N+1 查询：每个商品单独请求全量分类。 | 使用 DataLoader 批量加载 |
| F-P-2 | Medium | `graphql-gateway/cache.ts` | 88 | Redis 使用 `KEYS` 命令阻塞：`invalidate` 函数使用 `redis.keys`，O(N) 阻塞命令，单线程 Redis 会卡顿。 | 改用 `SCAN` 迭代 |
| F-P-3 | Medium | `pc-mall/src/api/graphql.ts` | 22-53,215-243 | GraphQL 客户端无请求去重与缓存：`graphqlQuery` 每次调用都发新请求。 | 添加缓存与去重 |
| F-P-4 | Medium | `pc-mall/src/views/ProductListView.vue` | 106-127,146-152 | 视图缺少防抖与并行加载：排序/分类/价格筛选点击即触发请求无防抖，`onMounted` 中 `loadCategories` 与 `loadProducts` 串行 `await`。 | 添加防抖，改为 `Promise.all` 并行 |

#### Low

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| F-P-5 | Low | `mobile-app/api/request.ts` | 98-102 | `mobile-app` UUID polyfill 使用 `Math.random()`：`generateUUID` 用 `Math.random()` 生成 v4 UUID，碰撞概率较高。 | 使用 `crypto.getRandomValues` |

### 3.4 逻辑缺陷

#### Medium

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| F-L-1 | Medium | `merchant-admin/src/store/user.ts` | 17,73-75 | `merchant-admin currentShopId` 不持久化：`currentShopId` 仅存于内存 `ref` 未写入 localStorage，刷新页面后丢失。 | 持久化至 localStorage |
| F-L-2 | Medium | `merchant-admin/src/store/user.ts` | 42-50 | `merchant-admin fetchUserInfo` 失败不跳转登录：catch 中仅清除 token 不跳转登录页，用户停留在错误页面。 | 失败时跳转登录页 |
| F-L-3 | Medium | `pc-mall/src/api/request.ts` | 147-188 | `pc-mall request.ts` 401 处理存在冗余分支：L147 已处理 401 但 L181-188 的 switch 又有 case 401。 | 简化 401 处理逻辑 |
| F-L-4 | Medium | `pc-mall/src/views/ProductListView.vue` | 146-152 | `ProductListView` 不监听路由查询变化：仅 `onMounted` 加载未 `watch route.query.keyword`，搜索页跳转后不刷新。 | 添加 watch 监听 |
| F-L-5 | Medium | `pc-mall/src/utils/format.ts` | 1-13 | `format.ts` 缺少空值与非法输入校验：`formatPrice` 若 `null`/`undefined` 会抛 `toFixed` 错误。 | 添加空值校验 |

#### Low

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| F-L-6 | Low | `tailor-is-frontend/shared/utils/validate.ts` | 22-24 | `validate.ts` 密码策略过弱：`isValidPwd` 仅校验长度 ≥6。 | 添加大小写/数字/符号复杂度要求 |

### 3.5 编码规范问题

#### Low

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| F-C-1 | Low | `tailor-is-frontend/mobile-app/main.js` | - | `mobile-app` 入口使用 `.js` 而非 `.ts`：其余子项目入口均为 `main.ts`。 | 迁移为 `main.ts` |
| F-C-2 | Low | 四个子项目 API 调用 | - | 四个子项目 API 调用风格不统一：pc-mall/merchant-admin 用 `request<any,T>`，mobile-app 用 `post<T>`，platform-admin 完全无类型。 | 统一为 mobile-app 模式 |
| F-C-3 | Low | `format.ts`、`crypto.ts` | - | 密码校验、文案硬编码等规范缺失：`format.ts` 硬编码中文，`crypto.ts` 实现重复。 | 抽取到 shared |

### 3.6 文档缺失

#### Low

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| F-D-1 | Low | `tailor-is-frontend/` 各子项目 | - | 缺少根级与子项目 README：仅 `e2e-tests` 有 README，pc-mall/merchant-admin/platform-admin/mobile-app/graphql-gateway 均无 README。 | 补充 README |
| F-D-2 | Low | `tailor-is-frontend/docs/FRONTEND-ARCHITECTURE.md` | 20-27 | 架构文档与实际结构不符：声称 `@shared/types/` 存在但实际 `shared/` 目录无 `types/` 文件夹。 | 修正文档或创建目录 |

---

## 四、部署与基础设施问题（34 项）

### 4.1 Docker/部署配置

#### Critical

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| D-1 | Critical | `deploy/.env.example` | 33,58,67 | 生产环境真实密码硬编码并提交至版本库：包含 `MYSQL_PASSWORD=mysql_ZmY2sr`、`REDIS_PASSWORD=redis_jD2N8n`、`RABBITMQ_PASSWORD=rabbitmq`。 | 替换为占位符 |
| D-2 | Critical | `tailor-is/deploy/k8s/ingress.yaml` | 155-156 | TLS 私钥提交至版本库：Ingress 资源内嵌 base64 编码的 `tls.crt` 和 `tls.key`。 | 改用 cert-manager 或 Secret 引用 |

#### High

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| D-3 | High | `deploy/docker-compose.prod.yml` | 126,242,282 | 数据库连接禁用 SSL：所有 JDBC URL 使用 `useSSL=false&allowPublicKeyRetrieval=true`。 | 改为 `useSSL=true` |
| D-4 | High | `docker-compose.yml`、`deploy/docker-compose.prod.yml` | 88,116-117 / 64,92,138,175,209,249,289 | 管理端口暴露至宿主机：RabbitMQ 管理 15672、Nacos 8848/9848、Grafana 3001、Prometheus 9090 等暴露。 | 移除宿主机端口映射 |
| D-5 | High | `deploy/docker-compose.prod.yml` | 124,171,205,239 | Actuator 端点全量暴露：`MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE: "*"` 暴露所有端点。 | 改为 `health,info` |

#### Medium

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| D-6 | Medium | `deploy/docker-compose-monitoring.yml` | 19,42,65 | 监控栈使用 host 网络模式无隔离：Prometheus/Alertmanager/Grafana 使用 `network_mode: host`。 | 使用 bridge 网络 |
| D-7 | Medium | `deploy/docker-compose-monitoring.yml`、`docker-compose.yml` | 60 / 774 | Grafana 默认密码硬编码：默认 `TailorIS2026@Grafana`；`docker-compose.yml` 默认 `admin123`。 | 使用环境变量 |
| D-8 | Medium | `deploy/docker-compose-monitoring.yml` | - | Prometheus/Alertmanager 无健康检查：prometheus 和 alertmanager 服务均无 healthcheck。 | 添加 healthcheck |

### 4.2 Kubernetes 配置

#### Critical

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| D-9 | Critical | `tailor-is/deploy/prometheus.yml` | 31,57,83,109,135,161,188,214,240,322 | K8s 监控命名空间不匹配监控完全失效：部署命名空间为 `tailor-is-prod`，但 Prometheus 配置使用 `namespaces: names: [tailor-is]`。 | 改为 `[tailor-is-prod]` |

#### High

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| D-10 | High | `tailor-is/deploy/k8s/` 全目录 | - | 全部 Deployment 缺失 `securityContext`：无 `runAsNonRoot`、`readOnlyRootFilesystem`、`drop ALL capabilities` 配置。 | 添加 securityContext |
| D-11 | High | `tailor-is/deploy/k8s/` 全目录 | - | 无 PodDisruptionBudget：全目录无 PDB 资源，节点维护时可能导致服务中断。 | 为核心服务添加 PDB |
| D-12 | High | `tailor-is/deploy/k8s/` 全目录 | - | 无 NetworkPolicy：全目录无 NetworkPolicy，Pod 间流量无限制。 | 添加 NetworkPolicy 限制 Pod 间流量 |

#### Medium

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| D-13 | Medium | `tailor-is/deploy/k8s/core-services/ai-deployment.yaml` | 12 | AI 服务单副本无高可用：`replicas: 1`。 | 改为 2 |
| D-14 | Medium | `tailor-is/deploy/k8s/core-services/scale-down-cronjob.yaml` | 36 | 闲时缩容将支付服务降至 1 副本：支付服务闲时缩至 1 副本，无冗余。 | 最低副本改为 2 |
| D-15 | Medium | 所有 Deployment | - | `podAntiAffinity` 为 `preferred` 而非 `required`：所有 Deployment 使用 `preferredDuringSchedulingIgnoredDuringExecution`。 | 评估改为 `required` |
| D-16 | Medium | `tailor-is/deploy/k8s/ingress.yaml` | 58-64 | Ingress 暴露 Actuator 健康检查端点：`/actuator/health` 路径对外暴露。 | 移除外暴露 |

### 4.3 Nginx 配置

#### High

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| D-17 | High | `deploy/nginx/` 全目录 | - | 全局缺失速率限制：全目录无 `limit_req` 或 `limit_conn` 指令。 | 添加 `limit_req_zone` 与 `limit_req` |
| D-18 | High | `deploy/nginx/default.conf` | - | 开发用 `default.conf` 未包含安全头：无安全头无 SSL 无 HTTPS 跳转，`security-headers.conf` 未被引用。 | 引用 `security-headers.conf` |

#### Medium

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| D-19 | Medium | `deploy/nginx/cdn-optimization.conf` | 126 | CORS 配置缺失或过于宽松：`Access-Control-Allow-Origin "*"` 通配符。 | 改为白名单域名 |
| D-20 | Medium | `deploy/nginx/frontend.conf` | 40 | CSP 策略宽松：CSP 含 `'unsafe-inline' 'unsafe-eval'`。 | 收紧 CSP 策略 |
| D-21 | Medium | `deploy/nginx/frontend.conf` | 90-91 | 管理后台 IP 限制被注释：`/admin/` 的 `allow`/`deny` 被注释掉。 | 取消注释 |
| D-22 | Medium | `deploy/nginx/https.conf` | 45,49 | SSL 私钥文件可能缺失：引用 `server.key` 和 `ec.key` 但 `ssl/` 目录下仅有 `.crt` 和 `.csr`。 | 确认私钥文件存在 |

### 4.4 监控配置

#### High

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| D-23 | High | `deploy/prometheus/prometheus.yml` | 16 | Docker Compose 环境 Alertmanager 未连接：`targets: []` 为空。 | 配置实际 Alertmanager 地址 |
| D-24 | High | `deploy/prometheus/prometheus.yml` | 64-77 | Docker Compose 环境仅抓取 3 个服务：仅抓取 core-gateway、lite-gateway、user-service。 | 补充 order/payment/ai/merchant/product 抓取配置 |

#### Medium

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| D-25 | Medium | `deploy/docker-compose.prod.yml` | 61 | Prometheus 数据保留期偏短：`retention.time=15d`。 | 改为 30d+ |
| D-26 | Medium | `deploy/prometheus/prometheus.yml` | 35 | `node-exporter` 抓取目标错误：`localhost:9100`，容器内 localhost 指向容器自身。 | 改为 `node-exporter:9100` |

### 4.5 CI/CD 工作流

#### High

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| D-27 | High | `.github/workflows/` 全目录 | - | 完全缺失安全扫描工具：无 Trivy/Snyk/CodeQL/gitleaks，前端 CI 显式禁用审计 `npm ci --no-audit`。 | 添加 Trivy/gitleaks/npm audit |
| D-28 | High | `backend-ci.yml`、`frontend-ci.yml` | 85,106,116 / 60,146 | 测试与静态分析均不阻断构建：`continue-on-error: true` 导致测试失败仍可构建。 | 移除 `continue-on-error: true` |

#### Medium

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| D-29 | Medium | `.github/workflows/pr-check.yml` | 54-60 | PR 密钥扫描覆盖面不足：仅用简单 grep 匹配。 | 使用 gitleaks/trufflehog |
| D-30 | Medium | `backend-ci.yml` | 191 | Docker 镜像未推送至镜像仓库：`push: false`。 | 改为推送至镜像仓库 |

### 4.6 数据库/SQL 配置

#### Medium

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| D-31 | Medium | `tailor-is/sql/04_order_system.sql`、`01_user_system.sql` | 39-41,91,125,156,204-205 / 78-79 | 全部外键约束被注释禁用：订单→用户等关联无外键约束，数据一致性依赖应用层。 | 评估并恢复外键 |
| D-32 | Medium | `01_user_system.sql`、`04_order_system.sql` | 17,46,67 / 17,47,97 | 迁移脚本使用 `DROP TABLE IF EXISTS`：初始化脚本含 `DROP TABLE IF EXISTS`，生产环境误执行将丢数据。 | 引入 Flyway/Liquibase，移除危险语句 |

#### Low

| 编号 | 严重级别 | 文件路径 | 行号 | 问题描述 | 修复建议 |
| --- | --- | --- | --- | --- | --- |
| D-33 | Low | 订单表等 | - | 索引覆盖较好（**正面发现**）：订单表有 `idx_user_id`、`idx_status` 等合理索引。 | 无需修复 |
| D-34 | Low | `docs/reports/PROJECT-COMPREHENSIVE-AUDIT-FINAL.md` | - | 已有综合审计记录 158 个问题：已统计 19 Critical/49 High/67 Medium/23 Low。 | 参考已有报告 |

---

## 五、修复优先级建议汇总

### 5.1 P0 - 立即修复（Critical，17 项）

> 影响资金安全、数据安全、用户登录、生产凭证泄露，必须在发布前全部修复。

**后端（11 项）**

| 编号 | 问题摘要 | 影响面 |
| --- | --- | --- |
| B-L-1 | 用户刷新 Token 状态判断反转 | 全部用户无法刷新 Token |
| B-L-2 | 微信登录用户状态校验反转 | 微信用户无法登录 |
| B-L-3 | 新注册用户被创建为禁用状态 | 新用户注册后无法登录 |
| B-L-4 | 新微信用户被创建为禁用状态 | 微信新用户无法登录 |
| B-L-5 | 支付回调 merchantId 错误返回 orderId | 资金入账错误账户 |
| B-L-6 | 版权高风险作品未拦截 | 版权侵权法律风险 |
| B-S-1 | 版权服务 AES 密钥硬编码 | 加密密钥泄露 |
| B-S-2 | 版权服务使用 AES-ECB 模式 | 加密可被破解 |
| B-S-3 | 加密失败时返回明文 | 敏感数据明文落库 |
| B-S-4 | K8s Secret 文件包含占位凭证 | 数据库凭证泄露 |
| B-S-5 | 沙箱测试控制器在生产暴露 | 测试接口无认证暴露 |

**前端（3 项）**

| 编号 | 问题摘要 | 影响面 |
| --- | --- | --- |
| F-S-1 | crypto.ts 使用可逆 XOR 伪装加密 | token 等同明文存储 |
| F-S-2 | platform-admin 完全无加密/CSRF/错误处理 | 管理后台安全防护缺失 |
| F-S-3 | refresh_token 明文存储于 localStorage | token 可被 XSS 窃取 |

**部署（3 项）**

| 编号 | 严重级别 | 问题摘要 | 影响面 |
| --- | --- | --- | --- |
| D-1 | Critical | 生产环境真实密码硬编码 | 数据库/缓存/MQ 凭证泄露 |
| D-2 | Critical | TLS 私钥提交至版本库 | HTTPS 私钥泄露 |
| D-9 | Critical | K8s 监控命名空间不匹配 | 监控完全失效 |

### 5.2 P1 - 短期修复（High，32 项）

> 影响功能正确性、安全性、性能、高可用，建议在下一个迭代周期内修复。

- **后端（14 项）**：B-L-7、B-L-8、B-L-9、B-S-6 ~ B-S-11、B-P-1 ~ B-P-3、B-TX-1、B-TX-2
- **前端（6 项）**：F-S-4 ~ F-S-7、F-T-1、F-T-2
- **部署（12 项）**：D-3 ~ D-5、D-10 ~ D-12、D-17、D-18、D-23、D-24、D-27、D-28

### 5.3 P2 - 中期修复（Medium，45 项）

> 影响代码质量、可维护性、性能优化、配置规范，建议在 2-3 个迭代周期内修复。

- **后端（17 项）**：B-P-4、B-P-5、B-T-1 ~ B-T-3、B-C-1 ~ B-C-5、B-TX-3、B-D-1 ~ B-D-3、B-O-1 ~ B-O-3
- **前端（11 项）**：F-T-3、F-T-4、F-P-1 ~ F-P-4、F-L-1 ~ F-L-5
- **部署（17 项）**：D-6 ~ D-8、D-13 ~ D-16、D-19 ~ D-22、D-25、D-26、D-29、D-30、D-31、D-32

### 5.4 P3 - 长期优化（Low，11 项）

> 代码规范、文档完善、测试补充，可在日常迭代中逐步完善。

- **后端（2 项）**：B-D-4（正面发现）、B-D-5
- **前端（7 项）**：F-P-5、F-L-6、F-C-1 ~ F-C-3、F-D-1、F-D-2
- **部署（2 项）**：D-33（正面发现）、D-34

### 5.5 修复路径建议

1. **第一阶段（1-2 周）**：完成全部 17 项 Critical 修复，重点为登录逻辑反转（B-L-1 ~ B-L-4）、支付资金错误（B-L-5）、凭证泄露（D-1、D-2、B-S-1、B-S-4）、前端 token 安全（F-S-1 ~ F-S-3）。
2. **第二阶段（3-4 周）**：完成 32 项 High 修复，重点为库存释放（B-L-7）、网关签名（B-S-10）、事务边界（B-TX-1）、K8s 安全上下文（D-10 ~ D-12）、CI/CD 安全扫描（D-27、D-28）。
3. **第三阶段（5-8 周）**：完成 45 项 Medium 修复，重点为性能优化（N+1 查询、缓存）、类型定义规范化、Nginx 安全加固。
4. **第四阶段（持续）**：逐步完成 11 项 Low 优化，补充文档与测试。

---

## 六、参考文档

- `docs/reports/PROJECT-COMPREHENSIVE-AUDIT-FINAL.md` - 项目综合审计最终报告（158 个问题统计）
- `docs/reports/PROJECT-AUDIT-REPORT.md` - 项目审计报告
- `docs/reports/ISSUE-TRACKER.md` - 问题跟踪记录
- `docs/reports/SYSTEMATIC-IMPROVEMENT-AND-REMEDIATION-PLAN.md` - 系统化改进与修复方案
- `tailor-is-frontend/docs/FRONTEND-ARCHITECTURE.md` - 前端架构文档

---

*文档结束*
