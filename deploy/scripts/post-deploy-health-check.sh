#!/usr/bin/env bash
# ==============================================================================
# Tailor IS - 部署后自动验证脚本
# ==============================================================================
# 用途: 部署完成后自动验证所有微服务健康状态、API可达性、Nginx可用性
#
# 使用方式:
#   bash deploy/scripts/post-deploy-health-check.sh [environment]
# ==============================================================================
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

ENVIRONMENT="${1:-development}"
DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$DEPLOY_DIR/docker-compose.${ENVIRONMENT}.yml"
GATEWAY_URL="http://localhost:9000"
MAX_RETRIES=10
RETRY_INTERVAL=10
PASS=0
FAIL=0

# ========== 服务端口映射表（v4.0 对齐） ==========
declare -A SERVICE_PORTS=(
    ["core-gateway"]="9000"
    ["lite-gateway"]="8081"
    ["admin-service"]="8100"
    ["user-service"]="8101"
    ["product-service"]="8102"
    ["order-service"]="8103"
    ["payment-service"]="8104"
    ["marketing-service"]="8105"
    ["ai-service"]="8106"
    ["copyright-service"]="8107"
    ["community-service"]="8108"
    ["supply-service"]="8109"
    ["merchant-service"]="8110"
    ["message-service"]="8111"
    ["academy-service"]="8112"
    ["analytics-service"]="8113"
    ["message-im-service"]="8114"
    ["pattern-service"]="8115"
    ["design-service"]="8116"
    ["knowledge-service"]="8117"
)

# ========== 核心业务 API 端点 ==========
API_ENDPOINTS=(
    "/api/v1/user/health"
    "/api/v1/products?page=1&size=1"
    "/api/v1/order/health"
    "/api/v1/community/health"
    "/api/v1/copyright/health"
    "/api/v1/pattern/health"
    "/api/v1/design/health"
    "/api/v1/knowledge/health"
    "/api/v1/ai/health"
    "/api/v1/admin/health"
)

log_section() { echo ""; echo -e "${BLUE}========================================${NC}"; echo -e "${BLUE}  $1${NC}"; echo -e "${BLUE}========================================${NC}"; }
log_pass()  { echo -e "  ${GREEN}✅ PASS${NC} $1"; PASS=$((PASS + 1)); }
log_fail()  { echo -e "  ${RED}🔴 FAIL${NC} $1"; FAIL=$((FAIL + 1)); }
log_warn()  { echo -e "  ${YELLOW}⚠️  WARN${NC} $1"; }

# ==============================================================================
# 1. Docker 容器健康检查
# ==============================================================================
check_container_health() {
    log_section "Docker 容器健康检查"
    
    local containers=$(docker ps --format '{{.Names}}' 2>/dev/null | grep 'tailor-is-' || true)
    
    if [[ -z "$containers" ]]; then
        log_warn "未发现运行中的 Tailor IS 容器"
        return
    fi
    
    for container_name in $containers; do
        local status=$(docker inspect -f '{{.State.Health.Status}}' "$container_name" 2>/dev/null || echo "unknown")
        local service_key=$(echo "$container_name" | sed 's/tailor-is-//' | sed 's/-development//' | sed 's/-staging//' | sed 's/-production//')
        
        case "$status" in
            healthy)   log_pass "$container_name ($service_key) → $status" ;;
            starting)  log_warn "$container_name ($service_key) → $status (等待中)" ;;
            *)         log_fail "$container_name ($service_key) → $status" ;;
        esac
    done
}

# ==============================================================================
# 2. 各微服务 Actuator 健康检查
# ==============================================================================
check_actuator_health() {
    log_section "微服务 Actuator 端点检查"
    
    for service_name in "${!SERVICE_PORTS[@]}"; do
        local port="${SERVICE_PORTS[$service_name]}"
        local container="tailor-is-${service_name}"
        local url="http://127.0.0.1:${port}/actuator/health"
        
        # 尝试通过容器内部 curl 检查
        if docker exec "$container" curl -fsS "$url" -o /dev/null 2>/dev/null; then
            log_pass "$service_name → :${port}/actuator/health"
        else
            # 回退到主机 curl 检查
            if curl -fsS "$url" -o /dev/null 2>/dev/null; then
                log_pass "$service_name → :${port}/actuator/health"
            else
                log_fail "$service_name → :${port}/actuator/health (不可达)"
            fi
        fi
    done
}

# ==============================================================================
# 3. 网关 API 可达性检查
# ==============================================================================
check_gateway_api() {
    log_section "网关 API 可达性检查"
    
    if ! curl -fsS "$GATEWAY_URL/actuator/health" -o /dev/null 2>/dev/null; then
        log_fail "网关 $GATEWAY_URL 不可达"
        return
    fi
    log_pass "网关 $GATEWAY_URL 可达"
    
    for endpoint in "${API_ENDPOINTS[@]}"; do
        local http_code=$(curl -s -o /dev/null -w "%{http_code}" "$GATEWAY_URL$endpoint" 2>/dev/null || echo "000")
        case "$http_code" in
            200|302|401|403) log_pass "GET $endpoint → HTTP $http_code" ;;
            404) log_warn "GET $endpoint → HTTP $http_code (端点不存在)" ;;
            503) log_warn "GET $endpoint → HTTP $http_code (服务不可用)" ;;
            *)   log_fail "GET $endpoint → HTTP $http_code" ;;
        esac
    done
}

