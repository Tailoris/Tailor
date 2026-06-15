#!/bin/bash
# ==============================================================================
# Nacos 功能测试脚本
# ==============================================================================
# 版本: v1.0
# 用途: 对 Nacos 服务进行全面功能测试（健康检查、配置管理、服务注册发现、
#       命名空间管理、API认证、性能测试）
# 作者: Tailor IS Platform
# 生成日期: 2026-06-11
# 使用方法:
#   ./nacos-function-test.sh [选项]
#   常用选项:
#     --host <host>          指定 Nacos 主机地址 (默认: localhost)
#     --port <port>          指定 Nacos 端口 (默认: 8848)
#     --username <user>      登录用户名 (默认: nacos)
#     --password <pwd>       登录密码 (默认: nacos)
#     --namespace <ns>       指定测试命名空间 (默认: public)
#     --timeout <sec>        单次请求超时时间秒 (默认: 10)
#     --dry-run              仅显示测试计划而不执行
#     --verbose              显示详细的 curl 请求和响应
#     --no-cleanup           测试结束后不清理测试数据
#     --help                 显示帮助信息
# 退出码:
#   0 - 所有测试通过
#   1 - 有测试失败
#   2 - 脚本参数错误
# ==============================================================================

# ==============================================================================
# 第一部分: 脚本头部和配置区
# ==============================================================================

set -euo pipefail

# ---------- 颜色输出常量 ----------
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ---------- 日志函数 ----------
log_info()    { echo -e "${BLUE}[INFO]${NC}    $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARN]${NC}    $1"; }
log_error()   { echo -e "${RED}[ERROR]${NC}   $1"; }

# ---------- 配置变量 ----------
NACOS_HOST="localhost"
NACOS_PORT="8848"
USERNAME="nacos"
PASSWORD="nacos"
NAMESPACE="public"
TEST_SERVICE_NAME="tailor-is-test-service"
TIMEOUT=10

# ---------- 运行时状态变量 ----------
DRY_RUN=false
VERBOSE=false
NO_CLEANUP=false
AUTH_TOKEN=""
CUSTOM_NAMESPACE_ID=""
TEST_START_TIME=0
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
TEST_RESULT_JSON=""
TEST_RESULTS_DIR="/opt/tailor-is/reports"
TIMESTAMP=""

# ---------- 测试结果存储数组 (使用分隔符拼接方式) ----------
RESULT_NAMES=""
RESULT_STATUS=""
RESULT_DURATION=""
RESULT_DETAIL=""

# ==============================================================================
# 基础工具函数区
# ==============================================================================

# ---------- 记录单个测试结果 ----------
# 参数: 名称 状态(PASS/FAIL) 耗时(ms) 详细信息
record_result() {
    local name="$1"
    local status="$2"
    local duration="$3"
    local detail="${4:-}"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    if [ "$status" = "PASS" ]; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
        echo -e "  ${GREEN}✔ PASS${NC}  ${name} (${duration}ms)"
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo -e "  ${RED}✘ FAIL${NC}  ${name} (${duration}ms)"
        if [ -n "$detail" ]; then
            echo -e "    ${RED}原因: ${detail}${NC}"
        fi
    fi

    # 用 '|' 分隔存储，后续汇总使用
    RESULT_NAMES="${RESULT_NAMES}${RESULT_NAMES:+|}${name}"
    RESULT_STATUS="${RESULT_STATUS}${RESULT_STATUS:+|}${status}"
    RESULT_DURATION="${RESULT_DURATION}${RESULT_DURATION:+|}${duration}"
    RESULT_DETAIL="${RESULT_DETAIL}${RESULT_DETAIL:+|}${detail}"
}

# ---------- 执行 HTTP 请求并测量耗时 ----------
# 返回: 通过全局变量 RESPONSE_BODY / HTTP_STATUS / RESPONSE_DURATION_MS 获取结果
# 参数: method url [post_data] [extra_headers]
do_http_request() {
    local method="$1"
    local url="$2"
    local post_data="${3:-}"
    local extra_headers="${4:-}"

    local curl_args=(-s -w "\n%{http_code}\n%{time_total}" --max-time "$TIMEOUT" --connect-timeout "$TIMEOUT")

    if [ "$VERBOSE" = true ]; then
        curl_args+=(-v)
    fi

    if [ -n "$AUTH_TOKEN" ]; then
        curl_args+=(-H "Authorization: Bearer ${AUTH_TOKEN}")
    fi

    if [ -n "$extra_headers" ]; then
        curl_args+=(-H "$extra_headers")
    fi

    local output
    if [ "$method" = "GET" ]; then
        if [ "$VERBOSE" = true ]; then
            log_info "GET ${url}"
        fi
        output=$(curl "${curl_args[@]}" "$url" 2>&1 || true)
    elif [ "$method" = "POST" ]; then
        curl_args+=(-X POST -H "Content-Type: application/x-www-form-urlencoded")
        if [ -n "$post_data" ]; then
            curl_args+=(--data "$post_data")
        fi
        if [ "$VERBOSE" = true ]; then
            log_info "POST ${url} data=${post_data}"
        fi
        output=$(curl "${curl_args[@]}" "$url" 2>&1 || true)
    elif [ "$method" = "PUT" ]; then
        curl_args+=(-X PUT -H "Content-Type: application/x-www-form-urlencoded")
        if [ -n "$post_data" ]; then
            curl_args+=(--data "$post_data")
        fi
        if [ "$VERBOSE" = true ]; then
            log_info "PUT ${url} data=${post_data}"
        fi
        output=$(curl "${curl_args[@]}" "$url" 2>&1 || true)
    elif [ "$method" = "DELETE" ]; then
        curl_args+=(-X DELETE)
        if [ -n "$post_data" ]; then
            curl_args+=(-H "Content-Type: application/x-www-form-urlencoded" --data "$post_data")
        fi
        if [ "$VERBOSE" = true ]; then
            log_info "DELETE ${url} data=${post_data}"
        fi
        output=$(curl "${curl_args[@]}" "$url" 2>&1 || true)
    else
        log_error "不支持的 HTTP 方法: ${method}"
        return 1
    fi

    # 解析 curl 输出: 最后一行是 time_total，倒数第二行是 http_code，其余是响应体
    local total_lines
    total_lines=$(echo "$output" | wc -l)
    if [ "$total_lines" -lt 3 ]; then
        RESPONSE_BODY=""
        HTTP_STATUS="000"
        RESPONSE_DURATION_MS="0"
        return 1
    fi

    RESPONSE_DURATION_MS=$(echo "$output" | tail -n 1 | awk '{printf "%.0f", $1 * 1000}')
    HTTP_STATUS=$(echo "$output" | tail -n 2 | head -n 1 | tr -d '[:space:]')
    RESPONSE_BODY=$(echo "$output" | head -n $((total_lines - 2)) | sed '/^$/d' | tail -n +1)

    if [ "$VERBOSE" = true ]; then
        log_info "HTTP ${HTTP_STATUS} 耗时 ${RESPONSE_DURATION_MS}ms"
        if [ -n "$RESPONSE_BODY" ]; then
            echo "  Body: ${RESPONSE_BODY}"
        fi
    fi

    return 0
}

