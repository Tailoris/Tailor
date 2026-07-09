#!/usr/bin/env bash
# ==============================================================================
# Tailor IS - 质量度量仪表盘
# ==============================================================================
# 用途: 生成项目质量度量报告，包括代码量、规范合规率、测试覆盖率
#
# 使用方式:
#   bash deploy/scripts/quality-metrics.sh
#   bash deploy/scripts/quality-metrics.sh --json     # JSON 格式输出
# ==============================================================================
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SCRIPTS_DIR="$REPO_DIR/deploy/scripts"
OUTPUT_JSON=0
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

for arg in "$@"; do
    case "$arg" in
        --json) OUTPUT_JSON=1 ;;
    esac
done

# ==============================================================================
# 1. 代码量统计
# ==============================================================================
count_code() {
    local java_files=$(find "$REPO_DIR/tailor-is" -name "*.java" 2>/dev/null | wc -l)
    local java_lines=$(find "$REPO_DIR/tailor-is" -name "*.java" -exec cat {} + 2>/dev/null | wc -l)
    local ts_files=$(find "$REPO_DIR/tailor-is-frontend" -name "*.ts" -o -name "*.tsx" 2>/dev/null | grep -v node_modules | wc -l)
    local ts_lines=$(find "$REPO_DIR/tailor-is-frontend" -name "*.ts" -o -name "*.tsx" 2>/dev/null | grep -v node_modules | xargs cat 2>/dev/null | wc -l)
    local yml_files=$(find "$REPO_DIR/deploy" "$REPO_DIR/tailor-is" -name "*.yml" -o -name "*.yaml" 2>/dev/null | grep -v node_modules | wc -l)
    local sh_files=$(find "$REPO_DIR/deploy" -name "*.sh" 2>/dev/null | wc -l)
    
    JAVA_FILES=$java_files
    JAVA_LINES=$java_lines
    TS_FILES=$ts_files
    TS_LINES=$ts_lines
    YML_FILES=$yml_files
    SH_FILES=$sh_files
}

# ==============================================================================
# 2. 规范合规率
# ==============================================================================
check_compliance() {
    local port_ok=0
    local api_ok=0
    local gateway_ok=0
    local nginx_ok=0
    local container_ok=0
    
    # 端口检测
    if [[ -x "$SCRIPTS_DIR/check-port-allocation.sh" ]]; then
        if bash "$SCRIPTS_DIR/check-port-allocation.sh" --no-fail &>/dev/null; then
            port_ok=1
        fi
    fi
    
    # API 前缀检测
    if [[ -x "$SCRIPTS_DIR/check-api-prefix.sh" ]]; then
        if bash "$SCRIPTS_DIR/check-api-prefix.sh" --no-fail &>/dev/null; then
            api_ok=1
        fi
    fi
    
    # 网关路由检测
    if [[ -x "$SCRIPTS_DIR/check-gateway-routes.sh" ]]; then
        if bash "$SCRIPTS_DIR/check-gateway-routes.sh" --no-fail &>/dev/null; then
            gateway_ok=1
        fi
    fi
    
    # Nginx 配置检测
    if [[ -x "$SCRIPTS_DIR/check-nginx-config.sh" ]]; then
        if bash "$SCRIPTS_DIR/check-nginx-config.sh" --no-fail &>/dev/null; then
            nginx_ok=1
        fi
    fi
    
    # 容器端口检测
    if [[ -x "$SCRIPTS_DIR/check-container-ports.sh" ]]; then
        if bash "$SCRIPTS_DIR/check-container-ports.sh" --no-fail &>/dev/null; then
            container_ok=1
        fi
    fi
    
    PORT_OK=$port_ok
    API_OK=$api_ok
    GATEWAY_OK=$gateway_ok
    NGINX_OK=$nginx_ok
    CONTAINER_OK=$container_ok
    COMPLIANCE_TOTAL=$((port_ok + api_ok + gateway_ok + nginx_ok + container_ok))
}

