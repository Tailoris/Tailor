#!/bin/bash
# =============================================================================
# Tailor IS 周度质量报告生成脚本
# -----------------------------------------------------------------------------
# 用法: ./weekly-quality-report.sh [项目根目录]
#
# 功能:
#   1. 统计本周 PR 合并数 (gh CLI)
#   2. 统计测试覆盖率 (JaCoCo XML 报告)
#   3. 统计监控告警数 (Prometheus API)
#   4. 统计安全漏洞数 (gitleaks / trivy 报告)
#   5. 输出 Markdown 格式报告
#
# 依赖(可选, 缺失时对应章节标注 "N/A"):
#   - gh (GitHub CLI)        : PR 统计
#   - python3 / xmllint      : JaCoCo 覆盖率解析
#   - curl                   : Prometheus API 查询
#   - jq                     : JSON 解析
# =============================================================================
set -euo pipefail

# ----------------- 参数与路径 -----------------
PROJECT_ROOT="${1:-$(cd "$(dirname "$0")/../.." && pwd)}"
REPORT_DATE=$(date +%Y-%m-%d)
WEEK_AGO=$(date -d '7 days ago' +%Y-%m-%d 2>/dev/null || date -v-7d +%Y-%m-%d 2>/dev/null || echo "")
REPORT_DIR="${PROJECT_ROOT}/docs/reports"
REPORT_FILE="${REPORT_DIR}/weekly-quality-report-${REPORT_DATE}.md"

mkdir -p "${REPORT_DIR}"

# ----------------- 辅助函数 -----------------
# 检查命令是否存在
has_cmd() { command -v "$1" >/dev/null 2>&1; }

# 安全写入行(避免变量未设置导致 set -u 中断)
write_line() { printf '%s\n' "$*" >> "${REPORT_FILE}"; }

# ----------------- 初始化报告 -----------------
{
  echo "# 周度质量报告 - ${REPORT_DATE}"
  echo ""
  echo "| 项目 | 内容 |"
  echo "|------|------|"
  echo "| 报告日期 | ${REPORT_DATE} |"
  echo "| 统计区间 | ${WEEK_AGO} ~ ${REPORT_DATE} |"
  echo "| 项目根目录 | ${PROJECT_ROOT} |"
  echo ""
} > "${REPORT_FILE}"

# =============================================================================
# 1. PR 统计 (gh CLI)
# =============================================================================
write_line "## 1. PR 统计"
write_line ""

if has_cmd gh; then
  if gh auth status >/dev/null 2>&1; then
    MERGED_PR_COUNT=$(gh pr list \
      --state merged \
      --search "merged:>=${WEEK_AGO}" \
      --json number \
      --jq 'length' 2>/dev/null || echo "0")
    write_line "- 本周合并 PR 数: **${MERGED_PR_COUNT}**"

    # 列出最近合并的 PR (最多 10 条)
    write_line ""
    write_line "### 最近合并的 PR"
    write_line ""
    write_line "| PR | 标题 | 合并时间 |"
    write_line "|----|------|---------|"
    gh pr list --state merged --search "merged:>=${WEEK_AGO}" \
      --json number,title,mergedAt \
      --jq -r '.[] | "| #\(.number) | \(.title) | \(.mergedAt) |"' 2>/dev/null \
      | head -10 >> "${REPORT_FILE}" || true
    write_line ""
  else
    write_line "- 本周合并 PR 数: N/A (gh 未认证, 请运行 \`gh auth login\`)"
  fi
else
  write_line "- 本周合并 PR 数: N/A (未安装 gh CLI)"
fi
write_line ""

# =============================================================================
# 2. 测试覆盖率 (JaCoCo XML 报告)
# =============================================================================
write_line "## 2. 测试覆盖率"
write_line ""

# 查找 JaCoCo XML 报告
JACOCO_XML=$(find "${PROJECT_ROOT}/tailor-is" -path '*/target/site/jacoco/jacoco.xml' 2>/dev/null | head -1 || echo "")

if [ -n "${JACOCO_XML}" ] && [ -f "${JACOCO_XML}" ]; then
  # 优先用 python3 解析 (跨平台), 回退到 grep
  if has_cmd python3; then
    COVERAGE_INFO=$(python3 - <<'PYEOF' "${JACOCO_XML}" 2>/dev/null || echo "PARSE_ERROR")
