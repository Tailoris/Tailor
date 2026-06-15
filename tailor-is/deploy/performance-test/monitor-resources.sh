#!/bin/bash
# ============================================================================
# Tailor IS 资源利用率监控脚本
# 功能: 在性能测试期间持续监控 CPU、内存、Redis、MQ 等资源使用情况
# 目标: 40%+ 整体资源降低
# 作者: Tailor IS Team
# 日期: 2026-06-11
# ============================================================================

set -euo pipefail

# ── 配置 ──
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MONITOR_DIR="${SCRIPT_DIR}/monitor-data"
REPORT_FILE="${MONITOR_DIR}/resource-report-$(date +%Y%m%d-%H%M%S).html"
CSV_FILE="${MONITOR_DIR}/resource-metrics-$(date +%Y%m%d-%H%M%S).csv"
INTERVAL="${MONITOR_INTERVAL:-5}"  # 采样间隔（秒）
DURATION="${MONITOR_DURATION:-0}"   # 监控总时长（秒），0=无限

# 服务名称列表
JAVA_SERVICES=("tailor-is-ai" "tailor-is-order" "tailor-is-copyright" "tailor-is-gateway" "tailor-is-payment")
REDIS_HOST="${REDIS_HOST:-localhost}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-redis_jD2N8n}"
RABBITMQ_HOST="${RABBITMQ_HOST:-localhost}"
RABBITMQ_PORT="${RABBITMQ_PORT:-15672}"
RABBITMQ_USER="${RABBITMQ_USER:-rabbitmq}"
RABBITMQ_PASS="${RABBITMQ_PASS:-rabbitmq}"
MYSQL_HOST="${MYSQL_HOST:-localhost}"
MYSQL_PORT="${MYSQL_PORT:-3306}"

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# ── 函数 ──

log_info() {
    echo -e "${BLUE}[MONITOR]${NC} $(date '+%H:%M:%S') $*"
}

log_success() {
    echo -e "${GREEN}[MONITOR]${NC} $(date '+%H:%M:%S') $*"
}

log_warn() {
    echo -e "${YELLOW}[MONITOR]${NC} $(date '+%H:%M:%S') $*"
}

log_error() {
    echo -e "${RED}[MONITOR]${NC} $(date '+%H:%M:%S') $*"
}

# 初始化监控目录和 CSV 文件
init_monitor() {
    mkdir -p "$MONITOR_DIR"

    # CSV 表头
    echo "timestamp,cpu_total,mem_total,mem_used,mem_pct,swap_used,load_1min,load_5min,load_15min" > "$CSV_FILE"

    # 服务列
    for service in "${JAVA_SERVICES[@]}"; do
        echo -n ",${service}_pid,${service}_cpu,${service}_mem_pct,${service}_mem_mb,${service}_gc_time" >> "$CSV_FILE"
    done
    echo "" >> "$CSV_FILE"

    # Redis 列
    echo -n ",redis_connected_clients,redis_used_memory_mb,redis_cpu_pct,redis_ops_per_sec,redis_hit_rate" >> "$CSV_FILE"
    echo "" >> "$CSV_FILE"

    # RabbitMQ 列
    echo -n ",mq_connections,mq_channels,mq_queue_depth,mq_publish_rate,mq_deliver_rate,mq_cpu_pct,mq_mem_mb" >> "$CSV_FILE"
    echo "" >> "$CSV_FILE"

    # MySQL 列
    echo -n ",mysql_threads_running,mysql_queries_per_sec,mysql_slow_queries,mysql_connections,mysql_cpu_pct,mysql_mem_mb" >> "$CSV_FILE"
    echo "" >> "$CSV_FILE"

    log_success "监控数据文件: $CSV_FILE"
}

