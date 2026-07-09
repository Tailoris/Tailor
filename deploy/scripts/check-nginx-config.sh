#!/usr/bin/env bash
# ==============================================================================
# Tailor IS - Nginx 配置静态校验脚本 (v4.0)
# ==============================================================================
# 用途: 校验 Nginx 配置是否符合 v4.0 网关统一收敛架构规范
#   1. 执行 nginx -t 语法校验
#   2. 扫描配置文件，拦截直连业务服务的 proxy_pass
#   3. 校验所有上游网关端口为 9000/8081，拦截旧 8080 地址
#   4. 校验 WebSocket 转发目标为 lite-gateway，禁止直连 IM 服务
#   5. 检测是否存在业务微服务 upstream 定义
#
# 权威文档: Tailor-IS 企业级微服务路由与端口规范（v4.0 优化版）
#
# 使用方式:
#   bash deploy/scripts/check-nginx-config.sh                # 严格模式
#   bash deploy/scripts/check-nginx-config.sh --no-fail      # 宽松模式
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
# v4.0 规范允许的 upstream 定义
# ==============================================================================
ALLOWED_UPSTREAMS=(
  "tailoris_core_gateway"
  "tailoris_lite_gateway"
  "tailoris_graphql"
)

# v4.0 规范禁止的业务服务直连 upstream
FORBIDDEN_UPSTREAMS=(
  "tailoris_product_service"
  "tailoris_community_service"
  "tailoris_message_im_service"
  "tailoris_user_service"
  "tailoris_order_service"
  "tailoris_payment_service"
)

# ==============================================================================
# 步骤 1: Nginx 配置文件存在性检测
# ==============================================================================
log_section "步骤 1: Nginx 配置文件检测"

NGINX_CONF_DIR="deploy/nginx"
if [[ -d "$NGINX_CONF_DIR" ]]; then
  CONF_FILES=$(find "$NGINX_CONF_DIR" -name "*.conf" -type f)
  if [[ -n "$CONF_FILES" ]]; then
    log_ok "找到 Nginx 配置文件: $CONF_FILES"
  else
    log_warning "Nginx 配置目录为空"
    WARNING_COUNT=$((WARNING_COUNT + 1))
  fi
else
  log_critical "Nginx 配置目录不存在: $NGINX_CONF_DIR"
  CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
fi

# ==============================================================================
# 步骤 2: 检测直连业务服务的 upstream 定义
# ==============================================================================
log_section "步骤 2: 检测直连业务服务 upstream"

