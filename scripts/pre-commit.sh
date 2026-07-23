#!/bin/sh
# ==============================================================================
# Tailor-IS Pre-commit Hook — 自动化质量门禁
# ==============================================================================
# 触发时机: git commit 之前
# 功能: 代码规范检查、安全扫描、路由端口校验
# 安装方式: cp scripts/pre-commit.sh .git/hooks/pre-commit && chmod +x .git/hooks/pre-commit
# ==============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PASS=0
FAIL=0

check_pass() {
    PASS=$((PASS + 1))
    echo "  ${GREEN}✓ PASS${NC}: $1"
}

check_fail() {
    FAIL=$((FAIL + 1))
    echo "  ${RED}✗ FAIL${NC}: $1"
}

echo ""
echo "=============================================="
echo " Tailor-IS Pre-commit Quality Gate"
echo "=============================================="
echo ""

# =============================================================================
# 1. 密钥硬编码检查
# =============================================================================
echo "[1/7] 密钥硬编码检查..."

# 检查 env 文件中是否包含真实密钥（非占位符）
if [ -f "deploy/tailor-is-prod/env/.env.base" ]; then
    if grep -qE "(AES_KEY|JWT_SECRET|GATEWAY_HMAC_SECRET)=[A-Za-z0-9+/=]{20,}" deploy/tailor-is-prod/env/.env.base 2>/dev/null; then
        check_fail ".env.base 中发现硬编码密钥，请使用占位符 <PLEASE_GENERATE>"
    else
        check_pass "env 文件中无硬编码密钥"
    fi
fi

# 检查 Java 代码中是否硬编码密码
HARDCODED_PASSWORD=$(git diff --cached --name-only | grep '\.java$' | xargs grep -l 'password\s*=\s*"[^$]' 2>/dev/null || true)
if [ -n "$HARDCODED_PASSWORD" ]; then
    check_fail "Java 代码中发现硬编码密码: $HARDCODED_PASSWORD"
else
    check_pass "Java 代码中无硬编码密码"
fi

echo ""

# =============================================================================
# 2. 路由端口合规检查
# =============================================================================
echo "[2/7] 路由端口合规检查..."

# 检查 Controller 中是否使用 RoutePortConstant 常量
HARDCODED_ROUTE=$(git diff --cached --name-only | grep 'Controller\.java$' | xargs grep -l '@RequestMapping("/api' 2>/dev/null || true)
if [ -n "$HARDCODED_ROUTE" ]; then
    check_fail "Controller 中发现硬编码路由，请使用 RoutePortConstant: $HARDCODED_ROUTE"
else
    check_pass "Controller 路由使用常量引用"
fi

# 检查配置文件中是否有超出端口区间的端口
PORT_VIOLATION=$(git diff --cached --name-only | grep '\.yml$' | xargs grep -E 'port:\s*(8[0-9]{3}|9[0-9]{3})' 2>/dev/null | grep -v '810[0-7]\|811[0-7]\|9000\|8081' || true)
if [ -n "$PORT_VIOLATION" ]; then
    check_fail "配置文件中发现超出分配区间的端口: $PORT_VIOLATION"
else
    check_pass "端口配置在合规区间内"
fi

echo ""

# =============================================================================
# 3. 异常处理器完整性检查
# =============================================================================
echo "[3/7] 异常处理器完整性检查..."

# 检查子模块异常处理器是否包含 Exception.class 兜底
SUB_MODULE_EXCEPTION_HANDLER=$(git diff --cached --name-only | grep 'ExceptionHandler\.java$' | grep -v 'common/src' || true)
if [ -n "$SUB_MODULE_EXCEPTION_HANDLER" ]; then
    VIOLATION=$(echo "$SUB_MODULE_EXCEPTION_HANDLER" | xargs grep -l '@ExceptionHandler(Exception\.class)' 2>/dev/null || true)
    if [ -n "$VIOLATION" ]; then
        check_fail "子模块异常处理器包含 Exception.class 兜底: $VIOLATION"
    else
        check_pass "子模块异常处理器无 Exception.class 兜底"
    fi
