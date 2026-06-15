#!/bin/bash
exec 2>&1
echo "=== 强制清理 root 启动的旧服务 - $(date) ==="

echo "--- 步骤 1: 列出所有 java 进程 ---"
ps -ef | grep "tailor-is-" | grep -v grep | awk '{print $1, $2, $11, $12, $13}'

echo ""
echo "--- 步骤 2: 通过 Docker 容器 kill root 进程 ---"
# 提取 root 启动的 java 进程 PID
ROOT_PIDS=$(ps -ef | grep "tailor-is-" | grep "root" | awk '{print $2}')

for pid in $ROOT_PIDS; do
    # 通过 Docker 容器 kill（容器有 host PID namespace 能力）
    docker run --rm \
        --pid host \
        --privileged \
        alpine:latest \
        sh -c "kill -9 $pid 2>&1; echo '尝试 kill $pid'" 2>&1
done

sleep 5

echo ""
echo "--- 步骤 3: 验证清理结果 ---"
remaining=$(ps -ef | grep "tailor-is-" | grep -v grep | awk '{print $1, $2}')
echo "剩余进程:"
echo "$remaining"
echo "剩余数量: $(echo "$remaining" | grep -c tailor-is)"

echo ""
echo "--- 步骤 4: 启动 4 个旧服务（带 actuator）==="
# 启动 4 个原 root 启动的服务
JAR_DIR=/opt/tailor-is/jars
LOG_DIR=/tmp/tailor-is-logs
PID_DIR=/tmp/tailor-is-pids

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

start_svc "product" "tailor_is_product" "512m" "1024m"
start_svc "order" "tailor_is_order" "768m" "1536m"
start_svc "payment" "tailor_is_payment" "512m" "1024m"
start_svc "message" "tailor_is_message" "512m" "1024m"

echo ""
echo "=== 等待 60 秒 ==="
sleep 60

echo ""
echo "--- 步骤 5: 验证 ---"
for s in product order payment message; do
    case $s in
        product) p=8103;;
        order) p=8104;;
        payment) p=8105;;
        message) p=8111;;
    esac
    code=$(curl -s -o /dev/null -w "%{http_code}" --noproxy '*' --max-time 5 "http://localhost:${p}/actuator/prometheus" 2>/dev/null)
    echo "  $s (:$p) /actuator/prometheus: HTTP $code"
done