for conf_file in "$NGINX_CONF_DIR"/*.conf; do
  if [[ ! -f "$conf_file" ]]; then
    continue
  fi

  for forbidden in "${FORBIDDEN_UPSTREAMS[@]}"; do
    if grep -n "upstream $forbidden" "$conf_file"; then
      log_critical "$conf_file 包含禁止的直连 upstream '$forbidden'，v4.0 规范禁止 Nginx 直连业务微服务"
      CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
    fi
  done
done

# 检测是否有指向业务服务端口的 upstream
BUSINESS_PORTS=(8100 8101 8102 8103 8104 8105 8106 8107 8108 8109 8110 8111 8112 8113 8114 8115)
for conf_file in "$NGINX_CONF_DIR"/*.conf; do
  if [[ ! -f "$conf_file" ]]; then
    continue
  fi

  for port in "${BUSINESS_PORTS[@]}"; do
    # 检测 upstream 中是否直接指向业务端口
    if grep -nE "server\s+[^;]+:$port" "$conf_file" | grep -v "core_gateway\|lite_gateway\|graphql"; then
      log_critical "$conf_file 包含指向业务服务端口 $port 的 upstream，v4.0 规范禁止直连业务微服务"
      CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
    fi
  done
done

log_ok "未检测到直连业务服务的 upstream 定义"

# ==============================================================================
# 步骤 3: 检测直连业务服务的 proxy_pass
# ==============================================================================
log_section "步骤 3: 检测直连业务服务的 proxy_pass"

for conf_file in "$NGINX_CONF_DIR"/*.conf; do
  if [[ ! -f "$conf_file" ]]; then
    continue
  fi

  # 检测 proxy_pass 直接指向业务端口
  for port in "${BUSINESS_PORTS[@]}"; do
    if grep -n "proxy_pass.*:$port" "$conf_file"; then
      log_critical "$conf_file 包含直连业务服务端口 $port 的 proxy_pass，所有 API 必须经过网关转发"
      CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
    fi
  done

  # 检测 proxy_pass 直接指向 IP:端口形式（非 upstream）
  if grep -nE 'proxy_pass\s+http://[0-9]' "$conf_file" | grep -v "127.0.0.1:9000\|172.17.0.1:9000\|host.docker.internal:9000\|127.0.0.1:8081\|172.17.0.1:8081\|host.docker.internal:8081\|127.0.0.1:4000\|172.17.0.1:4000\|host.docker.internal:4000"; then
    log_critical "$conf_file 包含非网关的直连 proxy_pass，所有 API 必须经过网关转发"
    CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
  fi
done

log_ok "未检测到直连业务服务的 proxy_pass"

# ==============================================================================
# 步骤 4: 校验网关端口配置
# ==============================================================================
log_section "步骤 4: 校验网关端口配置"

for conf_file in "$NGINX_CONF_DIR"/*.conf; do
  if [[ ! -f "$conf_file" ]]; then
    continue
  fi

  # core-gateway 端口检测：允许两种合法配置
  # 1. Docker 内部网络: tailor-is-core-gateway:8080（容器名称 + 容器端口）
  # 2. Host 网络: localhost:9000 或 127.0.0.1:9000（宿主机端口）
  CORE_GATEWAY_VALID=$(grep -E 'tailoris_core_gateway.*8080|localhost:9000|127\.0\.0\.1:9000' "$conf_file")
  if [[ -n "$CORE_GATEWAY_VALID" ]]; then
    log_ok "$conf_file core-gateway 端口配置正确"
  else
    if grep -nE '8080' "$conf_file" | grep -v "server_name\|listen\|ssl\|Nacos\|tailoris_core_gateway"; then
      log_critical "$conf_file 包含非网关的 8080 端口配置"
      CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
    fi
  fi

  # lite-gateway 端口检测：允许两种合法配置
  # 1. Docker 内部网络: tailor-is-lite-gateway:8081
  # 2. Host 网络: localhost:8081 或 127.0.0.1:8081
  LITE_GATEWAY_VALID=$(grep -E 'tailoris_lite_gateway.*8081|localhost:8081|127\.0\.0\.1:8081' "$conf_file")
  if [[ -n "$LITE_GATEWAY_VALID" ]]; then
    log_ok "$conf_file lite-gateway 端口配置正确"
  else
    log_warning "$conf_file 未找到有效的 lite-gateway 端口配置"
    WARNING_COUNT=$((WARNING_COUNT + 1))
  fi
done

# ==============================================================================
# 步骤 5: 校验 WebSocket 转发配置
# ==============================================================================
log_section "步骤 5: 校验 WebSocket 转发配置"

for conf_file in "$NGINX_CONF_DIR"/*.conf; do
  if [[ ! -f "$conf_file" ]]; then
    continue
  fi

  # 检测 WebSocket 是否经过 lite-gateway
  if grep -n "location /ws/" "$conf_file"; then
    if grep -A5 "location /ws/" "$conf_file" | grep -q "8081\|lite_gateway"; then
      log_ok "$conf_file WebSocket 转发目标为 lite-gateway（符合规范）"
    else
      if grep -A5 "location /ws/" "$conf_file" | grep -q "8114"; then
        log_critical "$conf_file WebSocket 直接转发至 IM 服务端口 8114，v4.0 规范要求必须经过 lite-gateway"
        CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
      else
        log_warning "$conf_file WebSocket 转发目标不明确，建议确认是否指向 lite-gateway:8081"
        WARNING_COUNT=$((WARNING_COUNT + 1))
      fi
    fi
  fi
done

# ==============================================================================
# 总结报告
# ==============================================================================
log_section "检测总结"

echo "  ✅ OK:                $(( ${#ALLOWED_UPSTREAMS[@]} ))"
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
  log_ok "Nginx 配置符合 v4.0 规范"
  if [[ "$WARNING_COUNT" -gt 0 ]]; then
    log_warning "存在 $WARNING_COUNT 个警告项，建议复核"
  fi
  exit 0
fi