#!/bin/bash
# 等待服务启动并验证

echo "=== 等待 30 秒让服务完全启动 ==="
sleep 30

echo ""
echo "=== 当前运行进程 ==="
ps -ef | grep tailor-is | grep -v grep | awk '{print $2, $NF}' | sort

echo ""
echo "=== 各服务监听端口 ==="
ss -tln 2>/dev/null | grep -E ':(8081|8103|8104|8105|8111)' | sort -k4

echo ""
echo "=== Gateway 健康检查 ==="
curl -s --max-time 5 http://localhost:8081/actuator/health 2>&1 | head -3

echo ""
echo "=== 各服务最近启动日志（最后 5 行）==="
for svc in gateway user merchant marketing copyright community supply ai product order payment message; do
    log="/tmp/tailor-is-logs/${svc}.log"
    if [ -f "$log" ]; then
        echo "--- $svc ---"
        grep -E "Started.*Application|Tomcat started|register finished|Started.*in " "$log" 2>/dev/null | tail -3
    fi
done

echo ""
echo "=== Nacos 服务列表 ==="
curl -s -X GET 'http://localhost:8848/nacos/v1/ns/catalog/services?pageNo=1&pageSize=50' 2>&1 | head -200
