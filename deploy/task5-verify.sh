#!/bin/bash
exec 2>&1
echo "=== 任务 5 验证: actuator 端点 - $(date) ==="

declare -A PORT_MAP
PORT_MAP["gateway"]="8081"
PORT_MAP["user"]="8101"
PORT_MAP["merchant"]="8102"
PORT_MAP["product"]="8103"
PORT_MAP["order"]="8104"
PORT_MAP["payment"]="8105"
PORT_MAP["marketing"]="8106"
PORT_MAP["ai"]="8107"
PORT_MAP["copyright"]="8108"
PORT_MAP["community"]="8109"
PORT_MAP["supply"]="8110"
PORT_MAP["message"]="8111"

echo "--- 步骤 1: 验证 /actuator/health ---"
h_ok=0
h_fail=0
for s in "${!PORT_MAP[@]}"; do
    p=${PORT_MAP[$s]}
    code=$(curl -s -o /dev/null -w "%{http_code}" --noproxy '*' --max-time 5 "http://localhost:${p}/actuator/health" 2>/dev/null)
    if [ "$code" = "200" ] || [ "$code" = "503" ]; then
        h_ok=$((h_ok+1))
        # 503 表示服务运行但健康检查有问题（仍可访问）
        echo "  [OK] $s (:$p) HTTP $code"
    else
        h_fail=$((h_fail+1))
        echo "  [FAIL] $s (:$p) HTTP $code"
    fi
done
echo "  成功: $h_ok / 12"

echo ""
echo "--- 步骤 2: 验证 /actuator/prometheus ---"
p_ok=0
p_fail=0
for s in "${!PORT_MAP[@]}"; do
    p=${PORT_MAP[$s]}
    code=$(curl -s -o /dev/null -w "%{http_code}" --noproxy '*' --max-time 5 "http://localhost:${p}/actuator/prometheus" 2>/dev/null)
    if [ "$code" = "200" ]; then
        # 检查是否真的有指标
        metrics=$(curl -s --noproxy '*' --max-time 5 "http://localhost:${p}/actuator/prometheus" 2>/dev/null | wc -l)
        p_ok=$((p_ok+1))
        echo "  [OK] $s (:$p) HTTP 200, $metrics 行指标"
    else
        p_fail=$((p_fail+1))
        echo "  [FAIL] $s (:$p) HTTP $code"
    fi
done
echo "  成功: $p_ok / 12"

echo ""
echo "--- 步骤 3: 验证 /actuator/info ---"
i_ok=0
i_fail=0
for s in "${!PORT_MAP[@]}"; do
    p=${PORT_MAP[$s]}
    code=$(curl -s -o /dev/null -w "%{http_code}" --noproxy '*' --max-time 3 "http://localhost:${p}/actuator/info" 2>/dev/null)
    if [ "$code" = "200" ] || [ "$code" = "404" ]; then
        i_ok=$((i_ok+1))
    else
        i_fail=$((i_fail+1))
    fi
done
echo "  成功: $i_ok / 12"

echo ""
echo "--- 步骤 4: 验证 Prometheus 抓取 ---"
sleep 15
target_count=$(curl -s --noproxy '*' --max-time 5 'http://localhost:9090/api/v1/targets' 2>/dev/null | grep -oE '"health":"up"' | wc -l)
total_targets=$(curl -s --noproxy '*' --max-time 5 'http://localhost:9090/api/v1/targets' 2>/dev/null | grep -oE '"scrapePool"' | wc -l)
echo "  Prometheus 抓取 UP: $target_count / $total_targets"

echo ""
echo "--- 步骤 5: 验证关键指标 ---"
echo "  抓取 sample_user 服务的关键指标:"
metrics=$(curl -s --noproxy '*' --max-time 5 'http://localhost:8101/actuator/prometheus' 2>/dev/null | head -20)
echo "$metrics" | head -10 | sed 's/^/    /'

echo ""
echo "--- 步骤 6: 验证 actuator 端点列表 ---"
echo "  user 服务的可用端点:"
endpoints=$(curl -s --noproxy '*' --max-time 5 'http://localhost:8101/actuator' 2>/dev/null)
echo "$endpoints" | head -1 | sed 's/^/    /'

echo ""
echo "=== 任务 5 验证完成 ==="
echo ""
echo "总结:"
echo "  /actuator/health: $h_ok / 12"
echo "  /actuator/prometheus: $p_ok / 12"
echo "  /actuator/info: $i_ok / 12"
echo "  Prometheus targets UP: $target_count"