# ---------- 检查端口是否可连接 ----------
check_port_connectable() {
    local host="$1"
    local port="$2"
    if command -v nc >/dev/null 2>&1; then
        nc -z -w "$TIMEOUT" "$host" "$port" >/dev/null 2>&1
    else
        # 退回到 bash 内置的 /dev/tcp
        (exec 3<>/dev/tcp/"$host"/"$port") 2>/dev/null
    fi
}

# ---------- JSON 字符串转义 ----------
json_escape() {
    local s="$1"
    s="${s//\\/\\\\}"
    s="${s//\"/\\\"}"
    s="${s//$'\n'/\\n}"
    s="${s//$'\r'/\\r}"
    s="${s//$'\t'/\\t}"
    printf "%s" "$s"
}

# ---------- 打印分组标题 ----------
print_section() {
    local title="$1"
    echo ""
    echo "===================================================================="
    echo "  ${title}"
    echo "===================================================================="
}

# ==============================================================================
# 命令行参数解析区
# ==============================================================================

show_help() {
    cat << 'HELP_EOF'
Nacos 功能测试脚本 v1.0

用法: ./nacos-function-test.sh [选项]

选项:
  --host <host>          Nacos 主机地址 (默认: localhost)
  --port <port>          Nacos 端口 (默认: 8848)
  --username <user>      登录用户名 (默认: nacos)
  --password <pwd>       登录密码 (默认: nacos)
  --namespace <ns>       测试使用的命名空间 (默认: public)
  --timeout <sec>        单次请求超时秒 (默认: 10)
  --dry-run              仅列出测试计划而不实际执行
  --verbose              打印详细 curl 请求/响应信息
  --no-cleanup           测试结束后保留测试数据
  --help                 显示此帮助信息

示例:
  ./nacos-function-test.sh
  ./nacos-function-test.sh --host 192.168.1.100 --port 8848 --verbose
  ./nacos-function-test.sh --host nacos.example.com --username admin --password mypwd
  ./nacos-function-test.sh --dry-run
HELP_EOF
}

parse_arguments() {
    while [ $# -gt 0 ]; do
        case "$1" in
            --host)
                NACOS_HOST="$2"; shift 2 ;;
            --port)
                NACOS_PORT="$2"; shift 2 ;;
            --username)
                USERNAME="$2"; shift 2 ;;
            --password)
                PASSWORD="$2"; shift 2 ;;
            --namespace)
                NAMESPACE="$2"; shift 2 ;;
            --timeout)
                TIMEOUT="$2"; shift 2 ;;
            --dry-run)
                DRY_RUN=true; shift ;;
            --verbose)
                VERBOSE=true; shift ;;
            --no-cleanup)
                NO_CLEANUP=true; shift ;;
            --help|-h)
                show_help; exit 0 ;;
            *)
                log_error "未知参数: $1"
                show_help
                exit 2 ;;
        esac
    done
}

# ==============================================================================
# 第二部分: 服务可用性测试
# ==============================================================================

run_availability_tests() {
    print_section "第二部分: 服务可用性测试"
    local base_url="http://${NACOS_HOST}:${NACOS_PORT}"
    local start_ms duration_ms

    # --- 测试 1: 端口连通性 ---
    start_ms=$(date +%s%3N)
    if check_port_connectable "$NACOS_HOST" "$NACOS_PORT"; then
        duration_ms=$(( $(date +%s%3N) - start_ms ))
        record_result "端口连通性检查 ${NACOS_HOST}:${NACOS_PORT}" "PASS" "$duration_ms" ""
    else
        duration_ms=$(( $(date +%s%3N) - start_ms ))
        record_result "端口连通性检查 ${NACOS_HOST}:${NACOS_PORT}" "FAIL" "$duration_ms" \
            "无法连接到 ${NACOS_HOST}:${NACOS_PORT}"
        return 1
    fi

    # --- 测试 2: metrics 健康检查接口 ---
    do_http_request "GET" "${base_url}/nacos/v1/ns/operator/metrics" "" ""
    local http_code="$HTTP_STATUS"
    local body="$RESPONSE_BODY"
    local duration_ms="$RESPONSE_DURATION_MS"

    if echo "$http_code" | grep -qE "^2[0-9][0-9]$"; then
        # --- 测试 3: 响应内容包含关键字 UP 或 status ---
        if echo "$body" | grep -qE "UP|status|StatusCheck"; then
            record_result "/nacos/v1/ns/operator/metrics 健康检查" "PASS" "$duration_ms" \
                "HTTP ${http_code}, 响应包含健康关键字"
        else
            record_result "/nacos/v1/ns/operator/metrics 健康检查" "FAIL" "$duration_ms" \
                "HTTP ${http_code}, 但响应体未包含 UP/status 关键字"
        fi
    else
        record_result "/nacos/v1/ns/operator/metrics 健康检查" "FAIL" "$duration_ms" \
            "HTTP ${http_code}, 响应体: ${body:-(空)}"
    fi

    # --- 测试 4: 首页可访问 ---
    do_http_request "GET" "${base_url}/nacos/" "" ""
    if echo "$HTTP_STATUS" | grep -qE "^(2|3)[0-9][0-9]$"; then
        record_result "Nacos Web 首页可达" "PASS" "$RESPONSE_DURATION_MS" \
            "HTTP ${HTTP_STATUS}"
    else
        record_result "Nacos Web 首页可达" "FAIL" "$RESPONSE_DURATION_MS" \
            "HTTP ${HTTP_STATUS}, 响应体: ${RESPONSE_BODY:-(空)}"
    fi
}