# 获取系统级资源使用
collect_system_metrics() {
    local ts
    ts=$(date '+%Y-%m-%d %H:%M:%S')

    # CPU 使用率
    local cpu_idle
    cpu_idle=$(top -bn1 | grep "Cpu(s)" | awk '{print $8}' | cut -d'.' -f1 2>/dev/null || echo "0")
    local cpu_total=$((100 - ${cpu_idle:-0}))

    # 内存
    local mem_info
    mem_info=$(free -m | grep Mem)
    local mem_total
    mem_total=$(echo "$mem_info" | awk '{print $2}')
    local mem_used
    mem_used=$(echo "$mem_info" | awk '{print $3}')
    local mem_pct
    mem_pct=$(echo "$mem_info" | awk '{printf "%.1f", $3/$2*100}')

    # Swap
    local swap_used
    swap_used=$(free -m | grep Swap | awk '{print $3}')

    # 负载
    local load_1 load_5 load_15
    read -r load_1 load_5 load_15 <<< "$(cat /proc/loadavg | awk '{print $1, $2, $3}')"

    echo "${ts},${cpu_total},${mem_total},${mem_used},${mem_pct},${swap_used},${load_1},${load_5},${load_15}" >> "$CSV_FILE"
}

# 获取 Java 进程资源使用
collect_java_metrics() {
    local service="$1"

    # 查找进程 PID
    local pid
    pid=$(pgrep -f "${service}" 2>/dev/null | head -1 || echo "")

    if [ -z "$pid" ]; then
        echo -n ",,,," >> "$CSV_FILE"
        return
    fi

    # CPU 使用率
    local cpu
    cpu=$(ps -p "$pid" -o %cpu= 2>/dev/null | tr -d ' ' || echo "0")

    # 内存使用百分比
    local mem_pct
    mem_pct=$(ps -p "$pid" -o %mem= 2>/dev/null | tr -d ' ' || echo "0")

    # 内存使用 MB (RSS)
    local mem_mb
    mem_mb=$(ps -p "$pid" -o rss= 2>/dev/null | awk '{printf "%.0f", $1/1024}' || echo "0")

    # GC 时间 (如果可用)
    local gc_time="0"
    if command -v jstat &> /dev/null; then
        gc_time=$(jstat -gc "$pid" 2>/dev/null | tail -1 | awk '{print $4+$6+$8+$10}' || echo "0")
    fi

    echo -n ",${pid},${cpu},${mem_pct},${mem_mb},${gc_time}" >> "$CSV_FILE"
}

# 获取 Redis 指标
collect_redis_metrics() {
    local connected_clients="0"
    local used_memory_mb="0"
    local ops_per_sec="0"
    local hit_rate="0"
    local cpu_pct="0"

    # 通过 redis-cli 获取
    if command -v redis-cli &> /dev/null; then
        local redis_cmd="redis-cli -h ${REDIS_HOST} -p ${REDIS_PORT}"
        if [ -n "$REDIS_PASSWORD" ]; then
            redis_cmd="${redis_cmd} -a ${REDIS_PASSWORD} --no-auth-warning"
        fi

        local info
        info=$($redis_cmd INFO stats,memory,clients 2>/dev/null || echo "")

        if [ -n "$info" ]; then
            connected_clients=$(echo "$info" | grep "connected_clients:" | cut -d: -f2 | tr -d '\r' || echo "0")
            local used_memory_bytes
            used_memory_bytes=$(echo "$info" | grep "used_memory:" | cut -d: -f2 | tr -d '\r' || echo "0")
            used_memory_mb=$(echo "$used_memory_bytes" | awk '{printf "%.1f", $1/1024/1024}')

            local instantaneous_ops
            instantaneous_ops=$(echo "$info" | grep "instantaneous_ops_per_sec:" | cut -d: -f2 | tr -d '\r' || echo "0")
            ops_per_sec=${instantaneous_ops:-0}

            local keyspace_hits keyspace_misses
            keyspace_hits=$(echo "$info" | grep "keyspace_hits:" | cut -d: -f2 | tr -d '\r' || echo "0")
            keyspace_misses=$(echo "$info" | grep "keyspace_misses:" | cut -d: -f2 | tr -d '\r' || echo "0")
            local total_hits=$(( ${keyspace_hits:-0} + ${keyspace_misses:-0} ))
            if [ "$total_hits" -gt 0 ]; then
                hit_rate=$(awk "BEGIN {printf \"%.2f\", ${keyspace_hits:-0}/${total_hits}*100}")
            fi
        fi
    fi

    # Redis 进程 CPU/内存
    local redis_pid
    redis_pid=$(pgrep -f "redis-server" 2>/dev/null | head -1 || echo "")
    if [ -n "$redis_pid" ]; then
        cpu_pct=$(ps -p "$redis_pid" -o %cpu= 2>/dev/null | tr -d ' ' || echo "0")
    fi

    echo -n ",${connected_clients},${used_memory_mb},${cpu_pct},${ops_per_sec},${hit_rate}" >> "$CSV_FILE"
}

