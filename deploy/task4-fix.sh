#!/bin/bash
exec 2>&1
echo "=== 任务 4 修复: Prometheus + Grafana 权限问题 ==="

echo "--- 步骤 1: 停止并删除容器 ---"
docker stop tailor-is-prometheus tailor-is-grafana 2>&1 | head -3
docker rm tailor-is-prometheus tailor-is-grafana 2>&1 | head -3

echo ""
echo "--- 步骤 2: 修复目录权限（通过 Docker）---"
# 使用 root 容器修复权限
docker run --rm \
    -v /opt/tailor-is/prometheus:/dest \
    alpine:latest \
    sh -c "
        chown -R 65534:65534 /dest 2>&1
        chmod -R 777 /dest
        ls -la /dest/ | head -10
    " 2>&1

docker run --rm \
    -v /opt/tailor-is/grafana:/dest \
    alpine:latest \
    sh -c "
        chown -R 472:472 /dest 2>&1
        chmod -R 777 /dest
        ls -la /dest/ | head -10
    " 2>&1

# Prometheus 使用 nobody 用户（uid 65534），Grafana 使用 grafana 用户（uid 472）
echo ""
echo "--- 步骤 3: 重新启动 Prometheus ---"
docker run -d \
    --name tailor-is-prometheus \
    --network host \
    --restart unless-stopped \
    -v /opt/tailor-is/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro \
    -v /opt/tailor-is/prometheus/alerts.yml:/etc/prometheus/alerts.yml:ro \
    -v /opt/tailor-is/prometheus/data:/prometheus \
    -u "65534:65534" \
    prom/prometheus:v2.50.0 \
    --config.file=/etc/prometheus/prometheus.yml \
    --storage.tsdb.path=/prometheus \
    --web.enable-lifecycle 2>&1

echo ""
echo "--- 步骤 4: 重新启动 Grafana ---"
docker run -d \
    --name tailor-is-grafana \
    --network host \
    --restart unless-stopped \
    -e "GF_SECURITY_ADMIN_USER=admin" \
    -e "GF_SECURITY_ADMIN_PASSWORD=TailorIS2026@Grafana" \
    -e "GF_USERS_ALLOW_SIGN_UP=false" \
    -v /opt/tailor-is/grafana:/var/lib/grafana \
    -u "472:472" \
    grafana/grafana:10.4.0 2>&1

echo ""
echo "--- 步骤 5: 等待服务启动 ---"
sleep 15

echo ""
echo "--- 步骤 6: 验证状态 ---"
docker ps | grep -E "tailor-is-(prometheus|grafana)"

echo ""
echo "--- 步骤 7: 端口检查 ---"
for p in 9090 3000; do
    cnt=$(ss -tln 2>/dev/null | grep -c ":${p} ")
    echo "  port ${p}: ${cnt}"
done

echo ""
echo "--- 步骤 8: 验证 Prometheus ---"
for i in {1..20}; do
    code=$(curl -s -o /dev/null -w "%{http_code}" --noproxy '*' --max-time 3 http://localhost:9090/ 2>/dev/null)
    if [ "$code" = "200" ] || [ "$code" = "302" ]; then
        echo "  Prometheus 启动 (HTTP $code, 尝试 $i/20)"
        break
    fi
    sleep 2
done

echo ""
echo "--- 步骤 9: 验证 Grafana ---"
for i in {1..20}; do
    code=$(curl -s -o /dev/null -w "%{http_code}" --noproxy '*' --max-time 3 http://localhost:3000/ 2>/dev/null)
    if [ "$code" = "200" ] || [ "$code" = "302" ]; then
        echo "  Grafana 启动 (HTTP $code, 尝试 $i/20)"
        break
    fi
    sleep 2
done

echo ""
echo "--- 步骤 10: 测试 Prometheus Targets API ---"
sleep 10
targets=$(curl -s --noproxy '*' --max-time 5 'http://localhost:9090/api/v1/targets' 2>&1 | head -c 300)
echo "  Targets: $targets"

echo ""
echo "=== 任务 4 修复完成 ==="
echo "访问地址:"
echo "  Prometheus: http://localhost:9090"
echo "  Grafana:    http://localhost:3000 (admin / TailorIS2026@Grafana)"
