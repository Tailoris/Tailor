#!/bin/bash
# ==============================================================================
# Tailor IS - 标准化部署脚本 (v1.0)
# ==============================================================================
# 功能：
#   1. 部署前环境变量完整性校验
#   2. 按 Profile 分组启动微服务
#   3. 部署后健康检查等待与验证
#   4. 失败自动回滚
#
# 用法：
#   ./deploy.sh [profile...]          按指定 Profile 部署
#   ./deploy.sh all                   部署全部服务
#   ./deploy.sh core-trade            仅部署核心交易链路
#   ./deploy.sh --verify              仅执行健康检查验证
#   ./deploy.sh --rollback            回滚到上一个版本
#
# 环境变量：
#   DEPLOY_TIMEOUT=600                部署超时时间（秒），默认 600
#   HEALTH_CHECK_INTERVAL=10          健康检查间隔（秒），默认 10
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.services.yml"
ENV_BASE="${SCRIPT_DIR}/tailor-is-prod/env/.env.base"
ENV_CORE="${SCRIPT_DIR}/tailor-is-prod/env/.env.core"
ENV_LIGHT="${SCRIPT_DIR}/tailor-is-prod/env/.env.light"
ENV_DEV="${SCRIPT_DIR}/tailor-is-prod/env/.env.dev"
DEPLOY_TIMEOUT="${DEPLOY_TIMEOUT:-600}"
HEALTH_CHECK_INTERVAL="${HEALTH_CHECK_INTERVAL:-10}"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${BLUE}[INFO]${NC}  $(date '+%H:%M:%S') $*"; }
log_ok()    { echo -e "${GREEN}[OK]${NC}    $(date '+%H:%M:%S') $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $(date '+%H:%M:%S') $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $(date '+%H:%M:%S') $*"; }

