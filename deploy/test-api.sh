#!/bin/bash
exec 2>&1
echo "=== Gateway 路由测试 - $(date '+%H:%M:%S') ==="
echo ""

echo "[测试1] Gateway 根路径"
curl -s --max-time 5 -w "  HTTP: %{http_code}\n" http://localhost:8081/ 2>&1 | head -5

echo ""
echo "[测试2] 通过 Gateway 调用各服务 (默认 401 是预期，需鉴权)"
for path in "/api/v1/user/test" "/api/v1/product/list" "/api/v1/order/test" "/tailor-is-user/actuator/health" "/tailor-is-product/actuator/health"; do
    code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "http://localhost:8081${path}" 2>/dev/null)
    echo "  $path: HTTP $code"
done

echo ""
echo "[测试3] 直接访问各服务 actuator (内部接口)"
for port_name in "8081:gateway" "8101:user" "8103:product" "8104:order" "8105:payment" "8106:marketing" "8107:ai" "8108:copyright" "8109:community" "8110:supply" "8111:message" "8102:merchant"; do
    port=$(echo $port_name | cut -d: -f1)
    name=$(echo $port_name | cut -d: -f2)
    code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 "http://localhost:${port}/actuator/health" 2>/dev/null)
    rt=$(curl -s -o /dev/null -w "%{time_total}" --max-time 3 "http://localhost:${port}/actuator/health" 2>/dev/null)
    rt_ms=$(echo "$rt * 1000" | bc 2>/dev/null | cut -d. -f1)
    echo "  $name (:$port) HTTP=$code RT=${rt_ms}ms"
done

echo ""
echo "[测试4] 数据库表统计"
for db in tailor_is_user tailor_is_product tailor_is_order tailor_is_payment tailor_is_message tailor_is_merchant tailor_is_marketing tailor_is_copyright tailor_is_community tailor_is_supply tailor_is_ai; do
    cnt=$(mysql -h127.0.0.1 -P3306 -uroot -pmysql_CA75Yk -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$db';" 2>/dev/null)
    echo "  $db: $cnt 张表"
done
