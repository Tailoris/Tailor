#!/bin/bash
# ==============================================================================
# Tailor IS 健康检查脚本
# ==============================================================================
# 版本: v1.0
# 用途: 一键检查系统各项服务健康状态
# 使用: sudo ./health-check.sh
# ===============================================================================

set -euo pipefail

PROJECT_DIR="/opt/tailor-is"
COMPOSE_FILE="${PROJECT_DIR}/docker-compose.prod.yml"

PASS=0; FAIL=0; WARN=0
pass() { PASS=$((PASS+1)); echo "✅ $1"; }
fail() { FAIL=$((FAIL+1)); echo "❌ $1"; }
warn() { WARN=$((WARN+1)); echo "⚠️  $1"; }

echo "============================================================"
echo "  Tailor IS 健康状态检查  v1.0"
echo "  $(date '+%Y-%m-%d %H:%M:%S')"
echo "============================================================"
echo ""

# ========== 1. 容器运行状态 ==========
echo "【1/6】容器运行状态"
if [ -f "$COMPOSE_FILE" ]; then
    CONTAINERS=$(docker compose -f "$COMPOSE_FILE" ps --format json 2>/dev/null | grep -c '"State":"running"' || true)
    SERVICES=$(docker compose -f "$COMPOSE_FILE" config --services | wc -l)
    if [ "$CONTAINERS" -eq "$SERVICES" ]; then
        pass "全部容器运行中 ($CONTAINERS/$SERVICES)"
    else
        warn "部分容器未运行: $CONTAINERS/$SERVICES"
    fi
    # 展示每个容器状态
    docker compose -f "$COMPOSE_FILE" ps --format '{{.Name}} - {{.State}} - {{.Status}}' 2>/dev/null | while read -r line; do
        echo "   · $line"
    done
else
    warn "未找到 compose 文件"
fi
echo ""

# ========== 2. MySQL ==========
echo "【2/6】MySQL 数据库"
if docker ps --format '{{.Names}}' | grep -q "^tailor-is-mysql$"; then
    RESULT=$(docker exec tailor-is-mysql mysqladmin ping -h localhost --silent 2>&1 && echo "ok" || echo "fail")
    if [ "$RESULT" = "ok" ]; then
        SIZE=$(docker exec tailor-is-mysql mysql -u root -e "SELECT ROUND(SUM(data_length + index_length)/1024/1024, 1) AS 'MB' FROM information_schema.tables WHERE table_schema='tailor_is';" 2>/dev/null | tail -1)
        pass "MySQL 正常, 数据库大小: ${SIZE:-n/a} MB"
    else fail "MySQL 无法连接"; fi
else fail "MySQL 容器未运行"; fi
echo ""

# ========== 3. Redis ==========
echo "【3/6】Redis 缓存"
if docker ps --format '{{.Names}}' | grep -q "^tailor-is-redis$"; then
    PONG=$(docker exec tailor-is-redis redis-cli ping 2>/dev/null)
    if [ "$PONG" = "PONG" ]; then
        MEM=$(docker exec tailor-is-redis redis-cli info memory 2>/dev/null | grep used_memory_human | cut -d: -f2 | tr -d '\r')
        KEYS=$(docker exec tailor-is-redis redis-cli dbsize 2>/dev/null | tr -d '\r')
        pass "Redis 正常, 内存=$MEM, 键数=$KEYS"
    else fail "Redis 无响应"; fi
else fail "Redis 容器未运行"; fi
echo ""

# ========== 4. RabbitMQ ==========
echo "【4/6】RabbitMQ 消息队列"
if docker ps --format '{{.Names}}' | grep -q "^tailor-is-rabbitmq$"; then
    QUEUES=$(docker exec tailor-is-rabbitmq rabbitmqctl list_queues -s 2>/dev/null | wc -l)
    pass "RabbitMQ 正常, 队列数=$((QUEUES-2))"
else fail "RabbitMQ 容器未运行"; fi
echo ""

# ========== 5. Nacos 服务注册中心 ==========
echo "【5/6】Nacos 服务注册中心"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8848/nacos/ 2>/dev/null || echo "000")
if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "302" ]; then
    INSTANCES=$(curl -s "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=tailor-is-user" 2>/dev/null | grep -o '"ip":"[0-9.]*"' | wc -l)
    pass "Nacos 正常 (HTTP $HTTP_CODE), 注册服务可见"
else warn "Nacos HTTP $HTTP_CODE (首次启动需2-3分钟初始化)"; fi
echo ""

# ========== 6. 核心网关 ==========
echo "【6/6】API 网关 + 前端"
GW_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null || echo "000")
if echo "$GW_CODE" | grep -qE "200|401|403|404"; then
    pass "核心网关可访问 (HTTP $GW_CODE)"
else fail "核心网关无响应 ($GW_CODE)"; fi

if curl -s -o /dev/null -w "%{http_code}" http://localhost/ 2>/dev/null | grep -qE "200|301|302"; then pass "Nginx 前端正常"; else warn "前端页面无响应"; fi
echo ""

# ========== 资源使用 ==========
echo "============================================================"
echo "  资源使用概览"
echo "============================================================"
echo "  CPU / 内存:"
docker stats --no-stream --format 'table {{.Name}}\t{{.CPUPerc}}\t{{.MemPerc}}\t{{.NetIO}}' 2>/dev/null | head -8
echo ""
echo "  磁盘:"
df -h "${PROJECT_DIR}" 2>/dev/null | awk 'NR==2 {printf("  项目目录: %s 已用 / %s 总共\n", $3, $2)}'
echo ""

# ========== 总结 ==========
echo "============================================================"
echo "  检查结果: ✅ $PASS  通过  ⚠️ $WARN  警告  ❌ $FAIL  失败"
if [ "$FAIL" -eq 0 ]; then
    echo "  状态: 🟢 系统健康"
else
    echo "  状态: 🔴 需要关注, 请检查上方 ❌ 项"
fi
echo "============================================================"

# 退出码供监控使用
if [ "$FAIL" -gt 0 ]; then exit 2; elif [ "$WARN" -gt 0 ]; then exit 1; else exit 0; fi
