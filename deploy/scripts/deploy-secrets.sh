#!/bin/bash
# ==============================================================================
# Tailor IS - Secret 部署脚本
# ==============================================================================
# 从环境变量生成并部署 Kubernetes Secret
#
# 用法:
#   # 方式一: 直接设置环境变量
#   export MYSQL_ROOT_PASSWORD="your-password"
#   export MYSQL_PASSWORD="your-password"
#   # ... 设置所有必需的环境变量
#   ./deploy-secrets.sh
#
#   # 方式二: 从 .env 文件加载
#   source .env.production && ./deploy-secrets.sh
#
#   # 方式三: 仅生成 YAML 不部署 (dry-run)
#   DRY_RUN=true ./deploy-secrets.sh
#
# 环境变量列表 (所有必填):
#   MYSQL_ROOT_PASSWORD      - MySQL root 密码
#   MYSQL_USERNAME           - MySQL 用户名
#   MYSQL_PASSWORD           - MySQL 密码
#   REDIS_PASSWORD           - Redis 密码
#   RABBITMQ_USERNAME        - RabbitMQ 用户名
#   RABBITMQ_PASSWORD        - RabbitMQ 密码
#   RABBITMQ_ERLANG_COOKIE   - RabbitMQ Erlang Cookie
#   NACOS_AUTH_TOKEN         - Nacos 认证 Token
#   NACOS_AUTH_IDENTITY_VALUE - Nacos 身份认证值
#   GATEWAY_HMAC_SECRET      - 网关 HMAC 签名密钥
#   JWT_SECRET               - JWT 密钥
#   AES_KEY                  - AES 加密密钥
#   AI_API_KEY               - AI 服务 API Key
#   AI_MODEL_ENDPOINT        - AI 模型端点
#   WECHAT_PAY_APP_ID        - 微信支付 AppId
#   WECHAT_PAY_MCH_ID        - 微信支付商户号
#   WECHAT_PAY_API_KEY       - 微信支付 API Key
#   WECHAT_PAY_API_V3_KEY    - 微信支付 V3 Key
#   ALIPAY_APP_ID            - 支付宝 AppId
#   ALIPAY_PRIVATE_KEY       - 支付宝私钥
#   ALIPAY_PUBLIC_KEY        - 支付宝公钥
#   BLOCKCHAIN_API_KEY       - 区块链 API Key
#   BLOCKCHAIN_ENDPOINT      - 区块链端点
#   OSS_ACCESS_KEY_ID        - OSS Access Key ID
#   OSS_ACCESS_KEY_SECRET    - OSS Access Key Secret
#   SMTP_USERNAME            - SMTP 用户名
#   SMTP_PASSWORD            - SMTP 密码
#   DINGTALK_WEBHOOK         - 钉钉 Webhook URL
#   DINGTALK_SECRET          - 钉钉 Secret
#   FEISHU_WEBHOOK           - 飞书 Webhook URL
#   WECOM_WEBHOOK            - 企业微信 Webhook URL
#   GRAFANA_PASSWORD         - Grafana 管理员密码
# ==============================================================================

set -euo pipefail

NAMESPACE="${K8S_NAMESPACE:-tailor-is-prod}"
DRY_RUN="${DRY_RUN:-false}"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

# 检查必需的环境变量
check_required_var() {
  local var_name="$1"
  if [ -z "${!var_name:-}" ]; then
    log_error "缺少必需的环境变量: ${var_name}"
    log_error "请设置环境变量后重试，或从 .env 文件加载: source .env.production && $0"
    return 1
  fi
}

log_info "===== Tailor IS Secret 部署 ====="
log_info "命名空间: ${NAMESPACE}"
log_info "模式: $([ "$DRY_RUN" = "true" ] && echo 'dry-run (仅生成 YAML)' || echo '部署')"

# 验证所有必需环境变量
REQUIRED_VARS=(
  MYSQL_ROOT_PASSWORD
  MYSQL_USERNAME
  MYSQL_PASSWORD
  REDIS_PASSWORD
  RABBITMQ_USERNAME
  RABBITMQ_PASSWORD
  RABBITMQ_ERLANG_COOKIE
  NACOS_AUTH_TOKEN
  NACOS_AUTH_IDENTITY_VALUE
  GATEWAY_HMAC_SECRET
  JWT_SECRET
  AES_KEY
  AI_API_KEY
  AI_MODEL_ENDPOINT
  WECHAT_PAY_APP_ID
  WECHAT_PAY_MCH_ID
  WECHAT_PAY_API_KEY
  WECHAT_PAY_API_V3_KEY
  ALIPAY_APP_ID
  ALIPAY_PRIVATE_KEY
  ALIPAY_PUBLIC_KEY
  BLOCKCHAIN_API_KEY
  BLOCKCHAIN_ENDPOINT
  OSS_ACCESS_KEY_ID
  OSS_ACCESS_KEY_SECRET
  SMTP_USERNAME
  SMTP_PASSWORD
  DINGTALK_WEBHOOK
  DINGTALK_SECRET
  FEISHU_WEBHOOK
  WECOM_WEBHOOK
  GRAFANA_PASSWORD
)

