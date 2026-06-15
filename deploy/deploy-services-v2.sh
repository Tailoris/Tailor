#!/bin/bash
# Tailor IS 部署执行脚本 v2 - 修正数据库名

set +e
mkdir -p /tmp/tailor-is-logs
mkdir -p /tmp/tailor-is-pids

LOG_DIR=/tmp/tailor-is-logs
PID_DIR=/tmp/tailor-is-pids
JAR_DIR=/opt/tailor-is/jars

# 检查服务是否运行
is_running() {
    local name=$1
    if ps -ef | grep "tailor-is-${name}" | grep -v grep > /dev/null 2>&1; then
        return 0
    fi
    return 1
}

# 启动服务（修正：每个服务使用各自的数据库）
start_service() {
    local name=$1           # 服务名（如 user）
    local jar=$2            # JAR 文件名
    local db_name=$3        # 数据库名（如 tailor_is_user）
    local xms=${4:-512m}
    local xmx=${5:-1024m}
    local extra_args=$6

    # 检查已运行
    if is_running $name; then
        echo "[SKIP] $name 已在运行"
        return 0
    fi

    # 清理可能存在的旧 PID 文件
    rm -f $PID_DIR/${name}.pid

    echo "[START] 启动 $name (DB: $db_name) ..."
    cd $JAR_DIR

    nohup java -Xms${xms} -Xmx${xmx} \
        -Dspring.application.name=tailor-is-${name} \
        -Dspring.datasource.url="jdbc:mysql://127.0.0.1:3306/${db_name}?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true" \
        -Dspring.datasource.username=root \
        -Dspring.datasource.password=mysql_CA75Yk \
        -Dspring.data.redis.host=172.28.249.179 \
        -Dspring.data.redis.port=6379 \
        -Dspring.data.redis.password=redis_RSeR4G \
        -Dspring.cloud.nacos.discovery.server-addr=localhost:8848 \
        -Dspring.cloud.nacos.config.enabled=false \
        -Dseata.enabled=false \
        -Dspring.main.allow-circular-references=true \
        -Dspring.rabbitmq.username=rabbitmq \
        -Dspring.rabbitmq.password=rabbitmq \
        $extra_args \
        -jar ${jar} > $LOG_DIR/${name}.log 2>&1 &

    local new_pid=$!
    echo $new_pid > $PID_DIR/${name}.pid
    echo "[OK] $name 启动命令已发出 (PID: $new_pid, DB: $db_name)"
}

echo "============================================"
echo "=== Tailor IS 部署执行 v2 - $(date) ==="
echo "============================================"

# 1. Gateway（最后启动，特殊配置）
echo ""
echo "--- 1. 启动 Gateway ---"
if ! is_running gateway; then
    cd $JAR_DIR
    nohup java -Xms256m -Xmx512m \
        -Dspring.cloud.nacos.config.import-check.enabled=false \
        -Dspring.cloud.nacos.discovery.server-addr=localhost:8848 \
        -Dspring.cloud.nacos.config.server-addr=localhost:8848 \
        -Dspring.data.redis.host=localhost \
        -jar tailor-is-gateway-1.0.0.jar > $LOG_DIR/gateway.log 2>&1 &
    echo $! > $PID_DIR/gateway.pid
    echo "[OK] gateway 启动 (PID: $!)"
else
    echo "[SKIP] gateway 已在运行"
fi

# 2-8. 其他微服务（使用各自的数据库）
echo ""
echo "--- 2. 启动 User ---"
start_service "user" "tailor-is-user-1.0.0.jar" "tailor_is_user" "512m" "1024m" ""

echo ""
echo "--- 3. 启动 Merchant ---"
start_service "merchant" "tailor-is-merchant-1.0.0.jar" "tailor_is_merchant" "512m" "1024m" ""

echo ""
echo "--- 4. 启动 Marketing ---"
start_service "marketing" "tailor-is-marketing-1.0.0.jar" "tailor_is_marketing" "512m" "1024m" ""

echo ""
echo "--- 5. 启动 Copyright ---"
start_service "copyright" "tailor-is-copyright-1.0.0.jar" "tailor_is_copyright" "1024m" "2048m" ""

echo ""
echo "--- 6. 启动 Community ---"
start_service "community" "tailor-is-community-1.0.0.jar" "tailor_is_community" "512m" "1024m" ""

echo ""
echo "--- 7. 启动 Supply ---"
start_service "supply" "tailor-is-supply-1.0.0.jar" "tailor_is_supply" "512m" "1024m" ""

echo ""
echo "--- 8. 启动 AI ---"
start_service "ai" "tailor-is-ai-1.0.0.jar" "tailor_is_ai" "512m" "1024m" ""

echo ""
echo "============================================"
echo "=== 等待 60 秒让服务初始化 ==="
echo "============================================"
sleep 60

echo ""
echo "=== 启动结果汇总 ==="
ps -ef | grep -E 'tailor-is' | grep -v grep | awk '{print $2, $NF}' | sort
