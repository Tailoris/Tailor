#!/usr/bin/env bash
# =============================================================================
# Tailor IS - 1Panel 集成化部署执行脚本 (v1.1)
# ==============================================================================
# 前置条件:
#   1. 1Panel 面板已安装, 端口 11336, 安装了 MySQL / Redis / RabbitMQ / Nacos / OpenResty
#   2. 当前用户已加入 docker 组 (或具备 sudo)
#   3. deploy/.env.production 已填入正确凭据
#
# 使用:
#   sudo ./deploy-to-1panel.sh --check      # 仅做环境/连通性检查
#   sudo ./deploy-to-1panel.sh --up         # 拉取镜像 + up -d (默认)
#   sudo ./deploy-to-1panel.sh --down       # 下线业务容器 (保留数据)
#   sudo ./deploy-to-1panel.sh --logs       # 跟踪最近 200 行业务容器日志
# ==============================================================================

set -eo pipefail

RED='\033[0;31m';  GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log_info()  { echo -e "${BLUE}[INFO]${NC}  $1"; }
log_ok()    { echo -e "${GREEN}[ OK ]${NC}  $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $1"; }
log_error() { echo -e "${RED}[FAIL]${NC}  $1"; }

PROJECT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
ENV_FILE="${PROJECT_DIR}/.env"
COMPOSE_FILE="${PROJECT_DIR}/docker-compose.prod.yml"

