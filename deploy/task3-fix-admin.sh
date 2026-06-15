#!/bin/bash
exec 2>&1
echo "=== 修复 Nacos 默认管理员 - $(date) ==="

# 方案：停止 Nacos，添加默认管理员配置，重新启动
echo "--- 步骤 1: 停止 Nacos ---"
cd /opt/1panel/apps/nacos/nacos
docker compose down 2>&1
sleep 3

echo ""
echo "--- 步骤 2: 修改 .env 添加默认管理员配置 ---"
# 通过 docker 修改
docker run --rm \
    -v /opt/1panel/apps/nacos/nacos:/dest \
    alpine:latest \
    sh -c "
        echo '' >> /dest/.env
        echo '# Default admin user (Nacos 3.x)' >> /dest/.env
        echo 'NACOS_AUTH_ADMIN_USERNAME=nacos' >> /dest/.env
        echo 'NACOS_AUTH_ADMIN_PASSWORD=nacos' >> /dest/.env
        tail -10 /dest/.env
    " 2>&1

echo ""
echo "--- 步骤 3: 重新启动 Nacos ---"
docker compose up -d 2>&1
sleep 30

echo ""
echo "--- 步骤 4: 等待 Nacos 完全启动 ---"
for i in {1..20}; do
    code=$(curl -s -o /dev/null -w "%{http_code}" --noproxy '*' --max-time 3 http://localhost:8848/nacos/ 2>/dev/null)
    if [ "$code" = "200" ]; then
        echo "  Nacos 启动 (HTTP $code, 尝试 $i/20)"
        sleep 5  # 额外等待以让初始化完成
        break
    fi
    sleep 3
done

echo ""
echo "--- 步骤 5: 测试登录 ---"
login=$(curl -s --noproxy '*' -X POST "http://localhost:8848/nacos/v1/auth/users/login" \
    -d "username=nacos&password=nacos" --max-time 5)
echo "  登录响应: $login"

if echo "$login" | grep -q "accessToken"; then
    echo "  [OK] 登录成功!"
    TOKEN=$(echo "$login" | grep -oE '"accessToken":"[^"]*"' | sed 's/"accessToken":"//;s/"$//')
    echo "  Token: ${TOKEN:0:50}..."

    # 创建服务账户
    echo ""
    echo "--- 步骤 6: 创建服务账户 ---"
    create=$(curl -s --noproxy '*' -X POST "http://localhost:8848/nacos/v1/auth/users" \
        -H "Authorization: Bearer $TOKEN" \
        -d "username=tailor_service&password=TailorIS2026@Secure" --max-time 5)
    echo "  创建响应: $create"
fi
