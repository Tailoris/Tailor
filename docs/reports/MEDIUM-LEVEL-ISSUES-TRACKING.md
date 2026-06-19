# Medium 级别问题修复追踪报告

**修复阶段**: W3-W8 (Sprint 1-2)
**起始日期**: 2026-06-17
**责任人**: AI项目助理 + 开发团队
**总问题数**: 67项
**报告版本**: V1.0

---

## 1. 问题分类总览

| 类别 | 数量 | 处理策略 | 计划时间 | 修复状态 |
|------|:---:|---------|:------:|:------:|
| 代码规范类 | 20 | IDE自动修复+Code Review | W1-W8 | 0/20 |
| 配置类 | 8 | 配置文件改造 | W1-W4 | 0/8 |
| 业务逻辑类 | 7 | 业务开发中同步修复 | W3-W10 | 0/7 |
| 前端类 | 16 | 前端Sprint中处理 | W2-W12 | 0/16 |
| CI/CD类 | 8 | 流水线优化 | W2-W6 | 0/8 |
| 测试类 | 8 | 测试编写时同步 | W3-W16 | 0/8 |
| **合计** | **67** | - | - | **0/67** |

---

## 2. 完整问题清单（67项）

### 2.1 代码规范类（20项）

| 编号 | 文件 | 行号 | 问题描述 | 风险等级 | 修复状态 |
|:---:|------|:---:|---------|:---:|:---:|
| B-M01 | [AuthController.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-user/src/main/java/com/tailoris/user/controller/AuthController.java#L90) | L90 | 魔法数字`7`（Bearer前缀长度）应定义为常量 | Medium | ⏳ |
| B-M02 | [SysUserServiceImpl.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-user/src/main/java/com/tailoris/user/service/impl/SysUserServiceImpl.java#L105) | L105 | 魔法数字`7`（手机号截取位置） | Medium | ⏳ |
| B-M03 | [SysUserServiceImpl.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-user/src/main/java/com/tailoris/user/service/impl/SysUserServiceImpl.java#L152) | L152 | 字符串"用户不存在"重复4次 | Medium | ⏳ |
| B-M04 | [SysUserServiceImpl.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-user/src/main/java/com/tailoris/user/service/impl/SysUserServiceImpl.java#L58) | L58 | passwordEncoder命名不规范 | Medium | ⏳ |
| B-M05 | [SysUserServiceImpl.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-user/src/main/java/com/tailoris/user/service/impl/SysUserServiceImpl.java#L17) | L17 | 无用导入：SysUserRole | Medium | ⏳ |
| B-M06 | [SysUserServiceImpl.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-user/src/main/java/com/tailoris/user/service/impl/SysUserServiceImpl.java#L18-L21) | L18-21 | 无用导入：多个Mapper未使用 | Medium | ⏳ |
| B-M07 | [SysUserServiceImpl.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-user/src/main/java/com/tailoris/user/service/impl/SysUserServiceImpl.java#L34) | L34 | 无用导入：TimeUnit | Medium | ⏳ |
| B-M08 | [SysUserServiceImpl.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-user/src/main/java/com/tailoris/user/service/impl/SysUserServiceImpl.java#L56-L58) | L56-58 | 静态属性定义顺序错误 | Medium | ⏳ |
| B-M09 | [AuthController.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-user/src/main/java/com/tailoris/user/controller/AuthController.java#L22) | L22 | 缺少类级别Javadoc | Medium | ⏳ |
| B-M10 | 全部Java文件 | 行1 | CRLF换行符，应为LF | Medium | ⏳ |
| B-M11 | 全部Controller | - | 缺少方法级别Javadoc | Medium | ⏳ |
| B-M12 | 全部Entity | - | 缺少类级别Javadoc | Medium | ⏳ |
| B-M13 | [AddressController.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-user/src/main/java/com/tailoris/user/controller/AddressController.java#L5) | L5 | 无用导入：BusinessException | Medium | ⏳ |
| B-M14 | [SysUserService.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-user/src/main/java/com/tailoris/user/service/SysUserService.java#L3) | L3 | 无用导入：Page | Medium | ⏳ |
| B-M15 | [SysPermissionMapper.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-user/src/main/java/com/tailoris/user/mapper/SysPermissionMapper.java#L14-L16) | L14-16 | 字符串拼接操作符应在行首 | Medium | ⏳ |
| B-M16 | [UserApplication.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-user/src/main/java/com/tailoris/user/UserApplication.java#L8) | L8 | SpringBootApplication类不应公开构造器 | Medium | ⏳ |
| B-M31 | [checkstyle.xml](file:///F:/Tailor/Tailor%20is/tailor-is/checkstyle.xml) | 全文 | 最大违规数设为100过高 | Medium | ⏳ |
| B-M32 | [sonar-project.properties](file:///F:/Tailor/Tailor%20is/tailor-is/sonar-project.properties) | 全文 | Sonar配置缺少排除规则 | Medium | ⏳ |
| B-M39 | [SysUserServiceImpl.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-user/src/main/java/com/tailoris/user/service/impl/SysUserServiceImpl.java#L37) | L37 | Javadoc首句未以句号结尾 | Medium | ⏳ |
| B-M40 | [ci.yml](file:///F:/Tailor/Tailor%20is/tailor-is/.github/workflows/ci.yml#L93) | L93 | staging/qa环境跳过测试不合理 | Medium | ⏳ |

### 2.2 配置类（8项）

| 编号 | 文件 | 行号 | 问题描述 | 风险等级 | 修复状态 |
|:---:|------|:---:|---------|:---:|:---:|
| B-M23 | [docker-compose.yml](file:///F:/Tailor/Tailor%20is/tailor-is/docker-compose.yml#L15) | L15 | MySQL未配置慢查询日志输出路径 | Medium | ⏳ |
| B-M24 | [docker-compose.yml](file:///F:/Tailor/Tailor%20is/tailor-is/docker-compose.yml#L49) | L49 | Redis未配置密码保护 | Medium | ⏳ |
| B-M25 | [docker-compose.yml](file:///F:/Tailor/Tailor%20is/tailor-is/docker-compose.yml#L133) | L133 | Nacos健康检查使用curl但Alpine镜像可能无curl | Medium | ⏳ |
| B-M26 | [prometheus.yml](file:///F:/Tailor/Tailor%20is/tailor-is/prometheus/prometheus.yml) | 全文 | 缺少应用metrics端点采集配置 | Medium | ⏳ |
| B-M27 | [grafana/datasources/prometheus.yml](file:///F:/Tailor/Tailor%20is/tailor-is/grafana/datasources/prometheus.yml) | 全文 | 数据源配置缺少认证 | Medium | ⏳ |
| B-M41 | [docker-compose.yml](file:///F:/Tailor/Tailor%20is/tailor-is/docker-compose.yml#L248-L249) | L248-249 | Grafana密码硬编码为ChangeMe123! | Medium | ⏳ |
| B-M42 | [SkyWalking agent.config](file:///F:/Tailor/Tailor%20is/tailor-is/skywalking/agent.config) | 全文 | SkyWalking Agent配置缺少采样率设置 | Medium | ⏳ |
| B-M43 | [application.yml](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-copyright/src/main/resources/application.yml) | 全文 | 版权模块缺少application-dev.yml环境配置 | Medium | ⏳ |

### 2.3 业务逻辑类（7项）

| 编号 | 文件 | 行号 | 问题描述 | 风险等级 | 修复状态 |
|:---:|------|:---:|---------|:---:|:---:|
| B-M17 | [ProductServiceImpl.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-product/src/main/java/com/tailoris/product/service/impl/ProductServiceImpl.java#L69-L98) | L69-98 | saveProductBaseInfo字段逐一设置，应使用BeanUtils.copyProperties | Medium | ⏳ |
| B-M18 | [ProductServiceImpl.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-product/src/main/java/com/tailoris/product/service/impl/ProductServiceImpl.java#L100-L124) | L100-124 | saveProductSkus方法循环内逐一构建，可优化为Stream API | Medium | ⏳ |
| B-M19 | [OrderServiceImpl.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-order/src/main/java/com/tailoris/order/service/impl/OrderServiceImpl.java#L66-L67) | L66-67 | cartsByShop分组后遍历，可简化为flatMap | Medium | ⏳ |
| B-M20 | [OrderServiceImpl.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-order/src/main/java/com/tailoris/order/service/impl/OrderServiceImpl.java#L140-L147) | L140-147 | TransactionSynchronization内部匿名类可改为Lambda | Medium | ⏳ |
| B-M21 | [PatternGenerateServiceImpl.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-ai/src/main/java/com/tailoris/ai/service/impl/PatternGenerateServiceImpl.java#L58-L78) | L58-78 | SVG生成使用String.format可读性差 | Medium | ⏳ |
| B-M22 | [PatternGenerateServiceImpl.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-ai/src/main/java/com/tailoris/ai/service/impl/PatternGenerateServiceImpl.java#L20) | L20 | 缺少类级别注释说明 | Medium | ⏳ |
| B-M33 | [ProductServiceImpl.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-product/src/main/java/com/tailoris/product/service/impl/ProductServiceImpl.java#L326-L327) | L326-327 | viewCount直接内存+1后更新DB，高并发下不准确 | Medium | ⏳ |
| B-M34 | [ProductServiceImpl.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-product/src/main/java/com/tailoris/product/service/impl/ProductServiceImpl.java#L95) | L95 | 初始好评率硬编码100.00%不合理 | Medium | ⏳ |
| B-M35 | [OrderServiceImpl.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-order/src/main/java/com/tailoris/order/service/impl/OrderServiceImpl.java#L117-L120) | L117-120 | discountAmount和couponAmount硬编码为0 | Medium | ⏳ |
| B-M36 | [SysUserServiceImpl.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-user/src/main/java/com/tailoris/user/service/impl/SysUserServiceImpl.java#L70) | L70 | 用户状态判断用`== 0`应为枚举 | Medium | ⏳ |
| B-M37 | [OrderServiceImpl.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-order/src/main/java/com/tailoris/order/service/impl/OrderServiceImpl.java#L165) | L165 | 订单状态判断应使用状态机模式 | Medium | ⏳ |
| B-M38 | [ProductServiceImpl.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-product/src/main/java/com/tailoris/product/service/impl/ProductServiceImpl.java#L173) | L173 | 商品状态使用魔法数字 | Medium | ⏳ |

> 注：业务逻辑类实际为12项，已涵盖B-M17~B-M22（6项代码质量类业务）和B-M33~B-M38（6项业务逻辑类）。

### 2.4 前端类（16项）

| 编号 | 文件 | 行号 | 问题描述 | 风险等级 | 修复状态 |
|:---:|------|:---:|---------|:---:|:---:|
| F-M01 | [mobile-app/api/request.ts](file:///F:/Tailor/Tailor%20is/tailor-is-frontend/mobile-app/api/request.ts#L37) | L37 | Loading提示文本硬编码"加载中..." | Medium | ⏳ |
| F-M02 | [pc-mall/src/api/request.ts](file:///F:/Tailor/Tailor%20is/tailor-is-frontend/pc-mall/src/api/request.ts#L15) | L15 | 超时时间30秒过长 | Medium | ⏳ |
| F-M03 | [mobile-app/api/request.ts](file:///F:/Tailor/Tailor%20is/tailor-is-frontend/mobile-app/api/request.ts#L25) | L25 | UNAUTH_CODES硬编码 | Medium | ⏳ |
| F-M04 | [merchant-admin/src/i18n/index.ts](file:///F:/Tailor/Tailor%20is/tailor-is-frontend/merchant-admin/src/i18n/index.ts) | 全文 | i18n国际化配置不完整 | Medium | ⏳ |
| F-M05 | [pc-mall/src/styles/responsive.scss](file:///F:/Tailor/Tailor%20is/tailor-is-frontend/pc-mall/src/styles/responsive.scss) | 全文 | 响应式断点不完整 | Medium | ⏳ |
| F-M06 | [merchant-admin/src/styles/responsive.scss](file:///F:/Tailor/Tailor%20is/tailor-is-frontend/merchant-admin/src/styles/responsive.scss) | 全文 | 响应式断点不完整 | Medium | ⏳ |
| F-M07 | [shared/components/SkipNav.vue](file:///F:/Tailor/Tailor%20is/tailor-is-frontend/shared/components/SkipNav.vue) | 全文 | 无障碍SkipNav组件未引用 | Medium | ⏳ |
| F-M08 | [shared/plugins/a11y-directive.ts](file:///F:/Tailor/Tailor%20is/tailor-is-frontend/shared/plugins/a11y-directive.ts) | 全文 | 无障碍指令未全局注册 | Medium | ⏳ |
| F-M09 | [mobile-app/components/skeleton/skeleton.vue](file:///F:/Tailor/Tailor%20is/tailor-is-frontend/mobile-app/components/skeleton/skeleton.vue) | 全文 | 骨架屏组件缺少动画效果 | Medium | ⏳ |
| F-M10 | [pc-mall/src/components/ProductCard.vue](file:///F:/Tailor/Tailor%20is/tailor-is-frontend/pc-mall/src/components/ProductCard.vue) | 全文 | ProductCard缺少图片懒加载 | Medium | ⏳ |
| F-M11 | [mobile-app/api/cart.ts](file:///F:/Tailor/Tailor%20is/tailor-is-frontend/mobile-app/api/cart.ts) | 全文 | 购物车API缺少本地缓存 | Medium | ⏳ |
| F-M12 | [pc-mall/src/utils/format.ts](file:///F:/Tailor/Tailor%20is/tailor-is-frontend/pc-mall/src/utils/format.ts) | 全文 | 工具函数缺少单元测试 | Medium | ⏳ |
| F-M13 | [e2e-tests/playwright.config.ts](file:///F:/Tailor/Tailor%20is/tailor-is-frontend/e2e-tests/playwright.config.ts) | 全文 | Playwright配置缺少多浏览器测试 | Medium | ⏳ |
| F-M14 | [e2e-tests/tests/auth.spec.ts](file:///F:/Tailor/Tailor%20is/tailor-is-frontend/e2e-tests/tests/auth.spec.ts) | 全文 | E2E测试用例过少 | Medium | ⏳ |
| F-M15 | [pc-mall/tsconfig.json](file:///F:/Tailor/Tailor%20is/tailor-is-frontend/pc-mall/tsconfig.json) | 全文 | TypeScript strict模式未启用 | Medium | ⏳ |
| F-M16 | [merchant-admin/tsconfig.json](file:///F:/Tailor/Tailor%20is/tailor-is-frontend/merchant-admin/tsconfig.json) | 全文 | TypeScript strict模式未启用 | Medium | ⏳ |

### 2.5 CI/CD类（8项）

| 编号 | 文件 | 行号 | 问题描述 | 风险等级 | 修复状态 |
|:---:|------|:---:|---------|:---:|:---:|
| B-M28 | [ci.yml](file:///F:/Tailor/Tailor%20is/tailor-is/.github/workflows/ci.yml#L40) | L40 | OWASP检测每次CI都执行，耗时较长 | Medium | ⏳ |
| B-M29 | [ci.yml](file:///F:/Tailor/Tailor%20is/tailor-is/.github/workflows/ci.yml#L59-L64) | L59-64 | 构建产物保留仅7天，不利于回溯 | Medium | ⏳ |
| B-M30 | [cd.yml](file:///F:/Tailor/Tailor%20is/tailor-is/.github/workflows/cd.yml#L110-L121) | L110-121 | 生产部署无回滚机制 | Medium | ⏳ |
| TD-M01 | [prometheus.yml](file:///F:/Tailor/Tailor%20is/tailor-is/prometheus/prometheus.yml) | 全文 | 监控告警阈值未配置 | Medium | ⏳ |
| TD-M02 | [alertmanager.yml](file:///F:/Tailor/Tailor%20is/tailor-is/alertmanager/alertmanager.yml) | 全文 | 告警接收人未配置 | Medium | ⏳ |
| TD-M03 | [docker-compose.yml](file:///F:/Tailor/Tailor%20is/tailor-is/docker-compose.yml) | 全文 | 服务依赖启动顺序优化 | Medium | ⏳ |
| TD-M04 | [Dockerfile](file:///F:/Tailor/Tailor%20is/tailor-is/Dockerfile) | 全文 | 镜像标签应包含Git SHA | Medium | ⏳ |
| TD-M05 | [.github/dependabot.yml](file:///F:/Tailor/Tailor%20is/tailor-is/.github/dependabot.yml) | 全文 | 依赖自动更新未配置 | Medium | ⏳ |

### 2.6 测试类（8项）

| 编号 | 文件 | 行号 | 问题描述 | 风险等级 | 修复状态 |
|:---:|------|:---:|---------|:---:|:---:|
| T-M01 | [UserServiceTest.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-user/src/test/java/com/tailoris/user/service/UserServiceTest.java) | - | UserService测试用例覆盖不全 | Medium | ⏳ |
| T-M02 | [OrderServiceTest.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-order/src/test/java/com/tailoris/order/service/OrderServiceTest.java) | - | 订单状态流转测试缺失 | Medium | ⏳ |
| T-M03 | [ProductServiceTest.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-product/src/test/java/com/tailoris/product/service/ProductServiceTest.java) | - | 库存并发测试缺失 | Medium | ⏳ |
| T-M04 | [MerchantServiceTest.java](file:///F:/Tailor/Tailor%20is/tailor-is/tailor-is-merchant/src/test/java/com/tailoris/merchant/service/MerchantServiceTest.java) | - | 商家入驻流程测试缺失 | Medium | ⏳ |
| T-M05 | 全部Controller | - | Controller Mock测试缺失 | Medium | ⏳ |
| T-M06 | 全部Mapper | - | Mapper测试覆盖率<30% | Medium | ⏳ |
| T-M07 | 工具类 | - | 工具类单元测试覆盖<60% | Medium | ⏳ |
| T-M08 | 配置文件 | - | 缺少配置加载测试 | Medium | ⏳ |

---

## 3. 修复执行计划

### 3.1 时间规划

```
W3 (6/17-6/23): 代码规范类 - 8项 + 配置类 - 3项 = 11项
W4 (6/24-6/30): 业务逻辑类 - 4项 + CI/CD类 - 4项 = 8项
W5 (7/01-7/07): 前端类 - 8项 + 测试类 - 4项 = 12项
W6 (7/08-7/14): 前端类 - 8项 + 业务逻辑类 - 3项 = 11项
W7 (7/15-7/21): 业务逻辑类 - 5项 + 收尾 = 11项
W8 (7/22-7/28): 收尾与回归测试 = 14项
```

### 3.2 资源分配

| 角色 | 投入 | 负责类别 |
|------|:---:|---------|
| 后端开发 | 2人 | 代码规范、业务逻辑、CI/CD |
| 前端开发 | 1人 | 前端类 |
| 测试开发 | 0.5人 | 测试类 |
| DevOps | 0.5人 | CI/CD类 |

### 3.3 验收标准

- [ ] 67项问题100%修复
- [ ] 新增/修改的代码测试覆盖率≥80%
- [ ] 集成测试全部通过
- [ ] 回归测试0新Bug
- [ ] Code Review通过率100%

---

## 4. 风险与缓解

| 风险 | 等级 | 缓解措施 |
|------|:---:|---------|
| 配置类修改影响生产 | 中 | 灰度发布、回滚方案 |
| 业务逻辑重构引入新Bug | 中 | 完整测试覆盖 |
| 前端API超时调整影响体验 | 低 | A/B测试验证 |
| CI/CD流水线调整导致构建失败 | 低 | 保留旧流水线备份 |

---

**报告持续更新中...**
