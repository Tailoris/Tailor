#!/bin/bash
exec 2>&1
echo "=== 详细检查 Nacos 鉴权配置 ==="

CONTAINER=8ed1ba786523

echo ""
echo "--- application.properties 鉴权相关行 ---"
docker exec $CONTAINER grep -E "nacos.core.auth|auth.system|auth.enabled|token" /home/nacos/conf/application.properties 2>&1

echo ""
echo "--- Nacos 登录测试（默认账号）---"
# Nacos 2.x 默认凭据: nacos/nacos
resp=$(curl -s -X POST "http://localhost:8848/nacos/v1/auth/login" \
    -d "username=nacos&password=nacos" 2>&1)
echo "  nacos/nacos 登录响应: $resp" | head -1

# 也试试 1Panel 的应用商店默认密码（如果有）
echo ""
echo "--- 检查 1Panel 应用商店配置 ---"
ls -la /opt/1panel/apps/ 2>&1 | head -5

echo ""
echo "--- 测试当前 Nacos 鉴权状态 ---"
# 使用 Nacos 默认账号测试
token=$(curl -s -X POST "http://localhost:8848/nacos/v1/auth/login" \
    -d "username=nacos&password=nacos" 2>&1 | grep -oE '"accessToken":"[^"]*"' | head -1)
echo "  默认账号 token: $token"

if [ -z "$token" ]; then
    echo "  [INFO] 默认账号不可用，鉴权可能未启用"
    # 测试无 token 访问
    test=$(curl -s "http://localhost:8848/nacos/v1/cs/configs?dataId=tis&group=DEFAULT_GROUP" 2>&1)
    echo "  无 token 配置查询: $test" | head -2
else
    echo "  [OK] 默认账号可用，鉴权已启用"
fi

echo ""
echo "--- Nacos 容器环境变量 ---"
docker inspect $CONTAINER 2>&1 | grep -A2 -E "JVM|VM_OPT|env" | head -30

echo ""
echo "--- 完整 application.properties 鉴权段 ---"
docker exec $CONTAINER cat /home/nacos/conf/application.properties 2>&1 | grep -A1 -B1 -iE "auth|token|secret" | head -40
