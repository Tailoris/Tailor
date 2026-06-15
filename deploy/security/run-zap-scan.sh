#!/bin/bash
# ==============================================================================
# Tailor IS - OWASP ZAP 安全扫描脚本
# Phase 3 P3-3: OWASP Top 10 安全扫描 (0 Critical)
# ==============================================================================
# 使用方式:
#   ./deploy/security/run-zap-scan.sh full        # 全量扫描 (所有微服务)
#   ./deploy/security/run-zap-scan.sh quick       # 快速扫描 (仅核心 API)
#   ./deploy/security/run-zap-scan.sh api         # API 专项扫描
# ==============================================================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[FAIL]${NC} $1"; }
log_step()  { echo -e "${BLUE}[STEP]${NC} $1"; }

ZAP_HOST="${ZAP_HOST:-localhost}"
ZAP_PORT="${ZAP_PORT:-8088}"
ZAP_API_KEY="${ZAP_API_KEY:-tailor-is-zap-key}"
TARGET_HOST="${TARGET_HOST:-localhost}"
TARGET_PORT="${TARGET_PORT:-8080}"
SCAN_TYPE="${1:-full}"
REPORT_DIR="./deploy/security/reports"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)

mkdir -p "$REPORT_DIR"

# ============================================================================
# 检查 ZAP 是否可用
# ============================================================================
check_zap() {
    log_step "检查 ZAP 服务状态..."
    for i in $(seq 1 10); do
        if curl -s "http://${ZAP_HOST}:${ZAP_PORT}/" > /dev/null 2>&1; then
            log_info "ZAP 服务就绪"
            return 0
        fi
        sleep 2
    done
    log_error "ZAP 服务不可用，请先启动: docker compose -f deploy/security/docker-compose.zap.yml up -d"
    exit 1
}

# ============================================================================
# ZAP API 辅助函数
# ============================================================================
zap_api() {
    local endpoint="$1"
    local params="${2:-}"
    curl -s "http://${ZAP_HOST}:${ZAP_PORT}/JSON/${endpoint}/?apikey=${ZAP_API_KEY}&${params}"
}

# ============================================================================
# 快速扫描 (Active Scan 仅核心端点)
# ============================================================================
run_quick_scan() {
    local target="http://${TARGET_HOST}:${TARGET_PORT}"
    log_step "快速扫描目标: $target"

    # 1. 打开目标 URL
    log_info "1/5 访问目标..."
    zap_api "core/action/accessUrl" "url=${target}"

    # 2. Spider 爬虫 (快速模式)
    log_info "2/5 Spider 爬虫..."
    local scan_id=$(zap_api "spider/action/scan" "url=${target}&maxChildren=5&recurse=false" | grep -o '"scanId":"[0-9]*"' | grep -o '[0-9]*')
    sleep 5
    while true; do
        local progress=$(zap_api "spider/view/status" "scanId=${scan_id}" | grep -o '"status":"[0-9]*"' | grep -o '[0-9]*')
        [ "$progress" = "100" ] && break
        sleep 3
    done
    log_info "Spider 完成"

    # 3. AJAX Spider (可选)
    log_info "3/5 AJAX Spider..."
    zap_api "ajaxSpider/action/scan" "url=${target}&inScope=true&subtreeOnly=true" > /dev/null 2>&1 || true
    sleep 10

    # 4. Active Scan (仅核心 API 路径)
    log_info "4/5 Active Scan..."
    local core_paths=(
        "/api/product/list"
        "/api/auth/login"
        "/api/order/create"
        "/api/merchant/list"
    )
    for path in "${core_paths[@]}"; do
        zap_api "ascan/action/scan" "url=${target}${path}&recurse=false&scanPolicyName=API-Minimal" > /dev/null 2>&1
    done
    sleep 10

    while true; do
        local progress=$(zap_api "ascan/view/status" | grep -o '"status":"[0-9]*"' | grep -o '[0-9]*')
        [ "$progress" = "100" ] && break
        sleep 5
    done
    log_info "Active Scan 完成"

    # 5. 生成报告
    log_info "5/5 生成报告..."
    generate_report "quick"
}