# ==============================================================================
# 3. 服务统计
# ==============================================================================
count_services() {
    local dockerfiles=$(find "$REPO_DIR/tailor-is" -name "Dockerfile" 2>/dev/null | wc -l)
    local compose_services=$(grep -c "image: tailor-is-" "$REPO_DIR/deploy/docker-compose.services.yml" 2>/dev/null || echo 0)
    local controllers=$(grep -r "@RestController\|@RequestMapping" "$REPO_DIR/tailor-is" --include="*.java" -l 2>/dev/null | wc -l)
    local gateways=$(grep -r "@SpringBootApplication" "$REPO_DIR/tailor-is" --include="*.java" -l 2>/dev/null | wc -l)
    
    DOCKERFILES=$dockerfiles
    COMPOSE_SERVICES=$compose_services
    CONTROLLERS=$controllers
    MICROSERVICES=$gateways
}

# ==============================================================================
# 4. 安全配置统计
# ==============================================================================
check_security() {
    local has_aes=$(grep -rl "AES_KEY\|COPYRIGHT_AES_KEY" "$REPO_DIR/deploy" --include="*.yml" --include="*.env" 2>/dev/null | wc -l)
    local has_actuator_security=$(grep -rl "MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health" "$REPO_DIR/deploy" --include="*.yml" --include="*.env" 2>/dev/null | wc -l)
    local has_nginx_security=$(grep -c "X-Frame-Options\|X-Content-Type-Options" "$REPO_DIR/deploy/nginx/frontend.conf" 2>/dev/null || echo 0)
    local has_rate_limit=$(grep -r "limit_req\|rate_limit\|limit_conn" "$REPO_DIR/deploy/nginx/" --include="*.conf" -l 2>/dev/null | wc -l)
    local has_rbac=$(grep -r "@SaCheckRole\|@SaCheckPermission" "$REPO_DIR/tailor-is" --include="*.java" -l 2>/dev/null | wc -l)
    
    AES_CONFIGS=$has_aes
    ACTUATOR_SECURE=$has_actuator_security
    NGINX_SECURITY=$has_nginx_security
    RATE_LIMIT=$has_rate_limit
    RBAC_FILES=$has_rbac
}

# ==============================================================================
# 5. 拦截机制统计
# ==============================================================================
check_interception() {
    local has_precommit=0
    local has_pr_check=0
    local has_cicd_check=0
    
    if [[ -x "$REPO_DIR/.git/hooks/pre-commit" ]]; then
        has_precommit=1
    fi
    
    if [[ -f "$REPO_DIR/.github/workflows/pr-check.yml" ]]; then
        has_pr_check=1
    fi
    
    if grep -q "run-all-checks.sh" "$REPO_DIR/.github/workflows/ci-cd.yml" 2>/dev/null; then
        has_cicd_check=1
    fi
    
    PRECOMMIT=$has_precommit
    PR_CHECK=$has_pr_check
    CICD_CHECK=$has_cicd_check
    INTERCEPTION_TOTAL=$((has_precommit + has_pr_check + has_cicd_check))
}

