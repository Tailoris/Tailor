#!/bin/bash
# ==============================================================================
# 按Profile分组启动微服务脚本
# 用法: ./dev-start.sh <profile>
# 示例: ./dev-start.sh core-trade        # 启动核心交易链路
#       ./dev-start.sh core-ai-chain     # 启动AI+区块链
#       ./dev-start.sh all               # 启动全部服务
# ==============================================================================

set -e

MAX_CONTAINERS=8
PROFILE="${1:-core-trade}"
COMPOSE_FILE="deploy/docker-compose.services-v2.yml"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

cd "$PROJECT_DIR"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# infra 依赖检查
check_infra() {
    log_info "Checking infrastructure dependencies..."
    local missing=0
    for svc in mysql redis rabbitmq nacos; do
        if ! docker ps --format '{{.Names}}' 2>/dev/null | grep -q "tailor-is-${svc}"; then
            log_error "${svc} not running"
            missing=1
        fi
    done
    if [ "$missing" -eq 1 ]; then
        log_error "Infrastructure not ready. Start it first:"
        echo "  docker compose -f deploy/docker-compose.development.yml --profile infra up -d"
        exit 1
    fi
    log_info "Infrastructure OK"
}

# 启动服务
start_services() {
    local profile="$1"
    log_info "Starting services with profile: ${profile}"

    if [ "$profile" = "all" ]; then
        docker compose -f "$COMPOSE_FILE" \
            --profile core-trade \
            --profile core-ai-chain \
            --profile light-forum \
            --profile light-edu \
            --profile light-data-supply \
            up -d --build
    else
        docker compose -f "$COMPOSE_FILE" --profile "$profile" up -d --build
    fi
}

# 检查容器数量
check_container_count() {
    local count
    count=$(docker compose -f "$COMPOSE_FILE" ps -q 2>/dev/null | wc -l)
    if [ "$count" -gt "$MAX_CONTAINERS" ]; then
        log_warn "${count} containers running, exceeds recommended max of ${MAX_CONTAINERS}"
    fi
}

# 主流程
check_infra
start_services "$PROFILE"
check_container_count

echo ""
log_info "=== Service Status ==="
docker compose -f "$COMPOSE_FILE" ps

echo ""
log_info "=== CPU Usage ==="
top -bn1 | head -5