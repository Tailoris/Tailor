#!/bin/bash
exec 2>&1
echo "=== 尝试 Nacos 不同登录端点 ==="

echo "--- 控制台端口 8081 ---"
curl -s --noproxy '*' -o /dev/null -w 'HTTP %{http_code}\n' http://localhost:8081/
echo "--- 容器内 8080 ---"
curl -s --noproxy '*' -o /dev/null -w 'HTTP %{http_code}\n' http://localhost:8080/

echo ""
echo "--- 尝试各种登录端点 ---"
for ep in "/nacos/v1/auth/users/login" "/nacos/v1/auth/login" "/nacos/v2/auth/user/login" "/nacos/v2/auth/login" "/nacos/v1/auth/admin/login" "/nacos/v1/auth/authorization/login"; do
    code=$(curl -s --noproxy '*' -o /dev/null -w '%{http_code}' -X POST "http://localhost:8848${ep}" -d 'username=nacos&password=nacos' --max-time 3)
    echo "  ${ep}: HTTP ${code}"
done

echo ""
echo "--- 尝试 GET 用户列表（无认证）---"
for ep in "/nacos/v1/auth/users" "/nacos/v1/auth/users?pageNo=1&pageSize=10" "/nacos/v2/auth/user/list"; do
    code=$(curl -s --noproxy '*' -o /dev/null -w '%{http_code}' "http://localhost:8848${ep}" --max-time 3)
    echo "  ${ep}: HTTP ${code}"
done

echo ""
echo "--- 尝试直接调用容器内的 curl ---"
docker exec 1Panel-nacos-gJky-standalone \
    curl -s -X POST "http://localhost:8848/nacos/v1/auth/users/login" \
    -d "username=nacos&password=nacos" --max-time 5 2>&1
