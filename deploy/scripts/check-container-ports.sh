#!/usr/bin/env bash
# ==============================================================================
# Tailor IS - Docker 容器端口映射检测脚本 (v4.0)
# ==============================================================================
# 用途: 校验 Docker 容器端口映射是否符合 v4.0 规范
#   1. 读取 docker-compose.yml 全部容器端口配置
#   2. 检测容器内部固定端口是否符合规范（8100-8199）
#   3. 检测端口映射规则：业务服务宿主机端口 = 容器端口
#   4. 检测网关端口映射规则（core-gateway: 8080→9000, lite-gateway: 8081→8081）
#   5. 检测 Nginx 端口映射（80, 443）
#   6. 检测前端服务容器端口（不映射到宿主机）
#   7. 检测中间件端口（Redis/MongoDB/MySQL/Nacos）不暴露到宿主机
#
# 权威文档: Tailor-IS 企业级微服务路由与端口规范（v4.0 优化版）
#
# 使用方式:
#   bash deploy/scripts/check-container-ports.sh                # 严格模式
#   bash deploy/scripts/check-container-ports.sh --no-fail      # 宽松模式
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
# v4.0 规范容器端口配置表（对齐 RoutePortConstant）
# 端口段划分:
#   8100-8119: 业务微服务统一区间（已占用 8100\8115，预留 8116\8119 扩容）
#   9000-9099: 网关专属区间（core-gateway=9000，预留扩展多网关）
#   9200-9299: 监控运维独立分区
# ==============================================================================
CONTAINER_PORTS=(
  "tailor-is-core-gateway:8080"
  "tailor-is-lite-gateway:8081"
  "tailor-is-admin:8100"
  "tailor-is-user:8101"
  "tailor-is-product:8102"
  "tailor-is-order:8103"
  "tailor-is-payment:8104"
  "tailor-is-marketing:8105"
  "tailor-is-ai:8106"
  "tailor-is-copyright:8107"
  "tailor-is-community:8108"
  "tailor-is-supply:8109"
  "tailor-is-merchant:8110"
  "tailor-is-message:8111"
  "tailor-is-academy:8112"
  "tailor-is-analytics:8113"
  "tailor-is-message-im:8114"
  "tailor-is-pattern:8115"
)

# 中间件服务列表（不应暴露端口到宿主机）
MIDDLEWARE_SERVICES=(
  "redis"
  "mongodb"
  "mysql"
  "nacos"
)

# 前端服务列表（不应映射端口到宿主机）
FRONTEND_SERVICES=(
  "tailoris-frontend"
  "tailoris-frontend-admin"
)

# ==============================================================================
# 步骤 1: 读取 docker-compose.yml
# ==============================================================================
log_section "步骤 1: 读取 docker-compose.yml"

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

# ==============================================================================
# 步骤 2: 检测业务服务容器端口
# ==============================================================================
log_section "步骤 2: 检测业务服务容器端口"

for entry in "${CONTAINER_PORTS[@]}"; do
  SERVICE_NAME=$(echo "$entry" | cut -d: -f1)
  EXPECTED_PORT=$(echo "$entry" | cut -d: -f2)
  
  # 检测服务是否存在
  if ! grep -q "$SERVICE_NAME" "$DOCKER_COMPOSE_FILE"; then
    log_warning "服务 $SERVICE_NAME 未在 docker-compose 文件中定义"
    WARNING_COUNT=$((WARNING_COUNT + 1))
    continue
  fi
  
  # 提取实际端口映射或 SERVER_PORT 环境变量
  ACTUAL_MAPPING=$(grep -A30 "$SERVICE_NAME" "$DOCKER_COMPOSE_FILE" | grep -oE '[0-9]+:[0-9]+' | head -1)
  ACTUAL_SERVER_PORT=$(grep -A30 "$SERVICE_NAME" "$DOCKER_COMPOSE_FILE" | grep -oE 'SERVER_PORT=[0-9]+' | cut -d= -f2)
  
  if [[ -n "$ACTUAL_MAPPING" ]]; then
    CONTAINER_PORT=$(echo "$ACTUAL_MAPPING" | cut -d: -f2)
  elif [[ -n "$ACTUAL_SERVER_PORT" ]]; then
    CONTAINER_PORT="$ACTUAL_SERVER_PORT"
  else
    log_warning "服务 $SERVICE_NAME 未配置端口（无 ports 映射且无 SERVER_PORT）"
    WARNING_COUNT=$((WARNING_COUNT + 1))
    continue
  fi
  
  if [[ "$CONTAINER_PORT" == "$EXPECTED_PORT" ]]; then
    log_ok "$SERVICE_NAME 容器端口配置正确: $CONTAINER_PORT"
  else
    log_critical "$SERVICE_NAME 容器端口应为 $EXPECTED_PORT，当前配置: $CONTAINER_PORT"
    CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
  fi
