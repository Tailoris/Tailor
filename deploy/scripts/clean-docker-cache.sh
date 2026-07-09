#!/bin/bash
# ==============================================================================
# Tailor IS - 每日Docker缓存清理脚本
# 清理停止容器、无用镜像、临时文件、过期日志
# 用法: 由 crontab 每日凌晨2点调用
# ==============================================================================

set -e

LOG_DIR="/home/tailor/Tailoris/deploy/logs"
CLEAN_LOG="$LOG_DIR/clean.log"
RETENTION_DAYS=7

mkdir -p "$LOG_DIR"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$CLEAN_LOG"
}

log "===== 开始Docker资源清理 ====="

# 1. 清理已停止的容器
STOPPED_COUNT=$(docker ps -a -q -f status=exited | wc -l)
if [ "$STOPPED_COUNT" -gt 0 ]; then
    log "清理已停止容器: $STOPPED_COUNT 个"
    docker container prune -f 2>&1 | tee -a "$CLEAN_LOG"
fi

# 2. 清理无用镜像（dangling）
DANGLING_COUNT=$(docker images -f "dangling=true" -q | wc -l)
if [ "$DANGLING_COUNT" -gt 0 ]; then
    log "清理悬空镜像: $DANGLING_COUNT 个"
    docker image prune -f 2>&1 | tee -a "$CLEAN_LOG"
fi

# 3. 清理构建缓存
log "清理Docker构建缓存..."
docker builder prune -f 2>&1 | tee -a "$CLEAN_LOG"

# 4. 清理AI临时版型缓存
AI_TEMP_DIR="/data/tailor-is-prod/ai-resource/temp"
if [ -d "$AI_TEMP_DIR" ]; then
    log "清理AI临时文件 (保留${RETENTION_DAYS}天)..."
    find "$AI_TEMP_DIR" -type f -mtime +$RETENTION_DAYS -delete 2>/dev/null || true
fi

# 5. 清理过期日志
LOG_PATHS=(
    "/data/tailor-is-prod/core-services/logs"
    "/data/tailor-is-prod/gateway/access-logs"
)
for log_path in "${LOG_PATHS[@]}"; do
    if [ -d "$log_path" ]; then
        log "清理过期日志: $log_path (保留${RETENTION_DAYS}天)"
        find "$log_path" -name "*.log" -mtime +$RETENTION_DAYS -delete 2>/dev/null || true
        find "$log_path" -name "*.log.*" -mtime +$RETENTION_DAYS -delete 2>/dev/null || true
    fi
done

# 6. 清理系统临时文件
log "清理系统临时文件..."
find /tmp -name "tailor-is-*" -mtime +1 -delete 2>/dev/null || true

log "===== 清理完成 ====="