# Tailor IS 项目问题跟踪表

> 生成日期: 2026-06-11  
> 审计范围: 后端18个微服务 + 前端4个项目 + 基础设施 + 安全 + 文档  
> 总计问题: **86项** (Critical: 14 | High: 18 | Medium: 36 | Low: 18)  
> 最新状态: **86/86 已修复** (100% 修复率)

---

## 一、Critical 级别问题 (14项) — 必须立即修复,阻塞生产上线

| ID | 类型 | 模块 | 位置 | 问题描述 | 影响 | 复现步骤 | 修复建议 | 状态 |
|----|------|------|------|---------|------|---------|---------|------|
| C-001 | 安全漏洞 | 基础设施 | `deploy/.env.production` L1-L40 | **生产环境凭证硬编码**：MySQL/Redis/Nacos/RabbitMQ/MongoDB/ES/Grafana 全部密码及 AES 密钥明文存储 | 任何人获取代码即可访问全部生产基础设施 | 查看 deploy/.env.production 文件 | 1. 立即将该文件加入 .gitignore<br>2. 使用密钥管理服务<br>3. 重置所有已泄露密码 | 🟢 已修复 |
| C-002 | 安全漏洞 | 全部服务 | 所有 `application.yml` | **Fallback 默认密码硬编码**：如 `mysql_ZmY2sr`、`redis_jD2N8n`、`nacos_s3k8Fp` 等 | 攻击者可通过默认凭证访问服务 | 查看任意服务 application.yml 的 fallback 值 | 1. 移除所有默认密码<br>2. 必须通过环境变量提供 | 🟢 已修复 |
| C-003 | 安全漏洞 | gateway | `AuthInterceptor.java` L29 | **认证完全绕过**：`isValidToken()` 方法始终返回 `true`，所有请求无需认证 | 所有 API 接口无鉴权保护 | 1. 不调用登录接口<br>2. 直接访问 /api/order/list<br>3. 请求成功返回数据 | 1. 实现真实 Token 验证逻辑<br>2. 连接 Sa-Token 或 JWT 验证 | 🟢 已修复 |
| C-004 | 安全漏洞 | gateway | `application.yml` L19 | **Nacos 认证未启用**：`auth.enable: false`，配置中心可被任意修改 | 攻击者可修改服务配置、获取服务列表 | 1. 访问 http://localhost:8848/nacos<br>2. 使用默认空凭证登录<br>3. 修改任意配置 | 1. 启用 Nacos 认证 `auth.enable: true`<br>2. 配置强密码 | 🟢 已修复 |
| C-005 | 类型错误 | frontend pc-mall | `pc-mall/src/api/graphql.ts` L80-280 | **文件内容损坏**：`useOrderGraphQL` 函数体中混入了 `autoSync.ts` 的代码 | TypeScript 编译失败，运行时崩溃 | 1. 运行 `npm run build`<br>2. TypeScript 编译报错 | 1. 重新生成正确的 graphql.ts<br>2. 移除混入的 autoSync 代码 | 🟢 已修复 |
| C-006 | 逻辑缺陷 | frontend merchant-admin | `merchant-admin/src/api/request.ts` L83 | **TDZ 变量未定义**：`log.info()` 在第83行调用，但 `log` 定义在第151行 | 请求重试时抛出 ReferenceError 崩溃 | 1. 发起API请求<br>2. 模拟网络失败触发重试<br>3. 重试逻辑执行到 log.info() 时报错 | 将 `log` 对象定义移至使用位置之前 | 🟢 已修复 |
| C-007 | 安全漏洞 | frontend mobile-app | `mobile-app/utils/crypto.ts` L147-154 | **加密失败降级为明文**：`setSecure()` 在 catch 中将 Token 以明文存储 | 安全加密完全失效 | 1. 模拟加密失败(修改密钥)<br>2. 调用 setSecure('token', 'xxx')<br>3. 读取存储为明文 | 加密失败时拒绝存储或抛出错误 | 🟢 已修复 |
| C-008 | 安全漏洞 | frontend 全部 | `pc-mall/src/api/request.ts` L30-38, `merchant-admin/src/api/request.ts` L24-33 | **CSRF Token 不安全**：使用 `Math.random()` 生成，存储在 localStorage | XSS 攻击可读取/伪造 CSRF Token | 1. 在浏览器控制台执行 `localStorage.getItem('csrf_token')`<br>2. 可直接篡改 | 1. 服务端生成 CSRF Token<br>2. 使用 HttpOnly Cookie 传输 | 🟢 已修复 |
| C-009 | 安全漏洞 | frontend pc-mall | `pc-mall/src/views/ProductDetailView.vue` L87 | **XSS 漏洞**：`v-html="product.description"` 未做清理直接渲染 | 恶意描述内容可执行任意 JavaScript | 1. 创建商品，描述包含 `<script>alert(1)</script>`<br>2. 访问商品详情页<br>3. 脚本执行 | 使用 DOMPurify 清理：`v-html="DOMPurify.sanitize(product.description)"` | 🟢 已修复 |
| C-010 | 逻辑缺陷 | frontend mobile-app | `mobile-app/pages/order/offline-aftersale.vue` L260 | **核心功能 TODO 未实现**：`createAfterSale()` API 调用未实现 | 离线售后工单保存后无法真正提交到服务器 | 1. 离线创建售后工单<br>2. 保存草稿<br>3. 联网后点击提交<br>4. 提交不执行 | 实现 `createAfterSale` API 调用逻辑 | 🟢 已修复 |
| C-011 | 安全漏洞 | frontend 全部 | `pc-mall/src/store/user.ts` L7, `merchant-admin/src/store/user.ts` L12 | **Token 明文存储 localStorage**：`localStorage.setItem('token', res.token)` | XSS 攻击可直接读取用户 Token | 1. 登录系统<br>2. 浏览器控制台执行 `localStorage.getItem('token')`<br>3. 可直接获取有效 Token | 1. 改用 HttpOnly Cookie<br>2. 或至少加密后存储 | 🟢 已修复 |
| C-012 | 安全漏洞 | frontend pc-mall | `pc-mall/src/router/index.ts` L84-91 | **权限信息可篡改**：从 localStorage 解析 user_info 中的 roles/permissions | 用户可修改 localStorage 中的角色信息绕过权限 | 1. 登录系统<br>2. 控制台修改 `localStorage.setItem('user_info', JSON.stringify({roles:['admin']}))`<br>3. 刷新页面获得管理员权限 | 角色/权限从服务端验证，或添加签名校验 | 🟢 已修复 |
| C-013 | 架构缺陷 | backend common | `SnowflakeIdGenerator.java` | **单例与 Spring Bean 冲突**：SnowflakeIdGenerator 使用静态单例模式，但作为 Spring Bean 注册，可能导致 ID 生成冲突 | 分布式环境下可能产生重复 ID | 1. 启动多个服务实例<br>2. 并发创建订单<br>3. 可能出现 ID 冲突 | 1. 使用 Spring 管理的 Bean<br>2. 或改用雪花算法库(如 MyBatis-Plus 内置) | 🟢 已修复 |
| C-014 | 安全漏洞 | frontend mobile-app | `mobile-app/utils/crypto.ts` L66-77 | **XOR 加密不是真正的加密**：使用 XOR + 本地存储的密钥 | 攻击者获取设备后可轻易解密所有数据 | 1. 获取设备存储文件<br>2. 提取密钥<br>3. XOR 解密所有数据 | 使用 UniApp 原生加密存储 API 或 AES 加密 | 🟢 已修复 |

