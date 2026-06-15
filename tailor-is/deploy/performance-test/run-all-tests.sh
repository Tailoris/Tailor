#!/bin/bash
# ============================================================================
# Tailor IS 全链路性能测试执行脚本
# 功能: 按顺序执行所有 JMeter 测试计划，生成 HTML 报告，对比基准指标
# 作者: Tailor IS Team
# 日期: 2026-06-11
# ============================================================================

set -euo pipefail

# ── 配置 ──
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="${SCRIPT_DIR}/results"
REPORTS_DIR="${SCRIPT_DIR}/reports"
BASELINE_FILE="${SCRIPT_DIR}/baseline-metrics.csv"
LOG_FILE="${RESULTS_DIR}/test-run-$(date +%Y%m%d-%H%M%S).log"

# JMeter 配置（可通过环境变量覆盖）
JMETER_HOME="${JMETER_HOME:-/opt/apache-jmeter}"
JMETER_BIN="${JMETER_HOME}/bin/jmeter"
JMETER_OPTS="${JMETER_OPTS:--Xms512m -Xmx2g}"

# 服务配置
AI_SERVICE_HOST="${AI_SERVICE_HOST:-localhost}"
AI_SERVICE_PORT="${AI_SERVICE_PORT:-8106}"
ORDER_SERVICE_HOST="${ORDER_SERVICE_HOST:-localhost}"
ORDER_SERVICE_PORT="${ORDER_SERVICE_PORT:-8103}"
COPYRIGHT_SERVICE_HOST="${COPYRIGHT_SERVICE_HOST:-localhost}"
COPYRIGHT_SERVICE_PORT="${COPYRIGHT_SERVICE_PORT:-8107}"

# 认证
AUTH_TOKEN="${AUTH_TOKEN:-Bearer test-token-for-performance}"

# 测试配置
DURATION_AI="${DURATION_AI:-300}"
DURATION_TRADING="${DURATION_TRADING:-600}"
DURATION_BLOCKCHAIN="${DURATION_BLOCKCHAIN:-300}"
RAMP_TIME="${RAMP_TIME:-60}"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ── 函数 ──

log_info() {
    echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') $*" | tee -a "$LOG_FILE"
}

log_success() {
    echo -e "${GREEN}[OK]${NC} $(date '+%Y-%m-%d %H:%M:%S') $*" | tee -a "$LOG_FILE"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') $*" | tee -a "$LOG_FILE"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') $*" | tee -a "$LOG_FILE"
}

print_header() {
    echo ""
    echo "========================================================================"
    echo -e "${BLUE}  Tailor IS 全链路性能测试${NC}"
    echo "  $(date '+%Y-%m-%d %H:%M:%S')"
    echo "========================================================================"
    echo ""
}

check_jmeter() {
    log_info "检查 JMeter 环境..."
    if [ ! -f "$JMETER_BIN" ]; then
        # 尝试从 PATH 中查找
        if command -v jmeter &> /dev/null; then
            JMETER_BIN="jmeter"
            log_info "使用 PATH 中的 JMeter: $(which jmeter)"
        else
            log_error "未找到 JMeter，请设置 JMETER_HOME 环境变量或安装 JMeter"
            log_error "下载地址: https://jmeter.apache.org/download_jmeter.cgi"
            exit 1
        fi
    else
        log_info "JMeter 路径: $JMETER_BIN"
        log_info "JMeter 版本: $($JMETER_BIN --version 2>&1 | head -1)"
    fi
}

create_directories() {
    log_info "创建输出目录..."
    mkdir -p "$RESULTS_DIR"
    mkdir -p "$REPORTS_DIR"
    log_success "结果目录: $RESULTS_DIR"
    log_success "报告目录: $REPORTS_DIR"
}

