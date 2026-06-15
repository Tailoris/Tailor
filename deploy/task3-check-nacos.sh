#!/bin/bash
exec 2>&1
echo "=== 任务 3: Nacos 鉴权检查 - $(date) ==="

echo ""
echo "--- 步骤 1: Nacos 当前鉴权状态 ---"
# 检查 Nacos 是否启用了鉴权
# 通过未授权访问测试
nacos_unauth=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "http://localhost:8848/nacos/v1/cs/configs?dataId=test&group=DEFAULT_GROUP" 2>/dev/null)
echo "  无 Token 访问配置接口: HTTP $nacos_unauth (期望 403/401 表示已鉴权)"

# 检查 Nacos 配置文件
echo ""
echo "--- 步骤 2: 查找 Nacos 容器 ---"
docker ps 2>&1 | grep -i nacos
echo ""

# 查找 Nacos 容器 ID
nacos_container=$(docker ps 2>/dev/null | grep nacos | awk '{print $1}' | head -1)
echo "  Nacos 容器 ID: $nacos_container"

if [ -z "$nacos_container" ]; then
    echo "  Nacos 容器未找到，尝试其他方式..."

    # 查找 Nacos 进程
    nacos_pid=$(ps -ef | grep nacos | grep -v grep | awk '{print $2}' | head -1)
    echo "  Nacos 进程 PID: $nacos_pid"

    if [ -n "$nacos_pid" ]; then
        # 获取 Nacos 工作目录
        nacos_cwd=$(readlink /proc/$nacos_pid/cwd 2>/dev/null)
        echo "  Nacos CWD: $nacos_cwd"
        ls -la $nacos_cwd/conf/ 2>&1 | head -10
    fi
else
    echo "  Nacos 容器配置:"
    docker exec $nacos_container ls -la /home/nacos/conf/ 2>&1 | head -10
    echo ""
    echo "  application.properties 内容:"
    docker exec $nacos_container cat /home/nacos/conf/application.properties 2>&1 | head -40
fi

echo ""
echo "--- 步骤 3: Nacos 用户列表（如果未鉴权可访问）---"
curl -s "http://localhost:8848/nacos/v1/auth/users?pageNo=1&pageSize=10" 2>&1 | head -3

echo ""
echo "--- 步骤 4: Nacos 命名空间列表 ---"
curl -s "http://localhost:8848/nacos/v1/console/namespaces" 2>&1 | head -5

echo ""
echo "--- 步骤 5: 已注册服务列表（尝试未鉴权访问）---"
curl -s "http://localhost:8848/nacos/v1/ns/catalog/services?pageNo=1&pageSize=20" 2>&1 | head -3

echo ""
echo "=== 任务 3 评估完成 ==="
