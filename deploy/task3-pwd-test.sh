#!/bin/bash
exec 2>&1
echo "=== 详细登录测试 ==="

echo "--- 详细登录响应 ---"
curl -sLv --noproxy '*' -X POST 'http://localhost:8848/nacos/v1/auth/users/login' \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    -d 'username=nacos&password=nacos' --max-time 5 2>&1 | tail -20

echo ""
echo "--- 多种密码尝试 ---"
for p in "nacos" "Nacos@2024" "Nacos123!" "admin" "admin123" "secret" "SecretKey" "tailor" "tailoris2026" "Nacos" "123456" "1Panel" "1qaz2wsx"; do
    code=$(curl -s --noproxy '*' -o /dev/null -w '%{http_code}' -X POST 'http://localhost:8848/nacos/v1/auth/users/login' -d "username=nacos&password=${p}" --max-time 3)
    echo "  nacos/${p}: HTTP ${code}"
done
