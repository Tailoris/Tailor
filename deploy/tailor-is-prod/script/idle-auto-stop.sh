#!/bin/bash
# ==============================================================================
# 闲置服务自动停机脚本
# 配置 crontab: */30 * * * * /data/tailor-is-prod/script/idle-auto-stop.sh
# 检测30分钟内无活跃连接的Java容器并自动停止（保留核心服务）
# ==============================================================================
LOG_DIR="${LOG_DIR:-/home/tailor/Tailoris/deploy/tailor-is-prod/logs}"
mkdir -p "$LOG_DIR"
LOG_FILE="$LOG_DIR/idle-clean.log"

# 核心服务（永不自动停止）
CORE_SERVICES="tailor-is-core-gateway|tailor-is-user|tailor-is-product|tailor-is-order|tailor-is-payment"

TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
STOPPED_COUNT=0

# 获取所有运行中的Java容器（排除核心服务）
RUNNING_CONTAINERS=$(docker ps --format '{{.Names}}' 2>/dev/null | grep "tailor-is" | grep -vE "$CORE_SERVICES")

if [ -z "$RUNNING_CONTAINERS" ]; then
    echo "[${TIMESTAMP}] No non-core containers running" >> "$LOG_FILE"
    exit 0
fi

for container in $RUNNING_CONTAINERS; do
    # 获取容器内 SERVER_PORT 环境变量
    PORT=$(docker inspect "$container" --format '{{range .Config.Env}}{{.}}{{end}}' 2>/dev/null | tr ' ' '\n' | grep "^SERVER_PORT=" | cut -d'=' -f2)
    
    if [ -z "$PORT" ]; then
        continue
    fi
    
    # 检查是否有活跃的TCP连接
    CONNECTIONS=$(ss -tn state established "( sport = :$PORT )" 2>/dev/null | wc -l)
    
    # 小于等于1（仅ss表头行）表示无活跃连接
    if [ "$CONNECTIONS" -le 1 ]; then
        echo "[${TIMESTAMP}] Stopping idle container: $container (port $PORT, no active connections)" >> "$LOG_FILE"
        docker stop --time=30 "$container" 2>/dev/null
        if [ $? -eq 0 ]; then
            ((STOPPED_COUNT++))
        fi
    fi
done

if [ "$STOPPED_COUNT" -gt 0 ]; then
    echo "[${TIMESTAMP}] Stopped ${STOPPED_COUNT} idle container(s)" >> "$LOG_FILE"
fi

# 日志轮转：保留最近500行
if [ -f "$LOG_FILE" ]; then
    tail -n 500 "$LOG_FILE" > "${LOG_FILE}.tmp" && mv "${LOG_FILE}.tmp" "$LOG_FILE"
fi