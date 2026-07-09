#!/usr/bin/env bash
# ==============================================================================
# Tailor IS - v4.0 规范全量校验主入口脚本
# ==============================================================================
# 用途: 执行全部5套自动化校验脚本，输出综合检测报告
#
# 权威文档: Tailor-IS 企业级微服务路由与端口规范（v4.0 优化版）
#
# 使用方式:
#   bash deploy/scripts/run-all-checks.sh                # 严格模式（CI/CD使用）
#   bash deploy/scripts/run-all-checks.sh --no-fail      # 宽松模式（开发自测）
#   bash deploy/scripts/run-all-checks.sh --report       # 输出详细报告
#
# 退出码:
#   0 - 所有检测通过
#   1 - 存在严重违规
# ==============================================================================
set -euo pipefail

RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
GREEN='\033[0;32m'
NC='\033[0m'

NO_FAIL=0
GENERATE_REPORT=0

for arg in "$@"; do
  case "$arg" in
    --no-fail) NO_FAIL=1 ;;
    --report) GENERATE_REPORT=1 ;;
    --help|-h) sed -n '2,20p' "$0"; exit 0 ;;
  esac
done

log_section() { echo ""; echo -e "${GREEN}=======================================================${NC}"; echo -e "${GREEN}=== $1 ===${NC}"; echo -e "${GREEN}=======================================================${NC}"; }
log_summary() { echo -e "${BLUE}=== $1 ===${NC}"; }

# 脚本路径
SCRIPTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

CHECKS=(
  "网关路由一致性检测" "$SCRIPTS_DIR/check-gateway-routes.sh"
  "Nginx配置静态校验" "$SCRIPTS_DIR/check-nginx-config.sh"
  "端口分配冲突检测" "$SCRIPTS_DIR/check-port-allocation.sh"
  "容器端口映射检测" "$SCRIPTS_DIR/check-container-ports.sh"
  "API前缀规范检测" "$SCRIPTS_DIR/check-api-prefix.sh"
)

TOTAL_CRITICAL=0
TOTAL_WARNING=0
TOTAL_OK=0

# ==============================================================================
# 执行全部检测
# ==============================================================================
log_section "Tailor IS v4.0 规范全量校验"

for ((i=0; i<${#CHECKS[@]}; i+=2)); do
  CHECK_NAME="${CHECKS[i]}"
  CHECK_SCRIPT="${CHECKS[i+1]}"
  
  log_section "$CHECK_NAME"
  
  if [[ -f "$CHECK_SCRIPT" ]]; then
    if [[ "$NO_FAIL" == "1" ]]; then
      bash "$CHECK_SCRIPT" --no-fail
      EXIT_CODE=$?
    else
      bash "$CHECK_SCRIPT"
      EXIT_CODE=$?
    fi
    
    if [[ "$EXIT_CODE" -eq 0 ]]; then
      TOTAL_OK=$((TOTAL_OK + 1))
    else
      TOTAL_CRITICAL=$((TOTAL_CRITICAL + 1))
    fi
  else
    echo -e "${RED}🔴 脚本不存在: $CHECK_SCRIPT${NC}"
    TOTAL_CRITICAL=$((TOTAL_CRITICAL + 1))
  fi
done

# ==============================================================================
# 综合报告
# ==============================================================================
log_section "综合检测报告"

echo ""
echo -e "${BLUE}┌─────────────────────────────────────────────────────────────┐${NC}"
echo -e "${BLUE}│                 Tailor IS v4.0 规范校验报告                  │${NC}"
echo -e "${BLUE}├─────────────────────────────────────────────────────────────┤${NC}"
echo -e "${BLUE}│  检测项总数:          ${GREEN}5${NC}"
echo -e "${BLUE}│  ✅ 通过:             ${GREEN}$TOTAL_OK${NC}"
echo -e "${BLUE}│  🟡 警告:             ${YELLOW}$TOTAL_WARNING${NC}"
echo -e "${BLUE}│  🔴 严重违规:         ${RED}$TOTAL_CRITICAL${NC}"
echo -e "${BLUE}└─────────────────────────────────────────────────────────────┘${NC}"
echo ""

if [[ "$GENERATE_REPORT" == "1" ]]; then
  REPORT_FILE="tailoris-v4-check-report-$(date +%Y%m%d-%H%M%S).txt"
  echo "生成报告文件: $REPORT_FILE"
  echo ""
  
  echo "========================================================" > "$REPORT_FILE"
  echo "Tailor IS v4.0 规范校验报告" >> "$REPORT_FILE"
  echo "生成时间: $(date '+%Y-%m-%d %H:%M:%S')" >> "$REPORT_FILE"
  echo "========================================================" >> "$REPORT_FILE"
  echo "" >> "$REPORT_FILE"
  echo "检测项总数: 5" >> "$REPORT_FILE"
  echo "通过: $TOTAL_OK" >> "$REPORT_FILE"
  echo "警告: $TOTAL_WARNING" >> "$REPORT_FILE"
  echo "严重违规: $TOTAL_CRITICAL" >> "$REPORT_FILE"
  echo "" >> "$REPORT_FILE"
  
  if [[ "$TOTAL_CRITICAL" -eq 0 ]]; then
    echo "结论: 全部检测通过，符合 v4.0 规范" >> "$REPORT_FILE"
  else
    echo "结论: 存在 $TOTAL_CRITICAL 个严重违规，不符合 v4.0 规范" >> "$REPORT_FILE"
  fi
fi

# ==============================================================================
# 退出处理
# ==============================================================================
if [[ "$TOTAL_CRITICAL" -gt 0 ]]; then
  echo -e "${RED}🔴 检测失败: 存在 $TOTAL_CRITICAL 个严重违规，不符合 v4.0 规范${NC}"
  echo ""
  echo -e "${BLUE}请修复以下问题后重新检测:${NC}"
  echo "  1. 网关路由配置（check-gateway-routes.sh）"
  echo "  2. Nginx 配置（check-nginx-config.sh）"
  echo "  3. 端口分配（check-port-allocation.sh）"
  echo "  4. 容器端口映射（check-container-ports.sh）"
  echo "  5. API 前缀规范（check-api-prefix.sh）"
  
  if [[ "$NO_FAIL" == "1" ]]; then
    exit 0
  else
    exit 1
  fi
else
  echo -e "${GREEN}✅ 检测通过: 全部符合 v4.0 规范${NC}"
  exit 0
fi