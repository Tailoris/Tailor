#!/bin/bash
# ==============================================================================
# Tailor IS - Kubernetes 一键部署脚本
# ==============================================================================
# 功能:
#   1. 按顺序部署所有 K8s 资源: namespace → secrets → configmap → pvc
#      → statefulset → deployments → services → ingress → hpa → monitoring
#   2. 每步完成后验证部署状态
#   3. 支持 --dry-run 预览模式
#   4. 支持 --skip-infra 跳过基础设施 (MySQL/Redis/RabbitMQ)
#
# 使用方式:
#   chmod +x deploy-to-k8s.sh
#   ./deploy-to-k8s.sh                          # 完整部署
#   ./deploy-to-k8s.sh --dry-run                # 预览模式
#   ./deploy-to-k8s.sh --skip-infra             # 跳过基础设施
#   ./deploy-to-k8s.sh --namespace my-ns        # 指定命名空间
#   ./deploy-to-k8s.sh --context prod-cluster   # 指定 kubeconfig context
# ==============================================================================

set -euo pipefail

# ==============================================================================
# 配置变量
# ==============================================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PRODUCTION_DIR="${SCRIPT_DIR}/production"
NAMESPACE="tailor-is-prod"
KUBECTL_OPTS=""
DRY_RUN=false
SKIP_INFRA=false
CONTEXT=""
TIMEOUT_DEPLOY=300
TIMEOUT_STATEFULSET=600

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ==============================================================================
# 辅助函数
# ==============================================================================
log_info() {
    echo -e "${GREEN}[INFO]${NC}  $(date '+%Y-%m-%d %H:%M:%S') - $*"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC}  $(date '+%Y-%m-%d %H:%M:%S') - $*"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $*"
}

log_step() {
    echo -e "\n${BLUE}============================================${NC}"
    echo -e "${BLUE}[STEP]${NC} $*"
    echo -e "${BLUE}============================================${NC}"
}

kubectl_cmd() {
    if [ -n "$CONTEXT" ]; then
        kubectl --context "$CONTEXT" "$@"
    else
        kubectl "$@"
    fi
}

apply_resource() {
    local file="$1"
    local desc="$2"

    if [ "$DRY_RUN" = true ]; then
        log_info "[DRY-RUN] 将应用: $desc ($file)"
        kubectl_cmd apply -f "$file" --dry-run=client $KUBECTL_OPTS 2>&1 || true
        return 0
    fi

    log_info "应用: $desc ($file)"
    if kubectl_cmd apply -f "$file" $KUBECTL_OPTS; then
        log_info "✓ $desc 应用成功"
        return 0
    else
        log_error "✗ $desc 应用失败"
        return 1
    fi
}

wait_for_deployment() {
    local name="$1"
    local timeout="${2:-$TIMEOUT_DEPLOY}"

    if [ "$DRY_RUN" = true ]; then
        log_info "[DRY-RUN] 将等待 Deployment: $name"
        return 0
    fi

    log_info "等待 Deployment/$name 就绪 (超时: ${timeout}s)..."
    if kubectl_cmd wait --for=condition=available --timeout="${timeout}s" \
        deployment/"$name" -n "$NAMESPACE" 2>/dev/null; then
        log_info "✓ Deployment/$name 就绪"
        return 0
    else
        log_warn "⚠ Deployment/$name 等待超时，请手动检查"
        return 1
    fi
}

wait_for_statefulset() {
    local name="$1"
    local timeout="${2:-$TIMEOUT_STATEFULSET}"

    if [ "$DRY_RUN" = true ]; then
        log_info "[DRY-RUN] 将等待 StatefulSet: $name"
        return 0
    fi

    log_info "等待 StatefulSet/$name 就绪 (超时: ${timeout}s)..."
    if kubectl_cmd wait --for=jsonpath='{.status.readyReplicas}'=1 \
        --timeout="${timeout}s" statefulset/"$name" -n "$NAMESPACE" 2>/dev/null; then
        log_info "✓ StatefulSet/$name 就绪"
        return 0
    else
        log_warn "⚠ StatefulSet/$name 等待超时，请手动检查"
        return 1
    fi
}

