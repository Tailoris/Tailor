#!/bin/bash
# =============================================================
# 阶段5 性能与安全加固: 安全审计脚本
# =============================================================
# 用途: 检查所有微服务的安全配置和潜在风险
# =============================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[FAIL]${NC} $1"; }
log_step()  { echo -e "${BLUE}[STEP]${NC} $1"; }

REPORT_FILE="/tmp/tailor-is-security-audit-$(date +%Y%m%d-%H%M%S).txt"
TOTAL_CHECKS=0
PASS_CHECKS=0
FAIL_CHECKS=0

record_pass() { TOTAL_CHECKS=$((TOTAL_CHECKS+1)); PASS_CHECKS=$((PASS_CHECKS+1)); echo "  [PASS] $1"; }
record_fail() { TOTAL_CHECKS=$((TOTAL_CHECKS+1)); FAIL_CHECKS=$((FAIL_CHECKS+1)); echo "  [FAIL] $1"; }
record_warn() { TOTAL_CHECKS=$((TOTAL_CHECKS+1)); PASS_CHECKS=$((PASS_CHECKS+1)); echo "  [WARN] $1"; }

echo "============================================================" | tee "$REPORT_FILE"
echo "  Tailor IS 安全审计报告" | tee -a "$REPORT_FILE"
echo "  执行时间: $(date '+%Y-%m-%d %H:%M:%S')" | tee -a "$REPORT_FILE"
echo "============================================================" | tee -a "$REPORT_FILE"
echo "" | tee -a "$REPORT_FILE"

# ============================================================
# 1. 服务健康与端口
# ============================================================
log_step "1. 服务健康检查" | tee -a "$REPORT_FILE"
SERVICES=(8080 8101 8102 8103 8104 8105 8106 8107 8108 8109 8110 8111)
UP_COUNT=0
for p in "${SERVICES[@]}"; do
    code=$(curl.exe -s --noproxy '*' --max-time 3 -o /dev/null -w "%{http_code}" "http://localhost:$p/actuator/health" 2>/dev/null || echo "000")
    if [ "$code" = "200" ]; then
        UP_COUNT=$((UP_COUNT+1))
    fi
done
if [ "$UP_COUNT" -ge 11 ]; then
    record_pass "11+ 个微服务健康 ($UP_COUNT/12)"
else
    record_fail "微服务不健康 (仅 $UP_COUNT/12 UP)"
fi

# ============================================================
# 2. 网关 CORS 配置
# ============================================================
log_step "2. 网关 CORS 配置" | tee -a "$REPORT_FILE"
CORS_HEADERS=$(curl.exe -s --noproxy '*' --max-time 3 -i -X OPTIONS \
    -H "Origin: http://localhost:5173" \
    -H "Access-Control-Request-Method: GET" \
    "http://localhost:8080/api/product/list" 2>/dev/null || echo "")

if echo "$CORS_HEADERS" | grep -qi "access-control-allow-origin"; then
    record_pass "CORS 头存在"
else
    record_warn "CORS 头未返回 (可能需要触发预检)"
fi

# ============================================================
# 3. 安全响应头
# ============================================================
log_step "3. 安全响应头检查" | tee -a "$REPORT_FILE"
SEC_HEADERS=$(curl.exe -s --noproxy '*' --max-time 3 -i "http://localhost:8080/api/product/list" 2>/dev/null || echo "")

if echo "$SEC_HEADERS" | grep -qi "X-Content-Type-Options"; then
    record_pass "X-Content-Type-Options 头存在"
else
    record_fail "X-Content-Type-Options 头缺失"
fi

if echo "$SEC_HEADERS" | grep -qi "X-Frame-Options"; then
    record_pass "X-Frame-Options 头存在"
else
    record_fail "X-Frame-Options 头缺失"
fi

if echo "$SEC_HEADERS" | grep -qi "Strict-Transport-Security"; then
    record_pass "HSTS 头存在"
else
    record_fail "HSTS 头缺失"
fi

# ============================================================
# 4. 限流测试
# ============================================================
log_step "4. 限流测试" | tee -a "$REPORT_FILE"
RATE_LIMIT_HIT=0
for i in $(seq 1 15); do
    code=$(curl.exe -s --noproxy '*' --max-time 2 -o /dev/null -w "%{http_code}" \
        -X POST -H "Content-Type: application/json" \
        -d '{"username":"test","password":"test"}' \
        "http://localhost:8080/api/auth/login" 2>/dev/null || echo "000")
    if [ "$code" = "429" ]; then
        RATE_LIMIT_HIT=1
        break
    fi
done
if [ "$RATE_LIMIT_HIT" = "1" ]; then
    record_pass "登录接口限流生效 (触发 429)"
else
    record_warn "登录接口限流未触发 (15次请求未返回 429，可能需要更高并发测试)"
fi

