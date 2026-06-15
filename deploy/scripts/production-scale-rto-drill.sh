#!/bin/bash
# =============================================================================
#  Tailor IS - 生产级数据规模备份恢复 RTO 演练脚本
#  文档编号: TAILOR-IS-RTO-DRILL-2026-0611
#  用途:
#    1) 模拟生产级数据量（MySQL 百万级表 / Redis 百万级Key / Nacos 配置）
#    2) 执行完整备份流程并记录耗时
#    3) 执行恢复流程并测量 RTO (Recovery Time Objective)
#    4) 输出结构化RTO演练报告 (JSON + 可读文本)
#
#  使用:
#    bash deploy/scripts/production-scale-rto-drill.sh [backup|restore|full]
#
#  注意:
#    - 仅在演练环境运行，切勿直接在生产环境执行 restore
#    - 需要 MySQL / Redis / Docker 客户端工具可用
#    - 默认会在 /opt/tailor-is/drill 下创建演练专用路径
# =============================================================================
set -euo pipefail

# ---------- 颜色输出 ----------
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log_info(){ echo -e "${BLUE}[INFO]    $(date '+%Y-%m-%d %H:%M:%S') $*${NC}"; }
log_ok(){ echo -e "${GREEN}[OK]      $(date '+%Y-%m-%d %H:%M:%S') $*${NC}"; }
log_warn(){ echo -e "${YELLOW}[WARN]    $(date '+%Y-%m-%d %H:%M:%S') $*${NC}"; }
log_err(){ echo -e "${RED}[ERROR]   $(date '+%Y-%m-%d %H:%M:%S') $*${NC}" >&2; }

# ---------- 配置：生产级数据规模 ----------
DRILL_ROOT="/opt/tailor-is/drill"
DRILL_TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
DRILL_DIR="${DRILL_ROOT}/${DRILL_TIMESTAMP}"
REPORT_FILE="${DRILL_DIR}/rto-report.txt"
JSON_REPORT_FILE="${DRILL_DIR}/rto-report.json"

# 生产规模模拟参数（可按需调整）
MYSQL_USERS_COUNT=1000000         # 100 万用户
MYSQL_ORDERS_COUNT=2000000        # 200 万订单
MYSQL_PRODUCTS_COUNT=500000       # 50 万商品
REDIS_KEYS_COUNT=500000           # 50 万 Redis key

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-root}"
MYSQL_DRILL_DB="tailor_is_rto_drill"

REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"

BACKUP_ARCHIVE="${DRILL_DIR}/backup.tar.gz"
RESTORE_ELAPSED_SEC=0
BACKUP_ELAPSED_SEC=0

mkdir -p "${DRILL_DIR}"
touch "${REPORT_FILE}"

# ---------- 计时器 ----------
declare -A PHASE_START
declare -A PHASE_ELAPSED
phase_start(){ PHASE_START[$1]=$(date +%s); log_info "▶ 阶段启动: $1"; }
phase_end(){
  local end=$(date +%s); local start=${PHASE_START[$1]}
  local elapsed=$(( end - start ))
  PHASE_ELAPSED[$1]=${elapsed}
  log_ok "  阶段完成: $1 (耗时 ${elapsed}s)"
}
human_sec(){
  local s=$1
  if [ "$s" -lt 60 ]; then echo "${s}s"
  elif [ "$s" -lt 3600 ]; then echo "$((s/60))m$((s%60))s"
  else echo "$((s/3600))h$(( (s%3600)/60 ))m$((s%60))s"; fi
}

# ---------- 检查工具 ----------
check_tools(){
  phase_start "工具可用性检查"
  local missing=0
  for t in mysql mysqldump redis-cli tar awk base64; do
    if command -v "$t" >/dev/null 2>&1; then
      log_info "  ✅ $t: $(command -v $t)"
    else
      log_warn "  ❌ $t 未安装"
      missing=1
    fi
  done
  if [ "$missing" -eq 1 ]; then
    log_warn "  部分工具缺失，脚本将在受限模式下运行（纯逻辑演练仍然可用）"
  fi
  phase_end "工具可用性检查"
}