verify_deployment() {
    if [ "$DRY_RUN" = true ]; then
        return 0
    fi

    log_step "验证部署状态"

    echo ""
    echo "=== Pods ==="
    kubectl_cmd get pods -n "$NAMESPACE" -o wide 2>/dev/null || true

    echo ""
    echo "=== Services ==="
    kubectl_cmd get svc -n "$NAMESPACE" 2>/dev/null || true

    echo ""
    echo "=== Deployments ==="
    kubectl_cmd get deployments -n "$NAMESPACE" 2>/dev/null || true

    echo ""
    echo "=== StatefulSets ==="
    kubectl_cmd get statefulsets -n "$NAMESPACE" 2>/dev/null || true

    echo ""
    echo "=== HPA ==="
    kubectl_cmd get hpa -n "$NAMESPACE" 2>/dev/null || true

    echo ""
    echo "=== PVC ==="
    kubectl_cmd get pvc -n "$NAMESPACE" 2>/dev/null || true

    echo ""
    echo "=== Ingress ==="
    kubectl_cmd get ingress -n "$NAMESPACE" 2>/dev/null || true
}

# ==============================================================================
# 部署函数
# ==============================================================================
deploy_namespace() {
    log_step "1. 创建命名空间"
    apply_resource "${PRODUCTION_DIR}/namespace.yaml" "Namespace"
}

deploy_secrets() {
    log_step "2. 创建 Secrets"
    apply_resource "${PRODUCTION_DIR}/secrets.yaml" "Secrets"
}

deploy_configmap() {
    log_step "3. 创建 ConfigMap"
    apply_resource "${PRODUCTION_DIR}/configmap.yaml" "ConfigMap"
}

deploy_pvc() {
    log_step "4. 创建持久化存储 (PVC)"
    apply_resource "${PRODUCTION_DIR}/pvc.yaml" "PVCs"

    if [ "$DRY_RUN" = false ]; then
        log_info "等待 PVC 绑定..."
        kubectl_cmd wait --for=jsonpath='{.status.phase}'=Bound \
            --timeout=120s pvc --all -n "$NAMESPACE" 2>/dev/null || \
            log_warn "部分 PVC 未绑定，请检查 StorageClass 配置"
    fi
}

deploy_statefulsets() {
    log_step "5. 部署 StatefulSets (MySQL / Redis / RabbitMQ)"

    apply_resource "${PRODUCTION_DIR}/statefulset-mysql.yaml" "MySQL StatefulSet"
    apply_resource "${PRODUCTION_DIR}/statefulset-redis.yaml" "Redis StatefulSet"
    apply_resource "${PRODUCTION_DIR}/statefulset-rabbitmq.yaml" "RabbitMQ StatefulSet"

    wait_for_statefulset "mysql"
    wait_for_statefulset "redis"
    wait_for_statefulset "rabbitmq"
}

