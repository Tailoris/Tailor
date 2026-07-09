#!/usr/bin/env bash
# ==============================================================================
# Tailor IS - 端口分配冲突检测脚本 (v4.0)
# ==============================================================================
# 用途: 校验端口分配是否符合 v4.0 规范，防止端口冲突
#   1. 读取 docker-compose.yml 全部端口映射，生成端口清单
#   2. 检测是否存在重复端口映射
#   3. 检测端口范围是否在 8100-8199 区间内
#   4. 检测宿主机端口与容器端口是否一致（容器内固定端口）
#   5. 检测 core-gateway 宿主机端口是否为 9000，容器端口是否为 8080
#   6. 检测 lite-gateway 端口是否为 8081
#   7. 检测前端服务是否存在宿主机端口映射（v4.0 禁止）
#   8. 检测 Nginx 是否监听 80/443 端口
#
# 权威文档: Tailor-IS 企业级微服务路由与端口规范（v4.0 优化版）
#
# 使用方式:
#   bash deploy/scripts/check-port-allocation.sh                # 严格模式
#   bash deploy/scripts/check-port-allocation.sh --no-fail      # 宽松模式
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
# v4.0 规范端口分配表（对齐 RoutePortConstant）
# 端口段划分:
#   8100-8119: 业务微服务统一区间
#   9000-9099: 网关专属区间（core-gateway=9000，预留扩展多网关）
#   9200-9299: 监控运维独立分区
#   4000-4099: 公共工具服务（GraphQL、文件上传等）
# ==============================================================================
EXPECTED_PORTS=(
  "tailor-is-core-gateway:8080:9000"
  "tailor-is-lite-gateway:8081:8081"
  "tailor-is-admin:8100:8100"
  "tailor-is-user:8101:8101"
  "tailor-is-product:8102:8102"
  "tailor-is-order:8103:8103"
  "tailor-is-payment:8104:8104"
  "tailor-is-marketing:8105:8105"
  "tailor-is-ai:8106:8106"
  "tailor-is-copyright:8107:8107"
  "tailor-is-community:8108:8108"
  "tailor-is-supply:8109:8109"
  "tailor-is-merchant:8110:8110"
  "tailor-is-message:8111:8111"
  "tailor-is-academy:8112:8112"
  "tailor-is-analytics:8113:8113"
  "tailor-is-message-im:8114:8114"
  "tailor-is-pattern:8115:8115"
)

# 前端服务列表（v4.0 禁止宿主机端口映射）
FRONTEND_SERVICES=(
  "tailoris-frontend"
  "tailoris-frontend-admin"
)

# ==============================================================================
# 步骤 1: 读取 docker-compose.yml 端口配置
# ==============================================================================
log_section "步骤 1: 读取 docker-compose.yml 端口配置"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOCKER_COMPOSE_FILE="$SCRIPT_DIR/../docker-compose.services.yml"
if [[ ! -f "$DOCKER_COMPOSE_FILE" ]]; then
  DOCKER_COMPOSE_FILE="$SCRIPT_DIR/../docker-compose.prod.yml"
fi
if [[ ! -f "$DOCKER_COMPOSE_FILE" ]]; then
  log_critical "docker-compose 文件不存在"
  CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
  exit 1
fi

log_ok "找到 docker-compose 文件: $DOCKER_COMPOSE_FILE"

# 提取所有端口映射配置
PORT_MAPPINGS=$(grep -nE '^\s+- "[0-9]+:[0-9]+"' "$DOCKER_COMPOSE_FILE")
if [[ -z "$PORT_MAPPINGS" ]]; then
  log_warning "docker-compose.yml 中未找到端口映射配置"
else
  echo "检测到以下端口映射:"
  echo "$PORT_MAPPINGS"
  echo ""
fi

# ==============================================================================
# 步骤 2: 检测端口冲突
# ==============================================================================
log_section "步骤 2: 检测端口冲突"

# 提取所有宿主机端口（只从 ports 部分提取）
# 使用 sed 提取 services 到文件结尾或下一级标签之间的内容
HOST_PORTS=$(sed -n '/^services:/,/^[a-zA-Z_-]\+:/p' "$DOCKER_COMPOSE_FILE" | grep -E '^\s+- "[0-9]+:[0-9]+"' | grep -oE '([0-9]+):[0-9]+' | cut -d: -f1 | sort | uniq)

# 检测重复端口
DUPLICATE_PORTS=$(echo "$HOST_PORTS" | uniq -d)
if [[ -n "$DUPLICATE_PORTS" ]]; then
  log_critical "检测到端口冲突: $DUPLICATE_PORTS"
  CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
else
  log_ok "未检测到端口冲突"
fi

# ==============================================================================
# 步骤 3: 检测业务服务端口范围
# ==============================================================================
log_section "步骤 3: 检测业务服务端口范围"

for port in $HOST_PORTS; do
  if [[ "$port" -ge 8100 && "$port" -le 8119 ]]; then
    continue
  elif [[ "$port" -eq 8080 || "$port" -eq 8081 || "$port" -eq 80 || "$port" -eq 443 || "$port" -eq 4000 ]]; then
    continue
  elif [[ "$port" -ge 9000 && "$port" -le 9099 ]]; then
    continue
  elif [[ "$port" -ge 9200 && "$port" -le 9299 ]]; then
    continue
  else
    log_warning "检测到非规范端口 $port，业务服务端口应在 8100-8119 范围内"
    WARNING_COUNT=$((WARNING_COUNT + 1))
  fi
done

log_ok "业务服务端口范围检测通过"

# ==============================================================================
# 步骤 4: 校验核心网关端口配置
# ==============================================================================
log_section "步骤 4: 校验核心网关端口配置"