done

# ==============================================================================
# 步骤 3: 检测业务服务端口映射一致性
# ==============================================================================
log_section "步骤 3: 检测业务服务端口映射一致性"

for entry in "${CONTAINER_PORTS[@]}"; do
  SERVICE_NAME=$(echo "$entry" | cut -d: -f1)
  
  # 跳过网关服务（有特殊映射规则）
  if [[ "$SERVICE_NAME" == "tailor-is-core-gateway" || "$SERVICE_NAME" == "tailor-is-lite-gateway" ]]; then
    continue
  fi
  
  # 只从 ports 部分提取端口映射（排除 healthcheck 等其他配置）
  ACTUAL_MAPPING=$(grep -A30 "$SERVICE_NAME" "$DOCKER_COMPOSE_FILE" 2>/dev/null | grep -E '^\s+ports:' -A10 2>/dev/null | grep -oE '[0-9]+:[0-9]+' 2>/dev/null | head -1 || true)
  HAS_EXPOSE=$(grep -A30 "$SERVICE_NAME" "$DOCKER_COMPOSE_FILE" | grep -q '^\s*expose:' && echo "true" || echo "")
  HAS_SERVER_PORT=$(grep -A30 "$SERVICE_NAME" "$DOCKER_COMPOSE_FILE" | grep -q 'SERVER_PORT=' && echo "true" || echo "")
  
  if [[ -n "$ACTUAL_MAPPING" ]]; then
    HOST_PORT=$(echo "$ACTUAL_MAPPING" | cut -d: -f1)
    CONTAINER_PORT=$(echo "$ACTUAL_MAPPING" | cut -d: -f2)
    
    if [[ "$HOST_PORT" == "$CONTAINER_PORT" ]]; then
      log_ok "$SERVICE_NAME 端口映射一致: $HOST_PORT:$CONTAINER_PORT"
    else
      log_warning "$SERVICE_NAME 端口映射不一致: $HOST_PORT:$CONTAINER_PORT（建议宿主机与容器端口一致）"
      WARNING_COUNT=$((WARNING_COUNT + 1))
    fi
  elif [[ "$HAS_EXPOSE" == "true" ]]; then
    log_ok "$SERVICE_NAME 使用 expose 方式（仅内网通信），符合 v4.0 规范"
  elif [[ "$HAS_SERVER_PORT" == "true" ]]; then
    log_ok "$SERVICE_NAME 使用 SERVER_PORT 环境变量（仅内网通信），符合 v4.0 规范"
  else
    log_warning "$SERVICE_NAME 未配置端口映射、expose 且未设置 SERVER_PORT"
    WARNING_COUNT=$((WARNING_COUNT + 1))
  fi
done

# ==============================================================================
# 步骤 4: 检测网关端口映射规则
# ==============================================================================
log_section "步骤 4: 检测网关端口映射规则"

# core-gateway: 容器端口 8080 → 宿主机端口 9000
CORE_MAPPING=$(grep -A10 "tailor-is-core-gateway" "$DOCKER_COMPOSE_FILE" | grep -oE '[0-9]+:[0-9]+' | head -1)
if [[ "$CORE_MAPPING" == "9000:8080" ]]; then
  log_ok "core-gateway 端口映射正确: 9000:8080"
else
  log_critical "core-gateway 端口映射应为 9000:8080，当前配置: $CORE_MAPPING"
  CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
fi

