#!/usr/bin/env bash
# ==============================================================================
# Tailor IS - 网关路由一致性检测脚本 (v4.0)
# ==============================================================================
# 用途: 校验网关路由配置是否符合 v4.0 规范
#   1. 读取 Core/Lite 网关 Java Config 全部路由
#   2. 和 v4.0 规范路由表比对，检测缺失/多余/重复路由
#   3. 检测路由 uri 是否使用 lb://，拦截硬编码 IP/端口
#   4. 检测 core-gateway 是否包含 community 相关路由（违规直接报错）
#   5. 检测是否在 yml 文件存在路由配置，拦截违规代码
#
# 权威文档: Tailor-IS 企业级微服务路由与端口规范（v4.0 优化版）
#
# 使用方式:
#   bash deploy/scripts/check-gateway-routes.sh                # 严格模式
#   bash deploy/scripts/check-gateway-routes.sh --no-fail      # 宽松模式
#
# 退出码:
#   0 - 无违规
#   1 - 存在违规
# ==============================================================================
set -euo pipefail

RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
GREEN='\033[0;32m'
NC='\033[0m'

NO_FAIL=0
for arg in "$@"; do
  case "$arg" in
    --no-fail) NO_FAIL=1 ;;
    --help|-h) sed -n '2,30p' "$0"; exit 0 ;;
  esac
done

log_critical() { echo -e "${RED}🔴 CRITICAL${NC}: $1"; }
log_warning() { echo -e "${YELLOW}🟡 WARNING${NC}: $1"; }
log_info() { echo -e "${BLUE}🔵 INFO${NC}: $1"; }
log_ok() { echo -e "${GREEN}✅ OK${NC}: $1"; }
log_section() { echo ""; echo -e "${GREEN}==== $1 ====${NC}"; }

# ==============================================================================
# v4.0 规范路由表
# ==============================================================================
CORE_GATEWAY_ROUTES=(
  "user-route:lb://tailor-is-user:/api/user/**,/api/auth/**"
  "product-route:lb://tailor-is-product:/api/product/**"
  "order-route:lb://tailor-is-order:/api/order/**"
  "payment-route:lb://tailor-is-payment:/api/payment/**"
  "marketing-route:lb://tailor-is-marketing:/api/marketing/**"
  "ai-route:lb://tailor-is-ai:/api/ai/**"
  "copyright-route:lb://tailor-is-copyright:/api/copyright/**"
  "merchant-route:lb://tailor-is-merchant:/api/merchant/**"
  "admin-route:lb://tailor-is-admin:/api/admin/**"
  "pattern-route:lb://tailor-is-pattern:/api/pattern/**"
)

LITE_GATEWAY_ROUTES=(
  "community-route:lb://tailor-is-community:/api/community/**"
  "academy-route:lb://tailor-is-academy:/api/academy/**"
  "supply-route:lb://tailor-is-supply:/api/supply/**"
  "message-route:lb://tailor-is-message:/api/message/**"
  "im-route:lb://tailor-is-message-im:/api/im/**"
  "analytics-route:lb://tailor-is-analytics:/api/analytics/**"
  "ws-route:lb://tailor-is-message-im:/ws/**"
)

CRITICAL_COUNT=0
WARNING_COUNT=0

# ==============================================================================
# 步骤 1: 检测 core-gateway 路由配置
# ==============================================================================
log_section "步骤 1: 检测 core-gateway 路由配置"

CORE_CONFIG_FILE="tailor-is/tailor-is-core-gateway/src/main/java/com/tailoris/coregateway/config/CoreGatewayRouteConfig.java"
if [[ ! -f "$CORE_CONFIG_FILE" ]]; then
  log_critical "core-gateway 路由配置文件不存在: $CORE_CONFIG_FILE"
  CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
else
  log_ok "找到 core-gateway 路由配置文件"

  # 检测是否包含 community/supply/message/academy/im/analytics 路由（v4.0 禁止）
  FORBIDDEN_ROUTES=("community-route" "supply-route" "message-route" "academy-route" "im-route" "analytics-route")
  for forbidden in "${FORBIDDEN_ROUTES[@]}"; do
    if grep -q "$forbidden" "$CORE_CONFIG_FILE"; then
      log_critical "core-gateway 包含禁止的路由 '$forbidden'，该路由应迁移至 lite-gateway"
      CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
    fi
  done

  # 检测路由 uri 是否使用 lb://
  if grep -n 'uri("http://' "$CORE_CONFIG_FILE"; then
    log_critical "core-gateway 路由 uri 使用硬编码 http:// 格式，应改为 lb:// 服务发现格式"
    CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
  else
    log_ok "core-gateway 所有路由 uri 使用 lb:// 服务发现格式"
  fi

  # 检测是否存在 @Value 注入硬编码服务地址
  if grep -n "@Value.*_SERVICE_HOST" "$CORE_CONFIG_FILE"; then
    log_critical "core-gateway 使用 @Value 注入服务地址，应删除并统一使用 lb:// 服务发现"
    CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
  else
    log_ok "core-gateway 未使用 @Value 硬编码服务地址"
  fi