# ==============================================================================
# 第三部分: 配置管理功能测试
# ==============================================================================

run_config_tests() {
    print_section "第三部分: 配置管理功能测试"
    local base_url="http://${NACOS_HOST}:${NACOS_PORT}"
    local data_id="test-config-$(date +%s)"
    local group="DEFAULT_GROUP"
    local content="app.name=tailor-is-test\napp.version=1.0.0\ndb.url=jdbc:mysql://localhost:3306/test"
    local updated_content="app.name=tailor-is-test\napp.version=2.0.0\ndb.url=jdbc:mysql://localhost:3306/production"
    local body

    # --- 测试 1: 创建配置 ---
    do_http_request "POST" "${base_url}/nacos/v1/cs/configs" \
        "dataId=${data_id}&group=${group}&content=${content}&type=properties" ""
    body="$RESPONSE_BODY"
    if echo "$HTTP_STATUS" | grep -qE "^2[0-9][0-9]$" && echo "$body" | grep -qiE "true|成功|success|ok"; then
        record_result "创建配置 dataId=${data_id}" "PASS" "$RESPONSE_DURATION_MS" ""
    else
        record_result "创建配置 dataId=${data_id}" "FAIL" "$RESPONSE_DURATION_MS" \
            "HTTP ${HTTP_STATUS}, 响应: ${body:-(空)}"
    fi

    # 等待配置生效（Nacos 可能需要几百毫秒）
    sleep 1

    # --- 测试 2: 获取配置 ---
    do_http_request "GET" "${base_url}/nacos/v1/cs/configs?dataId=${data_id}&group=${group}" "" ""
    body="$RESPONSE_BODY"
    if echo "$HTTP_STATUS" | grep -qE "^2[0-9][0-9]$" && [ -n "$body" ]; then
        record_result "获取配置 dataId=${data_id}" "PASS" "$RESPONSE_DURATION_MS" \
            "返回 ${#body} 字节内容"
    else
        record_result "获取配置 dataId=${data_id}" "FAIL" "$RESPONSE_DURATION_MS" \
            "HTTP ${HTTP_STATUS}, 响应: ${body:-(空)}"
    fi

    # --- 测试 3: 查询配置列表（分页） ---
    do_http_request "GET" "${base_url}/nacos/v1/cs/configs?search=accurate&dataId=&group=&pageNo=1&pageSize=10&tenant=${NAMESPACE}" "" ""
    body="$RESPONSE_BODY"
    if echo "$HTTP_STATUS" | grep -qE "^2[0-9][0-9]$"; then
        record_result "分页查询配置列表 (pageSize=10)" "PASS" "$RESPONSE_DURATION_MS" \
            "HTTP ${HTTP_STATUS}"
    else
        record_result "分页查询配置列表 (pageSize=10)" "FAIL" "$RESPONSE_DURATION_MS" \
            "HTTP ${HTTP_STATUS}, 响应: ${body:-(空)}"
    fi

    # --- 测试 4: 监听配置变化 ---
    # listener 接口是长轮询，使用较短超时进行探测
    local listener_data="Listening-Configs=%5B%7B%22dataId%22%3A%22${data_id}%22%2C%22group%22%3A%22${group}%22%2C%22contentMD5%22%3A%22%22%2C%22tenant%22%3A%22%22%7D%5D"
    do_http_request "POST" "${base_url}/nacos/v1/cs/configs/listener" "$listener_data" "Long-Pulling-Timeout: 1000"
    body="$RESPONSE_BODY"
    # listener 的正常响应为 200 + 空字符串表示无变化，或 409 表示有变化
    if echo "$HTTP_STATUS" | grep -qE "^(2|4)[0-9][0-9]$"; then
        record_result "监听配置变化 (long-polling)" "PASS" "$RESPONSE_DURATION_MS" \
            "HTTP ${HTTP_STATUS}"
    else
        record_result "监听配置变化 (long-polling)" "FAIL" "$RESPONSE_DURATION_MS" \
            "HTTP ${HTTP_STATUS}, 响应: ${body:-(空)}"
    fi

    # --- 测试 5: 更新配置 ---
    do_http_request "POST" "${base_url}/nacos/v1/cs/configs" \
        "dataId=${data_id}&group=${group}&content=${updated_content}&type=properties" ""
    body="$RESPONSE_BODY"
    if echo "$HTTP_STATUS" | grep -qE "^2[0-9][0-9]$" && echo "$body" | grep -qiE "true|成功|success|ok"; then
        record_result "更新配置 dataId=${data_id}" "PASS" "$RESPONSE_DURATION_MS" ""
    else
        record_result "更新配置 dataId=${data_id}" "FAIL" "$RESPONSE_DURATION_MS" \
            "HTTP ${HTTP_STATUS}, 响应: ${body:-(空)}"
    fi

    # 保存 data_id 供清理使用
    TEST_CONFIG_DATA_IDS="${TEST_CONFIG_DATA_IDS:-}${TEST_CONFIG_DATA_IDS:+,}${data_id}"
}

# ==============================================================================
# 第四部分: 服务注册与发现功能测试
# ==============================================================================