---

## 二、High 级别问题 (18项) — 应在上线前修复

| ID | 类型 | 模块 | 位置 | 问题描述 | 影响 | 修复建议 | 状态 |
|----|------|------|------|---------|------|---------|------|
| H-001 | 安全漏洞 | gateway | `application.yml` L46-50 | **CORS 通配符 + 凭据组合漏洞**：`allowed-origin-patterns: "*"` + `allow-credentials: true` | 恶意网站可携带凭证发起跨域请求 | 限制 allowed-origin-patterns 为具体域名 | 🟢 已修复 |
| H-002 | 安全漏洞 | 全部服务 | pom.xml + application.yml | **Swagger 白名单暴露**：Knife4j 文档对所有 IP 开放 | API 接口文档暴露给攻击者 | 生产环境禁用 Swagger 或添加 IP 白名单 | 🟢 已修复 |
| H-003 | 安全漏洞 | common | `RedisClusterConfig.java`, `RedisStandaloneConfig.java` | **Redis 反序列化风险**：`GenericJackson2JsonRedisSerializer` 无类型白名单 | 恶意序列化数据可导致 RCE | 添加类型白名单或使用安全的序列化器 | 🟢 已修复 |
| H-004 | 安全漏洞 | user | `SmsService.java` | **短信验证码明文写入日志**：`log.info("验证码: {}", code)` | 日志泄露导致验证码被盗 | 移除验证码日志输出或使用脱敏 | 🟢 已修复 |
| H-005 | 安全漏洞 | gateway | `CsrfFilter.java` | **CSRF Filter 登录验证形同虚设**：验证逻辑不完整 | CSRF 防护未真正生效 | 完善 CSRF 验证逻辑 | 🟢 已修复 |
| H-006 | 类型错误 | frontend pc-mall | `pc-mall/src/types/index.ts` L179-183 | **ApiResponse data 字段缺少 null 处理**：`data: T` 应为 `data?: T \| null` | 错误响应时 data 为 null 导致类型错误 | 修改为 `data?: T \| null` | 🟢 已修复 |
| H-007 | 逻辑缺陷 | frontend pc-mall | `pc-mall/src/api/request.ts` L167-188 | **401/403/500 错误静默吞掉**：switch case 中全部使用 `break` 不提示不返回错误 | 请求失败时用户无感知，业务逻辑继续执行 | 添加错误提示并 Promise.reject | 🟢 已修复 |
| H-008 | 类型错误 | frontend pc-mall | `pc-mall/src/types/index.ts` L1-13 | **User 接口所有字段 required**：avatar/birthday/email 等可能为 null | API 返回 null 时类型不匹配 | 将可选字段标记为 `?` | 🟢 已修复 |
| H-009 | 逻辑缺陷 | backend order | `OrderServiceImpl.java` | **订单创建缺少幂等性锁**：重复提交可能创建多个订单 | 用户快速点击创建重复订单 | 添加分布式锁 `orderIdempotent:{userId}:{productId}` | 🟢 已修复 |
| H-010 | 逻辑缺陷 | backend marketing | `CouponServiceImpl.java` | **优惠券领取限制存在竞态条件**：检查与领取非原子操作 | 用户并发领取可能突破限制 | 使用 Redis Lua 脚本或数据库唯一约束 | 🟢 已修复 |
| H-011 | 架构缺陷 | backend common | 配置 | **平台费率硬编码**：`PLATFORM_FEE_RATE = 0.05` 硬编码在代码中 | 修改费率需要重新部署 | 提取到配置中心(Nacos)动态管理 | 🟢 已修复 |
| H-012 | 逻辑缺陷 | frontend mobile-app | `mobile-app/utils/autoSync.ts` L277 | **Math.min 在空数组上调用**：`Math.min(...remainingQueue.map(...))` 返回 Infinity | 同步调度器异常 | 添加空数组检查 | 🟢 已修复 |
| H-013 | 安全漏洞 | frontend pc-mall | `pc-mall/src/views/CheckoutView.vue` L191-225 | **URL 参数未校验**：`Number(route.query.productId)` 未做 NaN 检查 | 恶意 URL 可导致类型转换异常 | 添加参数校验和 NaN 检查 | 🟢 已修复 |
| H-014 | 性能瓶颈 | frontend pc-mall | `pc-mall/src/views/HomeView.vue` L206-213 | **onMounted 并行5个 API 请求无优先级**：同时发起所有请求 | 首屏加载性能受影响 | 区分关键/非关键数据，非关键数据延迟加载 | 🟢 已修复 |
| H-015 | 兼容性问题 | frontend mobile-app | `mobile-app/utils/offlineStorage.ts` L26-44 | **小程序 localStorage 10MB 限制**：大量离线数据可能写入失败 | 小程序端离线功能不可用 | 使用数据分片或 uni.setStorage | 🟢 已修复 |
| H-016 | 部署风险 | 基础设施 | 根目录 | **缺少根目录 docker-compose.yml**：README 引用但文件不存在 | 按文档无法一键启动项目 | 创建根目录 docker-compose.yml | 🟢 已修复 |
| H-017 | 部署风险 | 基础设施 | 根目录 | **缺少 .env.example**：无环境变量模板 | 部署时无参考配置 | 创建 .env.example 包含所有必要变量 | 🟢 已修复 |
| H-018 | 逻辑缺陷 | frontend platform-admin | `platform-admin/src/views/dashboard/DashboardView.vue` L138-169 | **Dashboard 使用假数据**：统计数字硬编码，无 API 调用 | 管理后台数据不真实 | 实现 API 数据获取 | 🟢 已修复 |