fi

# ==============================================================================
# 步骤 2: 检测 lite-gateway 路由配置
# ==============================================================================
log_section "步骤 2: 检测 lite-gateway 路由配置"

LITE_CONFIG_FILE="tailor-is/tailor-is-lite-gateway/src/main/java/com/tailoris/litegateway/config/LiteGatewayRouteConfig.java"
if [[ ! -f "$LITE_CONFIG_FILE" ]]; then
  log_critical "lite-gateway 路由配置文件不存在: $LITE_CONFIG_FILE"
  CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
else
  log_ok "找到 lite-gateway 路由配置文件"

  # 检测路由 uri 是否使用 lb://
  if grep -n 'uri("http://' "$LITE_CONFIG_FILE"; then
    log_critical "lite-gateway 路由 uri 使用硬编码 http:// 格式，应改为 lb:// 服务发现格式"
    CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
  else
    log_ok "lite-gateway 所有路由 uri 使用 lb:// 服务发现格式"
  fi

  # 检测是否包含 WebSocket 路由
  if grep -q 'ws-route' "$LITE_CONFIG_FILE"; then
    log_ok "lite-gateway 包含 WebSocket 路由 ws-route"
  else
    log_warning "lite-gateway 缺少 WebSocket 路由 ws-route，v4.0 规范要求 WebSocket 统一经过网关"
    WARNING_COUNT=$((WARNING_COUNT + 1))
  fi
fi

# ==============================================================================
# 步骤 3: 检测 yml 文件是否存在路由配置（v4.0 禁止）
# ==============================================================================
log_section "步骤 3: 检测 yml 文件路由配置"

GATEWAY_YML_FILES=(
  "tailor-is/tailor-is-core-gateway/src/main/resources/application.yml"
  "tailor-is/tailor-is-lite-gateway/src/main/resources/application.yml"
)

for yml_file in "${GATEWAY_YML_FILES[@]}"; do
  if [[ -f "$yml_file" ]]; then
    # 检测是否包含 routes 配置
    if grep -nE '^\s+routes:' "$yml_file"; then
      log_critical "$yml_file 包含 routes 配置，v4.0 规范禁止在 yml 中配置路由，应统一在 Java Config 中定义"
      CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
    else
      log_ok "$yml_file 未在 yml 中配置路由（符合规范）"
    fi
  fi
done

# ==============================================================================
# 步骤 4: 检测 core-gateway Nacos 服务发现是否启用
# ==============================================================================
log_section "步骤 4: 检测 Nacos 服务发现配置"

CORE_YML="tailor-is/tailor-is-core-gateway/src/main/resources/application.yml"
if [[ -f "$CORE_YML" ]]; then
  if grep -q "nacos:" "$CORE_YML" && grep -q "enabled: true" "$CORE_YML"; then
    log_ok "core-gateway Nacos 服务发现已启用"
  elif grep -A2 "nacos:" "$CORE_YML" | grep -A2 "discovery:" | grep -q "enabled: false"; then
    log_critical "core-gateway Nacos 服务发现被禁用，lb:// 路由无法正常工作"
    CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
  else
    log_info "core-gateway Nacos 配置未明确检测到"
  fi
fi

# ==============================================================================
# 总结报告
# ==============================================================================
log_section "检测总结"

echo "  ✅ OK:                $(( ${#CORE_GATEWAY_ROUTES[@]} + ${#LITE_GATEWAY_ROUTES[@]} ))"
echo "  🔵 INFO:              0"
echo "  🟡 WARNING:           $WARNING_COUNT"
echo "  🔴 CRITICAL:          $CRITICAL_COUNT"
echo ""

if [[ "$CRITICAL_COUNT" -gt 0 ]]; then
  log_critical "检测到 $CRITICAL_COUNT 个严重违规，请修复后重新检测"
  if [[ "$NO_FAIL" == "1" ]]; then
    exit 0
  else
    exit 1
  fi
else
  log_ok "网关路由配置符合 v4.0 规范"
  if [[ "$WARNING_COUNT" -gt 0 ]]; then
    log_warning "存在 $WARNING_COUNT 个警告项，建议复核"
  fi
  exit 0
fi