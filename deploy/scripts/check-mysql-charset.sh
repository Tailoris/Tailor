#!/bin/bash
set -e

MYSQL_HOST="${MYSQL_HOST:-localhost}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"
MYSQL_ARGS=""

if [ -n "$MYSQL_PASSWORD" ]; then
    MYSQL_ARGS="-p$MYSQL_PASSWORD"
fi

echo "=============================================="
echo " MySQL 字符集配置检查脚本"
echo "=============================================="
echo ""

echo "[1] 服务器全局字符集配置"
echo "------------------------"
mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" $MYSQL_ARGS -e "
SHOW VARIABLES LIKE 'character_set_%';
SHOW VARIABLES LIKE 'collation_%';
" 2>/dev/null || {
    echo "ERROR: 无法连接到MySQL服务器"
    echo "请确保MySQL服务已启动，并设置正确的环境变量："
    echo "  MYSQL_HOST, MYSQL_PORT, MYSQL_USER, MYSQL_PASSWORD"
    exit 1
}

echo ""
echo "[2] 数据库字符集检查"
echo "-------------------"
mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" $MYSQL_ARGS -e "
SELECT SCHEMA_NAME, DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME 
FROM INFORMATION_SCHEMA.SCHEMATA 
WHERE SCHEMA_NAME LIKE 'tailor_%';
"

echo ""
echo "[3] 表字符集检查（tailor_is_product示例）"
echo "---------------------------------------"
mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" $MYSQL_ARGS -e "
SELECT TABLE_NAME, TABLE_COLLATION 
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'tailor_is_product' 
LIMIT 10;
"

echo ""
echo "[4] 列字符集检查（含中文的列）"
echo "----------------------------"
mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" $MYSQL_ARGS -e "
SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_SET_NAME, COLLATION_NAME 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'tailor_is_product' 
AND CHARACTER_SET_NAME IS NOT NULL
LIMIT 20;
"

echo ""
echo "[5] 字符集一致性验证"
echo "-------------------"
RESULT=$(mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" $MYSQL_ARGS -N -e "
SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA LIKE 'tailor_%' 
AND CHARACTER_SET_NAME != 'utf8mb4';
")

if [ "$RESULT" -eq 0 ]; then
    echo "✓ 所有表列字符集均为 utf8mb4"
else
    echo "✗ 发现 $RESULT 个非 utf8mb4 的列，需要修复"
    mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" $MYSQL_ARGS -e "
    SELECT TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, CHARACTER_SET_NAME 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA LIKE 'tailor_%' 
    AND CHARACTER_SET_NAME != 'utf8mb4';
    "
fi

echo ""
echo "[6] 连接字符集测试"
echo "-----------------"
mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" $MYSQL_ARGS -e "
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SELECT '连接字符集测试：中文测试';
"

echo ""
echo "=============================================="
echo " 检查完成"
echo "=============================================="
echo ""
echo "建议的 my.cnf/my.ini 配置："
echo "[mysqld]"
echo "character-set-server = utf8mb4"
echo "collation-server = utf8mb4_unicode_ci"
echo "default-storage-engine = InnoDB"
echo "init_connect = 'SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci'"
echo ""
echo "[client]"
echo "default-character-set = utf8mb4"