# Sentinel 流控规则导入脚本
# 对应: Phase 1 / P1-5 / H-06
# 用途: 将 JSON 规则推送到 Sentinel Dashboard / Nacos
# 用法: ./import-sentinel-rules.sh <env: dev|prod>
# 依赖: curl, jq

#!/usr/bin/env bash
set -euo pipefail

ENV=${1:-dev}
SENTINEL_HOST=${SENTINEL_HOST:-localhost:8719}
SENTINEL_USER=${SENTINEL_USER:-sentinel}
SENTINEL_PASS=${SENTINEL_PASS:-sentinel}
RULES_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "==> 导入 Sentinel 规则到 ${SENTINEL_HOST} (env=${ENV})"

# 流控规则
echo "  - flow-rules"
curl -s -u "${SENTINEL_USER}:${SENTINEL_PASS}" \
  -X POST \
  -H "Content-Type: application/json" \
  --data @"${RULES_DIR}/flow-rules.json" \
  "${SENTINEL_HOST}/api/flow/rules" | jq .

# 熔断规则
echo "  - degrade-rules"
curl -s -u "${SENTINEL_USER}:${SENTINEL_PASS}" \
  -X POST \
  -H "Content-Type: application/json" \
  --data @"${RULES_DIR}/degrade-rules.json" \
  "${SENTINEL_HOST}/api/degrade/rules" | jq .

# 授权规则
echo "  - auth-rules"
curl -s -u "${SENTINEL_USER}:${SENTINEL_PASS}" \
  -X POST \
  -H "Content-Type: application/json" \
  --data @"${RULES_DIR}/auth-rules.json" \
  "${SENTINEL_HOST}/api/authority/rules" | jq .

# 系统规则
echo "  - system-rules"
curl -s -u "${SENTINEL_USER}:${SENTINEL_PASS}" \
  -X POST \
  -H "Content-Type: application/json" \
  --data @"${RULES_DIR}/system-rules.json" \
  "${SENTINEL_HOST}/api/system/rules" | jq .

echo "==> 全部规则导入完成"
echo ""
echo "访问 http://${SENTINEL_HOST} 查看规则生效情况"
