#!/bin/bash
exec 2>&1
echo "=== 任务 5: 滚动重启所有服务应用 actuator - $(date) ==="

JAR_DIR=/opt/tailor-is/jars
LOG_DIR=/tmp/tailor-is-logs
PID_DIR=/tmp/tailor-is-pids

# 数据库映射
declare -A DB_MAP
DB_MAP["user"]="tailor_is_user"
DB_MAP["merchant"]="tailor_is_merchant"
DB_MAP["product"]="tailor_is_product"
DB_MAP["order"]="tailor_is_order"
DB_MAP["payment"]="tailor_is_payment"
DB_MAP["marketing"]="tailor_is_marketing"
DB_MAP["ai"]="tailor_is_ai"
DB_MAP["copyright"]="tailor_is_copyright"
DB_MAP["community"]="tailor_is_community"
DB_MAP["supply"]="tailor_is_supply"
DB_MAP["message"]="tailor_is_message"

declare -A XMS
XMS["user"]="512m"
XMS["merchant"]="512m"
XMS["product"]="512m"
XMS["message"]="512m"
XMS["order"]="768m"
XMS["payment"]="512m"
XMS["copyright"]="1024m"
XMS["marketing"]="768m"
XMS["community"]="512m"
XMS["supply"]="512m"
XMS["ai"]="512m"

declare -A XMX
XMX["user"]="1024m"
XMX["merchant"]="1024m"
XMX["product"]="1024m"
XMX["message"]="1024m"
XMX["order"]="1536m"
XMX["payment"]="1024m"
XMX["copyright"]="2048m"
XMX["marketing"]="1536m"
XMX["community"]="1024m"
XMX["supply"]="1024m"
XMX["ai"]="1024m"

start_svc() {
    local name=$1
    local db_name=$2
    local xms=$3
    local xmx=$4

    if ps -ef | grep "tailor-is-${name}" | grep -v grep > /dev/null 2>&1; then
        echo "[SKIP] ${name} 已在运行"
        return 0
    fi

    rm -f $PID_DIR/${name}.pid
    cd $JAR_DIR

    echo "[START] ${name} (actuator 启用)..."

    nohup java -Xms${xms} -Xmx${xmx} \
        -Dspring.application.name=tailor-is-${name} \
        -Dspring.datasource.url="jdbc:mysql://127.0.0.1:3306/${db_name}?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true" \
        -Dspring.datasource.username=root \
        -Dspring.datasource.password=mysql_CA75Yk \
        -Dspring.data.redis.host=172.18.0.5 \
        -Dspring.data.redis.port=6379 \
        -Dspring.data.redis.password=redis_RSeR4G \
        -Dspring.cloud.nacos.discovery.server-addr=localhost:8848 \
        -Dspring.cloud.nacos.config.enabled=false \
        -Dseata.enabled=false \
        -Dspring.main.allow-circular-references=true \
        -Dspring.rabbitmq.username=rabbitmq \
        -Dspring.rabbitmq.password=rabbitmq \
        -Dmanagement.endpoints.web.exposure.include=health,info,prometheus,metrics \
        -Dmanagement.endpoint.health.show-details=always \
        -Dmanagement.metrics.tags.application=tailor-is \
        -jar tailor-is-${name}-1.0.0.jar > $LOG_DIR/${name}.log 2>&1 &

    echo $! > $PID_DIR/${name}.pid
    echo "[OK] ${name} 启动 (PID: $!)"
}

# 重启顺序：先业务基础→业务核心→业务扩展→网关
# 第一批：基础服务
echo "=== 第一批：基础服务（user, merchant, product, message）==="
for s in user merchant product message; do
    start_svc "$s" "${DB_MAP[$s]}" "${XMS[$s]}" "${XMX[$s]}"
    sleep 2
done

# 第二批：核心服务
echo ""
echo "=== 第二批：核心服务（order, payment, copyright, marketing, community, supply, ai）==="
for s in order payment copyright marketing community supply ai; do
    start_svc "$s" "${DB_MAP[$s]}" "${XMS[$s]}" "${XMX[$s]}"
    sleep 2
done

# Gateway 最后启动
echo ""
echo "=== Gateway ==="
cd $JAR_DIR
nohup java -Xms256m -Xmx512m \
    -Dspring.cloud.nacos.config.import-check.enabled=false \
    -Dspring.cloud.nacos.discovery.server-addr=localhost:8848 \
    -Dspring.data.redis.host=localhost \
    -Dmanagement.endpoints.web.exposure.include=health,info,prometheus,metrics \
    -Dmanagement.endpoint.health.show-details=always \
    -jar tailor-is-gateway-1.0.0.jar > $LOG_DIR/gateway.log 2>&1 &
echo $! > $PID_DIR/gateway.pid
echo "[OK] gateway 启动 (PID: $!)"

echo ""
echo "=== 等待 60 秒让所有服务完全启动 ==="
sleep 60

echo ""
echo "=== 启动结果验证 ==="
ps -ef | grep -E 'tailor-is' | grep -v grep | wc -l | awk '{print "运行进程数:", $0}'

echo ""
echo "=== 各服务 actuator 验证 ==="
declare -A PORT_MAP
PORT_MAP["gateway"]="8081"
PORT_MAP["user"]="8101"
PORT_MAP["merchant"]="8102"
PORT_MAP["product"]="8103"
PORT_MAP["order"]="8104"
PORT_MAP["payment"]="8105"
PORT_MAP["marketing"]="8106"
PORT_MAP["ai"]="8107"
PORT_MAP["copyright"]="8108"
PORT_MAP["community"]="8109"
PORT_MAP["supply"]="8110"
PORT_MAP["message"]="8111"

ok=0
fail=0
for s in "${!PORT_MAP[@]}"; do
    p=${PORT_MAP[$s]}
    if ps -ef | grep "tailor-is-${s}" | grep -v grep > /dev/null 2>&1; then
        if ss -tln 2>/dev/null | grep -q ":${p} "; then
            ok=$((ok+1))
            echo "  [OK] $s (:$p) 运行中"
        else
            fail=$((fail+1))
            echo "  [WARN] $s (:$p) 进程在但端口未监听"
        fi
    else
        fail=$((fail+1))
        echo "  [FAIL] $s 未运行"
    fi
done
echo ""
echo "健康: $ok / 失败: $fail"
