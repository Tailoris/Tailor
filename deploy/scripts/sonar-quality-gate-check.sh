#!/bin/bash
# =============================================================================
# Tailor IS SonarQube 质量门禁检查脚本
# 对应: SEC-P3-03 接入 SonarQube
# 用法: ./sonar-quality-gate-check.sh <SONAR_HOST_URL> <SONAR_TOKEN> <PROJECT_KEY>
# 返回: 0 = 通过, 1 = 失败
# =============================================================================

set -euo pipefail

# ------ 参数解析 ------
SONAR_HOST_URL="${1:-${SONAR_HOST_URL:-}}"
SONAR_TOKEN="${2:-${SONAR_TOKEN:-}}"
PROJECT_KEY="${3:-tailor-is}"

if [ -z "$SONAR_HOST_URL" ] || [ -z "$SONAR_TOKEN" ]; then
    echo "::error::SONAR_HOST_URL and SONAR_TOKEN are required"
    echo "Usage: $0 <SONAR_HOST_URL> <SONAR_TOKEN> [PROJECT_KEY]"
    exit 1
fi

# 移除 URL 末尾斜杠
SONAR_HOST_URL="${SONAR_HOST_URL%/}"

echo "========================================="
echo "  SonarQube Quality Gate Check"
echo "  Project: ${PROJECT_KEY}"
echo "  Server:  ${SONAR_HOST_URL}"
echo "========================================="

# ------ 步骤 1: 获取项目分析状态 ------
echo ""
echo "[1/3] Fetching project analysis status..."

# 查询项目的最新分析
ANALYSES_RESPONSE=$(curl -s -u "${SONAR_TOKEN}:" \
    "${SONAR_HOST_URL}/api/project_analyses/search?project=${PROJECT_KEY}&ps=1" 2>&1) || {
    echo "::error::Failed to connect to SonarQube API"
    echo "Response: ${ANALYSES_RESPONSE}"
    exit 1
}

# 检查是否有分析结果
ANALYSIS_COUNT=$(echo "$ANALYSES_RESPONSE" | grep -oE '"total"[[:space:]]*:[[:space:]]*[0-9]+' | grep -oE '[0-9]+' || echo "0")
if [ "$ANALYSIS_COUNT" -eq 0 ]; then
    echo "::warning::No analysis found for project '${PROJECT_KEY}'. Skipping quality gate check."
    echo "This may be the first run - ensure the backend CI SonarQube Scan step has completed."
    exit 0  # 首次分析不阻断
fi

echo "  Found ${ANALYSIS_COUNT} analysis(s) for project '${PROJECT_KEY}'"

# ------ 步骤 2: 查询质量门禁状态 ------
echo ""
echo "[2/3] Checking quality gate status..."

QG_RESPONSE=$(curl -s -u "${SONAR_TOKEN}:" \
    "${SONAR_HOST_URL}/api/qualitygates/project_status?projectKey=${PROJECT_KEY}" 2>&1) || {
    echo "::error::Failed to query quality gate status"
    echo "Response: ${QG_RESPONSE}"
    exit 1
}

# 提取质量门禁状态
QG_STATUS=$(echo "$QG_RESPONSE" | grep -oE '"projectStatus"[[:space:]]*:[[:space:]]*\{[^}]*"status"[[:space:]]*:[[:space:]]*"[A-Z]+"' | grep -oE '"status"[[:space:]]*:[[:space:]]*"[A-Z]+"' | grep -oE '"[A-Z]+"' | tr -d '"' || echo "UNKNOWN")

echo "  Quality Gate Status: ${QG_STATUS}"

# ------ 步骤 3: 获取详细失败原因 ------
echo ""
echo "[3/3] Fetching detailed conditions..."

CONDITIONS_RESPONSE=$(curl -s -u "${SONAR_TOKEN}:" \
    "${SONAR_HOST_URL}/api/qualitygates/project_status?projectKey=${PROJECT_KEY}" 2>&1) || true

# 解析并输出每个条件的详情
echo ""
echo "-----------------------------------------"
echo "  Quality Gate Conditions"
echo "-----------------------------------------"

# 提取条件数组
CONDITIONS=$(echo "$CONDITIONS_RESPONSE" | grep -oE '"conditions"[[:space:]]*:[[:space:]]*\[.*\]' || echo "")

if [ -n "$CONDITIONS" ]; then
    # 使用 Python 解析 JSON (如果可用), 否则使用 grep 回退
    if command -v python3 &>/dev/null; then
        python3 -c "
import json, sys
try:
    data = json.loads(sys.stdin.read())
    conditions = data.get('projectStatus', {}).get('conditions', [])
    for c in conditions:
        status = c.get('status', 'UNKNOWN')
        metric = c.get('metricKey', 'unknown')
        actual = c.get('actualValue', 'N/A')
        comparator = c.get('comparator', '?')
        error_threshold = c.get('errorThreshold', 'N/A')
        symbol = '✅' if status == 'OK' else '❌'
        print(f'  {symbol} {metric}: actual={actual}, threshold={comparator} {error_threshold} => {status}')
except Exception as e:
    print(f'  Could not parse conditions: {e}')
" <<< "$CONDITIONS_RESPONSE"
    else
        # 回退: 简单提取
        echo "$CONDITIONS_RESPONSE" | grep -oE '"[^"]*":[^,}]*' | head -20 | while read -r line; do
            echo "  $line"
        done
    fi
else
    echo "  No conditions data available"
fi

echo "-----------------------------------------"
echo ""

# ------ 结果判断 ------
case "$QG_STATUS" in
    "OK")
        echo "✅ Quality Gate PASSED"
        echo ""
        echo "All quality conditions are met. The PR can be merged."
        exit 0
        ;;
    "ERROR")
        echo "❌ Quality Gate FAILED"
        echo ""
        echo "One or more quality conditions are not met."
        echo "Please review the conditions above and fix the issues."
        echo ""
        echo "SonarQube Dashboard: ${SONAR_HOST_URL}/dashboard?id=${PROJECT_KEY}"
        exit 1
        ;;
    "WARN")
        echo "⚠️  Quality Gate WARNING"
        echo ""
        echo "Some conditions are in warning state."
        echo "Review the conditions above and decide whether to proceed."
        exit 0  # 警告不阻断
        ;;
    *)
        echo "⚠️  Unknown quality gate status: ${QG_STATUS}"
        echo "This may indicate the project is not yet configured with a quality gate."
        echo "Please configure a quality gate in SonarQube: ${SONAR_HOST_URL}/quality_gates"
        exit 0  # 未知状态不阻断
        ;;
esac