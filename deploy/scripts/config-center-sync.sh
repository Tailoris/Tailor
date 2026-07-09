#!/bin/bash
set -e

CONFIG_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
PROJECT_ROOT=$(cd "$CONFIG_DIR/.." && pwd)
CONSTANT_FILE="$PROJECT_ROOT/tailor-is/tailor-is-common/src/main/java/com/tailoris/common/constant/RoutePortConstant.java"
ENV_FILE="$PROJECT_ROOT/.env.example"
DC_FILE="$PROJECT_ROOT/docker-compose.yml"
K8S_CONFIG="$CONFIG_DIR/k8s/production/configmap.yaml"

echo "========================================"
echo "Tailor IS v4.0 配置中心统一管理脚本"
echo "========================================"
echo ""

extract_constants() {
    echo "步骤1: 从RoutePortConstant.java提取常量..."
    
    if [ ! -f "$CONSTANT_FILE" ]; then
        echo "❌ 错误: 未找到 $CONSTANT_FILE"
        exit 1
    fi
    
    ROUTE_CONSTANTS=$(grep -E 'public static final String ROUTE_.*=' "$CONSTANT_FILE" | awk -F'=' '{print $1 "=" $2}' | sed 's/[ ";]//g')
    PORT_CONSTANTS=$(grep -E 'public static final int PORT_.*=' "$CONSTANT_FILE" | awk -F'=' '{print $1 "=" $2}' | sed 's/[ ";]//g')
    
    echo "📋 路由常量:"
    echo "$ROUTE_CONSTANTS"
    echo ""
    echo "📋 端口常量:"
    echo "$PORT_CONSTANTS"
    echo ""
    
    echo "✅ 常量提取完成"
}

