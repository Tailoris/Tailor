# Tailor IS v4.0 Admin服务待迁移模块清单

## 一、文档说明

**文档版本**: v1.0  
**创建日期**: 2026-07-06  
**最后更新**: 2026-07-06  
**文档状态**: 正式版  
**审核人**: 待指定  
**适用范围**: Tailor IS v4.0 Admin服务中心化迁移项目

---

## 二、迁移状态概览

### 2.1 整体迁移进度

| 阶段 | 状态 | 完成率 | 说明 |
|------|------|--------|------|
| 第一阶段 | ✅ 已完成 | 100% | Order/Product/Community/Merchant Controller迁移 |
| 第二阶段 | 🔄 进行中 | 0% | Analytics/Academy/Supply Controller迁移 |
| 第三阶段 | ⏳ 待启动 | 0% | Admin服务优化与治理 |

### 2.2 当前Admin服务Controller清单

| Controller | 所属服务 | 状态 | 说明 |
|------------|---------|------|------|
| AdminOrderController | tailor-is-admin | ✅ 已迁移 | 从order-service迁移 |
| AdminProductController | tailor-is-admin | ✅ 已迁移 | 从product-service迁移 |
| AdminCommunityController | tailor-is-admin | ✅ 已迁移 | 从community-service迁移 |
| AdminMerchantController | tailor-is-admin | ✅ 已迁移 | 从merchant-service迁移 |
| AdminUserController | tailor-is-admin | ✅ 已有 | 用户管理（核心功能） |
| AdminDashboardController | tailor-is-admin | ✅ 已有 | 数据看板（核心功能） |
| AdminAuditLogController | tailor-is-admin | ✅ 已有 | 审计日志（核心功能） |
| AdminRoleController | tailor-is-admin | ✅ 已有 | 角色权限（核心功能） |
| AdminSystemSettingsController | tailor-is-admin | ✅ 已有 | 系统设置（核心功能） |

---

## 三、待迁移模块详细清单

### 3.1 P1优先级模块

| 模块名称 | 模块标识 | 当前状态 | 优先级 | 预计迁移时间 | 负责人 | 依赖服务 |
|---------|---------|---------|--------|-------------|--------|---------|
| 数据分析管理 | AdminAnalyticsController | 待开发 | P1 | 第3周 | 张三 | analytics-service |
| 纸样设计管理 | AdminPatternController | 待开发 | P1 | 第3周 | 李四 | pattern-service |
| 版权管理 | AdminCopyrightController | 待开发 | P1 | 第3周 | 王五 | copyright-service |

#### 3.1.1 AdminAnalyticsController

**模块描述**: 数据趋势分析服务的管理功能，包括报告管理、数据查询、统计分析等。

**当前状态**: 
- 业务服务: `tailor-is-analytics` 存在，但无Admin相关代码
- Admin服务: 无对应Controller

**需迁移功能**:
| 功能 | API路径 | 说明 |
|------|--------|------|
| 报告列表查询 | GET /api/v1/admin/analytics/reports | 分页查询数据分析报告 |
| 报告详情查询 | GET /api/v1/admin/analytics/reports/{id} | 获取报告详情 |
| 报告生成 | POST /api/v1/admin/analytics/reports | 生成新的分析报告 |
| 报告状态更新 | PUT /api/v1/admin/analytics/reports/{id}/status | 更新报告状态 |
| 数据统计 | GET /api/v1/admin/analytics/statistics | 获取数据统计概览 |

**迁移依赖**:
- Feign Client: 需要创建 `AnalyticsFeignClient`
- DTO定义: 需要定义报告相关DTO
- 权限配置: 需要配置 `analytics:read/write/manage` 权限

**风险评估**:
| 风险 | 概率 | 影响 | 应对策略 |
|------|------|-----|---------|
| 数据量大导致查询慢 | 中 | 高 | 添加缓存策略 |
| 报告生成耗时较长 | 高 | 中 | 使用异步任务处理 |

---

#### 3.1.2 AdminPatternController

**模块描述**: 纸样设计服务的管理功能，包括纸样审核、设计管理、评分管理等。

**当前状态**: 
- 业务服务: `tailor-is-pattern` 存在，但无Admin相关代码
- Admin服务: 无对应Controller

