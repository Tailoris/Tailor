#!/bin/bash
# ==============================================================================
# Tailor IS - 分组按需启动脚本
# 用法: bash dev-start.sh infra [core-trade|core-ai-chain|light-forum|light-edu|light-data-supply]
# 示例: bash dev-start.sh infra core-trade
# ==============================================================================

set -e

COMPOSE_DIR="/home/tailor/Tailoris/deploy"
COMPOSE_FILE="$COMPOSE_DIR/docker-compose.services.yml"
DEV_COMPOSE_FILE="$COMPOSE_DIR/docker-compose.development.yml"
LOG_DIR="/home/tailor/Tailoris/deploy/logs"
STARTUP_LOG="$LOG_DIR/startup.log"

mkdir -p "$LOG_DIR"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() {
    echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$STARTUP_LOG"
}

# 校验参数
if [ $# -eq 0 ]; then
    echo -e "${RED}错误: 请指定启动分组${NC}"
    echo "可用分组: infra, core-trade, core-ai-chain, light-forum, light-edu, light-data-supply"
    echo "示例: bash dev-start.sh infra core-trade"
    exit 1
fi

# 校验 infra 是否为第一个参数
FIRST_GROUP=$(echo "$1" | tr '[:upper:]' '[:lower:]')
if [ "$FIRST_GROUP" != "infra" ]; then
    echo -e "${RED}错误: 必须先启动 infra 分组（中间件），请将 infra 作为第一个参数${NC}"
    echo "正确示例: bash dev-start.sh infra core-trade"
    exit 1
fi

# 校验分组数量（最多12个Java容器）
MAX_CONTAINERS=12
PROFILES=""
JAVA_COUNT=0

for arg in "$@"; do
    group=$(echo "$arg" | tr '[:upper:]' '[:lower:]')
    PROFILES="$PROFILES --profile $group"
    
    case "$group" in
        infra)       JAVA_COUNT=$((JAVA_COUNT + 0)) ;;  # 中间件不算Java容器
        core-trade)  JAVA_COUNT=$((JAVA_COUNT + 9)) ;;  # gateway-core, lite-gateway, admin, user, product, order, payment, marketing, merchant
        core-ai-chain) JAVA_COUNT=$((JAVA_COUNT + 4)) ;; # ai, copyright, pattern, design
        light-forum) JAVA_COUNT=$((JAVA_COUNT + 3)) ;;  # community, message, message-im
        light-edu)   JAVA_COUNT=$((JAVA_COUNT + 2)) ;;  # academy, knowledge
        light-data-supply) JAVA_COUNT=$((JAVA_COUNT + 2)) ;; # analytics, supply
        *) echo -e "${YELLOW}警告: 未知分组 '$arg'，已跳过${NC}" ;;
    esac
done

if [ $JAVA_COUNT -gt $MAX_CONTAINERS ]; then
    echo -e "${RED}错误: 单次最多启动 ${MAX_CONTAINERS} 个Java容器，当前将启动 ${JAVA_COUNT} 个${NC}"
    echo -e "${YELLOW}建议: 分批启动，先启动 infra + 核心分组，再启动其他分组${NC}"
    exit 1
fi

# 检查Docker状态
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}错误: Docker 未运行，请先启动 Docker${NC}"
    exit 1
fi

log "${GREEN}===== 开始按分组启动服务 =====${NC}"
log "启动分组: $*"
log "预计Java容器数: $JAVA_COUNT"

# 先启动中间件
log "${YELLOW}[1/2] 启动中间件 (infra)...${NC}"
docker compose -f "$DEV_COMPOSE_FILE" up -d mysql redis rabbitmq nacos 2>&1 | tee -a "$STARTUP_LOG"

# 等待中间件健康
log "${YELLOW}等待中间件就绪...${NC}"
sleep 10

# 检查中间件健康状态
for svc in tailor-is-mysql-dev tailor-is-redis-dev tailor-is-rabbitmq-dev tailor-is-nacos-dev; do
    if docker ps --format '{{.Names}}' | grep -q "$svc"; then
        log "  ${GREEN}✓ $svc 运行中${NC}"
    else
        log "  ${RED}✗ $svc 未运行${NC}"
    fi
done

# 启动业务服务
log "${YELLOW}[2/2] 启动业务服务...${NC}"
docker compose -f "$COMPOSE_FILE" $PROFILES up -d 2>&1 | tee -a "$STARTUP_LOG"

# 输出状态摘要
log ""
log "${GREEN}===== 服务启动完成 =====${NC}"
log "运行中的容器:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | head -20 | tee -a "$STARTUP_LOG"

echo ""
echo -e "${GREEN}提示: 使用 'bash dev-stop-all.sh' 停止所有业务服务${NC}"
echo -e "${GREEN}监控: 使用 'docker stats' 查看资源占用${NC}"