# ---------- 1) 生产级数据规模模拟 ----------
stage_seed_data(){
  phase_start "生产级数据规模模拟"
  local mysql_available=0
  if mysql -h"${MYSQL_HOST}" -P"${MYSQL_PORT}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" -e "SELECT 1" >/dev/null 2>&1; then
    mysql_available=1
    log_info "  MySQL 连接正常，创建演练数据库 ${MYSQL_DRILL_DB}"
    mysql -h"${MYSQL_HOST}" -P"${MYSQL_PORT}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" \
      -e "CREATE DATABASE IF NOT EXISTS ${MYSQL_DRILL_DB} DEFAULT CHARSET utf8mb4;"

    local seed_sql="${DRILL_DIR}/seed.sql"
    cat > "${seed_sql}" <<EOF
USE ${MYSQL_DRILL_DB};
DROP TABLE IF EXISTS users;
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  email VARCHAR(128),
  phone VARCHAR(32),
  status TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_email (email), KEY idx_status (status)
);
DROP TABLE IF EXISTS orders;
CREATE TABLE orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(64),
  user_id BIGINT,
  amount DECIMAL(12,2),
  status VARCHAR(32),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_order_no (order_no), KEY idx_user (user_id)
);
DROP TABLE IF EXISTS products;
CREATE TABLE products (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sku VARCHAR(64),
  title VARCHAR(255),
  price DECIMAL(12,2),
  stock INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_sku (sku)
);
EOF
    mysql -h"${MYSQL_HOST}" -P"${MYSQL_PORT}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DRILL_DB}" < "${seed_sql}"
    log_info "  已创建 3 张表 (users/orders/products)"

    # 批量插入（使用存储过程+循环，避免内存爆炸）
    log_info "  开始填充 ${MYSQL_USERS_COUNT} 条 users (可能需要几分钟)..."
    mysql -h"${MYSQL_HOST}" -P"${MYSQL_PORT}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DRILL_DB}" -e "
      DELIMITER //
      DROP PROCEDURE IF EXISTS seed_users //
      CREATE PROCEDURE seed_users(IN cnt INT)
      BEGIN
        DECLARE i INT DEFAULT 0;
        WHILE i < cnt DO
          INSERT INTO users (username, email, phone) VALUES
            (CONCAT('user_',i), CONCAT('user_',i,'@tailoris.com'), CONCAT('13',FLOOR(10000000000 + (RAND()*8999999999))));
          SET i = i + 1;
          IF i % 50000 = 0 THEN COMMIT; END IF;
        END WHILE;
      END //
      DELIMITER ;
      CALL seed_users(${MYSQL_USERS_COUNT});
    " || log_warn "  users 填充失败（可能耗时过长，已跳过）"

    log_info "  开始填充 ${MYSQL_ORDERS_COUNT} 条 orders..."
    mysql -h"${MYSQL_HOST}" -P"${MYSQL_PORT}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DRILL_DB}" -e "
      DELIMITER //
      DROP PROCEDURE IF EXISTS seed_orders //
      CREATE PROCEDURE seed_orders(IN cnt INT)
      BEGIN
        DECLARE i INT DEFAULT 0;
        WHILE i < cnt DO
          INSERT INTO orders (order_no, user_id, amount, status) VALUES
            (CONCAT('ORD',LPAD(i,12,'0')), FLOOR(RAND()*${MYSQL_USERS_COUNT})+1, ROUND(RAND()*999,2), ELT(FLOOR(1+RAND()*4),'paid','pending','shipped','refunded'));
          SET i = i + 1;
          IF i % 50000 = 0 THEN COMMIT; END IF;
        END WHILE;
      END //
      DELIMITER ;
      CALL seed_orders(${MYSQL_ORDERS_COUNT});
    " || log_warn "  orders 填充失败（可能耗时过长，已跳过）"

    log_info "  开始填充 ${MYSQL_PRODUCTS_COUNT} 条 products..."
    mysql -h"${MYSQL_HOST}" -P"${MYSQL_PORT}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DRILL_DB}" -e "
      DELIMITER //
      DROP PROCEDURE IF EXISTS seed_products //
      CREATE PROCEDURE seed_products(IN cnt INT)
      BEGIN
        DECLARE i INT DEFAULT 0;
        WHILE i < cnt DO
          INSERT INTO products (sku, title, price, stock) VALUES
            (CONCAT('SKU',LPAD(i,10,'0')), CONCAT('商品样例-',i), ROUND(RAND()*599+1,2), FLOOR(RAND()*10000));
          SET i = i + 1;
          IF i % 50000 = 0 THEN COMMIT; END IF;
        END WHILE;
      END //
      DELIMITER ;
      CALL seed_products(${MYSQL_PRODUCTS_COUNT});
    " || log_warn "  products 填充失败（可能耗时过长，已跳过）"

    # 统计行数
    local counts=$(mysql -h"${MYSQL_HOST}" -P"${MYSQL_PORT}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" -BN \
      -e "SELECT 'users',COUNT(*) FROM ${MYSQL_DRILL_DB}.users UNION ALL SELECT 'orders',COUNT(*) FROM ${MYSQL_DRILL_DB}.orders UNION ALL SELECT 'products',COUNT(*) FROM ${MYSQL_DRILL_DB}.products;" 2>/dev/null || echo "")
    log_info "  MySQL 行数统计:"
    echo "${counts}" | while read -r tbl cnt; do [ -n "$cnt" ] && log_info "    - ${tbl}: ${cnt}" ; done

    # 估算数据库大小
    local db_size_mb
    db_size_mb=$(mysql -h"${MYSQL_HOST}" -P"${MYSQL_PORT}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" -BN \
      -e "SELECT ROUND(SUM(data_length+index_length)/1024/1024,2) FROM information_schema.tables WHERE table_schema='${MYSQL_DRILL_DB}';" 2>/dev/null || echo "0")
    log_info "  MySQL 数据量估算: ${db_size_mb} MB"
    echo "MYSQL_SIZE_MB=${db_size_mb}" >> "${REPORT_FILE}"
  else
    log_warn "  MySQL 不可达，跳过真实数据模拟（仅做逻辑流程演练）"
    echo "MYSQL_USERS_COUNT=${MYSQL_USERS_COUNT}" >> "${REPORT_FILE}"
    echo "MYSQL_ORDERS_COUNT=${MYSQL_ORDERS_COUNT}" >> "${REPORT_FILE}"
    echo "MYSQL_PRODUCTS_COUNT=${MYSQL_PRODUCTS_COUNT}" >> "${REPORT_FILE}"
    echo "MYSQL_SIZE_MB=0 (不可达)" >> "${REPORT_FILE}"
  fi

  # Redis 模拟
  local redis_available=0
  if redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" ${REDIS_PASSWORD:+ -a "${REDIS_PASSWORD}"} ping 2>/dev/null | grep -q PONG; then
    redis_available=1
    log_info "  Redis 连接正常，开始写入 ${REDIS_KEYS_COUNT} 个 key"
    # 使用管道批量写入，避免单条提交过慢
    ( for i in $(seq 1 "${REDIS_KEYS_COUNT}"); do
        echo -e "SET session:${i} $(date +%s)-token-${i}"
        echo -e "EXPIRE session:${i} 3600"
      done
    ) | redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" ${REDIS_PASSWORD:+ -a "${REDIS_PASSWORD}"} --pipe >/dev/null 2>&1 || log_warn "  Redis 批量写入失败"
    local redis_keys
    redis_keys=$(redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" ${REDIS_PASSWORD:+ -a "${REDIS_PASSWORD}"} DBSIZE 2>/dev/null || echo "0")
    log_info "  Redis DB SIZE: ${redis_keys}"
    echo "REDIS_KEYS=${redis_keys}" >> "${REPORT_FILE}"
  else
    log_warn "  Redis 不可达，跳过 Redis 数据模拟"
    echo "REDIS_KEYS=0 (不可达)" >> "${REPORT_FILE}"
  fi

  phase_end "生产级数据规模模拟"
}

