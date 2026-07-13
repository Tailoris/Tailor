# Sentinel 流控规则导入脚本
# 对应: Phase 1 / P1-5 / H-06
# 用途: 将 JSON 规则推送到 Sentinel Dashboard / Nacos
# 用法: ./import-sentinel-rules.sh <env: dev|prod>
# 依赖: curl, jq
#
# 注意: 1.8.6 版本的 Sentinel Dashboard 实际路径为
#       /v1/flow/rule, /degrade/rule, /authority/rule, /system/rule (单数)
#       认证方式为 session-cookie, 先 POST /auth/login 拿 cookie
#       应用必须先在 Dashboard 中注册 (Sentinel client 启动时会自动注册)
#       否则 API 会返回 "given ip does not belong to given app"

#!/usr/bin/env bash
set -euo pipefail

ENV=${1:-dev}
SENTINEL_HOST=${SENTINEL_HOST:-localhost:8719}
SENTINEL_USER=${SENTINEL_USER:-sentinel}
SENTINEL_PASS=${SENTINEL_PASS:-sentinel}
RULES_DIR="$(cd "$(dirname "$0")" && pwd)"
COOKIE_JAR=$(mktemp)
trap "rm -f ${COOKIE_JAR}" EXIT

# 要导入规则的应用列表 (与 IP:Port)
# 这些应用需要先在 Sentinel Dashboard 中注册 (Sentinel client 启动时自动注册)
DEFAULT_APP="${DEFAULT_APP:-tailor-is-product}"
DEFAULT_IP="${DEFAULT_IP:-127.0.0.1}"
DEFAULT_PORT="${DEFAULT_PORT:-8720}"

echo "==> 登录 Sentinel Dashboard ${SENTINEL_HOST} (env=${ENV})"
LOGIN_RESP=$(curl -s -c "${COOKIE_JAR}" -X POST \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${SENTINEL_USER}&password=${SENTINEL_PASS}" \
  "${SENTINEL_HOST}/auth/login")
echo "    ${LOGIN_RESP}"
echo "${LOGIN_RESP}" | grep -q '"success":true' || {
  echo "❌ 登录失败, 请检查 AUTH_USERNAME/AUTH_PASSWORD"
  exit 1
}

# 推送单条规则 (Sentinel 1.8.6 单条新增 API)
push_rule_one() {
  local label=$1
  local path=$2
  local file=$3
  echo "  - ${label}  (${path})"
  local tmp
  tmp=$(mktemp)
  jq --arg app "${DEFAULT_APP}" \
     --arg ip "${DEFAULT_IP}" \
     --argjson port "${DEFAULT_PORT}" \
     '. + {app: $app, ip: $ip, port: $port}' \
     "${RULES_DIR}/${file}" > "${tmp}"
  RESP=$(curl -s -b "${COOKIE_JAR}" -X POST \
    -H "Content-Type: application/json" \
    --data @"${tmp}" \
    "${SENTINEL_HOST}${path}")
  rm -f "${tmp}"
  echo "${RESP}" | jq -c '.success,.msg' 2>/dev/null || echo "${RESP}"
}

# 推送规则 (使用 /rules 批量接口)
push_rule_batch() {
  local label=$1
  local path=$2
  local file=$3
  echo "  - ${label}  (${path})"
  local tmp
  tmp=$(mktemp)
  jq --arg app "${DEFAULT_APP}" \
     --arg ip "${DEFAULT_IP}" \
     --argjson port "${DEFAULT_PORT}" \
     'map(. + {app: $app, ip: $ip, port: $port})' \
     "${RULES_DIR}/${file}" > "${tmp}"
  RESP=$(curl -s -b "${COOKIE_JAR}" -X POST \
    -H "Content-Type: application/json" \
    --data @"${tmp}" \
    "${SENTINEL_HOST}${path}")
  rm -f "${tmp}"
  echo "${RESP}" | jq -c '.success,.msg' 2>/dev/null || echo "${RESP}"
}

echo "==> 目标应用: ${DEFAULT_APP} @ ${DEFAULT_IP}:${DEFAULT_PORT}"
echo "    (如果应用尚未在 Dashboard 中注册, 推送会失败: given ip does not belong to given app)"

# 流控规则
push_rule_batch "flow-rules"    "/v1/flow/rules"     flow-rules.json

# 熔断规则
push_rule_batch "degrade-rules" "/degrade/rules"     degrade-rules.json

# 授权规则
push_rule_batch "auth-rules"    "/authority/rules"   auth-rules.json

# 系统规则
push_rule_batch "system-rules"  "/system/rules"      system-rules.json

echo "==> 全部规则推送完成"
echo ""
echo "访问 http://${SENTINEL_HOST} (sentinel/sentinel) 查看规则生效情况"