import sys, xml.etree.ElementTree as ET
try:
    tree = ET.parse(sys.argv[1])
    root = tree.getroot()
    # 汇总所有 counter (BUNDLE/REPORT 级别)
    counters = {}
    for c in root.iter('counter'):
        ctype = c.get('type')
        missed = int(c.get('missed', 0))
        covered = int(c.get('covered', 0))
        total = missed + covered
        if total > 0:
            counters[ctype] = (covered, total, covered * 100.0 / total)
    for ctype in ('LINE', 'BRANCH', 'INSTRUCTION', 'METHOD', 'CLASS'):
        if ctype in counters:
            cov, tot, pct = counters[ctype]
            print(f"{ctype}|{cov}|{tot}|{pct:.2f}")
except Exception as e:
    print("PARSE_ERROR", file=sys.stderr)
    sys.exit(1)
PYEOF
    if [ "${COVERAGE_INFO}" != "PARSE_ERROR" ] && [ -n "${COVERAGE_INFO}" ]; then
      write_line "| 指标 | 覆盖 | 总数 | 覆盖率 |"
      write_line "|------|------|------|--------|"
      echo "${COVERAGE_INFO}" | while IFS='|' read -r ctype cov tot pct; do
        write_line "| ${ctype} | ${cov} | ${tot} | ${pct}% |"
      done
      write_line ""
      write_line "- 报告来源: \`${JACOCO_XML}\`"
    else
      write_line "- 后端覆盖率: 解析失败, 请检查 JaCoCo XML 报告格式"
    fi
  else
    write_line "- 后端覆盖率: N/A (需要 python3 解析 JaCoCo XML)"
  fi
else
  write_line "- 后端覆盖率: N/A (未找到 JaCoCo 报告, 请先运行 \`mvn test jacoco:report\`)"
fi
write_line ""
write_line "- 前端覆盖率: [从 Vitest 报告读取, 需配置 \`--coverage\` 输出]"
write_line ""

# =============================================================================
# 3. 监控告警 (Prometheus API)
# =============================================================================
write_line "## 3. 监控告警"
write_line ""

PROMETHEUS_URL="${PROMETHEUS_URL:-http://localhost:9090}"
if has_cmd curl && has_cmd jq; then
  # 查询过去 7 天触发的告警数 (ALERTS_FOR_STATE metric)
  ALERT_COUNT=$(curl -s --connect-timeout 5 --max-time 10 \
    "${PROMETHEUS_URL}/api/v1/query?query=count(count_over_time(ALERTS{alertstate=\"firing\"}[7d]))" 2>/dev/null \
    | jq -r '.data.result[0].value[1] // "0"' 2>/dev/null || echo "0")

  if [ "${ALERT_COUNT}" != "0" ] && [ -n "${ALERT_COUNT}" ]; then
    write_line "- 本周告警数: **${ALERT_COUNT}**"
  else
    write_line "- 本周告警数: 0 (或 Prometheus 不可达: ${PROMETHEUS_URL})"
  fi

  # 查询当前 firing 状态告警
  FIRING_ALERTS=$(curl -s --connect-timeout 5 --max-time 10 \
    "${PROMETHEUS_URL}/api/v1/query?query=ALERTS{alertstate=\"firing\"}" 2>/dev/null \
    | jq -r '.data.result | length' 2>/dev/null || echo "0")
  write_line "- 当前 Firing 告警数: ${FIRING_ALERTS}"
else
  write_line "- 本周告警数: N/A (需要 curl + jq, 或 Prometheus 未配置)"
  write_line "  - Prometheus URL: ${PROMETHEUS_URL} (可通过 \`PROMETHEUS_URL\` 环境变量覆盖)"
fi
write_line ""

# =============================================================================
# 4. 安全扫描 (gitleaks / trivy)
# =============================================================================
write_line "## 4. 安全扫描"
write_line ""

# gitleaks 报告
GITLEAKS_REPORT=$(find "${PROJECT_ROOT}" -name 'gitleaks*.json' -newermt "${WEEK_AGO}" 2>/dev/null | head -1 || echo "")
if [ -n "${GITLEAKS_REPORT}" ] && [ -f "${GITLEAKS_REPORT}" ] && has_cmd jq; then
  GITLEAKS_COUNT=$(jq 'length' "${GITLEAKS_REPORT}" 2>/dev/null || echo "0")
  write_line "- gitleaks 发现: **${GITLEAKS_COUNT}** 个密钥泄露 (报告: \`${GITLEAKS_REPORT}\`)"
elif has_cmd gitleaks; then
  write_line "- gitleaks 发现: [运行 \`gitleaks detect --source . --report-path gitleaks-report.json\` 获取最新扫描]"