run_discovery_tests() {
    print_section "第四部分: 服务注册与发现功能测试"
    local base_url="http://${NACOS_HOST}:${NACOS_PORT}"
    local service_name="${TEST_SERVICE_NAME}-$(date +%s)"
    local service_ip="127.0.0.1"
    local service_port="19876"
    local cluster_name="DEFAULT"
    local body

    # --- 测试 1: 注册服务实例 ---
    do_http_request "POST" "${base_url}/nacos/v1/ns/instance" \
        "serviceName=${service_name}&ip=${service_ip}&port=${service_port}&weight=1.0&healthy=true&ephemeral=true&clusterName=${cluster_name}&namespaceId=${NAMESPACE}" ""
    body="$RESPONSE_BODY"
    if echo "$HTTP_STATUS" | grep -qE "^2[0-9][0-9]$" && echo "$body" | grep -qiE "ok|true|成功"; then
        record_result "注册服务实例 ${service_name} ${service_ip}:${service_port}" "PASS" "$RESPONSE_DURATION_MS" ""
    else
        record_result "注册服务实例 ${service_name} ${service_ip}:${service_port}" "FAIL" "$RESPONSE_DURATION_MS" \
            "HTTP ${HTTP_STATUS}, 响应: ${body:-(空)}"
    fi

    # 等待实例注册完成
    sleep 2

    # --- 测试 2: 查询服务实例列表 ---
    do_http_request "GET" "${base_url}/nacos/v1/ns/instance/list?serviceName=${service_name}&namespaceId=${NAMESPACE}" "" ""
    body="$RESPONSE_BODY"
    if echo "$HTTP_STATUS" | grep -qE "^2[0-9][0-9]$"; then
        # 计算返回的 hosts 数
        local host_count
        host_count=$(echo "$body" | grep -o '"ip"' | wc -l || true)
        record_result "查询服务实例列表 ${service_name}" "PASS" "$RESPONSE_DURATION_MS" \
            "发现 ${host_count} 个实例"
    else
        record_result "查询服务实例列表 ${service_name}" "FAIL" "$RESPONSE_DURATION_MS" \
            "HTTP ${HTTP_STATUS}, 响应: ${body:-(空)}"
    fi

    # --- 测试 3: 查询单个服务实例详情 ---
    do_http_request "GET" "${base_url}/nacos/v1/ns/instance?serviceName=${service_name}&ip=${service_ip}&port=${service_port}&namespaceId=${NAMESPACE}" "" ""
    body="$RESPONSE_BODY"
    if echo "$HTTP_STATUS" | grep -qE "^2[0-9][0-9]$" && [ -n "$body" ]; then
        record_result "查询实例详情 ${service_ip}:${service_port}" "PASS" "$RESPONSE_DURATION_MS" \
            "返回 ${#body} 字节"
    else
        record_result "查询实例详情 ${service_ip}:${service_port}" "FAIL" "$RESPONSE_DURATION_MS" \
            "HTTP ${HTTP_STATUS}, 响应: ${body:-(空)}"
    fi

    # --- 测试 4: 更新服务实例健康状态 ---
    do_http_request "PUT" "${base_url}/nacos/v1/ns/instance/health" \
        "serviceName=${service_name}&ip=${service_ip}&port=${service_port}&healthy=false&namespaceId=${NAMESPACE}" ""
    body="$RESPONSE_BODY"
    if echo "$HTTP_STATUS" | grep -qE "^2[0-9][0-9]$"; then
        record_result "更新实例健康状态 (healthy=false)" "PASS" "$RESPONSE_DURATION_MS" \
            "HTTP ${HTTP_STATUS}"
    else
        record_result "更新实例健康状态 (healthy=false)" "FAIL" "$RESPONSE_DURATION_MS" \
            "HTTP ${HTTP_STATUS}, 响应: ${body:-(空)}"
    fi

    # --- 测试 5: 获取服务名称列表 ---
    do_http_request "GET" "${base_url}/nacos/v1/ns/service/list?pageNo=1&pageSize=10&namespaceId=${NAMESPACE}" "" ""
    body="$RESPONSE_BODY"
    if echo "$HTTP_STATUS" | grep -qE "^2[0-9][0-9]$"; then
        record_result "获取服务名称列表" "PASS" "$RESPONSE_DURATION_MS" \
            "HTTP ${HTTP_STATUS}"
    else
        record_result "获取服务名称列表" "FAIL" "$RESPONSE_DURATION_MS" \
            "HTTP ${HTTP_STATUS}, 响应: ${body:-(空)}"
    fi

    # --- 测试 6: 注销服务实例 ---
    do_http_request "DELETE" "${base_url}/nacos/v1/ns/instance" \
        "serviceName=${service_name}&ip=${service_ip}&port=${service_port}&namespaceId=${NAMESPACE}" ""
    body="$RESPONSE_BODY"
    if echo "$HTTP_STATUS" | grep -qE "^2[0-9][0-9]$"; then
        record_result "注销服务实例 ${service_name}" "PASS" "$RESPONSE_DURATION_MS" \
            "HTTP ${HTTP_STATUS}"
    else
        record_result "注销服务实例 ${service_name}" "FAIL" "$RESPONSE_DURATION_MS" \
            "HTTP ${HTTP_STATUS}, 响应: ${body:-(空)}"
    fi

    # 保存服务名供清理使用
    TEST_SERVICE_NAMES="${TEST_SERVICE_NAMES:-}${TEST_SERVICE_NAMES:+,}${service_name}"
}

# ==============================================================================
# 第五部分: 命名空间管理测试
# ==============================================================================