# lite-gateway: 容器端口 8081 → 宿主机端口 8081
LITE_MAPPING=$(grep -A10 "tailor-is-lite-gateway" "$DOCKER_COMPOSE_FILE" | grep -oE '[0-9]+:[0-9]+' | head -1)
if [[ "$LITE_MAPPING" == "8081:8081" ]]; then
  log_ok "lite-gateway 端口映射正确: 8081:8081"
else
  log_critical "lite-gateway 端口映射应为 8081:8081，当前配置: $LITE_MAPPING"
  CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
fi

# ==============================================================================
# 步骤 5: 检测前端服务端口映射
# ==============================================================================
log_section "步骤 5: 检测前端服务端口映射"

for frontend in "${FRONTEND_SERVICES[@]}"; do
  if grep -q "$frontend" "$DOCKER_COMPOSE_FILE"; then
    if grep -A10 "$frontend" "$DOCKER_COMPOSE_FILE" | grep -qE '^\s+- "[0-9]+:[0-9]+"'; then
      log_warning "$frontend 存在宿主机端口映射，v4.0 规范建议前端服务仅通过 Nginx 反向代理访问"
      WARNING_COUNT=$((WARNING_COUNT + 1))
    else
      log_ok "$frontend 未配置宿主机端口映射（符合规范）"
    fi
  fi
done

# ==============================================================================
# 步骤 6: 检测中间件端口暴露
# ==============================================================================
log_section "步骤 6: 检测中间件端口暴露"

for middleware in "${MIDDLEWARE_SERVICES[@]}"; do
  if grep -q "$middleware" "$DOCKER_COMPOSE_FILE"; then
    # 检测是否有端口映射
    if grep -A10 "$middleware" "$DOCKER_COMPOSE_FILE" | grep -qE '^\s+- "[0-9]+:[0-9]+"'; then
      # 检查是否是1Panel约束的端口（允许1Panel管理端口）
      MAPPING=$(grep -A10 "$middleware" "$DOCKER_COMPOSE_FILE" | grep -oE '[0-9]+:[0-9]+' | head -1)
      if [[ "$MAPPING" == "6379:6379" || "$MAPPING" == "27017:27017" || "$MAPPING" == "3306:3306" || "$MAPPING" == "8848:8848" ]]; then
        log_info "$middleware 端口映射 $MAPPING（1Panel 约束允许）"
      else
        log_warning "$middleware 存在端口映射 $MAPPING，建议仅内部访问"
        WARNING_COUNT=$((WARNING_COUNT + 1))
      fi
    else
      log_ok "$middleware 未暴露端口到宿主机（符合规范）"
    fi
  fi
done

# ==============================================================================
# 步骤 7: 检测端口范围
# ==============================================================================
log_section "步骤 7: 检测端口范围"

# 提取所有容器端口
CONTAINER_PORTS_ALL=$(grep -oE '[0-9]+:[0-9]+' "$DOCKER_COMPOSE_FILE" | cut -d: -f2 | sort -u)

for port in $CONTAINER_PORTS_ALL; do
  if [[ "$port" -ge 8100 && "$port" -le 8119 ]]; then
    continue
  elif [[ "$port" -eq 8080 || "$port" -eq 8081 || "$port" -eq 80 || "$port" -eq 443 || "$port" -eq 4000 ]]; then
    continue
  elif [[ "$port" -eq 6379 || "$port" -eq 27017 || "$port" -eq 3306 || "$port" -eq 8848 ]]; then
    continue
  elif [[ "$port" -ge 9000 && "$port" -le 9099 ]]; then
    continue
  elif [[ "$port" -ge 9200 && "$port" -le 9299 ]]; then
    continue
  else
    log_warning "检测到非规范容器端口 $port，业务服务容器端口应在 8100-8119 范围内"
    WARNING_COUNT=$((WARNING_COUNT + 1))
  fi
done

# ==============================================================================
# 总结报告
# ==============================================================================
log_section "检测总结"

echo "  ✅ OK:                $(( ${#CONTAINER_PORTS[@]} ))"
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
  log_ok "容器端口映射符合 v4.0 规范"
  if [[ "$WARNING_COUNT" -gt 0 ]]; then
    log_warning "存在 $WARNING_COUNT 个警告项，建议复核"
  fi
  exit 0
fi