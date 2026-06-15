#!/bin/bash
# 阶段6 监控检查脚本

echo "=== 服务健康检查 ==="
for p in 8080 8101 8102 8103 8104 8105 8106 8107 8108 8109 8110 8111; do
    result=$(curl.exe -s --noproxy '*' --max-time 2 -o /dev/null -w "%{http_code}" "http://localhost:$p/actuator/health" 2>&1)
    echo "Port $p : HTTP $result"
done

echo ""
echo "=== Prometheus 抓取状态 ==="
curl.exe -s --noproxy '*' --max-time 5 "http://localhost:9090/api/v1/targets" 2>&1 | head -c 2000

echo ""
echo ""
echo "=== Nacos 服务注册 ==="
curl.exe -s --noproxy '*' --max-time 5 "http://localhost:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=20" 2>&1 | head -c 2000

echo ""
echo ""
echo "=== 监控服务 ==="
curl.exe -s --noproxy '*' --max-time 3 -o /dev/null -w "Prometheus 9090: HTTP %{http_code}\n" "http://localhost:9090/-/healthy"
curl.exe -s --noproxy '*' --max-time 3 -o /dev/null -w "Grafana 3000: HTTP %{http_code}\n" "http://localhost:3000/api/health"
