#!/bin/bash
exec 2>&1
echo "=== 任务 5 强制重启: 所有服务应用 actuator - $(date) ==="

echo "--- 步骤 1: 停止所有服务 ---"
for svc in gateway user merchant product order payment message marketing community copyright supply ai; do
    if pgrep -f "tailor-is-${svc}" > /dev/null 2>&1; then
        pkill -9 -f "tailor-is-${svc}" 2>/dev/null
        echo "  停止: $svc"
    fi
done
sleep 5
echo "  当前进程数: $(ps -ef | grep -c '[t]ailor-is-')"

echo ""
echo "--- 步骤 2: 清理 PID 文件 ---"
rm -f /tmp/tailor-is-pids/*.pid
ls /tmp/tailor-is-pids/ 2>&1

echo ""
echo "--- 步骤 3: 启动所有服务（带 actuator）==="
# 启动脚本 - 与 task5-restart-all 相同，但确保不跳过
JAR_DIR=/opt/tailor-is/jars
LOG_DIR=/tmp/tailor-is-logs
PID_DIR=/tmp/tailor-is-pids

declare -A SERVICES
SERVICES["user"]="tailor_is_user:512m:1024m"
SERVICES["merchant"]="tailor_is_merchant:512m:1024m"
SERVICES["product"]="tailor_is_product:512m:1024m"
SERVICES["message"]="tailor_is_message:512m:1024m"
SERVICES["order"]="tailor_is_order:768m:1536m"
SERVICES["payment"]="tailor_is_payment:512m:1024m"
SERVICES["copyright"]="tailor_is_copyright:1024m:2048m"
SERVICES["marketing"]="tailor_is_marketing:768m:1536m"
SERVICES["community"]="tailor_is_community:512m:1024m"
SERVICES["supply"]="tailor_is_supply:512m:1024m"
SERVICES["ai"]="tailor_is_ai:512m:1024m"

# 第一批：基础服务
echo "=== 第一批：基础服务 ==="
for s in user merchant product message; do
    IFS=':' read -r db xms xmx <<< "${SERVICES[$s]}"
    cd $JAR_DIR
    nohup java -Xms${xms} -Xmx${xmx} \
        -Dspring.application.name=tailor-is-${s} \
        -Dspring.datasource.url="jdbc:mysql://127.0.0.1:3306/${db}?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true" \
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
        -jar tailor-is-${s}-1.0.0.jar > $LOG_DIR/${s}.log 2>&1 &
    pid=$!
    echo $pid > $PID_DIR/${s}.pid
    echo "  [OK] $s 启动 (PID: $pid)"
    sleep 1
done

# 第二批：核心服务
echo ""
echo "=== 第二批：核心服务 ==="
for s in order payment copyright marketing community supply ai; do
    IFS=':' read -r db xms xmx <<< "${SERVICES[$s]}"
    cd $JAR_DIR
    nohup java -Xms${xms} -Xmx${xmx} \
        -Dspring.application.name=tailor-is-${s} \
        -Dspring.datasource.url="jdbc:mysql://127.0.0.1:3306/${db}?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true" \
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
        -jar tailor-is-${s}-1.0.0.jar > $LOG_DIR/${s}.log 2>&1 &
    pid=$!
    echo $pid > $PID_DIR/${s}.pid
    echo "  [OK] $s 启动 (PID: $pid)"
    sleep 1
done

echo ""
echo "=== Gateway 最后启动 ==="
cd $JAR_DIR
nohup java -Xms256m -Xmx512m \
    -Dspring.cloud.nacos.config.import-check.enabled=false \
    -Dspring.cloud.nacos.discovery.server-addr=localhost:8848 \
    -Dspring.data.redis.host=localhost \
    -Dmanagement.endpoints.web.exposure.include=health,info,prometheus,metrics \
    -Dmanagement.endpoint.health.show-details=always \
    -jar tailor-is-gateway-1.0.0.jar > $LOG_DIR/gateway.log 2>&1 &
echo $! > $PID_DIR/gateway.pid
echo "  [OK] gateway 启动 (PID: $!)"

echo ""
echo "=== 等待 80 秒 ==="
sleep 80

echo ""
echo "=== 启动结果 ==="
running=$(ps -ef | grep -c '[t]ailor-is-')
echo "运行进程数: $running / 12"

echo ""
echo "=== 端口监听验证 ==="
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
for s in "${!PORT_MAP[@]}"; do
    p=${PORT_MAP[$s]}
    if ss -tln 2>/dev/null | grep -q ":${p} "; then
        ok=$((ok+1))
        echo "  [OK] $s (:$p)"
    else
        echo "  [FAIL] $s (:$p)"
    fi
done
echo ""
echo "健康: $ok / 12"
