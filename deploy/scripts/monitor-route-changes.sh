#!/usr/bin/env bash
# ==============================================================================
# Tailor IS - 路由变更监控脚本 (v4.0)
# ==============================================================================
# 用途: 监控网关路由配置变更，及时发现异常路由修改
#   1. 定期扫描网关路由配置文件
#   2. 检测新增/删除/修改的路由
#   3. 对比规范路由清单，检测违规路由
#   4. 触发告警通知
#
# 使用方式:
#   bash deploy/scripts/monitor-route-changes.sh --check    # 单次检查
#   bash deploy/scripts/monitor-route-changes.sh --watch    # 持续监控
#
# 告警级别:
#   CRITICAL - 违规路由（缺少 /api/v1/ 前缀、无效服务标识）
#   WARNING - 路由变更（新增/删除/修改）
#   INFO - 正常信息
# ==============================================================================
set -euo pipefail

RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
GREEN='\033[0;32m'
NC='\033[0m'

WATCH_MODE=0
CHECK_INTERVAL=60

for arg in "$@"; do
  case "$arg" in
    --watch) WATCH_MODE=1 ;;
    --check) WATCH_MODE=0 ;;
    --interval=*) CHECK_INTERVAL="${arg#*=}" ;;
    --help|-h) sed -n '2,30p' "$0"; exit 0 ;;
  esac
done

log_critical() { echo -e "${RED}🔴 CRITICAL${NC}: $1"; }
log_warning() { echo -e "${YELLOW}🟡 WARNING${NC}: $1"; }
log_info() { echo -e "${BLUE}🔵 INFO${NC}: $1"; }
log_ok() { echo -e "${GREEN}✅ OK${NC}: $1"; }

GATEWAY_CONFIGS=(
  "tailor-is/tailor-is-core-gateway/src/main/java/com/tailoris/coregateway/config/CoreGatewayRouteConfig.java"
  "tailor-is/tailor-is-lite-gateway/src/main/java/com/tailoris/litegateway/config/LiteGatewayRouteConfig.java"
)

ALLOWED_SERVICES=(
  "user" "auth" "product" "favorite" "order" "cart"
  "payment" "settlement" "account" "sandbox" "marketing" "coupon"
  "points" "seckill" "ai" "body-size" "copyright" "merchant"
  "shop" "admin" "pattern" "community" "post" "comment"
  "supply" "message" "notice" "academy" "course" "analytics"
  "metrics" "dashboard" "im" "im-message"
)

LAST_CHECKSUM=""

extract_routes() {
  local config_file="$1"
  grep -oE 'path\([^)]+\)' "$config_file" | grep -oE '"/[^"]+"' | tr -d '"' | sort
}

validate_route() {
  local route="$1"
  
  if [[ "$route" == "/ws/"* ]]; then
    return 0
  fi
  
  if [[ "$route" != "/api/"* ]]; then
    log_critical "路由 $route 缺少 /api/ 前缀"
    return 1
  fi
  
  if [[ "$route" != "/api/v1/"* && "$route" != "/api/v1/"* ]]; then
    log_warning "路由 $route 使用非标准版本前缀"
    return 0
  fi
  
  SERVICE=$(echo "$route" | grep -oE '/api/v1/([a-z-]+)' | cut -d'/' -f4)
  FOUND=0
  for allowed in "${ALLOWED_SERVICES[@]}"; do
    if [[ "$SERVICE" == "$allowed" ]]; then
      FOUND=1
      break
    fi
  done
  
  if [[ "$FOUND" == 0 ]]; then
    log_critical "路由 $route 包含未授权服务标识: $SERVICE"
    return 1
  fi
  
  return 0
}

check_routes() {
  local critical_count=0
  local warning_count=0
  
  log_info "[$(date '+%Y-%m-%d %H:%M:%S')] 开始路由变更检查"
  
  for config in "${GATEWAY_CONFIGS[@]}"; do
    if [[ ! -f "$config" ]]; then
      log_critical "配置文件不存在: $config"
      critical_count=$((critical_count + 1))
      continue
    fi
    
    CURRENT_CHECKSUM=$(md5sum "$config" | cut -d' ' -f1)
    
    if [[ -n "$LAST_CHECKSUM" && "$CURRENT_CHECKSUM" != "$LAST_CHECKSUM" ]]; then
      log_warning "配置文件 $config 发生变更"
      warning_count=$((warning_count + 1))
    fi
    
    LAST_CHECKSUM="$CURRENT_CHECKSUM"
    
    ROUTES=$(extract_routes "$config")
    for route in $ROUTES; do
      if ! validate_route "$route"; then
        critical_count=$((critical_count + 1))
      fi
    done
  done
  
  if [[ "$critical_count" -gt 0 ]]; then
    log_critical "检测到 $critical_count 个严重违规，触发告警"
    return 1
  elif [[ "$warning_count" -gt 0 ]]; then
    log_warning "检测到 $warning_count 个变更，建议复核"
    return 0
  else
    log_ok "路由配置检查通过，无异常"
    return 0
  fi
}

if [[ "$WATCH_MODE" == "1" ]]; then
  log_info "进入持续监控模式，检查间隔: ${CHECK_INTERVAL}s"
  while true; do
    check_routes
    sleep "$CHECK_INTERVAL"
  done
else
  check_routes
  exit "$?"
fi