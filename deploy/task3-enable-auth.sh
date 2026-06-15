#!/bin/bash
exec 2>&1
echo "=== 任务 3: Nacos 鉴权开启 - $(date) ==="

# 1. 修改 Nacos .env 文件
echo ""
echo "--- 步骤 1: 修改 Nacos .env 启用鉴权 ---"
# 通过 Docker 修改 .env 文件
docker run --rm \
    -v /opt/1panel/apps/nacos/nacos:/dest \
    alpine:latest \
    sh -c "
        sed -i 's/NACOS_AUTH_ENABLE=\"false\"/NACOS_AUTH_ENABLE=\"true\"/' /dest/.env
        cat /dest/.env | grep NACOS_AUTH
    " 2>&1

echo ""
echo "--- 步骤 2: 重启 Nacos 容器 ---"
# 获取 Nacos 容器名
NACOS_CONTAINER="1Panel-nacos-gJky-standalone"
echo "  Nacos 容器: $NACOS_CONTAINER"

# 使用 docker compose 重启（确保配置生效）
cd /opt/1panel/apps/nacos/nacos
docker compose restart 2>&1 || docker-compose restart 2>&1

echo "  等待 Nacos 重启完成..."
sleep 30

echo ""
echo "--- 步骤 3: 验证 Nacos 鉴权状态 ---"
# 测试无 token 访问
test_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "http://localhost:8848/nacos/v1/cs/configs?dataId=test&group=DEFAULT_GROUP" 2>/dev/null)
echo "  无 Token 访问配置: HTTP $test_code (期望 403)"

# 测试登录
echo ""
echo "--- 步骤 4: 测试默认账号登录 ---"
login_resp=$(curl -s -X POST "http://localhost:8848/nacos/v1/auth/login" \
    -d "username=nacos&password=nacos" 2>&1)
echo "  nacos/nacos 登录响应: $login_resp" | head -1

login_resp2=$(curl -s -X POST "http://localhost:8848/nacos/v1/auth/login" \
    -d "username=nacos&password=SecretKey012345678901234567890123456789012345678901234567890123456789" 2>&1)
echo "  nacos/Token 登录响应: $login_resp2" | head -1

echo ""
echo "=== 任务 3 步骤 1-4 完成 ==="
