#!/bin/bash
# =============================================================
# 阶段6 监控完善: Prometheus 抓取状态检查
# =============================================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo "============================================================"
echo "  Prometheus 抓取状态检查"
echo "  执行时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "============================================================"

PROMETHEUS_URL="http://localhost:9090"
TOTAL=0
UP=0
DOWN=0

echo ""
echo "=== 1. Active Targets 抓取状态 ==="

# 解析 Prometheus targets API
TARGETS_JSON=$(curl.exe -s --noproxy '*' --max-time 10 "$PROMETHEUS_URL/api/v1/targets" 2>/dev/null)
if [ -z "$TARGETS_JSON" ]; then
    echo "[FAIL] 无法连接 Prometheus ($PROMETHEUS_URL)"
    exit 1
fi

# 简单解析 (使用 jq 如果可用，否则用 grep/sed)
if command -v jq >/dev/null 2>&1; then
    echo "$TARGETS_JSON" | jq -r '.data.activeTargets[] | "\(.labels.service // .labels.job)|\(.health)|\(.lastError // "ok")"' | sort | while IFS='|' read -r svc health err; do
        TOTAL=$((TOTAL+1))
        if [ "$health" = "up" ]; then
            UP=$((UP+1))
            echo -e "  ${GREEN}[UP]${NC}   $svc"
        else
            DOWN=$((DOWN+1))
            echo -e "  ${RED}[DOWN]${NC} $svc - $err"
        fi
    done
else
    # 不使用 jq 的简单方式
    echo "$TARGETS_JSON" | grep -oE '"labels":\{"[^}]*\}' | head -30
fi

echo ""
echo "=== 2. 关键指标存在性 ==="

for metric in jvm_memory_used_bytes http_server_requests_seconds_count hikaricp_connections_active jvm_threads_states_threads; do
    result=$(curl.exe -s --noproxy '*' --max-time 5 "$PROMETHEUS_URL/api/v1/query?query=$metric" 2>/dev/null | grep -o '"resultType"' | wc -l)
    if [ "$result" -gt 0 ]; then
        echo -e "  ${GREEN}[OK]${NC}   $metric: 已采集"
    else
        echo -e "  ${YELLOW}[WARN]${NC} $metric: 未采集"
    fi
done

echo ""
echo "=== 3. 告警规则 ==="
ALERTS=$(curl.exe -s --noproxy '*' --max-time 5 "$PROMETHEUS_URL/api/v1/rules" 2>/dev/null)
RULES_COUNT=$(echo "$ALERTS" | grep -o '"name":"[A-Z][^"]*"' | sort -u | wc -l)
echo "  加载告警规则数: $RULES_COUNT"

if [ "$RULES_COUNT" -gt 0 ]; then
    echo "$ALERTS" | grep -oE '"name":"[A-Z][^"]*"' | sort -u | head -10 | while read -r line; do
        name=$(echo "$line" | sed 's/"name":"//;s/"$//')
        echo "    - $name"
    done
fi

echo ""
echo "=== 4. 告警状态 ==="
ALERT_STATUS=$(curl.exe -s --noproxy '*' --max-time 5 "$PROMETHEUS_URL/api/v1/alerts" 2>/dev/null)
FIRING=$(echo "$ALERT_STATUS" | grep -o '"state":"firing"' | wc -l)
PENDING=$(echo "$ALERT_STATUS" | grep -o '"state":"pending"' | wc -l)
INACTIVE=$(echo "$ALERT_STATUS" | grep -o '"state":"inactive"' | wc -l)
echo "  Firing: $FIRING"
echo "  Pending: $PENDING"
echo "  Inactive: $INACTIVE"

echo ""
echo "=== 5. ServiceMonitor 抓取间隔 ==="
echo "  scrape_interval: 15s"
echo "  scrape_timeout: 10s"

echo ""
echo "=== 6. Grafana 仪表盘 ==="
GRAFANA_DASHBOARDS=$(curl.exe -s --noproxy '*' --max-time 5 -u admin:admin "http://localhost:3000/api/search?type=dash-db" 2>/dev/null)
DASH_COUNT=$(echo "$GRAFANA_DASHBOARDS" | grep -o '"uid"' | wc -l)
if [ "$DASH_COUNT" -gt 0 ]; then
    echo -e "  ${GREEN}[OK]${NC}   Grafana 仪表盘数: $DASH_COUNT"
    echo "$GRAFANA_DASHBOARDS" | grep -oE '"title":"[^"]*"' | head -10 | while read -r line; do
        title=$(echo "$line" | sed 's/"title":"//;s/"$//')
        echo "    - $title"
    done
else
    echo -e "  ${YELLOW}[WARN]${NC} Grafana 未配置仪表盘或无法访问"
fi

echo ""
echo "=== 7. 总结 ==="
echo "============================================================"
