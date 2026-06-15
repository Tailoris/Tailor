#!/bin/bash
# ==============================================================================
# Tailor IS - 阶段3 性能压测脚本 (P3-2: 全链路性能压测)
# ==============================================================================
# 用途: 使用 k6 对核心业务链路进行性能压测
# 目标: P95 ≤ 200ms, QPS ≥ 1000
#
# 前置条件:
#   1. 安装 k6: https://k6.io/docs/get-started/installation/
#   2. 微服务集群已启动
#   3. 基础测试数据已准备
#
# 使用方式:
#   ./deploy/perf/run-perf-test.sh all          # 全量压测
#   ./deploy/perf/run-perf-test.sh smoke        # 冒烟测试 (轻量)
#   ./deploy/perf/run-perf-test.sh stress       # 压力测试 (逐步加压)
#   ./deploy/perf/run-perf-test.sh endurance    # 耐久测试 (长时间)
# ==============================================================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

BASE_URL="${BASE_URL:-http://localhost:8080}"
TEST_TYPE="${1:-all}"
RESULTS_DIR="./deploy/perf/results/$(date +%Y%m%d-%H%M%S)"
K6_SCRIPT_DIR="./deploy/perf/k6-scripts"

log_info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
log_step()  { echo -e "${BLUE}[STEP]${NC} $1"; }

mkdir -p "$RESULTS_DIR"

# ============================================================================
# 冒烟测试 (验证服务可用性)
# ============================================================================
run_smoke_test() {
    log_step "执行冒烟测试 (Smoke Test)..."
    k6 run --out json="$RESULTS_DIR/smoke.json" \
        --env BASE_URL="$BASE_URL" \
        --env VUS=1 \
        --env DURATION=10s \
        "$K6_SCRIPT_DIR/smoke-test.js" 2>&1 | tee "$RESULTS_DIR/smoke.log"
    log_info "冒烟测试完成"
}

# ============================================================================
# 负载测试 (模拟正常流量)
# ============================================================================
run_load_test() {
    log_step "执行负载测试 (Load Test) - 目标 QPS ≥ 1000..."
    k6 run --out json="$RESULTS_DIR/load.json" \
        --env BASE_URL="$BASE_URL" \
        --env VUS=50 \
        --env DURATION=60s \
        "$K6_SCRIPT_DIR/load-test.js" 2>&1 | tee "$RESULTS_DIR/load.log"
    log_info "负载测试完成"
}

# ============================================================================
# 压力测试 (逐步加压直到极限)
# ============================================================================
run_stress_test() {
    log_step "执行压力测试 (Stress Test) - 逐步加压..."
    k6 run --out json="$RESULTS_DIR/stress.json" \
        --env BASE_URL="$BASE_URL" \
        --env VUS=10 \
        --env DURATION=30s \
        "$K6_SCRIPT_DIR/stress-test.js" 2>&1 | tee "$RESULTS_DIR/stress.log"
    log_info "压力测试完成"
}

# ============================================================================
# 耐久测试 (长时间稳定性)
# ============================================================================
run_endurance_test() {
    log_step "执行耐久测试 (Endurance Test) - 持续 30 分钟..."
    k6 run --out json="$RESULTS_DIR/endurance.json" \
        --env BASE_URL="$BASE_URL" \
        --env VUS=30 \
        --env DURATION=1800s \
        "$K6_SCRIPT_DIR/endurance-test.js" 2>&1 | tee "$RESULTS_DIR/endurance.log"
    log_info "耐久测试完成"
}

# ============================================================================
# 核心业务链路专项测试
# ============================================================================
run_business_flow_test() {
    log_step "执行核心业务链路测试..."
    k6 run --out json="$RESULTS_DIR/business-flow.json" \
        --env BASE_URL="$BASE_URL" \
        --env VUS=20 \
        --env DURATION=120s \
        "$K6_SCRIPT_DIR/business-flow-test.js" 2>&1 | tee "$RESULTS_DIR/business-flow.log"
    log_info "业务链路测试完成"
}

# ============================================================================
# 主流程
# ============================================================================
echo "============================================================"
echo "  Tailor IS 性能压测 - 阶段3 (P3-2)"
echo "  目标: P95 ≤ 200ms, QPS ≥ 1000"
echo "  测试时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "  结果目录: $RESULTS_DIR"
echo "============================================================"

case "$TEST_TYPE" in
    smoke)
        run_smoke_test
        ;;
    stress)
        run_smoke_test
        run_stress_test
        ;;
    endurance)
        run_smoke_test
        run_endurance_test
        ;;
    business)
        run_smoke_test
        run_business_flow_test
        ;;
    all|*)
        run_smoke_test
        run_load_test
        run_business_flow_test
        ;;
esac

echo ""
echo "============================================================"
echo "  压测完成! 结果保存在: $RESULTS_DIR"
echo "  JSON 结果可用于 Grafana 可视化"
echo "============================================================"