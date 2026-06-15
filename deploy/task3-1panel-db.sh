#!/bin/bash
exec 2>&1
echo "=== 1Panel 数据库查询 ==="

echo "--- 1Panel 数据库表 ---"
sqlite3 /opt/1panel/db/1Panel.db ".tables" 2>&1

echo ""
echo "--- nacos 应用配置 ---"
sqlite3 /opt/1panel/db/1Panel.db "SELECT * FROM app_installs WHERE name LIKE '%nacos%';" 2>&1 | head -3

echo ""
echo "--- app_installs 表结构 ---"
sqlite3 /opt/1panel/db/1Panel.db ".schema app_installs" 2>&1
