#!/bin/bash
# ==============================================================================
# Tailor IS - 整机CPU负载监控告警脚本
# 逻辑: 每小时检测整机CPU，超过85%输出TOP5占用容器日志
# 用法: 由 crontab 每小时自动调用
# ==============================================================================

set -e

LOG_DIR="/home/tailor/Tailoris/deploy/logs"
ALERT_LOG="$LOG_DIR/cpu-alert.log"
CPU_THRESHOLD=85

mkdir -p "$LOG_DIR"

# 获取整机CPU使用率（1分钟平均负载换算或直接取top）
# 使用 top 获取CPU idle，计算使用率
CPU_IDLE=$(top -bn1 | grep "Cpu(s)" | awk '{print $8}' | cut -d'%' -f1 | cut -d',' -f1)
CPU_USAGE=$(echo "100 - $CPU_IDLE" | bc 2>/dev/null || echo "0")

TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

# 比较CPU使用率
CPU_USAGE_INT=$(echo "$CPU_USAGE" | cut -d'.' -f1)

if [ "$CPU_USAGE_INT" -gt "$CPU_THRESHOLD" ] 2>/dev/null; then
    {
        echo "=========================================="
        echo "[$TIMESTAMP] ⚠ CPU 告警: ${CPU_USAGE}% (阈值: ${CPU_THRESHOLD}%)"
        echo "=========================================="
        echo ""
        echo "TOP5 CPU占用容器:"
        docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemPerc}}\t{{.MemUsage}}" 2>/dev/null | head -7
        echo ""
        echo "整机负载:"
        uptime
        echo ""
        echo "内存使用:"
        free -h
        echo "=========================================="
        echo ""
    } >> "$ALERT_LOG"
    
    # 同时输出到标准输出（crontab会发邮件）
    echo "[$TIMESTAMP] CPU ALERT: ${CPU_USAGE}% (threshold: ${CPU_THRESHOLD}%)"
else
    # 正常情况也记录（可选，方便追踪趋势）
    echo "[$TIMESTAMP] CPU OK: ${CPU_USAGE}%" >> "$ALERT_LOG"
fi