#!/bin/bash
exec 2>&1
echo "=== 任务 4: Prometheus 监控部署 - $(date) ==="

# 配置所有 12 个微服务的 Prometheus 抓取目标
PROM_CONFIG=/opt/tailor-is/prometheus/prometheus.yml
PROM_DIR=/opt/tailor-is/prometheus
PROM_DATA=/opt/tailor-is/prometheus/data

# 服务配置
declare -A SERVICES
SERVICES["gateway"]="8081"
SERVICES["user"]="8101"
SERVICES["merchant"]="8102"
SERVICES["product"]="8103"
SERVICES["order"]="8104"
SERVICES["payment"]="8105"
SERVICES["marketing"]="8106"
SERVICES["ai"]="8107"
SERVICES["copyright"]="8108"
SERVICES["community"]="8109"
SERVICES["supply"]="8110"
SERVICES["message"]="8111"

echo "--- 步骤 1: 创建 prometheus 配置目录 ---"
mkdir -p $PROM_DIR
mkdir -p $PROM_DATA

echo ""
echo "--- 步骤 2: 生成 prometheus.yml ---"
cat > /tmp/prometheus.yml << 'PROMEOF'
# Prometheus Configuration for Tailor IS
# Generated: $(date)

global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    monitor: 'tailor-is-monitor'
    env: 'production'

scrape_configs:
  # Prometheus self-monitoring
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
        labels:
          service: 'prometheus'
          env: 'production'

  # Tailor IS Microservices
  - job_name: 'tailor-is-microservices'
    metrics_path: '/actuator/prometheus'
    scheme: 'http'
    scrape_interval: 15s
    scrape_timeout: 10s
    static_configs:
      - targets:
PROMEOF

# 添加所有服务到 targets
for svc in "${!SERVICES[@]}"; do
    port=${SERVICES[$svc]}
    echo "        - 'localhost:${port}'" >> /tmp/prometheus.yml
done

cat >> /tmp/prometheus.yml << 'PROMEOF'
    labels:
      group: 'tailor-is'
      env: 'production'

# Separated by service for better granularity
  - job_name: 'tailor-is-services'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
    static_configs:
PROMEOF

for svc in "${!SERVICES[@]}"; do
    port=${SERVICES[$svc]}
    cat >> /tmp/prometheus.yml << PROMOEF
      - targets: ['localhost:${port}']
        labels:
          service: 'tailor-is-${svc}'
          port: '${port}'
          env: 'production'
PROMEOF
done

# 通过 Docker 复制
docker run --rm \
    -v /tmp:/src:ro \
    -v $PROM_DIR:/dest \
    alpine:latest \
    sh -c "cp /src/prometheus.yml /dest/prometheus.yml && ls -la /dest/prometheus.yml" 2>&1

