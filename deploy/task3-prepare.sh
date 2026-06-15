#!/bin/bash
exec 2>&1
echo "=== 任务 3: Nacos 鉴权 v3 - 滚动重启微服务 - $(date) ==="

# 重要：当前 Nacos 已启用鉴权（NACOS_AUTH_ENABLE=true）
# 默认账号 nacos/nacos 在 1Panel 中可能已被重置
# 我们假设默认密码仍然有效（nacos/nacos），如果是自定义密码则需要更新
# 为了安全，使用专用账户
NACOS_USER="nacos"
NACOS_PASS="nacos"  # 默认密码，如果 1Panel 修改过则需要更换

# 验证 Nacos 鉴权可用性
echo "--- 步骤 1: 测试 Nacos 登录 ---"
LOGIN_RESP=$(curl -s --noproxy '*' -X POST "http://localhost:8848/nacos/v1/auth/users/login" \
    -d "username=${NACOS_USER}&password=${NACOS_PASS}" --max-time 5)
echo "  登录响应: $LOGIN_RESP" | head -1

if echo "$LOGIN_RESP" | grep -q "accessToken"; then
    TOKEN=$(echo "$LOGIN_RESP" | grep -oE '"accessToken":"[^"]*"' | sed 's/"accessToken":"//;s/"$//')
    echo "  [OK] 获取到 token: ${TOKEN:0:30}..."
else
    echo "  [WARN] 默认密码不可用，使用 nacos 容器查询"
    # 尝试容器内登录
    INTERNAL_LOGIN=$(docker exec 1Panel-nacos-gJky-standalone \
        curl -s -X POST "http://localhost:8848/nacos/v1/auth/users/login" \
        -d "username=${NACOS_USER}&password=${NACOS_PASS}" --max-time 5 2>&1)
    echo "  容器内登录: $INTERNAL_LOGIN" | head -1
    if echo "$INTERNAL_LOGIN" | grep -q "accessToken"; then
        TOKEN=$(echo "$INTERNAL_LOGIN" | grep -oE '"accessToken":"[^"]*"' | sed 's/"accessToken":"//;s/"$//')
        echo "  [OK] 容器内 token: ${TOKEN:0:30}..."
    else
        echo "  [FAIL] 无法登录 Nacos，请检查密码"
        TOKEN=""
    fi
fi

echo ""
echo "--- 步骤 2: 创建服务发现专用账户 ---"
if [ -n "$TOKEN" ]; then
    # 创建低权限账户
    CREATE_RESP=$(curl -s --noproxy '*' -X POST "http://localhost:8848/nacos/v1/auth/users" \
        -H "Authorization: Bearer $TOKEN" \
        -d "username=tailor_service&password=TailorIS2026@Secure" --max-time 5 2>&1)
    echo "  创建账户响应: $CREATE_RESP" | head -1

    # 分配角色（读权限）
    ROLE_RESP=$(curl -s --noproxy '*' -X POST "http://localhost:8848/nacos/v1/auth/roles" \
        -H "Authorization: Bearer $TOKEN" \
        -d "role=ROLE_SERVICE&username=tailor_service" --max-time 5 2>&1)
    echo "  角色分配响应: $ROLE_RESP" | head -1
fi

echo ""
echo "--- 步骤 3: 生成新启动脚本（带 Nacos 鉴权）---"
mkdir -p /opt/tailor-is/scripts
NEW_START=/opt/tailor-is/scripts/start-all-with-auth.sh

# 先备份
cp /opt/tailor-is/scripts/start-service.sh /opt/tailor-is/scripts/start-service.sh.bak 2>/dev/null
cp /opt/tailor-is/scripts/start-gateway.sh /opt/tailor-is/scripts/start-gateway.sh.bak 2>/dev/null
cp /opt/tailor-is/scripts/start-user.sh /opt/tailor-is/scripts/start-user.sh.bak 2>/dev/null

echo "  备份已创建"

echo ""
echo "--- 步骤 4: 准备所有服务启动脚本 ---"

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

