#!/bin/bash
echo "=== 部署验证报告 - $(date) ==="
echo ""

echo "### L1: 基础设施 ###"
mysql -h127.0.0.1 -P3306 -uroot -pmysql_CA75Yk -e "SELECT VERSION();" 2>/dev/null | tail -1 | xargs -I{} echo "  MySQL: {}"
redis-cli -h 127.0.0.1 -p 6379 -a redis_RSeR4G PING 2>/dev/null | head -1 | xargs -I{} echo "  Redis: {}"
echo "  Nacos: HTTP $(curl -s -o /dev/null -w '%{http_code}' http://localhost:8848/nacos/)"
echo ""

echo "### L2: 微服务健康 ###"
declare -A services=(["gateway"]="8081" ["user"]="8101" ["merchant"]="8102" ["product"]="8103" ["order"]="8104" ["payment"]="8105" ["marketing"]="8106" ["ai"]="8107" ["copyright"]="8108" ["community"]="8109" ["supply"]="8110" ["message"]="8111")
healthy=0
total=${#services[@]}
for svc in "${!services[@]}"; do
    port=${services[$svc]}
    if ps -ef | grep "tailor-is-${svc}" | grep -v grep > /dev/null 2>&1; then
        if ss -tln 2>/dev/null | grep -q ":${port} "; then
            echo "  [OK] $svc (port $port)"
            healthy=$((healthy+1))
        else
            echo "  [WARN] $svc (port $port 未监听)"
        fi
    else
        echo "  [FAIL] $svc 未运行"
    fi
done
echo "  健康: $healthy / $total"
echo ""

echo "### L3: Nacos 注册 ###"
for svc in tailor-is-gateway tailor-is-user tailor-is-product tailor-is-order tailor-is-payment tailor-is-message tailor-is-merchant tailor-is-marketing tailor-is-copyright tailor-is-community tailor-is-supply tailor-is-ai; do
    inst=$(curl -s -X GET "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=${svc}" 2>/dev/null | grep -oE '"ip":"[^"]*"' | head -1)
    echo "  $svc: $inst"
done
echo ""

echo "### L4: 性能 ###"
for port_name in "8081:gateway" "8101:user" "8103:product" "8104:order"; do
    port=$(echo $port_name | cut -d: -f1)
    name=$(echo $port_name | cut -d: -f2)
    rt=$(curl -s -o /dev/null -w "%{time_total}" --max-time 3 "http://localhost:${port}/actuator/health" 2>/dev/null)
    if [ -n "$rt" ]; then
        rt_ms=$(echo "$rt * 1000" | bc 2>/dev/null | cut -d. -f1)
        echo "  $name: ${rt_ms}ms"
    fi
done
echo ""
echo "=== 完成 ==="