# ==============================================================================
# 4. Nginx 前端可达性检查
# ==============================================================================
check_nginx() {
    log_section "Nginx 前端可达性检查"
    
    local nginx_urls=(
        "http://localhost"
        "http://localhost/admin/"
        "http://localhost/merchant/"
    )
    
    for url in "${nginx_urls[@]}"; do
        local http_code=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "000")
        case "$http_code" in
            200|301|302|401) log_pass "GET $url → HTTP $http_code" ;;
            404) log_warn "GET $url → HTTP $http_code (页面未找到)" ;;
            *)   log_fail "GET $url → HTTP $http_code" ;;
        esac
    done
    
    # 验证 Nginx 安全头
    log_section "Nginx 安全头验证"
    local headers=$(curl -sI "http://localhost" 2>/dev/null || echo "")
    
    if echo "$headers" | grep -qi "X-Frame-Options"; then
        log_pass "X-Frame-Options 已设置"
    else
        log_fail "X-Frame-Options 缺失"
    fi
    
    if echo "$headers" | grep -qi "X-Content-Type-Options"; then
        log_pass "X-Content-Type-Options 已设置"
    else
        log_fail "X-Content-Type-Options 缺失"
    fi
}

# ==============================================================================
# 5. 端口占用检查
# ==============================================================================
check_port_conflicts() {
    log_section "端口占用检查"
    
    local conflicts=0
    for service_name in "${!SERVICE_PORTS[@]}"; do
        local port="${SERVICE_PORTS[$service_name]}"
        local pid=$(lsof -ti:"$port" 2>/dev/null || echo "")
        if [[ -n "$pid" ]]; then
            local proc=$(ps -p "$pid" -o comm= 2>/dev/null || echo "unknown")
            log_pass "端口 $port ($service_name) → $proc (PID: $pid)"
        else
            log_warn "端口 $port ($service_name) → 未监听"
        fi
    done
}

# ==============================================================================
# 6. 数据库连接检查
# ==============================================================================
check_database() {
    log_section "数据库连接检查"
    
    # MySQL
    if command -v mysqladmin &>/dev/null; then
        if mysqladmin ping -h 127.0.0.1 --silent 2>/dev/null; then
            log_pass "MySQL (3306) 连接正常"
        else
            log_warn "MySQL (3306) 不可达"
        fi
    fi
    
    # Redis
    if command -v redis-cli &>/dev/null; then
        if redis-cli -h 127.0.0.1 ping 2>/dev/null | grep -q PONG; then
            log_pass "Redis (6379) 连接正常"
        else
            log_warn "Redis (6379) 不可达"
        fi
    fi
    
    # MongoDB
    if command -v mongosh &>/dev/null; then
        if mongosh --host 127.0.0.1 --eval "db.runCommand({ping:1})" --quiet 2>/dev/null | grep -q ok; then
            log_pass "MongoDB (27017) 连接正常"
        else
            log_warn "MongoDB (27017) 不可达"
        fi
    fi
}

# ==============================================================================
# 主流程
# ==============================================================================
main() {
    echo ""
    echo -e "${GREEN}╔══════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║  Tailor IS 部署后健康检查 (${ENVIRONMENT})                ║${NC}"
    echo -e "${GREEN}║  $(date '+%Y-%m-%d %H:%M:%S')                         ║${NC}"
    echo -e "${GREEN}╚══════════════════════════════════════════════════╝${NC}"
    
    check_container_health
    check_actuator_health
    check_gateway_api
    check_nginx
    check_port_conflicts
    check_database
    
    # ========== 汇总报告 ==========
    log_section "汇总报告"
    TOTAL=$((PASS + FAIL))
    echo ""
    echo -e "${BLUE}┌────────────────────────────────────────────┐${NC}"
    echo -e "${BLUE}│  总检查项:  ${TOTAL}${NC}"
    echo -e "${BLUE}│  ✅ 通过:   ${GREEN}${PASS}${NC}"
    echo -e "${BLUE}│  🔴 失败:   ${RED}${FAIL}${NC}"
    echo -e "${BLUE}│  通过率:    ${GREEN}$(( PASS * 100 / (TOTAL > 0 ? TOTAL : 1) ))${NC}%"
    echo -e "${BLUE}└────────────────────────────────────────────┘${NC}"
    echo ""
    
    if [[ "$FAIL" -gt 0 ]]; then
        echo -e "${RED}🔴 部署后检查未通过: ${FAIL} 项检查失败${NC}"
        exit 1
    else
        echo -e "${GREEN}✅ 部署后检查全部通过${NC}"
        exit 0
    fi
}

main "$@"