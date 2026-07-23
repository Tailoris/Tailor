#!/usr/bin/env bash
# ==============================================================================
# Tailor IS - 端口占用冲突检测脚本
# ==============================================================================
# 用途: 扫描宿主机当前监听的 TCP 端口，与《Tailor-IS-系统路由与端口规范 v3.0》
#       §2 端口分配总表中的标准登记表比对，输出冲突/未登记/缺失告警。
#
# 关联文档: docs/Tailor-IS-系统路由与端口规范.md (v3.0)
#
# 使用方式:
#   bash deploy/scripts/check-port-conflicts.sh                # 严格模式（CRITICAL 时退出 1）
#   bash deploy/scripts/check-port-conflicts.sh --no-fail      # 宽松模式（仅展示，退出 0）
#   bash deploy/scripts/check-port-conflicts.sh --extra "8086,8087"  # 追加白名单端口
#
# 退出码:
#   0 - 无 CRITICAL 冲突
#   1 - 存在 CRITICAL 冲突（标准端口被非预期服务占用）
#   2 - 脚本执行错误（如缺少 ss/netstat 命令）
#
# CI/CD 集成:
#   - name: 端口冲突检测
#     run: bash deploy/scripts/check-port-conflicts.sh --no-fail
# ==============================================================================
set -euo pipefail

# ==============================================================================
# 颜色定义
# ==============================================================================
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

# ==============================================================================
# 参数解析
# ==============================================================================
NO_FAIL=0
EXTRA_PORTS=""
for arg in "$@"; do
  case "$arg" in
    --no-fail)
      NO_FAIL=1
      ;;
    --extra)
      # 下一个参数是端口列表（简单解析）
      shift_next=1
      ;;
    --extra=*)
      EXTRA_PORTS="${arg#--extra=}"
      ;;
    --help|-h)
      sed -n '2,20p' "$0"
      exit 0
      ;;
    *)
      if [[ "${shift_next:-0}" == "1" ]]; then
        EXTRA_PORTS="$arg"
        shift_next=0
      fi
      ;;
  esac
done

# ==============================================================================
# 标准端口登记表（与 docs/Tailor-IS-系统路由与端口规范.md v3.0 §2 对齐）
# 格式: "端口|服务名|类别"
# 类别: gateway / core / lite / common / frontend / infra / monitor / onepanel
# ==============================================================================
STANDARD_PORTS=(
  # 1Panel 管理端口（不可侵占）
  "3306|MySQL|onepanel"
  "5672|RabbitMQ-AMQP|onepanel"
  "6379|Redis|onepanel"
  "8080|Nacos-UI|onepanel"
  "8848|Nacos-API|onepanel"
  "9848|Nacos-gRPC|onepanel"
  "15672|RabbitMQ-Mgmt|onepanel"
  "11336|1Panel|onepanel"

  # 网关层
  "9000|core-gateway|gateway"
  "8081|lite-gateway|gateway"

  # 核心业务服务（core-gateway 路由域）
  "8100|admin-service|core"
  "8101|user-service|core"
  "8102|product-service|core"
  "8103|order-service|core"
  "8104|payment-service|core"
  "8105|marketing-service|core"
  "8106|ai-service|core"
  "8107|copyright-service|core"
  "8110|merchant-service|core"
  "8115|pattern-service|core"

  # 轻量业务服务（lite-gateway 路由域）
  "8108|community-service|lite"
  "8109|supply-service|lite"
  "8111|message-service|lite"
  "8112|academy-service|lite"
  "8113|analytics-service|lite"
  "8114|message-im-service|lite"

  # 公共服务
  "8115|pattern-service|lite"
  "8116|design-service|lite"
  "8117|knowledge-service|lite"
  "4000|graphql-gateway|common"

  # 前端服务
  "3000|pc-mall-ssr|frontend"
  "3002|merchant-admin|frontend"
  "3003|platform-admin|frontend"
  "3004|mobile-app-h5|frontend"

  # 基础设施（业务自管）
  "26379|Redis-Sentinel|infra"
  "8858|Sentinel-Dashboard|infra"

  # 监控运维
  "80|Nginx-HTTP|monitor"
  "443|Nginx-HTTPS|monitor"
  "9090|Prometheus|monitor"
  "3001|Grafana|monitor"
  "9093|Alertmanager|monitor"
  "9095|Alert-Webhook|monitor"
  "9100|Node-Exporter|monitor"
  "9104|MySQLd-Exporter|monitor"
  "9121|Redis-Exporter|monitor"
  "9115|Blackbox-Exporter|monitor"
)

