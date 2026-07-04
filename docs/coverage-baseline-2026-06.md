# Tailor IS - 单元测试覆盖率基线报告

> **报告日期**: 2026-06-23
> **基线版本**: v1.0.0
> **测试模块**: tailor-is-common (公共组件)

## 一、总体覆盖率

| 指标 | 数值 | 阈值 | 状态 |
|------|------|------|------|
| INSTRUCTION | 64.34% | 60% | ✅ 达标 |
| BRANCH | 63.36% | 50% | ✅ 达标 |
| LINE | **61.90%** | 60% | ✅ 达标 |
| COMPLEXITY | 60.09% | 60% | ✅ 达标 |
| METHOD | 65.86% | 60% | ✅ 达标 |
| CLASS | 56.03% | 70% | ⚠️ 接近 |

## 二、模块覆盖率（部分）

| 模块 | 覆盖率 | 状态 |
|------|--------|------|
| com.tailoris.common.cache | 100% | ✅ |
| com.tailoris.common.crypto | 100% | ✅ |
| com.tailoris.common.util | 88.84% | ✅ |
| com.tailoris.common.exception | 100% | ✅ |
| com.tailoris.common.validator | 0% | ❌ 待补 |
| com.tailoris.common.interceptor | 0% | ❌ 待补 |
| com.tailoris.common.config | 0% | ❌ 待补 |

## 三、下一步

1. ✅ 公共模块达到 60% 目标
2. ❌ 业务模块（user/order/...）需补充测试
3. ❌ validator/interceptor/config 模块需补充
4. CI 已集成覆盖率门禁（build 阻断）

## 四、待办测试用例

- [ ] CrossDbReferenceValidator 完整测试
- [ ] AuthInterceptor 完整测试
- [ ] NacosConfig 测试
- [ ] UserService 业务测试
- [ ] OrderService 业务测试
- [ ] PaymentService 业务测试
