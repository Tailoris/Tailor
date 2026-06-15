#!/bin/bash
echo "=== 检查现有数据库 ==="
mysql -hlocalhost -P3306 -uroot -pmysql_CA75Yk -e "SHOW DATABASES;" 2>&1 | head -30
echo ""
echo "=== SQL 文件列表 ==="
ls -la /opt/tailor-is/sql/ 2>&1
echo ""
echo "=== SQL 文件首行（创建数据库）==="
for f in /opt/tailor-is/sql/*.sql; do
    name=$(basename $f)
    line1=$(head -1 "$f" 2>/dev/null)
    dbname=$(grep -iE "^CREATE DATABASE|^USE " "$f" 2>/dev/null | head -2)
    echo "--- $name ---"
    echo "  $line1"
    echo "  $dbname"
done
