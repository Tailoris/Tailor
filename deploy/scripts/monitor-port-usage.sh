#!/usr/bin/env bash
# ==============================================================================
# Tailor IS - 端口占用监控脚本 (v4.0)
# ==============================================================================
# 用途: 监控服务端口占用情况，检测端口冲突
#   1. 扫描所有服务配置文件中的端口配置
#   2. 检测端口冲突（同一端口被多个服务使用）
#   3. 检测端口是否落在规范区间内
#   4. 检测运行时端口占用情况
#   5. 自动处理端口冲突（可选）
#
# 使用方式:
#   bash deploy/scripts/monitor-port-usage.sh --check          # 单次检查
#   bash deploy/scripts/monitor-port-usage.sh --watch          # 持续监控
#   bash deploy/scripts/monitor-port-usage.sh --fix            # 检测并自动修复
#
# 告警级别:
#   CRITICAL - 端口冲突、端口超出规范区间
#   WARNING - 端口即将被占用、运行时异常
#   INFO - 正常信息
# ==============================================================================
set -euo pipefail

RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
GREEN='\033[0;32m'
NC='\033[0m'

WATCH_MODE=0
FIX_MODE=0
CHECK_INTERVAL=300

for arg in "$@"; do
  case "$arg" in
    --watch) WATCH_MODE=1 ;;
    --check) WATCH_MODE=0 ;;
    --fix) FIX_MODE=1 ;;
    --interval=*) CHECK_INTERVAL="${arg#*=}" ;;
    --help|-h) sed -n '2,30p' "$0"; exit 0 ;;
  esac
done

log_critical() { echo -e "${RED}🔴 CRITICAL${NC}: $1"; }
log_warning() { echo -e "${YELLOW}🟡 WARNING${NC}: $1"; }
log_info() { echo -e "${BLUE}🔵 INFO${NC}: $1"; }
log_ok() { echo -e "${GREEN}✅ OK${NC}: $1"; }

declare -A PORT_RANGES=(
  ["user"]="8080-8089"
  ["copyright"]="8080-8089"
  ["admin"]="8080-8089"
  ["merchant"]="8080-8089"
  ["pattern"]="8090-8099"
  ["design"]="8090-8099"
  ["fabric"]="8100-8109"
  ["supply"]="8100-8109"
  ["product"]="8100-8109"
  ["marketing"]="8100-8109"
  ["message"]="8100-8109"
  ["order"]="8110-8119"
  ["trade"]="8110-8119"
  ["payment"]="8110-8119"
  ["data"]="8120-8129"
  ["analytics"]="8120-8129"
  ["community"]="8130-8139"
  ["edu"]="8130-8139"
  ["academy"]="8130-8139"
  ["ai"]="8140-8149"
  ["gateway"]="9000-9009"
)

check_port_allocation() {
  local critical_count=0
  local warning_count=0
  
  log_info "[$(date '+%Y-%m-%d %H:%M:%S')] 开始端口占用检查"
  
  declare -A PORT_USAGE=()
  
  BACKEND_DIR="tailor-is"
  for app_yml in $(find "$BACKEND_DIR" -name "application.yml" -path "*/src/main/resources/*"); do
    SERVICE_NAME=$(basename $(dirname $(dirname $(dirname "$app_yml"))) | sed 's/tailor-is-//')
    PORT=$(grep -E 'server:\s*\n\s*port:' "$app_yml" | grep -oE '[0-9]+' | head -1)
    
    if [[ -z "$PORT" ]]; then
      PORT=$(grep -E 'port:\s*\$\{SERVER_PORT:[0-9]+\}' "$app_yml" | grep -oE '[0-9]+' | head -1)
    fi
    
    if [[ -z "$PORT" ]]; then
      log_warning "$SERVICE_NAME 未配置端口"
      warning_count=$((warning_count + 1))
      continue
    fi
    
    if [[ -n "${PORT_USAGE[$PORT]}" ]]; then
      log_critical "端口冲突: 端口 $PORT 被 ${PORT_USAGE[$PORT]} 和 $SERVICE_NAME 同时使用"
      critical_count=$((critical_count + 1))
      
      if [[ "$FIX_MODE" == "1" ]]; then
        log_info "尝试自动修复端口冲突..."
        FIXED=false
        for service_type in "${!PORT_RANGES[@]}"; do
          if [[ "$SERVICE_NAME" == *"$service_type"* ]]; then
            RANGE="${PORT_RANGES[$service_type]}"
            MIN=$(echo "$RANGE" | cut -d- -f1)
            MAX=$(echo "$RANGE" | cut -d- -f2)
            for candidate_port in $(seq "$MIN" "$MAX"); do
              if [[ -z "${PORT_USAGE[$candidate_port]}" ]]; then
                log_info "将 $SERVICE_NAME 端口从 $PORT 修改为 $candidate_port"
                sed -i "s/server:\s*\n\s*port:\s*${PORT}/server:\n  port: ${candidate_port}/" "$app_yml"
                PORT_USAGE[$candidate_port]="$SERVICE_NAME"
                FIXED=true
                break
              fi
            done
            break
          fi
        done
        
        if [[ "$FIXED" == "false" ]]; then
          log_critical "无法自动修复端口冲突，区间内所有端口均已被占用"
        fi
      fi
    else
      PORT_USAGE[$PORT]="$SERVICE_NAME"
    fi
    
    VALID=false
    for service_type in "${!PORT_RANGES[@]}"; do
      if [[ "$SERVICE_NAME" == *"$service_type"* ]]; then
        RANGE="${PORT_RANGES[$service_type]}"
        MIN=$(echo "$RANGE" | cut -d- -f1)
        MAX=$(echo "$RANGE" | cut -d- -f2)
        if [[ "$PORT" -ge "$MIN" && "$PORT" -le "$MAX" ]]; then
          VALID=true
          break
        fi
      fi
    done
    
    if [[ "$VALID" == "false" ]]; then
      log_critical "$SERVICE_NAME 端口 $PORT 不在规范区间内"
      critical_count=$((critical_count + 1))
    fi
  done
  
  log_info "已扫描 ${#PORT_USAGE[@]} 个服务端口配置"
  
  if [[ "$critical_count" -gt 0 ]]; then
    log_critical "检测到 $critical_count 个严重违规"
    return 1
  else
    log_ok "端口分配检查通过"
    return 0
  fi
}

check_runtime_ports() {
  log_info "检测运行时端口占用情况"
  
  for port in "${!PORT_USAGE[@]}"; do
    if ss -tlnp | grep -q ":$port "; then
      PID=$(ss -tlnp | grep ":$port " | grep -oE 'pid=[0-9]+' | cut -d= -f2)
      if [[ -n "$PID" ]]; then
        PROCESS=$(ps -p "$PID" -o comm=)
        log_ok "端口 $port 正在被 $PROCESS(PID:$PID) 使用"
      else
        log_warning "端口 $port 已被占用，但无法识别进程"
      fi
    else
      log_warning "端口 $port 已配置但未在运行"
    fi
  done
}

if [[ "$WATCH_MODE" == "1" ]]; then
  log_info "进入持续监控模式，检查间隔: ${CHECK_INTERVAL}s"
  while true; do
    check_port_allocation
    check_runtime_ports
    sleep "$CHECK_INTERVAL"
  done
else
  check_port_allocation
  check_runtime_ports
  exit "$?"
fi