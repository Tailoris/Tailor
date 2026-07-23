#!/usr/bin/env bash
# ==============================================================================
# Tailor IS - 路由端口规范综合检查脚本 (v4.0)
# ==============================================================================
# 用途: Maven 构建环节强制执行路由端口规范检查
#   1. 检测所有 application.yml 端口配置是否符合规范区间
#   2. 检测 Controller @RequestMapping 是否使用 /api/v1/ 前缀
#   3. 检测是否使用 RoutePortConstant 常量
#   4. 检测网关路由配置与后端接口一致性
#   5. 检测端口冲突
#
# 使用方式:
#   bash deploy/scripts/check-route-port-spec.sh
#
# 退出码:
#   0 - 通过
#   1 - 违规
# ==============================================================================
set -euo pipefail

RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
GREEN='\033[0;32m'
NC='\033[0m'

log_critical() { echo -e "${RED}🔴 CRITICAL${NC}: $1"; }
log_warning() { echo -e "${YELLOW}🟡 WARNING${NC}: $1"; }
log_info() { echo -e "${BLUE}🔵 INFO${NC}: $1"; }
log_ok() { echo -e "${GREEN}✅ OK${NC}: $1"; }
log_section() { echo ""; echo -e "${GREEN}==== $1 ====${NC}"; }

CRITICAL_COUNT=0
WARNING_COUNT=0

# v4.0 规范端口区间定义 (8100-8117 连续区间)
#   核心业务服务: 8100-8107 (admin, user, product, order, payment, marketing, ai, copyright)
#   轻量业务服务: 8108-8117 (community, supply, merchant, message, academy, analytics, im, pattern, design, knowledge)
declare -A PORT_RANGES=(
  ["admin"]="8100-8100"
  ["user"]="8101-8101"
  ["product"]="8102-8102"
  ["order"]="8103-8103"
  ["payment"]="8104-8104"
  ["marketing"]="8105-8105"
  ["ai"]="8106-8106"
  ["copyright"]="8107-8107"
  ["community"]="8108-8108"
  ["supply"]="8109-8109"
  ["merchant"]="8110-8110"
  ["message"]="8111-8111"
  ["academy"]="8112-8112"
  ["analytics"]="8113-8113"
  ["im"]="8114-8114"
  ["pattern"]="8115-8115"
  ["design"]="8116-8116"
  ["knowledge"]="8117-8117"
  ["gateway"]="9000-9000"
)

# 步骤 1: 检测 application.yml 端口配置
log_section "步骤 1: 检测 application.yml 端口配置"

