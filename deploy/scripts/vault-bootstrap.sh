#!/bin/bash
# ==============================================================================
# Tailor IS - Vault 引导脚本
# ==============================================================================
# 功能:
#   1. 初始化 Vault (如果未初始化)
#   2. 解封 Vault
#   3. 创建 AppRole 认证
#   4. 写入所有 Secrets (MySQL, Redis, RabbitMQ, 支付, AI, 区块链)
#   5. 创建只读 Policy
#   6. 输出 AppRole Role ID 和 Secret ID
#
# 使用方式:
#   chmod +x vault-bootstrap.sh
#   ./vault-bootstrap.sh                          # 交互式引导
#   ./vault-bootstrap.sh --auto                   # 自动模式 (从环境变量读取)
#   ./vault-bootstrap.sh --status                 # 查看 Vault 状态
# ==============================================================================

set -euo pipefail

VAULT_ADDR="${VAULT_ADDR:-http://127.0.0.1:8200}"
VAULT_NAMESPACE="${VAULT_NAMESPACE:-}"
AUTO_MODE=false
STATUS_MODE=false

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }
log_step()  { echo -e "\n${BLUE}[STEP]${NC} $*"; }

# ==============================================================================
# 检查 Vault 状态
# ==============================================================================
check_vault_status() {
    log_step "检查 Vault 状态"

    local response
    response=$(curl -s "${VAULT_ADDR}/v1/sys/seal-status" 2>/dev/null) || {
        log_error "无法连接到 Vault: ${VAULT_ADDR}"
        log_error "请确保 Vault 已启动，或设置 VAULT_ADDR 环境变量"
        exit 1
    }

    local initialized sealed
    initialized=$(echo "$response" | grep -o '"initialized":\(true\|false\)' | cut -d: -f2)
    sealed=$(echo "$response" | grep -o '"sealed":\(true\|false\)' | cut -d: -f2)

    echo "  Vault 地址:    ${VAULT_ADDR}"
    echo "  已初始化:       ${initialized}"
    echo "  已密封:         ${sealed}"

    if [ "$initialized" = "true" ] && [ "$sealed" = "false" ]; then
        echo "  状态:           ${GREEN}运行中 (已解封)${NC}"
    elif [ "$initialized" = "true" ] && [ "$sealed" = "true" ]; then
        echo "  状态:           ${YELLOW}已密封 (需要解封)${NC}"
    else
        echo "  状态:           ${YELLOW}未初始化${NC}"
    fi
}

# ==============================================================================
# 初始化 Vault
# ==============================================================================
init_vault() {
    log_step "初始化 Vault"

    local response
    response=$(curl -s "${VAULT_ADDR}/v1/sys/init" 2>/dev/null)

    local initialized
    initialized=$(echo "$response" | grep -o '"initialized":\(true\|false\)' | cut -d: -f2)

    if [ "$initialized" = "true" ]; then
        log_info "Vault 已经初始化，跳过"
        return 0
    fi

    log_info "正在初始化 Vault (5个密钥分片, 3个阈值)..."
    response=$(curl -s -X PUT "${VAULT_ADDR}/v1/sys/init" \
        -d '{"secret_shares": 5, "secret_threshold": 3}')

    echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response"

    # 保存初始化信息
    echo "$response" > /tmp/vault-init-keys.json
    chmod 600 /tmp/vault-init-keys.json

    log_warn "============================================="
    log_warn "  IMPORTANT: 请安全保存以下信息!"
    log_warn "  初始化信息已保存到: /tmp/vault-init-keys.json"
    log_warn "============================================="

    # 提取 keys
    UNSEAL_KEY_1=$(echo "$response" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['keys'][0])" 2>/dev/null)
    UNSEAL_KEY_2=$(echo "$response" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['keys'][1])" 2>/dev/null)
    UNSEAL_KEY_3=$(echo "$response" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['keys'][2])" 2>/dev/null)
    ROOT_TOKEN=$(echo "$response" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['root_token'])" 2>/dev/null)

    export UNSEAL_KEY_1 UNSEAL_KEY_2 UNSEAL_KEY_3 ROOT_TOKEN
    export VAULT_TOKEN="$ROOT_TOKEN"

    log_info "Vault 初始化完成"
}