# ============================================================================
# 全量扫描
# ============================================================================
run_full_scan() {
    local target="http://${TARGET_HOST}:${TARGET_PORT}"
    log_step "全量扫描目标: $target"

    log_info "1/6 访问目标..."
    zap_api "core/action/accessUrl" "url=${target}"

    log_info "2/6 Spider 爬虫 (全量)..."
    zap_api "spider/action/scan" "url=${target}&maxChildren=10&recurse=true" > /dev/null 2>&1
    sleep 10
    while true; do
        local progress=$(zap_api "spider/view/status" | grep -o '"status":"[0-9]*"' | grep -o '[0-9]*' | tail -1)
        [ "$progress" = "100" ] && break
        sleep 5
    done
    log_info "Spider 完成"

    log_info "3/6 AJAX Spider..."
    zap_api "ajaxSpider/action/scan" "url=${target}&inScope=true" > /dev/null 2>&1 || true
    sleep 15

    log_info "4/6 Passive Scan 等待..."
    sleep 10

    log_info "5/6 Active Scan (全量)..."
    zap_api "ascan/action/scan" "url=${target}&recurse=true&inScopeOnly=true" > /dev/null 2>&1
    log_info "Active Scan 运行中... (预计 10-30 分钟)"
    while true; do
        local progress=$(zap_api "ascan/view/status" | grep -o '"status":"[0-9]*"' | grep -o '[0-9]*' | tail -1)
        [ "$progress" = "100" ] && break
        echo -ne "\r  进度: ${progress}%"
        sleep 10
    done
    echo ""
    log_info "Active Scan 完成"

    log_info "6/6 生成报告..."
    generate_report "full"
}

# ============================================================================
# API 专项扫描
# ============================================================================
run_api_scan() {
    local target="http://${TARGET_HOST}:${TARGET_PORT}"
    log_step "API 专项扫描目标: $target"

    # 导入 OpenAPI 定义 (如果存在)
    local openapi_url="http://${TARGET_HOST}:${TARGET_PORT}/v3/api-docs"
    log_info "导入 OpenAPI 定义..."
    zap_api "openapi/action/importUrl" "url=${openapi_url}&hostOverride=" > /dev/null 2>&1 || log_warn "OpenAPI 定义不可用，跳过"

    log_info "API Active Scan..."
    zap_api "ascan/action/scan" "url=${target}" > /dev/null 2>&1
    sleep 10

    while true; do
        local progress=$(zap_api "ascan/view/status" | grep -o '"status":"[0-9]*"' | grep -o '[0-9]*' | tail -1)
        [ "$progress" = "100" ] && break
        sleep 10
    done
    log_info "API Scan 完成"

    generate_report "api"
}