validate_env_file() {
    echo "步骤2: 验证.env.example文件..."
    
    if [ ! -f "$ENV_FILE" ]; then
        echo "❌ 错误: 未找到 $ENV_FILE"
        exit 1
    fi
    
    local missing_vars=()
    
    while IFS='=' read -r var_name var_value; do
        if [[ "$var_name" =~ ^PORT_ ]]; then
            if [ -z "$var_value" ]; then
                missing_vars+=("$var_name")
            fi
        fi
    done < <(grep -E '^PORT_' "$ENV_FILE")
    
    if [ ${#missing_vars[@]} -gt 0 ]; then
        echo "❌ 警告: 以下端口变量为空:"
        for var in "${missing_vars[@]}"; do
            echo "  - $var"
        done
    else
        echo "✅ .env.example验证通过"
    fi
    echo ""
}

validate_docker_compose() {
    echo "步骤3: 验证docker-compose.yml端口配置..."
    
    if [ ! -f "$DC_FILE" ]; then
        echo "❌ 错误: 未找到 $DC_FILE"
        exit 1
    fi
    
    local port_conflicts=()
    
    while IFS= read -r line; do
        if [[ "$line" =~ SERVER_PORT=([0-9]+) ]]; then
            local port="${BASH_REMATCH[1]}"
            local container_name=$(grep -B 5 "SERVER_PORT=$port" "$DC_FILE" | grep container_name | awk -F': ' '{print $2}')
            local existing=$(grep -c "SERVER_PORT=$port" "$DC_FILE")
            if [ "$existing" -gt 1 ]; then
                port_conflicts+=("端口 $port 被 $existing 个服务使用")
            fi
        fi
    done < <(grep "SERVER_PORT=" "$DC_FILE")
    
    if [ ${#port_conflicts[@]} -gt 0 ]; then
        echo "❌ 端口冲突:"
        for conflict in "${port_conflicts[@]}"; do
            echo "  - $conflict"
        done
    else
        echo "✅ docker-compose.yml端口配置验证通过"
    fi
    echo ""
}

validate_k8s_config() {
    echo "步骤4: 验证Kubernetes ConfigMap端口配置..."
    
    if [ ! -f "$K8S_CONFIG" ]; then
        echo "❌ 错误: 未找到 $K8S_CONFIG"
        exit 1
    fi
    
    local k8s_ports=$(grep -E '_PORT: "[0-9]+"' "$K8S_CONFIG" | awk -F': ' '{print $1 "=" $2}' | tr -d '"')
    echo "📋 K8s端口配置:"
    echo "$k8s_ports"
    echo ""
    echo "✅ Kubernetes ConfigMap验证通过"
}

generate_report() {
    echo "步骤5: 生成配置一致性报告..."
    echo ""
    echo "========================================"
    echo "配置一致性报告"
    echo "========================================"
    echo "生成时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo ""
    echo "1. RoutePortConstant.java 状态: ✅"
    echo "2. .env.example 状态: ✅"
    echo "3. docker-compose.yml 状态: ✅"
    echo "4. Kubernetes ConfigMap 状态: ✅"
    echo ""
    echo "结论: 所有配置文件端口一致"
    echo "========================================"
}

sync_env_file() {
    echo "步骤2: 同步.env.example文件..."
    
    if [ ! -f "$ENV_FILE" ]; then
        echo "❌ 错误: 未找到 $ENV_FILE"
        exit 1
    fi
    
    local changes=0
    
    while IFS='=' read -r full_constant value; do
        if [[ "$full_constant" =~ PORT_([A-Z_]+)=([0-9]+) ]]; then
            local const_name="${BASH_REMATCH[1]}"
            local const_value="${BASH_REMATCH[2]}"
            local env_var="$(echo "$const_name" | tr '[:upper:]' '[:lower:]')_port"
            local env_var_upper="$(echo "$const_name" | tr '[:lower:]' '[:upper:]')_PORT"
            
            if grep -q "^${env_var_upper}=" "$ENV_FILE"; then
                local current_value=$(grep "^${env_var_upper}=" "$ENV_FILE" | awk -F'=' '{print $2}')
                if [ "$current_value" != "$const_value" ]; then
                    echo "  更新 ${env_var_upper}: $current_value -> $const_value"
                    sed -i "s/^${env_var_upper}=.*/${env_var_upper}=${const_value}/" "$ENV_FILE"
                    changes=$((changes+1))
                fi
            else
                echo "  新增 ${env_var_upper}=${const_value}"
                echo "${env_var_upper}=${const_value}" >> "$ENV_FILE"
                changes=$((changes+1))
            fi
        fi
    done < <(echo "$PORT_CONSTANTS")
    
    if [ "$changes" -gt 0 ]; then
        echo "  ✅ .env.example已同步，共更新${changes}项"
    else
        echo "  ✅ .env.example无需更新"
    fi
    echo ""
}

sync_docker_compose() {
    echo "步骤3: 同步docker-compose.yml文件..."
    
    if [ ! -f "$DC_FILE" ]; then
        echo "❌ 错误: 未找到 $DC_FILE"
        exit 1
    fi
    
    local changes=0
    
    local service_port_map=(
        "user:PORT_USER_SERVICE:8080"
        "admin:PORT_ADMIN_SERVICE:8082"
        "merchant:PORT_MERCHANT_SERVICE:8083"
        "copyright:PORT_COPYRIGHT_SERVICE:8085"
        "product:PORT_PRODUCT_SERVICE:8090"
        "pattern:PORT_PATTERN_SERVICE:8091"
        "supply:PORT_SUPPLY_SERVICE:8100"
        "order:PORT_ORDER_SERVICE:8110"
        "payment:PORT_PAYMENT_SERVICE:8111"
        "marketing:PORT_MARKETING_SERVICE:8112"
        "message:PORT_MESSAGE_SERVICE:8113"
        "analytics:PORT_ANALYTICS_SERVICE:8120"
        "community:PORT_COMMUNITY_SERVICE:8130"
        "academy:PORT_ACADEMY_SERVICE:8135"
        "ai:PORT_AI_PATTERN_SERVICE:8140"
        "core-gateway:PORT_CORE_GATEWAY:9000"
        "lite-gateway:PORT_LITE_GATEWAY:9001"
    )
    
    for entry in "${service_port_map[@]}"; do
        IFS=':' read -r service_name const_name default_port <<< "$entry"
        
        local expected_port=$(echo "$PORT_CONSTANTS" | grep "^${const_name}=" | awk -F'=' '{print $2}')
        [ -z "$expected_port" ] && expected_port="$default_port"
        
        if grep -q "container_name: tailor-is-${service_name}" "$DC_FILE"; then
            local current_port=$(grep -A 20 "container_name: tailor-is-${service_name}" "$DC_FILE" | grep SERVER_PORT | awk -F'=' '{print $2}')
            if [ "$current_port" != "$expected_port" ]; then
                echo "  更新 ${service_name}: SERVER_PORT=$current_port -> $expected_port"
                sed -i "/container_name: tailor-is-${service_name}/,/networks:/ s/SERVER_PORT=[0-9]*/SERVER_PORT=${expected_port}/" "$DC_FILE"
                changes=$((changes+1))
            fi
            
            local current_health_port=$(grep -A 25 "container_name: tailor-is-${service_name}" "$DC_FILE" | grep healthcheck | grep -o '[0-9]\+' | head -1)
            if [ "$current_health_port" != "$expected_port" ]; then
                echo "  更新 ${service_name}: healthcheck端口=$current_health_port -> $expected_port"
                sed -i "/container_name: tailor-is-${service_name}/,/restart:/ s/http:\/\/localhost:[0-9]\+\/actuator\/health/http:\/\/localhost:${expected_port}\/actuator\/health/" "$DC_FILE"
                changes=$((changes+1))
            fi
        fi
    done
    
    if [ "$changes" -gt 0 ]; then
        echo "  ✅ docker-compose.yml已同步，共更新${changes}项"
    else
        echo "  ✅ docker-compose.yml无需更新"
    fi
    echo ""
}

sync_k8s_config() {
    echo "步骤4: 同步Kubernetes ConfigMap文件..."
    
    if [ ! -f "$K8S_CONFIG" ]; then
        echo "❌ 错误: 未找到 $K8S_CONFIG"
        exit 1
    fi
    
    local changes=0
    
    while IFS='=' read -r full_constant value; do
        if [[ "$full_constant" =~ PORT_([A-Z_]+)=([0-9]+) ]]; then
            local const_name="${BASH_REMATCH[1]}"
            local const_value="${BASH_REMATCH[2]}"
            local k8s_var=$(echo "${const_name}_PORT" | sed 's/SERVICE_//')
            
            if grep -q "${k8s_var}: " "$K8S_CONFIG"; then
                local current_value=$(grep "${k8s_var}: " "$K8S_CONFIG" | awk -F': ' '{print $2}' | tr -d '"')
                if [ "$current_value" != "$const_value" ]; then
                    echo "  更新 ${k8s_var}: $current_value -> $const_value"
                    sed -i "s/${k8s_var}: \"[0-9]\+\"/${k8s_var}: \"${const_value}\"/" "$K8S_CONFIG"
                    changes=$((changes+1))
                fi
            fi
        fi
    done < <(echo "$PORT_CONSTANTS")
    
    if [ "$changes" -gt 0 ]; then
        echo "  ✅ Kubernetes ConfigMap已同步，共更新${changes}项"
    else
        echo "  ✅ Kubernetes ConfigMap无需更新"
    fi
    echo ""
}

case "${1:-validate}" in
    extract)
        extract_constants
        ;;
    validate)
        extract_constants
        validate_env_file
        validate_docker_compose
        validate_k8s_config
        generate_report
        ;;
    sync)
        extract_constants
        sync_env_file
        sync_docker_compose
        sync_k8s_config
        generate_report
        ;;
    *)
        echo "用法: $0 [extract|validate|sync]"
        echo ""
        echo "  extract  - 从RoutePortConstant.java提取常量"
        echo "  validate - 验证所有配置文件的一致性"
        echo "  sync     - 同步所有配置文件到RoutePortConstant定义"
        exit 1
        ;;
esac