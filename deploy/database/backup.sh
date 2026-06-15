#!/bin/bash
# ==============================================================================
# Tailor IS - MySQL 数据库备份脚本
# ==============================================================================
# 用法:
#   ./backup.sh                    # 备份所有 Tailor IS 数据库
#   ./backup.sh tailor_is_order    # 仅备份指定数据库
#   BACKUP_DIR=/opt/backups ./backup.sh  # 自定义备份目录
#
# 定时备份 (crontab):
#   0 2 * * * /opt/tailor-is/deploy/database/backup.sh >> /var/log/tailor-is-backup.log 2>&1
# ==============================================================================

set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/opt/tailor-is/backups}"
DATE=$(date +%Y%m%d_%H%M%S)

# MySQL 连接配置
MYSQL_HOST="${MYSQL_HOST:-localhost}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USERNAME:-root}"
MYSQL_PASS="${MYSQL_PASSWORD:-}"

# 备份保留天数
RETENTION_DAYS="${RETENTION_DAYS:-7}"

# 需要备份的数据库列表
DATABASES="${1:-tailor_is_user tailor_is_merchant tailor_is_product tailor_is_order tailor_is_payment tailor_is_marketing tailor_is_ai tailor_is_copyright tailor_is_community tailor_is_academy tailor_is_supply tailor_is_message}"

# mysqldump 额外参数
DUMP_OPTS="--single-transaction --routines --triggers --events --hex-blob"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1" >&2; }

# 创建备份目录
mkdir -p "$BACKUP_DIR"

# 检查 mysqldump 是否可用
if ! command -v mysqldump &> /dev/null; then
    log_error "mysqldump 未找到，请安装 MySQL 客户端工具"
    exit 1
fi

# 构建 mysqldump 命令
MYSQLDUMP_CMD="mysqldump -h${MYSQL_HOST} -P${MYSQL_PORT} -u${MYSQL_USER}"
if [ -n "$MYSQL_PASS" ]; then
    MYSQLDUMP_CMD="${MYSQLDUMP_CMD} -p${MYSQL_PASS}"
fi

# 备份统计
TOTAL=0
SUCCESS=0
FAILED=0
BACKUP_FILES=()

log_info "开始备份 Tailor IS 数据库..."
log_info "备份目录: ${BACKUP_DIR}"
log_info "保留天数: ${RETENTION_DAYS}"
log_info "数据库列表: ${DATABASES}"
log_info "-------------------------------------------"

for DB in $DATABASES; do
    TOTAL=$((TOTAL + 1))
    BACKUP_FILE="${BACKUP_DIR}/${DB}_${DATE}.sql.gz"
    
    log_info "正在备份 ${DB}..."
    
    if ${MYSQLDUMP_CMD} ${DUMP_OPTS} "$DB" 2>/dev/null | gzip > "$BACKUP_FILE" 2>&1; then
        FILE_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
        log_info "  ✅ ${DB} 备份成功 (${FILE_SIZE})"
        SUCCESS=$((SUCCESS + 1))
        BACKUP_FILES+=("$BACKUP_FILE")
    else
        log_error "  ❌ ${DB} 备份失败"
        rm -f "$BACKUP_FILE"
        FAILED=$((FAILED + 1))
    fi
done

log_info "-------------------------------------------"

# 清理过期备份
log_info "清理 ${RETENTION_DAYS} 天前的备份..."
EXPIRED_COUNT=$(find "$BACKUP_DIR" -name "*.sql.gz" -mtime +${RETENTION_DAYS} 2>/dev/null | wc -l)
if [ "$EXPIRED_COUNT" -gt 0 ]; then
    find "$BACKUP_DIR" -name "*.sql.gz" -mtime +${RETENTION_DAYS} -delete
    log_info "  已删除 ${EXPIRED_COUNT} 个过期备份文件"
else
    log_info "  无过期备份"
fi

# 生成备份清单
CATALOG_FILE="${BACKUP_DIR}/backup_${DATE}.catalog"
{
    echo "Tailor IS Database Backup Catalog"
    echo "Date: ${DATE}"
    echo "Host: ${MYSQL_HOST}:${MYSQL_PORT}"
    echo "User: ${MYSQL_USER}"
    echo "Total databases: ${TOTAL}"
    echo "Successful: ${SUCCESS}"
    echo "Failed: ${FAILED}"
    echo ""
    echo "Files:"
    for f in "${BACKUP_FILES[@]}"; do
        echo "  $(basename "$f") ($(du -h "$f" | cut -f1))"
    done
} > "$CATALOG_FILE"

# 输出总结
log_info "备份完成!"
log_info "  总计: ${TOTAL} | 成功: ${SUCCESS} | 失败: ${FAILED}"
log_info "  备份清单: ${CATALOG_FILE}"

if [ "$FAILED" -gt 0 ]; then
    log_error "有 ${FAILED} 个数据库备份失败，请检查日志"
    exit 1
fi

exit 0
