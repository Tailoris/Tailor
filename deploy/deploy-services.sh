#!/bin/bash
# Tailor IS 部署执行脚本 - 启动所有微服务

set +e
mkdir -p /tmp/tailor-is-logs
mkdir -p /tmp/tailor-is-pids

# 通用环境变量
export REDIS_HOST=172.18.0.5
export REDIS_PORT=6379
export REDIS_PASSWORD=redis_RSeR4G
export RABBITMQ_HOST=172.18.0.4
export RABBITMQ_PORT=5672
export RABBITMQ_USERNAME=rabbitmq
export RABBITMQ_PASSWORD=rabbitmq
export NACOS_ADDR=localhost:8848
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=mysql_CA75Yk

LOG_DIR=/tmp/tailor-is-logs
PID_DIR=/tmp/tailor-is-pids
JAR_DIR=/opt/tailor-is/jars

start_service() {
    local name=$1
    local jar=$2
    local xms=${3:-512m}
    local xmx=${4:-1024m}
    local extra_args=$5

    if [ -z "$name" ]; then
        echo "[ERROR] 服务名不能为空"
        return 1
    fi

    # 检查是否已运行
    local pid_file=$PID_DIR/$name.pid
    if [ -f $pid_file ]; then
        local old_pid=$(cat $pid_file 2>/dev/null)
        if [ -n "$old_pid" ] && kill -0 $old_pid 2>/dev/null; then
            echo "[SKIP] $name 已在运行 (PID: $old_pid)"
            return 0
        fi
    fi

    # 检查进程
    if ps -ef | grep "tailor-is-$name" | grep -v grep | grep -v "stop-services" > /dev/null 2>&1; then
        echo "[SKIP] $name 进程已存在"
        return 0
    fi

    echo "[START] 启动 $name ..."
    cd $JAR_DIR

    nohup java -Xms${xms} -Xmx${xmx} \
        -Dspring.application.name=tailor-is-${name} \
        -Dspring.datasource.url="jdbc:mysql://localhost:3306/tailor_is?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true" \
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
        $extra_args \
        -jar ${jar} > $LOG_DIR/${name}.log 2>&1 &

    local new_pid=$!
    echo $new_pid > $pid_file
    echo "[OK] $name 启动命令已发出 (PID: $new_pid)"
}

echo "============================================"
echo "=== Tailor IS 部署执行开始 - $(date) ==="
echo "============================================"

# 1. Gateway（最后启动，因为它是流量入口）
echo ""
echo "--- 1. 启动 Gateway ---"
start_service "gateway" "tailor-is-gateway-1.0.0.jar" "256m" "512m" \
    "-Dspring.cloud.nacos.config.import-check.enabled=false -Dspring.cloud.nacos.discovery.server-addr=localhost:8848 -Dspring.cloud.nacos.config.server-addr=localhost:8848 -Dspring.data.redis.host=localhost"

# 2. User（修复密码不一致）
echo ""
echo "--- 2. 启动 User（密码统一为 redis_RSeR4G）---"
start_service "user" "tailor-is-user-1.0.0.jar" "512m" "1024m" ""

# 3. Merchant
echo ""
echo "--- 3. 启动 Merchant ---"
start_service "merchant" "tailor-is-merchant-1.0.0.jar" "512m" "1024m" ""

# 4. Marketing
echo ""
echo "--- 4. 启动 Marketing ---"
start_service "marketing" "tailor-is-marketing-1.0.0.jar" "512m" "1024m" ""

# 5. Copyright（区块链版权，需要更多内存）
echo ""
echo "--- 5. 启动 Copyright ---"
start_service "copyright" "tailor-is-copyright-1.0.0.jar" "1024m" "2048m" ""

# 6. Community
echo ""
echo "--- 6. 启动 Community ---"
start_service "community" "tailor-is-community-1.0.0.jar" "512m" "1024m" ""

# 7. Supply
echo ""
echo "--- 7. 启动 Supply ---"
start_service "supply" "tailor-is-supply-1.0.0.jar" "512m" "1024m" ""

# 8. AI
echo ""
echo "--- 8. 启动 AI ---"
start_service "ai" "tailor-is-ai-1.0.0.jar" "512m" "1024m" ""

echo ""
echo "============================================"
echo "=== 所有服务启动命令已发出 ==="
echo "=== 等待 30 秒让服务初始化 ==="
echo "============================================"
sleep 30

echo ""
echo "=== 启动结果汇总 ==="
ps -ef | grep -E 'tailor-is' | grep -v grep | awk '{print $2, $NF}' | sort
