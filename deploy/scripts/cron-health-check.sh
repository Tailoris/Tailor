#!/usr/bin/env bash
# ==============================================================================
# Tailor IS - 定期巡检脚本（crontab 集成）
# ==============================================================================
# 用途: 定时执行健康检查，异常时自动告警，生成巡检日志
#
# crontab 配置示例（每 5 分钟执行一次）:
#   */5 * * * * /data/tailor-is-prod/script/cron-health-check.sh >> /var/log/tailor-is/health-check.log 2>&1
#
# 使用方式:
#   bash deploy/scripts/cron-health-check.sh [environment]
# ==============================================================================
set -euo pipefail

ENVIRONMENT="${1:-production}"
REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DEPLOY_DIR="$REPO_DIR/deploy"
LOG_DIR="/var/log/tailor-is"
ALERT_LOG="$LOG_DIR/alerts.log"
HEALTH_LOG="$LOG_DIR/health-check.log"
HOSTNAME=$(hostname)
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

mkdir -p "$LOG_DIR"

# ========== 告警阈值 ==========
CPU_THRESHOLD=80
MEM_THRESHOLD=85
DISK_THRESHOLD=90
CONTAINER_RESTART_THRESHOLD=3

# ========== 告警函数 ==========
send_alert() {
    local level="$1"
    local message="$2"
    echo "[$TIMESTAMP] [$level] $message" >> "$ALERT_LOG"
    
    # 可选: 发送钉钉/企业微信/邮件告警
    # curl -X POST "$WEBHOOK_URL" -H "Content-Type: application/json" \
    #     -d "{\"msgtype\":\"text\",\"text\":{\"content\":\"[$level] $message\"}}"
}

# ==============================================================================
# 1. 系统资源检查
# ==============================================================================
check_system_resources() {
    # CPU
    local cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1 | cut -d'.' -f1)
    if [[ "$cpu_usage" -gt "$CPU_THRESHOLD" ]]; then
        send_alert "CRITICAL" "CPU 使用率过高: ${cpu_usage}% (阈值: ${CPU_THRESHOLD}%)"
    fi
    
    # 内存
    local mem_usage=$(free | grep Mem | awk '{printf "%.0f", $3/$2 * 100}')
    if [[ "$mem_usage" -gt "$MEM_THRESHOLD" ]]; then
        send_alert "CRITICAL" "内存使用率过高: ${mem_usage}% (阈值: ${MEM_THRESHOLD}%)"
    fi
    
    # 磁盘
    local disk_usage=$(df -h / | awk 'NR==2 {print $5}' | sed 's/%//')
    if [[ "$disk_usage" -gt "$DISK_THRESHOLD" ]]; then
        send_alert "CRITICAL" "磁盘使用率过高: ${disk_usage}% (阈值: ${DISK_THRESHOLD}%)"
    fi
    
    echo "[$TIMESTAMP] SYSTEM: CPU=${cpu_usage}% MEM=${mem_usage}% DISK=${disk_usage}%" >> "$HEALTH_LOG"
}

# ==============================================================================
# 2. Docker 容器健康检查
# ==============================================================================
check_containers() {
    local unhealthy=0
    local containers=$(docker ps --format '{{.Names}}' 2>/dev/null | grep 'tailor-is-' || true)
    
    for container_name in $containers; do
        local status=$(docker inspect -f '{{.State.Health.Status}}' "$container_name" 2>/dev/null || echo "unknown")
        local restarts=$(docker inspect -f '{{.RestartCount}}' "$container_name" 2>/dev/null || echo "0")
        
        # 检查健康状态
        if [[ "$status" != "healthy" && "$status" != "starting" ]]; then
            unhealthy=$((unhealthy + 1))
            send_alert "WARNING" "容器 $container_name 状态异常: $status"
        fi
        
        # 检查重启次数
        if [[ "$restarts" -gt "$CONTAINER_RESTART_THRESHOLD" ]]; then
            send_alert "WARNING" "容器 $container_name 重启次数过多: $restarts"
        fi
    done
    
    # 检查是否有容器停止
    local stopped=$(docker ps -a --format '{{.Names}}' --filter "status=exited" 2>/dev/null | grep 'tailor-is-' | wc -l)
    if [[ "$stopped" -gt 0 ]]; then
        send_alert "CRITICAL" "$stopped 个容器已停止运行"
    fi
    
    echo "[$TIMESTAMP] CONTAINERS: running=$(echo "$containers" | wc -l) unhealthy=$unhealthy stopped=$stopped" >> "$HEALTH_LOG"
}

