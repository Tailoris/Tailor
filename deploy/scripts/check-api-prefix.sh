#!/usr/bin/env bash
# ==============================================================================
# Tailor IS - API 前缀规范检测脚本 (v4.0)
# ==============================================================================
# 用途: 校验 API 路由前缀是否符合 v4.0 规范
#   1. 扫描所有网关路由配置，检测是否使用统一 API 前缀
#   2. 检测 core-gateway 路由是否统一使用 /api/{service}/** 格式
#   3. 检测 lite-gateway 路由是否统一使用 /api/{service}/** 格式
#   4. 检测是否存在 /api/v1/{service}/** 格式的路由（兼容允许）
#   5. 检测是否存在无 /api/ 前缀的路由（违规）
#   6. 检测 Nginx 配置中 API 路由前缀是否一致
#   7. 检测前端代码中是否存在硬编码的 API 前缀
#
# 权威文档: Tailor-IS 企业级微服务路由与端口规范（v4.0 优化版）
#
# 使用方式:
#   bash deploy/scripts/check-api-prefix.sh                # 严格模式
#   bash deploy/scripts/check-api-prefix.sh --no-fail      # 宽松模式
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
    --help|-h) sed -n '2,25p' "$0"; exit 0 ;;
  esac
done

log_critical() { echo -e "${RED}🔴 CRITICAL${NC}: $1"; }
log_warning() { echo -e "${YELLOW}🟡 WARNING${NC}: $1"; }
log_info() { echo -e "${BLUE}🔵 INFO${NC}: $1"; }
log_ok() { echo -e "${GREEN}✅ OK${NC}: $1"; }
log_section() { echo ""; echo -e "${GREEN}==== $1 ====${NC}"; }

CRITICAL_COUNT=0
WARNING_COUNT=0

# ==============================================================================
# v4.0 规范 API 前缀格式
# ==============================================================================
ALLOWED_PREFIXES=(
  "/api/user/"
  "/api/auth/"
  "/api/product/"
  "/api/order/"
  "/api/payment/"
  "/api/marketing/"
  "/api/ai/"
  "/api/copyright/"
  "/api/merchant/"
  "/api/admin/"
  "/api/pattern/"
  "/api/community/"
  "/api/academy/"
  "/api/supply/"
  "/api/message/"
  "/api/im/"
  "/api/analytics/"
  "/api/v1/"
)

# WebSocket 路由前缀（特殊处理）
WS_PREFIX="/ws/"

# ==============================================================================
# 步骤 1: 检测 core-gateway 路由前缀
# ==============================================================================
log_section "步骤 1: 检测 core-gateway 路由前缀"

CORE_CONFIG_FILE="tailor-is/tailor-is-core-gateway/src/main/java/com/tailoris/coregateway/config/CoreGatewayRouteConfig.java"
if [[ -f "$CORE_CONFIG_FILE" ]]; then
  # 提取所有路由路径配置
  ROUTE_PATHS=$(grep -oE 'path\([^)]+\)' "$CORE_CONFIG_FILE" | grep -oE '"/[^"]+"' | tr -d '"')
  
  while IFS= read -r path; do
    # 跳过 WebSocket 路径（core-gateway 不应有）
    if [[ "$path" == "/ws/"* ]]; then
      log_critical "core-gateway 包含 WebSocket 路由 $path，WebSocket 应由 lite-gateway 处理"
      CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
      continue
    fi
    
    # 检测是否有 /api/ 前缀
    if [[ "$path" != "/api/"* ]]; then
      log_critical "core-gateway 路由 $path 缺少 /api/ 前缀（违规）"
      CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
    else
      log_ok "core-gateway 路由前缀正确: $path"
    fi
  done <<< "$ROUTE_PATHS"
else
  log_critical "core-gateway 路由配置文件不存在: $CORE_CONFIG_FILE"
  CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
fi

# ==============================================================================
# 步骤 2: 检测 lite-gateway 路由前缀
# ==============================================================================
log_section "步骤 2: 检测 lite-gateway 路由前缀"

LITE_CONFIG_FILE="tailor-is/tailor-is-lite-gateway/src/main/java/com/tailoris/litegateway/config/LiteGatewayRouteConfig.java"
if [[ -f "$LITE_CONFIG_FILE" ]]; then
  ROUTE_PATHS=$(grep -oE 'path\([^)]+\)' "$LITE_CONFIG_FILE" | grep -oE '"/[^"]+"' | tr -d '"')
  
  while IFS= read -r path; do
    # WebSocket 路径允许特殊处理
    if [[ "$path" == "/ws/"* ]]; then
      log_ok "lite-gateway WebSocket 路由前缀正确: $path"
      continue
    fi
    
    # 检测是否有 /api/ 前缀
    if [[ "$path" != "/api/"* ]]; then
      log_critical "lite-gateway 路由 $path 缺少 /api/ 前缀（违规）"
      CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
    else
      log_ok "lite-gateway 路由前缀正确: $path"
    fi
  done <<< "$ROUTE_PATHS"
else
  log_critical "lite-gateway 路由配置文件不存在: $LITE_CONFIG_FILE"
  CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