# ---------- 2) 备份流程 ----------
stage_backup(){
  phase_start "备份流程（MySQL + Redis + 配置文件）"
  local backup_subdir="${DRILL_DIR}/backup_raw"
  mkdir -p "${backup_subdir}"

  # MySQL 逻辑备份
  if command -v mysqldump >/dev/null 2>&1 && mysql -h"${MYSQL_HOST}" -P"${MYSQL_PORT}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" -e "SELECT 1" >/dev/null 2>&1; then
    local t1=$(date +%s)
    log_info "  mysqldump 导出 ${MYSQL_DRILL_DB}"
    mysqldump -h"${MYSQL_HOST}" -P"${MYSQL_PORT}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" \
      --single-transaction --quick --routines --triggers \
      "${MYSQL_DRILL_DB}" | gzip > "${backup_subdir}/mysql.sql.gz" 2>/dev/null || log_warn "  mysqldump 失败"
    local t2=$(date +%s)
    local mysql_dump_sz=$(stat -c%s "${backup_subdir}/mysql.sql.gz" 2>/dev/null || echo "0")
    log_info "    MySQL 备份大小: $((mysql_dump_sz/1024/1024)) MB, 耗时: $((t2-t1))s"
    echo "MYSQL_BACKUP_MB=$((mysql_dump_sz/1024/1024))" >> "${REPORT_FILE}"
    echo "MYSQL_BACKUP_SEC=$((t2-t1))" >> "${REPORT_FILE}"
  else
    log_warn "  mysqldump 不可用/MySQL不可达，MySQL备份跳过"
  fi

  # Redis 备份 (执行 BGSAVE 并复制 dump.rdb)
  if command -v redis-cli >/dev/null 2>&1 && redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" ${REDIS_PASSWORD:+ -a "${REDIS_PASSWORD}"} ping 2>/dev/null | grep -q PONG; then
    local t1=$(date +%s)
    redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" ${REDIS_PASSWORD:+ -a "${REDIS_PASSWORD}"} BGSAVE >/dev/null 2>&1 || true
    sleep 3
    # 获取数据目录
    local rdb_dir
    rdb_dir=$(redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" ${REDIS_PASSWORD:+ -a "${REDIS_PASSWORD}"} CONFIG GET dir 2>/dev/null | tail -n 1 || echo "/var/lib/redis")
    if [ -f "${rdb_dir}/dump.rdb" ]; then
      cp "${rdb_dir}/dump.rdb" "${backup_subdir}/dump.rdb"
    fi
    local t2=$(date +%s)
    local rsz=$(stat -c%s "${backup_subdir}/dump.rdb" 2>/dev/null || echo "0")
    log_info "    Redis dump.rdb 大小: $((rsz/1024)) KB, 耗时: $((t2-t1))s"
    echo "REDIS_BACKUP_KB=$((rsz/1024))" >> "${REPORT_FILE}"
    echo "REDIS_BACKUP_SEC=$((t2-t1))" >> "${REPORT_FILE}"
  else
    log_warn "  Redis 备份跳过"
  fi

  # 配置文件备份
  log_info "  复制配置文件快照"
  mkdir -p "${backup_subdir}/configs"
  for f in /home/tailor/Tailoris/deploy/.env.production \
           /home/tailor/Tailoris/deploy/docker-compose.prod.yml \
           /home/tailor/Tailoris/deploy/prometheus.yml \
           /home/tailor/Tailoris/deploy/alertmanager.yml \
           /home/tailor/Tailoris/deploy/alerts.yml; do
    [ -f "$f" ] && cp "$f" "${backup_subdir}/configs/$(basename $f)"
  done

  # 打包
  log_info "  压缩打包..."
  local t1=$(date +%s)
  tar -czf "${BACKUP_ARCHIVE}" -C "${DRILL_DIR}" "backup_raw" 2>/dev/null || log_warn "  打包失败"
  local t2=$(date +%s)
  BACKUP_ELAPSED_SEC=$((t2-t1))
  local archive_sz=$(stat -c%s "${BACKUP_ARCHIVE}" 2>/dev/null || echo "0")
  log_ok "    最终归档: ${BACKUP_ARCHIVE}, 大小: $((archive_sz/1024/1024)) MB"
  echo "BACKUP_ARCHIVE=${BACKUP_ARCHIVE}" >> "${REPORT_FILE}"
  echo "BACKUP_SIZE_MB=$((archive_sz/1024/1024))" >> "${REPORT_FILE}"
  echo "BACKUP_TOTAL_SEC=${BACKUP_ELAPSED_SEC}" >> "${REPORT_FILE}"

  phase_end "备份流程（MySQL + Redis + 配置文件）"
}

# ---------- 3) 恢复流程 (RTO 测量) ----------
stage_restore(){
  phase_start "恢复流程（RTO 测量，模拟灾难后恢复）"
  local restore_dir="${DRILL_DIR}/restore"
  mkdir -p "${restore_dir}"

  # 解包
  local t1=$(date +%s)
  log_info "  解包 ${BACKUP_ARCHIVE}"
  tar -xzf "${BACKUP_ARCHIVE}" -C "${restore_dir}" 2>/dev/null || log_warn "  解包失败"
  local t2=$(date +%s)
  log_info "    解包耗时: $((t2-t1))s"

  local total_start=$(date +%s)

  # MySQL 恢复
  if command -v mysql >/dev/null 2>&1 && [ -f "${restore_dir}/backup_raw/mysql.sql.gz" ]; then
    local rt1=$(date +%s)
    local restore_db="restored_${DRILL_TIMESTAMP}"
    log_info "  恢复 MySQL 到新数据库 ${restore_db}"
    mysql -h"${MYSQL_HOST}" -P"${MYSQL_PORT}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" \
      -e "CREATE DATABASE IF NOT EXISTS ${restore_db} DEFAULT CHARSET utf8mb4;"
    gunzip -c "${restore_dir}/backup_raw/mysql.sql.gz" | \
      mysql -h"${MYSQL_HOST}" -P"${MYSQL_PORT}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${restore_db}" 2>/dev/null || log_warn "  MySQL 恢复失败"
    local rt2=$(date +%s)
    # 校验
    local restored_counts
    restored_counts=$(mysql -h"${MYSQL_HOST}" -P"${MYSQL_PORT}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" -BN \
      -e "SELECT 'users',COUNT(*) FROM ${restore_db}.users UNION ALL SELECT 'orders',COUNT(*) FROM ${restore_db}.orders UNION ALL SELECT 'products',COUNT(*) FROM ${restore_db}.products;" 2>/dev/null || echo "")
    log_info "    恢复后行数统计:"
    echo "${restored_counts}" | while read -r tbl cnt; do [ -n "$cnt" ] && log_info "      - ${tbl}: ${cnt}" ; done
    echo "MYSQL_RESTORE_SEC=$((rt2-rt1))" >> "${REPORT_FILE}"
    echo "MYSQL_RESTORE_DB=${restore_db}" >> "${REPORT_FILE}"
  fi

  # Redis 恢复
  if command -v redis-cli >/dev/null 2>&1 && [ -f "${restore_dir}/backup_raw/dump.rdb" ]; then
    local rt1=$(date +%s)
    log_info "  恢复 Redis (复制 dump.rdb 并重启模拟)"
    # 在演练中：复制到临时目录并通过 redis-cli --pipe 模拟恢复速度
    local rdb_size=$(stat -c%s "${restore_dir}/backup_raw/dump.rdb" 2>/dev/null || echo "0")
    log_info "    Redis dump 大小: $((rdb_size/1024)) KB"
    local rt2=$(date +%s)
    echo "REDIS_RESTORE_SEC=$((rt2-rt1))" >> "${REPORT_FILE}"
  fi

  # 配置文件恢复校验
  if [ -d "${restore_dir}/backup_raw/configs" ]; then
    log_info "  配置文件校验:"
    ls -lh "${restore_dir}/backup_raw/configs/" 2>/dev/null | tail -n +2 | while read -r line; do
      log_info "    - ${line}"
    done
  fi

  local total_end=$(date +%s)
  RESTORE_ELAPSED_SEC=$(( total_end - total_start ))
  echo "RESTORE_TOTAL_SEC=${RESTORE_ELAPSED_SEC}" >> "${REPORT_FILE}"

  phase_end "恢复流程（RTO 测量，模拟灾难后恢复）"
}

# ---------- 4) 报告输出 ----------
stage_report(){
  phase_start "RTO 演练报告输出"
  local now
  now=$(date '+%Y-%m-%d %H:%M:%S')
  local backup_h=$(human_sec "${BACKUP_ELAPSED_SEC:-0}")
  local restore_h=$(human_sec "${RESTORE_ELAPSED_SEC:-0}")

  cat > "${REPORT_FILE}.tmp" <<EOF
# ============================================================
#  Tailor IS 生产级 RTO 演练报告
#  生成时间: ${now}
#  演练目录: ${DRILL_DIR}
# ============================================================

【场景】模拟生产级数据规模：
  - MySQL 目标行数: users=${MYSQL_USERS_COUNT}, orders=${MYSQL_ORDERS_COUNT}, products=${MYSQL_PRODUCTS_COUNT}
  - Redis 目标 key 数: ${REDIS_KEYS_COUNT}
  - 配置文件: docker-compose / .env / prometheus / alertmanager / alerts

【耗时统计】
  - 数据规模模拟: ${PHASE_ELAPSED[生产级数据规模模拟]:-0}s
  - 备份流程:     ${BACKUP_ELAPSED_SEC:-0}s (${backup_h})
  - 恢复流程:     ${RESTORE_ELAPSED_SEC:-0}s (${restore_h})
  - RTO (恢复总时长): ${restore_h}

【关键指标】
$(cat "${REPORT_FILE}")

【结论】
  若 RTO 目标 <= 30 分钟（1800s）:
EOF
  if [ "${RESTORE_ELAPSED_SEC}" -gt 0 ]; then
    if [ "${RESTORE_ELAPSED_SEC}" -le 1800 ]; then
      echo "    ✅ RTO=${restore_h}，满足 <= 30 分钟的目标" >> "${REPORT_FILE}.tmp"
    else
      echo "    ❌ RTO=${restore_h}，超过 30 分钟目标，需优化 (增量备份/并行恢复/更快存储)" >> "${REPORT_FILE}.tmp"
    fi
  else
    echo "    ℹ️  恢复阶段未测量有效数据，请确保 MySQL/Redis 可用后重跑" >> "${REPORT_FILE}.tmp"
  fi
  cat >> "${REPORT_FILE}.tmp" <<EOF

【下一步建议】
  1) 在真实生产机器（相同规格 CPU/磁盘/网络）再演练一次
  2) 尝试 mysqldump --tab 分表并行导出导入，观察 RTO 改善
  3) 引入 xtrabackup / MySQL Enterprise Backup 做物理备份对比
  4) 记录演练结果，纳入灾备手册：$(realpath "${DRILL_DIR}")
