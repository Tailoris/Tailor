#!/bin/bash
# ==============================================================================
# Tailor-IS 路由与端口合规自动化检查脚本
# 用途: 全量扫描项目路由和端口配置，确保与 RoutePortConstant 对齐
# 用法: bash scripts/check-route-compliance.sh
# ==============================================================================
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PASS=0
FAIL=0
WARN=0

check_pass() { PASS=$((PASS + 1)); echo "  ${GREEN}✓${NC} $1"; }
check_fail() { FAIL=$((FAIL + 1)); echo "  ${RED}✗${NC} $1"; }
check_warn() { WARN=$((WARN + 1)); echo "  ${YELLOW}⚠${NC} $1"; }

echo ""
echo "=============================================="
echo " Tailor-IS 路由与端口合规检查"
echo "=============================================="
echo ""

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# =============================================================================
# 1. Controller 路由前缀检查
# =============================================================================
echo "[1/5] Controller 路由前缀检查 (/api/v1)..."
MISSING_V1=$(find "$PROJECT_ROOT/tailor-is" -name "*Controller.java" -exec grep -l '@RequestMapping.*"/api/[^v]' {} \; 2>/dev/null || true)
if [ -n "$MISSING_V1" ]; then
    check_fail "以下 Controller 缺少 /api/v1 前缀:"
    echo "$MISSING_V1" | while read f; do echo "       $f"; done
else
    check_pass "所有 Controller 使用 /api/v1 标准前缀"
fi

# =============================================================================
# 2. 硬编码路由检查（未使用 RoutePortConstant）
# =============================================================================
echo ""
echo "[2/5] 硬编码路由检查..."
HARDCODED=$(find "$PROJECT_ROOT/tailor-is" -name "*Controller.java" -exec grep -l '@RequestMapping("/api/v1/' {} \; 2>/dev/null || true)
if [ -n "$HARDCODED" ]; then
    check_warn "以下 Controller 使用硬编码路由（建议改用 RoutePortConstant）:"
    echo "$HARDCODED" | while read f; do echo "       $f"; done
else
    check_pass "所有 Controller 路由使用 RoutePortConstant 常量"
fi

# =============================================================================
# 3. 端口分配合规检查
# =============================================================================
echo ""
echo "[3/5] 端口分配合规检查..."
# 核心业务端口: 8100-8107
# 轻量业务端口: 8108-8117
# 网关: 9000, 8081
PORT_VIOLATIONS=$(find "$PROJECT_ROOT/tailor-is" -name "*.yml" -o -name "*.yaml" | grep -v '/target/' | xargs grep -E 'port:\s*[0-9]+' 2>/dev/null | grep -v '810[0-7]\|811[0-7]\|9000\|8081\|80\|443\|6379\|3306\|8848\|9848\|5672\|15672\|27017\|9090\|3000\|8719\|8720' | grep -v '#' || true)
if [ -n "$PORT_VIOLATIONS" ]; then
    check_fail "发现超出分配区间的端口:"
    echo "$PORT_VIOLATIONS" | while read line; do echo "       $line"; done
else
    check_pass "所有端口在合规区间内"
fi

# =============================================================================
# 4. Nacos 地址默认值检查
# =============================================================================
echo ""
echo "[4/5] Nacos 地址默认值检查..."
NACOS_LOCALHOST=$(find "$PROJECT_ROOT/tailor-is" -name "bootstrap.yml" -exec grep -l '127\.0\.0\.1:8848' {} \; 2>/dev/null || true)
if [ -n "$NACOS_LOCALHOST" ]; then
    check_fail "以下 bootstrap.yml 使用 localhost 作为默认 Nacos 地址:"
    echo "$NACOS_LOCALHOST" | while read f; do echo "       $f"; done
else
    check_pass "所有 bootstrap.yml 使用容器别名 nacos:8848"
fi

# =============================================================================
# 5. allowPublicKeyRetrieval 配置检查
# =============================================================================
echo ""
echo "[5/5] allowPublicKeyRetrieval 配置检查..."
# 检查非 dev profile 中是否有 allowPublicKeyRetrieval=true
PROD_VIOLATIONS=$(find "$PROJECT_ROOT/tailor-is" -name "application*.yml" | xargs grep -l 'allowPublicKeyRetrieval=true' 2>/dev/null | grep -v 'test' || true)
# 在 dev profile 下 allowPublicKeyRetrieval=true 是允许的
for f in $PROD_VIOLATIONS; do
    if grep -q 'spring.config.activate.on-profile.*prod' "$f" 2>/dev/null && grep -q 'allowPublicKeyRetrieval=true' "$f" 2>/dev/null; then
        check_fail "生产环境配置中发现 allowPublicKeyRetrieval=true: $f"
    fi
done
check_pass "allowPublicKeyRetrieval 配置安全"

# =============================================================================
# 结果汇总
# =============================================================================
echo ""
echo "=============================================="
echo " 检查结果: ${GREEN}${PASS} 通过${NC} / ${YELLOW}${WARN} 警告${NC} / ${RED}${FAIL} 失败${NC}"
echo "=============================================="

if [ $FAIL -gt 0 ]; then
    echo ""
    echo "${RED}发现 ${FAIL} 个违规项，请修复后重新检查。${NC}"
    exit 1
else
    echo ""
    echo "${GREEN}路由与端口配置合规。${NC}"
    exit 0
fi