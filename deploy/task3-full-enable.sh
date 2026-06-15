#!/bin/bash
exec 2>&1
echo "=== 任务 3: Nacos 鉴权完全开启 - $(date) ==="

echo ""
echo "--- 步骤 1: 完整停止 Nacos 容器 ---"
cd /opt/1panel/apps/nacos/nacos
docker compose down 2>&1
sleep 3
echo "[OK] Nacos 容器已停止"

echo ""
echo "--- 步骤 2: 启动 Nacos（重新加载 .env）---"
docker compose up -d 2>&1
sleep 5
echo "  等待 Nacos 启动..."
sleep 25

echo ""
echo "--- 步骤 3: 验证容器环境变量 ---"
docker inspect 1Panel-nacos-gJky-standalone 2>&1 | grep -E "NACOS_AUTH_ENABLE" | head -3

echo ""
echo "--- 步骤 4: 等待 Nacos API 就绪 ---"
for i in {1..30}; do
    code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 http://localhost:8848/nacos/ 2>/dev/null)
    if [ "$code" != "000" ] && [ -n "$code" ]; then
        echo "  Nacos 响应: HTTP $code (尝试 $i/30)"
        break
    fi
    sleep 2
done

echo ""
echo "--- 步骤 5: 测试鉴权（无 token 应返回 403）---"
test_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "http://localhost:8848/nacos/v1/cs/configs?dataId=test&group=DEFAULT_GROUP" 2>/dev/null)
echo "  无 Token 配置查询: HTTP $test_code (期望 403)"

echo ""
echo "--- 步骤 6: 测试默认账号登录 ---"
# Nacos 2.x 默认登录端点
login_resp=$(curl -s -X POST "http://localhost:8848/nacos/v1/auth/users/login" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "username=nacos&password=nacos" 2>&1)
echo "  nacos/nacos 登录: $login_resp" | head -1

# 备用端点
login_resp2=$(curl -s -X POST "http://localhost:8848/nacos/v1/auth/login" \
    -d "username=nacos&password=nacos" 2>&1)
echo "  备用端点登录: $login_resp2" | head -1

echo ""
echo "--- 步骤 7: 创建服务发现专用账户 ---"
# 先用默认账号登录获取 token
ADMIN_TOKEN=$(echo "$login_resp" | grep -oE '"accessToken":"[^"]*"' | sed 's/"accessToken":"//;s/"//')
if [ -n "$ADMIN_TOKEN" ]; then
    echo "  管理员 token: ${ADMIN_TOKEN:0:30}..."

    # 创建服务账户
    create_resp=$(curl -s -X POST "http://localhost:8848/nacos/v1/auth/users" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -d "username=tailor_service&password=TailorIS2026!" 2>&1)
    echo "  创建账户响应: $create_resp" | head -1
else
    echo "  [WARN] 无法获取管理员 token，跳过账户创建"
fi

echo ""
echo "--- 步骤 8: 验证鉴权状态 ---"
# 用 nacos/nacos 访问服务列表
test_svc=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
    "http://localhost:8848/nacos/v1/ns/catalog/services?pageNo=1&pageSize=10" 2>&1)
echo "  带 token 服务列表: $test_svc" | head -1

echo ""
echo "=== 任务 3 阶段 1 完成 ==="
echo "下一步: 更新所有微服务的 Nacos 连接配置并重启"