run_namespace_tests() {
    print_section "第五部分: 命名空间管理测试"
    local base_url="http://${NACOS_HOST}:${NACOS_PORT}"
    local ns_name="test-namespace-$(date +%s)"
    local ns_id="test-ns-$(date +%s)"
    local body

    # --- 测试 1: 创建命名空间 ---
    do_http_request "POST" "${base_url}/nacos/v1/console/namespaces" \
        "customNamespaceId=${ns_id}&namespaceName=${ns_name}&namespaceDesc=Test%20namespace" ""
    body="$RESPONSE_BODY"
    # 不同版本 Nacos 接口路径不同，再尝试 v2 路径
    if ! echo "$HTTP_STATUS" | grep -qE "^2[0-9][0-9]$" || ! echo "$body" | grep -qiE "true|ok|成功"; then
        do_http_request "POST" "${base_url}/nacos/v1/console/namespaces/custom" \
            "customNamespaceId=${ns_id}&namespaceName=${ns_name}&namespaceDesc=Test%20namespace" ""
        body="$RESPONSE_BODY"
    fi

    if echo "$HTTP_STATUS" | grep -qE "^2[0-9][0-9]$" && echo "$body" | grep -qiE "true|ok|成功"; then
        CUSTOM_NAMESPACE_ID="$ns_id"
        record_result "创建命名空间 ${ns_name} (${ns_id})" "PASS" "$RESPONSE_DURATION_MS" ""
    else
        record_result "创建命名空间 ${ns_name} (${ns_id})" "FAIL" "$RESPONSE_DURATION_MS" \
            "HTTP ${HTTP_STATUS}, 响应: ${body:-(空)}"
        return 0
    fi

    # 等待命名空间生效
    sleep 1

    # --- 测试 2: 查询命名空间列表 ---
    do_http_request "GET" "${base_url}/nacos/v1/console/namespaces" "" ""
    body="$RESPONSE_BODY"
    if echo "$HTTP_STATUS" | grep -qE "^2[0-9][0-9]$" && echo "$body" | grep -q "$ns_id"; then
        record_result "查询命名空间列表（验证新命名空间）" "PASS" "$RESPONSE_DURATION_MS" \
            "列表中包含 ${ns_id}"
    else
        record_result "查询命名空间列表（验证新命名空间）" "FAIL" "$RESPONSE_DURATION_MS" \
            "HTTP ${HTTP_STATUS}, 未找到 ${ns_id}, 响应: ${body:-(空)}"
    fi

    # --- 测试 3: 在自定义命名空间中创建配置 ---
    local ns_data_id="namespace-isolated-config"
    local ns_group="DEFAULT_GROUP"
    local ns_content="isolated.key=isolated.value"
    do_http_request "POST" "${base_url}/nacos/v1/cs/configs" \
        "dataId=${ns_data_id}&group=${ns_group}&tenant=${ns_id}&content=${ns_content}&type=properties" ""
    body="$RESPONSE_BODY"
    if echo "$HTTP_STATUS" | grep -qE "^2[0-9][0-9]$" && echo "$body" | grep -qiE "true|ok|成功"; then
        record_result "在自定义命名空间中创建配置" "PASS" "$RESPONSE_DURATION_MS" \
            "tenant=${ns_id}"
    else
        record_result "在自定义命名空间中创建配置" "FAIL" "$RESPONSE_DURATION_MS" \
            "HTTP ${HTTP_STATUS}, 响应: ${body:-(空)}"
    fi

    # --- 测试 4: 从 default 命名空间读取不到隔离配置（验证隔离） ---
    sleep 1
    do_http_request "GET" "${base_url}/nacos/v1/cs/configs?dataId=${ns_data_id}&group=${ns_group}&tenant=public" "" ""
    body="$RESPONSE_BODY"
    # default 命名空间中应不存在该配置（响应为空或 404）
    if [ -z "$body" ] || echo "$HTTP_STATUS" | grep -qE "^4[0-9][0-9]$"; then
        record_result "验证命名空间隔离（public 中不可见）" "PASS" "$RESPONSE_DURATION_MS" \
            "HTTP ${HTTP_STATUS}, 响应体长度 ${#body}"
    else
        record_result "验证命名空间隔离（public 中不可见）" "FAIL" "$RESPONSE_DURATION_MS" \
            "HTTP ${HTTP_STATUS}, 响应体: ${body}"
    fi

    # --- 测试 5: 删除测试命名空间 ---
    do_http_request "DELETE" "${base_url}/nacos/v1/console/namespaces?namespaceId=${ns_id}" "" ""
    body="$RESPONSE_BODY"
    if echo "$HTTP_STATUS" | grep -qE "^2[0-9][0-9]$" && echo "$body" | grep -qiE "true|ok|成功"; then
        record_result "删除测试命名空间 ${ns_id}" "PASS" "$RESPONSE_DURATION_MS" ""
        CUSTOM_NAMESPACE_ID=""
    else
        record_result "删除测试命名空间 ${ns_id}" "FAIL" "$RESPONSE_DURATION_MS" \
            "HTTP ${HTTP_STATUS}, 响应: ${body:-(空)}"
    fi
}

# ==============================================================================
# 第六部分: API 认证测试
# ==============================================================================

run_auth_tests() {
    print_section "第六部分: API 认证测试"
    local base_url="http://${NACOS_HOST}:${NACOS_PORT}"
    local body saved_token="$AUTH_TOKEN"

    # --- 测试 1: 登录接口（正确凭据） ---
    AUTH_TOKEN=""
    do_http_request "POST" "${base_url}/nacos/v1/auth/login" \
        "username=${USERNAME}&password=${PASSWORD}" ""
    body="$RESPONSE_BODY"

    local login_ok=false
    local extracted_token=""
    if echo "$HTTP_STATUS" | grep -qE "^2[0-9][0-9]$"; then
        # 尝试从响应体中解析 accessToken / token
        extracted_token=$(echo "$body" | grep -oE '"accessToken"[[:space:]]*:[[:space:]]*"[^"]*"' | cut -d'"' -f4 || true)
        if [ -z "$extracted_token" ]; then
            extracted_token=$(echo "$body" | grep -oE '"token"[[:space:]]*:[[:space:]]*"[^"]*"' | cut -d'"' -f4 || true)
        fi
        if [ -n "$extracted_token" ]; then
            login_ok=true
            AUTH_TOKEN="$extracted_token"
        else
            # 部分版本登录成功返回 "ok" 或 true
            if echo "$body" | grep -qiE "ok|true|成功"; then
                login_ok=true
            fi
        fi
    fi

    if [ "$login_ok" = true ]; then
        record_result "API 登录接口（正确凭据）" "PASS" "$RESPONSE_DURATION_MS" \
            "token=${extracted_token:-(无 token，可能 Nacos 未启用认证)}"
    else
        record_result "API 登录接口（正确凭据）" "FAIL" "$RESPONSE_DURATION_MS" \
            "HTTP ${HTTP_STATUS}, 响应: ${body:-(空)}"
    fi

    # --- 测试 2: 使用 token 调用需认证 API ---
    if [ -n "$AUTH_TOKEN" ]; then
        do_http_request "GET" "${base_url}/nacos/v1/console/namespaces" "" ""
        if echo "$HTTP_STATUS" | grep -qE "^2[0-9][0-9]$"; then
            record_result "带 token 访问受保护接口" "PASS" "$RESPONSE_DURATION_MS" \
                "HTTP ${HTTP_STATUS}"
        else
            record_result "带 token 访问受保护接口" "FAIL" "$RESPONSE_DURATION_MS" \
                "HTTP ${HTTP_STATUS}, 响应: ${RESPONSE_BODY:-(空)}"
        fi

        # --- 测试 3: 不带 token 应被拒绝 ---
        local old_token="$AUTH_TOKEN"
        AUTH_TOKEN=""
        do_http_request "GET" "${base_url}/nacos/v1/console/namespaces" "" ""
        if echo "$HTTP_STATUS" | grep -qE "^(401|403)$"; then
            record_result "不带 token 访问受保护接口（应被拒绝）" "PASS" "$RESPONSE_DURATION_MS" \
                "HTTP ${HTTP_STATUS} (预期拒绝)"
        else
            # 如果是 200 可能意味着 Nacos 没有启用鉴权
            if echo "$HTTP_STATUS" | grep -qE "^2[0-9][0-9]$"; then
                record_result "不带 token 访问受保护接口（应被拒绝）" "FAIL" "$RESPONSE_DURATION_MS" \
                    "HTTP ${HTTP_STATUS} (Nacos 可能未启用鉴权，跳过该结果)"
            else
                record_result "不带 token 访问受保护接口（应被拒绝）" "FAIL" "$RESPONSE_DURATION_MS" \
                    "HTTP ${HTTP_STATUS}"
            fi
        fi
        AUTH_TOKEN="$old_token"
    else
        log_warning "未获取到有效 token，跳过认证相关测试（Nacos 可能未启用鉴权）"
    fi

    # 恢复 token
    AUTH_TOKEN="$saved_token"
}