# ---- helpers ----
tcp_open() {
  local host="$1" port="$2"; (echo > /dev/tcp/"$host"/"$port") 2>/dev/null
}
http_status() {
  curl -sS -m 5 -o /dev/null -w "%{http_code}" "$1" 2>/dev/null || echo "000"
}
load_env() {
  # export values from .env (ignore comments and empty lines, strip CR)
  if [ -f "$ENV_FILE" ]; then
    while IFS='=' read -r k v; do
      [[ -z "$k" || "$k" =~ ^# ]] && continue
      k="${k//[[:space:]]/}"; v="${v%$'\r'}"
      # shellcheck disable=SC2163
      export "$k=$v"
    done < "$ENV_FILE"
    log_ok "已载入 ${ENV_FILE}"
  else
    log_error "${ENV_FILE} 不存在, 请先执行: cp deploy/.env.production .env"
    exit 1
  fi
}

# ---- checks ----
check_tools() {
  log_info "检查 docker/docker-compose ..."
  if ! command -v docker >/dev/null 2>&1; then
    log_error "docker 未安装或不可用"
    exit 1
  fi
  if docker compose version >/dev/null 2>&1; then
    COMPOSE_BIN="docker compose"
  elif command -v docker-compose >/dev/null 2>&1; then
    COMPOSE_BIN="docker-compose"
  else
    log_error "docker compose (v2) 或 docker-compose (v1) 均未找到"
    exit 1
  fi
  # 权限检查 - 非 root 时, 无权限会提示加入 docker 组
  if [ "$(id -u)" -ne 0 ] && ! docker info >/dev/null 2>&1; then
    log_error "当前用户无 docker 权限, 请加入 docker 组: sudo usermod -aG docker $(id -un); newgrp docker"
    exit 1
  fi
  log_ok "工具链就绪 ($COMPOSE_BIN)"
}

check_1panel_infra() {
  log_info "扫描 1Panel 基础设施端口 ..."
  local ok=1
  declare -A targets=(
    ["1Panel 面板 (11336)"]="127.0.0.1:11336"
    ["MySQL (3306)"]="127.0.0.1:3306"
    ["Redis (6379)"]="127.0.0.1:6379"
    ["RabbitMQ AMQP (5672)"]="127.0.0.1:5672"
    ["RabbitMQ UI (15672)"]="127.0.0.1:15672"
    ["Nacos API (8848)"]="127.0.0.1:8848"
    ["Nacos UI (8081)"]="127.0.0.1:8081"
    ["OpenResty HTTP (80)"]="127.0.0.1:80"
  )
  for name in "${!targets[@]}"; do
    host="${targets[$name]%:*}"; port="${targets[$name]#*:}"
    if tcp_open "$host" "$port"; then
      log_ok "${name} -> ${host}:${port} OPEN"
    else
      ok=0; log_error "${name} -> ${host}:${port} CLOSED"
    fi
  done
  if [ "$ok" -ne 1 ]; then
    log_warn "1Panel 基础设施存在未监听端口, 请检查 1Panel 应用列表"
  fi
}

check_env_passwords() {
  log_info "校验 .env 中的凭据 ..."
  local missing=()
  [ -z "${MYSQL_PASSWORD:-}" ] && missing+=("MYSQL_PASSWORD")
  [ -z "${REDIS_PASSWORD:-}" ] && missing+=("REDIS_PASSWORD")
  [ -z "${RABBITMQ_PASSWORD:-}" ] && missing+=("RABBITMQ_PASSWORD")
  [ -z "${PANEL_PASSWORD:-}" ] && missing+=("PANEL_PASSWORD")
  if [ "${#missing[@]}" -gt 0 ]; then
    log_error "缺少配置项: ${missing[*]}"
    exit 1
  fi
  # 检查 CR 字符是否还存在 (LF/CRLF 问题)
  if grep -q $'\r' "$ENV_FILE"; then
    log_warn "$ENV_FILE 仍包含 \\r (CRLF), 将自动转换为 LF"
    sed -i 's/\r$//' "$ENV_FILE"
  fi
  log_ok "配置项校验通过"
}

verify_redis_auth() {
  log_info "验证 Redis 凭据: ${REDIS_HOST:-127.0.0.1}:${REDIS_PORT:-6379}"
  local resp; resp=$(printf 'AUTH %s\r\nPING\r\n' "$REDIS_PASSWORD" | timeout 3 bash -c "exec 3<>/dev/tcp/${REDIS_HOST:-127.0.0.1}/${REDIS_PORT:-6379}; cat >&3; head -c 40 <&3" 2>/dev/null || true)
  if echo "$resp" | grep -q "+OK"; then log_ok "Redis AUTH + PING 通过";
  else log_warn "Redis 响应异常: ${resp}"; fi
}

verify_rabbitmq_api() {
  log_info "验证 RabbitMQ API (${RABBITMQ_HOST:-127.0.0.1}:15672)"
  local code; code=$(curl -sS -m 5 -u "${RABBITMQ_USERNAME:-rabbitmq}:${RABBITMQ_PASSWORD:-rabbitmq}" \
    -o /dev/null -w "%{http_code}" "http://${RABBITMQ_HOST:-127.0.0.1}:15672/api/overview" 2>/dev/null || echo 000)
  if [ "$code" = "200" ]; then log_ok "RabbitMQ API 登录成功";
  else log_warn "RabbitMQ API 返回 ${code}"; fi
}

verify_nacos() {
  log_info "验证 Nacos API (${NACOS_ADDR:-127.0.0.1:8848})"
  local code; code=$(http_status "http://${NACOS_ADDR}/nacos/v1/console/health/liveness")
  if [ "$code" = "200" ]; then log_ok "Nacos liveness OK";
  else log_warn "Nacos liveness 返回 ${code}"; fi
}

verify_panel_login() {
  log_info "验证 1Panel 面板登录 (http://127.0.0.1:11336)"
  # 1Panel v2: POST /api/auth/login {username, password} -> returns token
  local code; code=$(curl -sS -m 8 -X POST "http://127.0.0.1:11336/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${PANEL_USER}\",\"password\":\"${PANEL_PASSWORD}\"}" \
    -o /tmp/1panel-login.json -w "%{http_code}" 2>/dev/null || echo 000)
  if [ "$code" = "200" ]; then
    log_ok "1Panel 面板登录成功 (HTTP 200)"
  else
    log_warn "1Panel 面板登录返回 ${code}, 响应: $(cat /tmp/1panel-login.json 2>/dev/null | head -c 300)"
  fi
}

# ---- actions ----
action_check() {
  check_tools; load_env; check_env_passwords; check_1panel_infra
  verify_redis_auth; verify_rabbitmq_api; verify_nacos; verify_panel_login
  log_ok "=== 环境检查通过, 可执行部署 ==="
}

action_up() {
  action_check
  log_info "docker compose -f ${COMPOSE_FILE} up -d"
  cd "${PROJECT_DIR}"
  $COMPOSE_BIN -f "$COMPOSE_FILE" up -d
  log_ok "=== 业务容器启动完成, 等待健康检查 ==="
  sleep 5
  $COMPOSE_BIN -f "$COMPOSE_FILE" ps
}

action_down() {
  log_info "docker compose -f ${COMPOSE_FILE} down (保留数据卷)"
  cd "${PROJECT_DIR}"
  $COMPOSE_BIN -f "$COMPOSE_FILE" down
}

action_logs() {
  cd "${PROJECT_DIR}"
  $COMPOSE_BIN -f "$COMPOSE_FILE" logs --tail=200 -f
}

usage() {
  echo "Usage: $0 {--check|--up|--down|--logs}"
}

# ---- entry ----
case "${1---up}" in
  --check) action_check ;;
  --up)    action_up ;;
  --down)  action_down ;;
  --logs)  action_logs ;;
  -h|--help|help) usage ;;
  *) log_error "未知参数: $1"; usage; exit 1 ;;
esac
