#!/bin/bash
# ==============================================================================
# Tailor IS 数据库定期备份脚本
# ==============================================================================
# 版本: v1.0
# 触发方式:
#   1. 手动执行: sudo ./backup.sh
#   2. CRON 定时 (推荐):
#      0 3 * * * /opt/tailor-is/deploy/scripts/backup.sh >> /var/log/tailor-is/backup.log 2>&1
#   3. 1Panel计划任务: 面板 -> 计划任务 -> 添加Shell脚本
# ==============================================================================

set -euo pipefail

PROJECT_DIR="/opt/tailor-is"
BACKUP_ROOT="${PROJECT_DIR}/backup"
DATE=$(date +%Y%m%d-%H%M%S)
BACKUP_DIR="${BACKUP_ROOT}/${DATE}"
RETENTION_DAYS=7  # 保留7天

MYSQL_USER="root"
MYSQL_CONTAINER="tailor-is-mysql"

# ============ 初始化 ============
mkdir -p "${BACKUP_DIR}" "${PROJECT_DIR}/logs"
LOG_FILE="${PROJECT_DIR}/logs/backup-${DATE}.log"
echo "[${DATE}] 开始备份..."

# ============ 1. MySQL 全量备份 ============
echo "[1/3] MySQL 全量备份..."
# 从 .env 读取密码
if [ -f "${PROJECT_DIR}/.env" ]; then
    MYSQL_PASSWORD=$(grep "^MYSQL_ROOT_PASSWORD=" "${PROJECT_DIR}/.env" | cut -d'=' -f2)
fi

if [ -n "${MYSQL_PASSWORD:-}" ]; then
    # 使用容器内 mysqldump
    docker exec "$MYSQL_CONTAINER" mysqldump -u"$MYSQL_USER" -p"${MYSQL_PASSWORD}" \
        --all-databases --single-transaction --quick --lock-tables=false \
        --default-character-set=utf8mb4 > "${BACKUP_DIR}/tailor-is-mysql-${DATE}.sql" 2>/dev/null
    gzip "${BACKUP_DIR}/tailor-is-mysql-${DATE}.sql"
    SIZE=$(du -h "${BACKUP_DIR}/tailor-is-mysql-${DATE}.sql.gz" | awk '{print $1}')
    echo "  ✓ MySQL备份完成: ${SIZE}"
else
    echo "  ⚠  未配置 MYSQL_ROOT_PASSWORD，跳过 MySQL 备份"
fi

# ============ 2. Redis 快照备份 ============
echo "[2/3] Redis AOF备份..."
if docker ps --format '{{.Names}}' | grep -q "^tailor-is-redis$"; then
    docker exec tailor-is-redis redis-cli BGSAVE > /dev/null 2>&1
    sleep 3
    REDIS_DATA=$(docker inspect tailor-is-redis --format='{{range .Mounts}}{{if eq .Destination "/data"}}{{.Source}}{{end}}{{end}}')
    if [ -n "${REDIS_DATA}" ] && [ -f "${REDIS_DATA}/dump.rdb" ]; then
        cp "${REDIS_DATA}/dump.rdb" "${BACKUP_DIR}/redis-dump-${DATE}.rdb"
        echo "  ✓ Redis快照已保存"
    fi
else
    echo "  ⚠  Redis 容器未运行，跳过"
fi

# ============ 3. 配置文件备份 ============
echo "[3/3] 配置文件备份..."
tar czf "${BACKUP_DIR}/configs-${DATE}.tar.gz" \
    -C "${PROJECT_DIR}" .env docker-compose.prod.yml deploy/nginx 2>/dev/null
echo "  ✓ 配置文件已压缩备份"

# ============ 清理旧备份 ============
echo ""
echo "清理 ${RETENTION_DAYS} 天前的旧备份..."
find "${BACKUP_ROOT}" -maxdepth 1 -type d -mtime +${RETENTION_DAYS} -name "20*" | sort | head -5 | while read -r old; do
    echo "  删除: $old"
    rm -rf "$old"
done

# ============ 总结 ============
TOTAL_SIZE=$(du -sh "${BACKUP_DIR}" | awk '{print $1}')
echo ""
echo "=================================================="
echo "  ✅ 备份完成"
echo "  位置: ${BACKUP_DIR}"
echo "  大小: ${TOTAL_SIZE}"
echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "=================================================="
echo "[${DATE}] 完成 - ${TOTAL_SIZE}" >> "${LOG_FILE}"