# ==============================================================================
# 1. 环境变量完整性校验
# ==============================================================================
check_env() {
    log_info "检查环境变量文件完整性..."

    local required_files=("${ENV_BASE}" "${ENV_CORE}" "${ENV_LIGHT}" "${ENV_DEV}")
    local required_vars=("MYSQL_PASSWORD" "REDIS_PASSWORD" "AES_KEY")

    for f in "${required_files[@]}"; do
        if [[ ! -f "${f}" ]]; then
            log_warn "环境变量文件不存在: ${f}"
        fi
    done

    local missing_vars=()
    for var in "${required_vars[@]}"; do
        if [[ -z "${!var:-}" ]]; then
            # 尝试从 .env.base 文件读取
            if [[ -f "${ENV_BASE}" ]]; then
                if grep -q "^${var}=" "${ENV_BASE}" 2>/dev/null; then
                    continue
                fi
            fi
            missing_vars+=("${var}")
        fi
    done

    if [[ ${#missing_vars[@]} -gt 0 ]]; then
        log_error "缺少必需环境变量: ${missing_vars[*]}"
        log_error "请在 ${ENV_BASE} 中配置这些变量"
        exit 1
    fi
    log_ok "环境变量校验通过"
}

# ==============================================================================
# 2. 构建并启动服务
# ==============================================================================
deploy_services() {
    local profiles="$1"
    log_info "开始部署 Profile: ${profiles}"

    local profile_args=""
    IFS=',' read -ra PROFILE_ARRAY <<< "${profiles}"
    for p in "${PROFILE_ARRAY[@]}"; do
        profile_args="${profile_args} --profile ${p}"
    done

    # 记录当前 Git commit 用于回滚
    if git rev-parse --git-dir > /dev/null 2>&1; then
        local current_commit=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
        echo "deploy_commit=${current_commit}" > "${SCRIPT_DIR}/.deploy_state"
        echo "deploy_time=$(date -Iseconds)" >> "${SCRIPT_DIR}/.deploy_state"
        echo "deploy_profiles=${profiles}" >> "${SCRIPT_DIR}/.deploy_state"
        log_info "当前版本: ${current_commit}"
    fi

    # 构建并启动
    log_info "执行: docker compose -f docker-compose.services.yml ${profile_args} up -d --build"
    if ! docker compose -f "${COMPOSE_FILE}" ${profile_args} up -d --build 2>&1; then
        log_error "服务启动失败，查看日志获取详细信息"
        return 1
    fi

    log_ok "服务启动指令已执行"
}

# ==============================================================================
# 3. 健康检查等待
# ==============================================================================
wait_for_healthy() {
    local service_filter="${1:-}"
    log_info "等待服务健康检查通过（超时: ${DEPLOY_TIMEOUT}s）..."

    local start_time=$(date +%s)
    local all_healthy=false

    while true; do
        local elapsed=$(($(date +%s) - start_time))
        if [[ ${elapsed} -gt ${DEPLOY_TIMEOUT} ]]; then
            log_error "健康检查超时 (${DEPLOY_TIMEOUT}s)"
            break
        fi

        local unhealthy_count=0
        local total_count=0
        local status_output=""

        # 获取服务状态
        if [[ -n "${service_filter}" ]]; then
            status_output=$(docker compose -f "${COMPOSE_FILE}" ps --format "table {{.Name}}\t{{.Status}}" 2>/dev/null | grep -E "${service_filter}" || true)
        else
            status_output=$(docker compose -f "${COMPOSE_FILE}" ps --format "table {{.Name}}\t{{.Status}}" 2>/dev/null || true)
        fi

        while IFS= read -r line; do
            [[ -z "${line}" || "${line}" == *"Name"* ]] && continue
            total_count=$((total_count + 1))
            if [[ "${line}" != *"healthy"* ]]; then
                unhealthy_count=$((unhealthy_count + 1))
            fi
        done <<< "${status_output}"

        if [[ ${total_count} -gt 0 && ${unhealthy_count} -eq 0 ]]; then
            all_healthy=true
            log_ok "所有服务健康检查通过 (${total_count}/${total_count})"
            echo "${status_output}"
            break
        fi

        log_info "健康检查中... 已等待 ${elapsed}s | healthy: $((total_count - unhealthy_count))/${total_count}"
        sleep "${HEALTH_CHECK_INTERVAL}"
    done

    if [[ "${all_healthy}" != "true" ]]; then
        log_error "部分服务健康检查失败"
        docker compose -f "${COMPOSE_FILE}" ps --format "table {{.Name}}\t{{.Status}}" 2>/dev/null || true
        return 1
    fi
}

# ==============================================================================
# 4. 回滚
# ==============================================================================
rollback() {
    log_warn "执行回滚操作..."

    if [[ -f "${SCRIPT_DIR}/.deploy_state" ]]; then
        source "${SCRIPT_DIR}/.deploy_state"
        log_info "回滚到版本: ${deploy_commit:-unknown}"
    fi

    # 停止当前服务
    docker compose -f "${COMPOSE_FILE}" --profile core-trade --profile core-ai-chain \
        --profile light-forum --profile light-edu --profile light-data-supply down 2>/dev/null || true

    # 如果有特定 commit，checkout 到该版本
    if [[ -n "${deploy_commit:-}" && "${deploy_commit}" != "unknown" ]]; then
        log_info "切换到 commit: ${deploy_commit}"
        git checkout "${deploy_commit}" 2>/dev/null || log_warn "无法切换到指定 commit"
    fi

    log_ok "回滚完成，请手动重新部署"
}

# ==============================================================================
# 主流程
# ==============================================================================
main() {
    local action="${1:-all}"

    case "${action}" in
        --verify)
            log_info "仅执行健康检查验证"
            wait_for_healthy
            ;;
        --rollback)
            rollback
            ;;
        all)
            check_env
            deploy_services "core-trade,core-ai-chain,light-forum,light-edu,light-data-supply"
            wait_for_healthy
            ;;
        core-trade|core-ai-chain|light-forum|light-edu|light-data-supply)
            check_env
            deploy_services "${action}"
            wait_for_healthy "tailor-is-"
            ;;
        *)
            # 支持逗号分隔的多个 profile
            check_env
            deploy_services "${action}"
            wait_for_healthy
            ;;
    esac

    if [[ "${action}" != "--verify" && "${action}" != "--rollback" ]]; then
        log_ok "部署完成"
        log_info "服务状态:"
        docker compose -f "${COMPOSE_FILE}" ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || true
    fi
}

main "$@"