BACKEND_DIR="tailor-is"
for app_yml in $(find "$BACKEND_DIR" -name "application.yml" -path "*/src/main/resources/*"); do
  SERVICE_NAME=$(basename $(dirname $(dirname $(dirname "$app_yml"))) | sed 's/tailor-is-//')
  PORT=$(grep -E '^server:' "$app_yml" | sed -n '/server:/,/spring:/p' | grep -oE 'port:\s*[0-9]+' | grep -oE '[0-9]+')
  
  if [[ -z "$PORT" ]]; then
    log_warning "$app_yml 未配置 server.port"
    WARNING_COUNT=$((WARNING_COUNT + 1))
    continue
  fi
  
  PORT=${PORT##*=}
  PORT=${PORT//[^0-9]/}
  
  FOUND_RANGE="false"
  for service_type in "${!PORT_RANGES[@]}"; do
    if [[ "$SERVICE_NAME" == *"$service_type"* ]]; then
      RANGE="${PORT_RANGES[$service_type]}"
      MIN=$(echo "$RANGE" | cut -d- -f1)
      MAX=$(echo "$RANGE" | cut -d- -f2)
      if [[ "$PORT" -ge "$MIN" && "$PORT" -le "$MAX" ]]; then
        log_ok "$SERVICE_NAME 端口 $PORT 在规范区间 $RANGE 内"
      else
        log_critical "$SERVICE_NAME 端口 $PORT 不在规范区间 $RANGE 内"
        CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
      fi
      FOUND_RANGE="true"
      break
    fi
  done
  
  if [[ "$FOUND_RANGE" == "false" ]]; then
    log_warning "$SERVICE_NAME 未匹配到端口区间定义"
    WARNING_COUNT=$((WARNING_COUNT + 1))
  fi
done

# 步骤 2: 检测 Controller 路由前缀
log_section "步骤 2: 检测 Controller 路由前缀"

CONTROLLER_FILES=$(find "$BACKEND_DIR" -name "*Controller.java" -path "*/src/main/java/*")
for controller in $CONTROLLER_FILES; do
  REQUEST_MAPPING=$(grep -E '@RequestMapping\s*\(' "$controller" | head -1)
  if [[ -z "$REQUEST_MAPPING" ]]; then
    continue
  fi
  
  PATH=$(echo "$REQUEST_MAPPING" | grep -oE '"/[^"]+"' | tr -d '"')
  
  if [[ "$PATH" == "/api/v1/"* ]]; then
    log_ok "$(basename "$controller") 路由前缀正确: $PATH"
  elif [[ "$PATH" == "/api/"* ]]; then
    log_warning "$(basename "$controller") 使用旧路由前缀: $PATH（建议升级为 /api/v1/）"
    WARNING_COUNT=$((WARNING_COUNT + 1))
  elif [[ "$PATH" != "/" ]]; then
    log_critical "$(basename "$controller") 路由前缀不符合规范: $PATH（应为 /api/v1/ 开头）"
    CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
  fi
done

# 步骤 3: 检测 RoutePortConstant 使用情况
log_section "步骤 3: 检测 RoutePortConstant 使用情况"

CONSTANT_FILE="$BACKEND_DIR/tailor-is-common/src/main/java/com/tailoris/common/constant/RoutePortConstant.java"
if [[ -f "$CONSTANT_FILE" ]]; then
  log_ok "RoutePortConstant.java 存在"
else
  log_critical "RoutePortConstant.java 不存在"
  CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
fi

CONTROLLERS_USING_CONSTANT=$(grep -r "RoutePortConstant" "$BACKEND_DIR" --include="*.java" | grep -v "$CONSTANT_FILE" | wc -l)
if [[ "$CONTROLLERS_USING_CONSTANT" -gt 0 ]]; then
  log_ok "$CONTROLLERS_USING_CONSTANT 个文件使用了 RoutePortConstant"
else
  log_warning "未检测到使用 RoutePortConstant 的文件（建议统一使用常量）"
  WARNING_COUNT=$((WARNING_COUNT + 1))
fi

# 步骤 4: 检测网关路由与后端接口一致性
log_section "步骤 4: 检测网关路由与后端接口一致性"

GATEWAY_ROUTES=$(grep -oE '/api/v1/[a-z-]+' "$BACKEND_DIR/tailor-is-core-gateway/src/main/java/com/tailoris/coregateway/config/CoreGatewayRouteConfig.java" "$BACKEND_DIR/tailor-is-lite-gateway/src/main/java/com/tailoris/litegateway/config/LiteGatewayRouteConfig.java" | sort -u)

CONTROLLER_ROUTES=$(grep -roE '@RequestMapping\("/api/v1/[a-z-]+' "$BACKEND_DIR" --include="*.java" | grep -oE '/api/v1/[a-z-]+' | sort -u)

for gateway_route in $GATEWAY_ROUTES; do
  if echo "$CONTROLLER_ROUTES" | grep -q "^$gateway_route"; then
    log_ok "网关路由 $gateway_route 有对应后端接口"
  else
    log_warning "网关路由 $gateway_route 未找到对应后端接口"
    WARNING_COUNT=$((WARNING_COUNT + 1))
  fi
done

# 步骤 5: 检测端口冲突
log_section "步骤 5: 检测端口冲突"

USED_PORTS=$(grep -roE 'server:\s*\n\s*port:\s*[0-9]+' "$BACKEND_DIR" --include="*.yml" | grep -oE '[0-9]+' | sort)
DUPLICATES=$(echo "$USED_PORTS" | uniq -d)

if [[ -n "$DUPLICATES" ]]; then
  log_critical "检测到端口冲突: $DUPLICATES"
  CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
else
  log_ok "未检测到端口冲突"
fi

# 总结报告
log_section "检测总结"

echo "  ✅ OK:                $(( (${#PORT_RANGES[@]} * 2) + 2 - CRITICAL_COUNT - WARNING_COUNT ))"
echo "  🔵 INFO:              0"
echo "  🟡 WARNING:           $WARNING_COUNT"
echo "  🔴 CRITICAL:          $CRITICAL_COUNT"
echo ""

if [[ "$CRITICAL_COUNT" -gt 0 ]]; then
  log_critical "检测到 $CRITICAL_COUNT 个严重违规，请修复后重新检测"
  exit 1
else
  log_ok "路由端口规范检查通过"
  if [[ "$WARNING_COUNT" -gt 0 ]]; then
    log_warning "存在 $WARNING_COUNT 个警告项，建议复核"
  fi
  exit 0
fi