deploy_microservices() {
    log_step "6. 部署微服务 (Deployments + Services)"

    local services=(
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

    for svc in "${services[@]}"; do
        local file="${PRODUCTION_DIR}/deployments/${svc}.yaml"
        if [ -f "$file" ]; then
            apply_resource "$file" "$svc"
        else
            log_warn "文件不存在: $file，跳过"
        fi
    done

    log_info "等待所有微服务 Deployment 就绪..."
    for svc in "${services[@]}"; do
        wait_for_deployment "$svc" 180 || true
    done
}

deploy_ingress() {
    log_step "7. 部署 Ingress"
    apply_resource "${PRODUCTION_DIR}/ingress.yaml" "Ingress"
}

deploy_hpa() {
    log_step "8. 部署 HorizontalPodAutoscaler"
    apply_resource "${PRODUCTION_DIR}/hpa.yaml" "HPA"
}

deploy_monitoring() {
    log_step "9. 部署监控 (Prometheus / Grafana / ServiceMonitor)"

    apply_resource "${PRODUCTION_DIR}/monitoring/prometheus.yaml" "Prometheus"
    apply_resource "${PRODUCTION_DIR}/monitoring/grafana.yaml" "Grafana"
    apply_resource "${PRODUCTION_DIR}/monitoring/service-monitor.yaml" "ServiceMonitor"

    wait_for_deployment "prometheus" 120 || true
    wait_for_deployment "grafana" 120 || true
}

deploy_network_policy() {
    log_step "10. 部署网络策略"
    apply_resource "${PRODUCTION_DIR}/network-policy.yaml" "NetworkPolicy"
}

# ==============================================================================
# 主函数
# ==============================================================================
main() {
    # 解析参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            --dry-run)
                DRY_RUN=true
                shift
                ;;
            --skip-infra)
                SKIP_INFRA=true
                shift
                ;;
            --namespace)
                NAMESPACE="$2"
                shift 2
                ;;
            --context)
                CONTEXT="$2"
                shift 2
                ;;
            --help|-h)
                echo "用法: $0 [选项]"
                echo ""
                echo "选项:"
                echo "  --dry-run         预览模式，不实际部署"
                echo "  --skip-infra      跳过基础设施 (MySQL/Redis/RabbitMQ/PVC)"
                echo "  --namespace NS    指定命名空间 (默认: tailor-is-prod)"
                echo "  --context CTX     指定 kubeconfig context"
                echo "  --help, -h        显示帮助"
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
    echo "║         Tailor IS - Kubernetes 生产环境部署脚本              ║"
    echo "╠══════════════════════════════════════════════════════════════╣"
    echo "║  命名空间:  $NAMESPACE"
    echo "║  模式:      $([ "$DRY_RUN" = true ] && echo 'DRY-RUN (预览)' || echo 'LIVE (实际部署)')"
    echo "║  基础设施:  $([ "$SKIP_INFRA" = true ] && echo '跳过' || echo '部署')"
    echo "║  Context:   ${CONTEXT:-默认}"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo ""

    if [ "$DRY_RUN" = false ]; then
        # 检查 kubectl 可用性
        if ! command -v kubectl &> /dev/null; then
            log_error "kubectl 未安装，请先安装 kubectl"
            exit 1
        fi

        # 检查集群连接
        if ! kubectl_cmd cluster-info &> /dev/null; then
            log_error "无法连接到 Kubernetes 集群，请检查 kubeconfig 配置"
            log_error "提示: 使用 --context 指定正确的集群上下文"
            exit 1
        fi

        log_info "集群连接正常"
    fi

    # 按顺序执行部署
    deploy_namespace

    # 等待命名空间就绪
    sleep 2

    deploy_secrets
    deploy_configmap

    if [ "$SKIP_INFRA" = false ]; then
        deploy_pvc
        deploy_statefulsets
    else
        log_warn "跳过基础设施部署 (--skip-infra)"
    fi

    deploy_microservices
    deploy_ingress
    deploy_hpa
    deploy_monitoring
    deploy_network_policy

    # 最终验证
    verify_deployment

    echo ""
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║                    部署完成!                                 ║"
    echo "╠══════════════════════════════════════════════════════════════╣"
    echo "║  查看 Pod 状态:    kubectl get pods -n $NAMESPACE"
    echo "║  查看日志:         kubectl logs -f deployment/<name> -n $NAMESPACE"
    echo "║  查看 Ingress:     kubectl get ingress -n $NAMESPACE"
    echo "║  Grafana:          http://grafana.$NAMESPACE:3000"
    echo "║  Prometheus:       http://prometheus.$NAMESPACE:9090"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo ""

    if [ "$DRY_RUN" = false ]; then
        log_info "提示: 如果部分 Pod 未就绪，请运行以下命令排查:"
        log_info "  kubectl describe pod <pod-name> -n $NAMESPACE"
        log_info "  kubectl logs <pod-name> -n $NAMESPACE"
    fi
}

main "$@"