# ==============================================================================
# 解封 Vault
# ==============================================================================
unseal_vault() {
    log_step "解封 Vault"

    local response
    response=$(curl -s "${VAULT_ADDR}/v1/sys/seal-status")

    local sealed
    sealed=$(echo "$response" | grep -o '"sealed":\(true\|false\)' | cut -d: -f2)

    if [ "$sealed" = "false" ]; then
        log_info "Vault 已经解封，跳过"
        return 0
    fi

    # 获取解封密钥
    if [ "$AUTO_MODE" = true ]; then
        UNSEAL_KEY_1="${VAULT_UNSEAL_KEY_1:-}"
        UNSEAL_KEY_2="${VAULT_UNSEAL_KEY_2:-}"
        UNSEAL_KEY_3="${VAULT_UNSEAL_KEY_3:-}"
    else
        read -rsp "输入 Unseal Key 1: " UNSEAL_KEY_1
        echo ""
        read -rsp "输入 Unseal Key 2: " UNSEAL_KEY_2
        echo ""
        read -rsp "输入 Unseal Key 3: " UNSEAL_KEY_3
        echo ""
    fi

    if [ -z "$UNSEAL_KEY_1" ] || [ -z "$UNSEAL_KEY_2" ] || [ -z "$UNSEAL_KEY_3" ]; then
        log_error "解封密钥不能为空"
        exit 1
    fi

    curl -s -X PUT "${VAULT_ADDR}/v1/sys/unseal" -d "{\"key\": \"${UNSEAL_KEY_1}\"}" > /dev/null
    curl -s -X PUT "${VAULT_ADDR}/v1/sys/unseal" -d "{\"key\": \"${UNSEAL_KEY_2}\"}" > /dev/null
    curl -s -X PUT "${VAULT_ADDR}/v1/sys/unseal" -d "{\"key\": \"${UNSEAL_KEY_3}\"}" > /dev/null

    # 验证解封状态
    response=$(curl -s "${VAULT_ADDR}/v1/sys/seal-status")
    sealed=$(echo "$response" | grep -o '"sealed":\(true\|false\)' | cut -d: -f2)

    if [ "$sealed" = "false" ]; then
        log_info "Vault 解封成功"
    else
        log_error "Vault 解封失败，请检查密钥是否正确"
        exit 1
    fi
}

# ==============================================================================
# 登录 Vault
# ==============================================================================
login_vault() {
    log_step "登录 Vault"

    if [ -n "${VAULT_TOKEN:-}" ]; then
        log_info "使用已设置的 VAULT_TOKEN"
        return 0
    fi

    if [ "$AUTO_MODE" = true ]; then
        if [ -n "${VAULT_ROOT_TOKEN:-}" ]; then
            export VAULT_TOKEN="$VAULT_ROOT_TOKEN"
        else
            log_error "AUTO 模式需要设置 VAULT_ROOT_TOKEN 环境变量"
            exit 1
        fi
    else
        read -rsp "输入 Vault Root Token: " VAULT_TOKEN
        echo ""
        export VAULT_TOKEN
    fi

    # 验证 token
    if curl -s -H "X-Vault-Token: ${VAULT_TOKEN}" "${VAULT_ADDR}/v1/auth/token/lookup-self" | grep -q "data"; then
        log_info "Vault 登录成功"
    else
        log_error "Vault Token 无效"
        exit 1
    fi
}

