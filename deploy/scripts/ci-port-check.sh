#!/bin/bash
set -e

PROJECT_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
CONSTANT_FILE="$PROJECT_ROOT/tailor-is/tailor-is-common/src/main/java/com/tailoris/common/constant/RoutePortConstant.java"
DC_FILE="$PROJECT_ROOT/docker-compose.yml"
RESULT_FILE="/tmp/port-check-result.json"

echo "========================================"
echo "Tailor IS v4.0 CI端口占用检查脚本"
echo "========================================"
echo ""

errors=()

extract_ports_from_constant() {
    echo "步骤1: 从RoutePortConstant.java提取端口..."
    
    if [ ! -f "$CONSTANT_FILE" ]; then
        echo "❌ 错误: 未找到 $CONSTANT_FILE"
        exit 1
    fi
    
    PORT_CONSTANTS=$(grep -E 'public static final int PORT_' "$CONSTANT_FILE" | awk -F'=' '{print $2}' | sed 's/[ ";]//g')
    echo "📋 定义的端口: $PORT_CONSTANTS"
    echo "$PORT_CONSTANTS"
}

check_port_conflicts() {
    echo "步骤2: 检查端口冲突..."
    
    if [ ! -f "$DC_FILE" ]; then
        echo "❌ 错误: 未找到 $DC_FILE"
        exit 1
    fi
    
    local port_usage=()
    local conflicts=()
    
    while IFS= read -r line; do
        if [[ "$line" =~ SERVER_PORT=([0-9]+) ]]; then
            local port="${BASH_REMATCH[1]}"
            local container_name=$(grep -B 10 "SERVER_PORT=$port" "$DC_FILE" | grep container_name | tail -1 | awk -F': ' '{print $2}')
            port_usage+=("$port:$container_name")
        fi
    done < <(grep "SERVER_PORT=" "$DC_FILE")
    
    declare -A port_map
    for entry in "${port_usage[@]}"; do
        port=$(echo "$entry" | cut -d':' -f1)
        container=$(echo "$entry" | cut -d':' -f2)
        if [ -n "${port_map[$port]}" ]; then
            conflicts+=("端口 $port 被 ${port_map[$port]} 和 $container 重复使用")
        else
            port_map[$port]="$container"
        fi
    done
    
    if [ ${#conflicts[@]} -gt 0 ]; then
        echo "❌ 发现端口冲突:"
        for conflict in "${conflicts[@]}"; do
            echo "  - $conflict"
            errors+=("$conflict")
        done
    else
        echo "✅ 端口冲突检查通过"
    fi
}

check_port_range() {
    echo "步骤3: 检查端口范围合规性..."
    
    local invalid_ports=()
    
    while IFS= read -r line; do
        if [[ "$line" =~ SERVER_PORT=([0-9]+) ]]; then
            local port="${BASH_REMATCH[1]}"
            local container_name=$(grep -B 10 "SERVER_PORT=$port" "$DC_FILE" | grep container_name | tail -1 | awk -F': ' '{print $2}')
            
            if [ "$port" -lt 8080 ] || [ "$port" -gt 9001 ]; then
                invalid_ports+=("$container_name 使用端口 $port，超出规范范围(8080-9001)")
            fi
        fi
    done < <(grep "SERVER_PORT=" "$DC_FILE")
    
    if [ ${#invalid_ports[@]} -gt 0 ]; then
        echo "❌ 发现不合规端口:"
        for invalid in "${invalid_ports[@]}"; do
            echo "  - $invalid"
            errors+=("$invalid")
        done
    else
        echo "✅ 端口范围检查通过"
    fi
}

generate_result() {
    echo "步骤4: 生成检查结果..."
    
    if [ ${#errors[@]} -eq 0 ]; then
        echo ""
        echo "========================================"
        echo "✅ 所有端口检查通过"
        echo "========================================"
        echo "{\"status\": \"success\", \"errors\": [], \"message\": \"所有端口检查通过\"}" > "$RESULT_FILE"
        exit 0
    else
        echo ""
        echo "========================================"
        echo "❌ 端口检查失败，共发现 ${#errors[@]} 个问题"
        echo "========================================"
        for error in "${errors[@]}"; do
            echo "  - $error"
        done
        echo "{\"status\": \"failure\", \"errors\": [\"${errors[*]}\"], \"message\": \"端口检查失败\"}" > "$RESULT_FILE"
        exit 1
    fi
}

extract_ports_from_constant
check_port_conflicts
check_port_range
generate_result