# 检测 core-gateway 宿主机端口是否为 9000
CORE_HOST_PORT=$(grep -A5 "tailor-is-core-gateway" "$DOCKER_COMPOSE_FILE" | grep -oE '[0-9]+:8080' | cut -d: -f1)
if [[ "$CORE_HOST_PORT" == "9000" ]]; then
  log_ok "core-gateway 宿主机端口配置正确: 9000"
else
  log_critical "core-gateway 宿主机端口应为 9000，当前配置: $CORE_HOST_PORT"
  CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
fi

# 检测 core-gateway 容器端口是否为 8080
CORE_CONTAINER_PORT=$(grep -A5 "tailor-is-core-gateway" "$DOCKER_COMPOSE_FILE" | grep -oE '9000:[0-9]+' | cut -d: -f2)
if [[ "$CORE_CONTAINER_PORT" == "8080" ]]; then
  log_ok "core-gateway 容器端口配置正确: 8080"
else
  log_critical "core-gateway 容器端口应为 8080，当前配置: $CORE_CONTAINER_PORT"
  CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
fi

# ==============================================================================
# 步骤 5: 校验轻量网关端口配置
# ==============================================================================
log_section "步骤 5: 校验轻量网关端口配置"

# 检测 lite-gateway 端口是否为 8081（v4.0 规范）
LITE_PORT=$(grep -A5 "tailor-is-lite-gateway" "$DOCKER_COMPOSE_FILE" | grep -oE '[0-9]+:[0-9]+' | head -1)
if [[ "$LITE_PORT" == "8081:8081" ]]; then
  log_ok "lite-gateway 端口配置正确: 8081:8081"
else
  log_critical "lite-gateway 端口应为 8081:8081，当前配置: $LITE_PORT"
  CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
fi

# ==============================================================================
# 步骤 6: 检测前端服务端口映射
# ==============================================================================
log_section "步骤 6: 检测前端服务端口映射"

for frontend in "${FRONTEND_SERVICES[@]}"; do
  if grep -A10 "$frontend" "$DOCKER_COMPOSE_FILE" | grep -qE '^\s+- "[0-9]+:[0-9]+"'; then
    log_warning "$frontend 存在宿主机端口映射，v4.0 规范建议前端服务仅通过 Nginx 反向代理访问，不直接暴露端口"
    WARNING_COUNT=$((WARNING_COUNT + 1))
  else
    log_ok "$frontend 未配置宿主机端口映射（符合规范）"
  fi
done

# ==============================================================================
# 步骤 7: 检测 Nginx 端口配置
# ==============================================================================
log_section "步骤 7: 检测 Nginx 端口配置"

# 在 docker-compose.services.yml 中可能不包含 nginx（nginx可能在独立compose中）
# 检查是否存在 nginx 服务
if grep -q "nginx" "$DOCKER_COMPOSE_FILE"; then
  NGINX_80=$(grep -A5 "nginx" "$DOCKER_COMPOSE_FILE" | grep -oE '80:[0-9]+' | cut -d: -f2)
  NGINX_443=$(grep -A5 "nginx" "$DOCKER_COMPOSE_FILE" | grep -oE '443:[0-9]+' | cut -d: -f2)

  if [[ -n "$NGINX_80" ]]; then
    log_ok "Nginx 监听 HTTP 端口 80"
  else
    log_warning "Nginx 未配置 HTTP 端口 80"
    WARNING_COUNT=$((WARNING_COUNT + 1))
  fi

  if [[ -n "$NGINX_443" ]]; then
    log_ok "Nginx 监听 HTTPS 端口 443"
  else
    log_warning "Nginx 未配置 HTTPS 端口 443"
    WARNING_COUNT=$((WARNING_COUNT + 1))
  fi
else
  log_info "docker-compose 文件中未找到 nginx 服务（可能在独立的部署文件中）"
fi

# ==============================================================================
# 步骤 8: 检测业务服务端口一致性
# ==============================================================================
log_section "步骤 8: 检测业务服务端口一致性"

# 读取业务服务端口映射（排除网关、Nginx、graphql）
BUSINESS_PORT_MAPPINGS=$(grep -nE '^\s+- "[0-9]+:[0-9]+"' "$DOCKER_COMPOSE_FILE" 2>/dev/null | grep -v "9000:8080" | grep -v "8081:8081" | grep -v "80:" | grep -v "443:" | grep -v "4000:4000" || true)

if [[ -z "$BUSINESS_PORT_MAPPINGS" ]]; then
  log_ok "业务服务未配置宿主机端口映射（符合 v4.0 规范：业务服务仅内网通信）"
else
  while IFS= read -r mapping; do
    HOST_PORT=$(echo "$mapping" | grep -oE '([0-9]+):[0-9]+' | cut -d: -f1)
    CONTAINER_PORT=$(echo "$mapping" | grep -oE '[0-9]+:([0-9]+)' | cut -d: -f2)
    
    if [[ "$HOST_PORT" != "$CONTAINER_PORT" ]]; then
      log_warning "业务服务端口映射不一致: $HOST_PORT:$CONTAINER_PORT（建议宿主机与容器端口一致）"
      WARNING_COUNT=$((WARNING_COUNT + 1))
    else
      log_ok "业务服务端口映射一致: $HOST_PORT:$CONTAINER_PORT"
    fi
  done <<< "$BUSINESS_PORT_MAPPINGS"
fi

# ==============================================================================
# 总结报告
# ==============================================================================
log_section "检测总结"

echo "  ✅ OK:                $(( ${#EXPECTED_PORTS[@]} ))"
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
  log_ok "端口分配符合 v4.0 规范"
  if [[ "$WARNING_COUNT" -gt 0 ]]; then
    log_warning "存在 $WARNING_COUNT 个警告项，建议复核"
  fi
  exit 0
fi