# ==============================================================================
# 生成报告
# ==============================================================================
generate_report() {
    count_code
    check_compliance
    count_services
    check_security
    check_interception
    
    local total_lines=$((JAVA_LINES + TS_LINES))
    local total_files=$((JAVA_FILES + TS_FILES + YML_FILES + SH_FILES))
    local compliance_pct=$((COMPLIANCE_TOTAL * 100 / 5))
    local interception_pct=$((INTERCEPTION_TOTAL * 100 / 3))
    local interception_label="拦截机制覆盖（${interception_pct}%）"
    
    if [[ "$OUTPUT_JSON" == "1" ]]; then
        cat <<JSON
{
  "timestamp": "$TIMESTAMP",
  "code": {
    "java_files": $JAVA_FILES,
    "java_lines": $JAVA_LINES,
    "typescript_files": $TS_FILES,
    "typescript_lines": $TS_LINES,
    "yml_files": $YML_FILES,
    "shell_files": $SH_FILES,
    "total_files": $total_files,
    "total_lines": $total_lines
  },
  "services": {
    "dockerfiles": $DOCKERFILES,
    "compose_services": $COMPOSE_SERVICES,
    "controllers": $CONTROLLERS,
    "microservices": $MICROSERVICES
  },
  "compliance": {
    "port_allocation": $((PORT_OK * 100)),
    "api_prefix": $((API_OK * 100)),
    "gateway_routes": $((GATEWAY_OK * 100)),
    "nginx_config": $((NGINX_OK * 100)),
    "container_ports": $((CONTAINER_OK * 100)),
    "overall_pct": $compliance_pct
  },
  "security": {
    "aes_key_configs": $AES_CONFIGS,
    "actuator_secured": $((ACTUATOR_SECURE > 0 ? 1 : 0)),
    "nginx_security_headers": $NGINX_SECURITY,
    "rate_limiting_rules": $RATE_LIMIT,
    "rbac_files": $RBAC_FILES
  },
  "interception": {
    "pre_commit_hook": $((PRECOMMIT == 1)),
    "pr_check_pipeline": $((PR_CHECK == 1)),
    "cicd_check_pipeline": $((CICD_CHECK == 1)),
    "layers_pct": $interception_pct
  }
}
JSON
    else
        cat <<REPORT

╔══════════════════════════════════════════════════════════════╗
║            Tailor IS 质量度量仪表盘                           ║
║            ${TIMESTAMP}                        ║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║  📊 代码规模                                                 ║
║     Java 文件:     ${JAVA_FILES} 个 (${JAVA_LINES} 行)                        
║     TypeScript:    ${TS_FILES} 个 (${TS_LINES} 行)                          
║     YML 配置:      ${YML_FILES} 个
║     Shell 脚本:    ${SH_FILES} 个
║     总计:          ${total_files} 个文件, ${total_lines} 行代码
║                                                              ║
║  🏗️ 服务架构                                                 ║
║     Dockerfile:    ${DOCKERFILES} 个
║     Compose 服务:  ${COMPOSE_SERVICES} 个
║     Controller:    ${CONTROLLERS} 个
║     微服务:        ${MICROSERVICES} 个启动类
║                                                              ║
║  ✅ 规范合规率                                               ║
║     端口分配:      $([ $PORT_OK -eq 1 ] && echo "✅" || echo "🔴")
║     API 前缀:      $([ $API_OK -eq 1 ] && echo "✅" || echo "🔴")
║     网关路由:      $([ $GATEWAY_OK -eq 1 ] && echo "✅" || echo "🔴")
║     Nginx 配置:    $([ $NGINX_OK -eq 1 ] && echo "✅" || echo "🔴")
║     容器端口:      $([ $CONTAINER_OK -eq 1 ] && echo "✅" || echo "🔴")
║     综合合规率:    ${compliance_pct}%
║                                                              ║
║  🔒 安全配置                                                 ║
║     AES 密钥配置:  ${AES_CONFIGS} 处
║     Actuator 安全: $([ $ACTUATOR_SECURE -gt 0 ] && echo "✅ 已配置" || echo "🔴 未配置")
║     Nginx 安全头:  $([ "${NGINX_SECURITY}" -gt 0 ] 2>/dev/null && echo "✅ ${NGINX_SECURITY} 个" || echo "🔴 缺失")
║     速率限制:      $([ "${RATE_LIMIT}" -gt 0 ] 2>/dev/null && echo "✅ ${RATE_LIMIT} 个文件" || echo "🔴 未配置")
║     RBAC 文件:     ${RBAC_FILES} 个
║                                                              ║
║  🛡️ ${interception_label}                                      ║
║     提交前:        $([ $PRECOMMIT -eq 1 ] && echo "✅" || echo "🔴")
║     PR 检查:       $([ $PR_CHECK -eq 1 ] && echo "✅" || echo "🔴")
║     CI/CD 部署:    $([ $CICD_CHECK -eq 1 ] && echo "✅" || echo "🔴")
║                                                              ║
╚══════════════════════════════════════════════════════════════╝

REPORT
    fi
}

generate_report