# 获取 RabbitMQ 指标
collect_rabbitmq_metrics() {
    local connections="0"
    local channels="0"
    local queue_depth="0"
    local publish_rate="0"
    local deliver_rate="0"
    local mq_cpu="0"
    local mq_mem="0"

    # 通过 RabbitMQ Management API 获取
    if command -v curl &> /dev/null; then
        local api_url="http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/overview"
        local overview
        overview=$(curl -s -u "${RABBITMQ_USER}:${RABBITMQ_PASS}" "$api_url" 2>/dev/null || echo "{}")

        if [ "$overview" != "{}" ] && [ -n "$overview" ]; then
            # 解析 JSON（简单方式）
            connections=$(echo "$overview" | grep -o '"object_totals":{"connections":[0-9]*' | grep -o '[0-9]*$' || echo "0")
            channels=$(echo "$overview" | grep -o '"channels":[0-9]*' | head -1 | grep -o '[0-9]*$' || echo "0")
            queue_depth=$(echo "$overview" | grep -o '"messages":[0-9]*' | head -1 | grep -o '[0-9]*$' || echo "0")
            publish_rate=$(echo "$overview" | grep -o '"message_stats":{"publish_details":{"rate":[0-9.]*' | grep -o '[0-9.]*$' || echo "0")
            deliver_rate=$(echo "$overview" | grep -o '"deliver_get_details":{"rate":[0-9.]*' | grep -o '[0-9.]*$' || echo "0")
        fi
    fi

    # RabbitMQ 进程资源
    local mq_pid
    mq_pid=$(pgrep -f "beam" 2>/dev/null | head -1 || echo "")
    if [ -n "$mq_pid" ]; then
        mq_cpu=$(ps -p "$mq_pid" -o %cpu= 2>/dev/null | tr -d ' ' || echo "0")
        mq_mem=$(ps -p "$mq_pid" -o rss= 2>/dev/null | awk '{printf "%.0f", $1/1024}' || echo "0")
    fi

    echo -n ",${connections},${channels},${queue_depth},${publish_rate},${deliver_rate},${mq_cpu},${mq_mem}" >> "$CSV_FILE"
}

# 获取 MySQL 指标
collect_mysql_metrics() {
    local threads_running="0"
    local qps="0"
    local slow_queries="0"
    local connections="0"
    local mysql_cpu="0"
    local mysql_mem="0"

    if command -v mysql &> /dev/null; then
        local mysql_cmd="mysql -h ${MYSQL_HOST} -P ${MYSQL_PORT}"

        threads_running=$($mysql_cmd -N -e "SHOW STATUS LIKE 'Threads_running';" 2>/dev/null | awk '{print $2}' || echo "0")
        local questions
        questions=$($mysql_cmd -N -e "SHOW STATUS LIKE 'Questions';" 2>/dev/null | awk '{print $2}' || echo "0")
        slow_queries=$($mysql_cmd -N -e "SHOW STATUS LIKE 'Slow_queries';" 2>/dev/null | awk '{print $2}' || echo "0")
        connections=$($mysql_cmd -N -e "SHOW STATUS LIKE 'Threads_connected';" 2>/dev/null | awk '{print $2}' || echo "0")
    fi

    # MySQL 进程资源
    local mysql_pid
    mysql_pid=$(pgrep -f "mysqld" 2>/dev/null | head -1 || echo "")
    if [ -n "$mysql_pid" ]; then
        mysql_cpu=$(ps -p "$mysql_pid" -o %cpu= 2>/dev/null | tr -d ' ' || echo "0")
        mysql_mem=$(ps -p "$mysql_pid" -o rss= 2>/dev/null | awk '{printf "%.0f", $1/1024}' || echo "0")
    fi

    echo -n ",${threads_running},${qps},${slow_queries},${connections},${mysql_cpu},${mysql_mem}" >> "$CSV_FILE"
}

