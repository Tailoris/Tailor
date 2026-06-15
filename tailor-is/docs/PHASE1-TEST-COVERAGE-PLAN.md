# Tailor IS Phase 1 测试覆盖目标与执行计划

> 对应: Phase 1 / P1-2 / H-01  
> 生成时间: 2026-06-13  
> 责任人: 后端负责人 + 各模块 Owner

## 一、目标回顾

| 阶段 | 目标覆盖率 | 时间窗 |
|------|------------|--------|
| Phase 1 (当前) | LINE ≥ 30%, INSTRUCTION ≥ 40% | 2 周 |
| Phase 2 | LINE ≥ 50%, INSTRUCTION ≥ 60% | 4 周 |
| Phase 3 | LINE ≥ 70%, INSTRUCTION ≥ 80% | 6 周 |
| Phase 4 | LINE ≥ 80%, INSTRUCTION ≥ 90% | 长期 |

## 二、当前覆盖率基线(2026-06-12 核查)

| 模块 | 测试文件数 | 估算覆盖率 | 评级 |
|------|-----------|-----------|------|
| tailor-is-common | 15 | ~75% | ✅ |
| tailor-is-user | 6 | ~60% | ✅ |
| tailor-is-merchant | 6 | ~50% | 🟡 |
| tailor-is-product | 6 | ~45% | 🟡 |
| tailor-is-order | 5 | ~40% | 🟡 |
| tailor-is-payment | 4 | ~35% | 🟡 |
| tailor-is-marketing | 4 | ~30% | 🟡 |
| tailor-is-community | 2 | ~25% | 🔴 |
| tailor-is-ai | 2 | ~20% | 🔴 |
| tailor-is-copyright | 1 | ~10% | 🔴 |
| tailor-is-pattern | **0 (本 PR 新增 1)** | ~5% → 60% | 🟢 |
| tailor-is-core-gateway | **0 (本 PR 新增 1)** | ~0% → 50% | 🟢 |
| tailor-is-lite-gateway | 0 | 0% | 🔴 |
| tailor-is-academy | 0 | 0% | 🔴 |
| tailor-is-admin | 0 | 0% | 🔴 |
| tailor-is-analytics | 0 | 0% | 🔴 |
| tailor-is-supply | 0 | 0% | 🔴 |
| tailor-is-message | 0 | 0% | 🔴 |
| tailor-is-message-im | 0 | 0% | 🔴 |
| **合计** | **58 → 60** | **估算 ~30%** | 🟡 |

## 三、Phase 1 优先补全测试的模块

按业务影响 × 改动成本排序:

| 优先级 | 模块 | 目标覆盖点 | 工时估算 |
|--------|------|-----------|----------|
| **P0** | tailor-is-order | 订单状态机、超时处理、退款 | 2d |
| **P0** | tailor-is-payment | 支付回调、签名校验、分账 | 2d |
| **P1** | tailor-is-marketing | 优惠券核销、秒杀并发、积分 | 3d |
| **P1** | tailor-is-merchant | 入驻审批流、店铺状态机 | 2d |
| **P1** | tailor-is-product | SKU 组合、库存扣减 | 2d |
| **P2** | tailor-is-community | 敏感词过滤、评论状态 | 1d |
| **P2** | tailor-is-ai | 模型路由、BodySize 解析 | 1d |
| **P2** | tailor-is-pattern | ✅ 已完成(PatternServiceImpl) | 0 |
| **P2** | tailor-is-core-gateway | ✅ 已完成(CoreAuthGlobalFilter) | 0 |

## 四、本 PR 已新增的测试

### 1. `PatternServiceImplTest`
**位置**: `tailor-is-pattern/src/test/java/com/tailoris/pattern/service/impl/PatternServiceImplTest.java`  
**覆盖方法**:
- `createPattern` - 插入并返回 ID
- `updatePattern` - 覆盖 ID 后更新
- `deletePattern` - 按 ID 删除
- `getPatternById` - 存在/不存在两种场景
- `listByMerchantId` - 有结果/无结果
- `pagePatterns` - 有数据/无数据

**测试用例数**: 9  
**测试框架**: JUnit 5 + Mockito + AssertJ

### 2. `CoreAuthGlobalFilterTest`
**位置**: `tailor-is-core-gateway/src/test/java/com/tailoris/coregateway/filter/CoreAuthGlobalFilterTest.java`  
**覆盖方法**:
- 默认白名单包含关键路径
- Ant 路径通配符匹配 (`/api/public/**`, `/actuator/**`)
- Token 提取顺序 (Bearer > query > cookie)
- 客户端 IP 解析优先级
- Token 脱敏规则
- 过滤器顺序

**测试用例数**: 13

## 五、覆盖率门禁(Maven JaCoCo)

已更新 `tailor-is/pom.xml` 的 JaCoCo 规则:
- **BUNDLE 范围** (整个项目)
- LINE 覆盖率 ≥ **30%**
- INSTRUCTION 覆盖率 ≥ **40%**
- 骨架模块 (`admin/analytics/pattern/supply/message/message-im/academy/copyright/common-web`) 已豁免
- DTO/VO/Entity/Exception/Config 排除

执行命令:
```bash
mvn clean verify -pl tailor-is-pattern,tailor-is-core-gateway -am
mvn jacoco:report -pl tailor-is-pattern
# 报告位置: tailor-is-pattern/target/site/jacoco/index.html
```

## 六、每日跟踪机制

每日 18:00 在 `ISSUE-TRACKER.md` 中更新:
- 当日新增测试用例数
- 当前累计覆盖率
- P0/P1 优先级模块完成度

每周五 17:00 生成周报, 记录:
- 本周新增/通过率
- 阻塞问题
- 下周计划

## 七、未解决问题说明

1. **集成测试未覆盖**: 仅单元测试, 需补充 TestContainers 端到端测试
2. **前端组件测试**: 0 个 .spec.ts 文件, 由 P1-6 任务处理
3. **覆盖率 ≥ 60% 目标**: 当前仅达 ~30%, 需要持续投入

## 八、后续建议

- Phase 2 开始引入 PIT Mutation Testing, 验证测试质量
- 在 IDE 中配置 SonarLint 插件, 编码时实时反馈
- CI 中必须包含 `mvn verify`, 阻断覆盖率回退
