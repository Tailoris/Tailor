# Tailor-IS（裁智云）需求规格文档 SPEC.md

> **版本**: v1.1  
> **创建日期**: 2026-07-21  
> **最后更新**: 2026-07-21  
> **状态**: 已对齐（Aligned）  
> **审计来源**: VibeCoding Phase 1 — Align

---

## 1. 项目概述

### 1.1 产品定位

裁智云（Tailor-IS）是面向全球服装定制产业的数字化服务平台，提供版型定制、纸样设计、面料供应链、版权存证、AI 智能推荐、3D 预览等一站式服务。

### 1.2 核心用户角色

| 角色 | 标识 | 权限级别 |
|------|------|---------|
| 超级管理员 | SUPER_ADMIN | 全平台管理 |
| 平台管理员 | PLATFORM_ADMIN | 平台运营管理 |
| 版师 | PATTERN_MAKER | 纸样设计、订单处理 |
| 企业用户 | ENTERPRISE_USER | 批量下单、数据分析 |
| 普通用户 | USER | 浏览、下单、社区互动 |
| 商户 | MERCHANT | 商品管理、订单管理 |

### 1.3 多语言支持

| 语言 | 代码 | 前端覆盖 |
|------|------|---------|
| 简体中文 | zh-CN | pc-mall, platform-admin, merchant-admin |
| 英语 | en-US | pc-mall, platform-admin, merchant-admin |
| 西班牙语 | es-ES | pc-mall |
| 法语 | fr-FR | pc-mall |
| 日语 | ja-JP | pc-mall |
| 韩语 | ko-KR | pc-mall |

---

## 2. 功能范围

### 2.1 核心业务模块（P0 — 必须完整实现）

| 模块 | 功能 | 关键接口 | 数据库 |
|------|------|---------|--------|
| **用户系统** | 注册/登录/认证/RBAC | `/api/v1/auth/*`, `/api/v1/user/*` | tailor_is_user |
| **商品管理** | 商品 CRUD/分类/搜索/评价 | `/api/v1/product/*`, `/api/v1/products/*` | tailor_is_product |
| **订单交易** | 下单/支付/物流/售后 | `/api/v1/order/*`, `/api/v1/payment/*` | tailor_is_order, tailor_is_payment |
| **纸样定制** | 需求审核→初版→修改→交付→售后 | `/api/v1/pattern/*` | tailor_is_pattern |
| **设计管理** | 设计文件上传/格式转换/3D预览 | `/api/v1/design/*` | tailor_is_design |
| **版权存证** | 区块链存证/AI侵权检测 | `/api/v1/copyright/*` | tailor_is_copyright |
| **AI 算法** | 尺寸推荐/版型匹配/智能评分 | `/api/v1/ai/*` | tailor_is_ai |
| **管理后台** | 用户/订单/商品/商户/系统管理 | `/api/v1/admin/*` | tailor_is_admin |

### 2.2 轻量业务模块（P1 — 核心功能完整）

| 模块 | 功能 | 关键接口 |
|------|------|---------|
| 社区 | 发帖/评论/点赞/关注 | `/api/v1/community/*` |
| 面辅料供应链 | 面料采购/供应商管理 | `/api/v1/supply/*`, `/api/v1/fabric/*` |
| 商户管理 | 入驻审核/资质管理/店铺管理 | `/api/v1/merchant/*` |
| 消息通知 | 站内信/推送/邮件 | `/api/v1/message/*` |
| 即时通讯 | 版师-客户实时沟通 | `/api/v1/im/*` |
| 学堂 | 课程/教程/认证 | `/api/v1/academy/*` |
| 数据分析 | 免费月报/付费季报/企业定制报告 | `/api/v1/analytics/*` |
| 知识库 | 版型知识/工艺文档 | `/api/v1/knowledge/*` |
| 营销 | 优惠券/积分/秒杀 | `/api/v1/marketing/*` |

### 2.3 业务规则（强制实现）