HAS_ERRORS=false
for var in "${REQUIRED_VARS[@]}"; do
  if ! check_required_var "$var"; then
    HAS_ERRORS=true
  fi
done

if [ "$HAS_ERRORS" = "true" ]; then
  echo ""
  log_error "环境变量验证失败，请确保所有必需变量已设置。"
  exit 1
fi

log_info "所有必需环境变量已就绪。"

# 构建 kubectl 命令
KUBECTL_ARGS=(
  --from-literal=mysql-root-password="${MYSQL_ROOT_PASSWORD}"
  --from-literal=mysql-username="${MYSQL_USERNAME}"
  --from-literal=mysql-password="${MYSQL_PASSWORD}"
  --from-literal=redis-password="${REDIS_PASSWORD}"
  --from-literal=rabbitmq-username="${RABBITMQ_USERNAME}"
  --from-literal=rabbitmq-password="${RABBITMQ_PASSWORD}"
  --from-literal=rabbitmq-erlang-cookie="${RABBITMQ_ERLANG_COOKIE}"
  --from-literal=nacos-auth-token="${NACOS_AUTH_TOKEN}"
  --from-literal=nacos-auth-identity-value="${NACOS_AUTH_IDENTITY_VALUE}"
  --from-literal=gateway-hmac-secret="${GATEWAY_HMAC_SECRET}"
  --from-literal=jwt-secret="${JWT_SECRET}"
  --from-literal=aes-key="${AES_KEY}"
  --from-literal=ai-api-key="${AI_API_KEY}"
  --from-literal=ai-model-endpoint="${AI_MODEL_ENDPOINT}"
  --from-literal=wechat-pay-app-id="${WECHAT_PAY_APP_ID}"
  --from-literal=wechat-pay-mch-id="${WECHAT_PAY_MCH_ID}"
  --from-literal=wechat-pay-api-key="${WECHAT_PAY_API_KEY}"
  --from-literal=wechat-pay-api-v3-key="${WECHAT_PAY_API_V3_KEY}"
  --from-literal=alipay-app-id="${ALIPAY_APP_ID}"
  --from-literal=alipay-private-key="${ALIPAY_PRIVATE_KEY}"
  --from-literal=alipay-public-key="${ALIPAY_PUBLIC_KEY}"
  --from-literal=blockchain-api-key="${BLOCKCHAIN_API_KEY}"
  --from-literal=blockchain-endpoint="${BLOCKCHAIN_ENDPOINT}"
  --from-literal=oss-access-key-id="${OSS_ACCESS_KEY_ID}"
  --from-literal=oss-access-key-secret="${OSS_ACCESS_KEY_SECRET}"
  --from-literal=smtp-username="${SMTP_USERNAME}"
  --from-literal=smtp-password="${SMTP_PASSWORD}"
  --from-literal=dingtalk-webhook="${DINGTALK_WEBHOOK}"
  --from-literal=dingtalk-secret="${DINGTALK_SECRET}"
  --from-literal=feishu-webhook="${FEISHU_WEBHOOK}"
  --from-literal=wecom-webhook="${WECOM_WEBHOOK}"
  --from-literal=grafana-password="${GRAFANA_PASSWORD}"
)

if [ "$DRY_RUN" = "true" ]; then
  log_info "Dry-run 模式：生成 YAML 输出到 stdout"
  kubectl create secret generic tailor-is-secrets \
    "${KUBECTL_ARGS[@]}" \
    --namespace="${NAMESPACE}" \
    --dry-run=client -o yaml
  log_info "Dry-run 完成。如需部署，请去掉 DRY_RUN=true 环境变量。"
else
  log_info "正在部署 Secret 到命名空间 ${NAMESPACE}..."
  kubectl create secret generic tailor-is-secrets \
    "${KUBECTL_ARGS[@]}" \
    --namespace="${NAMESPACE}" \
    --dry-run=client -o yaml | kubectl apply -f -
  log_info "Secret 部署完成: ${NAMESPACE}/tailor-is-secrets"
fi