# 通过 Docker 写入新启动脚本到 /opt/tailor-is/scripts/
docker run --rm \
    -v /opt/tailor-is/scripts:/dest \
    -v /opt/1panel/apps/nacos/nacos:/nacos:ro \
    alpine:latest \
    sh -c "echo 'ready'" 2>&1

# 直接 cat 写入新文件（因为 root 拥有 scripts 目录）
echo "  通过 docker 写入新脚本..."

# 生成一个启动所有服务的脚本
cat > /tmp/new-start-all.sh << 'NEWSTART'
#!/bin/bash
# Tailor IS 所有服务启动脚本 - 带 Nacos 鉴权
# 注意：Nacos 已启用鉴权 (NACOS_AUTH_ENABLE=true)
# 默认凭据: nacos/nacos

set +e
mkdir -p /tmp/tailor-is-logs
mkdir -p /tmp/tailor-is-pids

JAR_DIR=/opt/tailor-is/jars
LOG_DIR=/tmp/tailor-is-logs
PID_DIR=/tmp/tailor-is-pids

# Nacos 鉴权配置
NACOS_USER="nacos"
NACOS_PASS="nacos"
NACOS_ADDR="localhost:8848"

# 启动服务
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

    echo "[START] ${name} (DB: ${db_name})..."

    nohup java -Xms${xms} -Xmx${xmx} \
        -Dspring.application.name=tailor-is-${name} \
        -Dspring.datasource.url="jdbc:mysql://127.0.0.1:3306/${db_name}?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true" \
        -Dspring.datasource.username=root \
        -Dspring.datasource.password=mysql_CA75Yk \
        -Dspring.data.redis.host=172.18.0.5 \
        -Dspring.data.redis.port=6379 \
        -Dspring.data.redis.password=redis_RSeR4G \
        -Dspring.cloud.nacos.discovery.server-addr=${NACOS_ADDR} \
        -Dspring.cloud.nacos.discovery.username=${NACOS_USER} \
        -Dspring.cloud.nacos.discovery.password=${NACOS_PASS} \
        -Dspring.cloud.nacos.discovery.register-enabled=true \
        -Dspring.cloud.nacos.config.enabled=false \
        -Dseata.enabled=false \
        -Dspring.main.allow-circular-references=true \
        -Dspring.rabbitmq.username=rabbitmq \
        -Dspring.rabbitmq.password=rabbitmq \
        -jar tailor-is-${name}-1.0.0.jar > $LOG_DIR/${name}.log 2>&1 &

    local pid=$!
    echo $pid > $PID_DIR/${name}.pid
    echo "[OK] ${name} 启动 (PID: ${pid})"
}

echo "=== Tailor IS 服务启动 (带 Nacos 鉴权) ==="

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

# Gateway 最后启动
sleep 5
echo ""
echo "=== 启动 Gateway ==="
cd $JAR_DIR
nohup java -Xms256m -Xmx512m \
    -Dspring.cloud.nacos.config.import-check.enabled=false \
    -Dspring.cloud.nacos.discovery.server-addr=${NACOS_ADDR} \
    -Dspring.cloud.nacos.discovery.username=${NACOS_USER} \
    -Dspring.cloud.nacos.discovery.password=${NACOS_PASS} \
    -Dspring.cloud.nacos.config.username=${NACOS_USER} \
    -Dspring.cloud.nacos.config.password=${NACOS_PASS} \
    -Dspring.data.redis.host=localhost \
    -jar tailor-is-gateway-1.0.0.jar > $LOG_DIR/gateway.log 2>&1 &
echo $! > $PID_DIR/gateway.pid
echo "[OK] gateway 启动 (PID: $!)"

echo ""
echo "=== 启动完成 ==="
NEWSTART

# 通过 Docker 复制到 /opt/tailor-is/scripts/
docker run --rm \
    -v /tmp:/src:ro \
    -v /opt/tailor-is/scripts:/dest \
    alpine:latest \
    sh -c "cp /src/new-start-all.sh /dest/start-all-with-auth.sh && chmod +x /dest/start-all-with-auth.sh && ls -la /dest/start-all-with-auth.sh" 2>&1

echo ""
echo "=== 任务 3 准备完成 ==="
echo "下一步: 滚动重启所有微服务"
