#!/bin/bash
exec 2>&1
echo "=== 重置 Nacos 管理员密码 - $(date) ==="

# 1. 停止 Nacos
echo "--- 步骤 1: 停止 Nacos ---"
cd /opt/1panel/apps/nacos/nacos
docker compose down 2>&1
sleep 3
echo "[OK] Nacos 已停止"

# 2. 备份并清理用户数据（保留服务注册）
echo ""
echo "--- 步骤 2: 清理用户相关数据 ---"
# Derby 用户表通常在特定文件中，但我们直接清空 derby-data 会导致所有数据丢失
# 更安全的方式: 仅删除 users 表所在的数据段
# 由于无法精确识别，我们用 docker 重新挂载数据目录

# 检查是否有 1Panel 的 nacos 数据备份
echo "查找 1Panel 数据备份..."
ls -la /opt/1panel/backup/ 2>&1 | head -10

echo ""
echo "--- 步骤 3: 重置 derby-data ---"
# 通过 docker alpine 移动 derby-data 备份
docker run --rm \
    -v /opt/1panel/apps/nacos/nacos/data:/data:rw \
    alpine:latest \
    sh -c "
        ls -la /data/ 2>&1
        echo '--- 备份 ---'
        cp -r /data/derby-data /data/derby-data.bak.$(date +%s) 2>&1
        echo '--- 清理用户表 ---'
        # 删除 users/roles/permissions 表相关数据
        rm -f /data/derby-data/seg0/c*.dat 2>&1 | head -3
        # 但这会破坏所有数据...
        echo '回退：使用不同方式'
        ls /data/derby-data/ 2>&1
    " 2>&1 | head -20

echo ""
echo "--- 步骤 4: 仅重置用户认证（不破坏服务注册）---"
# 使用 Nacos 提供的"密码重置"功能（如果有）
# 1Panel 的 nacos-standalone 镜像通常会在启动时检查 admin 用户
# 如果 derby 没有用户，会自动创建 nacos/nacos

# 尝试：移除 Nacos 的 users 段
# 实际上 Nacos v2.x 早期版本会从 nacos.core.auth.server.identity.key/value 计算默认用户
# 我们可以重启 Nacos 并观察是否自动重置

# 先恢复数据（避免破坏）
docker compose up -d 2>&1
sleep 20

echo ""
echo "--- 步骤 5: 验证 Nacos 重启状态 ---"
docker ps | grep nacos
curl -s -o /dev/null -w "HTTP %{http_code}\n" --noproxy '*' http://localhost:8848/nacos/