# ==============================================================================
# 1Panel 受保护端口清单（不可侵占）
# ==============================================================================
ONEPANEL_PORTS=(3306 5672 6379 8080 8848 9848 15672 11336)

# ==============================================================================
# 工具函数
# ==============================================================================
log_critical() {
  echo -e "${RED}🔴 CRITICAL${NC}: $1"
}

log_warning() {
  echo -e "${YELLOW}🟡 WARNING${NC}: $1"
}

log_info() {
  echo -e "${BLUE}🔵 INFO${NC}: $1"
}

log_ok() {
  echo -e "${GREEN}✅ OK${NC}: $1"
}

log_section() {
  echo ""
  echo -e "${GREEN}==== $1 ====${NC}"
}

# 检查命令是否存在
has_command() {
  command -v "$1" >/dev/null 2>&1
}

# ==============================================================================
# 获取当前监听的 TCP 端口列表
# 返回格式: "端口|进程信息" 每行一条
# ==============================================================================
get_listening_ports() {
  if has_command ss; then
    # ss 输出示例: LISTEN 0 128 0.0.0.0:9000 0.0.0.0:* users:(("docker-proxy",pid=1234,fd=4))
    ss -tlnp 2>/dev/null | awk '
      /LISTEN/ {
        for (i=1; i<=NF; i++) {
          if ($i ~ /:[0-9]+$/) {
            # 提取本地地址字段中的端口
            split($i, a, ":")
            port = a[length(a)]
            # 收集进程信息
            proc = ""
            for (j=i; j<=NF; j++) {
              if ($j ~ /users:/) {
                proc = $j
                break
              }
            }
            # 清理特殊字符
            gsub(/users:\(\("/, "", proc)
            gsub(/"\).*/, "", proc)
            gsub(/",pid=.*/, "", proc)
            print port "|" proc
          }
        }
      }
    ' | sort -u
  elif has_command netstat; then
    # netstat 回退方案
    netstat -tlnp 2>/dev/null | awk '
      /LISTEN/ {
        # 第4列是本地地址，格式 0.0.0.0:9000 或 *:9000 或 :::9000
        split($4, a, ":")
        port = a[length(a)]
        # 第7列是进程信息 pid/program
        proc = $7
        gsub(/[0-9]+\//, "", proc)
        print port "|" proc
      }
    ' | sort -u
  else
    echo ""
    return 1
  fi
}

# ==============================================================================
# 构建标准端口查询表
# ==============================================================================
declare -A STANDARD_PORT_MAP
declare -A STANDARD_CATEGORY_MAP

for entry in "${STANDARD_PORTS[@]}"; do
  port="${entry%%|*}"
  rest="${entry#*|}"
  service="${rest%%|*}"
  category="${rest##*|}"
  STANDARD_PORT_MAP["$port"]="$service"
  STANDARD_CATEGORY_MAP["$port"]="$category"
done

# ==============================================================================
# 主流程
# ==============================================================================
echo "Tailor IS 端口占用冲突检测"
echo "执行时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "主机名: $(hostname)"
echo "标准登记表版本: v3.0 (2026-07-02)"
echo ""

log_section "步骤 1: 扫描当前监听端口"

LISTENING_OUTPUT=$(get_listening_ports)
if [[ -z "$LISTENING_OUTPUT" ]]; then
  log_critical "无法获取监听端口列表（需要 ss 或 netstat 命令）"
  exit 2
fi

# 解析监听端口
declare -A LISTENING_PORT_PROC
LISTENING_PORT_COUNT=0
while IFS='|' read -r port proc; do
  if [[ -n "$port" ]]; then
    # 过滤非数字端口
    if [[ "$port" =~ ^[0-9]+$ ]]; then
      LISTENING_PORT_PROC["$port"]="${proc:-unknown}"
      LISTENING_PORT_COUNT=$((LISTENING_PORT_COUNT + 1))
    fi
  fi
done <<< "$LISTENING_OUTPUT"

log_ok "扫描到 $LISTENING_PORT_COUNT 个监听端口"

# ==============================================================================
# 步骤 2: 比对标准登记表 - 检查冲突（标准端口被非预期服务占用）
# ==============================================================================
log_section "步骤 2: 检查标准端口冲突"

CRITICAL_COUNT=0
WARNING_COUNT=0
INFO_COUNT=0
OK_COUNT=0

for port in "${!STANDARD_PORT_MAP[@]}"; do
  expected_service="${STANDARD_PORT_MAP[$port]}"
  category="${STANDARD_CATEGORY_MAP[$port]}"

  if [[ -n "${LISTENING_PORT_PROC[$port]:-}" ]]; then
    actual_proc="${LISTENING_PORT_PROC[$port]}"
    # 简单的进程名匹配（用于判断是否为预期服务）
    local_expected_lower=$(echo "$expected_service" | tr '[:upper:]' '[:lower:]')
    local_actual_lower=$(echo "$actual_proc" | tr '[:upper:]' '[:lower:]')

    # 检查是否是 1Panel 端口被非 1Panel 服务占用
    if [[ " ${ONEPANEL_PORTS[*]} " =~ " $port " ]]; then
      # 1Panel 端口：仅检查是否被 Docker 服务占用
      if echo "$actual_proc" | grep -qiE "docker|java|node|nginx|python|ruby|go"; then
        # 进一步检查是否为预期服务
        if echo "$actual_proc" | grep -qiE "(nacos|mysql|redis|rabbitmq|1panel|mariadb)" || [[ "$local_expected_lower" == "$local_actual_lower" ]]; then
          log_ok "1Panel 端口 $port ($expected_service) 由 $actual_proc 监听 - 符合预期"
          OK_COUNT=$((OK_COUNT + 1))
        else
          log_critical "1Panel 端口 $port (预期: $expected_service) 被非预期进程占用: $actual_proc"
          log_critical "  回避建议: 立即停止冲突进程 $actual_proc，1Panel 端口绝对不可侵占"
          CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
        fi
      else
        log_ok "1Panel 端口 $port ($expected_service) 由 $actual_proc 监听 - 符合预期"
        OK_COUNT=$((OK_COUNT + 1))
      fi
    else
      # 普通业务端口：仅检查是否被同名进程占用（不强制，仅提示）
      if echo "$actual_proc" | grep -qiE "$(echo $expected_service | tr '-' '.')"; then
        log_ok "端口 $port ($expected_service) 由 $actual_proc 监听 - 符合预期"
        OK_COUNT=$((OK_COUNT + 1))
      else
        # 端口被占用但进程名不匹配，可能是同名服务的不同实例，不直接判为冲突
        log_info "端口 $port (登记: $expected_service) 当前由 $actual_proc 监听"
        INFO_COUNT=$((INFO_COUNT + 1))
      fi
    fi
  else
    # 标准端口未监听
    if [[ "$category" == "onepanel" ]]; then
      log_warning "1Panel 端口 $port ($expected_service) 未监听 - 请检查 1Panel 服务状态"
      WARNING_COUNT=$((WARNING_COUNT + 1))
    else
      log_info "登记端口 $port ($expected_service) 未监听 - 服务可能未启动"
      INFO_COUNT=$((INFO_COUNT + 1))
    fi
  fi
done

# ==============================================================================
# 步骤 3: 检查未登记端口（监听但不在标准表中）
# ==============================================================================
log_section "步骤 3: 检查未登记端口"

# 合并额外白名单端口
EXTRA_PORT_LIST=()
if [[ -n "$EXTRA_PORTS" ]]; then
  IFS=',' read -ra EXTRA_PORT_LIST <<< "$EXTRA_PORTS"
fi

UNREGISTERED_COUNT=0
for port in "${!LISTENING_PORT_PROC[@]}"; do
  if [[ -z "${STANDARD_PORT_MAP[$port]:-}" ]]; then
    # 检查是否在额外白名单中
    in_extra=0
    for extra_port in "${EXTRA_PORT_LIST[@]}"; do
      if [[ "$port" == "$extra_port" ]]; then
        in_extra=1
        break
      fi
    done

    actual_proc="${LISTENING_PORT_PROC[$port]}"
    if [[ "$in_extra" == "1" ]]; then
      log_info "端口 $port ($actual_proc) 在额外白名单中 - 跳过"
    else
      # 检查是否是常见的系统端口（如 22 SSH, 25 SMTP 等）
      case "$port" in
        22|25|111|631)
          log_info "系统端口 $port ($actual_proc) - 跳过"
          ;;
        *)
          log_warning "端口 $port 未在标准登记表中 - 当前由 $actual_proc 监听"
          log_warning "  处理建议: 若为正式服务，请按规范文档 §9 流程补登记；若为临时调试，请使用 --extra 参数加入白名单"
          UNREGISTERED_COUNT=$((UNREGISTERED_COUNT + 1))
          WARNING_COUNT=$((WARNING_COUNT + 1))
          ;;
      esac
    fi
  fi
done

if [[ "$UNREGISTERED_COUNT" == "0" ]]; then
  log_ok "无未登记端口"
fi

# ==============================================================================
# 步骤 4: 1Panel 端口冲突专项检查
# ==============================================================================
log_section "步骤 4: 1Panel 端口冲突专项检查"

ONEPANEL_CONFLICT=0
for port in "${ONEPANEL_PORTS[@]}"; do
  # 检查 docker-compose 文件是否映射了 1Panel 端口
  compose_files=(
    "deploy/docker-compose.services.yml"
    "deploy/docker-compose.prod.yml"
    "deploy/docker-compose.development.yml"
    "deploy/docker-compose.monitoring.yml"
    "deploy/docker-compose.frontend.yml"
    "deploy/docker-compose.staging.yml"
  )

  for compose_file in "${compose_files[@]}"; do
    full_path="$(cd "$(dirname "$0")/../.." && pwd)/$compose_file"
    if [[ -f "$full_path" ]]; then
      # 检查端口映射 "8080:" 或 ":8080" 形式
      if grep -E "^\s+-\s+\"${port}:" "$full_path" >/dev/null 2>&1; then
        log_critical "1Panel 端口 $port 在 $compose_file 中被映射为宿主端口"
        log_critical "  回避建议: 修改该服务的宿主端口映射，1Panel 端口不可侵占"
        ONEPANEL_CONFLICT=$((ONEPANEL_CONFLICT + 1))
        CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
      fi
    fi
  done
done

if [[ "$ONEPANEL_CONFLICT" == "0" ]]; then
  log_ok "docker-compose 文件未映射 1Panel 端口"
fi

# ==============================================================================
# 步骤 5: 总结报告
# ==============================================================================
log_section "检测总结"

echo "  ✅ OK (符合预期):       $OK_COUNT"
echo "  🔵 INFO (信息性):       $INFO_COUNT"
echo "  🟡 WARNING (警告):      $WARNING_COUNT"
echo "  🔴 CRITICAL (严重冲突): $CRITICAL_COUNT"
echo ""

if [[ "$CRITICAL_COUNT" -gt 0 ]]; then
  log_critical "检测到 $CRITICAL_COUNT 个严重冲突，必须立即处理！"
  log_critical "参考文档: docs/Tailor-IS-系统路由与端口规范.md (v3.0) §8.3 自动回避策略"

  if [[ "$NO_FAIL" == "1" ]]; then
    log_warning "--no-fail 模式：不退出非零码"
    exit 0
  else
    exit 1
  fi
else
  log_ok "未检测到严重端口冲突"
  if [[ "$WARNING_COUNT" -gt 0 ]]; then
    log_warning "存在 $WARNING_COUNT 个警告项，建议复核"
  fi
  exit 0
fi
