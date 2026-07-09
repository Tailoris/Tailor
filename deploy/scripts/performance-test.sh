#!/bin/bash
set -e

PROJECT_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
THRESHOLD_FILE="$PROJECT_ROOT/deploy/scripts/performance-threshold.yml"
RESULT_FILE="/tmp/performance-test-result.json"
GATEWAY_URL="${GATEWAY_URL:-http://localhost:9000}"

echo "========================================"
echo "Tailor IS v4.0 性能测试脚本"
echo "========================================"
echo ""

errors=()

load_thresholds() {
    echo "步骤1: 加载性能阈值配置..."
    
    if [ ! -f "$THRESHOLD_FILE" ]; then
        echo "❌ 错误: 未找到阈值配置文件 $THRESHOLD_FILE"
        exit 1
    fi
    
    echo "📋 阈值配置内容:"
    cat "$THRESHOLD_FILE"
    echo ""
    echo "✅ 阈值配置加载完成"
}

test_api_login() {
    echo "步骤2: 测试登录接口..."
    
    local p95_threshold=$(grep -A 2 "login:" "$THRESHOLD_FILE" | grep p95 | awk -F': ' '{print $2}')
    local p99_threshold=$(grep -A 3 "login:" "$THRESHOLD_FILE" | grep p99 | awk -F': ' '{print $2}')
    local error_rate_threshold=$(grep -A 4 "login:" "$THRESHOLD_FILE" | grep error_rate | awk -F': ' '{print $2}')
    
    echo "📋 阈值: p95=$p95_thresholdms, p99=$p99_thresholdms, error_rate=$error_rate_threshold"
    
    local total_time=0
    local max_time=0
    local min_time=999999
    local error_count=0
    local iterations=10
    
    for i in $(seq 1 $iterations); do
        local start=$(date +%s%3N)
        local response=$(curl -s -w "%{http_code}" -o /dev/null "$GATEWAY_URL/api/v1/auth/login" -X POST -H "Content-Type: application/json" -d '{"username":"test","password":"test"}')
        local end=$(date +%s%3N)
        local duration=$((end - start))
        
        if [ "$response" != "200" ] && [ "$response" != "401" ]; then
            error_count=$((error_count+1))
        fi
        
        total_time=$((total_time + duration))
        [ "$duration" -gt "$max_time" ] && max_time=$duration
        [ "$duration" -lt "$min_time" ] && min_time=$duration
        
        echo "  请求 $i: ${duration}ms (status: $response)"
    done
    
    local avg_time=$((total_time / iterations))
    local p99_time=$max_time
    local p95_time=$((max_time * 95 / 100))
    local error_rate=$(echo "scale=4; $error_count / $iterations" | bc)
    
    echo ""
    echo "📊 测试结果:"
    echo "  平均响应时间: ${avg_time}ms"
    echo "  P95响应时间: ${p95_time}ms"
    echo "  P99响应时间: ${p99_time}ms"
    echo "  最大响应时间: ${max_time}ms"
    echo "  最小响应时间: ${min_time}ms"
    echo "  错误率: ${error_rate}"
    
    if [ "$p95_time" -gt "$p95_threshold" ]; then
        errors+=("登录接口P95响应时间${p95_time}ms超过阈值${p95_threshold}ms")
        echo "❌ P95超过阈值"
    else
        echo "✅ P95通过"
    fi
    
    if [ "$p99_time" -gt "$p99_threshold" ]; then
        errors+=("登录接口P99响应时间${p99_time}ms超过阈值${p99_threshold}ms")
        echo "❌ P99超过阈值"
    else
        echo "✅ P99通过"
    fi
    
    if (( $(echo "$error_rate > $error_rate_threshold" | bc -l) )); then
        errors+=("登录接口错误率${error_rate}超过阈值${error_rate_threshold}")
        echo "❌ 错误率超过阈值"
    else
        echo "✅ 错误率通过"
    fi
    
    echo ""
}

