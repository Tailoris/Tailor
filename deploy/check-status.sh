#!/bin/bash
echo "================================================"
echo "=== 1Panel 部署状态检查 ==="
echo "=== $(date '+%Y-%m-%d %H:%M:%S') ==="
echo "================================================"

echo ""
echo "=== 1. 1Panel 状态 ==="
systemctl is-active 1panel 2>&1

echo ""
echo "=== 2. 基础服务监听状态 ==="
ss -tln 2>/dev/null | grep -E ':(3306|6379|5672|8848|9848|15672|80|443) ' | sort -k4

echo ""
echo "=== 3. Tailor IS 进程列表 ==="
ps -ef | grep -E 'tailor-is' | grep -v grep | awk '{print $2, $NF}'

echo ""
echo "=== 4. 各微服务健康检查（端口 8081-8090）==="
for p in 8081 8082 8083 8084 8085 8086 8087 8088 8089 8090; do
    code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 "http://localhost:${p}/actuator/health" 2>/dev/null)
    if [ "$code" = "200" ]; then
        echo "  [OK] 端口 ${p}: HTTP ${code} 健康"
    else
        echo "  [--] 端口 ${p}: HTTP ${code:-未响应}"
    fi
done

echo ""
echo "=== 5. 基础设施服务连通性 ==="
ver=$(mysql -hlocalhost -P3306 -uroot -pmysql_CA75Yk -e "SELECT VERSION();" 2>/dev/null | tail -1)
if [ -n "$ver" ]; then
    echo "  [OK] MySQL: $ver"
else
    echo "  [FAIL] MySQL 连接失败"
fi

redis_ping=$(redis-cli -h localhost -p 6379 -a redis_RSeR4G PING 2>/dev/null)
if echo "$redis_ping" | grep -q PONG; then
    echo "  [OK] Redis: PONG"
else
    echo "  [FAIL] Redis: $redis_ping"
fi

nacos_code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 http://localhost:8848/nacos/ 2>/dev/null)
if [ "$nacos_code" = "200" ] || [ "$nacos_code" = "302" ]; then
    echo "  [OK] Nacos: HTTP $nacos_code"
else
    echo "  [FAIL] Nacos: HTTP $nacos_code"
fi

rmq_status=$(curl -s -u rabbitmq:rabbitmq --max-time 3 http://localhost:15672/api/overview 2>/dev/null | grep -o 'rabbitmq_version' | head -1)
if [ -n "$rmq_status" ]; then
    echo "  [OK] RabbitMQ: Dashboard OK"
else
    echo "  [FAIL] RabbitMQ: Dashboard 无响应"
fi

echo ""
echo "=== 6. 数据库列表（tailor_is*）==="
mysql -hlocalhost -P3306 -uroot -pmysql_CA75Yk -N -e "SHOW DATABASES;" 2>/dev/null | grep tailor_is | sort

echo ""
echo "=== 7. 部署目录 ==="
ls -la /opt/tailor-is/ 2>/dev/null | head -10

echo ""
echo "=== 8. 系统资源 ==="
echo "  CPU: $(nproc) 核"
echo "  内存: $(free -h | grep Mem | awk '{print $3 " / " $2}')"
echo "  磁盘: $(df -h / | tail -1 | awk '{print $3 " / " $2 " (" $5 ")"}')"
echo "  负载: $(uptime | awk -F'load average:' '{print $2}' | head -c 50)"

echo ""
echo "================================================"
echo "检查完成"
echo "================================================"
