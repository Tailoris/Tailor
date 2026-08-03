#!/bin/bash
# =============================================================================
# 仪表盘构建产物校验脚本
# 作用：防止旧版仪表盘代码（如旧Dashboard、旧菜单结构）被意外打包部署
# 触发：前端构建完成后自动执行
# =============================================================================
set -e

PROJECT_DIR="tailor-is-frontend/platform-admin/dist"
FAILED=0
PASS=0
CHECKS=()

log_check() {
    local status=$1
    local message=$2
    if [ "$status" == "PASS" ]; then
        echo "✅ [PASS] $message"
        PASS=$((PASS + 1))
    else
        echo "❌ [FAIL] $message"
        FAILED=$((FAILED + 1))
    fi
    CHECKS+=("$status: $message")
}

echo "=========================================="
echo "仪表盘构建产物校验"
echo "时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="

# 检查1: dist目录存在
if [ -d "$PROJECT_DIR" ]; then
    log_check "PASS" "dist 目录存在"
else
    log_check "FAIL" "dist 目录不存在，可能构建未完成"
    echo "请先执行 cd tailor-is-frontend/platform-admin && npm run build"
    exit 1
fi

cd "$PROJECT_DIR"

# 检查2: 核心组件代码包含在构建产物中
echo ""
echo "--- 检查核心仪表盘组件 ---"

# 遍历所有 JS 文件，检查是否包含关键标识
ALL_JS_FILES=$(find . -name "*.js" -not -path "*/vendor/*" 2>/dev/null)

# 检查 Dashboard 相关标识
DASHBOARD_CHECKS=(
    "StatCard"
    "TrendChart"
    "DonutChart"
    "RankingList"
    "系统概览"
    "营收与订单趋势"
    "订单状态分布"
    "待办任务"
    "快捷入口"
)

for keyword in "${DASHBOARD_CHECKS[@]}"; do
    FOUND=false
    for file in $ALL_JS_FILES; do
        if grep -q "$keyword" "$file" 2>/dev/null; then
            FOUND=true
            break
        fi
    done
    
    if [ "$FOUND" == "true" ]; then
        log_check "PASS" "核心标识存在: $keyword"
    else
        log_check "FAIL" "核心标识缺失: $keyword - 构建产物可能为旧版"
    fi
done

echo ""
echo "=========================================="
echo "校验结果汇总"
echo "通过: $PASS / $((PASS + FAILED))"
echo "失败: $FAILED / $((PASS + FAILED))"

if [ $FAILED -gt 0 ]; then
    echo ""
    echo "🚨 警告: 检测到构建产物异常！"
    echo "可能原因:"
    echo "  1. 旧版仪表盘代码被引入"
    echo "  2. 构建配置被意外修改"
    echo "  3. 缓存污染导致未使用最新代码"
    echo ""
    echo "建议操作:"
    echo "  1. 检查是否有未提交的代码变更: git status"
    echo "  2. 强制清理缓存后重新构建:"
    echo "     cd tailor-is-frontend/platform-admin"
    echo "     rm -rf dist node_modules/.vite"
    echo "     npm run build"
    echo "  3. 检查 DashboardView.vue 是否被意外修改"
    echo "  4. 回退到黄金版本: git checkout dashboard-v2.0-golden -- tailor-is-frontend/platform-admin/src/"
    exit 1
else
    echo ""
    echo "✅ 仪表盘构建产物校验通过！"
    echo "可以安全部署。"
    exit 0
fi
