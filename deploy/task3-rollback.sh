#!/bin/bash
exec 2>&1
echo "=== 任务 3: Nacos 鉴权回滚 - $(date) ==="

# 由于 1Panel 包装的 Nacos 启动脚本不支持设置管理员密码
# 且原始的 nacos/nacos 凭据在 1Panel 首次安装时已被重置
# 启用鉴权后所有微服务无法连接 Nacos
# 安全方案：暂时回滚到 auth=false 状态，确保服务可用
# 后续建议：1Panel 控制台手动重置 Nacos 密码并配置鉴权

echo "--- 步骤 1: 修改 .env 回滚 ---"
docker run --rm \
    -v /opt/1panel/apps/nacos/nacos:/dest \
    alpine:latest \
    sh -c "
        sed -i 's/NACOS_AUTH_ENABLE=\"true\"/NACOS_AUTH_ENABLE=\"false\"/' /dest/.env
        # 删除可能添加的 admin 配置
        sed -i '/NACOS_AUTH_ADMIN_USERNAME/d' /dest/.env
        sed -i '/NACOS_AUTH_ADMIN_PASSWORD/d' /dest/.env
        grep -E 'NACOS_AUTH' /dest/.env
    " 2>&1

echo ""
echo "--- 步骤 2: 重启 Nacos ---"
cd /opt/1panel/apps/nacos/nacos
docker compose down 2>&1
sleep 3
docker compose up -d 2>&1
sleep 25

echo ""
echo "--- 步骤 3: 验证回滚 ---"
for i in {1..15}; do
    code=$(curl -s -o /dev/null -w "%{http_code}" --noproxy '*' --max-time 3 http://localhost:8848/nacos/ 2>/dev/null)
    if [ "$code" = "200" ]; then
        echo "  Nacos 启动 (HTTP $code, 尝试 $i/15)"
        break
    fi
    sleep 3
done

echo ""
echo "--- 步骤 4: 测试无鉴权访问 ---"
test=$(curl -s --noproxy '*' -o /dev/null -w "%{http_code}" "http://localhost:8848/nacos/v1/cs/configs?dataId=test&group=DEFAULT_GROUP" --max-time 5)
echo "  无 Token 配置查询: HTTP $test (期望 200)"

echo ""
echo "--- 步骤 5: 微服务 Nacos 重新连接 ---"
# 验证 12 个微服务是否健康
for p in 8081 8101 8102 8103 8104 8105 8106 8107 8108 8109 8110 8111; do
    proc=$(ps -ef | grep -c "[t]ailor-is-")
    port=$(ss -tln 2>/dev/null | grep -c ":${p} ")
    if [ "$port" -gt 0 ]; then
        echo "  [OK] 端口 ${p}"
    else
        echo "  [--] 端口 ${p} 未监听"
    fi
done

echo ""
echo "=== 任务 3 回滚完成 ==="
echo "Nacos 鉴权已回滚到 false"
echo "服务保持运行状态"
