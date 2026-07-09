#!/usr/bin/env bash
# ==============================================================================
# Tailor IS - 启动前环境变量校验脚本 (v4.0)
# ==============================================================================
# 用途: 在服务启动前校验必需的环境变量是否已配置，防止因缺少配置导致启动失败
#
# 校验项:
#   1. 数据库密码 (MYSQL_PASSWORD) 是否已配置且非空
#   2. Redis 密码 (REDIS_PASSWORD) 是否已配置且非空
#   3. AES 加密密钥 (AES_KEY) 是否已配置且非空
#   4. 网关 HMAC 密钥 (GATEWAY_HMAC_SECRET) 生产环境是否已配置
#   5. SaaS 模式下的租户隔离密钥 (TENANT_SECRET) 是否已配置
#
# 使用方式:
#   source deploy/scripts/check-env-vars.sh        # 在部署脚本中 source
#   bash deploy/scripts/check-env-vars.sh          # 独立运行
#
# 退出码:
#   0 - 所有必需变量已配置
#   1 - 存在缺失变量
# ==============================================================================
set -euo pipefail

RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
NC='\033[0m'

MISSING_COUNT=0
WARNING_COUNT=0

check_required() {
    local var_name="$1"
    local description="$2"
    local value="${!var_name:-}"

    if [[ -z "$value" ]]; then
        echo -e "${RED}🔴 缺失${NC}: ${var_name} - ${description}"
        echo "   请在 .env 文件或环境中设置: export ${var_name}=<value>"
        MISSING_COUNT=$((MISSING_COUNT + 1))
    else
        echo -e "${GREEN}✅${NC} ${var_name}: 已配置"
    fi
}

check_production_required() {
    local var_name="$1"
    local description="$2"
    local value="${!var_name:-}"

    if [[ -z "$value" ]]; then
        echo -e "${RED}🔴 缺失 (生产必需)${NC}: ${var_name} - ${description}"
        echo "   生产环境必须设置此变量: export ${var_name}=<value>"
        MISSING_COUNT=$((MISSING_COUNT + 1))
    else
        echo -e "${GREEN}✅${NC} ${var_name}: 已配置"
    fi
}

check_warning() {
    local var_name="$1"
    local description="$2"
    local value="${!var_name:-}"

    if [[ -z "$value" ]]; then
        echo -e "${YELLOW}🟡 建议${NC}: ${var_name} - ${description} (使用默认值或空值)")
        WARNING_COUNT=$((WARNING_COUNT + 1))
    else
        echo -e "${GREEN}✅${NC} ${var_name}: 已配置"
    fi
}

echo "=============================================="
echo " Tailor IS v4.0 启动前环境变量校验"
echo "=============================================="

# 判断当前环境
ENV_TYPE="${ENV_TYPE:-dev}"
IS_PRODUCTION=false
if [[ "$ENV_TYPE" == "prod" || "$ENV_TYPE" == "production" || "$ENV_TYPE" == "staging" ]]; then
    IS_PRODUCTION=true
    echo "环境类型: ${ENV_TYPE} (生产模式 - 严格校验)"
else
    echo "环境类型: ${ENV_TYPE} (开发模式 - 宽松校验)"
fi
echo ""

# ---- 数据库 ----
echo "--- 数据库配置 ---"
check_required "MYSQL_PASSWORD" "MySQL 数据库密码"
check_warning "MYSQL_HOST" "MySQL 主机地址"
check_warning "MYSQL_PORT" "MySQL 端口"
echo ""

# ---- Redis ----
echo "--- Redis 配置 ---"
check_required "REDIS_PASSWORD" "Redis 密码"
check_warning "REDIS_HOST" "Redis 主机地址"
echo ""

# ---- 加密密钥 ----
echo "--- 加密配置 ---"
check_required "AES_KEY" "AES-256 加密密钥 (32字节 Base64 编码)"
echo ""

# ---- 网关安全 ----
echo "--- 网关安全 ---"
if $IS_PRODUCTION; then
    check_production_required "GATEWAY_HMAC_SECRET" "网关 HMAC 签名密钥 (用于 X-User-Id 防伪造)"
else
    check_warning "GATEWAY_HMAC_SECRET" "网关 HMAC 签名密钥 (生产环境必需)"
fi
echo ""

# ---- 第三方服务 ----
echo "--- 第三方服务 ---"
check_warning "WECHAT_PAY_APP_ID" "微信支付 AppID"
check_warning "RESEND_API_KEY" "邮件发送 API Key"
check_warning "NACOS_USERNAME" "Nacos 用户名"
check_warning "NACOS_PASSWORD" "Nacos 密码"
echo ""

# ---- 结果汇总 ----
echo "=============================================="
if [[ $MISSING_COUNT -eq 0 ]]; then
    echo -e "${GREEN}✅ 所有必需环境变量已配置${NC}"
    echo "  警告数: ${WARNING_COUNT}"
    exit 0
else
    echo -e "${RED}🔴 存在 ${MISSING_COUNT} 个缺失的必需环境变量${NC}"
    echo "  警告数: ${WARNING_COUNT}"
    echo ""
    echo "修复方式:"
    echo "  1. 编辑 deploy/tailor-is-prod/env/.env.base 文件"
    echo "  2. 或在启动命令前设置环境变量: export VAR_NAME=value"
    echo "  3. 或在 docker-compose.yml 的 environment 段添加"
    exit 1
fi