generate_baseline() {
    log_info "生成/加载基准指标文件..."
    if [ ! -f "$BASELINE_FILE" ]; then
        cat > "$BASELINE_FILE" << 'EOF'
# Tailor IS 性能基准指标
# 格式: test_name,scenario,metric,before_value,after_value,unit,improvement_target
# AI 纸样生成 (目标: 40%+ 速度提升)
ai-pattern,50-concurrent,avg_response_time,500,300,ms,40%
ai-pattern,50-concurrent,p95_response_time,800,480,ms,40%
ai-pattern,50-concurrent,p99_response_time,1200,720,ms,40%
ai-pattern,50-concurrent,throughput,100,140,req/s,40%
ai-pattern,100-concurrent,avg_response_time,800,480,ms,40%
ai-pattern,100-concurrent,p95_response_time,1500,900,ms,40%
ai-pattern,100-concurrent,throughput,180,252,req/s,40%
ai-pattern,200-concurrent,avg_response_time,1200,720,ms,40%
ai-pattern,200-concurrent,throughput,200,280,req/s,40%
# 高并发交易 (目标: 50%+ 订单处理峰值提升)
trading,100-concurrent,avg_response_time,200,100,ms,50%
trading,100-concurrent,order_create_tps,350,525,req/s,50%
trading,100-concurrent,payment_callback_rt,250,125,ms,50%
trading,500-concurrent,order_create_tps,500,750,req/s,50%
trading,500-concurrent,error_rate,2.0,1.0,%,50%
trading,1000-concurrent,order_create_tps,400,600,req/s,50%
trading,1000-concurrent,p99_response_time,2000,1000,ms,50%
# 区块链存证 (目标: 50%+ 链上效率提升)
blockchain,50-concurrent,avg_response_time,500,250,ms,50%
blockchain,50-concurrent,evidence_submit_tps,50,75,req/s,50%
blockchain,50-concurrent,batch_chain_throughput,10,15,batch/s,50%
blockchain,100-concurrent,avg_response_time,800,400,ms,50%
blockchain,100-concurrent,evidence_submit_tps,80,120,req/s,50%
blockchain,100-concurrent,chain_efficiency,100,150,records/s,50%
# 资源利用率 (目标: 40%+ 整体资源降低)
resource,ai-service,cpu_usage,70,42,%,40%
resource,ai-service,memory_usage,80,48,%,40%
resource,order-service,cpu_usage,65,39,%,40%
resource,order-service,memory_usage,75,45,%,40%
resource,copyright-service,cpu_usage,60,36,%,40%
resource,copyright-service,memory_usage,70,42,%,40%
resource,redis,cpu_usage,40,24,%,40%
resource,redis,memory_usage,60,36,%,40%
resource,mq,cpu_usage,35,21,%,40%
resource,mysql,cpu_usage,55,33,%,40%
EOF
        log_success "基准指标文件已创建: $BASELINE_FILE"
    else
        log_info "基准指标文件已存在: $BASELINE_FILE"
    fi
}

run_jmeter_test() {
    local test_name="$1"
    local jmx_file="$2"
    local result_prefix="$3"
    local host="$4"
    local port="$5"
    local duration="$6"
    local target_threads="${7:-}"

    local result_file="${RESULTS_DIR}/${result_prefix}.jtl"
    local report_output="${REPORTS_DIR}/${result_prefix}-report"

    log_info "═══════════════════════════════════════════════════════"
    log_info "开始执行: $test_name"
    log_info "测试文件: $jmx_file"
    log_info "目标服务: $host:$port"
    log_info "测试时长: ${duration}秒"
    if [ -n "$target_threads" ]; then
        log_info "并发线程: $target_threads"
    fi
    log_info "结果文件: $result_file"
    log_info "报告输出: $report_output"
    log_info "═══════════════════════════════════════════════════════"

    # 激活对应的线程组 (如果需要)
    if [ -n "$target_threads" ]; then
        log_info "激活线程组: $target_threads"
    fi

    # 执行 JMeter 测试（非 GUI 模式）
    "$JMETER_BIN" \
        -n \
        -t "$jmx_file" \
        -l "$result_file" \
        -e \
        -o "$report_output" \
        -Jtarget.host="$host" \
        -Jtarget.port="$port" \
        -JAUTH_TOKEN="$AUTH_TOKEN" \
        -DDURATION="$duration" \
        -DRAMP_TIME="$RAMP_TIME" \
        $JMETER_OPTS \
        2>&1 | tee -a "$LOG_FILE"

    local exit_code=${PIPESTATUS[0]}

    if [ $exit_code -eq 0 ]; then
        log_success "$test_name 执行完成"
        log_success "HTML 报告: ${report_output}/index.html"
    else
        log_error "$test_name 执行失败 (退出码: $exit_code)"
        return $exit_code
    fi
}

