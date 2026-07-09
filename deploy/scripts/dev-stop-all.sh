#!/bin/bash
# ==============================================================================
# Tailor IS - 一键关停全部业务服务（保留中间件）
# 用法: bash dev-stop-all.sh
# ==============================================================================

set -e

COMPOSE_DIR="/home/tailor/Tailoris/deploy"
LOG_DIR="/home/tailor/Tailoris/deploy/logs"
STOP_LOG="$LOG_DIR/stop.log"

mkdir -p "$LOG_DIR"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() {
    echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$STOP_LOG"
}

log "${YELLOW}===== 开始停止业务服务 =====${NC}"

# 中间件容器名称（保留不停止）
MIDDLEWARE_CONTAINERS="tailor-is-mysql-dev tailor-is-redis-dev tailor-is-rabbitmq-dev tailor-is-nacos-dev"

# 获取所有运行中的Java业务容器
BUSINESS_CONTAINERS=$(docker ps --format '{{.Names}}' | grep -E 'tailor-is-' | grep -v -E "$(echo $MIDDLEWARE_CONTAINERS | tr ' ' '|')" || true)

if [ -z "$BUSINESS_CONTAINERS" ]; then
    log "${GREEN}没有运行中的业务容器${NC}"
else
    STOPPED_COUNT=0
    for container in $BUSINESS_CONTAINERS; do
        log "停止容器: $container"
        docker stop "$container" 2>&1 | tee -a "$STOP_LOG" || true
        STOPPED_COUNT=$((STOPPED_COUNT + 1))
    done
    log "${GREEN}已停止 $STOPPED_COUNT 个业务容器${NC}"
fi

# 显示中间件状态
log ""
log "${GREEN}===== 中间件状态 =====${NC}"
for mw in $MIDDLEWARE_CONTAINERS; do
    if docker ps --format '{{.Names}}' | grep -q "$mw"; then
        log "  ${GREEN}✓ $mw 运行中（已保留）${NC}"
    else
        log "  ${YELLOW}○ $mw 未运行${NC}"
    fi
done

log ""
log "${GREEN}===== 停止完成 =====${NC}"
echo -e "${GREEN}提示: 使用 'bash dev-start.sh infra <分组>' 重新启动${NC}"