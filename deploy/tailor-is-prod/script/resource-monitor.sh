#!/bin/bash
# ==============================================================================
# CPU负载监控告警脚本
# 配置 crontab: */5 * * * * /data/tailor-is-prod/script/resource-monitor.sh
# 检测整机CPU使用率，超过阈值时输出TOP5容器信息并记录告警日志
# ==============================================================================
LOG_DIR="${LOG_DIR:-/home/tailor/Tailoris/deploy/tailor-is-prod/logs}"
mkdir -p "$LOG_DIR"
LOG_FILE="$LOG_DIR/cpu-alert.log"

# 获取CPU使用率（取整）
CPU_USAGE=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1 | cut -d'.' -f1)

# 如果解析失败，使用 /proc/stat 兜底
if [ -z "$CPU_USAGE" ]; then
    CPU_USAGE=$(grep 'cpu ' /proc/stat | awk '{usage=($2+$4)*100/($2+$4+$5)} END {print int(usage)}')
fi

TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

if [ "$CPU_USAGE" -gt 85 ]; then
    {
        echo "=========================================="
        echo "[${TIMESTAMP}] CRITICAL: CPU usage at ${CPU_USAGE}%"
        echo ""
        echo "Top 5 CPU-consuming containers:"
        docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}" 2>/dev/null | head -6
        echo ""
        echo "Top 5 CPU-consuming processes:"
        ps aux --sort=-%cpu | head -6
        echo "=========================================="
    } >> "$LOG_FILE"
elif [ "$CPU_USAGE" -gt 70 ]; then
    echo "[${TIMESTAMP}] WARNING: CPU usage at ${CPU_USAGE}%" >> "$LOG_FILE"
fi

# 日志轮转：保留最近1000行
if [ -f "$LOG_FILE" ]; then
    tail -n 1000 "$LOG_FILE" > "${LOG_FILE}.tmp" && mv "${LOG_FILE}.tmp" "$LOG_FILE"
fi