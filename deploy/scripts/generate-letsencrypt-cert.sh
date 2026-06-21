#!/bin/bash
# ==============================================================================
# Tailor IS - Let's Encrypt 证书申请脚本
# ==============================================================================
# 用法:
#   ./generate-letsencrypt-cert.sh <domain> [email]
#   ./generate-letsencrypt-cert.sh api.tailoris.com admin@tailoris.com
#
# 说明:
#   使用 certbot 申请 Let's Encrypt 免费 SSL 证书，支持 webroot 和 DNS 两种验证方式
# ==============================================================================

set -euo pipefail

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }
log_step()  { echo -e "${BLUE}[STEP]${NC}  $*"; }

# ===================== 参数解析 =====================
DOMAIN=""
EMAIL="admin@tailoris.com"

if [ $# -lt 1 ]; then
    echo "用法: $0 <domain> [email]"
    echo ""
    echo "示例:"
    echo "  $0 api.tailoris.com"
    echo "  $0 api.tailoris.com admin@tailoris.com"
    echo "  $0 'tailoris.com,www.tailoris.com,api.tailoris.com' admin@tailoris.com"
    echo ""
    exit 1
fi

DOMAIN="$1"
shift
if [ $# -ge 1 ]; then
    EMAIL="$1"
fi

# ===================== 配置 =====================
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
SSL_DIR="$PROJECT_DIR/deploy/nginx/ssl"
WEBROOT_PATH="/var/www/certbot"
LETSENCRYPT_LIVE="/etc/letsencrypt/live"

# 将逗号分隔的域名拆分为数组
IFS=',' read -ra DOMAIN_ARRAY <<< "$DOMAIN"
PRIMARY_DOMAIN="${DOMAIN_ARRAY[0]}"

log_info "========================================="
log_info "Tailor IS Let's Encrypt 证书申请工具"
log_info "========================================="
log_info "主域名:     $PRIMARY_DOMAIN"
log_info "所有域名:   ${DOMAIN_ARRAY[*]}"
log_info "联系邮箱:   $EMAIL"
log_info ""

# ===================== 检查 certbot 是否安装 =====================
if ! command -v certbot &> /dev/null; then
    log_error "未检测到 certbot，请先安装。"
    echo ""
    echo "安装方式:"
    echo "  Ubuntu/Debian:  sudo apt update && sudo apt install -y certbot python3-certbot-nginx"
    echo "  CentOS/RHEL:    sudo dnf install -y certbot python3-certbot-nginx"
    echo "  Docker:         docker pull certbot/certbot"
    echo "  macOS:          brew install certbot"
    echo "  snap:           sudo snap install --classic certbot"
    echo ""
    exit 1
fi

log_info "certbot 已安装: $(certbot --version 2>&1 | head -1)"

# ===================== 构建 certbot 参数 =====================
CERTBOT_DOMAINS=""
for d in "${DOMAIN_ARRAY[@]}"; do
    CERTBOT_DOMAINS="$CERTBOT_DOMAINS -d $d"
done

# ===================== 选择验证方式 =====================
echo ""
echo "请选择验证方式:"
echo "  1) Webroot 验证 (推荐 - 需要有公网可访问的 80 端口)"
echo "  2) DNS 手动验证 (适合无公网 80 端口或内网环境)"
echo "  3) DNS 自动验证 (需要 DNS 服务商 API)"
echo "  4) Standalone 验证 (certbot 独立运行，需要 80 端口空闲)"
echo ""
read -rp "请输入选项 [1-4] (默认: 1): " VALIDATION_METHOD
VALIDATION_METHOD="${VALIDATION_METHOD:-1}"

# ===================== 执行证书申请 =====================
case "$VALIDATION_METHOD" in
    1)
        log_step "===== Webroot 验证方式 ====="
        log_info "请确保 Nginx 已配置 /.well-known/acme-challenge/ 路径。"

        # 创建 webroot 目录
        sudo mkdir -p "$WEBROOT_PATH"

        echo ""
        log_info "执行证书申请..."
        sudo certbot certonly --webroot \
            -w "$WEBROOT_PATH" \
            $CERTBOT_DOMAINS \
            --email "$EMAIL" \
            --agree-tos \
            --no-eff-email \
            --preferred-challenges http

        log_info "✅ Webroot 验证完成"
        ;;

    2)
        log_step "===== DNS 手动验证方式 ====="
        echo ""
        log_info "此方式需要在 DNS 服务商处添加 TXT 记录。"
        log_info "certbot 会提示您在 DNS 中添加 _acme-challenge 记录。"
        echo ""

        log_info "执行证书申请..."
        sudo certbot certonly --manual \
            --preferred-challenges dns \
            $CERTBOT_DOMAINS \
            --email "$EMAIL" \
            --agree-tos \
            --no-eff-email

        log_info "✅ DNS 手动验证完成"
        ;;

    3)
        log_step "===== DNS 自动验证方式 ====="
        echo ""
        echo "支持以下 DNS 服务商插件:"
        echo "  - cloudflare:    certbot-dns-cloudflare"
        echo "  - aliyun:        certbot-dns-aliyun"
        echo "  - dnspod:        certbot-dns-dnspod"
        echo "  - route53:       certbot-dns-route53"
        echo "  - digitalocean:  certbot-dns-digitalocean"
        echo ""
        read -rp "请输入 DNS 服务商名称 (如 cloudflare): " DNS_PROVIDER

        case "$DNS_PROVIDER" in
            cloudflare)
                read -rp "请输入 Cloudflare API Token 文件路径: " CF_CREDENTIALS
                sudo certbot certonly \
                    --dns-cloudflare \
                    --dns-cloudflare-credentials "$CF_CREDENTIALS" \
                    $CERTBOT_DOMAINS \
                    --email "$EMAIL" \
                    --agree-tos \
                    --no-eff-email
                ;;
            aliyun)
                read -rp "请输入阿里云 AccessKey 凭证文件路径: " ALI_CREDENTIALS
                sudo certbot certonly \
                    --dns-aliyun \
                    --dns-aliyun-credentials "$ALI_CREDENTIALS" \
                    $CERTBOT_DOMAINS \
                    --email "$EMAIL" \
                    --agree-tos \
                    --no-eff-email
                ;;
            *)
                log_error "不支持的服务商: $DNS_PROVIDER"
                log_info "请使用方式 2 (DNS 手动验证) 或手动运行 certbot"
                exit 1
                ;;
        esac

        log_info "✅ DNS 自动验证完成"
        ;;

    4)
        log_step "===== Standalone 验证方式 ====="
        log_warn "standalone 方式需要暂时停止 Nginx (释放 80 端口)。"
        read -rp "是否继续? [y/N]: " CONFIRM
        if [[ "$CONFIRM" != "y" && "$CONFIRM" != "Y" ]]; then
            log_info "已取消。"
            exit 0
        fi

        log_info "停止 Nginx..."
        sudo systemctl stop nginx 2>/dev/null || true

        log_info "执行证书申请..."
        sudo certbot certonly --standalone \
            $CERTBOT_DOMAINS \
            --email "$EMAIL" \
            --agree-tos \
            --no-eff-email

        log_info "启动 Nginx..."
        sudo systemctl start nginx 2>/dev/null || true

        log_info "✅ Standalone 验证完成"
        ;;
    *)
        log_error "无效选项: $VALIDATION_METHOD"
        exit 1
        ;;
