#!/usr/bin/env bash
# ==============================================================================
# Tailor IS - 自动化回滚脚本
# ==============================================================================
# 用途: 回滚到指定备份或上一个已知健康版本
#
# 使用方式:
#   bash deploy/scripts/rollback.sh [environment] [backup_time]
#   bash deploy/scripts/rollback.sh staging latest
#   bash deploy/scripts/rollback.sh production 20260710_080000
#   bash deploy/scripts/rollback.sh staging --list        # 列出所有备份
# ==============================================================================
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

ENVIRONMENT="${1:-staging}"
BACKUP_TIME="${2:-latest}"
DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="$DEPLOY_DIR/backups"
COMPOSE_FILE="docker-compose.${ENVIRONMENT}.yml"

log()   { echo -e "[$(date '+%H:%M:%S')] $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# ==============================================================================
# 列出所有备份
# ==============================================================================
list_backups() {
    log "=============================================="
    log "  可用备份列表 (${ENVIRONMENT})"
    log "=============================================="
    
    if [[ ! -d "$BACKUP_DIR" ]]; then
        log "  备份目录不存在: $BACKUP_DIR"
        return
    fi
    
    local count=0
    for backup in $(ls -dt "$BACKUP_DIR"/*/ 2>/dev/null || true); do
        local name=$(basename "$backup")
        count=$((count + 1))
        local files=$(ls "$backup" 2>/dev/null | tr '\n' ' ')
        echo -e "  ${GREEN}[$count]${NC} $name  → $files"
    done
    
    if [[ $count -eq 0 ]]; then
        log "  ${YELLOW}未找到备份${NC}"
    fi
    echo ""
}

# ==============================================================================
# 验证环境
# ==============================================================================
validate_environment() {
    if [[ ! "$ENVIRONMENT" =~ ^(development|staging|production)$ ]]; then
        error "无效环境: $ENVIRONMENT。仅支持 development/staging/production"
    fi
    
    if [[ ! -f "$DEPLOY_DIR/$COMPOSE_FILE" ]]; then
        error "Compose 文件不存在: $COMPOSE_FILE"
    fi
}

# ==============================================================================
# 执行回滚
# ==============================================================================
do_rollback() {
    local backup_path="$BACKUP_DIR/$BACKUP_TIME"
    
    if [[ "$BACKUP_TIME" == "latest" ]]; then
        backup_path=$(ls -dt "$BACKUP_DIR"/*/ 2>/dev/null | head -1 || echo "")
        if [[ -z "$backup_path" ]]; then
            error "未找到任何备份"
        fi
        BACKUP_TIME=$(basename "$backup_path")
    fi
    
    if [[ ! -d "$backup_path" ]]; then
        error "备份不存在: $backup_path"
    fi
    
    echo ""
    echo -e "${YELLOW}╔══════════════════════════════════════════════════╗${NC}"
    echo -e "${YELLOW}║  ⚠️  回滚操作确认                                ║${NC}"
    echo -e "${YELLOW}╠══════════════════════════════════════════════════╣${NC}"
    echo -e "${YELLOW}║  环境:     ${ENVIRONMENT}${NC}"
    echo -e "${YELLOW}║  备份:     ${BACKUP_TIME}${NC}"
    echo -e "${YELLOW}║  目标配置: ${COMPOSE_FILE}${NC}"
    echo -e "${YELLOW}╚══════════════════════════════════════════════════╝${NC}"
    echo ""
    
    # 非交互模式自动确认，交互模式询问
    if [[ -t 0 ]]; then
        read -rp "确认回滚? (yes/no): " CONFIRM
        if [[ "$CONFIRM" != "yes" ]]; then
            log "回滚已取消"
            exit 0
        fi
    fi
    
    log "=============================================="
    log "  开始回滚 (${ENVIRONMENT} → ${BACKUP_TIME})"
    log "=============================================="
    
    local current_time=$(date +"%Y%m%d_%H%M%S")
    
    # 步骤 1: 创建回滚前快照
    log "步骤 1/5: 创建回滚前快照..."
    mkdir -p "$BACKUP_DIR/pre_rollback_$current_time"
    if [[ -f "$DEPLOY_DIR/$COMPOSE_FILE" ]]; then
        cp "$DEPLOY_DIR/$COMPOSE_FILE" "$BACKUP_DIR/pre_rollback_$current_time/"
    fi
    log "  ✅ 快照已保存到 pre_rollback_$current_time"
    
    # 步骤 2: 恢复配置
    log "步骤 2/5: 恢复配置..."
    if [[ -f "$backup_path/$COMPOSE_FILE" ]]; then
        cp "$backup_path/$COMPOSE_FILE" "$DEPLOY_DIR/$COMPOSE_FILE"
        log "  ✅ docker-compose 配置已恢复"
    else
        log "  ⚠️  备份中未找到 $COMPOSE_FILE，保留当前配置"
    fi
    
    if [[ -f "$backup_path/.env" ]]; then
        cp "$backup_path/.env" "$DEPLOY_DIR/.env"
        log "  ✅ .env 文件已恢复"
    fi
    
    # 步骤 3: 停止当前服务
    log "步骤 3/5: 停止当前服务..."
    cd "$DEPLOY_DIR"
    docker compose -f "$COMPOSE_FILE" down --timeout 30 2>/dev/null || true
    log "  ✅ 服务已停止"
    
    # 步骤 4: 启动备份版本服务
    log "步骤 4/5: 启动备份版本服务..."
    docker compose -f "$COMPOSE_FILE" up -d --remove-orphans
    log "  ✅ 服务已启动"
    
    # 步骤 5: 健康检查
    log "步骤 5/5: 健康检查..."
    sleep 15  # 等待服务启动
    
    local healthy_count=0
    local unhealthy_count=0
    
    for container in $(docker compose -f "$COMPOSE_FILE" ps -q 2>/dev/null); do
        local name=$(docker inspect -f '{{.Name}}' "$container" 2>/dev/null | sed 's/^\///')
        local status=$(docker inspect -f '{{.State.Health.Status}}' "$container" 2>/dev/null || echo "unknown")
        
        if [[ "$status" == "healthy" ]]; then
            healthy_count=$((healthy_count + 1))
            log "  ✅ $name → $status"
        else
            unhealthy_count=$((unhealthy_count + 1))
            log "  🔴 $name → $status"
        fi
    done
    
    echo ""
    echo -e "${BLUE}┌────────────────────────────────────────────┐${NC}"
    echo -e "${BLUE}│  回滚健康检查结果                           │${NC}"
    echo -e "${BLUE}│  健康:   ${GREEN}${healthy_count}${NC}"
    echo -e "${BLUE}│  异常:   ${RED}${unhealthy_count}${NC}"
    echo -e "${BLUE}└────────────────────────────────────────────┘${NC}"
    echo ""
    
    if [[ $unhealthy_count -gt 0 ]]; then
        echo -e "${RED}🔴 回滚后仍有 ${unhealthy_count} 个服务异常，请检查日志${NC}"
        echo -e "   docker compose -f $DEPLOY_DIR/$COMPOSE_FILE logs --tail=50"
        exit 1
    else
        echo -e "${GREEN}✅ 回滚成功，所有服务健康${NC}"
    fi
}

# ==============================================================================
# 主入口
# ==============================================================================
main() {
    if [[ "$BACKUP_TIME" == "--list" ]]; then
        list_backups
        exit 0
    fi
    
    if [[ "$BACKUP_TIME" == "--help" || "$BACKUP_TIME" == "-h" ]]; then
        sed -n '2,15p' "$0"
        exit 0
    fi
    
    validate_environment
    do_rollback
}

main "$@"