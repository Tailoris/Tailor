#!/bin/bash
# ==============================================================================
# Tailor IS 集成测试脚本
# 启动所有预构建微服务镜像，验证 Nacos 注册和服务状态
# ==============================================================================
set -e

# 加载 .env 文件中的 AES_KEY
if [ -f .env ]; then
    AES_KEY=$(grep -E '^AES_KEY=' .env | head -1 | cut -d'=' -f2-)
fi
# 确保 AES_KEY 是 32 字节（AES-256 要求）
AES_KEY="${AES_KEY:-XU7gf2DyoBpmLJsvj3jDWrhmytcR2pYa}"
if [ "${#AES_KEY}" -ne 32 ]; then
    # 如果密钥长度不是 32，使用默认 32 字节密钥
    AES_KEY="XU7gf2DyoBpmLJsvj3jDWrhmytcR2pYa"
fi

# 1Panel 管理的基础设施凭据
MYSQL_PASSWORD="mysql_ZmY2sr"
REDIS_PASSWORD="redis_jD2N8n"
RABBITMQ_USERNAME="guest"
RABBITMQ_PASSWORD="guest"
NACOS_ADDR="host.docker.internal:8848"
AES_KEY="${AES_KEY:-tailor-is-aes-key-2026-06-12-test}"

NETWORK="tailor-is-network"
RESULTS_FILE="/tmp/integration_test_results.txt"
> "$RESULTS_FILE"

log() { echo "[$(date +%H:%M:%S)] $*" | tee -a "$RESULTS_FILE"; }
fail() { log "FAIL: $*"; return 1; }

cleanup() {
    log "=== 清理测试容器 ==="
    for svc in tailor-is-user-test tailor-is-merchant-test tailor-is-product-test \
               tailor-is-order-test tailor-is-payment-test tailor-is-marketing-test \
               tailor-is-ai-test tailor-is-community-test; do
        docker stop "$svc" 2>/dev/null || true
        docker rm "$svc" 2>/dev/null || true
    done
}
trap cleanup EXIT

# ==============================================================================
# 启动单个服务
# ==============================================================================
start_service() {
    local name=$1 image=$2 port=$3 extra_env=$4
    local container="tailor-is-${name}-test"
    
    log "启动 $name (端口 $port) ..."
    docker run -d --name "$container" \
        --network "$NETWORK" \
        --add-host=host.docker.internal:host-gateway \
        -p "${port}:${port}" \
        -e NACOS_ADDR="$NACOS_ADDR" \
        -e NACOS_USERNAME=nacos \
        -e NACOS_PASSWORD=nacos \
        -e MYSQL_HOST=host.docker.internal \
        -e MYSQL_PORT=3306 \
        -e MYSQL_USERNAME=root \
        -e MYSQL_PASSWORD="$MYSQL_PASSWORD" \
        -e REDIS_HOST=host.docker.internal \
        -e REDIS_PORT=6379 \
        -e REDIS_PASSWORD="$REDIS_PASSWORD" \
        -e RABBITMQ_HOST=host.docker.internal \
        -e RABBITMQ_PORT=5672 \
        -e RABBITMQ_USERNAME="$RABBITMQ_USERNAME" \
        -e RABBITMQ_PASSWORD="$RABBITMQ_PASSWORD" \
        -e AES_KEY="$AES_KEY" \
        $extra_env \
        "$image" 2>&1
    
    if [ $? -ne 0 ]; then
        fail "$name 容器启动失败"
        return 1
    fi
    log "  $name 容器已创建"
}

