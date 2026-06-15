#!/bin/bash
# 自动备份调度脚本
# 用法: schedule-backup.sh [daily|weekly|monthly]

BACKUP_TYPE="${1:-daily}"
BACKUP_ROOT="/home/tailor/Tailoris/backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="$BACKUP_ROOT/$BACKUP_TYPE/$TIMESTAMP"

echo "[$(date)] 开始 $BACKUP_TYPE 备份..."
mkdir -p "$BACKUP_DIR"

# 数据库备份
mysqldump --protocol=tcp -h 127.0.0.1 -u root \
  --single-transaction --quick \
  tailor_is > "$BACKUP_DIR/database_$TIMESTAMP.sql" 2>/dev/null

# 配置备份
cp /home/tailor/Tailoris/deploy/.env.production "$BACKUP_DIR/" 2>/dev/null
cp /home/tailor/Tailoris/docker-compose.prod.yml "$BACKUP_DIR/" 2>/dev/null

# 完整性校验
sha256sum "$BACKUP_DIR"/* > "$BACKUP_DIR/backup_$TIMESTAMP.sha256" 2>/dev/null

# 清理旧备份 (按类型保留)
case $BACKUP_TYPE in
    daily)
        find "$BACKUP_ROOT/daily" -type d -mtime +7 -exec rm -rf {} + 2>/dev/null
        ;;
    weekly)
        find "$BACKUP_ROOT/weekly" -type d -mtime +30 -exec rm -rf {} + 2>/dev/null
        ;;
    monthly)
        find "$BACKUP_ROOT/monthly" -type d -mtime +365 -exec rm -rf {} + 2>/dev/null
        ;;
esac

echo "[$(date)] $BACKUP_TYPE 备份完成: $BACKUP_DIR"