# ==============================================================================
# 第七部分: 性能测试
# ==============================================================================

run_performance_tests() {
    print_section "第七部分: 性能测试"
    local base_url="http://${NACOS_HOST}:${NACOS_PORT}"
    local iterations=100
    local success_count=0
    local total_duration=0
    local i body
    local perf_data_id="perf-test-config-$(date +%s)"

    # --- 测试 1: 配置发布 QPS 测试 ---
    log_info "开始配置发布性能测试: ${iterations} 次连续发布"
    success_count=0
    total_duration=0
    for i in $(seq 1 $iterations); do
        do_http_request "POST" "${base_url}/nacos/v1/cs/configs" \
            "dataId=${perf_data_id}&group=DEFAULT_GROUP&content=key${i}=value${i}&type=properties" ""
        if echo "$HTTP_STATUS" | grep -qE "^2[0-9][0-9]$"; then
            success_count=$((success_count + 1))
            total_duration=$((total_duration + RESPONSE_DURATION_MS))
        fi
    done

    if [ "$success_count" -gt 0 ]; then
        local avg=$((total_duration / success_count))
        record_result "配置发布性能测试 (${iterations} 次, 成功 ${success_count} 次)" "PASS" \
            "$avg" "平均响应时间 ${avg}ms"
    else
        record_result "配置发布性能测试" "FAIL" "0" "0 次成功"
    fi

    # 清理性能测试配置
    sleep 1
    do_http_request "DELETE" "${base_url}/nacos/v1/cs/configs" \
        "dataId=${perf_data_id}&group=DEFAULT_GROUP" ""

    # --- 测试 2: 服务注册/发现 QPS 测试 ---
    local perf_service="perf-test-service-$(date +%s)"
    log_info "开始服务注册/发现性能测试: ${iterations} 次连续查询"
    # 先注册一个实例
    do_http_request "POST" "${base_url}/nacos/v1/ns/instance" \
        "serviceName=${perf_service}&ip=127.0.0.1&port=9999&weight=1.0&healthy=true&ephemeral=true&namespaceId=${NAMESPACE}" ""
    sleep 2

    success_count=0
    total_duration=0
    for i in $(seq 1 $iterations); do
        do_http_request "GET" "${base_url}/nacos/v1/ns/instance/list?serviceName=${perf_service}&namespaceId=${NAMESPACE}" "" ""
        if echo "$HTTP_STATUS" | grep -qE "^2[0-9][0-9]$"; then
            success_count=$((success_count + 1))
            total_duration=$((total_duration + RESPONSE_DURATION_MS))
        fi
    done

    if [ "$success_count" -gt 0 ]; then
        local avg=$((total_duration / success_count))
        record_result "服务发现性能测试 (${iterations} 次, 成功 ${success_count} 次)" "PASS" \
            "$avg" "平均响应时间 ${avg}ms"
    else
        record_result "服务发现性能测试" "FAIL" "0" "0 次成功"
    fi

    # 清理性能测试实例
    do_http_request "DELETE" "${base_url}/nacos/v1/ns/instance" \
        "serviceName=${perf_service}&ip=127.0.0.1&port=9999&namespaceId=${NAMESPACE}" ""
}

# ==============================================================================
# 第八部分: 测试报告生成
# ==============================================================================

