# Tailor IS 代码质量改进与问题修复综合方案

> 版本: v1.0 | 日期: 2026-06-19 | 问题总数: 107 (Critical:26 / High:34 / Medium:34 / Low:13)

---

## 一、分阶段实施策略

### 阶段一: Critical 级紧急修复 (P0)

**目标**: 消除所有阻断核心业务、安全漏洞、系统不可用问题

#### 1.1 支付安全修复 (最高优先级)

| 问题ID | 问题描述 | 修复方案 | 涉及文件 |
|--------|---------|---------|---------|
| BE-C-2 | 支付回调无签名验证 | 在 payCallback 处理前调用 alipayService.verifyCallback(params) 验签 | tailor-is-payment/.../PaymentController.java |
| BE-C-3 | merchantId 可被篡改 | merchantId 从 PaymentRecord 中获取，不信任客户端传入 | tailor-is-payment/.../PaymentServiceImpl.java |
| BE-H-11 | 担保账户竞态条件 | 改用 `UPDATE ... SET balance = balance + ? WHERE id = ?` 原子操作 | tailor-is-payment/.../EscrowServiceImpl.java |
| BE-H-12 | 商家结算竞态条件 | 同上，使用原子UPDATE | tailor-is-payment/.../SettlementServiceImpl.java |

#### 1.2 认证授权修复

| 问题ID | 问题描述 | 修复方案 | 涉及文件 |
|--------|---------|---------|---------|
| BE-C-5 | 用户状态判断反转 | `if (user.getStatus() != USER_STATUS_NORMAL)` | tailor-is-user/.../SysUserServiceImpl.java:378 |
| BE-C-4 | 社区置顶/加精无权限校验 | 添加 `@SaCheckRole("admin")` 或 `@SaCheckPermission` | tailor-is-community/.../PostController.java:83-94 |
| BE-C-6 | 订单详情IDOR越权 | 增加 userId 参数并校验 `order.getUserId().equals(userId)` | tailor-is-order/.../OrderServiceImpl.java:416-422 |
| BE-C-7 | AI版型越权访问 | 所有方法增加 userId 参数并校验归属 | tailor-is-ai/.../PatternServiceImpl.java |
| BE-H-18 | 下游服务信任 X-User-Id | 网关删除客户端传入的 X-User-Id 后再设置；或下游通过Sa-Token校验 | 多个Controller + 网关过滤器 |

#### 1.3 前端阻塞性修复

| 问题ID | 问题描述 | 修复方案 | 涉及文件 |
|--------|---------|---------|---------|
| FE-C-1 | cancelOrder 无限递归 | 重命名为 handleCancelOrder | mobile-app/pages/order/list.vue:114 |
| FE-C-2 | 登录后未保存Token | 登录成功后 `localStorage.setItem('token', res.token)` | platform-admin/.../LoginView.vue:262-272 |
| FE-C-3 | 订单确认页硬编码商品 | 根据 cartIds/productId 调用商品API获取真实数据 | mobile-app/pages/order/confirm.vue:95-118 |
| FE-C-4 | rich-text XSS | 引入 DOMPurify 过滤 product.content | mobile-app/pages/product/detail.vue:31 |
| FE-C-5 | crypto.ts 加密失效 | 修复 encrypt() 使用 AES-GCM 或删除误导分支 | mobile-app/utils/crypto.ts:266-273 |

#### 1.4 部署配置修复

| 问题ID | 问题描述 | 修复方案 | 涉及文件 |
|--------|---------|---------|---------|
| DEP-C-1 | frontend.conf 路由不匹配 | 移除 /api/core/ 和 /api/lite/ 前缀，直接按服务路径分流 | deploy/nginx/frontend.conf |
| DEP-C-2 | gateway-application.yml 遗留 | 删除该文件 | deploy/gateway-application.yml |
| DEP-C-3 | prod.yml 使用 127.0.0.1 | 改为 host.docker.internal 或服务名 | deploy/docker-compose.prod.yml |
| DEP-C-4 | SSL私钥缺失 | 生成 server.key、ec.key、dhparam.pem | deploy/nginx/ssl/ |
| DEP-C-5 | Prometheus标签错位 | 重新对齐所有服务标签与端口 | deploy/prometheus.yml |
| DEP-C-6 | 数据库初始化不匹配 | 统一SQL脚本与compose配置 | deploy/sql/ + docker-compose*.yml |
| DEP-C-7 | k8s ingress rewrite-target | 移除 rewrite-target 注解 | tailor-is/deploy/k8s/ingress.yaml |
| DEP-C-8 | 硬编码生产凭据 | 移除硬编码，使用环境变量 | deploy/gateway-application.yml:158-160 |

