#!/bin/bash
# ============================================================
# 生产环境性能压测脚本 - performance-benchmark.sh
# 功能: 对 HTTP/MySQL/Redis/RabbitMQ/Nacos 进行综合性性能压测
# 版本: 1.0.0
# ============================================================
set -euo pipefail

# ---------- 颜色输出常量 ----------
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'
PURPLE='\033[0;35m'; CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

# ---------- 元数据 ----------
SCRIPT_VERSION="1.0.0"
START_TIME=$(date +%s)
HOSTNAME_VAL=$(hostname)

# ---------- 配置变量 ----------
THREADS=(10 25 50 100)
REQUESTS=100
OUTPUT_DIR="/tmp/benchmark-$(date +%Y%m%d-%H%M%S)"
SERVICES_TO_TEST=("http" "mysql" "redis" "rabbitmq" "nacos")
TEST_MODE="standard"
RESUME_FILE=""
declare -A TEST_RESULTS
declare -a COMPLETED_TESTS
declare -a CSV_ROWS
TEST_SUMMARY_FILE=""

# ---------- 目标服务配置 (环境变量覆盖) ----------
HTTP_URL="${HTTP_URL:-http://localhost:8080/actuator/health}"
HTTP_STATIC_URL="${HTTP_STATIC_URL:-http://localhost:8080/static}"
HTTP_API_URL="${HTTP_API_URL:-http://localhost:8080/api/v1}"
MYSQL_HOST="${MYSQL_HOST:-localhost}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"
MYSQL_DATABASE="${MYSQL_DATABASE:-test}"
REDIS_HOST="${REDIS_HOST:-localhost}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
RABBITMQ_HOST="${RABBITMQ_HOST:-localhost}"
RABBITMQ_MANAGEMENT_PORT="${RABBITMQ_MANAGEMENT_PORT:-15672}"
RABBITMQ_USER="${RABBITMQ_USER:-guest}"
RABBITMQ_PASSWORD="${RABBITMQ_PASSWORD:-guest}"
RABBITMQ_VHOST="${RABBITMQ_VHOST:-/}"
NACOS_HOST="${NACOS_HOST:-localhost}"
NACOS_PORT="${NACOS_PORT:-8848}"
NACOS_NAMESPACE="${NACOS_NAMESPACE:-public}"

# ============================================================
# 日志函数
# ============================================================
log_info()    { echo -e "${BLUE}[INFO]${NC}  $*"; }
log_success() { echo -e "${GREEN}[OK]${NC}    $*"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error()   { echo -e "${RED}[FAIL]${NC}  $*"; }
log_header()  {
    echo ""; echo -e "${BOLD}${CYAN}============================================================${NC}"
    echo -e "${BOLD}${CYAN}  $*${NC}"
    echo -e "${BOLD}${CYAN}============================================================${NC}"
}
log_section() { echo ""; echo -e "${PURPLE}>>> $*${NC}"; }

format_duration() {
    local s=$1
    printf "%02dh %02dm %02ds" $((s/3600)) $((s%3600/60)) $((s%60))
}
sec_to_ms() { echo "$1 * 1000" | bc -l 2>/dev/null | awk '{printf "%.2f", $0}'; }

# ============================================================
# 统计工具
# ============================================================
calc_avg() { echo "$*" | tr ' ' '\n' | awk '{s+=$1; n++} END{if(n>0) printf "%.4f", s/n; else print "0"}'; }
calc_p95() { echo "$*" | tr ' ' '\n' | sort -n | awk '{a[NR]=$1} END{n=NR; if(n==0){print 0;exit} i=int(n*95/100); if(i<1)i=1; print a[i]}'; }
calc_p99() { echo "$*" | tr ' ' '\n' | sort -n | awk '{a[NR]=$1} END{n=NR; if(n==0){print 0;exit} i=int(n*99/100); if(i<1)i=1; print a[i]}'; }

mark_completed() { COMPLETED_TESTS+=("$1"); echo "$1" >> "${RESUME_FILE}"; }
is_completed() { for t in "${COMPLETED_TESTS[@]}"; do [[ "$t" == "$1" ]] && return 0; done; return 1; }
save_result() { TEST_RESULTS["${1}|${2}"]="$3"; }
add_csv_row() { CSV_ROWS+=("$*"); }

# ============================================================
# 参数解析
# ============================================================
show_help() {
    cat <<EOF
${BOLD}生产环境性能压测脚本 v${SCRIPT_VERSION}${NC}

用法: $0 [选项]

选项:
  --quick              快速测试 (100 请求)
  --standard           标准测试 (500 请求, 默认)
  --stress             压力测试 (5000 请求)
  --duration <分钟>    指定测试时长
  --services <列表>    指定测试服务 (逗号分隔: http,mysql,redis,rabbitmq,nacos)
  --output <目录>      输出目录
  --resume <文件>      从断点恢复
  --help               显示帮助

环境变量: HTTP_URL, MYSQL_HOST/PORT/USER/PASSWORD/DATABASE,
         REDIS_HOST/PORT/PASSWORD, RABBITMQ_HOST/USER/PASSWORD/VHOST,
         NACOS_HOST/PORT

示例:
  $0 --quick --services http,mysql
  HTTP_URL=http://gateway.local:8080/health $0 --standard
EOF
}

parse_args() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --quick) TEST_MODE="quick"; REQUESTS=100; shift ;;
            --standard) TEST_MODE="standard"; REQUESTS=500; shift ;;
            --stress) TEST_MODE="stress"; REQUESTS=5000; shift ;;
            --duration) shift 2 ;;
            --services) IFS=',' read -ra SERVICES_TO_TEST <<< "$2"; shift 2 ;;
            --output) OUTPUT_DIR="$2"; shift 2 ;;
            --resume) RESUME_FILE="$2"; shift 2 ;;
            --help|-h) show_help; exit 0 ;;
            *) log_error "未知参数: $1"; show_help; exit 1 ;;
        esac
    done
}