| 规则 | 常量位置 | 说明 |
|------|---------|------|
| 15天售后时效 | `OrderConstants.AFTER_SALE_DEADLINE_DAYS` | 超过15天不可申请售后 |
| 33-33-34分期付款 | `OrderConstants.INSTALLMENT_RATIO_*` | 三阶段分期比例 |
| 加急订单1.5倍计费 | `OrderConstants.URGENT_ORDER_PRICE_MULTIPLIER` | 加急处理24小时 |
| 纸样三大评分维度 | `PatternConstants.RATING_DIMENSION_*` | 版型精准度40%+工艺清晰度35%+适配性25% |
| 纸样定制五阶段流程 | `PatternConstants.PATTERN_STATUS_*` | 审核→初版→修改→交付→售后 |
| AES-256文件加密 | `PatternConstants.FILE_ENCRYPTION_ALGORITHM` | 纸样/设计文件端到端加密 |
| 三級权限隔离 | RBAC + Sa-Token | 管理员/版师/企业用户 |

---

## 3. 输入输出规范

### 3.1 统一 API 返回体

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "error": null
}
```

| 场景 | HTTP Code | code | message |
|------|-----------|------|---------|
| 成功 | 200 | 200 | "success" |
| 未登录/Token无效 | 401 | 401 | "请先登录" |
| 角色权限不足 | 403 | 403 | "无此角色权限" |
| 操作权限不足 | 403 | 403 | "无此操作权限" |
| 业务异常 | 400 | 400 | 具体业务消息 |
| 账号密码错误 | 400 | 400 | "账号或密码不正确" |
| 参数校验失败 | 400 | 400 | 具体校验消息 |
| 数据源连接失败 | 503 | 503 | "服务暂时不可用" |
| 系统内部错误 | 500 | 500 | "系统内部错误" |

### 3.2 请求格式

- Content-Type: `application/json;charset=UTF-8`
- 所有 DTO 必须使用 `@Valid` 校验
- 分页请求统一使用 `PageRequest(page, size)`
- 分页响应统一使用 `PageResponse(total, pages, current, size, records)`

### 3.3 认证方式

- Header: `Authorization: Bearer {token}`
- Token 有效期: 1800秒（30分钟）
- 活跃超时: 7200秒（2小时）
- CSRF Token: `X-CSRF-Token` 头
- 网关透传: `X-User-Id` 头

---

## 4. UI 风格与交互标准

### 4.1 前端技术栈

| 端 | 框架 | UI库 | 状态管理 | 构建工具 |
|----|------|------|---------|---------|
| PC商城 | Vue 3 + TS | Element Plus | Pinia | Vite |
| 平台管理 | Vue 3 + TS | Element Plus | Pinia | Vite |
| 商户管理 | Vue 3 + TS | Element Plus | Pinia | Vite |
| 移动端 | uni-app | - | - | - |

### 4.2 交互标准

- 所有 API 请求必须经过网关（端口 9000），禁止直连微服务
- 错误提示互斥：同一错误 3 秒内不重复弹窗
- 401 自动跳转登录页，弹窗提示「登录已过期，请重新登录」
- 503 弹窗提示「服务暂时不可用，请检查网络或稍后重试」
- 账号错误弹窗提示「账号或密码不正确」
- 加载状态：超过 500ms 显示 loading，超过 15s 显示超时提示

---

## 5. 性能标准

| 指标 | 目标 | 测量方式 |
|------|------|---------|
| API 响应时间（P95） | < 500ms | 网关日志 |
| 页面首屏加载 | < 2s | Lighthouse |
| 并发用户数 | 1000+ | k6 压测 |
| 数据库连接池 | 20 最大连接 | Druid 监控 |
| Redis 缓存命中率 | > 80% | Redis INFO |
| 文件上传大小 | < 100MB | 前端 + 后端双重校验 |

---

## 6. 禁止行为

### 6.1 架构禁止

- ❌ 禁止 `network_mode: host`
- ❌ 禁止 API 绕过网关直连微服务
- ❌ 禁止多套 docker-compose 配置共存
- ❌ 禁止 1Panel 托管运行微服务
- ❌ 禁止容器名使用 `-service` 后缀

### 6.2 代码禁止

- ❌ 禁止硬编码端口/路由/IP/密码/密钥
- ❌ 禁止子模块异常处理器含 `Exception.class` 兜底
- ❌ 禁止 `allowPublicKeyRetrieval=true` 在生产环境
- ❌ 禁止删除/简化异常处理分支
- ❌ 禁止简化版权检测、加密传输、权限校验逻辑
- ❌ 禁止前端使用 `any` 类型
- ❌ 禁止使用 `@ExceptionHandler(Exception.class)` 在子模块
- ❌ 禁止参数使用 `Map` 替代 DTO（无 `@Valid` 校验）

### 6.3 安全禁止

- ❌ 禁止明文存储密钥/密码
- ❌ 禁止日志打印手机号/邮箱/文件哈希
- ❌ 禁止跨层直接调用（Controller → Mapper）
- ❌ 禁止一次性大面积重构无关代码

---

## 7. 安全约束

### 7.1 传输安全

- 文件传输：AES-256-GCM 端到端加密
- 密码存储：BCrypt-12 哈希
- 生产环境：HTTPS + useSSL=true
- 开发环境：HTTP + useSSL=false + allowPublicKeyRetrieval=true

### 7.2 访问控制

- RBAC 三级权限：管理员 / 版师 / 企业用户
- 所有敏感接口标注 `@SaCheckRole`
- 网关统一认证 + `X-User-Id` 透传
- CSRF Token 双重验证

### 7.3 数据安全

- 数据库连接禁止硬编码宿主机IP
- 密钥统一通过环境变量注入
- 生产环境 `.env` 不入版本控制
- 定时备份：MySQL 每日凌晨3点，保留30天

### 7.4 注入防护

- 前端 XSS 过滤：HTML 转义 + 标签过滤
- 前端 CSP 策略：禁止 inline script（生产环境）
- 后端 SQL 注入防护：MyBatis-Plus 参数化查询
- 后端 CSRF 防护：Token 双重验证

---

## 8. 技术栈边界

### 8.1 允许的依赖

| 类别 | 依赖 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.3.5 |
| 微服务 | Spring Cloud | 2023.0.3 |
| 注册中心 | Nacos | 2.3.2 |
| ORM | MyBatis-Plus | 3.5.7 |
| 认证 | Sa-Token | 1.38.0 |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 7.x |
| 消息队列 | RabbitMQ | 3.x |
| API文档 | Knife4j | 4.5.0 |
| 工具类 | Hutool | 5.8.32 |
| 前端框架 | Vue 3 + TypeScript | - |
| UI库 | Element Plus | - |
| 构建工具 | Vite | - |

### 8.2 禁止的依赖

- ❌ 未评估的第三方 3D 预览工具
- ❌ 未评估的区块链 SDK
- ❌ 冷门/废弃的第三方库
- ❌ Vue 2 / React / Angular（前端统一 Vue 3）

---

## 9. 验收条件

### 9.1 功能验收

- [ ] 用户注册/登录/登出正常（含邮箱+手机号+微信）
- [ ] 商品浏览/搜索/下单/支付全流程
- [ ] 纸样定制五阶段流程完整
- [ ] 版权存证+AI侵权检测
- [ ] 管理后台 CRUD 全部正常
- [ ] 多语言切换（6种语言）

### 9.2 非功能验收

- [ ] 所有 API 通过网关访问（端口 9000）
- [ ] 异常处理分层精准（401/403/400/503/500）
- [ ] 前端错误弹窗互斥（不重复弹出）
- [ ] 编译零错误（允许 null type safety 警告）
- [ ] 单元测试覆盖率 > 60%（核心模块）
- [ ] 安全扫描零 Critical 漏洞

### 9.3 部署验收

- [ ] Docker Compose 单套配置启动全部服务
- [ ] 所有服务健康检查通过
- [ ] 容器命名符合 `tailor-is-*` 规范
- [ ] 网络使用 `tailoris-services` bridge
- [ ] 无 host 网络模式

---

## 10. 变更记录

| 日期 | 版本 | 变更内容 |
|------|------|---------|
| 2026-07-21 | v1.1 | 初始版本 — VibeCoding Phase 1 Align 输出 |