else
    check_pass "无子模块异常处理器变更"
fi

echo ""

# =============================================================================
# 4. 容器命名规范检查
# =============================================================================
echo "[4/7] 容器命名规范检查..."

# 检查 docker-compose 中容器名是否包含 -service 后缀
COMPOSE_CHANGED=$(git diff --cached --name-only | grep 'docker-compose' || true)
if [ -n "$COMPOSE_CHANGED" ]; then
    SERVICE_SUFFIX=$(echo "$COMPOSE_CHANGED" | xargs grep 'container_name.*-service' 2>/dev/null || true)
    if [ -n "$SERVICE_SUFFIX" ]; then
        check_fail "容器名包含 -service 后缀: $SERVICE_SUFFIX"
    else
        check_pass "容器命名符合 tailor-is-* 规范"
    fi

    # 检查是否有 network_mode: host
    HOST_NETWORK=$(echo "$COMPOSE_CHANGED" | xargs grep 'network_mode.*host' 2>/dev/null || true)
    if [ -n "$HOST_NETWORK" ]; then
        check_fail "发现 network_mode: host，请使用 bridge 网络"
    else
        check_pass "无 host 网络模式"
    fi
else
    check_pass "无 docker-compose 变更"
fi

echo ""

# =============================================================================
# 5. 前端 TypeScript 类型检查
# =============================================================================
echo "[5/7] TypeScript 类型检查..."

# 检查是否有 any 类型使用
ANY_TYPE=$(git diff --cached --name-only | grep '\.tsx\?$' | xargs grep -n '\bany\b' 2>/dev/null | grep -v '//.*any\|/\*.*any' || true)
if [ -n "$ANY_TYPE" ]; then
    check_fail "TypeScript 中发现 any 类型: $ANY_TYPE"
else
    check_pass "无 any 类型使用"
fi

echo ""

# =============================================================================
# 6. 数据库连接安全检查
# =============================================================================
echo "[6/7] 数据库连接安全检查..."

# 检查生产环境配置中是否有 allowPublicKeyRetrieval=true
PROD_VIOLATION=$(git diff --cached --name-only | grep 'application-prod\|application\.yml' | xargs grep 'allowPublicKeyRetrieval=true' 2>/dev/null || true)
if [ -n "$PROD_VIOLATION" ]; then
    check_fail "生产环境配置中发现 allowPublicKeyRetrieval=true: $PROD_VIOLATION"
else
    check_pass "生产环境 allowPublicKeyRetrieval 配置安全"
fi

echo ""

# =============================================================================
# 7. API 路由前缀合规检查（/api/v1 标准）
# =============================================================================
echo "[7/7] API 路由前缀合规检查..."

# 检查 Controller 中是否缺少 /v1 前缀
MISSING_V1=$(git diff --cached --name-only | grep 'Controller\.java$' | xargs grep -l '@RequestMapping.*"/api/[^v]' 2>/dev/null || true)
if [ -n "$MISSING_V1" ]; then
    check_fail "Controller 路由缺少 /api/v1 前缀: $MISSING_V1"
else
    check_pass "所有 API 路由使用 /api/v1 标准前缀"
fi

echo ""

# =============================================================================
# 结果汇总
# =============================================================================
echo "=============================================="
echo " 检查结果: ${GREEN}${PASS} 通过${NC} / ${RED}${FAIL} 失败${NC}"
echo "=============================================="

if [ $FAIL -gt 0 ]; then
    echo ""
    echo "${RED}提交被阻断！请修复以上 ${FAIL} 个问题后重新提交。${NC}"
    echo ""
    exit 1
else
    echo ""
    echo "${GREEN}全部检查通过，允许提交。${NC}"
    echo ""
    exit 0
fi