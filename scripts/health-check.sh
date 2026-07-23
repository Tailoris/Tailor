#!/bin/bash
# ==============================================================================
# Tailor-IS 健康检查聚合脚本
# 用途: 一键检查所有微服务健康状态，输出汇总报告
# 用法: bash scripts/health-check.sh
# ==============================================================================
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

HEALTHY=0
UNHEALTHY=0
UNREACHABLE=0

check_service() {
    local name=$1
    local host=$2
    local port=$3
    local url="http://${host}:${port}/actuator/health"

    if curl -s --connect-timeout 3 --max-time 5 "$url" > /dev/null 2>&1; then
        local status=$(curl -s "$url" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)
        if [ "$status" = "UP" ]; then
            printf "  ${GREEN}✓${NC} %-25s ${GREEN}UP${NC}\n" "$name"
            HEALTHY=$((HEALTHY + 1))
        else
            printf "  ${YELLOW}⚠${NC} %-25s ${YELLOW}${status:-UNKNOWN}${NC}\n" "$name"
            UNHEALTHY=$((UNHEALTHY + 1))
        fi
    else
        printf "  ${RED}✗${NC} %-25s ${RED}DOWN${NC}\n" "$name"
        UNREACHABLE=$((UNREACHABLE + 1))
    fi
}

echo ""
echo "=============================================="
echo " Tailor-IS 健康检查聚合报告"
echo " $(date '+%Y-%m-%d %H:%M:%S')"
echo "=============================================="
echo ""

# ===== 基础设施 =====
echo "${BLUE}[基础设施]${NC}"
check_service "MySQL" "mysql" "3306" || check_service "MySQL" "localhost" "3306"
check_service "Redis" "redis" "6379" || check_service "Redis" "localhost" "6379"
check_service "Nacos" "nacos" "8848" || check_service "Nacos" "localhost" "8848"
check_service "RabbitMQ" "rabbitmq" "15672" || check_service "RabbitMQ" "localhost" "15672"
check_service "MongoDB" "mongodb" "27017" || check_service "MongoDB" "localhost" "27017"

echo ""

# ===== 网关服务 =====
echo "${BLUE}[网关服务]${NC}"
check_service "core-gateway" "localhost" "9000"
check_service "lite-gateway" "localhost" "8081"

echo ""

# ===== 核心业务服务 (8100-8107) =====
echo "${BLUE}[核心业务服务]${NC}"
check_service "admin-service" "localhost" "8100"
check_service "user-service" "localhost" "8101"
check_service "product-service" "localhost" "8102"
check_service "order-service" "localhost" "8103"
check_service "payment-service" "localhost" "8104"
check_service "marketing-service" "localhost" "8105"
check_service "ai-service" "localhost" "8106"
check_service "copyright-service" "localhost" "8107"

echo ""

# ===== 轻量业务服务 (8108-8117) =====
echo "${BLUE}[轻量业务服务]${NC}"
check_service "community-service" "localhost" "8108"
check_service "supply-service" "localhost" "8109"
check_service "merchant-service" "localhost" "8110"
check_service "message-service" "localhost" "8111"
check_service "academy-service" "localhost" "8112"
check_service "analytics-service" "localhost" "8113"
check_service "message-im-service" "localhost" "8114"
check_service "pattern-service" "localhost" "8115"
check_service "design-service" "localhost" "8116"
check_service "knowledge-service" "localhost" "8117"

echo ""
echo "=============================================="
printf " 汇总: ${GREEN}%d 健康${NC} / ${YELLOW}%d 不健康${NC} / ${RED}%d 不可达${NC}\n" $HEALTHY $UNHEALTHY $UNREACHABLE
echo "=============================================="

TOTAL=$((HEALTHY + UNHEALTHY + UNREACHABLE))
if [ $UNREACHABLE -gt 0 ] || [ $UNHEALTHY -gt 0 ]; then
    echo ""
    echo "${RED}警告: 存在不健康或不可达的服务，请检查容器状态。${NC}"
    exit 1
else
    echo ""
    echo "${GREEN}所有服务健康检查通过。${NC}"
    exit 0
fi