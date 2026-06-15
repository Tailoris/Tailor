#!/bin/bash
exec 2>&1
echo "=== 任务 4: Prometheus 部署 - $(date) ==="

PROM_DIR=/opt/tailor-is/prometheus
GRAFANA_DIR=/opt/tailor-is/grafana
DASH_DIR=/opt/tailor-is/grafana/dashboards

# 创建目录
echo "--- 步骤 1: 创建目录 ---"
docker run --rm -v /opt/tailor-is:/opt alpine:latest sh -c "
    mkdir -p /opt/tailor-is/prometheus/data
    mkdir -p /opt/tailor-is/prometheus/dashboards
    mkdir -p /opt/tailor-is/grafana
    echo '目录创建完成'
"

# 复制配置文件
echo ""
echo "--- 步骤 2: 复制配置 ---"
docker run --rm \
    -v "/mnt/host/f/Tailor/Tailor is/deploy":/src:ro \
    -v /opt/tailor-is/prometheus:/dest \
    alpine:latest sh -c "
        cp /src/prometheus.yml /dest/prometheus.yml
        cp /src/alerts.yml /dest/alerts.yml
        cp /src/docker-compose-monitoring.yml /dest/docker-compose-monitoring.yml
        ls -la /dest/
    " 2>&1

# 复制 Grafana 仪表盘
mkdir -p /tmp/dashboards
cp "/mnt/host/f/Tailor/Tailor is/deploy/dashboard.json" /tmp/dashboards/ 2>/dev/null
docker run --rm \
    -v "/mnt/host/f/Tailor/Tailor is/deploy":/src:ro \
    -v /opt/tailor-is/grafana/dashboards:/dest \
    alpine:latest sh -c "
        if [ -f /src/dashboard.json ]; then
            cp /src/dashboard.json /dest/tailor-is-dashboard.json
        fi
        ls -la /dest/ 2>&1
    " 2>&1

# 验证配置
echo ""
echo "--- 步骤 3: 验证配置 ---"
echo "prometheus.yml 行数: $(docker run --rm -v /opt/tailor-is/prometheus:/src:ro alpine:latest wc -l /src/prometheus.yml 2>&1 | head -1)"
echo "alerts.yml 行数: $(docker run --rm -v /opt/tailor-is/prometheus:/src:ro alpine:latest wc -l /src/alerts.yml 2>&1 | head -1)"

# 启动 Prometheus
echo ""
echo "--- 步骤 4: 启动 Prometheus + Grafana ---"
cd /opt/tailor-is/prometheus
docker compose -f docker-compose-monitoring.yml up -d 2>&1
sleep 20

# 验证
echo ""
echo "--- 步骤 5: 验证监控服务 ---"
docker ps | grep -E "prometheus|grafana"

echo ""
echo "--- 步骤 6: 端口检查 ---"
for p in 9090 3000; do
    cnt=$(ss -tln 2>/dev/null | grep -c ":${p} ")
    echo "  port ${p}: ${cnt}"
done

echo ""
echo "--- 步骤 7: 验证 Prometheus 抓取目标 ---"
# 等待 Prometheus 启动
for i in {1..15}; do
    code=$(curl -s -o /dev/null -w "%{http_code}" --noproxy '*' --max-time 3 http://localhost:9090/ 2>/dev/null)
    if [ "$code" = "200" ] || [ "$code" = "302" ]; then
        echo "  Prometheus 启动 (HTTP $code, 尝试 $i/15)"
        break
    fi
    sleep 2
done

# 测试 targets API
targets=$(curl -s --noproxy '*' --max-time 5 'http://localhost:9090/api/v1/targets' 2>&1 | head -c 200)
echo "  Targets 响应: $targets" | head -1

echo ""
echo "--- 步骤 8: 验证 Grafana ---"
grafana_code=$(curl -s -o /dev/null -w "%{http_code}" --noproxy '*' --max-time 5 http://localhost:3000/ 2>/dev/null)
echo "  Grafana 主页: HTTP $grafana_code"

echo ""
echo "=== 任务 4 完成 ==="
echo ""
echo "监控访问地址:"
echo "  Prometheus: http://localhost:9090"
echo "  Grafana: http://localhost:3000 (admin / TailorIS2026@Grafana)"