# ==============================================================================
# 3. 网关可达性检查
# ==============================================================================
check_gateway() {
    local gateway_url="http://localhost:9000/actuator/health"
    local http_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$gateway_url" 2>/dev/null || echo "000")
    
    if [[ "$http_code" != "200" ]]; then
        send_alert "CRITICAL" "网关不可达: HTTP $http_code"
    fi
    
    echo "[$TIMESTAMP] GATEWAY: HTTP $http_code" >> "$HEALTH_LOG"
}

# ==============================================================================
# 4. 数据库连接检查
# ==============================================================================
check_database() {
    # MySQL
    if mysqladmin ping -h 127.0.0.1 --silent 2>/dev/null; then
        echo "[$TIMESTAMP] DB: MySQL OK" >> "$HEALTH_LOG"
    else
        send_alert "CRITICAL" "MySQL 连接失败"
    fi
    
    # Redis
    if redis-cli -h 127.0.0.1 ping 2>/dev/null | grep -q PONG; then
        echo "[$TIMESTAMP] DB: Redis OK" >> "$HEALTH_LOG"
    else
        send_alert "CRITICAL" "Redis 连接失败"
    fi
}

# ==============================================================================
# 5. 日志文件大小检查
# ==============================================================================
check_log_size() {
    local max_log_size_mb=500
    local log_dirs=("/var/log/tailor-is" "/var/log/docker" "/var/log/nginx")
    
    for log_dir in "${log_dirs[@]}"; do
        if [[ -d "$log_dir" ]]; then
            local size_mb=$(du -sm "$log_dir" 2>/dev/null | cut -f1)
            if [[ "$size_mb" -gt "$max_log_size_mb" ]]; then
                send_alert "WARNING" "日志目录 $log_dir 过大: ${size_mb}MB (阈值: ${max_log_size_mb}MB)"
            fi
        fi
    done
}

# ==============================================================================
# 6. 证书过期检查
# ==============================================================================
check_cert_expiry() {
    local cert_dirs=("/etc/nginx/ssl" "/etc/letsencrypt/live")
    
    for cert_dir in "${cert_dirs[@]}"; do
        if [[ -d "$cert_dir" ]]; then
            for cert in $(find "$cert_dir" -name "*.pem" -o -name "fullchain.pem" 2>/dev/null); do
                if command -v openssl &>/dev/null; then
                    local expiry=$(openssl x509 -enddate -noout -in "$cert" 2>/dev/null | cut -d= -f2)
                    local expiry_epoch=$(date -d "$expiry" +%s 2>/dev/null || echo 0)
                    local now_epoch=$(date +%s)
                    local days_left=$(( (expiry_epoch - now_epoch) / 86400 ))
                    
                    if [[ "$days_left" -lt 30 && "$days_left" -gt 0 ]]; then
                        send_alert "WARNING" "SSL 证书 $cert 将在 $days_left 天后过期"
                    elif [[ "$days_left" -le 0 ]]; then
                        send_alert "CRITICAL" "SSL 证书 $cert 已过期"
                    fi
                fi
            done
        fi
    done
}

# ==============================================================================
# 主流程
# ==============================================================================
main() {
    local start_time=$(date +%s)
    
    check_system_resources
    check_containers
    check_gateway
    check_database
    check_log_size
    check_cert_expiry
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    echo "[$TIMESTAMP] CHECK_COMPLETE: duration=${duration}s" >> "$HEALTH_LOG"
}

main