---

## 三、Medium 级别问题 (36项)

| ID | 类型 | 模块 | 问题描述 | 修复建议 | 状态 |
|----|------|------|---------|---------|------|
| M-001 | 性能瓶颈 | order/product | **N+1 查询**：部分关联查询未使用 JOIN 或批量查询 | 使用 MyBatis-Plus @TableField 或批量查询 | 🟢 已修复 |
| M-002 | 安全漏洞 | 全部服务 | **生产环境 Swagger 未禁用**：knife4j 在 prod profile 中未关闭 | 添加 `knife4j.production=true` | 🟢 已修复 |
| M-003 | 代码质量 | common | **BCryptPasswordEncoder 重复定义**：多处定义相同 Bean | 统一到 common-security 模块 | 🟢 已修复 |
| M-004 | 代码质量 | common | **MD5 工具方法使用**：部分场景仍使用 MD5（如密码哈希） | 迁移到 BCrypt 或 Argon2 | 🟢 已修复 |
| M-005 | 代码质量 | frontend pc-mall | **ProductDetailView price * 1.2 魔法数字** | 提取为 `ORIGINAL_PRICE_MULTIPLIER` 常量 | 🟢 已修复 |
| M-006 | 代码质量 | frontend 全部 | **useCountdown composable 在3个项目复制** | 提取到 shared 包 | 🟢 已修复 |
| M-007 | 代码质量 | frontend 全部 | **storage.ts / validate.ts 在3个项目复制** | 提取到 shared 包 | 🟢 已修复 |
| M-008 | 代码质量 | frontend pc-mall | **HomeView.vue 365行 / CheckoutView.vue 442行** | 拆分为子组件 | 🟢 已修复 |
| M-009 | UI/UX | frontend pc-mall | **ProductDetailView 固定宽度 480px 小屏幕溢出** | 添加媒体查询响应式 | 🟢 已修复 |
| M-010 | UI/UX | frontend 全部 | **大量硬编码中文文本**：未使用 i18n `$t()` 函数 | 系统性迁移到 locale 文件 | 🟢 已修复 |
| M-011 | UI/UX | frontend pc-mall | **ProductDetailView/CheckoutView 缺少 error 状态** | 添加错误状态展示 | 🟢 已修复 |
| M-012 | UI/UX | frontend pc-mall | **CheckoutView 3列 grid 移动端太窄** | 移动端改为 1-2 列 | 🟢 已修复 |
| M-013 | 性能瓶颈 | frontend pc-mall | **ProductCard 每个实例注册 scroll/resize 监听器** | 使用 IntersectionObserver | 🟢 已修复 |
| M-014 | 代码质量 | frontend pc-mall | **ProductDetailView 使用 `!` 非空断言** | 使用可选链或提前 return | 🟢 已修复 |
| M-015 | 代码质量 | frontend pc-mall | **skuAttributes computed 中重复属性解析逻辑** | 提取为 normalizeSkuAttributes() | 🟢 已修复 |
| M-016 | 代码质量 | frontend merchant-admin | **PageResponse 与 pc-mall 分页字段名不一致** | 统一分页接口 | 🟢 已修复 |
| M-017 | 代码质量 | frontend merchant-admin | **fetchShopList 使用硬编码假数据** | 调用实际 API | 🟢 已修复 |
| M-018 | 功能缺失 | frontend pc-mall | **ProductDetailView 评价功能为空**：`<el-empty>` 占位 | 实现评价加载和展示 | 🟢 已修复 |
| M-019 | 功能缺失 | frontend merchant-admin | **FinanceWithdraw 路由复用 FinanceSettlementView** | 创建独立的提现页面 | 🟢 已修复 |
| M-020 | 依赖问题 | frontend pc-mall | **`@rollup/rollup-linux-x64-gnu` 不应在 dependencies 中** | 移除或移到 optionalDependencies | 🟢 已修复 |
| M-021 | 文档缺失 | 根目录 | **缺少 README.md** | 创建项目入口 README | 🟢 已修复 |
| M-022 | 文档缺失 | 根目录 | **缺少 LICENSE 文件** | 添加开源许可证 | 🟢 已修复 |
| M-023 | 文档缺失 | 根目录 | **缺少 CHANGELOG** | 添加版本变更日志 | 🟢 已修复 |
| M-024 | 文档缺失 | 根目录 | **缺少 CONTRIBUTING.md** | 创建贡献指南 | 🟢 已修复 |
| M-025 | 文档缺失 | tailor-is/docs | **README 引用的文档不存在**：DEPLOYMENT-GUIDE.md, SEATA-SETUP.md, SONARQUBE-GUIDE.md | 创建缺失文档或更新链接 | 🟢 已修复 |
| M-026 | 文档缺失 | 运维 | **缺少备份/恢复/灾难恢复独立文档** | 创建运维手册 | 🟢 已修复 |
| M-027 | 文档缺失 | 运维 | **1Panel 部署文档分散**：计划/检查单/风险评估缺少集中指南 | 创建 1Panel 一键部署指南 | 🟢 已修复 |
| M-028 | 部署风险 | K8s | **缺少健康检查配置**：Deployment 中无 liveness/readiness probe 端口验证 | 添加 Spring Boot Actuator 健康检查 | 🟢 已修复 |
| M-029 | 部署风险 | Docker | **缺少资源限制**：docker-compose 中无 mem_limit/cpu_limit | 添加资源限制配置 | 🟢 已修复 |
| M-030 | 部署风险 | Nginx | **缺少 SSL/TLS 配置** | 添加 HTTPS 配置 | 🟢 已修复 |
| M-031 | 部署风险 | 数据库 | **缺少数据库备份方案** | 创建备份脚本和恢复流程 | 🟢 已修复 |
| M-032 | 安全漏洞 | frontend 全部 | **无障碍性缺失**：shared a11y 组件未被各项目使用 | 集成无障碍组件 | 🟢 已修复 |
| M-033 | 兼容性问题 | frontend mobile-app | **navigator.connection 实验性 API** Safari 不可用 | 添加降级逻辑 | 🟢 已修复 |
| M-034 | 兼容性问题 | frontend mobile-app | **globalThis.b/atob 小程序可能不支持** | 使用 uni 的 base64 工具 | 🟢 已修复 |
| M-035 | 性能瓶颈 | 数据库 | **缺少索引优化文档** | 根据慢查询日志添加索引 | 🟢 已修复 |
| M-036 | 架构缺陷 | gateway | **旧 gateway 模块未移除**：core-gateway/lite-gateway 已创建但旧 gateway 仍存在 | 移除旧模块或标记 deprecated | 🟢 已修复 |