**需迁移功能**:
| 功能 | API路径 | 说明 |
|------|--------|------|
| 纸样列表查询 | GET /api/v1/admin/pattern | 分页查询纸样列表 |
| 纸样详情查询 | GET /api/v1/admin/pattern/{id} | 获取纸样详情 |
| 纸样审核 | PUT /api/v1/admin/pattern/{id}/audit | 审核纸样 |
| 纸样评分 | PUT /api/v1/admin/pattern/{id}/score | 更新纸样评分 |
| 纸样删除 | DELETE /api/v1/admin/pattern/{id} | 删除纸样 |

**迁移依赖**:
- Feign Client: 需要创建 `PatternFeignClient`
- DTO定义: 需要定义纸样相关DTO
- 权限配置: 需要配置 `pattern:read/write/audit` 权限

**风险评估**:
| 风险 | 概率 | 影响 | 应对策略 |
|------|------|-----|---------|
| 纸样文件较大 | 高 | 中 | 使用流式处理 |
| 版权存证同步 | 中 | 高 | 确保版权存证完成后再审核 |

---

#### 3.1.3 AdminCopyrightController

**模块描述**: 版权服务的管理功能，包括版权审核、侵权检测、存证管理等。

**当前状态**: 
- 业务服务: `tailor-is-copyright` 存在，仅有BlockchainConfig配置
- Admin服务: 无对应Controller

**需迁移功能**:
| 功能 | API路径 | 说明 |
|------|--------|------|
| 版权列表查询 | GET /api/v1/admin/copyright | 分页查询版权列表 |
| 版权详情查询 | GET /api/v1/admin/copyright/{id} | 获取版权详情 |
| 版权审核 | PUT /api/v1/admin/copyright/{id}/audit | 审核版权申请 |
| 侵权检测 | POST /api/v1/admin/copyright/{id}/detect | 执行侵权检测 |
| 存证记录查询 | GET /api/v1/admin/copyright/{id}/evidence | 查询存证记录 |

**迁移依赖**:
- Feign Client: 需要创建 `CopyrightFeignClient`
- DTO定义: 需要定义版权相关DTO
- 权限配置: 需要配置 `copyright:read/write/audit` 权限

**风险评估**:
| 风险 | 概率 | 影响 | 应对策略 |
|------|------|-----|---------|
| 区块链调用超时 | 中 | 高 | 配置超时重试机制 |
| AI检测耗时较长 | 高 | 中 | 使用异步任务处理 |

---

### 3.2 P2优先级模块

| 模块名称 | 模块标识 | 当前状态 | 优先级 | 预计迁移时间 | 负责人 | 依赖服务 |
|---------|---------|---------|--------|-------------|--------|---------|
| 线上学堂管理 | AdminAcademyController | 待开发 | P2 | 第4周 | 赵六 | academy-service |
| 供应链管理 | AdminSupplyController | 待开发 | P2 | 第4周 | 钱七 | supply-service |
| 营销活动管理 | AdminMarketingController | 待开发 | P2 | 第4周 | 孙八 | marketing-service |

#### 3.2.1 AdminAcademyController

**模块描述**: 线上学堂服务的管理功能，包括课程管理、讲师管理、学习进度等。

**需迁移功能**:
| 功能 | API路径 | 说明 |
|------|--------|------|
| 课程列表查询 | GET /api/v1/admin/academy/courses | 分页查询课程列表 |
| 课程详情查询 | GET /api/v1/admin/academy/courses/{id} | 获取课程详情 |
| 课程创建 | POST /api/v1/admin/academy/courses | 创建新课程 |
| 课程状态更新 | PUT /api/v1/admin/academy/courses/{id}/status | 更新课程状态 |
| 讲师管理 | GET /api/v1/admin/academy/instructors | 查询讲师列表 |

---

#### 3.2.2 AdminSupplyController

**模块描述**: 供应链服务的管理功能，包括面料管理、供应商管理、采购订单等。

**需迁移功能**:
| 功能 | API路径 | 说明 |
|------|--------|------|
| 面料列表查询 | GET /api/v1/admin/supply/fabrics | 分页查询面料列表 |
| 面料详情查询 | GET /api/v1/admin/supply/fabrics/{id} | 获取面料详情 |
| 供应商管理 | GET /api/v1/admin/supply/suppliers | 查询供应商列表 |
| 供应商审核 | PUT /api/v1/admin/supply/suppliers/{id}/audit | 审核供应商 |
| 采购订单管理 | GET /api/v1/admin/supply/purchase-orders | 查询采购订单 |

