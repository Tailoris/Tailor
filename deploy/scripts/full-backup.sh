#!/bin/bash
# ==============================================================================
# Tailor IS 全量备份脚本
# ==============================================================================
# 功能: MySQL 全库备份 + 项目资产（AI/区块链/静态资源）+ 环境变量 & Nginx 证书
# 用法: sudo ./full-backup.sh
# 备份目录: /data/tailor_backup/YYYYMMDD_HHMMSS/
# ==============================================================================

set -e

# ---------- 颜色输出常量 ----------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# ---------- 日志函数 ----------
log_info()    { echo -e "${GREEN}[INFO]    $(date '+%Y-%m-%d %H:%M:%S') $*${NC}"; }
log_warn()    { echo -e "${YELLOW}[WARN]    $(date '+%Y-%m-%d %H:%M:%S') $*${NC}"; }
log_error()   { echo -e "${RED}[ERROR]   $(date '+%Y-%m-%d %H:%M:%S') $*${NC}" >&2; }

# ---------- 配置变量 ----------
BACKUP_TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_ROOT="/data/tailor_backup"
BACKUP_DIR="${BACKUP_ROOT}/${BACKUP_TIMESTAMP}"

MYSQL_CONTAINER="tailor-is-mysql"
MYSQL_USER="root"

# 项目资产目录（按需备份）
ASSETS_DIRS=(
    "/data/tailor-is-prod/ai-resource"
    "/data/tailor-is-prod/chain-storage"
    "/data/tailor-is-prod/static-cdn"
)

CONFIG_DIRS=(
    "/data/tailor-is-prod/env"
    "/data/tailor-is-prod/gateway/ssl"
    "/data/tailor-is-prod/script"
)

# ---------- 加载 MySQL 密码 ----------
MYSQL_ROOT_PASSWORD=""

# 方案1: 从 deploy/.env 读取
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DEPLOY_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
if [ -f "${DEPLOY_DIR}/.env" ]; then
    MYSQL_ROOT_PASSWORD=$(grep -E "^MYSQL_ROOT_PASSWORD=" "${DEPLOY_DIR}/.env" | head -1 | cut -d'=' -f2-)
    if [ -n "${MYSQL_ROOT_PASSWORD}" ]; then
        log_info "从 ${DEPLOY_DIR}/.env 加载 MySQL 密码"
    fi
fi

# 方案2: 从 deploy/tailor-is-prod/env/.env.base 读取
if [ -z "${MYSQL_ROOT_PASSWORD}" ] && [ -f "${DEPLOY_DIR}/tailor-is-prod/env/.env.base" ]; then
    MYSQL_ROOT_PASSWORD=$(grep -E "^MYSQL_ROOT_PASSWORD=" "${DEPLOY_DIR}/tailor-is-prod/env/.env.base" | head -1 | cut -d'=' -f2-)
    if [ -n "${MYSQL_ROOT_PASSWORD}" ]; then
        log_info "从 ${DEPLOY_DIR}/tailor-is-prod/env/.env.base 加载 MySQL 密码"
    fi
fi

if [ -z "${MYSQL_ROOT_PASSWORD}" ]; then
    log_error "无法获取 MYSQL_ROOT_PASSWORD，请检查 deploy/.env 或 deploy/tailor-is-prod/env/.env.base"
    exit 1
fi

# ---------- 创建备份目录 ----------
log_info "创建备份目录: ${BACKUP_DIR}"
mkdir -p "${BACKUP_DIR}"

# =============================================================================
# 1. MySQL 全量备份
# =============================================================================
log_info "开始 MySQL 全库备份..."

if docker ps --format '{{.Names}}' | grep -q "^${MYSQL_CONTAINER}$"; then
    docker exec "${MYSQL_CONTAINER}" mysqldump \
        -u"${MYSQL_USER}" \
        -p"${MYSQL_ROOT_PASSWORD}" \
        --all-databases \
        --single-transaction \
        --quick \
        --lock-tables=false \
        --default-character-set=utf8mb4 \
        > "${BACKUP_DIR}/mysql_full.sql" 2>/dev/null

    if [ -f "${BACKUP_DIR}/mysql_full.sql" ] && [ -s "${BACKUP_DIR}/mysql_full.sql" ]; then
        MYSQL_SIZE=$(du -h "${BACKUP_DIR}/mysql_full.sql" | cut -f1)
        log_info "MySQL 全库备份完成 (${MYSQL_SIZE})"
    else
        log_error "MySQL 备份文件为空或创建失败"
        exit 1
    fi
else
    log_warn "MySQL 容器 ${MYSQL_CONTAINER} 未运行，跳过数据库备份"
fi

# =============================================================================
# 2. 项目资产备份（AI 资源 / 区块链存储 / 静态资源）
# =============================================================================
log_info "开始项目资产备份..."

ASSETS_BACKED_UP=0
for dir in "${ASSETS_DIRS[@]}"; do
    if [ -d "$dir" ]; then
        dir_basename=$(basename "$dir")
        tar_file="${BACKUP_DIR}/${dir_basename}.tar.gz"
        log_info "  压缩 ${dir} -> ${tar_file}"
        tar -czf "$tar_file" -C "$(dirname "$dir")" "$dir_basename" 2>/dev/null
        ASSETS_BACKED_UP=$((ASSETS_BACKED_UP + 1))
    else
        log_warn "  目录不存在，跳过: ${dir}"
    fi
done

if [ $ASSETS_BACKED_UP -gt 0 ]; then
    log_info "项目资产备份完成，共 ${ASSETS_BACKED_UP} 个目录"
else
    log_warn "未找到任何项目资产目录，已跳过"
fi

# =============================================================================
# 3. 环境变量 & Nginx 证书备份
# =============================================================================
log_info "开始环境变量 & 证书备份..."

CONFIG_BACKED_UP=0
for dir in "${CONFIG_DIRS[@]}"; do
    if [ -d "$dir" ]; then
        dir_basename=$(basename "$dir")
        # 使用路径层级作为 tar 内部路径，避免同名冲突
        # 例如 /data/tailor-is-prod/env -> tar 内路径为 tailor-is-prod/env
        relative_path=$(echo "$dir" | sed 's|^/data/||')
        tar_file="${BACKUP_DIR}/$(echo "$relative_path" | tr '/' '_').tar.gz"
        log_info "  压缩 ${dir} -> ${tar_file}"
        tar -czf "$tar_file" -C "/data" "$relative_path" 2>/dev/null
        CONFIG_BACKED_UP=$((CONFIG_BACKED_UP + 1))
    else
        log_warn "  目录不存在，跳过: ${dir}"
    fi
done

if [ $CONFIG_BACKED_UP -gt 0 ]; then
    log_info "环境变量 & 证书备份完成，共 ${CONFIG_BACKED_UP} 个目录"
else
    log_warn "未找到任何配置/证书目录，已跳过"
fi

# =============================================================================
# 4. 输出备份摘要
# =============================================================================
echo ""
echo "=============================================="
echo -e "${GREEN}  备份完成!${NC}"
echo "=============================================="
echo "  备份路径: ${BACKUP_DIR}"
echo "  备份时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "----------------------------------------------"
echo "  文件清单:"
ls -lh "${BACKUP_DIR}" | tail -n +2 | while read -r line; do
    echo "    $line"
done
echo "----------------------------------------------"
TOTAL_SIZE=$(du -sh "${BACKUP_DIR}" | cut -f1)
echo "  总大小:   ${TOTAL_SIZE}"
echo "=============================================="
echo ""

log_info "全量备份脚本执行完毕"
exit 0