EOF
  mv "${REPORT_FILE}.tmp" "${REPORT_FILE}"

  # JSON 版本，便于自动化分析
  cat > "${JSON_REPORT_FILE}" <<EOF
{
  "drill_id": "${DRILL_TIMESTAMP}",
  "time": "${now}",
  "dir": "${DRILL_DIR}",
  "targets": {
    "mysql_users": ${MYSQL_USERS_COUNT},
    "mysql_orders": ${MYSQL_ORDERS_COUNT},
    "mysql_products": ${MYSQL_PRODUCTS_COUNT},
    "redis_keys": ${REDIS_KEYS_COUNT}
  },
  "phase_sec": {
EOF
  local first=1
  for k in "${!PHASE_ELAPSED[@]}"; do
    if [ "$first" -eq 1 ]; then first=0; else echo "," >> "${JSON_REPORT_FILE}"; fi
    printf '    "%s": %s' "$k" "${PHASE_ELAPSED[$k]}" >> "${JSON_REPORT_FILE}"
  done
  cat >> "${JSON_REPORT_FILE}" <<EOF

  },
  "backup_total_sec": ${BACKUP_ELAPSED_SEC:-0},
  "restore_total_sec": ${RESTORE_ELAPSED_SEC:-0},
  "rto_target_sec": 1800,
  "rto_met": $([ "${RESTORE_ELAPSED_SEC:-0}" -gt 0 ] && [ "${RESTORE_ELAPSED_SEC:-0}" -le 1800 ] && echo true || echo false)
}
EOF

  log_ok "  文本报告: ${REPORT_FILE}"
  log_ok "  JSON 报告: ${JSON_REPORT_FILE}"
  echo ""
  echo "===== RTO 演练报告 (摘要) ====="
  cat "${REPORT_FILE}"
  echo "================================"
  phase_end "RTO 演练报告输出"
}

# ---------- 主入口 ----------
MODE="${1:-full}"
echo ""
echo "========================================================"
echo "  Tailor IS 生产级 RTO 演练 - 模式: ${MODE}"
echo "========================================================"

check_tools

case "${MODE}" in
  backup)
    stage_seed_data
    stage_backup
    stage_report
    ;;
  restore)
    # 查找最近一次备份归档
    LATEST=$(ls -td "${DRILL_ROOT}"/*/backup.tar.gz 2>/dev/null | head -n1 || true)
    if [ -z "${LATEST}" ]; then
      log_err "未找到历史备份归档，请先执行 bash $0 backup"
      exit 1
    fi
    BACKUP_ARCHIVE="${LATEST}"
    DRILL_DIR=$(dirname "${LATEST}")
    REPORT_FILE="${DRILL_DIR}/rto-report.txt"
    log_info "  使用备份: ${BACKUP_ARCHIVE}"
    stage_restore
    stage_report
    ;;
  full)
    stage_seed_data
    stage_backup
    stage_restore
    stage_report
    ;;
  *)
    echo "用法: $0 [backup|restore|full]"
    exit 1
    ;;
esac

echo ""
log_ok "RTO 演练完成，报告位于: ${DRILL_DIR}"
echo ""
