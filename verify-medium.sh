#!/bin/bash
# 验证Medium问题实际状态 (M-005 ~ M-027, M-032 ~ M-036)

echo "========================================"
echo "Medium问题验证报告"
echo "========================================"
echo ""

FIXED=0
NOT_FIXED=0

# M-005: 检查魔法数字
echo "检查 M-005: ProductDetailView price * 1.2 魔法数字..."
if grep -q "price \* 1\.2" tailor-is-frontend/pc-mall/src/views/ProductDetailView.vue 2>/dev/null; then
    echo "  ❌ 未修复: 存在魔法数字1.2"
    NOT_FIXED=$((NOT_FIXED+1))
else
    echo "  ✅ 已修复: 使用常量"
    FIXED=$((FIXED+1))
fi

# M-006: 检查useCountdown
echo "检查 M-006: useCountdown composable提取到shared..."
if [ -f "tailor-is-frontend/shared/composables/useCountdown.ts" ]; then
    echo "  ✅ 已修复: shared包存在"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: shared包不存在"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# M-007: 检查storage/validate提取
echo "检查 M-007: storage.ts/validate.ts提取到shared..."
if [ -f "tailor-is-frontend/shared/utils/storage.ts" ] && [ -f "tailor-is-frontend/shared/utils/validate.ts" ]; then
    echo "  ✅ 已修复: shared包存在"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: shared包不存在"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# M-008: 检查大型组件拆分
echo "检查 M-008: HomeView/CheckoutView拆分为子组件..."
if [ -f "tailor-is-frontend/pc-mall/src/components/CategoryNav.vue" ] && [ -f "tailor-is-frontend/pc-mall/src/components/AddressSelector.vue" ]; then
    echo "  ✅ 已修复: 子组件已创建"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: 子组件不存在"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# M-009: 检查响应式
echo "检查 M-009: ProductDetailView响应式..."
if grep -q "@media.*768px" tailor-is-frontend/pc-mall/src/views/ProductDetailView.vue 2>/dev/null; then
    echo "  ✅ 已修复: 存在响应式断点"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: 无响应式"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# M-010: 检查i18n
echo "检查 M-010: i18n国际化..."
if [ -f "tailor-is-frontend/pc-mall/src/locales/zh-CN.json" ]; then
    echo "  ✅ 已修复: i18n语言包存在"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: 无i18n"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# M-011: 检查error状态
echo "检查 M-011: CheckoutView error状态..."
if grep -q "el-result.*error\|error.*container" tailor-is-frontend/pc-mall/src/views/CheckoutView.vue 2>/dev/null; then
    echo "  ✅ 已修复: error状态已添加"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: 无error状态"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# M-012: 检查响应式grid
echo "检查 M-012: CheckoutView grid响应式..."
if grep -q "grid-template-columns.*1fr" tailor-is-frontend/pc-mall/src/components/AddressSelector.vue 2>/dev/null; then
    echo "  ✅ 已修复: 响应式grid"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: 无响应式grid"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# M-013: 检查IntersectionObserver
echo "检查 M-013: ProductCard使用IntersectionObserver..."
if grep -q "IntersectionObserver" tailor-is-frontend/pc-mall/src/components/ProductCard.vue 2>/dev/null; then
    echo "  ✅ 已修复: 使用IntersectionObserver"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: 仍使用scroll监听器"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# M-014: 检查非空断言
echo "检查 M-014: ProductDetailView 非空断言..."
if grep -q 'prod\!\.id\|product\!\.' tailor-is-frontend/pc-mall/src/views/ProductDetailView.vue 2>/dev/null; then
    echo "  ❌ 未修复: 存在非空断言"
    NOT_FIXED=$((NOT_FIXED+1))
else
    echo "  ✅ 已修复: 无非空断言"
    FIXED=$((FIXED+1))
fi

# M-015: 检查normalizeSkuAttributes
echo "检查 M-015: skuAttributes逻辑复用..."
if grep -q "normalizeSkuAttributes" tailor-is-frontend/pc-mall/src/views/ProductDetailView.vue 2>/dev/null; then
    echo "  ✅ 已修复: 提取了normalizeSkuAttributes"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: 逻辑未复用"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# M-016: 检查分页字段统一
echo "检查 M-016: PageResponse分页字段统一..."
if grep -q "records.*total.*pages.*current.*size" tailor-is-frontend/merchant-admin/src/types/index.ts 2>/dev/null; then
    echo "  ✅ 已修复: 分页字段已统一"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: 分页字段不一致"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# M-017: 检查fetchShopList
echo "检查 M-017: fetchShopList调用真实API..."
if grep -q "getMerchantShops\|/shops/my" tailor-is-frontend/merchant-admin/src/store/user.ts 2>/dev/null; then
    echo "  ✅ 已修复: 调用真实API"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: 仍使用mock数据"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# M-018: 检查评价功能
echo "检查 M-018: ProductDetailView评价功能..."
if [ -f "tailor-is-frontend/pc-mall/src/api/review.ts" ]; then
    echo "  ✅ 已修复: review API已创建"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: 无review API"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# M-019: 检查FinanceWithdraw独立页面
