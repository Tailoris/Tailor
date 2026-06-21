#!/bin/bash
# ==============================================================================
# Tailor IS - Kubernetes 回滚脚本
# ==============================================================================
# 功能:
#   1. 回滚到上一个版本 (kubectl rollout undo)
#   2. 回滚到指定 revision 版本
#   3. 支持回滚单个服务或所有服务
#   4. 支持查看回滚历史
#
# 使用方式:
#   chmod +x rollback.sh
#   ./rollback.sh                          # 交互式回滚
#   ./rollback.sh --service user-service   # 回滚指定服务
#   ./rollback.sh --all --to-revision 3    # 回滚所有服务到 revision 3
#   ./rollback.sh --history user-service   # 查看服务回滚历史
# ==============================================================================

set -euo pipefail

# ==============================================================================
# 配置
# ==============================================================================
NAMESPACE="tailor-is-prod"
CONTEXT=""

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 所有微服务列表
ALL_SERVICES=(
    "core-gateway"
    "lite-gateway"
    "user-service"
    "product-service"
    "order-service"
    "payment-service"
    "marketing-service"
    "ai-service"
    "copyright-service"
    "community-service"
    "supply-service"
    "merchant-service"
    "message-service"
    "message-im-service"
    "academy-service"
    "analytics-service"
    "pattern-service"
    "admin-service"
    "api-service"
)

# 基础设施 StatefulSet 列表
INFRA_SERVICES=(
    "mysql"
    "redis"
    "rabbitmq"
)

# ==============================================================================
# 辅助函数
# ==============================================================================
log_info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

kubectl_cmd() {
    if [ -n "$CONTEXT" ]; then
        kubectl --context "$CONTEXT" "$@"
    else
        kubectl "$@"
    fi
}

show_history() {
    local resource="$1"
    local kind="${2:-deployment}"

    log_info "查看 $kind/$resource 回滚历史:"
    echo ""
    kubectl_cmd rollout history "$kind/$resource" -n "$NAMESPACE" 2>/dev/null || {
        log_error "无法获取 $kind/$resource 的回滚历史"
        return 1
    }
}

rollback_resource() {
    local resource="$1"
    local kind="${2:-deployment}"
    local revision="${3:-}"

    if [ -n "$revision" ]; then
        log_info "回滚 $kind/$resource 到 revision $revision..."
        kubectl_cmd rollout undo "$kind/$resource" -n "$NAMESPACE" --to-revision="$revision"
    else
        log_info "回滚 $kind/$resource 到上一个版本..."
        kubectl_cmd rollout undo "$kind/$resource" -n "$NAMESPACE"
    fi

    if [ $? -eq 0 ]; then
        log_info "✓ $kind/$resource 回滚成功"
    else
        log_error "✗ $kind/$resource 回滚失败"
        return 1
    fi
}

wait_for_rollout() {
    local resource="$1"
    local kind="${2:-deployment}"

    log_info "等待 $kind/$resource 回滚完成..."
    if kubectl_cmd rollout status "$kind/$resource" -n "$NAMESPACE" --timeout=300s 2>/dev/null; then
        log_info "✓ $kind/$resource 回滚完成"
        return 0
    else
        log_warn "⚠ $kind/$resource 回滚状态检查超时，请手动验证"
        return 1
    fi
}