# ==============================================================================
# 创建 Secrets Engine & Policy & AppRole
# ==============================================================================
setup_vault_backend() {
    log_step "配置 Vault Backend"

    # 1. 创建 KV v2 Secrets Engine
    log_info "创建 KV v2 Secrets Engine: tailor-is"
    curl -s -X POST "${VAULT_ADDR}/v1/sys/mounts/tailor-is" \
        -H "X-Vault-Token: ${VAULT_TOKEN}" \
        -d '{"type": "kv-v2", "description": "Tailor IS Production Secrets"}' \
        > /dev/null 2>&1 || log_info "Secrets Engine 可能已存在"

    # 2. 创建 AppRole 认证
    log_info "创建 AppRole 认证"
    curl -s -X POST "${VAULT_ADDR}/v1/sys/auth/approle" \
        -H "X-Vault-Token: ${VAULT_TOKEN}" \
        -d '{"type": "approle", "description": "Tailor IS AppRole Authentication"}' \
        > /dev/null 2>&1 || log_info "AppRole 认证可能已存在"

    # 3. 创建只读 Policy
    log_info "创建只读 Policy: tailor-is-readonly"
    curl -s -X PUT "${VAULT_ADDR}/v1/sys/policies/acl/tailor-is-readonly" \
        -H "X-Vault-Token: ${VAULT_TOKEN}" \
        -d '{
          "policy": "path \"tailor-is/data/*\" { capabilities = [\"read\"] } path \"tailor-is/metadata/*\" { capabilities = [\"list\"] }"
        }' > /dev/null 2>&1

    # 4. 创建 AppRole Role
    log_info "创建 AppRole Role: tailor-is"
    curl -s -X POST "${VAULT_ADDR}/v1/auth/approle/role/tailor-is" \
        -H "X-Vault-Token: ${VAULT_TOKEN}" \
        -d '{
          "token_policies": "tailor-is-readonly",
          "token_ttl": "1h",
          "token_max_ttl": "4h",
          "secret_id_ttl": "720h",
          "secret_id_num_uses": 0
        }' > /dev/null 2>&1 || log_info "AppRole Role 可能已存在"

    log_info "Backend 配置完成"
}

