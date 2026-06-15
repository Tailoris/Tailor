#!/bin/bash
exec 2>&1
echo "=== 重置 Nacos 管理员密码 v2 - $(date) ==="

echo ""
echo "--- 步骤 1: 备份 derby-data ---"
TS=$(date +%s)
docker run --rm \
    -v /opt/1panel/apps/nacos/nacos/data:/data:rw \
    alpine:latest \
    sh -c "cp -r /data/derby-data /data/derby-data.bak.${TS} && echo '备份完成'" 2>&1

ls -la /opt/1panel/apps/nacos/nacos/data/ | head -5

echo ""
echo "--- 步骤 2: 删除 derby-data (将重新初始化) ---"
docker run --rm \
    -v /opt/1panel/apps/nacos/nacos/data:/data:rw \
    alpine:latest \
    sh -c "rm -rf /data/derby-data && echo 'derby-data 已删除'" 2>&1

ls -la /opt/1panel/apps/nacos/nacos/data/

echo ""
echo "--- 步骤 3: 重新启动 Nacos (auth 仍启用) ---"
cd /opt/1panel/apps/nacos/nacos
docker compose up -d 2>&1
echo "  等待 Nacos 启动..."
sleep 30

echo ""
echo "--- 步骤 4: 验证 Nacos 状态 ---"
docker ps | grep nacos

echo ""
echo "--- 步骤 5: 测试默认密码登录 ---"
for i in {1..10}; do
    code=$(curl -s -o /dev/null -w "%{http_code}" --noproxy '*' --max-time 5 http://localhost:8848/nacos/ 2>/dev/null)
    if [ "$code" = "200" ]; then
        echo "  Nacos 启动成功 (HTTP $code, 尝试 $i/10)"
        break
    fi
    sleep 3
done

# 尝试默认密码
echo ""
echo "--- 步骤 6: 测试 nacos/nacos 登录 ---"
login=$(curl -s --noproxy '*' -X POST "http://localhost:8848/nacos/v1/auth/users/login" \
    -d "username=nacos&password=nacos" --max-time 5)
echo "  nacos/nacos 登录: $login"

# 尝试用 env 中的 token
echo ""
echo "--- 步骤 7: 测试 Token 登录 ---"
login2=$(curl -s --noproxy '*' -X POST "http://localhost:8848/nacos/v1/auth/users/login" \
    -d "username=nacos&password=SecretKey012345678901234567890123456789012345678901234567890123456789" --max-time 5)
echo "  Token 登录: $login2"
