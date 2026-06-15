#!/bin/bash
# ================================================================
# Tailor IS - 监控告警栈 一键启动脚本
# ================================================================
# 功能:
#   1) 检查并加载告警通知渠道配置（钉钉/飞书/企微/邮件）
#   2) 构建 alert-webhook Docker 镜像
#   3) 通过 docker compose 启动 prometheus / alertmanager / grafana / alert-webhook
#   4) 等待健康检查通过后输出各服务访问地址
#
# 前置条件:
#   - Docker & Docker Compose v2+ 已安装
#   - 各渠道 webhook 已配置（见下方环境变量）
#
# 使用:
#   cd deploy
#   bash start-monitoring.sh
# ================================================================

set -e
COMPOSE_FILE="docker-compose-monitoring.yml"

echo "================================================================"
echo "  Tailor IS - 启动 Prometheus + Alertmanager + Grafana + alert-webhook"
echo "================================================================"
echo ""

# -------- 1) 环境变量检查 & 提示 --------
echo "[1/4] 检查告警通知渠道配置 ..."

check_env() {
  local name="$1"
  local val="${!name}"
  if [ -n "$val" ]; then
    echo "  ✅ $name = 已配置"
  else
    echo "  ⚠️  $name = 未配置 (对应渠道将被跳过)"
  fi
}

# 若 ./.env.monitoring 存在则加载
if [ -f ./.env.monitoring ]; then
  set -a; source ./.env.monitoring; set +a
  echo "  已加载 ./.env.monitoring 中的环境变量"
fi

check_env DINGTALK_WEBHOOK
check_env DINGTALK_SECRET
check_env FEISHU_WEBHOOK
check_env WECOM_WEBHOOK
check_env RESEND_API_KEY
check_env ALERT_TO_EMAIL
check_env GRAFANA_PASSWORD

# 至少有一个渠道才能算“有告警”
has_channel=false
[ -n "$DINGTALK_WEBHOOK" ] && has_channel=true
[ -n "$FEISHU_WEBHOOK" ] && has_channel=true
[ -n "$WECOM_WEBHOOK" ] && has_channel=true
[ -n "$RESEND_API_KEY" ] && [ -n "$ALERT_TO_EMAIL" ] && has_channel=true

if [ "$has_channel" = "false" ]; then
  echo ""
  echo "  ⚠️  ⚠️  ⚠️  警告：当前未配置任何通知渠道"
  echo "  请在运行脚本前设置至少一个（推荐邮件 + 飞书/钉钉）："
  echo "      export DINGTALK_WEBHOOK='https://...'"
  echo "      export FEISHU_WEBHOOK='https://...'"
  echo "      export RESEND_API_KEY='re_xxx'"
  echo "      export ALERT_TO_EMAIL='ops@tailoris.com'"
  echo ""
  echo "  或在 ./deploy/.env.monitoring 中写入以上变量，下次将自动加载"
fi
echo ""

# -------- 2) 构建 alert-webhook 镜像 --------
echo "[2/4] 构建 alert-webhook Docker 镜像 ..."
docker build -t tailor-is-alert-webhook:latest ./alert-webhook 2>&1 | tail -5
echo "  ✅ alert-webhook 镜像构建完成"
echo ""

# -------- 3) 启动监控栈 --------
echo "[3/4] docker compose up -d ..."
docker compose -f "$COMPOSE_FILE" up -d 2>&1 | tail -10
echo "  ✅ 所有服务已启动"
echo ""

# -------- 4) 健康检查 & 输出访问地址 --------
echo "[4/4] 健康检查 ..."
wait_for_http() {
  local url="$1"
  local name="$2"
  for i in $(seq 1 20); do
    code=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "000")
    if [ "$code" = "200" ]; then
      echo "  ✅ $name -> $url (HTTP 200)"
      return 0
    fi
    sleep 2
  done
  echo "  ❌ $name -> $url 未在 40s 内返回 200"
  return 1
}

wait_for_http "http://127.0.0.1:9090/-/healthy" "Prometheus"
wait_for_http "http://127.0.0.1:9093/-/healthy"  "Alertmanager"
wait_for_http "http://127.0.0.1:3000/api/health"  "Grafana"
wait_for_http "http://127.0.0.1:8080/health"      "alert-webhook"

echo ""
echo "================================================================"
echo "  ✅ 监控告警栈启动完成"
echo "================================================================"
echo "  Prometheus    : http://127.0.0.1:9090/alerts"
echo "  Alertmanager  : http://127.0.0.1:9093/#/alerts"
echo "  Grafana       : http://127.0.0.1:3000  (admin / ${GRAFANA_PASSWORD:-TailorIS2026@Grafana})"
echo "  alert-webhook : http://127.0.0.1:8080/health"
echo ""
echo "  模拟告警触发（可选）: "
echo "    curl -X POST http://127.0.0.1:9093/-/api/v2/alerts -H 'Content-Type: application/json' \\"
echo "      -d '[{\"status\":\"firing\",\"labels\":{\"alertname\":\"TestAlert\",\"severity\":\"critical\",\"service\":\"tailor-is-user\"},\"annotations\":{\"summary\":\"测试告警\"}}]'"
echo ""
echo "================================================================"
