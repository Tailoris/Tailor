#!/bin/bash
exec 2>&1
echo "=== 任务 2: AI 数据库检查 - $(date) ==="

echo ""
echo "--- 步骤 1: 检查 AI 相关 SQL 脚本 ---"

# 在多个位置查找
echo "  1. /opt/tailor-is/sql/"
ls -la /opt/tailor-is/sql/ 2>&1

echo ""
echo "  2. 项目根目录 SQL"
ls -la /opt/tailor-is/*.sql 2>&1 | head -10
ls -la /opt/tailor-is/sql/ 2>&1

echo ""
echo "  3. 全项目搜索 ai 相关 SQL"
find /opt/tailor-is /mnt/host/f/Tailor -name "*ai*.sql" 2>/dev/null | head -20
find /opt/tailor-is /mnt/host/f/Tailor -iname "*ai*.sql" 2>/dev/null | head -20

echo ""
echo "  4. 检查项目 SQL 目录"
ls -la /mnt/host/f/Tailor/Tailor\ is/tailor-is/sql/ 2>&1 | head -30

echo ""
echo "--- 步骤 2: 检查数据库列表 ---"
mysql -h127.0.0.1 -P3306 -uroot -pmysql_CA75Yk -N -e "SHOW DATABASES LIKE 'tailor_is%';" 2>/dev/null | sort

echo ""
echo "--- 步骤 3: 检查 tailor_is_ai 当前状态 ---"
mysql -h127.0.0.1 -P3306 -uroot -pmysql_CA75Yk -e "USE tailor_is_ai; SHOW TABLES;" 2>&1
echo ""
echo "  tailor_is_ai 表数量: $(mysql -h127.0.0.1 -P3306 -uroot -pmysql_CA75Yk -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='tailor_is_ai';" 2>/dev/null)"

echo ""
echo "--- 步骤 4: AI 服务日志中的数据库引用 ---"
ls -la /opt/tailor-is/logs/ai.log 2>&1
echo ""
echo "  AI 服务启动日志中的 DB 引用:"
grep -E "tailor_is|database|jdbc:" /opt/tailor-is/logs/ai.log 2>/dev/null | head -10