generate_reports() {
    print_section "第八部分: 测试报告生成"
    local report_file="${TEST_RESULTS_DIR}/nacos-test-result-${TIMESTAMP}.json"
    local summary_file="${TEST_RESULTS_DIR}/nacos-test-summary-${TIMESTAMP}.txt"

    mkdir -p "$TEST_RESULTS_DIR" 2>/dev/null || true

    # ---------- 构建 JSON 报告 ----------
    local pass_rate=0
    if [ "$TOTAL_TESTS" -gt 0 ]; then
        pass_rate=$(( (PASSED_TESTS * 100) / TOTAL_TESTS ))
    fi

    local test_end_time
    test_end_time=$(date +%s)
    local total_duration=$(( test_end_time - TEST_START_TIME ))

    # 构造 JSON 内容
    local json_body="{"
    json_body="${json_body}\"timestamp\":\"${TIMESTAMP}\","
    json_body="${json_body}\"nacos_host\":\"$(json_escape "${NACOS_HOST}")\","
    json_body="${json_body}\"nacos_port\":\"${NACOS_PORT}\","
    json_body="${json_body}\"namespace\":\"$(json_escape "${NAMESPACE}")\","
    json_body="${json_body}\"total_tests\":${TOTAL_TESTS},"
    json_body="${json_body}\"passed\":${PASSED_TESTS},"
    json_body="${json_body}\"failed\":${FAILED_TESTS},"
    json_body="${json_body}\"pass_rate\":\"${pass_rate}%\","
    json_body="${json_body}\"total_duration_sec\":${total_duration},"
    json_body="${json_body}\"tests\":["

    # 拆分每个测试结果
    local IFS='|'
    local -a names=($RESULT_NAMES)
    local -a statuses=($RESULT_STATUS)
    local -a durations=($RESULT_DURATION)
    local -a details=($RESULT_DETAIL)
    unset IFS

    local idx
    for idx in "${!names[@]}"; do
        local comma=","
        if [ "$idx" -eq 0 ]; then comma=""; fi
        local n_escaped=$(json_escape "${names[$idx]}")
        local d_escaped=$(json_escape "${details[$idx]:-}")
        json_body="${json_body}${comma}{\"name\":\"${n_escaped}\",\"status\":\"${statuses[$idx]}\",\"duration_ms\":${durations[$idx]:-0},\"detail\":\"${d_escaped}\"}"
    done

    json_body="${json_body}]}"

    # 写入 JSON 文件
    echo "$json_body" > "$report_file" 2>/dev/null || {
        # 失败时使用 python 辅助
        if command -v python3 >/dev/null 2>&1; then
            python3 -c "
import json, sys
data = {
    'timestamp': '${TIMESTAMP}',
    'nacos_host': '${NACOS_HOST}',
    'nacos_port': '${NACOS_PORT}',
    'namespace': '${NAMESPACE}',
    'total_tests': ${TOTAL_TESTS},
    'passed': ${PASSED_TESTS},
    'failed': ${FAILED_TESTS},
    'pass_rate': '${pass_rate}%',
    'total_duration_sec': ${total_duration},
}
tests = []
ns = '${RESULT_NAMES//|/|}'.split('|')
ss = '${RESULT_STATUS//|/|}'.split('|')
ds = '${RESULT_DURATION//|/|}'.split('|')
for i in range(len(ns)):
    tests.append({'name': ns[i], 'status': ss[i] if i<len(ss) else 'PASS', 'duration_ms': int(ds[i]) if i<len(ds) else 0})
data['tests'] = tests
with open('${report_file}', 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=2)
" 2>/dev/null || echo "$json_body" > "$report_file"
        else
            echo "$json_body" > "$report_file"
        fi
    }

    # JSON 格式化（若 python 可用）
    if command -v python3 >/dev/null 2>&1; then
        python3 -c "import json,sys; d=json.load(open('${report_file}')); json.dump(d, open('${report_file}','w'), ensure_ascii=False, indent=2)" 2>/dev/null || true
    fi

    log_success "JSON 报告已生成: ${report_file}"

    # ---------- 生成人类可读摘要 ----------
    {
        echo "============================================================"
        echo "  Nacos 功能测试摘要报告"
        echo "============================================================"
        echo "  生成时间: ${TIMESTAMP}"
        echo "  测试目标: ${NACOS_HOST}:${NACOS_PORT}"
        echo "  命名空间: ${NAMESPACE}"
        echo "  总耗时  : ${total_duration} 秒"
        echo "------------------------------------------------------------"
        echo "  总测试数: ${TOTAL_TESTS}"
        echo "  通过    : ${PASSED_TESTS}"
        echo "  失败    : ${FAILED_TESTS}"
        echo "  通过率  : ${pass_rate}%"
        echo "------------------------------------------------------------"
        echo "  详细结果:"
        local idx
        for idx in "${!names[@]}"; do
            local symbol="✔"
            if [ "${statuses[$idx]}" = "FAIL" ]; then symbol="✘"; fi
            echo "    ${symbol} [${statuses[$idx]}] ${names[$idx]} (${durations[$idx]:-0}ms)"
        done
        echo "============================================================"
        if [ "$FAILED_TESTS" -eq 0 ]; then
            echo "  状态: 🟢 全部通过"
        elif [ "$pass_rate" -ge 80 ]; then
            echo "  状态: 🟡 基本正常，存在问题项"
        else
            echo "  状态: 🔴 存在大量失败，需立即检查"
        fi
        echo "============================================================"
    } > "$summary_file" 2>/dev/null || true

    log_success "文本摘要已生成: ${summary_file}"

    # ---------- 控制台输出摘要 ----------
    echo ""
    echo "============================================================"
    echo "  测试汇总"
    echo "============================================================"
    echo "  目标    : http://${NACOS_HOST}:${NACOS_PORT}"
    echo "  总测试  : ${TOTAL_TESTS}"
    echo "  通过    : ${GREEN}${PASSED_TESTS}${NC}"
    echo "  失败    : ${RED}${FAILED_TESTS}${NC}"
    echo "  通过率  : ${pass_rate}%"
    echo "  JSON报告: ${report_file}"
    echo "============================================================"
}

# ==============================================================================
# 第九部分: 清理功能
# ==============================================================================

run_cleanup() {
    if [ "$NO_CLEANUP" = true ]; then
        log_warning "--no-cleanup 已启用，跳过数据清理"
        return 0
    fi

    print_section "第九部分: 清理测试数据"
    local base_url="http://${NACOS_HOST}:${NACOS_PORT}"
    local data_id service_name

    # 清理测试配置
    if [ -n "${TEST_CONFIG_DATA_IDS:-}" ]; then
        IFS=',' read -ra CONFIG_IDS <<< "$TEST_CONFIG_DATA_IDS"
        for data_id in "${CONFIG_IDS[@]}"; do
            do_http_request "DELETE" "${base_url}/nacos/v1/cs/configs" \
                "dataId=${data_id}&group=DEFAULT_GROUP&tenant=${NAMESPACE}" ""
            if echo "$HTTP_STATUS" | grep -qE "^2[0-9][0-9]$"; then
                log_info "已清理配置: ${data_id}"
            else
                log_warning "清理配置失败: ${data_id} (HTTP ${HTTP_STATUS})"
            fi
        done
    fi

    # 清理测试服务实例
    if [ -n "${TEST_SERVICE_NAMES:-}" ]; then
        IFS=',' read -ra SERVICE_ARR <<< "$TEST_SERVICE_NAMES"
        for service_name in "${SERVICE_ARR[@]}"; do
            do_http_request "DELETE" "${base_url}/nacos/v1/ns/instance" \
                "serviceName=${service_name}&ip=127.0.0.1&port=19876&namespaceId=${NAMESPACE}" ""
            if echo "$HTTP_STATUS" | grep -qE "^2[0-9][0-9]$"; then
                log_info "已清理服务实例: ${service_name}"
            else
                log_warning "清理服务实例失败: ${service_name} (HTTP ${HTTP_STATUS})"
            fi
        done
    fi

    # 清理可能残留的命名空间
    if [ -n "$CUSTOM_NAMESPACE_ID" ]; then
        do_http_request "DELETE" "${base_url}/nacos/v1/console/namespaces?namespaceId=${CUSTOM_NAMESPACE_ID}" "" ""
        log_info "已清理命名空间: ${CUSTOM_NAMESPACE_ID}"
    fi

    log_success "清理完成"
}