# ==============================================================================
# 交互式回滚
# ==============================================================================
interactive_rollback() {
    echo ""
    echo "请选择要回滚的服务类型:"
    echo "  1) 微服务 (Deployment)"
    echo "  2) 基础设施 (StatefulSet)"
    echo "  3) 全部回滚"
    echo "  4) 退出"
    echo ""
    read -rp "请输入选项 [1-4]: " choice

    case $choice in
        1)
            echo ""
            echo "可用的微服务:"
            for i in "${!ALL_SERVICES[@]}"; do
                printf "  %2d) %s\n" "$((i+1))" "${ALL_SERVICES[$i]}"
            done
            echo ""
            read -rp "请选择服务编号 (或 'all' 全部): " svc_choice

            if [ "$svc_choice" = "all" ]; then
                for svc in "${ALL_SERVICES[@]}"; do
                    show_history "$svc"
                    read -rp "是否回滚 $svc? [y/N]: " confirm
                    if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
                        read -rp "回滚到指定 revision? (直接回车=上一个版本): " rev
                        rollback_resource "$svc" "deployment" "$rev"
                        wait_for_rollout "$svc"
                    fi
                done
            elif [[ "$svc_choice" =~ ^[0-9]+$ ]] && [ "$svc_choice" -le "${#ALL_SERVICES[@]}" ]; then
                local svc="${ALL_SERVICES[$((svc_choice-1))]}"
                show_history "$svc"
                read -rp "回滚到指定 revision? (直接回车=上一个版本): " rev
                rollback_resource "$svc" "deployment" "$rev"
                wait_for_rollout "$svc"
            else
                log_error "无效选择"
            fi
            ;;
        2)
            echo ""
            echo "可用的基础设施:"
            for i in "${!INFRA_SERVICES[@]}"; do
                printf "  %d) %s\n" "$((i+1))" "${INFRA_SERVICES[$i]}"
            done
            echo ""
            read -rp "请选择服务编号: " infra_choice

            if [[ "$infra_choice" =~ ^[0-9]+$ ]] && [ "$infra_choice" -le "${#INFRA_SERVICES[@]}" ]; then
                local svc="${INFRA_SERVICES[$((infra_choice-1))]}"
                show_history "$svc" "statefulset"
                read -rp "回滚到指定 revision? (直接回车=上一个版本): " rev
                rollback_resource "$svc" "statefulset" "$rev"
                wait_for_rollout "$svc" "statefulset"
            else
                log_error "无效选择"
            fi
            ;;
        3)
            log_warn "即将回滚所有微服务！"
            read -rp "确认? [y/N]: " confirm
            if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
                for svc in "${ALL_SERVICES[@]}"; do
                    rollback_resource "$svc"
                done
                for svc in "${ALL_SERVICES[@]}"; do
                    wait_for_rollout "$svc" || true
                done
                log_info "全部回滚完成"
            fi
            ;;
        4)
            log_info "退出"
            exit 0
            ;;
        *)
            log_error "无效选项"
            ;;
    esac
}

# ==============================================================================
# 主函数
# ==============================================================================
main() {
    local mode="interactive"
    local service=""
    local revision=""
    local show_hist=false

    while [[ $# -gt 0 ]]; do
        case $1 in
            --service|-s)
                service="$2"
                mode="single"
                shift 2
                ;;
            --all|-a)
                mode="all"
                shift
                ;;
            --to-revision|-r)
                revision="$2"
                shift 2
                ;;
            --history|-H)
                show_hist=true
                mode="history"
                service="$2"
                shift 2
                ;;
            --namespace|-n)
                NAMESPACE="$2"
                shift 2
                ;;
            --context|-c)
                CONTEXT="$2"
                shift 2
                ;;
            --help|-h)
                echo "用法: $0 [选项]"
                echo ""
                echo "选项:"
                echo "  --service, -s NAME    回滚指定服务"
                echo "  --all, -a             回滚所有微服务"
                echo "  --to-revision, -r N   回滚到指定 revision"
                echo "  --history, -H NAME    查看服务回滚历史"
                echo "  --namespace, -n NS    指定命名空间"
                echo "  --context, -c CTX     指定 kubeconfig context"
                echo "  --help, -h            显示帮助"
                echo ""
                echo "示例:"
                echo "  $0                                    # 交互式回滚"
                echo "  $0 --service user-service             # 回滚用户服务"
                echo "  $0 --service user-service -r 3        # 回滚到 revision 3"
                echo "  $0 --all                              # 回滚所有服务"
                echo "  $0 --history user-service             # 查看回滚历史"
                exit 0
                ;;
            *)
                log_error "未知参数: $1"
                exit 1
                ;;
        esac
    done

    echo ""
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║           Tailor IS - Kubernetes 回滚脚本                    ║"
    echo "╠══════════════════════════════════════════════════════════════╣"
    echo "║  命名空间:  $NAMESPACE"
    echo "║  模式:      $mode"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo ""

    # 检查 kubectl
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl 未安装"
        exit 1
    fi

    case $mode in
        interactive)
            interactive_rollback
            ;;
        single)
            if [ -z "$service" ]; then
                log_error "请指定 --service"
                exit 1
            fi
            show_history "$service"
            rollback_resource "$service" "deployment" "$revision"
            wait_for_rollout "$service"
            ;;
        all)
            log_warn "即将回滚所有微服务！"
            if [ "$revision" != "" ]; then
                log_warn "回滚到 revision: $revision"
            else
                log_warn "回滚到上一个版本"
            fi
            read -rp "确认? [y/N]: " confirm
            if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
                for svc in "${ALL_SERVICES[@]}"; do
                    rollback_resource "$svc" "deployment" "$revision"
                done
                for svc in "${ALL_SERVICES[@]}"; do
                    wait_for_rollout "$svc" || true
                done
                log_info "全部回滚完成"
            fi
            ;;
        history)
            show_history "$service"
            # Also show StatefulSet history
            for infra in "${INFRA_SERVICES[@]}"; do
                if [ "$service" = "$infra" ] || [ "$service" = "all" ]; then
                    show_history "$infra" "statefulset"
                fi
            done
            ;;
    esac
}

main "$@"