test_api_user_profile() {
    echo "步骤3: 测试用户信息接口..."
    
    local p95_threshold=$(grep -A 2 "user_profile:" "$THRESHOLD_FILE" | grep p95 | awk -F': ' '{print $2}')
    local p99_threshold=$(grep -A 3 "user_profile:" "$THRESHOLD_FILE" | grep p99 | awk -F': ' '{print $2}')
    local error_rate_threshold=$(grep -A 4 "user_profile:" "$THRESHOLD_FILE" | grep error_rate | awk -F': ' '{print $2}')
    
    echo "📋 阈值: p95=$p95_thresholdms, p99=$p99_thresholdms, error_rate=$error_rate_threshold"
    
    local total_time=0
    local max_time=0
    local error_count=0
    local iterations=10
    
    for i in $(seq 1 $iterations); do
        local start=$(date +%s%3N)
        local response=$(curl -s -w "%{http_code}" -o /dev/null "$GATEWAY_URL/api/v1/user/profile")
        local end=$(date +%s%3N)
        local duration=$((end - start))
        
        if [ "$response" != "200" ] && [ "$response" != "401" ]; then
            error_count=$((error_count+1))
        fi
        
        total_time=$((total_time + duration))
        [ "$duration" -gt "$max_time" ] && max_time=$duration
        
        echo "  请求 $i: ${duration}ms (status: $response)"
    done
    
    local avg_time=$((total_time / iterations))
    local p99_time=$max_time
    local p95_time=$((max_time * 95 / 100))
    local error_rate=$(echo "scale=4; $error_count / $iterations" | bc)
    
    echo ""
    echo "📊 测试结果:"
    echo "  平均响应时间: ${avg_time}ms"
    echo "  P95响应时间: ${p95_time}ms"
    echo "  P99响应时间: ${p99_time}ms"
    
    if [ "$p95_time" -gt "$p95_threshold" ]; then
        errors+=("用户信息接口P95响应时间${p95_time}ms超过阈值${p95_threshold}ms")
        echo "❌ P95超过阈值"
    else
        echo "✅ P95通过"
    fi
    
    if [ "$p99_time" -gt "$p99_threshold" ]; then
        errors+=("用户信息接口P99响应时间${p99_time}ms超过阈值${p99_threshold}ms")
        echo "❌ P99超过阈值"
    else
        echo "✅ P99通过"
    fi
    
    echo ""
}

test_gateway_latency() {
    echo "步骤4: 测试网关延迟..."
    
    local p95_threshold=$(grep -A 2 "latency:" "$THRESHOLD_FILE" | grep p95 | awk -F': ' '{print $2}')
    local p99_threshold=$(grep -A 3 "latency:" "$THRESHOLD_FILE" | grep p99 | awk -F': ' '{print $2}')
    
    echo "📋 阈值: p95=$p95_thresholdms, p99=$p99_thresholdms"
    
    local total_time=0
    local max_time=0
    local iterations=20
    
    for i in $(seq 1 $iterations); do
        local start=$(date +%s%3N)
        curl -s -o /dev/null "$GATEWAY_URL/api/v1/user/profile"
        local end=$(date +%s%3N)
        local duration=$((end - start))
        
        total_time=$((total_time + duration))
        [ "$duration" -gt "$max_time" ] && max_time=$duration
    done
    
    local avg_time=$((total_time / iterations))
    local p99_time=$max_time
    local p95_time=$((max_time * 95 / 100))
    
    echo ""
    echo "📊 测试结果:"
    echo "  平均延迟: ${avg_time}ms"
    echo "  P95延迟: ${p95_time}ms"
    echo "  P99延迟: ${p99_time}ms"
    
    if [ "$p95_time" -gt "$p95_threshold" ]; then
        errors+=("网关P95延迟${p95_time}ms超过阈值${p95_threshold}ms")
        echo "❌ P95超过阈值"
    else
        echo "✅ P95通过"
    fi
    
    if [ "$p99_time" -gt "$p99_threshold" ]; then
        errors+=("网关P99延迟${p99_time}ms超过阈值${p99_threshold}ms")
        echo "❌ P99超过阈值"
    else
        echo "✅ P99通过"
    fi
    
    echo ""
}

generate_result() {
    echo "步骤5: 生成测试结果..."
    
    echo ""
    echo "========================================"
    if [ ${#errors[@]} -eq 0 ]; then
        echo "✅ 所有性能测试通过"
        echo "{\"status\": \"success\", \"errors\": [], \"message\": \"所有性能测试通过\"}" > "$RESULT_FILE"
        exit 0
    else
        echo "❌ 性能测试失败，共发现 ${#errors[@]} 个问题"
        echo ""
        for error in "${errors[@]}"; do
            echo "  - $error"
        done
        echo "{\"status\": \"failure\", \"errors\": [\"${errors[*]}\"], \"message\": \"性能测试失败\"}" > "$RESULT_FILE"
        exit 1
    fi
    echo "========================================"
}

load_thresholds
test_api_login
test_api_user_profile
test_gateway_latency
generate_result