---

## 四、Low 级别问题 (18项)

| ID | 类型 | 模块 | 问题描述 | 修复建议 | 状态 |
|----|------|------|---------|---------|------|
| L-001 | 代码质量 | frontend mobile-app | `targetTime` 变量未初始化 | 初始化为 `new Date()` | 🟢 已修复 |
| L-002 | 代码质量 | frontend 全部 | Vite 版本较旧 `vite@^5.1.0` | 升级到 Vite 6 | 🟢 已修复 |
| L-003 | 代码质量 | 全部服务 | 部分方法缺少 Javadoc | 补充文档注释 | 🟢 已修复 |
| L-004 | 代码质量 | 全部服务 | 少量未使用的 import | 清理无用导入 | 🟢 已修复 |
| L-005 | 代码质量 | 全部服务 | 部分 catch 块空实现 | 添加日志记录 | 🟢 已修复 |
| L-006 | 代码质量 | frontend pc-mall | ProductDetailView 中 `max="99"` 硬编码 | 提取为常量 | 🟢 已修复 |
| L-007 | UI/UX | frontend 全部 | 部分按钮缺少 hover 状态 | 补充 hover 样式 | 🟢 已修复 |
| L-008 | UI/UX | frontend 全部 | 部分表格缺少排序功能 | 添加列排序 | 🟢 已修复 |
| L-009 | UI/UX | frontend mobile-app | 部分页面缺少骨架屏 | 添加骨架屏组件 | 🟢 已修复 |
| L-010 | 文档缺失 | 各模块 | 各微服务模块缺少独立 README | 为每个模块添加 README | 🟢 已修复 |
| L-011 | 部署风险 | 日志 | 日志级别未统一配置 | 使用 logback-spring.xml 统一管理 | 🟢 已修复 |
| L-012 | 部署风险 | Docker | Docker 镜像未优化多阶段构建 | 使用多阶段构建减小镜像体积 | 🟢 已修复 |
| L-013 | 性能瓶颈 | 缓存 | 部分缓存缺少 TTL 配置 | 统一配置缓存过期时间 | 🟢 已修复 |
| L-014 | 安全漏洞 | 日志 | 部分业务日志包含用户敏感信息 | 日志脱敏处理 | 🟢 已修复 |
| L-015 | 代码质量 | 全部服务 | 部分常量未提取到配置类 | 提取到 @ConfigurationProperties | 🟢 已修复 |
| L-016 | 代码质量 | frontend 全部 | 部分组件缺少 props 类型校验 | 使用 defineProps<{...}> 泛型语法 | 🟢 已修复 |
| L-017 | 性能瓶颈 | 前端 | 部分图片未使用懒加载 | 添加 v-lazy 或 loading="lazy" | 🟢 已修复 |
| L-018 | 文档缺失 | API | 部分接口缺少 @Operation 注解 | 补充 Swagger 注解 | 🟢 已修复 |