esac

# ===================== 复制证书到项目目录 =====================
log_step "===== 复制证书到项目目录 ====="
CERT_SRC="$LETSENCRYPT_LIVE/$PRIMARY_DOMAIN"

if [ -d "$CERT_SRC" ]; then
    mkdir -p "$SSL_DIR"

    log_info "复制 fullchain.pem..."
    sudo cp "$CERT_SRC/fullchain.pem" "$SSL_DIR/fullchain.pem"

    log_info "复制 privkey.pem..."
    sudo cp "$CERT_SRC/privkey.pem" "$SSL_DIR/privkey.pem"

    # 设置权限
    sudo chmod 644 "$SSL_DIR/fullchain.pem"
    sudo chmod 600 "$SSL_DIR/privkey.pem"

    log_info "证书已复制到: $SSL_DIR"
    ls -la "$SSL_DIR/fullchain.pem" "$SSL_DIR/privkey.pem"
else
    log_warn "证书目录不存在: $CERT_SRC"
    log_info "证书可能已存储在默认位置: $LETSENCRYPT_LIVE"
fi

# ===================== 设置自动续期 =====================
log_step "===== 设置自动续期 ====="

# 检查系统 crontab 中是否已有 certbot 续期任务
if sudo crontab -l 2>/dev/null | grep -q "certbot renew"; then
    log_info "certbot 自动续期任务已存在，跳过。"
else
    log_info "添加 certbot 自动续期 cron 任务 (每天 2:00 AM 检查)..."
    RENEW_SCRIPT="$SCRIPT_DIR/renew-ssl-certs.sh"
    (sudo crontab -l 2>/dev/null; echo "0 2 * * * $RENEW_SCRIPT >> /var/log/certbot-renew.log 2>&1") | sudo crontab -
    log_info "✅ 自动续期任务已添加"
fi

# 测试续期
log_info "测试证书续期..."
sudo certbot renew --dry-run 2>&1 | tail -5

# ===================== 完成 =====================
log_info ""
log_info "========================================="
log_info "✅ Let's Encrypt 证书申请完成!"
log_info "========================================="
echo ""
log_info "证书位置:"
echo "  Let's Encrypt:  $CERT_SRC"
echo "  项目 SSL 目录:  $SSL_DIR"
echo "  fullchain.pem:  $SSL_DIR/fullchain.pem"
echo "  privkey.pem:    $SSL_DIR/privkey.pem"
echo ""

log_info "后续步骤:"
echo "  1. 确保证书路径在 Nginx 配置中正确引用"
echo "  2. 重新加载 Nginx: sudo systemctl reload nginx 或 docker restart tailor-is-nginx"
echo "  3. 验证 HTTPS:   curl -I https://$PRIMARY_DOMAIN"
echo "  4. 在线测试:     https://www.ssllabs.com/ssltest/analyze.html?d=$PRIMARY_DOMAIN"
echo "  5. 查看证书信息: openssl x509 -in $SSL_DIR/fullchain.pem -noout -text"
echo ""
log_info "自动续期: 每天 2:00 AM 自动检查 (certbot 仅在到期前 30 天内续期)"