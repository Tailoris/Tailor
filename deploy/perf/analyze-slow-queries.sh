#!/bin/bash
# ==============================================================================
# Tailor IS - 慢查询分析脚本
# Phase 3 P3-5: 数据库索引优化 + SQL 审查 (慢查询 < 50ms)
# ==============================================================================
# 用途: 分析和诊断 MySQL 慢查询，辅助索引优化
# 使用: ./deploy/perf/analyze-slow-queries.sh
# ==============================================================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_step()  { echo -e "${BLUE}[STEP]${NC} $1"; }

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASS="${MYSQL_PASSWORD:-mysql_ZmY2sr}"
DB_NAME="${DB_NAME:-tailor_is}"

MYSQL_CMD="mysql -h${MYSQL_HOST} -P${MYSQL_PORT} -u${MYSQL_USER} -p${MYSQL_PASS}"

echo "============================================================"
echo "  Tailor IS 慢查询分析 - 阶段3 (P3-5)"
echo "  目标: 慢查询 < 50ms"
echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "============================================================"

# ============================================================================
# 1. 慢查询日志状态
# ============================================================================
log_step "1. 慢查询日志状态"
$MYSQL_CMD -e "
SHOW VARIABLES LIKE 'slow_query_log%';
SHOW VARIABLES LIKE 'long_query_time';
SHOW VARIABLES LIKE 'log_queries_not_using_indexes';
" 2>/dev/null || log_warn "无法连接 MySQL"

# ============================================================================
# 2. 当前慢查询 TOP 10
# ============================================================================
log_step "2. 最近慢查询 TOP 10"
$MYSQL_CMD -e "
SELECT
    sql_text,
    ROUND(query_time_ms, 2) AS time_ms,
    rows_examined,
    rows_sent,
    created_at
FROM ${DB_NAME}.slow_query_log
ORDER BY query_time_ms DESC
LIMIT 10;
" 2>/dev/null || log_warn "慢查询日志表为空或不存在"

# ============================================================================
# 3. 全表扫描检测
# ============================================================================
log_step "3. 全表扫描检测"
$MYSQL_CMD -e "
SELECT
    DIGEST_TEXT AS query_pattern,
    COUNT_STAR AS exec_count,
    ROUND(AVG_TIMER_WAIT / 1000000000, 2) AS avg_time_ms,
    ROUND(SUM_ROWS_EXAMINED / COUNT_STAR, 0) AS avg_rows_examined,
    ROUND(SUM_ROWS_SENT / COUNT_STAR, 0) AS avg_rows_sent
FROM performance_schema.events_statements_summary_by_digest
WHERE DIGEST_TEXT LIKE '%SELECT%'
  AND SUM_ROWS_EXAMINED > 10000
ORDER BY SUM_ROWS_EXAMINED DESC
LIMIT 10;
" 2>/dev/null || log_warn "performance_schema 不可用"

# ============================================================================
# 4. 未使用索引检测
# ============================================================================
log_step "4. 未使用索引检测"
$MYSQL_CMD -e "
SELECT
    OBJECT_NAME AS table_name,
    INDEX_NAME,
    COUNT_STAR AS total_io_ops
FROM performance_schema.table_io_waits_summary_by_index_usage
WHERE OBJECT_SCHEMA = '${DB_NAME}'
  AND INDEX_NAME IS NOT NULL
  AND INDEX_NAME != 'PRIMARY'
  AND COUNT_STAR = 0
ORDER BY OBJECT_NAME, INDEX_NAME;
" 2>/dev/null || log_warn "无未使用索引或 performance_schema 不可用"

# ============================================================================
# 5. 表大小与索引统计
# ============================================================================
log_step "5. 表大小与索引统计"
$MYSQL_CMD -e "
SELECT
    TABLE_NAME AS '表名',
    TABLE_ROWS AS '行数',
    ROUND(DATA_LENGTH / 1024 / 1024, 2) AS '数据大小(MB)',
    ROUND(INDEX_LENGTH / 1024 / 1024, 2) AS '索引大小(MB)',
    ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS '总大小(MB)'
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = '${DB_NAME}'
  AND TABLE_TYPE = 'BASE TABLE'
ORDER BY (DATA_LENGTH + INDEX_LENGTH) DESC
LIMIT 20;
" 2>/dev/null

# ============================================================================
# 6. 索引基数分析 (低选择性索引)
# ============================================================================
log_step "6. 低选择性索引分析"
$MYSQL_CMD -e "
SELECT
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    CARDINALITY,
    CASE
        WHEN CARDINALITY < 10 THEN 'VERY_LOW'
        WHEN CARDINALITY < 100 THEN 'LOW'
        WHEN CARDINALITY < 1000 THEN 'MEDIUM'
        ELSE 'GOOD'
    END AS selectivity
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = '${DB_NAME}'
  AND INDEX_NAME != 'PRIMARY'
  AND CARDINALITY < 1000
ORDER BY CARDINALITY ASC, TABLE_NAME;
" 2>/dev/null

# ============================================================================
# 7. 缺失索引建议 (基于慢查询)
# ============================================================================
log_step "7. 缺失索引分析"
$MYSQL_CMD -e "
SELECT
    t.TABLE_SCHEMA,
    t.TABLE_NAME,
    t.TABLE_ROWS,
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS s
     WHERE s.TABLE_SCHEMA = t.TABLE_SCHEMA AND s.TABLE_NAME = t.TABLE_NAME
       AND s.INDEX_NAME != 'PRIMARY') AS current_index_count
FROM INFORMATION_SCHEMA.TABLES t
WHERE t.TABLE_SCHEMA = '${DB_NAME}'
  AND t.TABLE_TYPE = 'BASE TABLE'
  AND t.TABLE_ROWS > 1000
  AND (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS s
       WHERE s.TABLE_SCHEMA = t.TABLE_SCHEMA AND s.TABLE_NAME = t.TABLE_NAME
         AND s.INDEX_NAME != 'PRIMARY') < 3
ORDER BY t.TABLE_ROWS DESC;
" 2>/dev/null

echo ""
log_info "分析完成!"