check_tools() {
    log_header "检查运行环境"
    for t in curl bc awk python3; do
        if command -v "$t" &>/dev/null; then log_success "  ✓ $t"; else log_error "  ✗ $t"; exit 1; fi
    done
    for t in mysql mysqlslap redis-cli redis-benchmark jq iostat; do
        if command -v "$t" &>/dev/null; then log_info "  + $t 可用"; else log_warn "  - $t 未安装 (相关测试降级)"; fi
    done
}

init_output_dir() {
    log_header "初始化输出目录: ${OUTPUT_DIR}"
    mkdir -p "${OUTPUT_DIR}/raw"
    RESUME_FILE="${OUTPUT_DIR}/resume.state"
    TEST_SUMMARY_FILE="${OUTPUT_DIR}/summary.txt"
    if [[ -n "${RESUME_FILE}" && -f "${RESUME_FILE}" ]]; then
        while IFS= read -r line; do [[ -n "$line" ]] && COMPLETED_TESTS+=("$line"); done < "${RESUME_FILE}"
        log_info "已恢复 ${#COMPLETED_TESTS[@]} 个测试记录"
    else
        > "${RESUME_FILE}"
    fi
}

has_service() { local s="$1"; for t in "${SERVICES_TO_TEST[@]}"; do [[ "$t" == "$s" ]] && return 0; done; return 1; }

# ============================================================
# HTTP 服务性能测试
# ============================================================
test_http_basic_qps() {
    is_completed "http_basic_qps" && return 0
    log_section "HTTP 基础QPS测试 (${REQUESTS}次请求)"
    local all_times="" all_connect="" all_transfer="" success=0 failed=0
    for i in $(seq 1 ${REQUESTS}); do
        local r code t_total t_connect t_transfer
        r=$(curl -s -o /dev/null -w "%{http_code}|%{time_total}|%{time_connect}|%{time_starttransfer}" \
            --connect-timeout 5 --max-time 30 "${HTTP_URL}" 2>/dev/null || echo "000|0|0|0")
        code=$(echo "$r" | cut -d'|' -f1)
        t_total=$(echo "$r" | cut -d'|' -f2)
        t_connect=$(echo "$r" | cut -d'|' -f3)
        t_transfer=$(echo "$r" | cut -d'|' -f4)
        if [[ "$code" =~ ^2[0-9][0-9]$ ]]; then
            all_times="$all_times $t_total"; all_connect="$all_connect $t_connect"
            all_transfer="$all_transfer $t_transfer"; ((success++)) || true
        else
            ((failed++)) || true
        fi
        (( i % 50 == 0 )) && log_info "  已发送 ${i}/${REQUESTS}"
    done
    local wall_time=$(( $(date +%s) - START_TIME ))
    local avg_ms qps p95 p99
    avg_ms=$(sec_to_ms "$(calc_avg $all_times)")
    p95=$(sec_to_ms "$(calc_p95 $all_times)")
    p99=$(sec_to_ms "$(calc_p99 $all_times)")
    qps=$(awk -v s="$success" -v t="$wall_time" 'BEGIN{if(t>0) printf "%.2f", s/t; else print s}')
    log_success "  成功=${success} 失败=${failed} | 平均=${avg_ms}ms P95=${p95}ms P99=${p99}ms | QPS=${qps}"
    save_result "http_basic" "qps" "$qps"; save_result "http_basic" "avg_ms" "$avg_ms"
    save_result "http_basic" "p95_ms" "$p95"; save_result "http_basic" "p99_ms" "$p99"
    add_csv_row "http,basic_qps,${qps},${avg_ms},${p95},${p99},${REQUESTS},success=${success}"
    mark_completed "http_basic_qps"
}

