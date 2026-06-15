#!/bin/bash
exec 2>&1
echo "=== 尝试重置 Nacos 管理员密码 ==="

# 方法 1: 在容器内查找 Nacos 用户配置
echo "--- 容器内查找用户表 ---"
docker exec 1Panel-nacos-gJky-standalone \
    find /home/nacos/data/derby-data -name "*.dat" 2>/dev/null | xargs -I{} bash -c 'echo "=== {} ==="; head -c 500 "{}" 2>/dev/null | strings 2>/dev/null | grep -iE "nacos|admin|user" | head -5'

echo ""
echo "--- 直接查看 derby-data 目录 ---"
docker exec 1Panel-nacos-gJky-standalone ls -laR /home/nacos/data/derby-data/ 2>&1 | head -30

echo ""
echo "--- Nacos 用户 SQL 初始化文件 ---"
docker exec 1Panel-nacos-gJky-standalone \
    find /home/nacos -name "*.sql" 2>/dev/null | head -5