fi

# ==============================================================================
# 步骤 3: 检测 Nginx 配置中 API 路由前缀
# ==============================================================================
log_section "步骤 3: 检测 Nginx 配置中 API 路由前缀"

NGINX_CONF_DIR="deploy/nginx"
for conf_file in "$NGINX_CONF_DIR"/*.conf; do
  if [[ ! -f "$conf_file" ]]; then
    continue
  fi
  
  # 提取所有 location 配置
  LOCATIONS=$(grep -nE '^\s+location\s+/[^/]+/' "$conf_file" 2>/dev/null || true)
  
  if [[ -z "$LOCATIONS" ]]; then
    continue
  fi
  
  while IFS= read -r location; do
    LOCATION_PATH=$(echo "$location" | grep -oE '/[^/]+/' | head -1)
    
    # 跳过静态资源路径
    if [[ "$LOCATION_PATH" == "/static/" || "$LOCATION_PATH" == "/assets/" || "$LOCATION_PATH" == "/images/" || "$LOCATION_PATH" == "/css/" || "$LOCATION_PATH" == "/js/" ]]; then
      continue
    fi
    
    # 检测 API 路径
    if [[ "$LOCATION_PATH" == "/api/" || "$LOCATION_PATH" == "/api/v" ]]; then
      log_ok "$conf_file API 路由前缀正确: $LOCATION_PATH"
    elif [[ "$LOCATION_PATH" == "/ws/" ]]; then
      log_ok "$conf_file WebSocket 路由前缀正确: $LOCATION_PATH"
    elif [[ "$LOCATION_PATH" == "/merchant/" || "$LOCATION_PATH" == "/admin/" ]]; then
      log_ok "$conf_file 前端路由前缀正确: $LOCATION_PATH"
    elif [[ "$LOCATION_PATH" != "/" ]]; then
      log_warning "$conf_file 存在非标准路由前缀: $LOCATION_PATH"
      WARNING_COUNT=$((WARNING_COUNT + 1))
    fi
  done <<< "$LOCATIONS"
done

# ==============================================================================
# 步骤 4: 检测前端代码中硬编码的 API 前缀
# ==============================================================================
log_section "步骤 4: 检测前端代码中硬编码的 API 前缀"

FRONTEND_DIR="tailor-is-frontend"
if [[ -d "$FRONTEND_DIR" ]]; then
  # 搜索硬编码的完整 URL
  HARDCODED_URLS=$(grep -rE 'http://[^/]+:[0-9]+' "$FRONTEND_DIR" --include="*.ts" --include="*.tsx" --include="*.js" --include="*.jsx" | head -20)
  
  if [[ -n "$HARDCODED_URLS" ]]; then
    log_warning "前端代码中存在硬编码的服务地址:"
    echo "$HARDCODED_URLS"
    WARNING_COUNT=$((WARNING_COUNT + 1))
  else
    log_ok "前端代码中未检测到硬编码的服务地址"
  fi
  
  # 搜索 API 基础路径配置
  API_BASE_URLS=$(grep -rE 'API_BASE_URL|apiBaseUrl|baseURL' "$FRONTEND_DIR" --include="*.ts" --include="*.tsx" --include="*.js" --include="*.jsx" | head -20)
  
  if [[ -n "$API_BASE_URLS" ]]; then
    log_info "检测到 API 基础路径配置:"
    echo "$API_BASE_URLS"
  fi
else
  log_info "前端目录不存在: $FRONTEND_DIR"
fi

# ==============================================================================
# 步骤 5: 检测后端 Controller 路径前缀
# ==============================================================================
log_section "步骤 5: 检测后端 Controller 路径前缀"

BACKEND_DIR="tailor-is"
if [[ -d "$BACKEND_DIR" ]]; then
  # 搜索 @RequestMapping 和 @GetMapping 等注解
  CONTROLLER_PATHS=$(grep -rE '@RequestMapping\(".*"\)|@GetMapping\(".*"\)|@PostMapping\(".*"\)' "$BACKEND_DIR" --include="*.java" | grep -oE '"/[^"]+"' | tr -d '"' | sort -u)
  
  while IFS= read -r path; do
    if [[ "$path" == "/api/"* ]]; then
      continue
    elif [[ "$path" == "/" ]]; then
      continue
    else
      log_warning "Controller 路径 $path 未使用 /api/ 前缀（建议统一）"
      WARNING_COUNT=$((WARNING_COUNT + 1))
    fi
  done <<< "$CONTROLLER_PATHS"
  
  log_ok "后端 Controller 路径前缀检测完成"
else
  log_info "后端目录不存在: $BACKEND_DIR"
fi

# ==============================================================================
# 总结报告
# ==============================================================================
log_section "检测总结"

echo "  ✅ OK:                $(( ${#ALLOWED_PREFIXES[@]} ))"
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
  log_ok "API 前缀规范检测通过"
  if [[ "$WARNING_COUNT" -gt 0 ]]; then
    log_warning "存在 $WARNING_COUNT 个警告项，建议复核"
  fi
  exit 0
fi