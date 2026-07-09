#!/bin/bash
# ==============================================================================
# Tailor IS - 服务健康检查脚本
# 用法: bash health-check.sh
# ==============================================================================

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "===== Tailor IS 服务健康检查 ====="
echo ""

# 中间件检查
echo "--- 中间件 ---"
for svc in mysql redis rabbitmq nacos; do
    case $svc in
        mysql)
            if mysqladmin ping -h 127.0.0.1 -u root -p'mysql_ZmY2sr' --silent 2>/dev/null; then
                echo -e "  ${GREEN}✓ MySQL${NC}"
            else
                echo -e "  ${RED}✗ MySQL${NC}"
            fi
            ;;
        redis)
            if redis-cli -a 'redis_jD2N8n' ping 2>/dev/null | grep -q PONG; then
                echo -e "  ${GREEN}✓ Redis${NC}"
            else
                echo -e "  ${RED}✗ Redis${NC}"
            fi
            ;;
        rabbitmq)
            if curl -s -f -m 5 http://localhost:15672 > /dev/null 2>&1; then
                echo -e "  ${GREEN}✓ RabbitMQ${NC}"
            else
                echo -e "  ${RED}✗ RabbitMQ${NC}"
            fi
            ;;
        nacos)
            if curl -s -f -m 5 http://localhost:8848/nacos/v1/console/health/liveness > /dev/null 2>&1; then
                echo -e "  ${GREEN}✓ Nacos${NC}"
            else
                echo -e "  ${RED}✗ Nacos${NC}"
            fi
            ;;
    esac
done

echo ""
echo "--- 微服务 ---"

# 服务端口映射
declare -A SERVICES=(
    [9000]="core-gateway"
    [8081]="lite-gateway"
    [8100]="admin-service"
    [8101]="user-service"
    [8102]="product-service"
    [8103]="order-service"
    [8104]="payment-service"
    [8105]="marketing-service"
    [8106]="ai-service"
    [8107]="copyright-service"
    [8108]="community-service"
    [8109]="supply-service"
    [8110]="merchant-service"
    [8111]="message-service"
    [8112]="academy-service"
    [8113]="analytics-service"
    [8114]="message-im-service"
    [8115]="pattern-service"
)

HEALTHY=0
UNHEALTHY=0

for port in "${!SERVICES[@]}"; do
    name="${SERVICES[$port]}"
    if curl -s -f -m 5 "http://127.0.0.1:${port}/actuator/health" > /dev/null 2>&1; then
        echo -e "  ${GREEN}✓ $name (:$port)${NC}"
        HEALTHY=$((HEALTHY + 1))
    else
        echo -e "  ${YELLOW}○ $name (:$port) - 未运行${NC}"
        UNHEALTHY=$((UNHEALTHY + 1))
    fi
done

echo ""
echo -e "健康: ${GREEN}${HEALTHY}${NC} / 异常: ${YELLOW}${UNHEALTHY}${NC}"
echo "===== 检查完成 ====="