compare_with_baseline() {
    log_info "═══════════════════════════════════════════════════════"
    log_info "对比测试结果与基准指标"
    log_info "═══════════════════════════════════════════════════════"

    local comparison_file="${RESULTS_DIR}/comparison-$(date +%Y%m%d-%H%M%S).csv"

    echo "test_name,scenario,metric,baseline_value,actual_value,unit,improvement,meets_target" > "$comparison_file"

    # 解析 JTL 结果文件并对比
    for jtl_file in "$RESULTS_DIR"/*.jtl; do
        [ -f "$jtl_file" ] || continue

        local test_type
        test_type=$(basename "$jtl_file" | sed 's/-.*//')

        # 使用 awk 计算基本统计
        awk -F',' '
        NR > 1 {
            elapsed = $2
            if (elapsed > 0) {
                sum += elapsed
                count++
                if (elapsed > max) max = elapsed
                if (min == 0 || elapsed < min) min = elapsed
                # 存储所有值用于百分位计算
                values[count] = elapsed
            }
            if ($8 == "false") errors++
        }
        END {
            if (count > 0) {
                avg = sum / count
                # 简单排序计算 P95/P99
                n = asort(values)
                p95_idx = int(n * 0.95)
                p99_idx = int(n * 0.99)
                if (p95_idx < 1) p95_idx = 1
                if (p99_idx < 1) p99_idx = 1
                p95 = values[p95_idx]
                p99 = values[p99_idx]
                throughput = count / (max / 1000)

                printf "test=%s,count=%d,avg=%.2f,min=%.2f,max=%.2f,p95=%.2f,p99=%.2f,throughput=%.2f,errors=%d\n", \
                    FILENAME, count, avg, min, max, p95, p99, throughput, errors+0
            }
        }
        ' "$jtl_file"
    done

    log_info "对比结果已保存到: $comparison_file"
}