echo "检查 M-019: FinanceWithdraw独立页面..."
if [ -f "tailor-is-frontend/merchant-admin/src/views/finance/FinanceWithdrawView.vue" ]; then
    echo "  ✅ 已修复: 独立页面已创建"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: 无独立页面"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# M-020: 检查@rollup依赖
echo "检查 M-020: @rollup依赖..."
if grep -q "@rollup/rollup-linux-x64-gnu" tailor-is-frontend/pc-mall/package.json 2>/dev/null; then
    echo "  ❌ 未修复: @rollup仍在dependencies"
    NOT_FIXED=$((NOT_FIXED+1))
else
    echo "  ✅ 已修复: @rollup已移除"
    FIXED=$((FIXED+1))
fi

# M-021~M-027: 检查文档
echo "检查 M-021: README.md..."
[ -f "README.md" ] && echo "  ✅ 已修复" && FIXED=$((FIXED+1)) || echo "  ❌ 未修复" && NOT_FIXED=$((NOT_FIXED+1))

echo "检查 M-022: LICENSE..."
[ -f "LICENSE" ] && echo "  ✅ 已修复" && FIXED=$((FIXED+1)) || echo "  ❌ 未修复" && NOT_FIXED=$((NOT_FIXED+1))

echo "检查 M-023: CHANGELOG.md..."
[ -f "CHANGELOG.md" ] && echo "  ✅ 已修复" && FIXED=$((FIXED+1)) || echo "  ❌ 未修复" && NOT_FIXED=$((NOT_FIXED+1))

echo "检查 M-024: CONTRIBUTING.md..."
[ -f "CONTRIBUTING.md" ] && echo "  ✅ 已修复" && FIXED=$((FIXED+1)) || echo "  ❌ 未修复" && NOT_FIXED=$((NOT_FIXED+1))

echo "检查 M-025: 缺失文档补全..."
if [ -f "tailor-is/docs/DEPLOYMENT-GUIDE.md" ] && [ -f "tailor-is/docs/SEATA-SETUP.md" ]; then
    echo "  ✅ 已修复: 文档已补全"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: 文档缺失"
    NOT_FIXED=$((NOT_FIXED+1))
fi

echo "检查 M-026: BACKUP-RECOVERY.md..."
[ -f "tailor-is/docs/BACKUP-RECOVERY.md" ] && echo "  ✅ 已修复" && FIXED=$((FIXED+1)) || echo "  ❌ 未修复" && NOT_FIXED=$((NOT_FIXED+1))

echo "检查 M-027: 1PANEL-DEPLOYMENT.md..."
[ -f "tailor-is/docs/1PANEL-DEPLOYMENT.md" ] && echo "  ✅ 已修复" && FIXED=$((FIXED+1)) || echo "  ❌ 未修复" && NOT_FIXED=$((NOT_FIXED+1))

# M-032: 检查无障碍性
echo "检查 M-032: 无障碍组件集成..."
if grep -q "installA11yDirectives" tailor-is-frontend/pc-mall/src/main.ts 2>/dev/null; then
    echo "  ✅ 已修复: a11y指令已集成"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: 无障碍组件未集成"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# M-033: 检查navigator.connection兼容
echo "检查 M-033: navigator.connection兼容..."
if grep -q "navigator.connection.*else\|connection.*undefined" tailor-is-frontend/mobile-app/utils/networkMonitor.ts 2>/dev/null; then
    echo "  ✅ 已修复: 降级逻辑存在"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: 无降级逻辑"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# M-034: 检查btoa/atob兼容
echo "检查 M-034: base64小程序兼容..."
if grep -q "base64Encode\|base64Decode" tailor-is-frontend/mobile-app/utils/crypto.ts 2>/dev/null; then
    echo "  ✅ 已修复: 兼容函数存在"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: 无兼容函数"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# M-035: 检查索引优化文档
echo "检查 M-035: INDEX-OPTIMIZATION.md..."
[ -f "tailor-is/docs/INDEX-OPTIMIZATION.md" ] && echo "  ✅ 已修复" && FIXED=$((FIXED+1)) || echo "  ❌ 未修复" && NOT_FIXED=$((NOT_FIXED+1))

# M-036: 检查gateway废弃标记
echo "检查 M-036: 旧gateway模块废弃..."
if grep -q "DEPRECATED" tailor-is/pom.xml 2>/dev/null; then
    echo "  ✅ 已修复: 已标记DEPRECATED"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: 未标记废弃"
    NOT_FIXED=$((NOT_FIXED+1))
fi

echo ""
echo "========================================"
echo "Medium问题验证结果汇总"
echo "========================================"
echo "已修复: $FIXED"
echo "未修复: $NOT_FIXED"
echo "总计: $((FIXED+NOT_FIXED))"
if [ $((FIXED+NOT_FIXED)) -gt 0 ]; then
    echo "修复率: $((FIXED*100/(FIXED+NOT_FIXED)))%"
fi
echo "========================================"