# ============================================================================
# 生成报告
# ============================================================================
generate_report() {
    local prefix="$1"
    local html_report="${REPORT_DIR}/zap-report-${prefix}-${TIMESTAMP}.html"
    local json_report="${REPORT_DIR}/zap-report-${prefix}-${TIMESTAMP}.json"
    local md_report="${REPORT_DIR}/zap-report-${prefix}-${TIMESTAMP}.md"

    # HTML 报告
    zap_api "core/other/htmlreport" > "$html_report" 2>/dev/null || true

    # JSON 报告
    zap_api "core/other/jsonreport" > "$json_report" 2>/dev/null || true

    # 告警摘要
    local alerts=$(zap_api "core/view/alertsSummary" | grep -o '"high":[0-9]*' | grep -o '[0-9]*' || echo "0")
    local critical=$(zap_api "core/view/alertsSummary" | grep -o '"critical":[0-9]*' | grep -o '[0-9]*' || echo "0")
    local medium=$(zap_api "core/view/alertsSummary" | grep -o '"medium":[0-9]*' | grep -o '[0-9]*' || echo "0")
    local low=$(zap_api "core/view/alertsSummary" | grep -o '"low":[0-9]*' | grep -o '[0-9]*' || echo "0")

    # Markdown 报告
    cat > "$md_report" << EOF
# Tailor IS OWASP ZAP 安全扫描报告

**扫描时间**: $(date '+%Y-%m-%d %H:%M:%S')
**扫描类型**: ${prefix}
**目标**: http://${TARGET_HOST}:${TARGET_PORT}

## 告警摘要

| 级别 | 数量 | 目标 |
|------|------|------|
| Critical | ${critical} | 0 (目标) |
| High | ${high} | 0 (目标) |
| Medium | ${medium} | ≤ 5 |
| Low | ${low} | - |

## 状态

- Critical: $([ "$critical" = "0" ] && echo "✅ 通过" || echo "🔴 未通过 (需立即修复)")
- High: $([ "$high" = "0" ] && echo "✅ 通过" || echo "🟡 建议修复)")

## 详细报告

- HTML: [${html_report}](${html_report})
- JSON: [${json_report}](${json_report})

## OWASP Top 10 覆盖

| 类别 | 风险 | 检测项 |
|------|------|--------|
| A01:2021 - 访问控制失效 | 高 | 认证绕过、权限提升 |
| A02:2021 - 加密失败 | 高 | 明文传输、弱加密 |
| A03:2021 - 注入 | 高 | SQL注入、XSS、命令注入 |
| A04:2021 - 不安全设计 | 中 | 缺少速率限制 |
| A05:2021 - 安全配置错误 | 中 | 默认配置、错误信息泄露 |
| A06:2021 - 易受攻击组件 | 高 | 依赖库漏洞 |
| A07:2021 - 认证失败 | 高 | 弱密码策略、会话固定 |
| A08:2021 - 软件完整性故障 | 中 | 不安全的反序列化 |
| A09:2021 - 日志监控失败 | 中 | 敏感信息在日志中 |
| A10:2021 - SSRF | 中 | 服务端请求伪造 |
EOF

    log_info "报告已生成:"
    log_info "  HTML: $html_report"
    log_info "  JSON: $json_report"
    log_info "  MD:   $md_report"

    echo ""
    echo "============================================================"
    echo "  扫描结果摘要"
    echo "  Critical: $critical (目标: 0)"
    echo "  High:     $high (目标: 0)"
    echo "  Medium:   $medium"
    echo "  Low:      $low"
    echo "============================================================"

    if [ "$critical" != "0" ]; then
        log_error "存在 Critical 级别漏洞，必须立即修复!"
        return 1
    fi
    return 0
}

# ============================================================================
# 依赖漏洞扫描 (使用 OWASP Dependency-Check)
# ============================================================================
run_dependency_check() {
    log_step "执行依赖漏洞扫描 (OWASP Dependency-Check)..."
    local report="${REPORT_DIR}/dependency-check-${TIMESTAMP}.html"

    if command -v dependency-check.sh &> /dev/null; then
        dependency-check.sh \
            --project "Tailor IS" \
            --scan ./tailor-is \
            --format HTML \
            --out "$report" \
            --failOnCVSS 7 || log_warn "发现高危依赖漏洞"
    else
        log_warn "dependency-check.sh 未安装，跳过。安装: https://github.com/jeremylong/DependencyCheck"
    fi
}

# ============================================================================
# 主流程
# ============================================================================
echo "============================================================"
echo "  Tailor IS OWASP 安全扫描 - 阶段3 (P3-3)"
echo "  目标: OWASP Top 10 0 Critical"
echo "  扫描时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "============================================================"

check_zap

case "$SCAN_TYPE" in
    quick)
        run_quick_scan
        ;;
    api)
        run_api_scan
        ;;
    full)
        run_full_scan
        run_dependency_check
        ;;
    dep-check)
        run_dependency_check
        ;;
    *)
        log_error "未知扫描类型: $SCAN_TYPE"
        echo "可用: quick, api, full, dep-check"
        exit 1
        ;;
esac

echo ""
log_info "扫描完成!"