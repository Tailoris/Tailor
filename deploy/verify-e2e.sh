#!/bin/bash
# 端到端部署验证

echo "==========================================="
echo "=== Tailor IS 部署验证报告 ==="
echo "=== $(date '+%Y-%m-%d %H:%M:%S') ==="
echo "==========================================="

echo ""
echo "### L1: 基础设施验证 ###"

# MySQL
mysql_check=$(mysql -h127.0.0.1 -P3306 -uroot -pmysql_CA75Yk -e "SELECT VERSION();" 2>&1 | tail -1)
if [[ "$mysql_check" == 5.* ]] || [[ "$mysql_check" == 8.* ]]; then
    echo "  [OK] MySQL: $mysql_check"
else
    echo "  [FAIL] MySQL: $mysql_check"
fi

# MySQL 数据库列表
db_count=$(mysql -h127.0.0.1 -P3306 -uroot -pmysql_CA75Yk -N -e "SHOW DATABASES;" 2>/dev/null | grep -c "tailor_is_")
echo "  [INFO] Tailor IS 数据库数量: $db_count / 10"
mysql -h127.0.0.1 -P3306 -uroot -pmysql_CA75Yk -N -e "SHOW DATABASES;" 2>/dev/null | grep "tailor_is_" | sort | sed 's/^/    - /'

# Redis
redis_ping=$(redis-cli -h 127.0.0.1 -p 6379 -a redis_RSeR4G PING 2>/dev/null)
if echo "$redis_ping" | grep -q PONG; then
    echo "  [OK] Redis: PONG"
else
    echo "  [FAIL] Redis: $redis_ping"
fi

# Nacos
nacos_code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 http://localhost:8848/nacos/ 2>/dev/null)
if [ "$nacos_code" = "200" ] || [ "$nacos_code" = "302" ]; then
    echo "  [OK] Nacos: HTTP $nacos_code"
else
    echo "  [FAIL] Nacos: HTTP $nacos_code"
fi

# RabbitMQ
rmq_check=$(curl -s -u rabbitmq:rabbitmq --max-time 5 http://localhost:15672/api/overview 2>/dev/null | grep -o 'rabbitmq_version[^,]*' | head -1)
if [ -n "$rmq_check" ]; then
    echo "  [OK] RabbitMQ: 正常"
else
    echo "  [FAIL] RabbitMQ: 无响应"
fi

echo ""
echo "### L2: 微服务健康验证 ###"
declare -A services=(
    ["gateway"]="8081"
    ["user"]="8101"
    ["merchant"]="8102"
    ["product"]="8103"
    ["order"]="8104"
    ["payment"]="8105"
    ["marketing"]="8106"
    ["ai"]="8107"
    ["copyright"]="8108"
    ["community"]="8109"
    ["supply"]="8110"
    ["message"]="8111"
)

healthy_count=0
total_count=${#services[@]}

for svc in "${!services[@]}"; do
    port=${services[$svc]}
    # 检查进程
    proc_count=$(ps -ef | grep "tailor-is-${svc}" | grep -v grep | wc -l)
    if [ "$proc_count" -gt 0 ]; then
        # 检查端口
        port_listen=$(ss -tln 2>/dev/null | grep -c ":${port} ")
        if [ "$port_listen" -gt 0 ]; then
            echo "  [OK] tailor-is-$svc (PID 进程存在, 端口 $port 监听)"
            healthy_count=$((healthy_count + 1))
        else
            echo "  [WARN] tailor-is-$svc (PID 存在, 端口 $port 未监听)"
        fi
    else
        echo "  [FAIL] tailor-is-$svc (进程未运行)"
    fi
done

echo ""
echo "  健康服务: $healthy_count / $total_count"

echo ""
echo "### L3: Nacos 服务注册验证 ###"
nacos_services=$(curl -s -X GET 'http://localhost:8848/nacos/v1/ns/catalog/services?pageNo=1&pageSize=50' 2>/dev/null)
service_count=$(echo "$nacos_services" | grep -oE '"name":"[^"]*"' | wc -l)
echo "  Nacos 已注册服务: $service_count"
if [ "$service_count" -gt 0 ]; then
    echo "$nacos_services" | grep -oE '"name":"[^"]*"' | sort | uniq | sed 's/^/    - /'
fi

echo ""
echo "### L4: 业务流程接口测试 ###"
# 1Panel 1panel-account
echo "  -- API 测试 --"

# 测试通过 Gateway 访问 (端口 8081)
api_test=$(curl -s --max-time 5 -o /dev/null -w "%{http_code}" http://localhost:8081/ 2>/dev/null)
echo "  Gateway (8081): HTTP $api_test"

# 通过 Nacos 的 API 拉取服务实例
for svc in tailor-is-user tailor-is-product tailor-is-gateway; do
    instances=$(curl -s -X GET "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=${svc}" 2>/dev/null | grep -oE '"ip":"[^"]*"' | head -1)
    echo "  $svc: $instances"
done

echo ""
echo "### L5: 性能基线测试（响应时间）###"
# 测试各服务响应时间
for port_name in "8081:gateway" "8101:user" "8103:product" "8104:order"; do
    port=$(echo $port_name | cut -d: -f1)
    name=$(echo $port_name | cut -d: -f2)
    rt=$(curl -s -o /dev/null -w "%{time_total}" --max-time 5 "http://localhost:${port}/actuator/health" 2>/dev/null)
    if [ -n "$rt" ] && [ "$rt" != "0.000000" ]; then
        rt_ms=$(echo "$rt * 1000" | bc 2>/dev/null | cut -d. -f1)
        echo "  $name 响应时间: ${rt_ms}ms"
    fi
done

echo ""
echo "==========================================="
echo "### 部署总结 ###"
echo "==========================================="
echo "总微服务数: $total_count"
echo "健康运行数: $healthy_count"
echo "运行率: $(echo "scale=1; $healthy_count * 100 / $total_count" | bc)%"
echo ""
echo "PIDS:"
ps -ef | grep tailor-is | grep -v grep | awk '{print "  " $2 " " $NF}' | sort

echo ""
echo "=== 验证完成 - $(date '+%Y-%m-%d %H:%M:%S') ==="