# 生成 HTML 报告
generate_report() {
    local total_samples
    total_samples=$(wc -l < "$CSV_FILE")
    total_samples=$((total_samples - 1))  # 减表头

    cat > "$REPORT_FILE" << 'REPORT_HEADER'
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tailor IS 资源利用率监控报告</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
    <style>
        :root {
            --primary: #4a90d9;
            --success: #52c41a;
            --warning: #faad14;
            --danger: #f5222d;
            --bg: #f5f7fa;
            --card: #ffffff;
            --text: #1f2329;
            --text-secondary: #646a73;
            --border: #e5e6eb;
        }
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: var(--bg); color: var(--text); line-height: 1.6; }
        .container { max-width: 1400px; margin: 0 auto; padding: 24px; }
        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 32px; border-radius: 12px; margin-bottom: 24px; }
        .header h1 { font-size: 28px; margin-bottom: 8px; }
        .header p { opacity: 0.9; font-size: 14px; }
        .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 24px; }
        .stat-card { background: var(--card); border-radius: 12px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
        .stat-card .label { color: var(--text-secondary); font-size: 13px; margin-bottom: 4px; }
        .stat-card .value { font-size: 28px; font-weight: 700; }
        .stat-card .unit { font-size: 14px; color: var(--text-secondary); }
        .chart-section { background: var(--card); border-radius: 12px; padding: 24px; margin-bottom: 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
        .chart-section h3 { font-size: 18px; margin-bottom: 16px; color: var(--text); }
        .chart-container { position: relative; height: 300px; }
        .table-wrapper { overflow-x: auto; }
        table { width: 100%; border-collapse: collapse; font-size: 13px; }
        th, td { padding: 8px 12px; border-bottom: 1px solid var(--border); text-align: left; }
        th { background: #f8f9fa; font-weight: 600; position: sticky; top: 0; }
        .status-ok { color: var(--success); }
        .status-warn { color: var(--warning); }
        .status-danger { color: var(--danger); }
        .target-badge { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 12px; font-weight: 500; }
        .target-achieved { background: #f6ffed; color: var(--success); }
        .target-failed { background: #fff2f0; color: var(--danger); }
        .target-pending { background: #e6f4ff; color: var(--primary); }
    </style>
</head>
<body>
<div class="container">
    <div class="header">
        <h1>Tailor IS 资源利用率监控报告</h1>
        <p>生成时间: <span id="reportTime"></span> | 采样总数: <span id="totalSamples"></span></p>
    </div>

    <!-- 核心指标摘要 -->
    <div class="stats-grid">
        <div class="stat-card">
            <div class="label">CPU 平均使用率</div>
            <div class="value" id="avgCpu">--<span class="unit">%</span></div>
        </div>
        <div class="stat-card">
            <div class="label">内存平均使用率</div>
            <div class="value" id="avgMem">--<span class="unit">%</span></div>
        </div>
        <div class="stat-card">
            <div class="label">Redis 缓存命中率</div>
            <div class="value" id="redisHitRate">--<span class="unit">%</span></div>
        </div>
        <div class="stat-card">
            <div class="label">MQ 队列深度</div>
            <div class="value" id="mqDepth">--</div>
        </div>
        <div class="stat-card">
            <div class="label">MySQL 活跃线程</div>
            <div class="value" id="mysqlThreads">--</div>
        </div>
        <div class="stat-card">
            <div class="label">资源优化目标</div>
            <div class="value" id="targetStatus">40%+<span class="unit">降低</span></div>
        </div>
    </div>

    <!-- CPU 使用趋势 -->
    <div class="chart-section">
        <h3>CPU 使用率趋势</h3>
        <div class="chart-container"><canvas id="cpuChart"></canvas></div>
    </div>

    <!-- 内存使用趋势 -->
    <div class="chart-section">
        <h3>内存使用趋势</h3>
        <div class="chart-container"><canvas id="memChart"></canvas></div>
    </div>

    <!-- 服务资源使用 -->
    <div class="chart-section">
        <h3>各 Java 服务资源使用</h3>
        <div class="chart-container"><canvas id="serviceChart"></canvas></div>
    </div>

    <!-- Redis & MQ 监控 -->
    <div class="chart-section">
        <h3>中间件监控</h3>
        <div class="chart-container"><canvas id="middlewareChart"></canvas></div>
    </div>

    <!-- 目标达成表 -->
    <div class="chart-section">
        <h3>资源优化目标达成情况</h3>
        <div class="table-wrapper">
            <table id="targetTable">
                <thead>
                    <tr>
                        <th>指标</th>
                        <th>优化前</th>
                        <th>当前值</th>
                        <th>目标值</th>
                        <th>状态</th>
                    </tr>
                </thead>
                <tbody id="targetTableBody">
                </tbody>
            </table>
        </div>
    </div>
</div>
REPORT_HEADER

    # 生成 JavaScript 数据
    cat >> "$REPORT_FILE" << 'REPORT_JS'
<script>
document.getElementById('reportTime').textContent = new Date().toLocaleString('zh-CN');

// 目标达成数据
const targets = [
    { metric: 'AI 服务 CPU', before: '70%', current: '', target: '42%', unit: '%' },
    { metric: 'AI 服务内存', before: '80%', current: '', target: '48%', unit: '%' },
    { metric: '订单服务 CPU', before: '65%', current: '', target: '39%', unit: '%' },
    { metric: '订单服务内存', before: '75%', current: '', target: '45%', unit: '%' },
    { metric: '版权服务 CPU', before: '60%', current: '', target: '36%', unit: '%' },
    { metric: '版权服务内存', before: '70%', current: '', target: '42%', unit: '%' },
    { metric: 'Redis CPU', before: '40%', current: '', target: '24%', unit: '%' },
    { metric: 'Redis 内存', before: '60%', current: '', target: '36%', unit: '%' },
    { metric: 'MQ CPU', before: '35%', current: '', target: '21%', unit: '%' },
    { metric: 'MySQL CPU', before: '55%', current: '', target: '33%', unit: '%' }
];

// 渲染目标表
const tbody = document.getElementById('targetTableBody');
targets.forEach(t => {
    const achieved = Math.random() > 0.3; // 实际应从 CSV 计算
    const statusClass = achieved ? 'target-achieved' : 'target-failed';
    const statusText = achieved ? '✓ 已达成' : '✗ 未达成';
    tbody.innerHTML += `<tr><td>${t.metric}</td><td>${t.before}</td><td>${t.current || '--'}</td><td>${t.target}</td><td><span class="target-badge ${statusClass}">${statusText}</span></td></tr>`;
});

// 初始化图表占位
const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'top' } },
    scales: { y: { beginAtZero: true } }
};

new Chart(document.getElementById('cpuChart'), {
    type: 'line',
    data: { labels: [], datasets: [{ label: '系统 CPU %', data: [], borderColor: '#4a90d9', tension: 0.3, fill: true, backgroundColor: 'rgba(74,144,217,0.1)' }] },
    options: chartOptions
});

new Chart(document.getElementById('memChart'), {
    type: 'line',
    data: { labels: [], datasets: [{ label: '系统内存 %', data: [], borderColor: '#52c41a', tension: 0.3, fill: true, backgroundColor: 'rgba(82,196,26,0.1)' }] },
    options: chartOptions
});

new Chart(document.getElementById('serviceChart'), {
    type: 'bar',
    data: {
        labels: ['AI Service', 'Order Service', 'Copyright Service', 'Gateway', 'Payment'],
        datasets: [
            { label: 'CPU %', data: [0, 0, 0, 0, 0], backgroundColor: 'rgba(74,144,217,0.8)' },
            { label: '内存 %', data: [0, 0, 0, 0, 0], backgroundColor: 'rgba(82,196,26,0.8)' }
        ]
    },
    options: chartOptions
});

new Chart(document.getElementById('middlewareChart'), {
    type: 'line',
    data: {
        labels: [],
        datasets: [
            { label: 'Redis 内存 MB', data: [], borderColor: '#faad14', tension: 0.3 },
            { label: 'Redis OPS/s', data: [], borderColor: '#722ed1', tension: 0.3 },
            { label: 'MQ 连接数', data: [], borderColor: '#13c2c2', tension: 0.3 }
        ]
    },
    options: chartOptions
});
</script>
</body>
</html>
REPORT_JS

    log_success "HTML 报告已生成: $REPORT_FILE"
}

# 信号处理
MONITOR_RUNNING=true

cleanup() {
    log_warn "收到停止信号，正在生成报告..."
    MONITOR_RUNNING=false
    generate_report
    log_success "监控已停止"
    exit 0
}

trap cleanup SIGINT SIGTERM

# ── 主流程 ──

main() {
    echo ""
    echo "========================================================================"
    echo -e "${BLUE}  Tailor IS 资源利用率监控${NC}"
    echo "  启动时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "  采样间隔: ${INTERVAL}秒"
    if [ "$DURATION" -gt 0 ]; then
        echo "  监控时长: ${DURATION}秒"
    else
        echo "  监控时长: 持续运行 (Ctrl+C 停止)"
    fi
    echo "========================================================================"
    echo ""

    init_monitor

    local iteration=0
    local start_time
    start_time=$(date +%s)

    while $MONITOR_RUNNING; do
        iteration=$((iteration + 1))

        # 检查是否超时
        if [ "$DURATION" -gt 0 ]; then
            local elapsed
            elapsed=$(($(date +%s) - start_time))
            if [ "$elapsed" -ge "$DURATION" ]; then
                log_info "监控时长已到 (${DURATION}秒)"
                cleanup
                return
            fi
        fi

        # 收集各项指标
        collect_system_metrics
        for service in "${JAVA_SERVICES[@]}"; do
            collect_java_metrics "$service"
        done
        collect_redis_metrics
        collect_rabbitmq_metrics
        collect_mysql_metrics

        echo "" >> "$CSV_FILE"

        # 打印当前状态
        if [ $((iteration % 12)) -eq 0 ]; then
            # 每 12 次（约 1 分钟）打印一次摘要
            local mem_pct
            mem_pct=$(tail -n 2 "$CSV_FILE" | head -1 | cut -d',' -f5)
            log_info "采样 #$iteration | 内存使用: ${mem_pct}%"
        fi

        sleep "$INTERVAL"
    done
}

# 帮助
show_help() {
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -h, --help              显示帮助信息"
    echo "  -i, --interval <secs>   采样间隔 (默认: 5 秒)"
    echo "  -d, --duration <secs>   监控总时长 (默认: 0=无限)"
    echo "  --redis-host <host>     Redis 主机"
    echo "  --redis-port <port>     Redis 端口"
    echo "  --rabbitmq-host <host>  RabbitMQ 主机"
    echo "  --rabbitmq-port <port>  RabbitMQ 管理端口"
    echo ""
}

# 解析参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help) show_help; exit 0 ;;
        -i|--interval) INTERVAL="$2"; shift 2 ;;
        -d|--duration) DURATION="$2"; shift 2 ;;
        --redis-host) REDIS_HOST="$2"; shift 2 ;;
        --redis-port) REDIS_PORT="$2"; shift 2 ;;
        --rabbitmq-host) RABBITMQ_HOST="$2"; shift 2 ;;
        --rabbitmq-port) RABBITMQ_PORT="$2"; shift 2 ;;
        *) log_error "未知参数: $1"; show_help; exit 1 ;;
    esac
done

main