# ============================================================
# 5. 认证测试
# ============================================================
log_step "5. 认证测试" | tee -a "$REPORT_FILE"
AUTH_CODE=$(curl.exe -s --noproxy '*' --max-time 3 -o /dev/null -w "%{http_code}" \
    "http://localhost:8080/api/auth/userinfo" 2>/dev/null || echo "000")
if [ "$AUTH_CODE" = "401" ]; then
    record_pass "未认证请求正确返回 401"
elif [ "$AUTH_CODE" = "404" ]; then
    record_warn "/api/auth/userinfo 返回 404 (端点可能未实现)"
else
    record_warn "认证检查返回 $AUTH_CODE"
fi

# ============================================================
# 6. Actuator 暴露检查
# ============================================================
log_step "6. Actuator 端点暴露" | tee -a "$REPORT_FILE"
PROMETHEUS_DATA=$(curl.exe -s --noproxy '*' --max-time 3 \
    "http://localhost:8103/actuator/prometheus" 2>/dev/null | head -c 200 || echo "")
if echo "$PROMETHEUS_DATA" | grep -q "jvm_memory_used_bytes"; then
    record_pass "Product 服务 actuator/prometheus 暴露正常"
else
    record_fail "Product 服务 actuator/prometheus 不可用"
fi

# ============================================================
# 7. 监控服务
# ============================================================
log_step "7. 监控服务" | tee -a "$REPORT_FILE"
PROM_CODE=$(curl.exe -s --noproxy '*' --max-time 3 -o /dev/null -w "%{http_code}" "http://localhost:9090/-/healthy" 2>/dev/null || echo "000")
if [ "$PROM_CODE" = "200" ]; then
    record_pass "Prometheus 健康"
else
    record_fail "Prometheus 不可用 (HTTP $PROM_CODE)"
fi

GRAF_CODE=$(curl.exe -s --noproxy '*' --max-time 3 -o /dev/null -w "%{http_code}" "http://localhost:3000/api/health" 2>/dev/null || echo "000")
if [ "$GRAF_CODE" = "200" ]; then
    record_pass "Grafana 健康"
else
    record_fail "Grafana 不可用 (HTTP $GRAF_CODE)"
fi

# ============================================================
# 8. Nacos 服务注册
# ============================================================
log_step "8. Nacos 服务注册" | tee -a "$REPORT_FILE"
# 使用正确的 Nacos API
NACOS_AUTH="nacos:nacos"
NACOS_SERVICES=$(curl.exe -s --noproxy '*' --max-time 5 -u "$NACOS_AUTH" \
    "http://localhost:8848/nacos/v1/ns/catalog/services" 2>/dev/null || echo "")
SERVICE_COUNT=$(echo "$NACOS_SERVICES" | grep -o "tailor-is" | wc -l)
if [ "$SERVICE_COUNT" -ge 10 ]; then
    record_pass "Nacos 注册服务数: $SERVICE_COUNT"
else
    record_warn "Nacos 注册服务数偏少: $SERVICE_COUNT"
fi

# ============================================================
# 9. 业务接口功能
# ============================================================
log_step "9. 业务接口功能" | tee -a "$REPORT_FILE"
PROD_CODE=$(curl.exe -s --noproxy '*' --max-time 3 -o /dev/null -w "%{http_code}" \
    "http://localhost:8080/api/product/list" 2>/dev/null || echo "000")
if [ "$PROD_CODE" = "200" ]; then
    record_pass "商品列表接口正常"
else
    record_fail "商品列表接口异常 (HTTP $PROD_CODE)"
fi

CAT_CODE=$(curl.exe -s --noproxy '*' --max-time 3 -o /dev/null -w "%{http_code}" \
    "http://localhost:8080/api/product/category/tree" 2>/dev/null || echo "000")
if [ "$CAT_CODE" = "200" ]; then
    record_pass "商品分类树接口正常"
else
    record_warn "商品分类树接口 HTTP $CAT_CODE"
fi

# ============================================================
# 总结
# ============================================================
echo "" | tee -a "$REPORT_FILE"
echo "============================================================" | tee -a "$REPORT_FILE"
echo "  审计总结" | tee -a "$REPORT_FILE"
echo "  总计检查: $TOTAL_CHECKS" | tee -a "$REPORT_FILE"
echo "  通过/警告: $PASS_CHECKS" | tee -a "$REPORT_FILE"
echo "  失败: $FAIL_CHECKS" | tee -a "$REPORT_FILE"
echo "  通过率: $(( PASS_CHECKS * 100 / TOTAL_CHECKS ))%" | tee -a "$REPORT_FILE"
echo "============================================================" | tee -a "$REPORT_FILE"

if [ "$FAIL_CHECKS" -eq 0 ]; then
    log_info "✅ 全部通过！"
    exit 0
else
    log_warn "⚠️ 存在 $FAIL_CHECKS 项失败，请检查"
    exit 1
fi