---

#### 3.2.3 AdminMarketingController

**模块描述**: 营销活动服务的管理功能，包括优惠券管理、活动管理、促销管理等。

**需迁移功能**:
| 功能 | API路径 | 说明 |
|------|--------|------|
| 优惠券列表查询 | GET /api/v1/admin/marketing/coupons | 分页查询优惠券列表 |
| 优惠券创建 | POST /api/v1/admin/marketing/coupons | 创建优惠券 |
| 活动列表查询 | GET /api/v1/admin/marketing/campaigns | 查询活动列表 |
| 活动状态更新 | PUT /api/v1/admin/marketing/campaigns/{id}/status | 更新活动状态 |
| 促销规则管理 | GET /api/v1/admin/marketing/rules | 查询促销规则 |

---

### 3.3 P3优先级模块

| 模块名称 | 模块标识 | 当前状态 | 优先级 | 预计迁移时间 | 负责人 | 依赖服务 |
|---------|---------|---------|--------|-------------|--------|---------|
| AI服务管理 | AdminAIController | 待开发 | P3 | 第5周 | 周九 | ai-service |
| 消息服务管理 | AdminMessageController | 待开发 | P3 | 第5周 | 吴十 | message-service |
| 支付服务管理 | AdminPaymentController | 待开发 | P3 | 第5周 | 郑十一 | payment-service |

---

## 四、遗留问题处理

### 4.1 业务服务中残留的Admin代码

| 文件路径 | 所属服务 | 状态 | 处理建议 |
|---------|---------|------|---------|
| community/service/CommunityAdminService.java | tailor-is-community | 待删除 | 迁移完成后删除 |
| community/service/impl/CommunityAdminServiceImpl.java | tailor-is-community | 待删除 | 迁移完成后删除 |
| community/service/impl/CommunityAdminServiceImplTest.java | tailor-is-community | 待删除 | 迁移完成后删除 |

**处理步骤**:
1. 确认admin-service中的AdminCommunityController已完整实现所有功能
2. 运行测试验证功能正常
3. 删除业务服务中的Admin相关代码
4. 更新网关路由配置

---

## 五、迁移计划甘特图

```
第1周    第2周    第3周    第4周    第5周    第6周
├────────┼────────┼────────┼────────┼────────┼─────────
│        │        │ P1模块 │ P2模块 │ P3模块 │ 优化治理 │
│        │        │迁移    │迁移    │迁移    │         │
│        │        │        │        │        │         │
│        │        │分析    │学堂    │AI      │         │
│        │        │纸样    │供应链  │消息    │         │
│        │        │版权    │营销    │支付    │         │
```

---

## 六、资源需求

| 资源类型 | 需求数量 | 说明 |
|---------|---------|------|
| 后端开发 | 3人 | 负责Controller和Service实现 |
| 前端开发 | 2人 | 负责管理后台页面开发 |
| 测试人员 | 2人 | 负责功能测试和回归测试 |
| DevOps | 1人 | 负责部署和配置管理 |

---

## 七、交付标准

### 7.1 代码质量标准

| 指标 | 阈值 | 说明 |
|------|------|------|
| 代码覆盖率 | ≥80% | 单元测试覆盖率 |
| 重复代码率 | ≤5% | SonarQube检测 |
| 代码复杂度 | ≤15 | 圈复杂度 |

### 7.2 功能验收标准

| 验收项 | 标准 |
|--------|------|
| API响应时间 | P95 ≤ 200ms |
| 接口可用性 | 99.9% |
| 权限校验 | 所有接口需权限验证 |
| 异常处理 | 统一错误码和错误信息 |

---

## 八、审批流程

| 审批环节 | 审批人 | 预计时间 |
|---------|--------|---------|
| 项目经理审批 | 待指定 | 1个工作日 |
| 技术负责人审批 | 待指定 | 1个工作日 |
| 架构师审批 | 待指定 | 1个工作日 |

---

## 九、版本记录

| 版本 | 日期 | 修改内容 | 修改人 |
|------|------|---------|--------|
| v1.0 | 2026-07-06 | 初始版本 | 系统生成 |