# ==============================================================================
# 等待服务健康
# ==============================================================================
wait_healthy() {
    local name=$1 timeout=${2:-120}
    local container="tailor-is-${name}-test"
    local elapsed=0
    
    log "  等待 $name 启动 (超时: ${timeout}s) ..."
    while [ $elapsed -lt $timeout ]; do
        # 检查容器状态
        local status=$(docker inspect -f '{{.State.Status}}' "$container" 2>/dev/null)
        if [ "$status" != "running" ]; then
            # 容器已退出，查看错误
            local exit_code=$(docker inspect -f '{{.State.ExitCode}}' "$container" 2>/dev/null)
            log "  $name 容器退出 (exit=$exit_code)，日志:"
            docker logs "$container" --tail 20 2>&1 | while read line; do log "    $line"; done
            fail "$name 容器意外退出"
            return 1
        fi
        
        # 检查 Spring Boot 启动标记
        if docker logs "$container" 2>&1 | grep -q "Started.*Application in"; then
            log "  $name 启动成功"
            return 0
        fi
        
        # 检查严重错误
        if docker logs "$container" 2>&1 | tail -5 | grep -q "Application run failed"; then
            log "  $name 启动失败，日志:"
            docker logs "$container" --tail 30 2>&1 | while read line; do log "    $line"; done
            fail "$name 启动失败"
            return 1
        fi
        
        sleep 5
        elapsed=$((elapsed + 5))
    done
    
    # 超时: 输出当前日志
    log "  $name 超时 (${timeout}s)，当前日志:"
    docker logs "$container" --tail 20 2>&1 | while read line; do log "    $line"; done
    fail "$name 启动超时"
    return 1
}

# ==============================================================================
# 主测试流程
# ==============================================================================
log "============================================"
log "Tailor IS 全量集成测试开始"
log "============================================"

# 1. 基础设施验证
log ""
log "=== 阶段 1: 基础设施验证 ==="
log "MySQL: $(mysql -h 127.0.0.1 -u root -p"$MYSQL_PASSWORD" -e "SELECT 'OK'" 2>&1 | tail -1)"
log "Redis: $(docker exec 1Panel-redis-9MsK redis-cli -a "$REDIS_PASSWORD" PING 2>&1 | tail -1)"
log "Nacos: $(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8848/nacos/ 2>&1)"
log "RabbitMQ: $(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:15672 2>&1)"

# 2. 批量启动核心微服务
log ""
log "=== 阶段 2: 启动核心微服务 ==="

# 先启动 user-service（最基础的服务）
start_service "user"      "tailor-is/user-service:latest"      8101
wait_healthy "user" 120 || true

# 并行启动其他核心服务
start_service "merchant"  "tailor-is/merchant-service:latest"   8110
start_service "product"   "tailor-is/product-service:latest"    8102
start_service "order"     "tailor-is/order-service:latest"      8103
start_service "payment"   "tailor-is/payment-service:latest"    8104
start_service "marketing" "tailor-is/marketing-service:latest"  8105
start_service "ai"        "tailor-is/ai-service:latest"         8106 \
    "-e SPRING_AUTOCONFIGURE_EXCLUDE=org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration"

# 启动 community
start_service "community" "tailor-is/community-service:latest"  8108

# 等待所有服务启动
log ""
log "=== 阶段 3: 等待所有服务启动 ==="

declare -A services=(
    [user]=8101 [merchant]=8110 [product]=8102 [order]=8103
    [payment]=8104 [marketing]=8105 [ai]=8106 [community]=8108
)

PASS=0
FAIL=0
for svc in "${!services[@]}"; do
    if wait_healthy "$svc" 180; then
        PASS=$((PASS + 1))
    else
        FAIL=$((FAIL + 1))
    fi
done

# 3. 验证结果
log ""
log "=== 阶段 4: 集成测试结果汇总 ==="
log ""

# 列出运行中的测试容器
log "运行中的容器:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep "tailor-is-.*-test" | while read line; do
    log "  $line"
done

log ""
log "============================================"
log "测试结果: 通过 $PASS / 失败 $FAIL / 总计 $((PASS + FAIL))"
log "============================================"

# 输出所有服务的最终日志摘要
log ""
log "=== 服务日志摘要 ==="
for svc in "${!services[@]}"; do
    container="tailor-is-${svc}-test"
    log "--- $svc ---"
    docker logs "$container" 2>&1 | grep -E "Started|Application run failed|ERROR" | tail -3 | while read line; do
        log "  $line"
    done
done

exit $FAIL