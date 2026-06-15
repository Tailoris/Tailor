#!/bin/bash
exec 2>&1
echo "=== 查找 1Panel 中的 nacos 密码 ==="

# 通过 Docker 挂载读取 1Panel 数据库
echo "--- Docker 方式读取 1Panel DB ---"
docker run --rm \
    -v /opt/1panel/db:/db:ro \
    alpine:latest \
    sh -c "
        which sqlite3 2>&1 || apk add sqlite 2>&1
        sqlite3 /db/1Panel.db '.tables' 2>&1
    " 2>&1 | head -30

echo ""
echo "--- 直接查看 nacos 数据目录 ---"
ls -la /opt/1panel/apps/nacos/nacos/data/ 2>&1 | head -20
find /opt/1panel/apps/nacos -name "users*" 2>&1 | head -10
find /opt/1panel/apps/nacos -name "*.der" 2>&1 | head -10

echo ""
echo "--- 容器内 nacos data 目录 ---"
docker exec 1Panel-nacos-gJky-standalone ls -la /home/nacos/data/ 2>&1 | head -20
