#!/bin/bash
exec 2>&1
echo "=== Nacos 身份检查测试 ==="

# Nacos 2.x+ 启用了 Server Identity Check
# 需要正确的 User-Agent 和 Request-Origin 头

echo "--- 添加 User-Agent 和 Request-Origin ---"
curl -sLv --noproxy '*' -X POST 'http://localhost:8848/nacos/v1/auth/users/login' \
    -H 'User-Agent: Nacos-Server' \
    -H 'Request-Origin: browser' \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    -d 'username=nacos&password=nacos' --max-time 5 2>&1 | tail -15

echo ""
echo "--- 模拟浏览器请求 ---"
curl -s --noproxy '*' -X POST 'http://localhost:8848/nacos/v1/auth/users/login' \
    -H 'User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36' \
    -H 'Accept: application/json, text/plain, */*' \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    -H 'Origin: http://localhost:8081' \
    -H 'Referer: http://localhost:8081/' \
    -d 'username=nacos&password=nacos' --max-time 5 2>&1

echo ""
echo "--- 添加 Authorization ---"
curl -s --noproxy '*' -X POST 'http://localhost:8848/nacos/v1/auth/users/login' \
    -H 'Authorization: Bearer ' \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    -d 'username=nacos&password=nacos' --max-time 5 2>&1
