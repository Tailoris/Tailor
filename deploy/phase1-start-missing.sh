#!/bin/bash
exec 2>&1
echo "=== P0 阶段 1: 启动缺失的 7 个服务 - $(date) ==="

# 检查已运行服务
echo ""
echo "--- 步骤 0: 检查当前运行状态 ---"
declare -A PORT_MAP
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
PORT_MAP["gateway"]="8080"

declare -A DB_MAP
DB_MAP["user"]="tailor_is_user:512m:1024m"
DB_MAP["merchant"]="tailor_is_merchant:512m:1024m"
DB_MAP["product"]="tailor_is_product:512m:1024m"
DB_MAP["order"]="tailor_is_order:768m:1536m"
DB_MAP["payment"]="tailor_is_payment:512m:1024m"
DB_MAP["marketing"]="tailor_is_marketing:768m:1536m"
DB_MAP["ai"]="tailor_is_ai:512m:1024m"
DB_MAP["copyright"]="tailor_is_copyright:1024m:2048m"
DB_MAP["community"]="tailor_is_community:512m:1024m"
DB_MAP["supply"]="tailor_is_supply:512m:1024m"
DB_MAP["message"]="tailor_is_message:512m:1024m"

running_count=0
for s in "${!PORT_MAP[@]}"; do
    p=${PORT_MAP[$s]}
    if ss -tln 2>/dev/null | grep -q ":${p} "; then
        running_count=$((running_count+1))
    fi
done
echo "  已运行服务: $running_count / 12"

# 需要启动的服务
missing_services=()
for s in user merchant marketing copyright community supply ai; do
    p=${PORT_MAP[$s]}
    if ! ss -tln 2>/dev/null | grep -q ":${p} "; then
        missing_services+=("$s")
    fi
done

echo "  缺失服务: ${missing_services[@]}"

if [ ${#missing_services[@]} -eq 0 ]; then
    echo "  所有服务已运行，退出"
    exit 0
fi

# 启动缺失服务
echo ""
echo "--- 步骤 1: 启动缺失服务 ---"
JAR_DIR=/opt/tailor-is/jars
LOG_DIR=/tmp/tailor-is-logs
PID_DIR=/tmp/tailor-is-pids

mkdir -p $LOG_DIR $PID_DIR

for s in "${missing_services[@]}"; do
    IFS=':' read -r db xms xmx <<< "${DB_MAP[$s]}"
    echo ""
    echo "  启动: $s (DB: $db, Heap: ${xms}~${xmx})"
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
    echo "    [OK] PID: $pid"
    sleep 2
done

echo ""
echo "--- 步骤 2: 等待 60 秒让服务完成启动 ---"
sleep 60

echo ""
echo "--- 步骤 3: 验证启动结果 ---"
ok=0
fail=0
for s in "${missing_services[@]}"; do
    p=${PORT_MAP[$s]}
    if ss -tln 2>/dev/null | grep -q ":${p} "; then
        ok=$((ok+1))
        echo "  [OK] $s (:$p)"

        # 健康检查
        health=$(curl -s --noproxy '*' --max-time 3 "http://localhost:${p}/actuator/health" 2>&1)
        if echo "$health" | grep -q '"status":"UP"'; then
            echo "    健康: UP"
        else
            echo "    健康: 未知 - $health" | head -c 200
        fi
    else
        fail=$((fail+1))
        echo "  [FAIL] $s (:$p) 未启动"
        if [ -f "$LOG_DIR/${s}.log" ]; then
            echo "    最后日志:"
            tail -5 "$LOG_DIR/${s}.log" | sed 's/^/      /'
        fi
    fi
done

echo ""
echo "=== 启动结果: 成功 $ok, 失败 $fail ==="

# 总计
total_running=0
for s in "${!PORT_MAP[@]}"; do
    p=${PORT_MAP[$s]}
    if ss -tln 2>/dev/null | grep -q ":${p} "; then
        total_running=$((total_running+1))
    fi
done
echo "=== 总计运行: $total_running / 12 (含 gateway) ==="
