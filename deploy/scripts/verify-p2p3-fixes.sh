#!/bin/bash
# ============================================================================
# P2/P3 修复验证脚本
# 验证所有 Medium 和 Low 级别架构优化问题的修复状态
# ============================================================================

set -euo pipefail

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

PASSED=0
FAILED=0
TOTAL=0

check() {
    local id="$1"
    local desc="$2"
    local condition="$3"
    ((TOTAL++))
    if eval "$condition"; then
        echo -e "${GREEN}[PASS]${NC} $id: $desc"
        ((PASSED++))
    else
        echo -e "${RED}[FAIL]${NC} $id: $desc"
        ((FAILED++))
    fi
}

echo "=============================================="
echo "  P2/P3 修复验证"
echo "  $(date '+%Y-%m-%d %H:%M:%S')"
echo "=============================================="
echo ""

# ── P2 验证 ──
echo "--- P2 级别问题 ---"

# P2-1: lite-gateway Sentinel
check "P2-1" "lite-gateway Sentinel 依赖" \
    "grep -q 'spring-cloud-starter-alibaba-sentinel' /home/tailor/Tailoris/tailor-is/tailor-is-lite-gateway/pom.xml"

check "P2-1" "lite-gateway Sentinel Gateway 适配器" \
    "grep -q 'spring-cloud-alibaba-sentinel-gateway' /home/tailor/Tailoris/tailor-is/tailor-is-lite-gateway/pom.xml"

check "P2-1" "SentinelGatewayConfig 已创建" \
    "[ -f /home/tailor/Tailoris/tailor-is/tailor-is-lite-gateway/src/main/java/com/tailoris/litegateway/config/SentinelGatewayConfig.java ]"

check "P2-1" "lite-gateway application.yml 含 Sentinel 配置" \
    "grep -q 'sentinel:' /home/tailor/Tailoris/tailor-is/tailor-is-lite-gateway/src/main/resources/application.yml"

# P2-2: TiDB 激活
check "P2-2" "TiDB 配置模板存在" \
    "[ -f /home/tailor/Tailoris/tailor-is/tailor-is-order/src/main/resources/application-tidb.yml ]"

check "P2-2" "prod profile 包含 tidb" \
    "grep -q 'tidb' /home/tailor/Tailoris/tailor-is/tailor-is-order/src/main/resources/application.yml"

# P2-3: 小程序原生组件
check "P2-3" "订单确认页 usingComponents" \
    "grep -A3 'pages/order/confirm' /home/tailor/Tailoris/tailor-is-frontend/mobile-app/src/pages.json | grep -q 'usingComponents'"

check "P2-3" "订单列表页 usingComponents" \
    "grep -A3 'pages/order/list' /home/tailor/Tailoris/tailor-is-frontend/mobile-app/src/pages.json | grep -q 'usingComponents'"

# P2-4: GraphQL Redis 缓存
check "P2-4" "ioredis 依赖已添加" \
    "grep -q 'ioredis' /home/tailor/Tailoris/tailor-is-frontend/graphql-gateway/package.json"

check "P2-4" "cache.ts 已创建" \
    "[ -f /home/tailor/Tailoris/tailor-is-frontend/graphql-gateway/cache.ts ]"

check "P2-4" "resolvers.ts 使用 getOrSet 缓存" \
    "grep -q 'getOrSet' /home/tailor/Tailoris/tailor-is-frontend/graphql-gateway/resolvers.ts"

# P2-5: SSR 构建
check "P2-5" "Dockerfile.ssr 已创建" \
    "[ -f /home/tailor/Tailoris/tailor-is-frontend/pc-mall/Dockerfile.ssr ]"

check "P2-5" "build:ssr 脚本已添加" \
    "grep -q 'build:ssr' /home/tailor/Tailoris/tailor-is-frontend/pc-mall/package.json"

check "P2-5" "SSR 生产环境支持" \
    "grep -q 'isProduction' /home/tailor/Tailoris/tailor-is-frontend/pc-mall/src/server/server.ts"

echo ""
echo "--- P3 级别问题 ---"

# P3-1: @SentinelResource
check "P3-1" "OrderServiceImpl @SentinelResource" \
    "grep -q 'SentinelResource' /home/tailor/Tailoris/tailor-is/tailor-is-order/src/main/java/com/tailoris/order/service/impl/OrderServiceImpl.java"

check "P3-1" "PaymentServiceImpl @SentinelResource" \
    "grep -q 'SentinelResource' /home/tailor/Tailoris/tailor-is/tailor-is-payment/src/main/java/com/tailoris/payment/service/impl/PaymentServiceImpl.java"

check "P3-1" "createOrderBlockHandler 已实现" \
    "grep -q 'createOrderBlockHandler' /home/tailor/Tailoris/tailor-is/tailor-is-order/src/main/java/com/tailoris/order/service/impl/OrderServiceImpl.java"

# P3-2: core-gateway RocketMQ 排除
check "P3-2" "core-gateway 无 RocketMQ 依赖" \
    "! grep -q 'rocketmq' /home/tailor/Tailoris/tailor-is/tailor-is-core-gateway/pom.xml || grep -q '无需 RocketMQ' /home/tailor/Tailoris/tailor-is/tailor-is-core-gateway/pom.xml"

# P3-3: Service Worker
check "P3-3" "sw.js 已创建" \
    "[ -f /home/tailor/Tailoris/tailor-is-frontend/mobile-app/src/static/sw.js ]"

check "P3-3" "Service Worker 含 Cache First 策略" \
    "grep -q 'cacheFirst' /home/tailor/Tailoris/tailor-is-frontend/mobile-app/src/static/sw.js"

# P3-4: 前端架构文档
check "P3-4" "FRONTEND-ARCHITECTURE.md 已创建" \
    "[ -f /home/tailor/Tailoris/tailor-is-frontend/docs/FRONTEND-ARCHITECTURE.md ]"

# P3-5: SSR 生产优化
check "P3-5" "server.ts 生产模式分离" \
    "grep -q 'NODE_ENV.*production' /home/tailor/Tailoris/tailor-is-frontend/pc-mall/src/server/server.ts"

check "P3-5" "server.ts 含健康检查端点" \
    "grep -q '/health' /home/tailor/Tailoris/tailor-is-frontend/pc-mall/src/server/server.ts"

# P3-6: 缓存预热
check "P3-6" "warmupCache 函数已实现" \
    "grep -q 'warmupCache' /home/tailor/Tailoris/tailor-is-frontend/graphql-gateway/cache.ts"

check "P3-6" "index.ts 调用 warmupCache" \
    "grep -q 'warmupCache' /home/tailor/Tailoris/tailor-is-frontend/graphql-gateway/index.ts"

echo ""
echo "=============================================="
echo "  验证结果: $PASSED/$TOTAL 通过"
if [ $FAILED -gt 0 ]; then
    echo -e "  ${RED}$FAILED 项失败${NC}"
else
    echo -e "  ${GREEN}全部通过!${NC}"
fi
echo "=============================================="