### 阶段二: High 级修复 (P1)

**目标**: 修复重要功能异常、性能问题、安全加固

#### 2.1 并发安全修复

| 问题ID | 修复项 | 方案 |
|--------|--------|------|
| BE-H-13 | 秒杀DB库存非原子 | `UPDATE ... SET available_stock = available_stock - 1 WHERE id = ? AND available_stock > 0` |
| BE-M-27 | 社区计数非原子 | `UPDATE ... SET count = count + 1 WHERE id = ?` |

#### 2.2 安全加固

| 问题ID | 修复项 | 方案 |
|--------|--------|------|
| BE-C-1 | 硬编码默认密码 | 移除常量，改为随机生成 |
| BE-H-14 | AES-CBC不安全 | 迁移到 AesGcmCrypto，标记旧类 @Deprecated |
| BE-H-16 | XSS过滤不完整 | 非JSON请求也包装为 XssRequestWrapper |
| BE-H-17 | Actuator暴露 | 移出白名单，仅暴露 /actuator/health |
| BE-M-25 | fastjson 1.x RCE | 迁移到 fastjson2 或 Jackson |
| BE-M-29 | Math.random 验证码 | 改用 SecureRandom |
| BE-M-35 | CSRF日志泄露Token | 对 storedToken 做掩码处理 |
| FE-H-1 | platform-admin Token明文 | 接入 AES-GCM 加密存储 |
| FE-H-2 | mobile-app Token明文 | 同上 |
| FE-H-3 | GraphQL Token不一致 | 使用 decryptSync() 解密后使用 |
| FE-H-5 | mock-server硬编码密码 | 移除或改为环境变量 |

#### 2.3 性能优化

| 问题ID | 修复项 | 方案 |
|--------|--------|------|
| BE-H-9 | MultiLevelCache配置失效 | 使用 @PostConstruct 初始化 |
| BE-H-10 | Redis KEYS命令 | 改用 SCAN |
| BE-H-24 | AlipayClient重复创建 | @PostConstruct 单例化 |
| BE-M-41 | 商品缓存过度清理 | 仅清理目标商品缓存 |

#### 2.4 前端功能修复

| 问题ID | 修复项 | 方案 |
|--------|--------|------|
| FE-H-4 | 备注假保存 | 接入真实API |
| FE-H-6 | mobile-app 缺 lang="ts" | 全量补充 |
| FE-H-7 | HTTP明文协议 | 生产环境强制HTTPS |
| FE-M-6 | SettingsView假保存 | 接入真实API |

### 阶段三: Medium/Low 级整改 (P2/P3)

**目标**: 代码规范、文档补全、体验优化

- 修复所有逻辑缺陷(缓存失效、重试空转、权限校验错误等)
- 消除魔法数字，使用枚举/常量
- 修复 Integer 拆箱 NPE 风险
- 清理死代码和 @Deprecated 方法
- 统一 console 移除配置
- 替换 placeholder 图片为本地资源
- 补充 alt 属性和错误边界
- 移除 eslint-disable 和 as any

---

## 二、优先级排序

```
P0 (立即修复):
  ├── 支付回调验签 + merchantId 修复 (资金安全)
  ├── 用户状态判断反转修复 (用户无法使用)
  ├── 前端递归崩溃 + Token未保存 (系统不可用)
  ├── Nginx路由 + SSL修复 (部署不可用)
  └── Prometheus标签修复 (监控失效)

P1 (24小时内修复):
  ├── 并发竞态条件修复 (数据一致性)
  ├── IDOR越权修复 (数据泄露)
  ├── Actuator/X-User-Id 加固 (安全)
  └── 前端Token加密 + 假保存修复

P2 (3个工作日内修复):
  ├── 缓存/性能优化
  ├── 逻辑缺陷修复
  └── TypeScript类型强化

P3 (迭代周期内):
  ├── 代码规范整改
  ├── 文档补全
  └── UI/UX优化
```

---

## 三、代码审查机制

### 3.1 审查流程

1. **提交前自检**: 开发者运行 `mvn checkstyle:check pmd:check` + `npm run lint`
2. **PR 提交**: 所有修复必须通过 PR，禁止直接 push main
3. **Code Review**: 至少1名审查者批准，Critical/High 修复需2名审查者
4. **CI 验证**: PR 必须通过所有 CI 检查后方可合并
5. **回归测试**: Critical/High 修复需附带测试用例

### 3.2 审查重点

- 安全: 输入校验、权限控制、敏感数据处理
- 并发: 共享状态修改、锁使用
- 性能: N+1查询、KEYS命令、不必要的对象创建
- 规范: 命名、注释、魔法数字
