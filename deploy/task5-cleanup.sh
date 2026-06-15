#!/bin/bash
exec 2>&1
echo "=== 任务 5 最终修复: 清理并重启残留服务 - $(date) ==="

echo "--- 步骤 1: 强制清理所有 java 进程 ---"
ps -ef | grep -E "tailor-is-" | grep -v grep
echo ""
echo "  当前 Java 进程:"
java_pids=$(ps -ef | grep -E "tailor-is-" | grep -v grep | awk '{print $2}')
echo "$java_pids"

# 强制 kill
for pid in $java_pids; do
    kill -9 $pid 2>/dev/null && echo "  KILLED: $pid"
done
sleep 5

echo ""
echo "  清理后进程数: $(ps -ef | grep -c '[t]ailor-is-')"

# 清理 PID 文件
rm -f /tmp/tailor-is-pids/*.pid

echo ""
echo "--- 步骤 2: 启动所有服务（带 actuator）==="
JAR_DIR=/opt/tailor-is/jars
LOG_DIR=/tmp/tailor-is-logs
PID_DIR=/tmp/tailor-is-pids

# 完整启动顺序
start_svc() {
    local s=$1
    local db=$2
    local xms=$3
    local xmx=$4

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
    local pid=$!
    echo $pid > $PID_DIR/${s}.pid
    echo "  [OK] $s 启动 (PID: $pid)"
    sleep 1
}

# 基础服务
start_svc "user" "tailor_is_user" "512m" "1024m"
start_svc "merchant" "tailor_is_merchant" "512m" "1024m"
start_svc "product" "tailor_is_product" "512m" "1024m"
start_svc "message" "tailor_is_message" "512m" "1024m"

# 核心服务
start_svc "order" "tailor_is_order" "768m" "1536m"
start_svc "payment" "tailor_is_payment" "512m" "1024m"
start_svc "copyright" "tailor_is_copyright" "1024m" "2048m"
start_svc "marketing" "tailor_is_marketing" "768m" "1536m"
start_svc "community" "tailor_is_community" "512m" "1024m"
start_svc "supply" "tailor_is_supply" "512m" "1024m"
start_svc "ai" "tailor_is_ai" "512m" "1024m"

# Gateway
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
echo "=== 验证所有服务的 /actuator/prometheus ==="
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
    code=$(curl -s -o /dev/null -w "%{http_code}" --noproxy '*' --max-time 5 "http://localhost:${p}/actuator/prometheus" 2>/dev/null)
    if [ "$code" = "200" ]; then
        ok=$((ok+1))
        echo "  [OK] $s (:$p) prometheus 200"
    else
        echo "  [FAIL] $s (:$p) prometheus $code"
    fi
done
echo ""
echo "成功: $ok / 12"