# ==============================================================================
# 写入 Secrets
# ==============================================================================
write_secrets() {
    log_step "写入 Secrets"

    local vault_cmd="curl -s -X POST -H \"X-Vault-Token: ${VAULT_TOKEN}\""

    # MySQL
    log_info "写入 MySQL 凭证..."
    $vault_cmd "${VAULT_ADDR}/v1/tailor-is/data/mysql" \
        -d "{\"data\": {\"password\": \"${MYSQL_PASSWORD:-CHANGE_ME_STRONG_PASSWORD}\", \"username\": \"${MYSQL_USERNAME:-root}\", \"host\": \"mysql.tailor-is-prod.svc.cluster.local\", \"port\": \"3306\"}}" > /dev/null

    # Redis
    log_info "写入 Redis 凭证..."
    $vault_cmd "${VAULT_ADDR}/v1/tailor-is/data/redis" \
        -d "{\"data\": {\"password\": \"${REDIS_PASSWORD:-CHANGE_ME_STRONG_PASSWORD}\", \"host\": \"redis.tailor-is-prod.svc.cluster.local\", \"port\": \"6379\"}}" > /dev/null

    # RabbitMQ
    log_info "写入 RabbitMQ 凭证..."
    $vault_cmd "${VAULT_ADDR}/v1/tailor-is/data/rabbitmq" \
        -d "{\"data\": {\"username\": \"${RABBITMQ_USER:-CHANGE_ME}\", \"password\": \"${RABBITMQ_PASSWORD:-CHANGE_ME_STRONG_PASSWORD}\", \"host\": \"rabbitmq.tailor-is-prod.svc.cluster.local\", \"port\": \"5672\"}}" > /dev/null

    # Nacos
    log_info "写入 Nacos 凭证..."
    $vault_cmd "${VAULT_ADDR}/v1/tailor-is/data/nacos" \
        -d "{\"data\": {\"auth_token\": \"${NACOS_AUTH_TOKEN:-CHANGE_ME_RANDOM_64_CHAR_HEX}\", \"identity_value\": \"${NACOS_IDENTITY_VALUE:-CHANGE_ME}\"}}" > /dev/null

    # JWT / AES / HMAC
    log_info "写入安全凭证 (JWT/AES/HMAC)..."
    $vault_cmd "${VAULT_ADDR}/v1/tailor-is/data/security" \
        -d "{\"data\": {\"jwt_secret\": \"${JWT_SECRET:-CHANGE_ME_RANDOM_STRING}\", \"aes_key\": \"${AES_KEY:-CHANGE_ME_RANDOM_32_CHAR}\", \"gateway_hmac_secret\": \"${GATEWAY_HMAC_SECRET:-CHANGE_ME_RANDOM_BASE64_32}\"}}" > /dev/null

    # AI API Key
    log_info "写入 AI API 凭证..."
    $vault_cmd "${VAULT_ADDR}/v1/tailor-is/data/ai" \
        -d "{\"data\": {\"api_key\": \"${AI_API_KEY:-CHANGE_ME_AI_API_KEY}\", \"model_endpoint\": \"${AI_MODEL_ENDPOINT:-https://api.openai.com/v1}\"}}" > /dev/null

    # 支付
    log_info "写入支付凭证..."
    $vault_cmd "${VAULT_ADDR}/v1/tailor-is/data/payment" \
        -d "{\"data\": {\"wechat_app_id\": \"${WECHAT_APP_ID:-CHANGE_ME}\", \"wechat_mch_id\": \"${WECHAT_MCH_ID:-CHANGE_ME}\", \"wechat_api_key\": \"${WECHAT_API_KEY:-CHANGE_ME}\", \"wechat_v3_key\": \"${WECHAT_V3_KEY:-CHANGE_ME}\", \"alipay_app_id\": \"${ALIPAY_APP_ID:-CHANGE_ME}\", \"alipay_private_key\": \"${ALIPAY_PRIVATE_KEY:-CHANGE_ME}\", \"alipay_public_key\": \"${ALIPAY_PUBLIC_KEY:-CHANGE_ME}\"}}" > /dev/null

    # 区块链
    log_info "写入区块链凭证..."
    $vault_cmd "${VAULT_ADDR}/v1/tailor-is/data/blockchain" \
        -d "{\"data\": {\"api_key\": \"${BLOCKCHAIN_API_KEY:-CHANGE_ME}\", \"endpoint\": \"${BLOCKCHAIN_ENDPOINT:-https://blockchain.example.com/api}\"}}" > /dev/null

    # OSS
    log_info "写入 OSS 凭证..."
    $vault_cmd "${VAULT_ADDR}/v1/tailor-is/data/oss" \
        -d "{\"data\": {\"access_key_id\": \"${OSS_ACCESS_KEY_ID:-CHANGE_ME}\", \"access_key_secret\": \"${OSS_ACCESS_KEY_SECRET:-CHANGE_ME}\"}}" > /dev/null

    # SMTP
    log_info "写入 SMTP 凭证..."
    $vault_cmd "${VAULT_ADDR}/v1/tailor-is/data/smtp" \
        -d "{\"data\": {\"username\": \"${SMTP_USERNAME:-CHANGE_ME}\", \"password\": \"${SMTP_PASSWORD:-CHANGE_ME}\", \"host\": \"smtp.tailoris.com\", \"port\": \"587\"}}" > /dev/null

    # Grafana
    log_info "写入 Grafana 凭证..."
    $vault_cmd "${VAULT_ADDR}/v1/tailor-is/data/grafana" \
        -d "{\"data\": {\"password\": \"${GRAFANA_PASSWORD:-CHANGE_ME_STRONG_PASSWORD}\", \"username\": \"admin\"}}" > /dev/null

    log_info "所有 Secrets 写入完成"
}