else
  write_line "- gitleaks 发现: [从最新扫描报告读取, 未找到报告或未安装 gitleaks]"
fi

# trivy 报告
TRIVY_REPORT=$(find "${PROJECT_ROOT}" -name 'trivy*.json' -newermt "${WEEK_AGO}" 2>/dev/null | head -1 || echo "")
if [ -n "${TRIVY_REPORT}" ] && [ -f "${TRIVY_REPORT}" ] && has_cmd jq; then
  TRIVY_CRITICAL=$(jq '[.Results[].Vulnerabilities[]? | select(.Severity=="CRITICAL")] | length' "${TRIVY_REPORT}" 2>/dev/null || echo "0")
  TRIVY_HIGH=$(jq '[.Results[].Vulnerabilities[]? | select(.Severity=="HIGH")] | length' "${TRIVY_REPORT}" 2>/dev/null || echo "0")
  TRIVY_MEDIUM=$(jq '[.Results[].Vulnerabilities[]? | select(.Severity=="MEDIUM")] | length' "${TRIVY_REPORT}" 2>/dev/null || echo "0")
  write_line "- Trivy 发现: Critical=${TRIVY_CRITICAL}, High=${TRIVY_HIGH}, Medium=${TRIVY_MEDIUM} (报告: \`${TRIVY_REPORT}\`)"
elif has_cmd trivy; then
  write_line "- Trivy 发现: [运行 \`trivy fs --format json -o trivy-report.json .\` 获取最新扫描]"
else
  write_line "- Trivy 发现: [从最新扫描报告读取, 未找到报告或未安装 trivy]"
fi
write_line ""

# =============================================================================
# 5. CI/CD 状态摘要
# =============================================================================
write_line "## 5. CI/CD 状态摘要"
write_line ""

if has_cmd gh && gh auth status >/dev/null 2>&1; then
  # 最近 7 天 workflow 运行统计
  WORKFLOW_STATS=$(gh run list --limit 100 --json status,conclusion,createdAt \
    --jq "[.[] | select(.createdAt >= \"${WEEK_AGO}\")] | {
      total: length,
      success: [.[] | select(.conclusion==\"success\")] | length,
      failure: [.[] | select(.conclusion==\"failure\")] | length,
      cancelled: [.[] | select(.conclusion==\"cancelled\")] | length
    }" 2>/dev/null || echo "")

  if [ -n "${WORKFLOW_STATS}" ] && has_cmd jq; then
    TOTAL=$(echo "${WORKFLOW_STATS}" | jq -r '.total // 0')
    SUCCESS=$(echo "${WORKFLOW_STATS}" | jq -r '.success // 0')
    FAILURE=$(echo "${WORKFLOW_STATS}" | jq -r '.failure // 0')
    CANCELLED=$(echo "${WORKFLOW_STATS}" | jq -r '.cancelled // 0')
    write_line "| 指标 | 数值 |"
    write_line "|------|------|"
    write_line "| 总运行数 | ${TOTAL} |"
    write_line "| 成功 | ${SUCCESS} |"
    write_line "| 失败 | ${FAILURE} |"
    write_line "| 取消 | ${CANCELLED} |"
    if [ "${TOTAL}" != "0" ] && [ "${TOTAL}" -gt 0 ] 2>/dev/null; then
      SUCCESS_RATE=$(python3 -c "print(f'{${SUCCESS}/${TOTAL}*100:.1f}%')" 2>/dev/null || echo "N/A")
      write_line "| 成功率 | ${SUCCESS_RATE} |"
    fi
  else
    write_line "- CI/CD 状态: 无法获取 (请确认 gh 已认证且有 workflow 运行记录)"
  fi
else
  write_line "- CI/CD 状态: N/A (需要 gh CLI 且已认证)"
fi
write_line ""

# =============================================================================
# 6. 改进建议
# =============================================================================
write_line "## 6. 改进建议"
write_line ""
write_line "- [ ] 跟进本周失败 CI 的根因分析"
write_line "- [ ] 处理 Critical/High 安全漏洞"
write_line "- [ ] 补充低覆盖率模块的单元测试"
write_line "- [ ] 复盘本周告警, 优化告警规则避免噪声"
write_line ""

# =============================================================================
# 报告生成完成
# =============================================================================
write_line "---"
write_line "*报告由 \`deploy/scripts/weekly-quality-report.sh\` 自动生成*"
write_line ""

echo "✅ 周度质量报告已生成: ${REPORT_FILE}"
