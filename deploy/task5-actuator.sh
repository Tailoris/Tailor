#!/bin/bash
exec 2>&1
echo "=== 任务 5: actuator 端点配置 - $(date) ==="

# 测试 1: 先检查一个服务的 actuator 端点
echo "--- 步骤 1: 检查当前 actuator 配置 ---"
echo "  /actuator 端点测试:"
for port_name in "8081:gateway" "8101:user" "8103:product"; do
    port=$(echo $port_name | cut -d: -f1)
    name=$(echo $port_name | cut -d: -f2)
    code=$(curl -s -o /dev/null -w "%{http_code}" --noproxy '*' --max-time 3 "http://localhost:${port}/actuator" 2>/dev/null)
    echo "  $name (:$port) /actuator: HTTP $code"
done

echo ""
echo "--- 步骤 2: 提取并查看 application.yml ---"
mkdir -p /tmp/cfg-check
cd /tmp/cfg-check
# 检查 user 服务
unzip -o /opt/tailor-is/jars/tailor-is-user-1.0.0.jar 'BOOT-INF/classes/application*' 2>&1 | tail -3
ls BOOT-INF/classes/ 2>&1 | head -5
echo ""
echo "--- application.yml 中 actuator 配置 ---"
grep -A5 -B1 -i "management\|actuator" BOOT-INF/classes/application.yml 2>/dev/null | head -30

echo ""
echo "--- 步骤 3: 准备 actuator 启用启动脚本 ---"
# 准备新的启动脚本，添加 actuator 配置
mkdir -p /opt/tailor-is/scripts-actuator
cp -r /opt/tailor-is/scripts/. /opt/tailor-is/scripts-actuator/ 2>/dev/null

# 创建新启动脚本（添加 actuator 暴露）
cat > /tmp/start-with-actuator.sh << 'ACTUATORSCRIPT'
#!/bin/bash
# Tailor IS 服务启动脚本 - 启用 actuator 健康端点
# 用于 Prometheus 监控指标抓取

set +e
mkdir -p /tmp/tailor-is-logs
mkdir -p /tmp/tailor-is-pids

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

start_svc() {
    local name=$1
    local db_name=$2
    local xms=${3:-512m}
    local xmx=${4:-1024m}

    if ps -ef | grep "tailor-is-${name}" | grep -v grep > /dev/null 2>&1; then
        echo "[SKIP] ${name} 已在运行"
        return 0
    fi

    rm -f $PID_DIR/${name}.pid
    cd $JAR_DIR

    echo "[START] ${name} (DB: ${db_name}, actuator 启用)..."

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
        -Dmanagement.metrics.tags.application=${name} \
        -jar tailor-is-${name}-1.0.0.jar > $LOG_DIR/${name}.log 2>&1 &

    local pid=$!
    echo $pid > $PID_DIR/${name}.pid
    echo "[OK] ${name} 启动 (PID: ${pid})"
}

# 基础服务
start_svc "user" "tailor_is_user" 512m 1024m
start_svc "merchant" "tailor_is_merchant" 512m 1024m
start_svc "product" "tailor_is_product" 512m 1024m
start_svc "message" "tailor_is_message" 512m 1024m

# 核心服务
start_svc "order" "tailor_is_order" 768m 1536m
start_svc "payment" "tailor_is_payment" 512m 1024m
start_svc "copyright" "tailor_is_copyright" 1024m 2048m
start_svc "marketing" "tailor_is_marketing" 768m 1536m
start_svc "community" "tailor_is_community" 512m 1024m
start_svc "supply" "tailor_is_supply" 512m 1024m
start_svc "ai" "tailor_is_ai" 512m 1024m

# Gateway
sleep 5
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

echo "=== 启动完成 ==="
ACTUATORSCRIPT

# 通过 Docker 复制到 /opt/tailor-is/scripts/
docker run --rm \
    -v /tmp:/src:ro \
    -v /opt/tailor-is/scripts:/dest \
    alpine:latest sh -c "
        cp /src/start-with-actuator.sh /dest/start-with-actuator.sh
        chmod +x /dest/start-with-actuator.sh
        ls -la /dest/start-with-actuator.sh
    " 2>&1

echo ""
echo "--- 步骤 4: 测试性重启 user 服务验证 actuator ---"
echo "  停止 user 服务..."
pkill -f "tailor-is-user-1.0.0" 2>/dev/null
sleep 3

# 启动 user 服务
echo "  启动 user 服务（带 actuator）..."
cd /opt/tailor-is/jars
nohup java -Xms512m -Xmx1024m \
    -Dspring.application.name=tailor-is-user \
    -Dspring.datasource.url="jdbc:mysql://127.0.0.1:3306/tailor_is_user?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true" \
    -Dspring.datasource.username=root \
    -Dspring.datasource.password=mysql_CA75Yk \
    -Dspring.data.redis.host=172.18.0.5 \
    -Dspring.data.redis.port=6379 \
    -Dspring.data.redis.password=redis_RSeR4G \
    -Dspring.cloud.nacos.discovery.server-addr=localhost:8848 \
    -Dspring.cloud.nacos.config.enabled=false \
    -Dseata.enabled=false \
    -Dspring.main.allow-circular-references=true \
    -Dmanagement.endpoints.web.exposure.include=health,info,prometheus,metrics \
    -Dmanagement.endpoint.health.show-details=always \
    -Dmanagement.metrics.tags.application=user \
    -jar tailor-is-user-1.0.0.jar > /tmp/tailor-is-logs/user.log 2>&1 &
echo $! > /tmp/tailor-is-pids/user.pid
echo "  user 启动 (PID: $!)"

echo ""
echo "--- 步骤 5: 等待服务启动并验证 ---"
sleep 50

echo "  验证 user 服务的 actuator 端点:"
echo "  /actuator/health:"
code=$(curl -s -o /dev/null -w "%{http_code}" --noproxy '*' --max-time 5 "http://localhost:8101/actuator/health" 2>/dev/null)
echo "    HTTP $code"
if [ "$code" = "200" ]; then
    body=$(curl -s --noproxy '*' --max-time 5 "http://localhost:8101/actuator/health" 2>/dev/null)
    echo "    Body: $body" | head -1
fi

echo ""
echo "  /actuator/prometheus:"
code=$(curl -s -o /dev/null -w "%{http_code}" --noproxy '*' --max-time 5 "http://localhost:8101/actuator/prometheus" 2>/dev/null)
echo "    HTTP $code"
if [ "$code" = "200" ]; then
    metrics=$(curl -s --noproxy '*' --max-time 5 "http://localhost:8101/actuator/prometheus" 2>/dev/null | head -5)
    echo "    前 5 行指标:"
    echo "$metrics" | head -5 | sed 's/^/      /'
fi

echo ""
echo "--- 步骤 6: 验证 Prometheus 抓取 user 服务 ---"
sleep 15
target_state=$(curl -s --noproxy '*' "http://localhost:9090/api/v1/targets?search=tailor-is-user" 2>/dev/null | head -c 500)
echo "  Prometheus 抓取状态: $target_state" | head -1

echo ""
echo "=== 任务 5 测试完成 ==="
echo ""
echo "下一步: 滚动重启所有服务以应用 actuator 配置"
echo "命令: bash /opt/tailor-is/scripts/start-with-actuator.sh"