# ==============================================================================
# 输出 AppRole Credentials
# ==============================================================================
output_approle_credentials() {
    log_step "AppRole Credentials"

    local role_id secret_id

    role_id=$(curl -s -H "X-Vault-Token: ${VAULT_TOKEN}" \
        "${VAULT_ADDR}/v1/auth/approle/role/tailor-is/role-id" \
        | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['role_id'])" 2>/dev/null)

    secret_id=$(curl -s -X POST -H "X-Vault-Token: ${VAULT_TOKEN}" \
        "${VAULT_ADDR}/v1/auth/approle/role/tailor-is/secret-id" \
        | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['secret_id'])" 2>/dev/null)

    echo ""
    echo "============================================="
    echo "  AppRole 认证信息 (用于 K8s 服务)"
    echo "============================================="
    echo "  Role ID:     ${role_id}"
    echo "  Secret ID:   ${secret_id}"
    echo "============================================="
    echo ""
    echo "  K8s Secret 创建命令:"
    echo "  kubectl create secret generic vault-approle \\"
    echo "    --from-literal=role-id=${role_id} \\"
    echo "    --from-literal=secret-id=${secret_id} \\"
    echo "    -n tailor-is-prod"
    echo ""
}

# ==============================================================================
# 主函数
# ==============================================================================
main() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --auto|-a)
                AUTO_MODE=true
                shift
                ;;
            --status|-s)
                STATUS_MODE=true
                shift
                ;;
            --addr)
                VAULT_ADDR="$2"
                shift 2
                ;;
            --help|-h)
                echo "用法: $0 [选项]"
                echo ""
                echo "选项:"
                echo "  --auto, -a          自动模式 (从环境变量读取)"
                echo "  --status, -s        仅查看 Vault 状态"
                echo "  --addr ADDR         指定 Vault 地址"
                echo "  --help, -h          显示帮助"
                echo ""
                echo "环境变量:"
                echo "  VAULT_ADDR          Vault 地址 (默认: http://127.0.0.1:8200)"
                echo "  VAULT_ROOT_TOKEN    自动模式下的 Root Token"
                echo "  VAULT_UNSEAL_KEY_1  自动模式下的解封密钥 1"
                echo "  VAULT_UNSEAL_KEY_2  自动模式下的解封密钥 2"
                echo "  VAULT_UNSEAL_KEY_3  自动模式下的解封密钥 3"
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
    echo "║           Tailor IS - Vault 引导脚本                         ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo ""

    if [ "$STATUS_MODE" = true ]; then
        check_vault_status
        exit 0
    fi

    check_vault_status

    # 检查是否需要初始化
    local response
    response=$(curl -s "${VAULT_ADDR}/v1/sys/seal-status")
    local initialized
    initialized=$(echo "$response" | grep -o '"initialized":\(true\|false\)' | cut -d: -f2)

    if [ "$initialized" != "true" ]; then
        if [ "$AUTO_MODE" = false ]; then
            read -rp "Vault 未初始化，是否初始化? [y/N]: " confirm
            if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
                log_info "退出"
                exit 0
            fi
        fi
        init_vault
    fi

    # 解封
    local sealed
    sealed=$(echo "$response" | grep -o '"sealed":\(true\|false\)' | cut -d: -f2)
    if [ "$sealed" = "true" ]; then
        unseal_vault
    fi

    # 登录
    login_vault

    # 配置 Backend
    setup_vault_backend

    # 写入 Secrets
    if [ "$AUTO_MODE" = false ]; then
        read -rp "是否写入 Secrets? [y/N]: " confirm
        if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
            log_info "跳过 Secrets 写入"
        else
            write_secrets
        fi
    else
        write_secrets
    fi

    # 输出 AppRole 凭证
    output_approle_credentials

    echo ""
    log_info "Vault 引导完成!"
    echo ""
    echo "下一步:"
    echo "  1. 将 Role ID 和 Secret ID 创建为 K8s Secret"
    echo "  2. 在每个微服务 Deployment 中添加 Vault Agent Injector 注解"
    echo "  3. 部署 vault-agent-injector: helm install vault hashicorp/vault"
}

main "$@"