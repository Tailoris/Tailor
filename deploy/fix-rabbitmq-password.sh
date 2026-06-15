#!/bin/bash
# =============================================================
# 修复 RabbitMQ 密码: 重启 user 服务,补全 -Dspring.rabbitmq.password
# 凭据: rabbitmq / rabbitmq (与 1Panel RabbitMQ 容器一致)
# =============================================================
exec 2>&1
set -u
echo "=== 修复 RabbitMQ 密码: 重启 user 服务 - $(date) ==="

JAR_DIR=/opt/tailor-is/jars
LOG_DIR=/opt/tailor-is/logs
PID_FILE=/tmp/tailor-is-pids/user.pid

# ---------- 0. 凭据确认 ----------
RMQ_USER="rabbitmq"
RMQ_PASS="rabbitmq"
echo "[0] 目标凭据: ${RMQ_USER} / ${RMQ_PASS}"

# ---------- 1. RabbitMQ Dashboard 自检 ----------
echo ""
echo "[1] 验证 RabbitMQ 实际凭据是否可用..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    -u "${RMQ_USER}:${RMQ_PASS}" \
    --max-time 5 \
    http://127.0.0.1:15672/api/overview)
if [ "$HTTP_CODE" = "200" ]; then
    echo "  [OK] rabbitmq/rabbitmq 凭据有效 (HTTP 200)"
else
    echo "  [FAIL] 凭据验证失败 HTTP=$HTTP_CODE"
    echo "  [HINT] 请确认 1Panel RabbitMQ 容器实际管理员密码"
    exit 1
fi

# ---------- 2. 停掉当前 user 进程 ----------
echo ""
echo "[2] 停掉当前 user 服务 (PID 11717)..."
pkill -9 -f "tailor-is-user-1.0.0.jar" 2>/dev/null
sleep 2
if pgrep -f "tailor-is-user-1.0.0.jar" > /dev/null; then
    echo "  [WARN] 进程仍在, 强制 kill -9"
    pkill -9 -9 -f "tailor-is-user-1.0.0.jar" 2>/dev/null
    sleep 1
fi
rm -f "$PID_FILE"
echo "  [OK] user 进程已停止"

# ---------- 3. 用正确参数重启 user ----------
echo ""
echo "[3] 启动 user 服务 (补全 RabbitMQ 密码)..."
mkdir -p "$LOG_DIR" "$(dirname "$PID_FILE")"

cd "$JAR_DIR"
nohup java -Xms256m -Xmx512m \
    -Dspring.cloud.nacos.config.import-check.enabled=false \
    -Dspring.cloud.nacos.discovery.server-addr=127.0.0.1:8848 \
    -Dspring.cloud.nacos.discovery.username=nacos \
    -Dspring.cloud.nacos.discovery.password=nacos \
    -Dspring.cloud.nacos.discovery.namespace= \
    -Dspring.datasource.url="jdbc:mysql://127.0.0.1:3306/tailor_is_user?useSSL=false&serverTimezone=UTC" \
    -Dspring.datasource.username=root \
    -Dspring.datasource.password=mysql_CA75Yk \
    -Dspring.data.redis.host=172.18.0.2 \
    -Dspring.data.redis.port=6379 \
    -Dspring.data.redis.password=redis_RSeR4G \
    -Dspring.rabbitmq.host=172.18.0.4 \
    -Dspring.rabbitmq.port=5672 \
    -Dspring.rabbitmq.username=${RMQ_USER} \
    -Dspring.rabbitmq.password=${RMQ_PASS} \
    -Dspring.rabbitmq.virtual-host=/ \
    -Dserver.port=8101 \
    -Dmanagement.endpoints.web.exposure.include=health,info,prometheus,metrics \
    -Dmanagement.endpoint.health.show-details=always \
    -jar tailor-is-user-1.0.0.jar --spring.profiles.active=prod \
    >> "$LOG_DIR/tailor-is-user.log" 2>&1 &

NEW_PID=$!
echo $NEW_PID > "$PID_FILE"
echo "  [OK] user 启动 (PID: $NEW_PID)"

# ---------- 4. 等待并验证 ----------
echo ""
echo "[4] 等待 30 秒让 user 完全启动..."
sleep 30

echo ""
echo "[5] 检查进程..."
if pgrep -f "tailor-is-user-1.0.0.jar" > /dev/null; then
    echo "  [OK] 进程运行中"
else
    echo "  [FAIL] 进程已退出, 查看日志:"
    tail -50 "$LOG_DIR/tailor-is-user.log"
    exit 1
fi

echo ""
echo "[6] 端口监听..."
if ss -tln | grep -q ":8101 "; then
    echo "  [OK] 8101 已监听"
else
    echo "  [FAIL] 8101 未监听"
fi

echo ""
echo "[7] RabbitMQ 鉴权关键字检查 (应无 ACCESS_REFUSED)..."
if grep -E "ACCESS_REFUSED|AmqpAuthenticationException" \
    "$LOG_DIR/tailor-is-user.log" | tail -3; then
    echo "  [FAIL] 仍存在鉴权失败, 请检查"
else
    echo "  [OK] 最近无 RabbitMQ 鉴权失败记录"
fi

echo ""
echo "[8] Spring 启动成功关键字..."
grep -E "Started UserApplication|Tomcat started" \
    "$LOG_DIR/tailor-is-user.log" | tail -3

echo ""
echo "=== 修复完成 - $(date) ==="