echo ""
echo "--- 步骤 3: 生成告警规则 ---"
cat > /tmp/alerts.yml << 'ALERTEOF'
# Prometheus Alert Rules for Tailor IS
groups:
  - name: tailor_is_alerts
    interval: 30s
    rules:
      # 服务可用性
      - alert: ServiceDown
        expr: up{job="tailor-is-services"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "服务 {{ \$labels.service }} 已下线"
          description: "服务 {{ \$labels.service }} 在端口 {{ \$labels.port }} 不可达超过 1 分钟"

      # 高错误率
      - alert: HighErrorRate
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (service)
          /
          sum(rate(http_server_requests_seconds_count[5m])) by (service)
          > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "{{ \$labels.service }} 错误率超过 5%"

      # 慢响应
      - alert: SlowResponse
        expr: |
          histogram_quantile(0.95,
            sum(rate(http_server_requests_seconds_bucket[5m])) by (service, le)
          ) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "{{ \$labels.service }} P95 响应时间超过 1s"

      # JVM 内存
      - alert: HighJVMMemory
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "{{ \$labels.service }} JVM 堆内存使用率 > 85%"

      # 数据库连接池
      - alert: DBPoolExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 3m
        labels:
          severity: warning
        annotations:
          summary: "{{ \$labels.service }} 数据库连接池使用率 > 90%"
ALERTEOF

docker run --rm \
    -v /tmp:/src:ro \
    -v $PROM_DIR:/dest \
    alpine:latest \
    sh -c "cp /src/alerts.yml /dest/alerts.yml && ls -la /dest/" 2>&1

echo ""
echo "--- 步骤 4: 验证配置文件 ---"
echo "prometheus.yml 行数: $(docker run --rm -v $PROM_DIR:/src:ro alpine:latest wc -l /src/prometheus.yml 2>&1 | head -1)"
echo "alerts.yml 行数: $(docker run --rm -v $PROM_DIR:/src:ro alpine:latest wc -l /src/alerts.yml 2>&1 | head -1)"

echo ""
echo "--- 步骤 5: 生成 Grafana 仪表盘配置 ---"
GRAFANA_DIR=/opt/tailor-is/grafana
mkdir -p $GRAFANA_DIR/dashboards

cat > /tmp/dashboard.json << 'DASHEOF'
{
  "dashboard": {
    "id": null,
    "title": "Tailor IS - 微服务监控",
    "tags": ["tailor-is", "microservice"],
    "timezone": "browser",
    "schemaVersion": 30,
    "version": 1,
    "refresh": "30s",
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "panels": [
      {
        "id": 1,
        "title": "服务可用性",
        "type": "stat",
        "gridPos": {"h": 6, "w": 24, "x": 0, "y": 0},
        "targets": [
          {
            "expr": "up{job='tailor-is-services'}",
            "legendFormat": "{{service}}"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "color": {"mode": "thresholds"},
            "thresholds": {
              "mode": "absolute",
              "steps": [
                {"color": "red", "value": null},
                {"color": "green", "value": 1}
              ]
            },
            "mappings": [
              {"options": {"0": {"text": "DOWN"}}, "type": "value"},
              {"options": {"1": {"text": "UP"}}, "type": "value"}
            ]
          }
        }
      },
      {
        "id": 2,
        "title": "P95 响应时间",
        "type": "timeseries",
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 6},
        "targets": [
          {
            "expr": "histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (service, le))",
            "legendFormat": "{{service}}"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "s",
            "custom": {"drawStyle": "line", "lineWidth": 2}
          }
        }
      },
      {
        "id": 3,
        "title": "QPS (每秒请求数)",
        "type": "timeseries",
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 6},
        "targets": [
          {
            "expr": "sum(rate(http_server_requests_seconds_count[1m])) by (service)",
            "legendFormat": "{{service}}"
          }
        ]
      },
      {
        "id": 4,
        "title": "JVM 堆内存使用",
        "type": "timeseries",
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 14},
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{area='heap'} / jvm_memory_max_bytes{area='heap'}",
            "legendFormat": "{{service}}"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "percentunit",
            "max": 1,
            "min": 0
          }
        }
      },
      {
        "id": 5,
        "title": "数据库连接池",
        "type": "timeseries",
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 14},
        "targets": [
          {
            "expr": "hikaricp_connections_active",
            "legendFormat": "{{service}} - active"
          },
          {
            "expr": "hikaricp_connections_idle",
            "legendFormat": "{{service}} - idle"
          }
        ]
      }
    ]
  }
}
DASHEOF

docker run --rm \
    -v /tmp:/src:ro \
    -v $GRAFANA_DIR/dashboards:/dest \
    alpine:latest \
    sh -c "cp /src/dashboard.json /dest/tailor-is-dashboard.json && ls -la /dest/" 2>&1

echo ""
echo "--- 步骤 6: 生成部署配置 ---"
cat > /tmp/docker-compose-monitoring.yml << 'COMPOSEEOF'
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:v2.50.0
    container_name: tailor-is-prometheus
    ports:
      - "9090:9090"
    volumes:
      - /opt/tailor-is/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - /opt/tailor-is/prometheus/alerts.yml:/etc/prometheus/alerts.yml:ro
      - /opt/tailor-is/prometheus/data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/usr/share/prometheus/console_libraries'
      - '--web.console.templates=/usr/share/consoles'
      - '--web.enable-lifecycle'
    networks:
      - tailor-mon
    restart: unless-stopped

  grafana:
    image: grafana/grafana:10.4.0
    container_name: tailor-is-grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=TailorIS2026@Grafana
      - GF_USERS_ALLOW_SIGN_UP=false
    volumes:
      - /opt/tailor-is/grafana:/var/lib/grafana
    networks:
      - tailor-mon
    restart: unless-stopped

  alertmanager:
    image: prom/alertmanager:v0.27.0
    container_name: tailor-is-alertmanager
    ports:
      - "9093:9093"
    networks:
      - tailor-mon
    restart: unless-stopped

networks:
  tailor-mon:
    driver: bridge
COMPOSEEOF

# 通过 Docker 复制到目标位置
docker run --rm \
    -v /tmp:/src:ro \
    -v $PROM_DIR:/dest \
    alpine:latest \
    sh -c "cp /src/docker-compose-monitoring.yml /dest/docker-compose-monitoring.yml && ls -la /dest/" 2>&1

echo ""
echo "=== 任务 4 配置完成 ==="
echo "配置文件已生成到: $PROM_DIR"
echo "Grafana 仪表盘: $GRAFANA_DIR/dashboards/tailor-is-dashboard.json"
echo ""
echo "启动命令: cd $PROM_DIR && docker compose -f docker-compose-monitoring.yml up -d"
echo ""
echo "访问地址:"
echo "  Prometheus: http://localhost:9090"
echo "  Grafana: http://localhost:3000 (admin / TailorIS2026@Grafana)"
echo "  AlertManager: http://localhost:9093"
