#!/bin/bash
# ==============================================================================
# 紧急关停非核心服务脚本
# 保留核心交易链路: core-gateway, user-service, product-service, order-service, payment-service
# 关停其余13个服务，紧急降低CPU占用
# 用法: ./emergency-stop-noncore.sh
# ==============================================================================

NONCORE_SERVICES=(
    "tailor-is-admin-service"
    "tailor-is-marketing-service"
    "tailor-is-ai-service"
    "tailor-is-copyright-service"
    "tailor-is-community-service"
    "tailor-is-supply-service"
    "tailor-is-merchant-service"
    "tailor-is-message-service"
    "tailor-is-academy-service"
    "tailor-is-analytics-service"
    "tailor-is-message-im-service"
    "tailor-is-pattern-service"
    "tailor-is-lite-gateway"
)

STOPPED_COUNT=0
FAILED_COUNT=0

echo "=== Emergency Stop Non-Core Services ==="
echo "Timestamp: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

for svc in "${NONCORE_SERVICES[@]}"; do
    if docker ps --format '{{.Names}}' | grep -q "^${svc}$"; then
        echo "Stopping $svc..."
        if docker stop --time=30 "$svc" 2>/dev/null; then
            echo "  [OK] $svc stopped"
            ((STOPPED_COUNT++))
        else
            echo "  [FAIL] $svc failed to stop"
            ((FAILED_COUNT++))
        fi
    else
        echo "  [SKIP] $svc not running"
    fi
done

echo ""
echo "=== Summary ==="
echo "Stopped: $STOPPED_COUNT"
echo "Failed: $FAILED_COUNT"
echo "Skipped (not running): $((${#NONCORE_SERVICES[@]} - STOPPED_COUNT - FAILED_COUNT))"

echo ""
echo "Waiting 5 seconds for CPU to stabilize..."
sleep 5

echo ""
echo "=== Current CPU Usage ==="
top -bn1 | head -5

echo ""
echo "=== Remaining Core Services ==="
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' | grep -E "tailor-is-(core-gateway|user-service|product-service|order-service|payment-service)"