test_http_concurrency() {
    is_completed "http_concurrency" && return 0
    log_section "HTTP 并发连接测试"
    for concur in "${THREADS[@]}"; do
        log_info "  并发=${concur}"
        local tmpdir="${OUTPUT_DIR}/raw/http-concur-${concur}" jobs=()
        mkdir -p "$tmpdir"
        local t_start=$(date +%s)
        for i in $(seq 1 ${REQUESTS}); do
            (curl -s -o /dev/null -w "%{time_total}" --connect-timeout 10 --max-time 60 "${HTTP_URL}" 2>/dev/null || echo "0") > "${tmpdir}/req-${i}.txt" &
            jobs+=($!)
            if (( ${#jobs[@]} >= concur )); then
                wait -n 2>/dev/null || true
                jobs=($(for j in "${jobs[@]}"; do kill -0 "$j" 2>/dev/null && echo "$j"; done || true))
            fi
        done
        wait
        local dur=$(( $(date +%s) - t_start ))
        local times=$(cat "${tmpdir}"/req-*.txt 2>/dev/null | tr '\n' ' ')
        local avg qps p95 p99
        avg=$(sec_to_ms "$(calc_avg $times)"); p95=$(sec_to_ms "$(calc_p95 $times)"); p99=$(sec_to_ms "$(calc_p99 $times)")
        qps=$(awk -v r="$REQUESTS" -v d="$dur" 'BEGIN{if(d>0) printf "%.2f", r/d; else print r}')
        log_success "    QPS=${qps} 平均=${avg}ms P95=${p95}ms P99=${p99}ms"
        save_result "http_concur_${concur}" "qps" "$qps"
        save_result "http_concur_${concur}" "avg_ms" "$avg"
        add_csv_row "http,concur_${concur},${qps},${avg},${p95},${p99},${REQUESTS},duration=${dur}s"
    done
    mark_completed "http_concurrency"
}

test_http_static_and_api() {
    is_completed "http_static_api" && return 0
    log_section "HTTP 静态资源与API测试"
    local endpoints=("${HTTP_STATIC_URL}/index.html:static" "${HTTP_API_URL}/health:api")
    for ep in "${endpoints[@]}"; do
        local url="${ep%%:*}" name="${ep##*:}"
        log_info "  测试: ${url}"
        local all_times=""
        for i in $(seq 1 30); do
            local t
            t=$(curl -s -o /dev/null -w "%{time_total}" --connect-timeout 5 "$url" 2>/dev/null || echo "0")
            [[ "$t" != "0" ]] && all_times="$all_times $t"
        done
        local avg p95
        avg=$(sec_to_ms "$(calc_avg $all_times)")
        p95=$(sec_to_ms "$(calc_p95 $all_times)")
        log_success "    ${name}: 平均=${avg}ms P95=${p95}ms"
        save_result "http_${name}" "avg_ms" "$avg"
        add_csv_row "http,${name},,${avg},${p95},,30,"
    done
    mark_completed "http_static_api"
}

run_http_tests() {
    has_service "http" || return 0
    log_header "开始 HTTP 服务性能测试"
    test_http_basic_qps; test_http_concurrency; test_http_static_and_api
    log_success "HTTP 测试完成"
}

# ============================================================
# MySQL 数据库性能测试
# ============================================================
build_mysql_cmd() {
    local pass=""
    [[ -n "${MYSQL_PASSWORD}" ]] && pass="-p${MYSQL_PASSWORD}"
    echo "mysql -h ${MYSQL_HOST} -P ${MYSQL_PORT} -u ${MYSQL_USER} ${pass} -D ${MYSQL_DATABASE} -N -B --connect_timeout=5"
}

test_mysql_basic() {
    is_completed "mysql_basic" && return 0
    log_section "MySQL 基础查询测试"
    local all_times=""
    for i in $(seq 1 ${REQUESTS}); do
        local ts te
        ts=$(date +%s%N)
        $(build_mysql_cmd) -e "SELECT 1;" &>/dev/null || true
        te=$(date +%s%N)
        all_times="$all_times $(( (te - ts) / 1000000 ))"
        (( i % 50 == 0 )) && log_info "  已测试 ${i}/${REQUESTS}"
    done
    local avg p95 p99
    avg=$(calc_avg $all_times); p95=$(calc_p95 $all_times); p99=$(calc_p99 $all_times)
    log_success "  平均=${avg}ms P95=${p95}ms P99=${p99}ms"
    save_result "mysql_basic" "avg_ms" "$avg"
    add_csv_row "mysql,basic,,${avg},${p95},${p99},${REQUESTS},"
    mark_completed "mysql_basic"
}

test_mysql_complex() {
    is_completed "mysql_complex" && return 0
    log_section "MySQL 复杂查询测试"
    local queries=("SELECT COUNT(*) FROM information_schema.tables" \
        "SELECT table_schema, COUNT(*) FROM information_schema.tables GROUP BY table_schema")
    local idx=0
    for q in "${queries[@]}"; do
        ((idx++))
        local all_times=""
        for i in $(seq 1 20); do
            local ts te
            ts=$(date +%s%N)
            $(build_mysql_cmd) -e "${q};" &>/dev/null || true
            te=$(date +%s%N)
            all_times="$all_times $(( (te - ts) / 1000000 ))"
        done
        local avg=$(calc_avg $all_times)
        log_success "  查询${idx}: 平均=${avg}ms"
        save_result "mysql_complex_${idx}" "avg_ms" "$avg"
        add_csv_row "mysql,complex_${idx},,${avg},,,,,"
    done
    mark_completed "mysql_complex"
}

test_mysql_concurrency() {
    is_completed "mysql_concur" && return 0
    log_section "MySQL 并发连接测试 (mysqlslap)"
    if command -v mysqlslap &>/dev/null; then
        local pass=""
        [[ -n "${MYSQL_PASSWORD}" ]] && pass="--password=${MYSQL_PASSWORD}"
        for concur in 10 25 50; do
            local out="${OUTPUT_DIR}/raw/mysqlslap-${concur}.txt"
            mysqlslap --host="${MYSQL_HOST}" --port="${MYSQL_PORT}" --user="${MYSQL_USER}" ${pass} \
                --concurrency="${concur}" --iterations=3 --auto-generate-sql \
                --number-of-queries="${REQUESTS}" 2>&1 | tee -a "$out" || true
            local avg_qps=$(grep -oP 'Average number of queries per second: \K[0-9.]+' "$out" 2>/dev/null || echo "0")
            local avg_lat=$(grep -oP 'Average query time: \K[0-9.]+' "$out" 2>/dev/null || echo "0")
            log_success "  并发=${concur}: QPS=${avg_qps} 平均=${avg_lat}s"
            save_result "mysql_concur_${concur}" "qps" "$avg_qps"
            add_csv_row "mysql,concur_${concur},${avg_qps},${avg_lat},,,,,"
        done
    else
        log_warn "  mysqlslap 未安装, 跳过"
    fi
    mark_completed "mysql_concur"
}

test_mysql_large() {
    is_completed "mysql_large" && return 0
    log_section "MySQL 大数据集测试"
    for rows in 1000 10000; do
        local all_times=""
        for i in $(seq 1 5); do
            local ts te
            ts=$(date +%s%N)
            $(build_mysql_cmd) -e "SELECT * FROM information_schema.columns LIMIT ${rows};" >/dev/null 2>&1 || true
            te=$(date +%s%N)
            all_times="$all_times $(( (te - ts) / 1000000 ))"
        done
        local avg=$(calc_avg $all_times)
        log_success "  ${rows} 行平均=${avg}ms"
        add_csv_row "mysql,large_${rows},,${avg},,,,,"
    done
    mark_completed "mysql_large"
}

run_mysql_tests() {
    has_service "mysql" || return 0
    log_header "开始 MySQL 数据库性能测试"
    test_mysql_basic; test_mysql_complex; test_mysql_concurrency; test_mysql_large
    log_success "MySQL 测试完成"
}

# ============================================================
# Redis 缓存性能测试
# ============================================================
build_redis_cmd() {
    local pass=""
    [[ -n "${REDIS_PASSWORD}" ]] && pass="-a ${REDIS_PASSWORD}"
    echo "redis-cli -h ${REDIS_HOST} -p ${REDIS_PORT} ${pass} --no-auth-warning"
}

test_redis_basic() {
    is_completed "redis_basic" && return 0
    log_section "Redis PING/SET/GET 测试"
    local all_ping="" all_set="" all_get=""
    for i in $(seq 1 ${REQUESTS}); do
        local ts te resp
        ts=$(date +%s%N)
        resp=$($(build_redis_cmd) PING 2>/dev/null || echo "ERR")
        te=$(date +%s%N)
        [[ "$resp" == "PONG" ]] && all_ping="$all_ping $(( (te - ts) / 1000000 ))"
        ts=$(date +%s%N)
        $(build_redis_cmd) SET "bench:key:${i}" "val_${i}" &>/dev/null || true
        te=$(date +%s%N)
        all_set="$all_set $(( (te - ts) / 1000000 ))"
        ts=$(date +%s%N)
        $(build_redis_cmd) GET "bench:key:${i}" &>/dev/null || true
        te=$(date +%s%N)
        all_get="$all_get $(( (te - ts) / 1000000 ))"
        (( i % 100 == 0 )) && log_info "  已测试 ${i}/${REQUESTS}"
    done
    local ping_avg set_avg get_avg
    ping_avg=$(calc_avg $all_ping); set_avg=$(calc_avg $all_set); get_avg=$(calc_avg $all_get)
    log_success "  PING=${ping_avg}ms SET=${set_avg}ms GET=${get_avg}ms"
    save_result "redis_ping" "avg_ms" "$ping_avg"
    save_result "redis_set" "avg_ms" "$set_avg"
    save_result "redis_get" "avg_ms" "$get_avg"
    add_csv_row "redis,ping,,${ping_avg},,,,${REQUESTS},"
    add_csv_row "redis,set,,${set_avg},,,,${REQUESTS},"
    add_csv_row "redis,get,,${get_avg},,,,${REQUESTS},"
    mark_completed "redis_basic"
}

test_redis_hash_list() {
    is_completed "redis_hash_list" && return 0
    log_section "Redis HASH/LIST 操作测试"
    local all_hset="" all_lpush="" all_lpop=""
    for i in $(seq 1 500); do
        local ts te
        ts=$(date +%s%N); $(build_redis_cmd) HSET "bench:hash:${i}" f1 v1 &>/dev/null || true
        te=$(date +%s%N); all_hset="$all_hset $(( (te - ts) / 1000000 ))"
        ts=$(date +%s%N); $(build_redis_cmd) LPUSH "bench:list" "${i}" &>/dev/null || true
        te=$(date +%s%N); all_lpush="$all_lpush $(( (te - ts) / 1000000 ))"
    done
    for i in $(seq 1 500); do
        local ts te
        ts=$(date +%s%N); $(build_redis_cmd) LPOP "bench:list" &>/dev/null || true
        te=$(date +%s%N); all_lpop="$all_lpop $(( (te - ts) / 1000000 ))"
    done
    local havg lavg lpavg
    havg=$(calc_avg $all_hset); lavg=$(calc_avg $all_lpush); lpavg=$(calc_avg $all_lpop)
    log_success "  HSET=${havg}ms LPUSH=${lavg}ms LPOP=${lpavg}ms"
    save_result "redis_hset" "avg_ms" "$havg"
    save_result "redis_lpush" "avg_ms" "$lavg"
    save_result "redis_lpop" "avg_ms" "$lpavg"
    add_csv_row "redis,hset,,${havg},,,,,"
    add_csv_row "redis,lpush,,${lavg},,,,,"
    add_csv_row "redis,lpop,,${lpavg},,,,,"
    mark_completed "redis_hash_list"
}

test_redis_benchmark() {
    is_completed "redis_bench" && return 0
    log_section "Redis redis-benchmark 标准测试"
    if command -v redis-benchmark &>/dev/null; then
        local pass=""
        [[ -n "${REDIS_PASSWORD}" ]] && pass="-a ${REDIS_PASSWORD}"
        local out="${OUTPUT_DIR}/raw/redis-bench.txt"
        redis-benchmark -h "${REDIS_HOST}" -p "${REDIS_PORT}" ${pass} \
            -n "${REQUESTS}" -c 50 -t set,get,incr 2>&1 | tee -a "$out" || true
        for cmd in SET GET INCR; do
            local qps
            qps=$(grep -i "${cmd}" "$out" | grep -oP '\K[0-9.]+(?= requests per second)' | head -1 || echo "0")
            [[ -n "$qps" && "$qps" != "0" ]] && log_success "  ${cmd}: ${qps} ops/sec"
            save_result "redis_bench_${cmd,,}" "qps" "$qps"
            add_csv_row "redis,bench_${cmd,,},${qps},,,,,"
        done
    else
        log_warn "  redis-benchmark 未安装, 跳过"
    fi
    mark_completed "redis_bench"
}

run_redis_tests() {
    has_service "redis" || return 0
    log_header "开始 Redis 缓存性能测试"
    test_redis_basic; test_redis_hash_list; test_redis_benchmark
    log_success "Redis 测试完成"
}

# ============================================================
# RabbitMQ 消息队列性能测试
# ============================================================
test_rabbitmq() {
    is_completed "rabbitmq_all" && return 0
    log_section "RabbitMQ 消息队列测试"
    local mgmt_url="http://${RABBITMQ_HOST}:${RABBITMQ_MANAGEMENT_PORT}"
    if ! curl -s -u "${RABBITMQ_USER}:${RABBITMQ_PASSWORD}" "${mgmt_url}/api/overview" &>/dev/null; then
        log_warn "  RabbitMQ Management API 不可访问, 跳过"
        mark_completed "rabbitmq_all"
        return 0
    fi
    local pub_count=1000
    [[ "$TEST_MODE" == "stress" ]] && pub_count=3000
    # 发布测试
    local all_pub="" t_start=$(date +%s)
    for i in $(seq 1 ${pub_count}); do
        local ts te
        ts=$(date +%s%N)
        curl -s -u "${RABBITMQ_USER}:${RABBITMQ_PASSWORD}" -X POST \
            -H "content-type:application/json" \
            -d "{\"properties\":{},\"routing_key\":\"bench\",\"payload\":\"msg_${i}\",\"payload_encoding\":\"string\"}" \
            "${mgmt_url}/api/exchanges/${RABBITMQ_VHOST}/amq.default/publish" &>/dev/null || true
        te=$(date +%s%N)
        all_pub="$all_pub $(( (te - ts) / 1000000 ))"
        (( i % 200 == 0 )) && log_info "  已发布 ${i}/${pub_count}"
    done
    local dur=$(( $(date +%s) - t_start ))
    local pub_avg qps
    pub_avg=$(calc_avg $all_pub)
    qps=$(awk -v c="$pub_count" -v d="$dur" 'BEGIN{if(d>0) printf "%.2f", c/d; else print c}')
    log_success "  发布: ${pub_count}条 平均=${pub_avg}ms QPS=${qps}"
    save_result "rabbitmq_publish" "avg_ms" "$pub_avg"
    save_result "rabbitmq_publish" "qps" "$qps"
    add_csv_row "rabbitmq,publish,${qps},${pub_avg},,,,${pub_count}"
    # 消费测试
    local all_con="" t_start2=$(date +%s)
    for i in $(seq 1 200); do
        local ts te
        ts=$(date +%s%N)
        curl -s -u "${RABBITMQ_USER}:${RABBITMQ_PASSWORD}" -X POST \
            -H "content-type:application/json" \
            -d '{"count":1,"requeue":true,"encoding":"auto"}' \
            "${mgmt_url}/api/queues/${RABBITMQ_VHOST}/bench/get" &>/dev/null || true
        te=$(date +%s%N)
        all_con="$all_con $(( (te - ts) / 1000000 ))"
    done
    local con_avg=$(calc_avg $all_con)
    log_success "  消费: 平均=${con_avg}ms"
    save_result "rabbitmq_consume" "avg_ms" "$con_avg"
    add_csv_row "rabbitmq,consume,,${con_avg},,,,200"
    mark_completed "rabbitmq_all"
}

run_rabbitmq_tests() {
    has_service "rabbitmq" || return 0
    log_header "开始 RabbitMQ 消息队列性能测试"
    test_rabbitmq
    log_success "RabbitMQ 测试完成"
}

# ============================================================
# Nacos 配置中心性能测试
# ============================================================
test_nacos() {
    is_completed "nacos_all" && return 0
    log_section "Nacos 配置与服务发现测试"
    local base="http://${NACOS_HOST}:${NACOS_PORT}/nacos/v1"
    local count=100
    [[ "$TEST_MODE" == "stress" ]] && count=500
    # 配置发布
    local all_pub="" success=0
    for i in $(seq 1 ${count}); do
        local ts te resp
        ts=$(date +%s%N)
        resp=$(curl -s -X POST "${base}/cs/configs" \
            -d "tenant=${NACOS_NAMESPACE}&dataId=bench-${i}&group=DEFAULT_GROUP&content=v_${i}" 2>/dev/null || echo "fail")
        te=$(date +%s%N)
        all_pub="$all_pub $(( (te - ts) / 1000000 ))"
        [[ "$resp" == "true" ]] && ((success++)) || true
        (( i % 20 == 0 )) && log_info "  配置发布 ${i}/${count}"
    done
    local pub_avg=$(calc_avg $all_pub)
    log_success "  配置发布: 成功=${success}/${count} 平均=${pub_avg}ms"
    save_result "nacos_publish" "avg_ms" "$pub_avg"
    add_csv_row "nacos,publish,,${pub_avg},,,,${success}"
    # 配置拉取
    local all_pull=""
    for i in $(seq 1 ${count}); do
        local ts te
        ts=$(date +%s%N)
        curl -s -G "${base}/cs/configs" \
            -d "tenant=${NACOS_NAMESPACE}&dataId=bench-${i}&group=DEFAULT_GROUP" &>/dev/null || true
        te=$(date +%s%N)
        all_pull="$all_pull $(( (te - ts) / 1000000 ))"
    done
    local pull_avg=$(calc_avg $all_pull)
    log_success "  配置拉取: 平均=${pull_avg}ms"
    save_result "nacos_pull" "avg_ms" "$pull_avg"
    add_csv_row "nacos,pull,,${pull_avg},,,,${count}"
    # 服务注册与发现
    local all_reg="" all_disc=""
    for i in $(seq 1 100); do
        local ts te
        ts=$(date +%s%N)
        curl -s -X POST "${base}/ns/instance" \
            -d "serviceName=bench-svc-${i}&ip=127.0.0.1&port=$((8000+i))&namespaceId=${NACOS_NAMESPACE}" &>/dev/null || true
        te=$(date +%s%N)
        all_reg="$all_reg $(( (te - ts) / 1000000 ))"
    done
    for i in $(seq 1 100); do
        local ts te
        ts=$(date +%s%N)
        curl -s -G "${base}/ns/instance/list" \
            -d "serviceName=bench-svc-${i}&namespaceId=${NACOS_NAMESPACE}" &>/dev/null || true
        te=$(date +%s%N)
        all_disc="$all_disc $(( (te - ts) / 1000000 ))"
    done
    local reg_avg=$(calc_avg $all_reg) disc_avg=$(calc_avg $all_disc)
    log_success "  服务注册: 平均=${reg_avg}ms | 服务发现: 平均=${disc_avg}ms"
    save_result "nacos_register" "avg_ms" "$reg_avg"
    save_result "nacos_discover" "avg_ms" "$disc_avg"
    add_csv_row "nacos,register,,${reg_avg},,,,100"
    add_csv_row "nacos,discover,,${disc_avg},,,,100"
    mark_completed "nacos_all"
}

run_nacos_tests() {
    has_service "nacos" || return 0
    log_header "开始 Nacos 配置中心性能测试"
    test_nacos
    log_success "Nacos 测试完成"
}

# ============================================================
# 资源监控测试
# ============================================================
MONITOR_PID=""

start_resource_monitor() {
    log_section "启动资源监控 (采样: 2s)"
    (
        while true; do
            local cpu_line mem_line ts=$(date +%s)
            cpu_line=$(head -n1 /proc/stat 2>/dev/null || echo "cpu 0 0 0 0 0 0 0")
            local idle_total used_total
            idle_total=$(echo "$cpu_line" | awk '{print $5+$6}')
            used_total=$(echo "$cpu_line" | awk '{print $2+$3+$4+$7+$8+$9}')
            mem_line=$(free 2>/dev/null | awk '/^Mem:/ {print $3, $2}')
            local mem_used mem_total
            read mem_used mem_total <<< "$mem_line"
            [[ -z "$mem_used" ]] && mem_used=0 && mem_total=1
            echo "${ts} ${used_total} ${idle_total} ${mem_used} ${mem_total}" >> "${OUTPUT_DIR}/raw/monitor.log"
            sleep 2
        done
    ) &
    MONITOR_PID=$!
    log_info "  监控 PID=${MONITOR_PID}"
}

stop_resource_monitor() {
    log_section "停止资源监控并汇总"
    [[ -n "${MONITOR_PID}" ]] && kill "${MONITOR_PID}" 2>/dev/null && wait "${MONITOR_PID}" 2>/dev/null || true
    local logf="${OUTPUT_DIR}/raw/monitor.log"
    if [[ -f "$logf" && $(wc -l < "$logf") -gt 1 ]]; then
        local cpu_util_samples="" mem_util_samples=""
        local first_u first_i last_u last_i
        # 读取第一行和最后一行的 CPU 累计值计算利用率
        read _ first_u first_i _ _ < <(head -n1 "$logf")
        read _ last_u last_i _ _ < <(tail -n1 "$logf")
        local u_diff=$(( last_u - first_u )) i_diff=$(( last_i - first_i )) total_diff=$(( u_diff + i_diff ))
        local cpu_avg
        if [[ $total_diff -gt 0 ]]; then
            cpu_avg=$(awk -v u="$u_diff" -v t="$total_diff" 'BEGIN{printf "%.2f", u*100/t}')
        else
            cpu_avg=0
        fi
        while read _ _ _ mem_u mem_t; do
            [[ -n "$mem_t" && "$mem_t" -gt 0 ]] && mem_util_samples="$mem_util_samples $(awk -v u="$mem_u" -v t="$mem_t" 'BEGIN{printf "%.2f", u*100/t}')"
        done < "$logf"
        local mem_avg mem_peak
        mem_avg=$(calc_avg $mem_util_samples)
        mem_peak=$(echo "$mem_util_samples" | tr ' ' '\n' | sort -n | tail -n1)
        log_success "  CPU: 平均=${cpu_avg}% | 内存: 平均=${mem_avg}% 峰值=${mem_peak}%"
        save_result "monitor_cpu" "avg_pct" "$cpu_avg"
        save_result "monitor_mem" "avg_pct" "$mem_avg"
        save_result "monitor_mem" "peak_pct" "$mem_peak"
        add_csv_row "system,cpu,,,,,${cpu_avg}%,avg"
        add_csv_row "system,memory,,,,,${mem_avg}%,peak=${mem_peak}%"
    fi
    # 磁盘 IO
    if command -v iostat &>/dev/null; then
        local io_out="${OUTPUT_DIR}/raw/disk-io.txt"
        iostat -d 1 2 2>/dev/null > "$io_out" || true
        local rkb wkb
        rkb=$(awk '/^[sv]d[a-z]/ {sum+=$4; n++} END{if(n>0) printf "%.2f", sum/n; else print "0"}' "$io_out")
        wkb=$(awk '/^[sv]d[a-z]/ {sum+=$5; n++} END{if(n>0) printf "%.2f", sum/n; else print "0"}' "$io_out")
        log_info "  磁盘: 读=${rkb}KB/s 写=${wkb}KB/s"
        save_result "monitor_disk" "read_kb_s" "$rkb"
        save_result "monitor_disk" "write_kb_s" "$wkb"
        add_csv_row "system,disk,,,,,${rkb}KB/s,${wkb}KB/s"
    fi
    mark_completed "resource_monitor"
}

# ============================================================
# 报告生成: CSV + JSON + 文本 + HTML
# ============================================================
generate_csv_report() {
    log_section "生成 CSV 报告"
    local csv="${OUTPUT_DIR}/benchmark-results.csv"
    {
        echo "service,test_name,qps,avg_ms,p95_ms,p99_ms,requests,extra"
        for row in "${CSV_ROWS[@]}"; do echo "$row"; done
    } > "$csv"
    log_success "  ${csv} ($(wc -l < "$csv") 行)"
}

generate_json_report() {
    log_section "生成 JSON 报告"
    local json="${OUTPUT_DIR}/benchmark-results.json"
    python3 - "$json" "${OUTPUT_DIR}" <<'PYEOF'
import json, sys, os, glob
from datetime import datetime
json_file, od = sys.argv[1], sys.argv[2]
data = {
    "metadata": {
        "version": "1.0.0",
        "generated_at": datetime.utcnow().isoformat() + "Z",
        "hostname": os.uname()[1],
        "output_dir": od,
    },
    "tests": [],
    "summary": {},
}
csv_path = os.path.join(od, "benchmark-results.csv")
if os.path.exists(csv_path):
    with open(csv_path) as f:
        lines = f.readlines()
    header = lines[0].strip().split(",")
    for line in lines[1:]:
        parts = line.strip().split(",")
        if len(parts) >= len(header):
            data["tests"].append(dict(zip(header, parts)))
by_svc = {}
for t in data["tests"]:
    s = t.get("service", "")
    by_svc.setdefault(s, []).append(t)
data["by_service"] = by_svc
with open(json_file, "w") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)
print(f"  {json_file} ({os.path.getsize(json_file)} bytes)")
PYEOF
    log_success "  JSON 报告完成"
}

generate_text_report() {
    log_section "生成文本报告"
    local txt="${OUTPUT_DIR}/summary.txt"
    {
        echo "============================================================"
        echo "  生产环境性能压测报告"
        echo "============================================================"
        echo ""
        echo "生成时间: $(date '+%Y-%m-%d %H:%M:%S')"
        echo "主机名: ${HOSTNAME_VAL}"
        echo "脚本版本: ${SCRIPT_VERSION}"
        echo "测试模式: ${TEST_MODE}"
        echo "请求数: ${REQUESTS}"
        echo "测试服务: ${SERVICES_TO_TEST[*]}"
        echo "总耗时: $(format_duration $(( $(date +%s) - START_TIME )))"
        echo ""
        echo "------------------------------------------------------------"
        echo "一、关键指标汇总"
        echo "------------------------------------------------------------"
        printf "%-12s %-20s %-12s %-12s %-12s %-12s\n" "服务" "测试项" "QPS" "平均(ms)" "P95(ms)" "P99(ms)"
        printf "%-12s %-20s %-12s %-12s %-12s %-12s\n" "-----" "-----" "---" "-------" "-------" "-------"
        for row in "${CSV_ROWS[@]}"; do
            IFS=',' read -ra cols <<< "$row"
            printf "%-12s %-20s %-12s %-12s %-12s %-12s\n" \
                "${cols[0]:-}" "${cols[1]:-}" "${cols[2]:-}" "${cols[3]:-}" "${cols[4]:-}" "${cols[5]:-}"
        done
        echo ""
        echo "------------------------------------------------------------"
        echo "二、性能瓶颈与优化建议"
        echo "------------------------------------------------------------"
        echo ""
        for row in "${CSV_ROWS[@]}"; do
            IFS=',' read -ra cols <<< "$row"
            local svc="${cols[0]:-}" test="${cols[1]:-}" qps="${cols[2]:-}" avg="${cols[3]:-}" p95="${cols[4]:-}" p99="${cols[5]:-}"
            local num_avg=$(echo "$avg" | grep -oP '[0-9.]+' | head -1 || echo 0)
            local num_p99=$(echo "$p99" | grep -oP '[0-9.]+' | head -1 || echo 0)
            if awk -v a="$num_avg" -v p="$num_p99" 'BEGIN{ exit !(a+0>500 || p+0>1000) }'; then
                echo "  ⚠ ${svc}/${test}: 延迟偏高 (avg=${avg}ms, p99=${p99}ms)"
                echo "     建议: 检查慢查询/索引, 增加缓存层, 横向扩容实例"
            fi
        done
        echo ""
        echo "------------------------------------------------------------"
        echo "三、系统资源"
        echo "------------------------------------------------------------"
        echo "  CPU 平均: ${TEST_RESULTS["monitor_cpu|avg_pct"]:-N/A}%"
        echo "  内存平均: ${TEST_RESULTS["monitor_mem|avg_pct"]:-N/A}% 峰值: ${TEST_RESULTS["monitor_mem|peak_pct"]:-N/A}%"
        echo "  磁盘读: ${TEST_RESULTS["monitor_disk|read_kb_s"]:-N/A} KB/s"
        echo "  磁盘写: ${TEST_RESULTS["monitor_disk|write_kb_s"]:-N/A} KB/s"
        echo ""
        echo "详细数据: ${OUTPUT_DIR}/benchmark-results.csv / .json"
    } > "$txt"
    log_success "  ${txt}"
}

generate_html_report() {
    log_section "生成 HTML 报告"
    local html="${OUTPUT_DIR}/benchmark-report.html"
    # 通过 CSV 生成表格行
    local table_rows=""
    for row in "${CSV_ROWS[@]}"; do
        IFS=',' read -ra cols <<< "$row"
        table_rows="${table_rows}<tr><td>${cols[0]:-}</td><td>${cols[1]:-}</td><td>${cols[2]:-}</td><td>${cols[3]:-}</td><td>${cols[4]:-}</td><td>${cols[5]:-}</td><td>${cols[6]:-}</td><td>${cols[7]:-}</td></tr>"
    done
    # 柱状图数据 (SVG)
    local bars="" x=0 max_val=1
    for row in "${CSV_ROWS[@]}"; do
        IFS=',' read -ra cols <<< "$row"
        local v=$(echo "${cols[3]:-0}" | grep -oP '[0-9.]+' | head -1 || echo 0)
        if awk -v a="$v" -v b="$max_val" 'BEGIN{ exit !(a+0>b+0) }'; then max_val="$v"; fi
    done
    [[ "$max_val" == "0" || -z "$max_val" ]] && max_val=1
    local idx=0
    for row in "${CSV_ROWS[@]}"; do
        IFS=',' read -ra cols <<< "$row"
        local v=$(echo "${cols[3]:-0}" | grep -oP '[0-9.]+' | head -1 || echo 0)
        local h
        h=$(awk -v v="$v" -v m="$max_val" 'BEGIN{ if(m+0>0) printf "%d", v*140/m; else print 0 }')
        local color="#4A90E2"
        awk -v a="$v" 'BEGIN{ exit !(a+0>500) }' && color="#E74C3C"
        bars="${bars}<rect x='${x}' y='$((150-h))' width='18' height='${h}' fill='${color}'><title>${cols[1]:-} ${v}ms</title></rect>"
        x=$((x + 22))
        ((idx++))
        [[ $idx -ge 30 ]] && break
    done
    local svg_w=$(( x + 10 ))

    cat > "$html" <<HTMLEOF
<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="UTF-8"><title>生产环境性能压测报告</title>
<style>
body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI','PingFang SC','Microsoft YaHei',sans-serif;margin:30px;background:#f5f7fa;color:#2c3e50}
h1{color:#2c3e50;border-bottom:3px solid #3498db;padding-bottom:10px}
h2{color:#2980b9;margin-top:30px;border-left:4px solid #3498db;padding-left:12px}
.meta{background:#fff;padding:16px 24px;border-radius:8px;box-shadow:0 2px 4px rgba(0,0,0,.08);margin-bottom:20px}
.meta p{margin:6px 0}
table{border-collapse:collapse;width:100%;background:#fff;box-shadow:0 2px 4px rgba(0,0,0,.08);border-radius:8px;overflow:hidden}
th{background:#3498db;color:#fff;padding:12px;text-align:left}
td{padding:10px 12px;border-bottom:1px solid #ecf0f1}
tr:nth-child(even){background:#fafbfc}
tr:hover{background:#eaf4fc}
.chart{background:#fff;padding:20px;border-radius:8px;box-shadow:0 2px 4px rgba(0,0,0,.08);margin-top:20px;overflow-x:auto}
svg{background:#fafbfc;border:1px solid #ecf0f1;border-radius:4px}
.tips{background:#fff8e1;border-left:4px solid #f39c12;padding:16px;border-radius:4px;margin-top:20px}
.tips h3{color:#d35400;margin-top:0}
HTMLEOF
    echo "</style></head><body>" >> "$html"
    echo "<h1>🚀 生产环境性能压测报告</h1>" >> "$html"
    echo "<div class='meta'><p><strong>生成时间:</strong> $(date '+%Y-%m-%d %H:%M:%S')</p><p><strong>主机:</strong> ${HOSTNAME_VAL} &nbsp;|&nbsp; <strong>模式:</strong> ${TEST_MODE} &nbsp;|&nbsp; <strong>请求数:</strong> ${REQUESTS}</p><p><strong>测试服务:</strong> ${SERVICES_TO_TEST[*]}</p><p><strong>总耗时:</strong> $(format_duration $(( $(date +%s) - START_TIME )))</p></div>" >> "$html"
    echo "<h2>📊 性能指标汇总</h2>" >> "$html"
    echo "<table><thead><tr><th>服务</th><th>测试项</th><th>QPS</th><th>平均(ms)</th><th>P95(ms)</th><th>P99(ms)</th><th>请求数</th><th>备注</th></tr></thead><tbody>${table_rows}</tbody></table>" >> "$html"
    echo "<h2>📈 平均延迟分布 (前30项)</h2><div class='chart'><svg width='${svg_w}' height='180'><line x1='0' y1='150' x2='${svg_w}' y2='150' stroke='#bdc3c7' stroke-width='1'/>${bars}</svg></div>" >> "$html"
    echo "<div class='tips'><h3>💡 性能优化建议</h3><ul>" >> "$html"
    for row in "${CSV_ROWS[@]}"; do
        IFS=',' read -ra cols <<< "$row"
        local num_avg
        num_avg=$(echo "${cols[3]:-0}" | grep -oP '[0-9.]+' | head -1 || echo 0)
        if awk -v a="$num_avg" 'BEGIN{ exit !(a+0>500) }'; then
            echo "<li><strong>${cols[0]:-}/${cols[1]:-}</strong>: 延迟 ${num_avg}ms 偏高 → 建议优化慢路径/增加缓存/横向扩容</li>" >> "$html"
        fi
    done
    echo "<li>CPU/内存/磁盘 IO 指标请参考 summary.txt 中的系统资源部分</li>" >> "$html"
    echo "</ul></div></body></html>" >> "$html"
    log_success "  ${html}"
}

generate_all_reports() {
    generate_csv_report
    generate_json_report
    generate_text_report
    generate_html_report
}

# ============================================================
# 主入口
# ============================================================
main() {
    parse_args "$@"
    log_header "生产环境性能压测脚本 v${SCRIPT_VERSION}"
    log_info "输出目录: ${OUTPUT_DIR}"
    log_info "测试服务: ${SERVICES_TO_TEST[*]}"
    log_info "测试模式: ${TEST_MODE} (请求数=${REQUESTS})"
    check_tools
    init_output_dir

    start_resource_monitor

    # 性能测试主流程
    run_http_tests
    run_mysql_tests
    run_redis_tests
    run_rabbitmq_tests
    run_nacos_tests

    stop_resource_monitor

    # 生成报告
    generate_all_reports

    log_header "测试完成 🎉"
    echo ""
    echo -e "${GREEN}  所有报告已生成: ${OUTPUT_DIR}${NC}"
    echo -e "${GREEN}    ├── benchmark-results.csv${NC}"
    echo -e "${GREEN}    ├── benchmark-results.json${NC}"
    echo -e "${GREEN}    ├── benchmark-report.html${NC}"
    echo -e "${GREEN}    └── summary.txt${NC}"
    echo ""
    log_success "总耗时: $(format_duration $(( $(date +%s) - START_TIME )))"
}

# 信号处理 - 优雅退出
cleanup() {
    log_warn "收到中断信号, 正在清理..."
    [[ -n "${MONITOR_PID}" ]] && kill "${MONITOR_PID}" 2>/dev/null || true
    log_warn "已中断。可使用 --resume ${OUTPUT_DIR}/resume.state 恢复断点"
    exit 130
}
trap cleanup SIGINT SIGTERM

main "$@"
