#!/bin/bash
# ==============================================================================
# Tailor IS 一键回滚脚本
# ==============================================================================
# 版本: v1.0
# 说明: 当部署出现问题时，一键回滚到上一个稳定版本
# 使用:
#   sudo ./rollback.sh                        # 交互式回滚
#   sudo ./rollback.sh --backup-dir <路径>   # 指定备份目录回滚
# ==============================================================================

set -euo pipefail

PROJECT_DIR="/opt/tailor-is"
COMPOSE_FILE="${PROJECT_DIR}/docker-compose.prod.yml"

# =============== 查找最近备份 ===============
find_latest_backup() {
    local latest=$(ls -td ${PROJECT_DIR}/backup/*/ 2>/dev/null | head -1)
    if [ -z "$latest" ]; then
        echo "❌ 未找到任何备份目录"
        exit 1
    fi
    echo "$latest"
}

# =============== 主流程 ===============
echo "============================================================"
echo "  Tailor IS 回滚工具 v1.0"
echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "============================================================"

BACKUP_DIR=""
while [ $# -gt 0 ]; do
    case "$1" in
        --backup-dir|-d) shift; BACKUP_DIR="$1" ;;
        --help|-h) echo "用法: ./rollback.sh [--backup-dir <备份目录路径>]"; exit 0 ;;
        *) echo "未知参数: $1"; exit 1 ;;
    esac
    shift
done

if [ -z "$BACKUP_DIR" ]; then
    BACKUP_DIR=$(find_latest_backup)
    echo ""
    echo "🗂  可用备份列表:"
    ls -td ${PROJECT_DIR}/backup/*/ 2>/dev/null | head -5
    echo ""
    echo "将使用最近备份: $BACKUP_DIR"
    echo ""
    read -r -p "确认继续回滚? [y/N] " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "已取消"; exit 0
    fi
fi

if [ ! -d "$BACKUP_DIR" ]; then
    echo "❌ 备份目录不存在: $BACKUP_DIR"; exit 1
fi

echo ""
echo "⚠️  警告: 回滚操作将:"
echo "   1. 停止当前服务"
echo "   2. 恢复 MySQL 数据（如有）"
echo "   3. 恢复配置文件"
echo "   4. 使用旧版镜像重新启动"
echo ""
read -r -p "请输入 'ROLLBACK' 确认: " confirm
if [ "$confirm" != "ROLLBACK" ]; then echo "已取消"; exit 0; fi

set +e
echo ""
echo "[1/4] 停止当前服务..."
docker compose -f "$COMPOSE_FILE" down
echo "[2/4] 恢复 MySQL 数据..."
if [ -f "${BACKUP_DIR}/mysql-backup.tar.gz" ]; then
    tar xzf "${BACKUP_DIR}/mysql-backup.tar.gz" -C "${PROJECT_DIR}"
    echo "✓ MySQL 数据已恢复"
else
    echo "ℹ  无 MySQL 备份文件，跳过"
fi
echo "[3/4] 恢复配置文件..."
if [ -f "${BACKUP_DIR}/.env" ]; then cp "${BACKUP_DIR}/.env" "${PROJECT_DIR}/.env"; echo "✓ .env 已恢复"; fi
if [ -f "${BACKUP_DIR}/docker-compose.prod.yml" ]; then cp "${BACKUP_DIR}/docker-compose.prod.yml" "$COMPOSE_FILE"; echo "✓ docker-compose.prod.yml 已恢复"; fi
echo "[4/4] 启动服务..."
cd "$PROJECT_DIR"
docker compose -f "$COMPOSE_FILE" up -d
set -e

echo ""
echo "============================================================"
echo "  ✅ 回滚完成"
echo "  请等待约30-60秒让服务完全启动"
echo "  查看状态: docker compose -f $COMPOSE_FILE ps"
echo "  查看日志: docker compose -f $COMPOSE_FILE logs -f"
echo "============================================================"
