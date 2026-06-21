#!/bin/bash
# ==============================================================================
# Tailor IS - 自签名 SSL 证书生成脚本
# ==============================================================================
# 用法: ./generate-ssl-cert.sh
# 输出: deploy/nginx/ssl/fullchain.pem 和 deploy/nginx/ssl/privkey.pem
# 说明: 生成用于 localhost 和 127.0.0.1 的自签名证书，仅用于开发/测试环境
# ==============================================================================

set -euo pipefail

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

# ===================== 配置 =====================
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
SSL_DIR="$PROJECT_DIR/deploy/nginx/ssl"
CERT_FILE="$SSL_DIR/fullchain.pem"
KEY_FILE="$SSL_DIR/privkey.pem"
CONFIG_FILE="$SSL_DIR/openssl.cnf"
DAYS_VALID=365
KEY_SIZE=2048
COUNTRY="CN"
STATE="Guangdong"
CITY="Shenzhen"
ORG="Tailor IS"
OU="Development"
COMMON_NAME="localhost"

log_info "========================================="
log_info "Tailor IS 自签名 SSL 证书生成工具"
log_info "========================================="
log_info "输出目录:   $SSL_DIR"
log_info "证书有效期: ${DAYS_VALID} 天"
log_info "密钥长度:   ${KEY_SIZE} 位"
log_info ""

# 创建 SSL 目录
mkdir -p "$SSL_DIR"

# ===================== 生成 OpenSSL 配置文件 =====================
log_info "生成 OpenSSL 配置文件..."
cat > "$CONFIG_FILE" << EOF
[req]
default_bits        = ${KEY_SIZE}
default_md          = sha256
prompt              = no
distinguished_name  = dn
req_extensions      = req_ext
x509_extensions     = v3_ext

[dn]
C  = ${COUNTRY}
ST = ${STATE}
L  = ${CITY}
O  = ${ORG}
OU = ${OU}
CN = ${COMMON_NAME}

[req_ext]
subjectAltName = @alt_names

[v3_ext]
subjectAltName = @alt_names
basicConstraints       = CA:FALSE
keyUsage               = digitalSignature, keyEncipherment
extendedKeyUsage       = serverAuth, clientAuth
subjectKeyIdentifier   = hash
authorityKeyIdentifier = keyid,issuer

[alt_names]
DNS.1 = localhost
DNS.2 = *.localhost
IP.1  = 127.0.0.1
IP.2  = ::1
EOF

# ===================== 生成私钥和证书 =====================
log_info "生成 ${KEY_SIZE} 位 RSA 私钥..."
openssl genrsa -out "$KEY_FILE" "$KEY_SIZE" 2>/dev/null

log_info "生成自签名证书 (有效期 ${DAYS_VALID} 天)..."
openssl req -new -x509 \
    -key "$KEY_FILE" \
    -out "$CERT_FILE" \
    -days "$DAYS_VALID" \
    -config "$CONFIG_FILE" \
    2>/dev/null

# ===================== 设置权限 =====================
log_info "设置文件权限..."
chmod 600 "$KEY_FILE"
chmod 644 "$CERT_FILE"

# 清理临时配置文件
rm -f "$CONFIG_FILE"

# ===================== 验证证书 =====================
log_info ""
log_info "========================================="
log_info "证书生成完成，正在验证..."
log_info "========================================="

if openssl x509 -in "$CERT_FILE" -noout -text > /dev/null 2>&1; then
    log_info "✅ 证书验证通过"
else
    log_error "证书验证失败!"
    exit 1
fi

# 显示证书信息
SUBJECT=$(openssl x509 -in "$CERT_FILE" -noout -subject | sed 's/^subject=//')
ISSUER=$(openssl x509 -in "$CERT_FILE" -noout -issuer | sed 's/^issuer=//')
EXPIRY=$(openssl x509 -in "$CERT_FILE" -noout -enddate | sed 's/^notAfter=//')
FINGERPRINT=$(openssl x509 -in "$CERT_FILE" -noout -fingerprint -sha256 | sed 's/^SHA256 Fingerprint=//')

log_info "证书信息:"
echo "  主题 (Subject):     $SUBJECT"
echo "  颁发者 (Issuer):    $ISSUER"
echo "  有效期至:           $EXPIRY"
echo "  SHA256 指纹:        $FINGERPRINT"
echo ""
echo "  证书文件:           $CERT_FILE"
echo "  私钥文件:           $KEY_FILE"
echo ""

log_warn "⚠️  这是自签名证书，浏览器会显示安全警告。"
log_warn "⚠️  仅用于开发/测试环境，生产环境请使用 Let's Encrypt 或商业证书。"
log_info ""

# 显示文件列表
ls -la "$CERT_FILE" "$KEY_FILE"