# ==============================================================================
# Dry-Run 测试计划展示
# ==============================================================================

show_test_plan() {
    echo "============================================================"
    echo "  Nacos 功能测试脚本 v1.0  -  Dry Run 模式"
    echo "============================================================"
    echo "  测试目标: http://${NACOS_HOST}:${NACOS_PORT}"
    echo "  命名空间: ${NAMESPACE}"
    echo "  超时时间: ${TIMEOUT} 秒"
    echo "  执行用户: ${USERNAME}"
    echo "============================================================"
    echo ""
    echo "【第二部分】服务可用性测试"
    echo "  1. TCP 端口连通性检查 ${NACOS_HOST}:${NACOS_PORT}"
    echo "  2. GET  /nacos/v1/ns/operator/metrics (健康检查接口)"
    echo "  3. GET  /nacos/ (首页可达性)"
    echo ""
    echo "【第三部分】配置管理功能测试"
    echo "  1. POST /nacos/v1/cs/configs (创建配置)"
    echo "  2. GET  /nacos/v1/cs/configs (获取配置)"
    echo "  3. GET  /nacos/v1/cs/configs?search=accurate (分页查询)"
    echo "  4. POST /nacos/v1/cs/configs/listener (长轮询监听)"
    echo "  5. POST /nacos/v1/cs/configs (更新配置)"
    echo ""
    echo "【第四部分】服务注册与发现功能测试"
    echo "  1. POST /nacos/v1/ns/instance (注册服务实例)"
    echo "  2. GET  /nacos/v1/ns/instance/list (查询实例列表)"
    echo "  3. GET  /nacos/v1/ns/instance (查询单个实例详情)"
    echo "  4. PUT  /nacos/v1/ns/instance/health (更新健康状态)"
    echo "  5. GET  /nacos/v1/ns/service/list (获取服务名列表)"
    echo "  6. DELETE /nacos/v1/ns/instance (注销实例)"
    echo ""
    echo "【第五部分】命名空间管理测试"
    echo "  1. POST /nacos/v1/console/namespaces (创建命名空间)"
    echo "  2. GET  /nacos/v1/console/namespaces (查询命名空间列表)"
    echo "  3. POST /nacos/v1/cs/configs (在自定义命名空间中创建配置)"
    echo "  4. GET  /nacos/v1/cs/configs (验证命名空间隔离)"
    echo "  5. DELETE /nacos/v1/console/namespaces (删除命名空间)"
    echo ""
    echo "【第六部分】API 认证测试"
    echo "  1. POST /nacos/v1/auth/login (正确凭据登录)"
    echo "  2. GET  受保护接口 (带 token)"
    echo "  3. GET  受保护接口 (不带 token，应被拒绝)"
    echo ""
    echo "【第七部分】性能测试"
    echo "  1. 配置发布 QPS 测试 - 100 次连续发布，计算平均响应时间"
    echo "  2. 服务发现 QPS 测试 - 100 次连续查询，计算平均响应时间"
    echo ""
    echo "【第八部分】测试报告"
    echo "  - 输出 JSON 格式报告到 ${TEST_RESULTS_DIR}/nacos-test-result-<时间戳>.json"
    echo "  - 输出人类可读文本摘要到 ${TEST_RESULTS_DIR}/nacos-test-summary-<时间戳>.txt"
    echo ""
    echo "【第九部分】清理（可通过 --no-cleanup 跳过）"
    echo "  - 删除所有测试配置"
    echo "  - 删除所有测试服务实例"
    echo "  - 删除测试命名空间"
    echo ""
    echo "============================================================"
    echo "  提示: 移除 --dry-run 参数以实际执行测试"
    echo "============================================================"
}

# ==============================================================================
# 主入口
# ==============================================================================

main() {
    parse_arguments "$@"

    TIMESTAMP=$(date +%Y%m%d-%H%M%S)
    TEST_START_TIME=$(date +%s)

    echo "============================================================"
    echo "  Nacos 功能测试脚本 v1.0"
    echo "  启动时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "  测试目标: http://${NACOS_HOST}:${NACOS_PORT}"
    echo "  命名空间: ${NAMESPACE}"
    echo "============================================================"

    # Dry-run 模式
    if [ "$DRY_RUN" = true ]; then
        show_test_plan
        exit 0
    fi

    # 环境检查
    if ! command -v curl >/dev/null 2>&1; then
        log_error "未检测到 curl 命令，请先安装 curl"
        exit 2
    fi

    # 按顺序执行各个测试部分
    # 第二部分: 服务可用性测试
    run_availability_tests || {
        log_warning "可用性测试有失败项，继续执行后续测试..."
    }

    # 第六部分: 认证（先获取 token，后续请求会自动带上）
    run_auth_tests

    # 第三部分: 配置管理
    run_config_tests

    # 第四部分: 服务注册发现
    run_discovery_tests

    # 第五部分: 命名空间
    run_namespace_tests

    # 第七部分: 性能测试
    run_performance_tests

    # 第八部分: 生成报告
    generate_reports

    # 第九部分: 清理
    run_cleanup

    echo ""
    echo "============================================================"
    echo "  测试完成: 共 ${TOTAL_TESTS} 项, 通过 ${PASSED_TESTS}, 失败 ${FAILED_TESTS}"
    echo "============================================================"

    # 根据结果返回退出码
    if [ "$FAILED_TESTS" -gt 0 ]; then
        exit 1
    else
        exit 0
    fi
}

# 启动主流程
main "$@"