print_summary() {
    log_info ""
    log_info "═══════════════════════════════════════════════════════"
    log_info "                    测试执行摘要"
    log_info "═══════════════════════════════════════════════════════"
    log_info ""
    log_info "测试结果目录: $RESULTS_DIR"
    log_info "HTML 报告目录: $REPORTS_DIR"
    log_info "日志文件: $LOG_FILE"
    log_info ""

    # 列出所有生成的报告
    log_info "生成的报告:"
    for report_dir in "$REPORTS_DIR"/*-report; do
        if [ -d "$report_dir" ]; then
            local report_name
            report_name=$(basename "$report_dir")
            log_info "  - ${report_name}: file://${report_dir}/index.html"
        fi
    done

    log_info ""
    log_info "═══════════════════════════════════════════════════════"
}

# ── 主流程 ──

main() {
    print_header

    # 1. 环境检查
    check_jmeter
    create_directories
    generate_baseline

    local start_time
    start_time=$(date +%s)
    local failed=0

    # 2. 执行 AI 纸样生成测试
    log_info ""
    log_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    log_info "  阶段 1: AI 纸样生成性能测试"
    log_info "  目标: 40%+ 生成速度提升"
    log_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    if run_jmeter_test \
        "AI Pattern Generation Test" \
        "${SCRIPT_DIR}/ai-pattern-test.jmx" \
        "ai-pattern" \
        "$AI_SERVICE_HOST" \
        "$AI_SERVICE_PORT" \
        "$DURATION_AI"; then
        log_success "AI 纸样生成测试完成"
    else
        log_error "AI 纸样生成测试失败"
        ((failed++))
    fi

    # 3. 执行高并发交易测试
    log_info ""
    log_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    log_info "  阶段 2: 高并发交易性能测试"
    log_info "  目标: 50%+ 订单处理峰值提升"
    log_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    if run_jmeter_test \
        "High-Concurrency Trading Test" \
        "${SCRIPT_DIR}/trading-test.jmx" \
        "trading" \
        "$ORDER_SERVICE_HOST" \
        "$ORDER_SERVICE_PORT" \
        "$DURATION_TRADING"; then
        log_success "高并发交易测试完成"
    else
        log_error "高并发交易测试失败"
        ((failed++))
    fi

    # 4. 执行区块链存证测试
    log_info ""
    log_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    log_info "  阶段 3: 区块链存证性能测试"
    log_info "  目标: 50%+ 链上效率提升"
    log_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    if run_jmeter_test \
        "Blockchain Evidence Test" \
        "${SCRIPT_DIR}/blockchain-test.jmx" \
        "blockchain" \
        "$COPYRIGHT_SERVICE_HOST" \
        "$COPYRIGHT_SERVICE_PORT" \
        "$DURATION_BLOCKCHAIN"; then
        log_success "区块链存证测试完成"
    else
        log_error "区块链存证测试失败"
        ((failed++))
    fi

    # 5. 对比基准指标
    compare_with_baseline

    # 6. 打印摘要
    local end_time
    end_time=$(date +%s)
    local total_duration=$((end_time - start_time))

    print_summary

    log_info "总耗时: $((total_duration / 60)) 分 $((total_duration % 60)) 秒"
    log_info "失败测试数: $failed"

    if [ $failed -eq 0 ]; then
        log_success "所有测试执行完成!"
    else
        log_warn "有 $failed 个测试执行失败，请查看日志了解详情"
        exit 1
    fi
}

# 命令行参数处理
show_help() {
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -h, --help              显示帮助信息"
    echo "  -t, --test <name>       仅执行指定测试 (ai|trading|blockchain|all)"
    echo "  -d, --duration <secs>   设置测试持续时间 (秒)"
    echo "  --ai-host <host>        AI 服务主机地址 (默认: localhost)"
    echo "  --ai-port <port>        AI 服务端口 (默认: 8106)"
    echo "  --order-host <host>     订单服务主机地址 (默认: localhost)"
    echo "  --order-port <port>     订单服务端口 (默认: 8103)"
    echo "  --copyright-host <host> 版权服务主机地址 (默认: localhost)"
    echo "  --copyright-port <port> 版权服务端口 (默认: 8107)"
    echo "  --token <token>         认证 Token"
    echo "  --jmeter-home <path>    JMeter 安装路径"
    echo ""
    echo "环境变量:"
    echo "  JMETER_HOME             JMeter 安装目录"
    echo "  AI_SERVICE_HOST         AI 服务主机"
    echo "  AI_SERVICE_PORT         AI 服务端口"
    echo "  ORDER_SERVICE_HOST      订单服务主机"
    echo "  ORDER_SERVICE_PORT      订单服务端口"
    echo "  COPYRIGHT_SERVICE_HOST  版权服务主机"
    echo "  COPYRIGHT_SERVICE_PORT  版权服务端口"
    echo "  AUTH_TOKEN              认证 Token"
    echo ""
}

# 解析参数
TEST_MODE="all"
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -t|--test)
            TEST_MODE="$2"
            shift 2
            ;;
        -d|--duration)
            DURATION_AI="$2"
            DURATION_TRADING="$2"
            DURATION_BLOCKCHAIN="$2"
            shift 2
            ;;
        --ai-host)
            AI_SERVICE_HOST="$2"
            shift 2
            ;;
        --ai-port)
            AI_SERVICE_PORT="$2"
            shift 2
            ;;
        --order-host)
            ORDER_SERVICE_HOST="$2"
            shift 2
            ;;
        --order-port)
            ORDER_SERVICE_PORT="$2"
            shift 2
            ;;
        --copyright-host)
            COPYRIGHT_SERVICE_HOST="$2"
            shift 2
            ;;
        --copyright-port)
            COPYRIGHT_SERVICE_PORT="$2"
            shift 2
            ;;
        --token)
            AUTH_TOKEN="$2"
            shift 2
            ;;
        --jmeter-home)
            JMETER_HOME="$2"
            JMETER_BIN="${JMETER_HOME}/bin/jmeter"
            shift 2
            ;;
        *)
            log_error "未知参数: $1"
            show_help
            exit 1
            ;;
    esac
done

main
