# Tailor IS 部署后代码质量改进与问题修复综合方案

> **文档编号**：PDR-2026Q2-001
> **编制时间**：2026-07-02
> **输入依据**：
> - 阶段二问题清单 `docs/reports/POST-DEPLOYMENT-ISSUE-INVENTORY.md`（41 项问题）
> - 阶段三模块质量审计报告 `docs/reports/MODULE-QUALITY-AUDIT-REPORT.md`（综合评分 7.71/10）
> **适用范围**：Tailor IS 全量代码库（后端 17 个微服务 + 2 个网关 + 前端多端 + 部署配置 + 文档）
> **编制人**：Tailor IS 质量保障团队

---

## 目录

1. [执行摘要](#1-执行摘要)
2. [优先级分类标准](#2-优先级分类标准)
3. [技术债务量化评估](#3-技术债务量化评估)
4. [修复方案详情](#4-修复方案详情)
5. [分阶段实施策略](#5-分阶段实施策略)
6. [每周进度跟踪与问题反馈机制](#6-每周进度跟踪与问题反馈机制)
7. [修复后预期效果](#7-修复后预期效果)
8. [附录](#8-附录)

---

## 1. 执行摘要

### 1.1 问题总览

经阶段二（部署后问题清单）与阶段三（模块质量审计）系统性排查，Tailor IS 项目共识别技术问题 **41 项**（去重后独立问题 40 项，PD-FE-001 与 PD-LD-002 为同一微信登录缺陷的两个视角），覆盖功能错误、类型定义、规则违规、性能瓶颈、安全漏洞、逻辑缺陷、文档缺失七大类别。模块质量审计综合评分 **7.71/10（B+）**，核心交易链路（订单/支付/版权）质量优秀（≥8.4），但网关路由、异常处理、IM/Academy 轻量服务、安全配置等方面存在明显短板。

#### 1.1.1 各严重级别分布

| 严重级别 | 数量 | 占比 | 处置时限 | 典型代表 |
|----------|------|------|----------|----------|
| 🔴 P0 阻断级 | 4 项 | 9.8% | 立即修复（24h 内） | 微信登录反转、订单 API 不匹配、地址 API 无路由、网关路由与 Controller 路径不一致 |
| 🟠 P1 严重级 | 18 项 | 43.9% | 本周修复（7 天内） | 购物车/用户中心/商品/社区接口不匹配、网关硬编码、支付链路分歧、硬编码密钥密码、Sa-Token 异常、IM 模块不完整 |
| 🟡 P2 一般级 | 14 项 | 34.1% | 本月修复（30 天内） | API 规范违规、性能瓶颈、dev/staging 默认密码、端口暴露、文档不一致 |
| 🟢 P3 轻微级 | 5 项 | 12.2% | 季度规划（90 天内） | any 类型、terser 构建优化、README 完善、契约文档补齐 |

#### 1.1.2 各类别分布

| 类别 | 编号前缀 | 数量 | 主要严重级别 |
|------|----------|------|--------------|
| 一、功能错误 | PD-FE | 11 | P0×3 + P1×8 |
| 二、类型定义错误 | PD-TD | 3 | P1×2 + P3×1 |
| 三、规则违规项 | PD-RV | 4 | P2×4 |
| 四、性能瓶颈 | PD-PB | 3 | P2×2 + P3×1 |
| 五、安全漏洞 | PD-SV | 11 | P1×5 + P2×6 |
| 六、逻辑缺陷 | PD-LD | 4 | P0×1 + P1×3 |
| 七、文档缺失 | PD-DM | 5 | P2×2 + P3×3 |

#### 1.1.3 模块审计补充发现

模块质量审计报告额外识别关键缺陷 **15 项**（FIX-001~010、OPT-001~008、LNG-001~005），其中 4 项严重级别与问题清单存在交集或互补关系，已合并纳入本方案：

| 审计编号 | 摘要 | 合并归属 |
|----------|------|----------|
| FIX-001 | 网关路由路径不匹配（AI/Copyright/Payment/IM/Academy/MerchantDashboard） | 归入 P0（与 PD-FE-010 同源，扩展至全量 v1 路由） |
| FIX-002 | Sa-Token 异常未处理 | 新增 P1（安全/异常处理） |
| FIX-003 | 支付验签公钥未配置时跳过 | 新增 P1（安全） |
| FIX-004/006 | IM 发送消息/REST/WebSocket 缺失 | 新增 P1（功能缺失） |
| FIX-005 | Spring MVC 内置异常未处理 | 新增 P1（异常处理） |
| FIX-007 | Academy 课程 CRUD 缺失 | 新增 P1（功能缺失） |
| FIX-008~010 | 商家接口/废弃 Controller/AlipayService 强转 | 归入 P2 |

### 1.2 修复优先级原则

本方案遵循 **"业务阻断优先、安全风险次之、契约一致第三、规范优化兜底"** 的四层优先级原则：

1. **业务阻断优先**：直接影响核心业务流程（登录、下单、支付、地址）的 P0 问题必须 24 小时内修复，恢复主链路可用。
2. **安全风险次之**：硬编码密钥/密码、Actuator 端点暴露、支付验签跳过等高危安全漏洞须本周内消除，避免被利用。
3. **契约一致第三**：前后端 API 契约不匹配、GraphQL 解析器错误、网关路由硬编码等问题须本周内统一，恢复接口可用性。
4. **规范优化兜底**：API 路径规范、性能瓶颈、文档完善等 P2/P3 问题按月/季度规划，持续提升工程质量。

### 1.3 预期修复后效果

| 维度 | 当前状态 | 修复后目标 | 提升幅度 |
|------|----------|------------|----------|
| 模块质量综合评分 | 7.71/10（B+） | ≥8.50/10（A-） | +0.79 |
| 核心业务链路可用性 | 登录/下单/地址/支付部分阻断 | 100% 可用 | 恢复 |
| 前后端 API 契约一致率 | ~60%（11 项不匹配） | ≥98% | +38pp |
| 高危安全漏洞数 | 11 项（含 5 项 P1） | 0 项 | -11 |
| 异常处理覆盖率 | 72%（18/25） | ≥95% | +23pp |
| 功能点覆盖率 | 90.4%（122/135） | ≥96% | +5.6pp |
| IM 模块评分 | 4.30/10（D） | ≥7.00/10（B） | +2.70 |
| Academy 模块评分 | 5.35/10（C） | ≥7.50/10（B+） | +2.15 |

---

## 2. 优先级分类标准

### 2.1 严重程度 × 影响范围矩阵

| 影响范围 \ 严重程度 | 🔴 阻断（核心功能完全不可用） | 🟠 严重（重要功能受损/高危安全） | 🟡 一般（次要功能异常/规范违规） | 🟢 轻微（整洁度/文档） |
|---------------------|------------------------------|--------------------------------|--------------------------------|------------------------|
| **核心功能**（登录/下单/支付/地址） | **P0** 立即修复 | P1 本周修复 | P2 本月修复 | P3 季度规划 |
| **重要功能**（购物车/用户中心/商品/社区/GraphQL） | P0 | **P1** 本周修复 | P2 | P3 |
| **次要功能**（性能/规范/监控） | P1 | P1/P2 | **P2** 本月修复 | P3 |
| **界面/文档**（构建/README/契约文档） | P2 | P2/P3 | P3 | **P3** 季度规划 |

### 2.2 优先级定义与处置时限

| 优先级 | 定义 | 处置时限 | 验证要求 | 发布门禁 |
|--------|------|----------|----------|----------|
| **P0 阻断级** | 阻断核心功能，主业务流程不可用 | 立即修复，24 小时内提交修复 PR | 单元测试 + 回归测试 + 灰度验证 | 必须修复方可发布 |
| **P1 严重级** | 重要功能受损或存在高危安全风险 | 本周修复，7 天内完成 | 单元测试 + 接口测试 | 必须修复方可发布 |
| **P2 一般级** | 次要功能异常或违反规范，不阻断主流程 | 本月修复，30 天内完成 | 单元测试 | 建议修复 |
| **P3 轻微级** | 代码整洁度、文档完善度等优化项 | 季度规划，90 天内完成 | 视情况 | 不阻断发布 |

### 2.3 P0/P1/P2/P3 问题清单映射

#### P0 阻断级（4 项）

| 序号 | 问题编号 | 问题摘要 | 来源 |
|------|----------|----------|------|
| 1 | PD-FE-001 / PD-LD-002 | 微信登录状态校验逻辑反转 | 问题清单 |
| 2 | PD-FE-002 | 订单 API 前后端路径/方法/参数全面不匹配 | 问题清单 |
| 3 | PD-FE-004 | 收货地址 API 路径完全不匹配且网关无路由 | 问题清单 |
| 4 | FIX-001 / PD-FE-010 | 网关路由与 Controller 路径不一致（v1 前缀全量缺失） | 模块审计 + 问题清单 |

#### P1 严重级（18 项 + 6 项审计补充）

| 序号 | 问题编号 | 问题摘要 | 类别 |
|------|----------|----------|------|
| 1 | PD-FE-003 | 购物车 checkoutCart 请求体不匹配 + 缺 clearCart | 功能错误 |
| 2 | PD-FE-005 | 用户资料/密码/收藏/积分接口缺失或路径不匹配 | 功能错误 |
| 3 | PD-FE-006 | 商品列表分页不一致 + 搜索路径不匹配 | 功能错误 |
| 4 | PD-FE-007 | 社区帖子 API 路径不匹配 | 功能错误 |
| 5 | PD-FE-008 | GraphQL 解析器 HTTP 方法与路径错误 | 功能错误 |
| 6 | PD-FE-009 | 网关 user/product 路由硬编码 localhost | 功能错误 |
| 7 | PD-FE-010 | 支付服务 v1 路径无法经网关可达（并入 P0-FIX-001） | 功能错误 |
| 8 | PD-FE-011 | 前端 payOrder 未传后端必需的 payType | 功能错误 |
| 9 | PD-TD-001 | GraphQL fetchProducts 期望 records 字段但后端返回裸 List | 类型错误 |
| 10 | PD-TD-003 | GraphQL mutation 方法签名与后端契约不符 | 类型错误 |
| 11 | PD-SV-001 | 版权服务 AES 密钥硬编码默认值残留 | 安全漏洞 |
| 12 | PD-SV-002 | prod.yml mysqld-exporter 硬编码数据库真实密码 | 安全漏洞 |
| 13 | PD-SV-003 | Grafana 管理员密码硬编码 | 安全漏洞 |
| 14 | PD-SV-004 | JDBC 连接串 allowPublicKeyRetrieval=true 残留 | 安全漏洞 |
| 15 | PD-SV-006 | Actuator 暴露 metrics 端点且 health 全量展示 | 安全漏洞 |
| 16 | PD-LD-001 | 订单支付存在双重分歧路径 | 逻辑缺陷 |
| 17 | PD-LD-003 | GraphQL clearCart 调用不存在的后端端点 | 逻辑缺陷 |
| 18 | PD-LD-004 | 商品列表与首页数据加载链路类型断点 | 逻辑缺陷 |
| 补 1 | FIX-002 | Sa-Token 异常未在全局处理器覆盖 | 异常处理 |
| 补 2 | FIX-003 | 支付验签公钥未配置时跳过验签 | 安全漏洞 |
| 补 3 | FIX-004 | IM 发送消息/已读/会话创建 REST 接口缺失 | 功能缺失 |
| 补 4 | FIX-005 | Spring MVC 内置异常未显式处理 | 异常处理 |
| 补 5 | FIX-006 | IM WebSocket 实时推送未实现 | 功能缺失 |
| 补 6 | FIX-007 | Academy 课程 CRUD 完全缺失 | 功能缺失 |

> 说明：FIX-001 已合并入 P0 第 4 项。FIX-008/009/010 归入 P2（商家接口、废弃 Controller、AlipayService 强转），OPT-001~008 归入 P2，LNG-001~005 归入 P3。

#### P2 一般级（14 项 + 审计补充）

| 序号 | 问题编号 | 问题摘要 |
|------|----------|----------|
| 1 | PD-RV-001 | 用户状态使用魔法数字 0 |
| 2 | PD-RV-002 | API 路径违反自有 RESTful 规范（单数、无 v1 前缀） |
| 3 | PD-RV-003 | 已弃用控制器指向不存在的 v1 版本 |
| 4 | PD-RV-004 | 网关 application.yml 与 Java Config 路由定义重复 |
| 5 | PD-PB-001 | vue-vendor chunk 合并 element-plus 易超体积阈值 |
| 6 | PD-PB-002 | 订单热门缓存回写循环内多次 Redis 调用未批量化 |
| 7 | PD-SV-005 | dev/staging 环境与脚本 useSSL=false |
| 8 | PD-SV-007 | dev/staging 环境默认密码 |
| 9 | PD-SV-008 | 管理端口在 staging/development/monitoring 显式暴露 |
| 10 | PD-SV-009 | prod.yml 监控组件使用 host 网络模式 |
| 11 | PD-SV-010 | Nacos 默认密码 nacos 硬编码 |
| 12 | PD-SV-011 | refresh_token 存储于 sessionStorage 存在 XSS 泄露窗口 |
| 13 | PD-DM-001 | api-overview.md 接口路径与前端实际调用不一致 |
| 14 | PD-DM-002 | api-overview.md 标注弃用至不存在的 v1 接口 |
| 补 1 | FIX-008 | 商家商品/订单接口缺失 |
| 补 2 | FIX-009 | 废弃 Controller 未清理（UserController/ProductController/OrderController） |
| 补 3 | FIX-010 | AlipayService 实现类强转违反依赖倒置 |
| 补 4 | OPT-001~008 | 乐观锁/库存预校验/统计硬编码/秒杀空实现/鉴权统一/Map 参数/ResultCode/上传鉴权 |

#### P3 轻微级（5 项 + 审计长期规划）

| 序号 | 问题编号 | 问题摘要 |
|------|----------|----------|
| 1 | PD-TD-002 | 前端 catch 子句使用 any 类型 |
| 2 | PD-PB-003 | 构建使用 terser 压缩慢于 esbuild |
| 3 | PD-DM-003 | 各服务 README 仅含启动说明，缺少 API 契约细节 |
| 4 | PD-DM-004 | 缺少前后端 API 契约对齐文档 |
| 5 | PD-DM-005 | 服务 README 含空密码占位符 |
| 补 1 | LNG-001~005 | 推广分销/Academy 完善/动态路由/HMAC 校验/全链路追踪 |

---

## 3. 技术债务量化评估

### 3.1 问题数统计

#### 3.1.1 按类别统计

| 类别 | P0 | P1 | P2 | P3 | 合计 | 占比 |
|------|----|----|----|----|------|------|
| 功能错误（PD-FE） | 3 | 8 | 0 | 0 | 11 | 26.8% |
| 类型定义错误（PD-TD） | 0 | 2 | 0 | 1 | 3 | 7.3% |
| 规则违规项（PD-RV） | 0 | 0 | 4 | 0 | 4 | 9.8% |
| 性能瓶颈（PD-PB） | 0 | 0 | 2 | 1 | 3 | 7.3% |
| 安全漏洞（PD-SV） | 0 | 5 | 6 | 0 | 11 | 26.8% |
| 逻辑缺陷（PD-LD） | 1 | 3 | 0 | 0 | 4 | 9.8% |
| 文档缺失（PD-DM） | 0 | 0 | 2 | 3 | 5 | 12.2% |
| 审计补充（FIX/OPT/LNG） | 1 | 6 | 11 | 5 | 23 | — |
| **合计** | **5** | **24** | **25** | **10** | **64**（含审计补充） | 100% |

> 注：去重后独立问题 41 项；审计补充项与问题清单存在交集，实际修复任务约 50 项。

#### 3.1.2 按模块分布

| 模块 | 涉及问题数 | 主要问题类型 | 当前评分 | 修复后预期评分 |
|------|------------|--------------|----------|----------------|
| tailor-is-user | 6 | 登录反转/地址 API/用户中心接口/魔法数字/废弃 Controller | 8.20 | 8.80 |
| tailor-is-order | 6 | 订单 API/支付双重路径/缓存批量化/废弃 Controller | 9.00 | 9.30 |
| tailor-is-product | 5 | 商品分页/搜索路径/废弃 Controller/统计硬编码 | 7.55 | 8.20 |
| tailor-is-payment | 4 | v1 路由/支付验签/AlipayService 强转/双重路径 | 8.40 | 8.90 |
| tailor-is-community | 2 | 帖子 API 路径/资源式 RESTful | 8.20 | 8.60 |
| tailor-is-copyright | 1 | AES 密钥硬编码 | 8.60 | 9.00 |
| tailor-is-message-im | 2 | REST 接口缺失/WebSocket 未实现 | 4.30 | 7.00 |
| tailor-is-academy | 1 | 课程 CRUD 缺失 | 5.35 | 7.50 |
| tailor-is-core-gateway | 5 | 路由硬编码/v1 路径/路由重复/host 网络 | 8.55 | 9.00 |
| tailor-is-common | 3 | Actuator/Sa-Token 异常/Spring MVC 异常 | — | — |
| 前端 pc-mall | 10 | 多处 API 不匹配/any 类型/terser/refresh_token | — | — |
| 前端 graphql-gateway | 5 | 解析器方法路径/类型断言/clearCart | — | — |
| 部署配置 deploy | 8 | 硬编码密码/端口暴露/网络模式/useSSL | — | — |
| 文档 docs | 5 | API 文档不一致/契约文档缺失/README | — | — |

### 3.2 预估工作量（人天）

#### 3.2.1 P0 阻断级工作量

| 问题编号 | 问题摘要 | 修复 | 测试 | 验证 | 合计（人天） |
|----------|----------|------|------|------|--------------|
| PD-FE-001/LD-002 | 微信登录状态校验反转 | 0.25 | 0.25 | 0.25 | **0.75** |
| PD-FE-002 | 订单 API 全面不匹配 | 2.0 | 1.0 | 0.5 | **3.5** |
| PD-FE-004 | 地址 API 路径不匹配且无网关路由 | 0.5 | 0.5 | 0.25 | **1.25** |
| FIX-001/PD-FE-010 | 网关路由与 Controller 路径不一致 | 1.0 | 0.5 | 0.5 | **2.0** |
| **小计** | | | | | **7.5** |

#### 3.2.2 P1 严重级工作量

| 问题编号 | 问题摘要 | 合计（人天） |
|----------|----------|--------------|
| PD-FE-003 | 购物车结算请求体不匹配 + clearCart | 1.0 |
| PD-FE-005 | 用户资料/密码/收藏/积分接口缺失 | 3.0 |
| PD-FE-006 | 商品列表分页 + 搜索路径 | 1.5 |
| PD-FE-007 | 社区帖子 API 路径 | 2.0 |
| PD-FE-008 | GraphQL 解析器方法/路径 | 1.5 |
| PD-FE-009 | 网关 user/product 路由硬编码 | 0.5 |
| PD-FE-011 | 前端 payOrder 未传 payType | 0.25 |
| PD-TD-001 | GraphQL fetchProducts 类型断言 | 0.5 |
| PD-TD-003 | GraphQL mutation 方法签名 | 0.5 |
| PD-SV-001 | AES 密钥硬编码 | 1.0 |
| PD-SV-002 | mysqld-exporter 硬编码密码 | 0.5 |
| PD-SV-003 | Grafana 硬编码密码 | 0.5 |
| PD-SV-004 | allowPublicKeyRetrieval 残留 | 1.0 |
| PD-SV-006 | Actuator metrics 端点暴露 | 0.5 |
| PD-LD-001 | 订单支付双重分歧路径 | 2.5 |
| PD-LD-003 | GraphQL clearCart 调用不存在端点 | 0.25 |
| PD-LD-004 | 首页商品列表类型断点 | 0.5 |
| FIX-002 | Sa-Token 异常未处理 | 0.5 |
| FIX-003 | 支付验签公钥未配置时跳过 | 0.5 |
| FIX-004 | IM 发送消息 REST 接口缺失 | 2.0 |
| FIX-005 | Spring MVC 内置异常未处理 | 0.5 |
| FIX-006 | IM WebSocket 实时推送未实现 | 5.0 |
| FIX-007 | Academy 课程 CRUD 缺失 | 2.0 |
| **小计** | | **28.0** |

#### 3.2.3 P2 一般级工作量

| 问题编号 | 问题摘要 | 合计（人天） |
|----------|----------|--------------|
| PD-RV-001 | 用户状态魔法数字 | 0.5 |
| PD-RV-002 | API 路径 RESTful 规范迁移 | 3.0 |
| PD-RV-003 | 已弃用控制器指向不存在的 v1 | 1.0 |
| PD-RV-004 | 网关路由定义重复 | 0.25 |
| PD-PB-001 | vue-vendor chunk 拆分 | 0.5 |
| PD-PB-002 | 订单缓存 Redis 批量化 | 1.5 |
| PD-SV-005 | dev/staging useSSL=false | 1.0 |
| PD-SV-007 | dev/staging 默认密码 | 1.0 |
| PD-SV-008 | 管理端口暴露 | 1.0 |
| PD-SV-009 | 监控组件 host 网络模式 | 0.5 |
| PD-SV-010 | Nacos 默认密码 | 0.25 |
| PD-SV-011 | refresh_token 迁移 httpOnly cookie | 2.0 |
| PD-DM-001 | api-overview.md 路径修订 | 1.0 |
| PD-DM-002 | api-overview.md 弃用标注修订 | 0.25 |
| FIX-008 | 商家商品/订单接口 | 3.0 |
| FIX-009 | 废弃 Controller 清理 | 1.0 |
| FIX-010 | AlipayService 强转修复 | 1.0 |
| OPT-001~008 | 8 项中期优化 | 6.0 |
| **小计** | | **24.75** |

#### 3.2.4 P3 轻微级工作量

| 问题编号 | 问题摘要 | 合计（人天） |
|----------|----------|--------------|
| PD-TD-002 | catch 子句 any 类型 | 0.5 |
| PD-PB-003 | terser 改 esbuild | 0.5 |
| PD-DM-003 | 各服务 README 补充 | 2.0 |
| PD-DM-004 | 前后端 API 契约对齐文档 | 2.0 |
| PD-DM-005 | README 空密码占位符 | 0.25 |
| LNG-001~005 | 5 项长期规划 | 10.0 |
| **小计** | | **15.25** |

#### 3.2.5 工作量汇总

| 优先级 | 问题数 | 工作量（人天） | 占比 | 建议人力 | 预计周期 |
|--------|--------|----------------|------|----------|----------|
| P0 阻断级 | 4 | 7.5 | 10.8% | 3 人并行 | 1 天 |
| P1 严重级 | 24 | 28.0 | 40.3% | 4 人并行 | 7 天 |
| P2 一般级 | 32 | 24.75 | 35.6% | 3 人并行 | 30 天 |
| P3 轻微级 | 10 | 15.25 | 13.3% | 2 人并行 | 90 天 |
| **合计** | **70** | **75.5** | 100% | — | — |

> 注：工作量含开发 + 单元测试 + 集成测试 + 验证；并行开发可压缩实际日历周期。

### 3.3 风险等级评估

| 风险项 | 概率 | 影响 | 风险等级 | 关联问题 | 缓解措施 |
|--------|------|------|----------|----------|----------|
| 微信登录完全不可用 | 必现 | 极高（C 端用户无法登录） | 🔴 极高 | PD-FE-001 | 24h 内修复并灰度验证 |
| 订单链路 404 全阻断 | 必现 | 极高（无法下单/支付/查询） | 🔴 极高 | PD-FE-002 | 优先修复契约不匹配 |
| 支付回调被伪造（公钥未配置） | 中 | 极高（资金损失） | 🔴 极高 | FIX-003 | 强制校验公钥配置，缺失即抛异常 |
| 订单支付绕过付款直接发货 | 高 | 极高（资金损失） | 🔴 极高 | PD-LD-001 | 移除 order-service 直接标记支付能力 |
| 硬编码密钥/密码泄露 | 中 | 高（凭证泄露/数据泄露） | 🔴 高 | PD-SV-001~004 | 全部迁移至环境变量/Vault |
| 网关路由 404 导致功能不可用 | 高 | 高（用户体验） | 🔴 高 | FIX-001 | 立即补充 v1 路由谓词 |
| IM 功能基本不可用 | 必现 | 高（功能缺失） | 🟡 中高 | FIX-004/006 | 补充 REST + WebSocket |
| 权限不足返回 500 而非 403 | 中 | 中（用户体验/安全语义） | 🟡 中 | FIX-002 | 补充 Sa-Token 异常处理器 |
| Actuator 端点信息泄露 | 中 | 中（内部指标泄露） | 🟡 中 | PD-SV-006 | 收敛端点 + 加认证 |
| 前后端契约不匹配批量存在 | 必现 | 中（多接口 404/405） | 🟡 中 | PD-FE-002~008 | 统一契约 + CI 校验 |
| 废弃 Controller 仍在运行 | 低 | 低（维护成本） | 🟢 低 | FIX-009 | 验证客户端迁移后移除 |
| API 路径规范不统一 | 高 | 低（一致性/可维护性） | 🟢 低 | PD-RV-002 | 分阶段迁移至 v1 复数 |

---

## 4. 修复方案详情

### 4.1 P0 阻断级问题修复方案（4 项）

---

#### 4.1.1 PD-FE-001 / PD-LD-002：微信登录状态校验逻辑反转

**问题描述**：
微信授权登录后，后端 `WechatLoginServiceImpl#wechatLogin` 在第 106 行使用 `user.getStatus() == 0` 判断用户状态，当 status=0（正常）时错误地抛出"账号已被禁用"异常，导致所有正常微信用户无法登录；而禁用用户（status≠0）反而被放行。同项目 `AuthController.java#L94` 验证码登录分支使用 `user.getStatus() != 0`（正确），两处状态判断逻辑不一致。`WechatLoginServiceImpl.java#L188` 新建微信用户时 `user.setStatus(0)` 进一步证明 0=正常。

**修复思路**：
1. 修正判断逻辑：将 `== 0` 改为 `!= 0`，使语义与状态机一致（status=0 正常放行，status≠0 禁用拦截）。
2. 提取状态常量：消除魔法数字 0，定义 `UserStatusEnum` 或常量 `USER_STATUS_NORMAL=0`，避免同类错误再次发生（与 PD-RV-001 联动）。
3. 统一全工程状态判断：核查 `AuthController.java#L94, L159` 等所有 `getStatus()` 调用点，确保使用同一常量。

**技术方案**：

```java
// 1. 在 tailor-is-user 的 constant 包新增 UserStatusEnum
public enum UserStatusEnum {
    NORMAL(0, "正常"),
    DISABLED(1, "禁用");
    // ...
}

// 2. 修正 WechatLoginServiceImpl.java#L106
// 修复前：if (user.getStatus() == 0) { throw new BusinessException("账号已被禁用"); }
// 修复后：
if (!UserStatusEnum.NORMAL.getCode().equals(user.getStatus())) {
    throw new BusinessException("账号已被禁用");
}

// 3. 统一 AuthController.java#L94, L159 也使用 UserStatusEnum.NORMAL
```

**验证方法**：
1. **单元测试**：编写 `WechatLoginServiceImplTest`，覆盖 status=0（应放行）、status=1（应拦截）、status=null（应拦截或抛 NPE 防护）三种场景。
2. **回归测试**：执行微信登录全流程，验证正常用户可登录、禁用用户被拦截。
3. **对照验证**：与 `AuthController.loginByCode` 的状态判断逻辑做断言一致性校验。
4. **静态检查**：Grep 全工程 `getStatus() == 0` 与 `getStatus() != 0`，确认无残留魔法数字。

**预估工作量**：0.75 人天（修复 0.25 + 测试 0.25 + 验证 0.25）

**关联问题**：PD-RV-001（魔法数字常量化，P2 阶段统一处理全工程）

---

#### 4.1.2 PD-FE-002：订单 API 前后端路径/方法/参数全面不匹配

**问题描述**：
前端 `order.ts` 与后端 `OrderController` 存在五重不匹配，导致订单全链路（下单、支付、取消、确认收货、订单查询）404：
1. **路径**：前端 `/orders`（复数）vs 后端 `/api/order`（单数）
2. **列表**：前端 `GET /orders` vs 后端 `GET /api/order/list`
3. **方法**：前端 `POST /orders/{orderNo}/pay|confirm|cancel` vs 后端 `PUT`
4. **参数**：后端 `payOrder` 要求 `@RequestParam Integer payType`、`cancelOrder` 要求 `@RequestParam String reason`，前端均未传
5. **弃用**：后端 `OrderController` 已标注 `@Deprecated`，但前端仍在调用

**文件位置**：
- 前端：`tailor-is-frontend/pc-mall/src/api/order.ts#L17-L58`
- 后端：`tailor-is/tailor-is-order/src/main/java/com/tailoris/order/controller/OrderController.java#L23-L83`

**修复思路**：
鉴于 `OrderController` 已弃用且前端仍在使用，存在两种策略，**推荐策略 A（前端对齐后端）**：

- **策略 A（推荐，工作量小）**：前端对齐后端实际路由，移除 `@Deprecated` 标注（因 v1 控制器尚未存在，见 PD-RV-003），并补传必需参数。
- **策略 B（工作量大）**：后端补齐 v1 控制器 `OrderControllerV1`，前端迁移至 `/api/v1/orders`，与 PD-RV-002 规范一致。

**技术方案（策略 A）**：

```typescript
// tailor-is-frontend/pc-mall/src/api/order.ts 修正
// 1. 创建订单：POST /orders → POST /api/order
export function createOrder(data: CreateOrderRequest) {
  return request({ url: '/api/order', method: 'post', data });
}

// 2. 订单列表：GET /orders → GET /api/order/list
export function getOrders(params: OrderQuery) {
  return request({ url: '/api/order/list', method: 'get', params });
}

// 3. 支付订单：POST /orders/{orderNo}/pay → PUT /api/order/{orderNo}/pay?payType=1
export function payOrder(orderNo: string, payType: number = 1) {
  return request({ url: `/api/order/${orderNo}/pay`, method: 'put', params: { payType } });
}

// 4. 取消订单：POST /orders/{orderNo}/cancel → PUT /api/order/{orderNo}/cancel?reason=xxx
export function cancelOrder(orderNo: string, reason: string) {
  return request({ url: `/api/order/${orderNo}/cancel`, method: 'put', params: { reason } });
}

// 5. 确认收货：POST /orders/{orderNo}/confirm → PUT /api/order/{orderNo}/confirm
export function confirmReceive(orderNo: string) {
  return request({ url: `/api/order/${orderNo}/confirm`, method: 'put' });
}

// 6. 订单详情：GET /orders/{orderNo} → GET /api/order/{orderNo}
export function getOrderDetail(orderNo: string) {
  return request({ url: `/api/order/${orderNo}`, method: 'get' });
}
```

```java
// 后端 OrderController.java 移除 @Deprecated 注解（待 v1 控制器落地后再弃用）
@RestController
@RequestMapping("/api/order")
public class OrderController {  // 移除 @Deprecated
    // ...
}
```

**验证方法**：
1. **接口契约对齐测试**：使用 Postman/Apifox 逐项验证 6 个订单接口的路径、方法、参数匹配。
2. **E2E 测试**：执行 `E2E-SHOP-007/008/009/010` 全链路（创建订单 → 支付 → 确认收货）。
3. **网关路由测试**：验证 `GET /api/order/list` 经网关可达（网关已有 `/api/order/**` 路由）。
4. **前端编译检查**：`npm run build` 通过，无 TS 类型错误。

**预估工作量**：3.5 人天（修复 2.0 + 测试 1.0 + 验证 0.5）

**关联问题**：PD-FE-011（payType 参数，一并修复）、PD-FE-008（GraphQL 订单解析器）、PD-RV-003（弃用标注治理）

---

#### 4.1.3 PD-FE-004：收货地址 API 路径完全不匹配且网关无路由

**问题描述**：
前端 `address.ts` 调用 `/api/addresses`（复数、无 `/user` 前缀），后端 `AddressController` 实际路径为 `/api/user/address`（单数、含 `/user`），网关 `CoreGatewayRouteConfig` 仅有 `/api/user/**`、`/api/auth/**` 路由，无 `/api/addresses/**` 路由，导致结算页地址加载/新增/默认设置全部 404。

**文件位置**：
- 前端：`tailor-is-frontend/pc-mall/src/api/address.ts#L24-L58`
- 后端：`tailor-is/tailor-is-user/src/main/java/com/tailoris/user/controller/AddressController.java#L41-L43`
- 网关：`tailor-is/tailor-is-core-gateway/src/main/java/com/tailoris/coregateway/config/CoreGatewayRouteConfig.java#L52`

**修复思路**：
前端路径对齐后端 `/api/user/address`，复用网关已有的 `/api/user/**` 路由，无需新增网关配置。

**技术方案**：

```typescript
// tailor-is-frontend/pc-mall/src/api/address.ts 修正
const BASE = '/api/user/address';

// 1. 地址列表：GET /addresses → GET /api/user/address/list
export function getAddresses() {
  return request({ url: `${BASE}/list`, method: 'get' });
}

// 2. 新增地址：POST /addresses → POST /api/user/address
export function addAddress(data: AddressForm) {
  return request({ url: BASE, method: 'post', data });
}

// 3. 修改地址：PUT /addresses/{id} → PUT /api/user/address/{id}
export function updateAddress(id: number, data: AddressForm) {
  return request({ url: `${BASE}/${id}`, method: 'put', data });
}

// 4. 删除地址：DELETE /addresses/{id} → DELETE /api/user/address/{id}
export function deleteAddress(id: number) {
  return request({ url: `${BASE}/${id}`, method: 'delete' });
}

// 5. 设置默认地址：PUT /addresses/{id}/default → PUT /api/user/address/{id}/default
export function setDefaultAddress(id: number) {
  return request({ url: `${BASE}/${id}/default`, method: 'put' });
}
```

**验证方法**：
1. **接口契约测试**：逐项验证 5 个地址接口的路径匹配。
2. **网关路由测试**：`GET /api/user/address/list` 经网关透传至 `lb://tailor-is-user`。
3. **E2E 测试**：结算页加载地址列表 → 新增地址 → 设置默认地址全流程。
4. **文档对齐**：核对 `api-overview.md` 路径与前端一致（与 PD-DM-001 联动）。

**预估工作量**：1.25 人天（修复 0.5 + 测试 0.5 + 验证 0.25）

**关联问题**：PD-FE-009（网关 user 路由硬编码，需先修复以确保 `lb://tailor-is-user` 可达）

---

#### 4.1.4 FIX-001 / PD-FE-010：网关路由与 Controller 路径不一致（v1 前缀全量缺失）

**问题描述**：
多个后端 Controller 使用 `/api/v1/**` 前缀，但网关路由谓词仅配置 `/api/**`（无 v1 段），导致经网关访问全部 404。模块审计识别涉及模块：AI、Copyright、Payment、IM、Academy、Merchant Dashboard。问题清单 PD-FE-010 进一步确认 payment-service 的 `/api/v1/payment/wechat` 经网关不可达。

**文件位置**：
- 网关：`tailor-is/tailor-is-core-gateway/src/main/java/com/tailoris/coregateway/config/CoreGatewayRouteConfig.java`
- 轻量网关：`tailor-is/tailor-is-lite-gateway/src/main/java/com/tailoris/litegateway/config/LiteGatewayRouteConfig.java`
- 后端示例：`PayController.java#L37`（`/api/v1/payment`）、`PatternController`（`/api/v1/ai/pattern`）、`CopyrightController`（`/api/v1/copyright`）

**修复思路**：
在网关路由配置中为所有 v1 控制器补充 `/api/v1/**` 路由谓词，使网关与 Controller 路径对齐。同时与 PD-FE-009 联动，将硬编码 `localhost` 改为 `lb://` 服务发现。

**技术方案**：

```java
// CoreGatewayRouteConfig.java 修正（同时修复 PD-FE-009 硬编码 + PD-FE-010 v1 路由）
@Bean
public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
    return builder.routes()
        // 用户服务（修复 PD-FE-009：localhost → lb://）
        .route("user-route", r -> r
            .path("/api/user/**", "/api/auth/**")
            .uri("lb://tailor-is-user"))  // 原 http://localhost:8101
        // 商品服务（修复 PD-FE-009：host.docker.internal → lb://）
        .route("product-route", r -> r
            .path("/api/product/**", "/api/products/**")
            .uri("lb://tailor-is-product"))  // 原 http://host.docker.internal:8102
        // 订单服务
        .route("order-route", r -> r
            .path("/api/order/**", "/api/cart/**", "/api/order/after-sale/**", "/api/logistics/**")
            .uri("lb://tailor-is-order"))
        // 支付服务（修复 PD-FE-010：增加 v1 路径）
        .route("payment-route", r -> r
            .path("/api/payment/**", "/api/v1/payment/**", "/api/settlement/**", "/api/escrow/**", "/api/reconciliation/**", "/api/account/**")
            .uri("lb://tailor-is-payment"))
        // AI 服务（增加 v1 路径）
        .route("ai-route", r -> r
            .path("/api/ai/**", "/api/v1/ai/**", "/api/body-size/**", "/api/v1/ai/size/**")
            .uri("lb://tailor-is-ai"))
        // 版权服务（增加 v1 路径）
        .route("copyright-route", r -> r
            .path("/api/copyright/**", "/api/v1/copyright/**")
            .uri("lb://tailor-is-copyright"))
        // 商家服务（增加 dashboard v1 路径）
        .route("merchant-route", r -> r
            .path("/api/merchant/**", "/api/shop/**", "/api/v1/merchant/**")
            .uri("lb://tailor-is-merchant"))
        // ... 其他路由
        .build();
}

// LiteGatewayRouteConfig.java 同步补充 academy/im v1 路径
@Bean
public RouteLocator liteRouteLocator(RouteLocatorBuilder builder) {
    return builder.routes()
        // Academy（增加 v1 路径）
        .route("academy-route", r -> r
            .path("/api/academy/**", "/api/v1/academy/**")
            .uri("lb://tailor-is-academy"))
        // IM（增加 v1 路径）
        .route("im-route", r -> r
            .path("/api/im/**", "/api/v1/im/**")
            .uri("lb://tailor-is-message-im"))
        // ...
        .build();
}
```

**验证方法**：
1. **路由矩阵测试**：编写 `IT-GW-001~009` 集成测试用例，逐项验证每个 v1 路径经网关可达。
2. **缺陷用例回归**：`IT-GW-008`（AI 路径不匹配缺陷用例）应从 404 变为 200。
3. **容器化部署验证**：Docker/k8s 环境下验证 `lb://` 服务发现生效，无 `localhost` 残留。
4. **静态检查**：Grep `CoreGatewayRouteConfig.java` 与 `LiteGatewayRouteConfig.java`，确认无 `localhost`、`host.docker.internal` 硬编码。

**预估工作量**：2.0 人天（修复 1.0 + 测试 0.5 + 验证 0.5）

**关联问题**：PD-FE-009（硬编码 localhost，一并修复）、PD-RV-004（YAML 与 Java Config 重复，P2 阶段清理）

---

### 4.2 P1 严重级问题修复方案（18 项 + 6 项审计补充）

---

#### 4.2.1 PD-FE-003：购物车 checkoutCart 请求体不匹配且缺少 clearCart 端点

**问题描述**：
前端 `CheckoutView.vue` 调用 `checkoutCart(ids)` 发送 `{ ids: [...] }` 对象，后端 `CartController.batchCheckout` 期望 `List<Long>`（裸数组），反序列化失败。且前端 `clearCart()` 调用 `DELETE /cart/clear`，后端无此端点。

**修复思路**：
1. 前端 `checkoutCart` 改为发送裸数组 `[1, 2, 3]`。
2. 后端 `CartController` 补充 `clearCart` 端点 `DELETE /api/cart/clear`。

**技术方案**：

```typescript
// tailor-is-frontend/pc-mall/src/api/cart.ts 修正
export function checkoutCart(ids: number[]) {
  // 修复前：return request({ url: '/cart/checkout', method: 'post', data: { ids } });
  // 修复后：发送裸数组
  return request({ url: '/api/cart/checkout', method: 'post', data: ids });
}

export function clearCart() {
  return request({ url: '/api/cart/clear', method: 'delete' });
}
```

```java
// CartController.java 补充 clearCart 端点
@DeleteMapping("/clear")
public Result<Boolean> clearCart(@RequestHeader("X-User-Id") Long userId) {
    cartService.clearCart(userId);
    return Result.success(true);
}
```

**验证方法**：购物车结算 E2E 测试 + 清空购物车接口测试。

**预估工作量**：1.0 人天

---

#### 4.2.2 PD-FE-005：用户资料/密码/收藏/积分接口缺失或路径不匹配

**问题描述**：
个人中心多项接口缺失或路径不匹配：
- `PUT /user/profile` vs 后端 `PUT /api/user/info`
- `PUT /user/password` 后端无此端点
- `GET /user/favorites`、`POST/DELETE /user/favorites/{id}` 后端无此端点（收藏在 `ProductFavoriteController`）
- `GET /user/points` 后端无此端点（积分在 marketing-service）

**修复思路**：
1. 前端 `updateProfile` 路径改为 `/api/user/info`。
2. 后端 `UserController` 补充 `PUT /api/user/password` 修改密码端点。
3. 前端收藏接口改为 `/api/product/favorite/**`（对齐 `ProductFavoriteController`）。
4. 前端积分接口改为 `/api/v1/marketing/points/**`（对齐 marketing-service `PointsController`，依赖 P0 网关 v1 路由修复）。

**技术方案**：

```typescript
// user.ts 修正
export function updateProfile(data: UserProfile) {
  return request({ url: '/api/user/info', method: 'put', data });
}

export function updatePassword(data: { oldPassword: string; newPassword: string }) {
  return request({ url: '/api/user/password', method: 'put', data });
}

// 收藏接口改为对齐 ProductFavoriteController
export function getFavorites(params: PageQuery) {
  return request({ url: '/api/product/favorite/list', method: 'get', params });
}
export function addFavorite(productId: number) {
  return request({ url: `/api/product/favorite/${productId}`, method: 'post' });
}
export function removeFavorite(productId: number) {
  return request({ url: `/api/product/favorite/${productId}`, method: 'delete' });
}

// 积分接口改为对齐 marketing-service
export function getPoints() {
  return request({ url: '/api/v1/marketing/points/balance', method: 'get' });
}
```

```java
// UserController.java 补充修改密码端点
@PutMapping("/password")
public Result<Boolean> updatePassword(@RequestBody @Valid PasswordUpdateRequest req,
                                       @RequestHeader("X-User-Id") Long userId) {
    userService.updatePassword(userId, req.getOldPassword(), req.getNewPassword());
    return Result.success(true);
}
```

**验证方法**：个人中心各功能 E2E 测试（资料修改、密码修改、收藏列表、积分查询）。

**预估工作量**：3.0 人天（接口多，需逐项对齐）

---

#### 4.2.3 PD-FE-006 / PD-LD-004 / PD-TD-001：商品列表分页不一致 + 搜索路径不匹配 + 类型断点

**问题描述**（合并 3 项同源问题）：
- 前端 `getProducts` 期望 `PageResponse`（含 `records`），后端 `PublicProductController.listProducts` 返回 `List<Product>`（无分页）→ `res.records` undefined。
- 前端 `searchProducts` 调用 `/products/search`，后端搜索在 `/api/product/search`（单数）→ 404。
- GraphQL `fetchProducts` 解析器同样假设 `{records: [...]}` 结构，访问 `raw.records.map` 抛 TypeError。
- 首页 `HomeView.vue` 调用 `getProducts` 时 `res.records` undefined，商品列表渲染异常。

**修复思路**：
后端 `listProducts` 改为分页返回 `PageResponse`，统一所有列表接口为分页结构；搜索路径统一为 `/api/products/search`（复数，对齐 RESTful 规范）。

**技术方案**：

```java
// PublicProductController.java 修正
@GetMapping("/list")
public Result<PageResponse<ProductVO>> listProducts(
        @RequestParam(defaultValue = "1") Integer current,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String sort) {
    PageResponse<ProductVO> page = productService.listProducts(current, size, categoryId, sort);
    return Result.success(page);
}
```

```typescript
// product.ts 修正搜索路径
export function searchProducts(params: { keyword: string; current?: number; size?: number }) {
  return request({ url: '/api/products/search', method: 'get', params });
  // 原 /products/search
}
```

```typescript
// graphql-gateway/resolvers.ts fetchProducts 修正（与后端 PageResponse 对齐）
const raw = await axios.get('/api/product/list', { params: { current, size } });
// 修复后：raw.data.records 可用（因后端已改为 PageResponse）
return raw.data.records.map(/* ... */);
```

**验证方法**：
1. 商品列表分页返回测试（含 records、total、current、size 字段）。
2. 搜索接口路径与分页结构测试。
3. GraphQL `products` 查询不再抛 TypeError。
4. 首页商品列表正常渲染。

**预估工作量**：1.5 人天（PD-FE-006）+ 0.5 人天（PD-TD-001）+ 0.5 人天（PD-LD-004）= 2.5 人天（合并修复后约 1.5 人天）

---

#### 4.2.4 PD-FE-007：社区帖子 API 路径不匹配

**问题描述**：
前端 `community.ts` 用资源式 `/community/posts`（复数），后端 `PostController` 用动作式 `/api/community/post/create`、`/post/list`、`/post/detail/{id}`；点赞 `/posts/{id}/like`、评论 `/posts/{id}/comments` 路径在 `PostController` 中无对应。

**修复思路**：
统一社区接口为资源式 RESTful 路径，补齐 like/comments 端点。

**技术方案**：

```java
// PostController.java 改为资源式（保留旧端点兼容或直接迁移）
@RestController
@RequestMapping("/api/community/post")
public class PostController {
    @GetMapping("/list")  // 保留
    public Result<PageResponse<PostVO>> list(@RequestParam(defaultValue="1") Integer current, ...) { ... }

    @GetMapping("/{id}")  // 新增（替代 /detail/{id}）
    public Result<PostVO> getById(@PathVariable Long id) { ... }

    @PostMapping  // 新增（替代 /create）
    public Result<Long> create(@RequestBody @Valid PostCreateRequest req) { ... }

    @PostMapping("/{id}/like")  // 新增点赞
    public Result<Boolean> like(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) { ... }

    @GetMapping("/{id}/comments")  // 新增评论列表
    public Result<PageResponse<CommentVO>> listComments(@PathVariable Long id, ...) { ... }

    @PostMapping("/{id}/comments")  // 新增评论
    public Result<Long> comment(@PathVariable Long id, @RequestBody @Valid CommentCreateRequest req) { ... }
}
```

```typescript
// community.ts 修正
export function getPosts(params: PageQuery) {
  return request({ url: '/api/community/post/list', method: 'get', params });
}
export function getPostDetail(id: number) {
  return request({ url: `/api/community/post/${id}`, method: 'get' });
}
export function createPost(data: PostForm) {
  return request({ url: '/api/community/post', method: 'post', data });
}
export function likePost(id: number) {
  return request({ url: `/api/community/post/${id}/like`, method: 'post' });
}
export function getComments(id: number, params: PageQuery) {
  return request({ url: `/api/community/post/${id}/comments`, method: 'get', params });
}
export function addComment(id: number, data: CommentForm) {
  return request({ url: `/api/community/post/${id}/comments`, method: 'post', data });
}
```

**验证方法**：社区浏览、发帖、评论、点赞 E2E 测试。

**预估工作量**：2.0 人天

---

#### 4.2.5 PD-FE-008 / PD-TD-003 / PD-LD-003：GraphQL 解析器方法/路径/签名错误

**问题描述**（合并 3 项同源问题）：
- `updateCartItem` 用 `post` 应为 `put`；`deleteCartItem` 用 `post` 应为 `delete`。
- `clearCart` 调用 `/cart/clear`（后端不存在），解析器却返回 `true` 误导前端。
- `order`/`orders` 查询调用 `/orders/{id}`、`/orders`（后端为 `/api/order/{orderNo}`、`/api/order/list`）。
- `UpdateCartItemInput` 仅含 `{id, quantity}`，后端 `CartUpdateRequest` 含 `checked` 字段。

**修复思路**：
按后端实际路由修正 GraphQL 解析器的 HTTP 方法和路径；`UpdateCartItemInput` 补齐 `checked` 字段；后端补 `clearCart` 端点（与 PD-FE-003 联动）或解析器返回真实状态。

**技术方案**：

```typescript
// graphql-gateway/resolvers.ts 修正
const resolvers = {
  Mutation: {
    updateCartItem: async (_, { input }) => {
      // 修复：post → put，路径修正，补 checked 字段
      const { data } = await axios.put(`/api/cart/${input.id}`, {
        quantity: input.quantity,
        checked: input.checked ?? true,  // 补齐后端必需字段
      });
      return transformCartItem(data.data);
    },
    deleteCartItem: async (_, { id }) => {
      // 修复：post → delete
      await axios.delete(`/api/cart/${id}`);
      return true;
    },
    clearCart: async (_, __, context) => {
      // 修复：调用后端真实端点（依赖 PD-FE-003 补充 clearCart）
      const { data } = await axios.delete('/api/cart/clear', {
        headers: { 'X-User-Id': context.userId }
      });
      return data.success;  // 返回真实状态，不再硬编码 true
    },
  },
  Query: {
    order: async (_, { orderNo }) => {
      // 修复：/orders/{id} → /api/order/{orderNo}
      const { data } = await axios.get(`/api/order/${orderNo}`);
      return transformOrder(data.data);
    },
    orders: async (_, { current, size }) => {
      // 修复：/orders → /api/order/list
      const { data } = await axios.get('/api/order/list', { params: { current, size } });
      return data.data.records.map(transformOrder);
    },
  },
};

// Input 类型补齐
const UpdateCartItemInput = new GraphQLInputObjectType({
  name: 'UpdateCartItemInput',
  fields: {
    id: { type: GraphQLNonNull(GraphQLID) },
    quantity: { type: GraphQLNonNull(GraphQLInt) },
    checked: { type: GraphQLBoolean },  // 新增
  },
});
```

**验证方法**：GraphQL Playground 逐项验证 mutation/query，HTTP 方法与路径正确。

**预估工作量**：1.5 人天（PD-FE-008）+ 0.5 人天（PD-TD-003）+ 0.25 人天（PD-LD-003）= 2.25 人天（合并修复后约 1.5 人天）

---

#### 4.2.6 PD-FE-009：网关 user/product 路由硬编码 localhost

**问题描述**：
`user-route` 用 `http://localhost:8101`，`product-route` 用 `http://host.docker.internal:8102`，其余路由用 `lb://`。该类注释（L17）明确要求"禁止写死 http://localhost:port"，属自有规范违规。Docker/k8s 部署下容器内无此服务，路由失败。

**修复思路**：改为 `lb://tailor-is-user` 和 `lb://tailor-is-product`。

**技术方案**：见 4.1.4 P0 修复方案（已合并处理）。

**验证方法**：容器化部署下验证 user/product 服务经网关可达；Grep 确认无 `localhost`/`host.docker.internal` 残留。

**预估工作量**：0.5 人天（与 P0-FIX-001 合并修复）

---

#### 4.2.7 PD-FE-010：支付服务 v1 路径无法经网关可达

**问题描述**：`PayController` 映射 `/api/v1/payment`，但网关 `payment-route` 谓词仅含 `/api/payment/**`，`/api/v1/payment/**` 不被匹配。同理 merchant/copyright/ai 等 v1 控制器均存在此问题。

**修复思路**：见 4.1.4 P0 修复方案（已合并处理，网关路由谓词增加 `/api/v1/payment/**` 等 v1 前缀）。

**预估工作量**：0 人天（已并入 P0-FIX-001）

---

#### 4.2.8 PD-FE-011：前端 payOrder 未传后端必需的 payType 参数

**问题描述**：前端 `payOrder(orderNo)` 未传 `payType`，后端 `@RequestParam Integer payType` 必填，请求被拒 400。

**修复思路**：见 4.1.2 P0 修复方案（已合并处理，前端补传 `payType`）。

**预估工作量**：0.25 人天（已并入 P0-PD-FE-002）

---

#### 4.2.9 PD-SV-001：版权服务 AES 密钥硬编码默认值残留

**问题描述**：
`CopyrightServiceImpl` `@Value("${copyright.aes.key:t8V4kL0mN2pR6sQ9wX3yZ7aB1cD5eF0hI4jK8lM2nO6pQ0rS4tU8vW0xY2zA4bC}")` 保留 64 字符硬编码默认密钥，若环境变量未设置将使用该明文密钥。

**修复思路**：
1. 移除 `@Value` 默认值，强制从配置注入。
2. 启动时校验密钥已配置，未配置则启动失败（fail-fast）。
3. 生产环境密钥通过 Vault 或 K8s Secret 注入。

**技术方案**：

```java
// CopyrightServiceImpl.java 修正
@Service
public class CopyrightServiceImpl implements CopyrightService {
    @Value("${copyright.aes.key}")  // 移除默认值
    private String aesKey;

    @PostConstruct
    public void validateKey() {
        if (StringUtils.isBlank(aesKey)) {
            throw new IllegalStateException("copyright.aes.key 未配置，服务无法启动");
        }
        if (aesKey.length() != 32 && aesKey.length() != 64) {
            throw new IllegalStateException("copyright.aes.key 长度必须为 32 或 64 字符");
        }
    }
    // ...
}
```

**验证方法**：
1. 不配置 `copyright.aes.key` 启动，应 fail-fast 抛异常。
2. 配置后启动正常，版权加密/解密功能可用。
3. Grep 确认源码无硬编码密钥字符串。

**预估工作量**：1.0 人天

---

#### 4.2.10 PD-SV-002：prod.yml mysqld-exporter 硬编码数据库真实密码

**问题描述**：
`docker-compose.prod.yml` `DATA_SOURCE_NAME: "root:mysql_ZmY2sr@tcp(localhost:3306)/"` 明文真实密码；`deploy/my.cnf#L3` 与 `perf/analyze-slow-queries.sh#L25` 同样硬编码。

**修复思路**：改用 `${MYSQL_PASSWORD}` 环境变量注入，清理 my.cnf 与脚本中的明文密码。

**技术方案**：

```yaml
# docker-compose.prod.yml 修正
mysqld-exporter:
  environment:
    DATA_SOURCE_NAME: "root:${MYSQL_PASSWORD}@tcp(mysql:3306)/"
```

```ini
# my.cnf 修正（移除明文密码，改用环境变量或 ~/.my.cnf 读取）
[client]
# password = ${MYSQL_PASSWORD}  # 移除明文，通过环境变量注入
```

```bash
# analyze-slow-queries.sh 修正
MYSQL_PWD="${MYSQL_PASSWORD}" mysqldumpslow /var/log/mysql/slow.log
```

**验证方法**：Grep `mysql_ZmY2sr` 确认全工程无残留；启动验证 exporter 连接正常。

**预估工作量**：0.5 人天

---

#### 4.2.11 PD-SV-003：Grafana 管理员密码硬编码

**问题描述**：
`GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_PASSWORD:-TailorIS2026@Grafana}` 保留默认明文密码；`task4-prometheus.sh`、`task4-fix.sh`、`task4-deploy.sh`、`start-monitoring.sh` 多处硬编码。

**修复思路**：移除所有默认值，使用 `${GRAFANA_PASSWORD:?required}` 强制注入。

**技术方案**：

```yaml
# docker-compose-monitoring.yml 修正
grafana:
  environment:
    GF_SECURITY_ADMIN_PASSWORD: "${GRAFANA_PASSWORD:?required}"
```

```bash
# 所有部署脚本修正
export GRAFANA_PASSWORD="${GRAFANA_PASSWORD:?GRAFANA_PASSWORD 未配置}"
```

**验证方法**：Grep `TailorIS2026@Grafana` 确认无残留；不配置环境变量启动应失败。

**预估工作量**：0.5 人天

---

#### 4.2.12 PD-SV-004：JDBC 连接串 allowPublicKeyRetrieval=true 残留

**问题描述**：
`deploy/docker-compose.services.yml` 全部 17 条 JDBC URL 与 `deploy/k8s/production/configmap.yaml` 全部 16 条 URL 均含 `allowPublicKeyRetrieval=true`，存在中间人攻击风险。

**修复思路**：
1. 生产环境移除 `allowPublicKeyRetrieval=true`，配置 MySQL SSL 证书。
2. 短期方案：改为 `allowPublicKeyRetrieval=false&useSSL=true&requireSSL=true`。
3. 长期方案：配置 MySQL 服务端 SSL 证书，JDBC 连接串指定 `trustServerCertificate` 与 `clientCertificateKeyStoreUrl`。

**技术方案**：

```yaml
# docker-compose.services.yml 与 configmap.yaml 修正
SPRING_DATASOURCE_URL: "jdbc:mysql://mysql:3306/tailoris?useUnicode=true&characterEncoding=utf8&useSSL=true&requireSSL=true&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=false"
```

**验证方法**：服务启动后数据库连接正常；Grep `allowPublicKeyRetrieval=true` 确认生产配置无残留（dev/staging 可保留，见 PD-SV-005）。

**预估工作量**：1.0 人天

---

#### 4.2.13 PD-SV-006：Actuator 暴露 metrics 端点且 health 全量展示

**问题描述**：
`tailor-is-common/src/main/resources/application.yml` 配置 `include: health,info,prometheus,metrics` 且 `show-details: always`，metrics 端点暴露 JVM/DB/线程池等内部指标，health 暴露组件依赖详情。

**修复思路**：
1. 移除 `metrics`，仅保留 `health,info,prometheus`。
2. `show-details` 改为 `when-authorized`。
3. prometheus 端点加认证（与 Sa-Token 集成或 Basic Auth）。

**技术方案**：

```yaml
# tailor-is-common/src/main/resources/application.yml 修正
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus  # 移除 metrics
  endpoint:
    health:
      show-details: when-authorized  # 原 always
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true

# 各服务 application-prod.yml 增加 prometheus 端点认证
spring:
  security:
    user:
      name: ${ACTUATOR_USER}
      password: ${ACTUATOR_PASSWORD}
```

**验证方法**：
1. 访问 `/actuator/metrics` 应 404。
2. 访问 `/actuator/health` 不展示组件详情（未认证时）。
3. 访问 `/actuator/prometheus` 需认证。
4. Prometheus 抓取配置补充 Basic Auth。

**预估工作量**：0.5 人天

---

#### 4.2.14 PD-LD-001：订单支付存在双重分歧路径

**问题描述**：
order-service 的 `payOrder`（L303）直接将订单标记为已支付并扣减库存，绕过真实支付网关；与 payment-service 真实支付流程并存，导致可被绕过付款直接发货。

```
路径 A（绕过）：前端 payOrder → order-service.payOrder → 直接标记 PAID + 扣库存
路径 B（真实）：前端 → payment-service.wechatPay → 微信支付 → 回调 → payCallback
```

**修复思路**：
移除 order-service 直接标记支付的能力，支付状态仅由 payment-service 回调驱动。`OrderController.payOrder` 改为仅创建支付单（调用 payment-service），不直接修改订单状态。

**技术方案**：

```java
// OrderServiceImpl.java 修正 payOrder 方法
public void payOrder(String orderNo, Integer payType) {
    OrderInfo order = getOrderByOrderNo(orderNo);
    OrderStateMachine.verifyTransition(order.getStatus(), OrderStatus.PAID.getCode());

    // 修复前：直接 order.setStatus(PAID); inventoryService.confirmDeduct(orderNo);
    // 修复后：仅创建支付单，由 payment-service 回调驱动状态变更
    PayRequest payRequest = new PayRequest();
    payRequest.setOrderNo(orderNo);
    payRequest.setPayType(payType);
    payRequest.setAmount(order.getTotalAmount());
    payRequest.setUserId(order.getUserId());
    payFeignClient.createPayment(payRequest);  // 调用 payment-service 创建支付单

    // 订单状态保持 PENDING_PAY，等待支付回调
    log.info("订单 {} 已创建支付单，等待支付回调", orderNo);
}

// 支付回调由 payment-service 的 PayController.payCallback 触发，
// 调用 order-service 的内部接口更新状态为 PAID 并确认扣减库存
```

**验证方法**：
1. 单元测试：`payOrder` 不再直接修改订单状态。
2. E2E 测试 `E2E-SHOP-008/009`：创建支付单 → 模拟回调 → 订单状态变 PAID。
3. 安全测试：直接调用 `payOrder` 后订单状态应仍为 PENDING_PAY，无法绕过付款。

**预估工作量**：2.5 人天（涉及订单/支付跨服务改造）

---

#### 4.2.15 FIX-002：Sa-Token 异常未在全局处理器覆盖

**问题描述**：
Sa-Token 异常（`NotLoginException`、`NotRoleException`、`NotPermissionException`、`DisableServiceException`）未在 `GlobalExceptionHandler` 中显式处理，导致返回 500 而非 401/403。

**修复思路**：
在 `GlobalExceptionHandler` 中新增 Sa-Token 异常处理器，返回正确的 HTTP 状态码与业务码。

**技术方案**：

```java
// GlobalExceptionHandler.java 新增
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLogin(NotLoginException e) {
        log.warn("未登录访问: {}", e.getMessage());
        return Result.fail(ResultCode.UNAUTHORIZED);  // 401
    }

    @ExceptionHandler(NotRoleException.class)
    public Result<Void> handleNotRole(NotRoleException e) {
        log.warn("角色不足: {}", e.getMessage());
        return Result.fail(ResultCode.FORBIDDEN);  // 403
    }

    @ExceptionHandler(NotPermissionException.class)
    public Result<Void> handleNotPermission(NotPermissionException e) {
        log.warn("权限不足: {}", e.getMessage());
        return Result.fail(ResultCode.FORBIDDEN);  // 403
    }

    @ExceptionHandler(DisableServiceException.class)
    public Result<Void> handleDisable(DisableServiceException e) {
        log.warn("账号被封禁: {}", e.getMessage());
        return Result.fail(ResultCode.ACCOUNT_DISABLED);  // 403
    }
}
```

**验证方法**：异常场景 4（权限不足）应从 500 变为 403；异常场景 1/2/3 保持 401。

**预估工作量**：0.5 人天

---

#### 4.2.16 FIX-003：支付验签公钥未配置时跳过验签

**问题描述**：
`PaymentServiceImpl.verifyCallbackSign` 在 `alipayPublicKey` 未配置时跳过验签（`return true`），仅开发环境容错，生产环境若忘配置将造成严重安全风险（回调可被伪造）。

**修复思路**：将 `return true` 改为 `throw new BusinessException("支付渠道公钥未配置")`，强制 fail-fast。

**技术方案**：

```java
// PaymentServiceImpl.java#L220-222 修正
private boolean verifyCallbackSign(String sign, String content, String publicKey) {
    if (StringUtils.isBlank(publicKey)) {
        // 修复前：log.warn("公钥未配置，跳过验签"); return true;
        // 修复后：强制抛异常，避免生产环境忘配置导致回调被伪造
        throw new BusinessException("支付渠道公钥未配置，拒绝处理回调");
    }
    return AlipaySignature.rsaCheckContent(content, sign, publicKey, "UTF-8");
}
```

**验证方法**：
1. 不配置 `payment.alipay.public-key`，模拟回调应抛异常。
2. 配置后回调验签正常。
3. 安全测试：伪造回调应被拦截。

**预估工作量**：0.5 人天

---

#### 4.2.17 FIX-004：IM 发送消息/已读/会话创建 REST 接口缺失

**问题描述**：
`ImService` 接口定义了 `sendMessage`、`markMessageRead`、`getOrCreateConversation` 完整能力，但 `ImController` 仅暴露查询接口，核心的发送消息、已读、会话创建均无 REST 入口。

**修复思路**：在 `ImController` 中补充 REST 接口。

**技术方案**：

```java
// ImController.java 补充
@RestController
@RequestMapping("/api/v1/im")
public class ImController {

    @PostMapping("/conversation")
    public Result<Long> createConversation(@RequestBody @Valid ConversationCreateRequest req,
                                            @RequestHeader("X-User-Id") Long userId) {
        Long conversationId = imService.getOrCreateConversation(userId, req.getTargetUserId(), req.getType());
        return Result.success(conversationId);
    }

    @PostMapping("/send")
    public Result<Long> sendMessage(@RequestBody @Valid ImMessageSendRequest req,
                                     @RequestHeader("X-User-Id") Long userId) {
        Long messageId = imService.sendMessage(userId, req.getConversationId(), req.getContent(), req.getType());
        return Result.success(messageId);
    }

    @PutMapping("/message/{id}/read")
    public Result<Boolean> markRead(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        imService.markMessageRead(userId, id);
        return Result.success(true);
    }
}
```

**验证方法**：IM 发送消息、已读、创建会话接口可用。

**预估工作量**：2.0 人天

---

#### 4.2.18 FIX-005：Spring MVC 内置异常未显式处理

**问题描述**：
`HttpMessageNotReadableException`、`MaxUploadSizeExceededException`、`HttpRequestMethodNotSupportedException`、`HttpMediaTypeNotSupportedException` 等 Spring MVC 内置异常未显式处理，被兜底 `Exception` 处理返回 500。

**修复思路**：
在 `GlobalExceptionHandler` 中新增对应异常处理器。

**技术方案**：

```java
// GlobalExceptionHandler.java 新增
@ExceptionHandler(HttpMessageNotReadableException.class)
public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
    log.warn("请求体格式错误: {}", e.getMessage());
    return Result.fail(ResultCode.BAD_REQUEST);  // 400
}

@ExceptionHandler(MaxUploadSizeExceededException.class)
public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
    log.warn("文件上传超限: {}", e.getMessage());
    return Result.fail(ResultCode.FILE_TOO_LARGE);  // 400
}

@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
    log.warn("方法不支持: {}", e.getMessage());
    return Result.fail(ResultCode.METHOD_NOT_ALLOWED);  // 405
}

@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
public Result<Void> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
    log.warn("媒体类型不支持: {}", e.getMessage());
    return Result.fail(ResultCode.UNSUPPORTED_MEDIA_TYPE);  // 415
}

@ExceptionHandler(TimeoutException.class)
public Result<Void> handleTimeout(TimeoutException e) {
    log.warn("请求超时: {}", e.getMessage());
    return Result.fail(ResultCode.REQUEST_TIMEOUT);  // 408
}

@ExceptionHandler(BlockException.class)
public Result<Void> handleSentinelBlock(BlockException e) {
    log.warn("Sentinel 限流: {}", e.getMessage());
    return Result.fail(ResultCode.TOO_MANY_REQUESTS);  // 429
}
```

**验证方法**：异常场景 18（数据格式错误）应从 500 变为 400；异常场景 19（文件超限）应从 500 变为 400。

**预估工作量**：0.5 人天

---

#### 4.2.19 FIX-006：IM WebSocket 实时推送未实现

**问题描述**：
全工程未发现 `@MessageMapping` 或 WebSocket 配置，IM 实时推送未实现，IM 功能基本不可用。

**修复思路**：
集成 Spring WebSocket 或 Netty，实现 `@MessageMapping("/im/send")` 实时推送。

**技术方案**：

```java
// 1. 新增 WebSocket 配置
@Configuration
@EnableWebSocketMessageBroker
public class ImWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/im").setAllowedOriginPatterns("*").withSockJS();
    }
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setUserDestinationPrefix("/user");
    }
}

// 2. 消息控制器
@Controller
public class ImWebSocketController {
    @MessageMapping("/im/send")
    public void sendMessage(ImMessageSendRequest req, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        imService.sendMessage(userId, req.getConversationId(), req.getContent(), req.getType());
        // 推送给接收方
        messagingTemplate.convertAndSendToUser(req.getTargetUserId().toString(),
            "/queue/messages", transformMessage(saved));
    }
}

// 3. 连接认证拦截器
@Component
public class ImAuthInterceptor implements ChannelInterceptor {
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            // 校验 Sa-Token
            Object loginId = StpUtil.getLoginIdByToken(token.replace("Bearer ", ""));
            accessor.getSessionAttributes().put("userId", Long.parseLong(loginId.toString()));
        }
        return message;
    }
}
```

**验证方法**：WebSocket 连接测试、消息实时推送测试、多端在线接收测试。

**预估工作量**：5.0 人天（新功能开发）

---

#### 4.2.20 FIX-007：Academy 课程 CRUD 完全缺失

**问题描述**：
`CourseController` 仅提供列表/详情/章节查询，课程创建/更新/删除 CRUD 完全缺失，覆盖率仅 50%。

**修复思路**：在 `CourseController` 中补充 `POST/PUT/DELETE /api/v1/academy/courses` 接口。

**技术方案**：

```java
// CourseController.java 补充
@RestController
@RequestMapping("/api/v1/academy")
public class CourseController {

    @PostMapping("/courses")
    @SaCheckRole("admin")
    public Result<Long> createCourse(@RequestBody @Valid CourseCreateRequest req) {
        Long id = courseService.createCourse(req);
        return Result.success(id);
    }

    @PutMapping("/courses/{id}")
    @SaCheckRole("admin")
    public Result<Boolean> updateCourse(@PathVariable Long id, @RequestBody @Valid CourseUpdateRequest req) {
        courseService.updateCourse(id, req);
        return Result.success(true);
    }

    @DeleteMapping("/courses/{id}")
    @SaCheckRole("admin")
    public Result<Boolean> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);  // 逻辑删除
        return Result.success(true);
    }

    @PostMapping("/courses/{id}/chapters")
    @SaCheckRole("admin")
    public Result<Long> addChapter(@PathVariable Long id, @RequestBody @Valid ChapterCreateRequest req) {
        Long chapterId = courseService.addChapter(id, req);
        return Result.success(chapterId);
    }
}
```

**验证方法**：课程 CRUD 接口测试，管理员角色校验测试。

**预估工作量**：2.0 人天

---

### 4.3 P2 一般级问题修复方案概要（14 项 + 审计补充）

> P2 问题采用概要形式，详细方案在实施时补充。

| 问题编号 | 问题摘要 | 修复思路 | 预估工作量 |
|----------|----------|----------|------------|
| PD-RV-001 | 用户状态魔法数字 0 | 定义 `UserStatusEnum`，替换全工程 `getStatus() == 0` 等魔法数字 | 0.5 人天 |
| PD-RV-002 | API 路径违反 RESTful 规范 | 分阶段迁移至 `/api/v1/{复数资源}`，与 PD-FE-002~007 联动 | 3.0 人天 |
| PD-RV-003 | 已弃用控制器指向不存在的 v1 | 补齐 v1 控制器或移除未落地的弃用标注 | 1.0 人天 |
| PD-RV-004 | 网关 YAML 与 Java Config 路由重复 | 删除 application.yml 重复路由，保留 Java Config 唯一权威 | 0.25 人天 |
| PD-PB-001 | vue-vendor chunk 超阈值 | 将 element-plus 独立为 `element-vendor` chunk | 0.5 人天 |
| PD-PB-002 | 订单缓存 Redis 未批量化 | 为 `HotOrderCache` 增加批量 put/get 接口，循环改批处理 | 1.5 人天 |
| PD-SV-005 | dev/staging useSSL=false | 统一启用 `useSSL=true` 并配置信任库 | 1.0 人天 |
| PD-SV-007 | dev/staging 默认密码 | 所有密码使用 `${VAR:?required}` 强制注入 | 1.0 人天 |
| PD-SV-008 | 管理端口暴露 | 仅暴露必要端口，生产用 bridge + 内部网络 | 1.0 人天 |
| PD-SV-009 | 监控组件 host 网络模式 | 改用 bridge 网络 + 端口映射 | 0.5 人天 |
| PD-SV-010 | Nacos 默认密码 nacos | 使用强随机密码并通过环境变量注入 | 0.25 人天 |
| PD-SV-011 | refresh_token 存 sessionStorage | 迁移至 httpOnly cookie，前端不再持久化 | 2.0 人天 |
| PD-DM-001 | api-overview.md 路径不一致 | 以后端实际路由为准修订文档 | 1.0 人天 |
| PD-DM-002 | api-overview.md 弃用标注误导 | 补齐 v1 接口或修正文档移除弃用标注 | 0.25 人天 |
| FIX-008 | 商家商品/订单接口缺失 | 新增 `MerchantProductController`、`MerchantOrderController`，通过 Feign 调用 | 3.0 人天 |
| FIX-009 | 废弃 Controller 未清理 | 验证客户端迁移后移除 `UserController`/`ProductController`/`OrderController` | 1.0 人天 |
| FIX-010 | AlipayService 强转 | 在 `AlipayService` 接口新增 `createQrCodePayment` 等方法签名 | 1.0 人天 |
| OPT-001 | 并发冲突未用乐观锁 | `OrderInfo` 增加 `@Version` 字段，启用 MyBatis-Plus 乐观锁 | 1.0 人天 |
| OPT-002 | 库存预扣减无 Redis 预校验 | `deductStock` 前增加 Redis 库存预校验 | 1.0 人天 |
| OPT-003 | getProductStats 硬编码 | 实现真实统计查询 | 0.5 人天 |
| OPT-004 | getSeckillProducts 空实现 | 对接 marketing 模块 `SeckillService` | 0.5 人天 |
| OPT-005 | 鉴权方式不统一 | 统一 `@SaCheckLogin` + `StpUtil.getLoginIdAsLong()` | 1.0 人天 |
| OPT-006 | loginByCode 用 Map 接收参数 | 改为 DTO + `@Valid` | 0.5 人天 |
| OPT-007 | ResultCode 缺细粒度业务码 | 新增 `INSUFFICIENT_STOCK` 等业务码 | 0.5 人天 |
| OPT-008 | 公共上传接口缺 @SaCheckLogin | `FileUploadController` 类上添加 `@SaCheckLogin` | 0.25 人天 |

---

### 4.4 P3 轻微级问题修复方案概要（5 项 + 长期规划）

| 问题编号 | 问题摘要 | 修复思路 | 预估工作量 |
|----------|----------|----------|------------|
| PD-TD-002 | catch 子句 any 类型 | 改为 `catch (error: unknown)` + `instanceof` 类型守卫 | 0.5 人天 |
| PD-PB-003 | terser 压缩慢于 esbuild | 评估迁移至 `minify: 'esbuild'`，CI 关闭 `reportCompressedSize` | 0.5 人天 |
| PD-DM-003 | 各服务 README 缺 API 契约 | 补充各服务 API 契约摘要或链接至 Knife4j | 2.0 人天 |
| PD-DM-004 | 缺前后端 API 契约对齐文档 | 新增前后端 API 契约矩阵文档，CI 校验前端调用路径在后端有对应路由 | 2.0 人天 |
| PD-DM-005 | README 空密码占位符 | 改为 `export MYSQL_PASSWORD=<your-password>` 占位提示 | 0.25 人天 |
| LNG-001 | 推广分销模块缺失 | marketing 新增 `PromotionController`，实现分销关系链 + 佣金计算 | 5.0 人天 |
| LNG-002 | Academy 模块功能薄弱 | 补充章节内容管理、学习进度跟踪、评论、问答 | 5.0 人天 |
| LNG-003 | 网关动态路由迁移 | 完成 Nacos 动态路由配置，移除 Java Config 硬编码 | 3.0 人天 |
| LNG-004 | 下游服务校验 HMAC 签名 | 实现 `X-User-Id-Signature` 校验拦截器 | 2.0 人天 |
| LNG-005 | 全链路追踪 | 集成 SkyWalking，完善链路追踪 ID 透传 | 3.0 人天 |

---

## 5. 分阶段实施策略

### 5.1 第一阶段（P0）：阻断级修复

| 项目 | 内容 |
|------|------|
| **目标** | 恢复核心业务链路（登录、下单、地址、网关路由）100% 可用 |
| **时间窗口** | T+1 天内完成修复与验证 |
| **范围** | 4 项 P0 问题：PD-FE-001/LD-002、PD-FE-002、PD-FE-004、FIX-001/PD-FE-010 |
| **负责人** | 后端负责人（登录/网关）+ 前端负责人（订单/地址）+ QA 负责人（验证） |
| **人力配置** | 3 人并行（后端 2 + 前端 1） |
| **工作量** | 7.5 人天（并行后日历周期约 1 天） |
| **修复内容** | 1. 微信登录状态校验反转修正 + 常量化<br>2. 订单 API 6 项接口契约对齐 + payType 参数补传<br>3. 地址 API 5 项接口路径对齐网关路由<br>4. 网关 v1 路由全量补充 + 硬编码 localhost 改 lb:// |
| **验证标准** | 1. 微信登录：正常用户可登录，禁用用户被拦截（单元测试 3 场景通过）<br>2. 订单链路：E2E-SHOP-007/008/009/010 全通过<br>3. 地址链路：结算页地址加载/新增/默认设置 E2E 通过<br>4. 网关路由：IT-GW-001~009 全通过，无 404<br>5. 容器化部署：Docker/k8s 下 user/product/payment/ai/copyright/im/academy 经网关可达<br>6. 静态检查：Grep 无 `localhost`/`host.docker.internal`/`getStatus() == 0` 残留 |
| **发布门禁** | P0 全部修复并通过验证方可发布；否则回滚 |
| **回滚预案** | 修复 PR 独立分支，灰度发布后全量；异常即回滚至上一稳定版本 |

### 5.2 第二阶段（P1）：严重级修复

| 项目 | 内容 |
|------|------|
| **目标** | 消除高危安全漏洞、统一前后端 API 契约、补齐异常处理与缺失功能 |
| **时间窗口** | T+7 天内完成修复与验证 |
| **范围** | 24 项 P1 问题（18 项问题清单 + 6 项审计补充） |
| **负责人** | 后端各模块负责人 + 前端负责人 + 安全负责人 + QA 负责人 |
| **人力配置** | 4 人并行（后端 2 + 前端 1 + 安全/部署 1） |
| **工作量** | 28.0 人天（并行后日历周期约 7 天） |
| **修复内容** | **功能契约（10 项）**：PD-FE-003/005/006/007/008/011、PD-TD-001/003、PD-LD-003/004<br>**网关路由（2 项）**：PD-FE-009（并入 P0）、PD-FE-010（并入 P0）<br>**安全漏洞（5 项）**：PD-SV-001/002/003/004/006<br>**逻辑缺陷（1 项）**：PD-LD-001（支付双重路径）<br>**异常处理（2 项）**：FIX-002（Sa-Token）、FIX-005（Spring MVC）<br>**安全（1 项）**：FIX-003（支付验签）<br>**功能缺失（3 项）**：FIX-004（IM REST）、FIX-006（IM WebSocket）、FIX-007（Academy CRUD） |
| **验证标准** | 1. 前后端 API 契约一致率 ≥98%（Postman/Apifox 全量验证）<br>2. 安全漏洞：Grep 无硬编码密钥/密码残留；Actuator metrics 端点 404；支付验签强制校验<br>3. 异常处理覆盖率 ≥95%（异常场景 1~25 全部正确处理）<br>4. IM 模块：发送消息/已读/会话创建 REST 接口可用；WebSocket 实时推送可用<br>5. Academy 模块：课程 CRUD 接口可用，覆盖率从 50% 提升至 100%<br>6. 支付链路：单一路径，无绕过付款直接发货<br>7. GraphQL：mutation/query 全部正常，无 TypeError |
| **发布门禁** | P1 全部修复并通过验证方可发布；IM WebSocket 与 Academy CRUD 可单独灰度 |

### 5.3 第三阶段（P2）：一般级修复

| 项目 | 内容 |
|------|------|
| **目标** | 消除规范违规、性能瓶颈、次要安全风险，提升工程质量 |
| **时间窗口** | T+30 天内完成修复与验证 |
| **范围** | 32 项 P2 问题（14 项问题清单 + 18 项审计补充） |
| **负责人** | 各模块负责人 + 前端负责人 + 部署负责人 |
| **人力配置** | 3 人并行 |
| **工作量** | 24.75 人天（并行后日历周期约 10 天） |
| **修复内容** | **规范违规（4 项）**：PD-RV-001~004（魔法数字、RESTful 规范、弃用标注、路由重复）<br>**性能瓶颈（2 项）**：PD-PB-001/002（chunk 拆分、Redis 批量化）<br>**安全漏洞（6 项）**：PD-SV-005/007/008/009/010/011（useSSL、默认密码、端口暴露、host 网络、Nacos 密码、refresh_token）<br>**文档缺失（2 项）**：PD-DM-001/002（API 文档修订）<br>**审计补充（18 项）**：FIX-008/009/010、OPT-001~008（商家接口、废弃 Controller、AlipayService、乐观锁、库存预校验、统计、秒杀、鉴权统一、Map 参数、ResultCode、上传鉴权） |
| **验证标准** | 1. 编码规范检查：Grep 无魔法数字、无单数 API 路径（v1 迁移完成）<br>2. 性能：vue-vendor chunk < 500KB；订单缓存 Redis 调用批量化<br>3. 安全：dev/staging 无默认密码；管理端口仅内部访问；refresh_token 在 httpOnly cookie<br>4. 文档：api-overview.md 与前端调用一致；契约矩阵文档 CI 校验通过<br>5. 模块审计：商家商品/订单接口可用；废弃 Controller 已清理；AlipayService 无强转 |

### 5.4 第四阶段（P3）：轻微级修复与持续改进

| 项目 | 内容 |
|------|------|
| **目标** | 代码整洁度优化、文档完善、长期功能规划 |
| **时间窗口** | T+90 天内完成修复与验证 |
| **范围** | 10 项 P3 问题（5 项问题清单 + 5 项长期规划） |
| **负责人** | 各模块负责人 + 前端负责人 + 架构师 |
| **人力配置** | 2 人并行 |
| **工作量** | 15.25 人天（并行后日历周期约 8 天） |
| **修复内容** | **类型/构建（2 项）**：PD-TD-002（any → unknown）、PD-PB-003（terser → esbuild）<br>**文档（3 项）**：PD-DM-003/004/005（README 完善、契约文档、密码占位符）<br>**长期规划（5 项）**：LNG-001~005（推广分销、Academy 完善、动态路由、HMAC 校验、全链路追踪） |
| **验证标准** | 1. 类型安全：Grep 无 `catch (error: any)` 残留<br>2. 构建：CI 构建速度提升 ≥30%<br>3. 文档：各服务 README 含 API 契约摘要；契约矩阵文档完善<br>4. 长期规划：推广分销/Academy 完善/动态路由/HMAC 校验/全链路追踪按规划落地 |

---

## 6. 每周进度跟踪与问题反馈机制

### 6.1 周报模板

```markdown
# Tailor IS 修复进度周报（第 N 周）

> 报告时间：YYYY-MM-DD ~ YYYY-MM-DD
> 报告人：XXX
> 阶段：P0 / P1 / P2 / P3

## 一、本周完成情况

### 1.1 计划完成项
| 问题编号 | 问题摘要 | 计划完成日 | 实际完成日 | 状态 | 负责人 | 验证结果 |
|----------|----------|------------|------------|------|--------|----------|
| PD-FE-001 | 微信登录状态校验反转 | 2026-07-02 | 2026-07-02 | ✅ 完成 | 张三 | 单元测试 3 场景通过 |
| PD-FE-002 | 订单 API 全面不匹配 | 2026-07-03 | 2026-07-03 | ✅ 完成 | 李四 | E2E 全通过 |
| ... | ... | ... | ... | ... | ... | ... |

### 1.2 进度统计
- 本周计划完成：X 项
- 本周实际完成：Y 项
- 完成率：Y/X = Z%
- 累计完成：M/41 项（M%）

### 1.3 工作量消耗
- 计划工作量：X 人天
- 实际消耗：Y 人天
- 偏差：±Z%

## 二、未完成项与延期分析

| 问题编号 | 问题摘要 | 计划完成日 | 延期天数 | 延期原因 | 调整后计划 | 负责人 |
|----------|----------|------------|----------|----------|------------|--------|
| PD-LD-001 | 订单支付双重路径 | 2026-07-05 | 2 天 | 跨服务改造复杂度超预期 | 2026-07-07 | 王五 |

## 三、下周计划

| 问题编号 | 问题摘要 | 计划完成日 | 负责人 | 风险点 |
|----------|----------|------------|--------|--------|
| PD-SV-001 | AES 密钥硬编码 | 2026-07-08 | 赵六 | Vault 集成需运维支持 |
| ... | ... | ... | ... | ... |

## 四、风险与问题

### 4.1 新增风险
- 风险描述：XXX
- 影响范围：XXX
- 缓解措施：XXX
- 负责人：XXX

### 4.2 阻塞问题
- 阻塞描述：XXX
- 阻塞对象：PD-XXX
- 阻塞原因：XXX
- 升级需求：是否需要升级到架构师/项目经理

## 五、质量指标跟踪

| 指标 | 上周值 | 本周值 | 目标值 | 趋势 |
|------|--------|--------|--------|------|
| 模块质量综合评分 | 7.71 | 7.85 | ≥8.50 | ↑ |
| 前后端 API 契约一致率 | 60% | 85% | ≥98% | ↑ |
| 高危安全漏洞数 | 11 | 6 | 0 | ↓ |
| 异常处理覆盖率 | 72% | 85% | ≥95% | ↑ |
| 功能点覆盖率 | 90.4% | 92% | ≥96% | ↑ |
| IM 模块评分 | 4.30 | 4.30 | ≥7.00 | — |
| Academy 模块评分 | 5.35 | 5.35 | ≥7.50 | — |

## 六、经验总结
- 本周最佳实践：XXX
- 本周踩坑记录：XXX
- 流程改进建议：XXX
```

### 6.2 问题反馈流程

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  修复中发现新问题  │ ──► │  记录至问题清单   │ ──► │  评估严重级别    │
│  （开发/测试）    │     │  （新增 PD-XXX） │     │  （P0/P1/P2/P3）│
└─────────────────┘     └─────────────────┘     └────────┬────────┘
                                                         │
                                                         ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  关闭问题并归档   │ ◄── │  验证修复效果     │ ◄── │  分配负责人修复   │
│  （周报同步）    │     │  （QA 回归测试）  │     │  （按优先级排期） │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

**反馈渠道**：
1. **即时反馈**：修复过程中发现的新问题立即提交至问题清单（新增编号 PD-FE-012 等），并通知 QA 负责人评估。
2. **每日站会**：每日 15 分钟站会同步进展与阻塞，阻塞问题当日升级。
3. **周报同步**：每周五 18:00 前提交周报，同步至项目群与质量保障团队。
4. **问题看板**：使用 Jira/GitLab Issue 跟踪每个问题的状态（待修复/修复中/待验证/已关闭）。

### 6.3 风险升级机制

| 升级级别 | 触发条件 | 升级路径 | 响应时限 |
|----------|----------|----------|----------|
| **L1 模块负责人** | 修复延期 ≤2 天 / 技术方案不确定 | 模块负责人 → 技术负责人 | 当日响应 |
| **L2 技术负责人** | 修复延期 >2 天 / 跨模块依赖 / 新增 P0 问题 | 技术负责人 → 架构师 + 项目经理 | 4 小时内响应 |
| **L3 架构师 + 项目经理** | 修复延期 >5 天 / 影响发布 / 安全事件 | 架构师 + 项目经理 → 项目总监 | 2 小时内响应 |
| **L4 项目总监** | 修复延期 >7 天 / 重大生产事故 | 项目总监 → CTO | 1 小时内响应 |

**升级流程**：
1. 发现人填写《风险升级单》（含问题描述、影响范围、已采取措施、需要的支持）。
2. 按升级路径逐级上报，每级响应时限内必须给出决策。
3. 升级决策记录至周报，全团队同步。

### 6.4 质量门禁检查点

| 检查点 | 检查时机 | 检查内容 | 通过标准 | 负责人 |
|--------|----------|----------|----------|--------|
| **CP1 修复前评估** | 修复开始前 | 问题影响范围、修复方案、回滚预案 | 方案评审通过 | 技术负责人 |
| **CP2 代码审查** | PR 提交后 | 代码规范、单元测试覆盖、无新增问题 | Code Review 通过 + ≥2 人 Approve | 模块负责人 |
| **CP3 单元测试** | PR 合并前 | 单元测试通过率、新增测试覆盖 | 通过率 100%，新增代码覆盖率 ≥80% | QA 负责人 |
| **CP4 集成测试** | 阶段验证时 | 接口契约、跨模块集成、异常场景 | 集成测试用例 100% 通过 | QA 负责人 |
| **CP5 安全扫描** | 阶段验证时 | 硬编码密钥/密码、Actuator 端点、依赖漏洞 | 安全扫描 0 高危 | 安全负责人 |
| **CP6 静态检查** | PR 合并前 | 魔法数字、any 类型、localhost 硬编码 | Grep 无残留 | 模块负责人 |
| **CP7 灰度发布** | 全量发布前 | 灰度环境功能验证、监控指标正常 | 灰度验证通过 | 运维负责人 |
| **CP8 阶段验收** | 每阶段结束 | 阶段目标达成、验证标准满足 | 验收报告签字 | 项目经理 |

---

## 7. 修复后预期效果

### 7.1 功能测试通过率目标

| 测试类型 | 当前通过率 | 修复后目标 | 验证方式 |
|----------|------------|------------|----------|
| 单元测试 | ~80% | ≥90% | JaCoCo 覆盖率报告 |
| 集成测试 | ~70% | ≥95% | IT-GW/IT-OP/IT-CB/IT-AI/IT-PI 用例 |
| E2E 测试 | ~60% | ≥95% | E2E-SHOP/E2E-MCH/E2E-CR/E2E-AI 用例 |
| 异常场景测试 | 72%（18/25） | ≥95% | 25 个异常场景 + 新增场景 |
| 接口契约测试 | ~60% | ≥98% | Postman/Apifox 全量接口 |

### 7.2 接口稳定性目标

| 指标 | 当前值 | 修复后目标 | 验证方式 |
|------|--------|------------|----------|
| 核心 API 可用率 | ~75%（P0 阻断） | ≥99.9% | Prometheus + Grafana 监控 |
| API 平均响应时间 | 未量化 | P95 < 500ms | APM 全链路追踪 |
| API 错误率 | ~15%（404/405/400） | <0.1% | 网关日志统计 |
| 网关路由成功率 | ~85% | ≥99.9% | 网关 metrics |
| 前后端契约一致率 | ~60% | ≥98% | 契约矩阵 CI 校验 |

### 7.3 性能指标目标

| 指标 | 当前值 | 修复后目标 | 验证方式 |
|------|--------|------------|----------|
| 首屏加载时间 | >3s（chunk 超阈值） | <2s | Lighthouse |
| vue-vendor chunk 体积 | >500KB | <500KB | 构建产物分析 |
| 订单详情查询缓存命中 | 低（N 次 Redis 调用） | 高（批量调用） | Redis 监控 |
| 构建速度 | 慢（terser） | 提升 ≥30% | CI 构建时长 |
| 库存扣减并发性能 | 数据库行锁 | Redis 预校验 + DB 行锁 | 压测 |

### 7.4 安全指标目标

| 指标 | 当前值 | 修复后目标 | 验证方式 |
|------|--------|------------|----------|
| 高危安全漏洞数 | 11 项 | 0 项 | 安全扫描 + 人工核查 |
| 硬编码密钥/密码数 | 8+ 处 | 0 处 | Grep 全工程扫描 |
| Actuator 暴露端点 | metrics + health 详情 | 仅 health + prometheus（认证） | 端点访问测试 |
| 支付回调验签 | 可跳过 | 强制校验 | 安全渗透测试 |
| refresh_token 存储 | sessionStorage（XSS 风险） | httpOnly cookie | 前端代码审查 |
| 管理端口暴露面 | staging/dev/prod 多端口 | 仅必要端口 | 端口扫描 |
| JDBC 连接安全 | allowPublicKeyRetrieval=true | useSSL=true + SSL 证书 | 连接串核查 |

### 7.5 用户体验目标

| 维度 | 当前状态 | 修复后目标 | 验证方式 |
|------|----------|------------|----------|
| 微信登录 | 完全不可用 | 正常用户 100% 可登录 | 用户测试 |
| 下单支付链路 | 404 阻断 | 全链路顺畅 | E2E-SHOP 全通过 |
| 个人中心 | 多接口 404 | 资料修改/密码/收藏/积分全可用 | 用户测试 |
| 社区功能 | 404 阻断 | 浏览/发帖/评论/点赞全可用 | 用户测试 |
| IM 即时通讯 | 不可用 | 发送/接收/已读实时可用 | 多端测试 |
| 学堂课程管理 | 仅查询 | CRUD 全可用 | 管理员测试 |
| 异常提示 | 部分 500 错误 | 准确的 4xx 状态码 + 业务码 | 异常场景测试 |
| 首屏加载 | >3s | <2s | Lighthouse |

### 7.6 模块质量评分提升预期

| 模块 | 当前评分 | P0 后 | P1 后 | P2 后 | P3 后 | 最终目标 |
|------|----------|-------|-------|-------|-------|----------|
| tailor-is-user | 8.20 | 8.40 | 8.60 | 8.75 | 8.80 | **8.80** |
| tailor-is-product | 7.55 | 7.55 | 8.00 | 8.10 | 8.20 | **8.20** |
| tailor-is-order | 9.00 | 9.10 | 9.20 | 9.25 | 9.30 | **9.30** |
| tailor-is-payment | 8.40 | 8.50 | 8.70 | 8.85 | 8.90 | **8.90** |
| tailor-is-ai | 7.30 | 7.80 | 7.90 | 8.00 | 8.10 | **8.10** |
| tailor-is-copyright | 8.60 | 8.60 | 8.80 | 8.90 | 9.00 | **9.00** |
| tailor-is-merchant | 7.50 | 7.50 | 7.60 | 8.00 | 8.10 | **8.10** |
| tailor-is-marketing | 7.70 | 7.70 | 7.70 | 7.90 | 8.00 | **8.00** |
| tailor-is-community | 8.20 | 8.20 | 8.50 | 8.55 | 8.60 | **8.60** |
| tailor-is-academy | 5.35 | 5.35 | 7.50 | 7.60 | 7.80 | **7.80** |
| tailor-is-supply | 8.20 | 8.20 | 8.20 | 8.20 | 8.20 | **8.20** |
| tailor-is-message | 8.40 | 8.40 | 8.40 | 8.50 | 8.50 | **8.50** |
| tailor-is-message-im | 4.30 | 4.30 | 7.00 | 7.20 | 7.30 | **7.30** |
| tailor-is-core-gateway | 8.55 | 9.00 | 9.00 | 9.10 | 9.10 | **9.10** |
| tailor-is-lite-gateway | 8.55 | 9.00 | 9.00 | 9.10 | 9.10 | **9.10** |
| tailor-is-admin | 8.05 | 8.05 | 8.20 | 8.30 | 8.35 | **8.35** |
| tailor-is-pattern | 7.35 | 7.85 | 7.95 | 8.05 | 8.10 | **8.10** |
| **加权平均** | **7.71** | **7.95** | **8.30** | **8.45** | **8.55** | **≥8.50** |

---

## 8. 附录

### 8.1 修复任务总览表

| 优先级 | 问题数 | 工作量（人天） | 日历周期 | 负责人 | 验证人 |
|--------|--------|----------------|----------|--------|--------|
| P0 阻断级 | 4 | 7.5 | 1 天 | 后端+前端负责人 | QA 负责人 |
| P1 严重级 | 24 | 28.0 | 7 天 | 各模块负责人 | QA 负责人 |
| P2 一般级 | 32 | 24.75 | 30 天 | 各模块负责人 | QA 负责人 |
| P3 轻微级 | 10 | 15.25 | 90 天 | 各模块负责人 | QA 负责人 |
| **合计** | **70** | **75.5** | — | — | — |

### 8.2 关键文件清单

| 文件路径 | 涉及问题 |
|----------|----------|
| `tailor-is/tailor-is-user/src/main/java/com/tailoris/user/service/impl/WechatLoginServiceImpl.java` | PD-FE-001/LD-002、PD-RV-001 |
| `tailor-is-frontend/pc-mall/src/api/order.ts` | PD-FE-002、PD-FE-011 |
| `tailor-is/tailor-is-order/src/main/java/com/tailoris/order/controller/OrderController.java` | PD-FE-002 |
| `tailor-is-frontend/pc-mall/src/api/address.ts` | PD-FE-004 |
| `tailor-is/tailor-is-core-gateway/src/main/java/com/tailoris/coregateway/config/CoreGatewayRouteConfig.java` | FIX-001、PD-FE-009/010、PD-RV-004 |
| `tailor-is/tailor-is-lite-gateway/src/main/java/com/tailoris/litegateway/config/LiteGatewayRouteConfig.java` | FIX-001 |
| `tailor-is-frontend/pc-mall/src/api/cart.ts` | PD-FE-003 |
| `tailor-is/tailor-is-order/src/main/java/com/tailoris/order/controller/CartController.java` | PD-FE-003、PD-LD-003 |
| `tailor-is-frontend/pc-mall/src/api/user.ts` | PD-FE-005 |
| `tailor-is/tailor-is-user/src/main/java/com/tailoris/user/controller/UserController.java` | PD-FE-005、PD-RV-003 |
| `tailor-is-frontend/pc-mall/src/api/product.ts` | PD-FE-006 |
| `tailor-is/tailor-is-product/src/main/java/com/tailoris/product/controller/PublicProductController.java` | PD-FE-006、PD-LD-004 |
| `tailor-is-frontend/pc-mall/src/api/community.ts` | PD-FE-007 |
| `tailor-is/tailor-is-community/src/main/java/com/tailoris/community/controller/PostController.java` | PD-FE-007 |
| `tailor-is-frontend/graphql-gateway/resolvers.ts` | PD-FE-008、PD-TD-001/003、PD-LD-003 |
| `tailor-is/tailor-is-copyright/src/main/java/com/tailoris/copyright/service/impl/CopyrightServiceImpl.java` | PD-SV-001 |
| `deploy/docker-compose.prod.yml` | PD-SV-002、PD-SV-009 |
| `deploy/docker-compose-monitoring.yml` | PD-SV-003 |
| `deploy/docker-compose.services.yml` | PD-SV-004 |
| `deploy/k8s/production/configmap.yaml` | PD-SV-004 |
| `tailor-is/tailor-is-common/src/main/resources/application.yml` | PD-SV-006 |
| `tailor-is/tailor-is-order/src/main/java/com/tailoris/order/service/impl/OrderServiceImpl.java` | PD-LD-001、PD-PB-002 |
| `tailor-is/tailor-is-common/src/main/java/com/tailoris/common/exception/GlobalExceptionHandler.java` | FIX-002、FIX-005 |
| `tailor-is/tailor-is-payment/src/main/java/com/tailoris/payment/service/impl/PaymentServiceImpl.java` | FIX-003 |
| `tailor-is/tailor-is-message-im/src/main/java/com/tailoris/messageim/controller/ImController.java` | FIX-004 |
| `tailor-is/tailor-is-academy/src/main/java/com/tailoris/academy/controller/CourseController.java` | FIX-007 |

### 8.3 验证用例索引

| 用例集 | 用例数 | 关联问题 | 验证阶段 |
|--------|--------|----------|----------|
| IT-GW（网关路由） | 9 | FIX-001、PD-FE-009/010 | P0 |
| IT-OP（订单支付） | 8 | PD-FE-002、PD-LD-001、FIX-003 | P0/P1 |
| IT-CB（版权区块链） | 6 | PD-SV-001 | P1 |
| IT-AI（AI 制版） | 5 | FIX-001（AI 路由） | P0 |
| IT-PI（商品库存） | 6 | PD-FE-006、OPT-002 | P1/P2 |
| E2E-SHOP（C 端购物） | 14 | PD-FE-001~011 | P0/P1 |
| E2E-MCH（商家运营） | 12 | FIX-008 | P2 |
| E2E-CR（版权存证） | 10 | PD-SV-001 | P1 |
| E2E-AI（AI 图案） | 9 | FIX-001 | P0 |
| 异常场景（25 项） | 25 | FIX-002、FIX-005 | P1 |

### 8.4 参考资料

1. `docs/reports/POST-DEPLOYMENT-ISSUE-INVENTORY.md` - 阶段二问题清单（41 项）
2. `docs/reports/MODULE-QUALITY-AUDIT-REPORT.md` - 阶段三模块质量审计报告
3. `tailor-is/docs/CODING_STANDARDS.md` - 编码规范（§1.3 常量命名、§6.1 RESTful 路径规范）
4. `docs/api/api-overview.md` - API 概览文档（待修订）

### 8.5 修订记录

| 版本 | 时间 | 修订人 | 修订内容 |
|------|------|--------|----------|
| v1.0 | 2026-07-02 | 质量保障团队 | 初版发布，覆盖 41 项问题清单 + 15 项审计发现 |

---

**文档结束**

> 本方案基于 2026-07-02 时的代码状态与问题清单编制。所有修复方案均通过静态代码审查得出，建议结合集成测试与压力测试进一步验证。修复过程中如发现新问题，请按 §6.2 问题反馈流程记录并纳入本方案。
