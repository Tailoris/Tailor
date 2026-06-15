#!/bin/bash
# ==============================================================================
# Tailor IS - MySQL 数据库恢复脚本
# ==============================================================================
# 用法:
#   ./restore.sh /opt/tailor-is/backups/tailor_is_order_20260611_020000.sql.gz
#   ./restore.sh tailor_is_order          # 自动查找最新备份
#   ./restore.sh --all                    # 恢复所有数据库 (从最新备份清单)
#
# 警告: 恢复操作会覆盖现有数据! 请确保已做好当前数据的备份!
# ==============================================================================

set -euo pipefail

# MySQL 连接配置
MYSQL_HOST="${MYSQL_HOST:-localhost}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USERNAME:-root}"
MYSQL_PASS="${MYSQL_PASSWORD:-}"

BACKUP_DIR="${BACKUP_DIR:-/opt/tailor-is/backups}"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1" >&2; }

# 构建 mysql 命令
MYSQL_CMD="mysql -h${MYSQL_HOST} -P${MYSQL_PORT} -u${MYSQL_USER}"
if [ -n "$MYSQL_PASS" ]; then
    MYSQL_CMD="${MYSQL_CMD} -p${MYSQL_PASS}"
fi

# 检查工具是否可用
if ! command -v mysql &> /dev/null; then
    log_error "mysql 客户端未找到，请安装 MySQL 客户端工具"
    exit 1
fi

# 查找最新备份文件
find_latest_backup() {
    local db_name="$1"
    local latest
    latest=$(ls -t "${BACKUP_DIR}/${db_name}_"*.sql.gz 2>/dev/null | head -1)
    if [ -z "$latest" ]; then
        log_error "未找到 ${db_name} 的备份文件"
        exit 1
    fi
    echo "$latest"
}

# 恢复单个数据库
restore_database() {
    local backup_file="$1"
    local db_name
    
    # 从文件名提取数据库名
    db_name=$(basename "$backup_file" | sed 's/_.*//')
    
    log_info "准备恢复数据库: ${db_name}"
    log_info "备份文件: ${backup_file}"
    log_info "文件大小: $(du -h "$backup_file" | cut -f1)"
    
    # 确认恢复
    if [ -t 0 ]; then
        log_warn "⚠️  此操作将覆盖数据库 ${db_name} 的现有数据!"
        read -rp "是否继续? (y/N): " confirm
        if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
            log_info "取消恢复"
            return 1
        fi
    fi
    
    # 先备份当前数据
    local pre_restore_backup="${BACKUP_DIR}/${db_name}_pre_restore_$(date +%Y%m%d_%H%M%S).sql.gz"
    log_info "正在备份当前数据到: ${pre_restore_backup}"
    if mysqldump -h"${MYSQL_HOST}" -P"${MYSQL_PORT}" -u"${MYSQL_USER}" \
        $([ -n "$MYSQL_PASS" ] && echo "-p${MYSQL_PASS}") \
        --single-transaction --routines --triggers --events \
        "$db_name" 2>/dev/null | gzip > "$pre_restore_backup"; then
        log_info "  当前数据备份成功"
    else
        log_warn "  当前数据备份失败，但继续恢复操作"
    fi
    
    # 执行恢复
    log_info "正在恢复 ${db_name}..."
    
    if [[ "$backup_file" == *.gz ]]; then
        gunzip -c "$backup_file" | ${MYSQL_CMD} "$db_name"
    else
        ${MYSQL_CMD} "$db_name" < "$backup_file"
    fi
    
    if [ $? -eq 0 ]; then
        log_info "  ✅ ${db_name} 恢复成功"
    else
        log_error "  ❌ ${db_name} 恢复失败"
        log_error "  恢复前备份: ${pre_restore_backup}"
        return 1
    fi
    
    return 0
}

# 主逻辑
main() {
    local input="${1:-}"
    
    if [ -z "$input" ]; then
        echo "用法: $0 <备份文件路径 | 数据库名 | --all>"
        echo ""
        echo "示例:"
        echo "  $0 /opt/tailor-is/backups/tailor_is_order_20260611_020000.sql.gz  # 从文件恢复"
        echo "  $0 tailor_is_order                    # 自动查找最新备份"
        echo "  $0 --all                              # 恢复所有数据库"
        echo ""
        echo "最近备份文件:"
        ls -lt "${BACKUP_DIR}"/*.sql.gz 2>/dev/null | head -5 || echo "  (无)"
        exit 1
    fi
    
    # --all: 从最新备份清单恢复所有数据库
    if [ "$input" = "--all" ]; then
        log_info "开始恢复所有数据库..."
        local latest_catalog
        latest_catalog=$(ls -t "${BACKUP_DIR}"/backup_*.catalog 2>/dev/null | head -1)
        
        if [ -z "$latest_catalog" ]; then
            log_error "未找到备份清单文件"
            exit 1
        fi
        
        log_info "使用备份清单: ${latest_catalog}"
        
        # 从清单中提取数据库名并恢复
        local count=0
        for backup_file in "${BACKUP_DIR}"/*.sql.gz; do
            if [ -f "$backup_file" ]; then
                restore_database "$backup_file" && count=$((count + 1))
            fi
        done
        
        log_info "全部恢复完成，共恢复 ${count} 个数据库"
        return
    fi
    
    # 从文件路径恢复
    if [ -f "$input" ]; then
        restore_database "$input"
        return
    fi
    
    # 从数据库名恢复 (自动查找最新备份)
    if [ -d "$BACKUP_DIR" ]; then
        local backup_file
        backup_file=$(find_latest_backup "$input")
        restore_database "$backup_file"
    else
        log_error "备份目录不存在: ${BACKUP_DIR}"
        exit 1
    fi
}

main "$@"
