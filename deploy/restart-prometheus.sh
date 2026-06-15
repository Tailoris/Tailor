#!/bin/bash
# =============================================================
# Prometheus + Alertmanager 容器重启脚本
# =============================================================
# 用途: 应用新的 prometheus.yml (含 Alertmanager 集成 + gateway 8080)
# 使用: bash deploy/restart-prometheus.sh
# =============================================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DEPLOY_DIR"

log() { echo -e "${GREEN}[$(date +%H:%M:%S)]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
err() { echo -e "${RED}[ERR]${NC} $1"; }

# =============================================================
# 前置检查
# =============================================================
log "1. 前置检查"

# 检查 docker
if ! command -v docker >/dev/null 2>&1; then
    err "Docker 未安装"
    exit 1
fi

# 检查配置文件存在
for f in prometheus.yml alerts.yml alertmanager.yml docker-compose-monitoring.yml; do
    if [ ! -f "$f" ]; then
        err "配置文件不存在: $f"
        exit 1
    fi
done

# 检查 alertmanager 目录
mkdir -p data/alertmanager grafana dashboards templates
[ -f templates/alert.tmpl ] || cat > templates/alert.tmpl <<'TEMPLATE_EOF'
{{ define "default.title" }}[{{ .Status | toUpper }}] {{ .GroupLabels.alertname }}{{ end }}
{{ define "default.content" }}
{{ range .Alerts }}
**Service:** {{ .Labels.service }}
**Severity:** {{ .Labels.severity }}
**Description:** {{ .Annotations.description }}
{{ end }}
{{ end }}
TEMPLATE_EOF

log "  ✓ 所有配置文件存在"

# =============================================================
# 2. 验证 prometheus 配置
# =============================================================
log "2. 验证 prometheus 配置"

# 简单的 YAML 验证
if grep -q "alerting:" prometheus.yml; then
    log "  ✓ alerting 配置存在"
else
    err "prometheus.yml 缺少 alerting 配置"
    exit 1
fi

if grep -q "localhost:9093" prometheus.yml; then
    log "  ✓ Alertmanager 目标已配置 (localhost:9093)"
else
    warn "Alertmanager 目标未配置"
fi

if grep -q "localhost:8080" prometheus.yml; then
    log "  ✓ Gateway 端口已修正为 8080"
else
    err "Gateway 端口仍为非 8080"
    exit 1
fi

# =============================================================
# 3. 验证 alertmanager 配置
# =============================================================
log "3. 验证 alertmanager 配置"

RECEIVERS=$(grep -c "^  - name:" alertmanager.yml)
if [ "$RECEIVERS" -ge 3 ]; then
    log "  ✓ Alertmanager 接收人数: $RECEIVERS"
else
    err "Alertmanager 接收人过少: $RECEIVERS"
    exit 1
fi

# =============================================================
# 4. 验证告警规则
# =============================================================
log "4. 验证告警规则"

if grep -q "alert: ServiceDown" alerts.yml && \
   grep -q "alert: HighErrorRate" alerts.yml && \
   grep -q "alert: SlowResponse" alerts.yml && \
   grep -q "alert: HighJVMMemory" alerts.yml && \
   grep -q "alert: DBPoolExhausted" alerts.yml; then
    log "  ✓ 5 个核心告警规则全部就绪"
else
    err "告警规则缺失"
    exit 1
fi

# 检查 HighErrorRate 阈值
if grep -A5 "alert: HighErrorRate" alerts.yml | grep -q "> 0.10"; then
    log "  ✓ HighErrorRate 阈值已更新为 10%"
else
    warn "HighErrorRate 阈值非 10%"
fi

# =============================================================
# 5. 备份数据
# =============================================================
log "5. 备份 Prometheus 数据"

if [ -d data ] && [ "$(ls -A data 2>/dev/null)" ]; then
    BACKUP_DIR="data-backup-$(date +%Y%m%d-%H%M%S)"
    log "  创建备份: $BACKUP_DIR"
    cp -r data "$BACKUP_DIR"
    log "  ✓ 备份完成"
else
    log "  无需备份 (首次部署或数据目录为空)"
fi

# =============================================================
# 6. 停止旧容器
# =============================================================
log "6. 停止旧容器"

for c in tailor-is-prometheus tailor-is-alertmanager tailor-is-grafana; do
    if docker ps -a --format '{{.Names}}' | grep -q "^${c}$"; then
        log "  停止 $c ..."
        docker stop "$c" 2>/dev/null || true
        docker rm "$c" 2>/dev/null || true
    fi
done

# =============================================================
# 7. 启动新容器
# =============================================================
log "7. 启动新容器 (含 Alertmanager)"

docker-compose -f docker-compose-monitoring.yml up -d 2>&1

# =============================================================
# 8. 等待启动
# =============================================================
log "8. 等待服务启动"

sleep 10

# =============================================================
# 9. 健康检查
# =============================================================
log "9. 健康检查"

# Prometheus
if curl -s --noproxy '*' --max-time 5 http://localhost:9090/-/healthy >/dev/null 2>&1; then
    log "  ✓ Prometheus 9090 健康"
else
    err "Prometheus 不健康"
    docker logs tailor-is-prometheus --tail 30
    exit 1
fi

# Alertmanager
sleep 5
if curl -s --noproxy '*' --max-time 5 http://localhost:9093/-/healthy >/dev/null 2>&1; then
    log "  ✓ Alertmanager 9093 健康"
else
    warn "Alertmanager 9093 未就绪（可能需等待更长时间）"
fi

# Grafana
if curl -s --noproxy '*' --max-time 5 -o /dev/null -w "%{http_code}" http://localhost:3000/api/health 2>/dev/null | grep -q "200"; then
    log "  ✓ Grafana 3000 健康"
else
    warn "Grafana 3000 未就绪"
fi

# =============================================================
# 10. 验证配置
# =============================================================
log "10. 验证配置加载"

# 检查 Alertmanager 集成
AM_STATUS=$(curl -s --noproxy '*' --max-time 5 http://localhost:9090/api/v1/alertmanagers 2>/dev/null)
if echo "$AM_STATUS" | grep -q "localhost:9093"; then
    log "  ✓ Alertmanager 已注册到 Prometheus"
else
    warn "Alertmanager 未注册到 Prometheus（等待发现）"
fi

# 检查目标数
sleep 15
UP_COUNT=$(curl -s --noproxy '*' --max-time 5 http://localhost:9090/api/v1/targets 2>/dev/null | grep -o '"health":"up"' | wc -l)
TOTAL_COUNT=$(curl -s --noproxy '*' --max-time 5 http://localhost:9090/api/v1/targets 2>/dev/null | grep -o '"health":"' | wc -l)
log "  Targets: $UP_COUNT UP / $TOTAL_COUNT TOTAL"

# =============================================================
# 完成
# =============================================================
echo ""
log "============================================================"
log "  ✅ 重启完成"
log "  Prometheus:    http://localhost:9090"
log "  Alertmanager:  http://localhost:9093"
log "  Grafana:       http://localhost:3000 (admin:admin)"
log "============================================================"
