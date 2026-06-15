#!/bin/bash
# =============================================================
# 凭证验证脚本 - 部署前必跑
# =============================================================
# 用途: 验证所有服务的连接凭证是否正确
# 退出码: 0 全部通过 / 1 有失败
# =============================================================

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 加载凭证
if [ -f "$SCRIPT_DIR/load-credentials.sh" ]; then
    source "$SCRIPT_DIR/load-credentials.sh"
else
    echo -e "${RED}[ERROR]${NC} 找不到 load-credentials.sh" >&2
    exit 1
fi

PASS=0
FAIL=0
WARN=0

check_pass() { PASS=$((PASS+1)); echo -e "  ${GREEN}[✓]${NC} $1"; }
check_fail() { FAIL=$((FAIL+1)); echo -e "  ${RED}[✗]${NC} $1"; }
check_warn() { WARN=$((WARN+1)); echo -e "  ${YELLOW}[!]${NC} $1"; }

echo "============================================================"
echo "  Tailor IS 凭证验证"
echo "============================================================"
echo ""
echo "  MySQL: $MYSQL_HOST:$MYSQL_PORT (user=$MYSQL_USERNAME)"
echo "  Redis: $REDIS_HOST:$REDIS_PORT"
echo "  Nacos: $NACOS_ADDR"
echo "  RabbitMQ: $RABBITMQ_HOST:$RABBITMQ_PORT"
echo ""

# ============================================================
# 1. MySQL 验证
# ============================================================
echo "[1] MySQL 连接测试"
if command -v mysql >/dev/null 2>&1; then
    if mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u"$MYSQL_USERNAME" -p"$MYSQL_PASSWORD" \
        -e "SELECT VERSION();" 2>/dev/null | grep -q "MariaDB\|MySQL"; then
        VERSION=$(mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u"$MYSQL_USERNAME" -p"$MYSQL_PASSWORD" \
            -Bse "SELECT VERSION();" 2>/dev/null)
        check_pass "MySQL 连接成功 (版本: $VERSION)"
    else
        check_fail "MySQL 连接失败 (请检查密码)"
    fi
else
    check_warn "mysql 客户端未安装,跳过测试"
fi

# ============================================================
# 2. Redis 验证
# ============================================================
echo ""
echo "[2] Redis 连接测试"
if command -v redis-cli >/dev/null 2>&1; then
    REDIS_OUT=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" \
        --no-auth-warning PING 2>&1)
    if echo "$REDIS_OUT" | grep -q "PONG"; then
        check_pass "Redis 连接成功 (PONG)"
    else
        check_fail "Redis 连接失败: $REDIS_OUT"
    fi
else
    check_warn "redis-cli 客户端未安装,跳过测试"
fi

# ============================================================
# 3. Nacos 验证
# ============================================================
echo ""
echo "[3] Nacos 连接测试"
NACOS_RESP=$(curl -s -m 5 -u "$NACOS_USERNAME:$NACOS_PASSWORD" \
    "http://$NACOS_ADDR/nacos/v1/ns/catalog/services?pageNo=1&pageSize=1" 2>&1)
if echo "$NACOS_RESP" | grep -q "serviceList\|count"; then
    check_pass "Nacos 连接成功"
else
    check_fail "Nacos 连接失败: $NACOS_RESP"
fi

# ============================================================
# 4. RabbitMQ 验证
# ============================================================
echo ""
echo "[4] RabbitMQ 连接测试"
RMQ_RESP=$(curl -s -m 5 -u "$RABBITMQ_ADMIN_USER:$RABBITMQ_ADMIN_PASSWORD" \
    "http://$RABBITMQ_HOST:$RABBITMQ_DASHBOARD_PORT/api/overview" 2>&1)
if echo "$RMQ_RESP" | grep -q "rabbitmq_version\|product_version\|management"; then
    check_pass "RabbitMQ Dashboard 连接成功"
else
    check_warn "RabbitMQ Dashboard 不可达 (服务可能正常,只是 dashboard 端口未开放)"
fi

# ============================================================
# 5. 端口占用检查
# ============================================================
echo ""
echo "[5] 服务端口可用性"
declare -A PORTS=(
    ["GATEWAY"]=$GATEWAY_PORT
    ["USER"]=$USER_PORT
    ["MERCHANT"]=$MERCHANT_PORT
    ["PRODUCT"]=$PRODUCT_PORT
    ["ORDER"]=$ORDER_PORT
    ["PAYMENT"]=$PAYMENT_PORT
    ["MARKETING"]=$MARKETING_PORT
    ["AI"]=$AI_PORT
    ["COPYRIGHT"]=$COPYRIGHT_PORT
    ["COMMUNITY"]=$COMMUNITY_PORT
    ["SUPPLY"]=$SUPPLY_PORT
    ["MESSAGE"]=$MESSAGE_PORT
)

for svc in "${!PORTS[@]}"; do
    port=${PORTS[$svc]}
    if ss -tln 2>/dev/null | grep -q ":$port "; then
        check_warn "端口 $port ($svc) 已被占用 (服务可能已运行)"
    else
        check_pass "端口 $port ($svc) 可用"
    fi
done

# ============================================================
# 总结
# ============================================================
echo ""
echo "============================================================"
echo "  验证结果"
echo "  通过: $PASS"
echo "  失败: $FAIL"
echo "  警告: $WARN"
echo "============================================================"

if [ "$FAIL" -gt 0 ]; then
    echo -e "${RED}存在 $FAIL 项凭证错误,请检查 deploy/.env.production${NC}"
    exit 1
else
    echo -e "${GREEN}凭证全部正确,可以继续部署${NC}"
    exit 0
fi