---

## 五、问题修复优先级建议

### 第一批修复（阻塞上线，1-2周）
- **C-001 ~ C-014** 全部 Critical 问题
- **H-001 ~ H-005** 安全类 High 问题

### 第二批修复（上线前必须，2-3周）
- **H-006 ~ H-018** 其余 High 问题
- **M-001 ~ M-004, M-028 ~ M-031** 性能/部署 Medium 问题

### 第三批修复（持续改进，3-4周）
- **M-005 ~ M-020** 代码质量 Medium 问题
- **M-021 ~ M-027, M-032 ~ M-036** 文档/UI Medium 问题
- **L-001 ~ L-018** 全部 Low 问题

---

## 六、修复验证标准

| 问题类型 | 验证方法 | 通过标准 |
|---------|---------|---------|
| 安全漏洞 | 渗透测试 + 自动化扫描 | OWASP ZAP 扫描 0 高危 |
| 类型错误 | TypeScript 编译 | `tsc --noEmit` 零错误 |
| 逻辑缺陷 | 单元测试 + 集成测试 | 相关测试用例 100% 通过 |
| 性能瓶颈 | JMeter 压测 | P95 ≤ 200ms |
| UI/UX 问题 | 多浏览器/多尺寸测试 | 320px~2560px 无溢出 |
| 文档缺失 | 文档审查 | 所有引用链接可访问 |
| 部署风险 | 1Panel 部署验证 | 一键部署成功，健康检查通过 |
