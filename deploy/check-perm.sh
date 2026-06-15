#!/bin/bash
exec 2>&1
echo "=== 权限提升方法测试 ==="

echo ""
echo "[1] sudo 密码配置"
sudo -n true 2>&1 && echo "  sudo NOPASSWD OK" || echo "  sudo 需要密码"

echo ""
echo "[2] 1pctl 工具"
which 1pctl
1pctl --help 2>&1 | head -10

echo ""
echo "[3] 1Panel 用户信息"
id tailor
groups tailor

echo ""
echo "[4] /opt/tailor-is 目录结构"
ls -la /opt/tailor-is/ 2>&1 | head -15

echo ""
echo "[5] 检查 setuid 二进制"
find / -perm -4000 -type f 2>/dev/null | head -10

echo ""
echo "[6] docker 是否可用"
docker ps 2>&1 | head -3
echo "  是否有 docker socket: $(ls -la /var/run/docker.sock 2>&1 | head -1)"
