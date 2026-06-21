#!/bin/bash
# ==============================================================================
# Tailor IS - SSL 证书自动续期脚本
# ==============================================================================
# 用法:
#   ./renew-ssl-certs.sh              (手动执行)
#   cron: 0 2 * * * /path/to/renew-ssl-certs.sh >> /var/log/certbot-renew.log 2>&1
#
# 说明:
#   1. 执行 certbot renew 检查并续期即将过期的证书
#   2. 将新证书复制到项目 SSL 目录
#   3. 重新加载 Nginx 使新证书生效
#   4. 记录日志
# ==============================================================================

set -euo pipefail

# ===================== 配置 =====================
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
SSL_DIR="$PROJECT_DIR/deploy/nginx/ssl"
LETSENCRYPT_LIVE="/etc/letsencrypt/live"
LOG_FILE="/var/log/certbot-renew.log"
NGINX_CONTAINER_NAME="tailor-is-nginx"
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

# ===================== 日志函数 =====================
log() {
    echo "[$TIMESTAMP] $*" | tee -a "$LOG_FILE"
}

log_separator() {
    echo "================================================================" | tee -a "$LOG_FILE"
}

# ===================== 开始 =====================
log_separator
log "SSL 证书续期任务开始"

# 检查 certbot 是否安装
if ! command -v certbot &> /dev/null; then
    log "[ERROR] certbot 未安装，跳过续期。"
    exit 1
fi

# ===================== 执行 certbot renew =====================
log "执行 certbot renew..."
CERTBOT_OUTPUT=$(sudo certbot renew --quiet 2>&1) || true
CERTBOT_EXIT_CODE=$?

log "certbot 退出码: $CERTBOT_EXIT_CODE"

if [ -n "$CERTBOT_OUTPUT" ]; then
    log "certbot 输出: $CERTBOT_OUTPUT"
fi

# ===================== 检查是否有证书被续期 =====================
RENEWED=false
if [ -d "$LETSENCRYPT_LIVE" ]; then
    for domain_dir in "$LETSENCRYPT_LIVE"/*/; do
        if [ -d "$domain_dir" ]; then
            DOMAIN=$(basename "$domain_dir")
            CERT_FILE="$domain_dir/fullchain.pem"

            if [ -f "$CERT_FILE" ]; then
                # 检查证书是否在最近 1 天内被修改 (即被续期)
                if [ "$(find "$CERT_FILE" -mtime -1 2>/dev/null)" ]; then
                    log "检测到证书已续期: $DOMAIN"
                    RENEWED=true
                fi
            fi
        fi
    done
fi

# ===================== 复制证书到项目目录 =====================
if [ "$RENEWED" = true ]; then
    log "复制续期后的证书到项目目录..."

    mkdir -p "$SSL_DIR"

    for domain_dir in "$LETSENCRYPT_LIVE"/*/; do
        if [ -d "$domain_dir" ]; then
            DOMAIN=$(basename "$domain_dir")
            CERT_FILE="$domain_dir/fullchain.pem"

            if [ -f "$CERT_FILE" ] && [ "$(find "$CERT_FILE" -mtime -1 2>/dev/null)" ]; then
                log "  复制 $DOMAIN 证书..."

                # 复制证书文件
                sudo cp "$domain_dir/fullchain.pem" "$SSL_DIR/fullchain.pem"
                sudo cp "$domain_dir/privkey.pem" "$SSL_DIR/privkey.pem"

                # 设置权限
                sudo chmod 644 "$SSL_DIR/fullchain.pem"
                sudo chmod 600 "$SSL_DIR/privkey.pem"

                log "  ✅ $DOMAIN 证书已复制到 $SSL_DIR"
            fi
        fi
    done
else
    log "没有证书被续期 (证书尚未到期或无需续期)。"
fi

# ===================== 重新加载 Nginx =====================
log "重新加载 Nginx..."

NGINX_RELOADED=false

# 尝试 Docker 方式
if command -v docker &> /dev/null && docker ps --format '{{.Names}}' 2>/dev/null | grep -q "$NGINX_CONTAINER_NAME"; then
    log "  检测到 Docker 容器 $NGINX_CONTAINER_NAME，执行 reload..."
    if docker exec "$NGINX_CONTAINER_NAME" nginx -s reload 2>/dev/null; then
        log "  ✅ Docker Nginx 已重新加载"
        NGINX_RELOADED=true
    else
        log "  [WARN] Docker Nginx reload 失败，尝试 restart..."
        if docker restart "$NGINX_CONTAINER_NAME" 2>/dev/null; then
            log "  ✅ Docker Nginx 已重启"
            NGINX_RELOADED=true
        fi
    fi
fi

# 尝试 systemd 方式
if [ "$NGINX_RELOADED" = false ] && command -v systemctl &> /dev/null; then
    if systemctl is-active --quiet nginx 2>/dev/null; then
        log "  检测到 systemd nginx 服务，执行 reload..."
        if sudo systemctl reload nginx 2>/dev/null; then
            log "  ✅ systemd Nginx 已重新加载"
            NGINX_RELOADED=true
        else
            log "  [WARN] systemd Nginx reload 失败"
        fi
    fi
fi

# 尝试 nginx 命令直接 reload
if [ "$NGINX_RELOADED" = false ] && command -v nginx &> /dev/null; then
    log "  尝试直接执行 nginx -s reload..."
    if sudo nginx -s reload 2>/dev/null; then
        log "  ✅ Nginx 已重新加载"
        NGINX_RELOADED=true
    else
        log "  [WARN] nginx -s reload 失败"
    fi
fi

if [ "$NGINX_RELOADED" = false ]; then
    log "[WARN] 无法重新加载 Nginx，请手动执行: nginx -s reload"
fi

# ===================== 验证 =====================
if [ -f "$SSL_DIR/fullchain.pem" ]; then
    EXPIRY_DATE=$(openssl x509 -in "$SSL_DIR/fullchain.pem" -noout -enddate 2>/dev/null | cut -d= -f2 || echo "未知")
    DAYS_LEFT=$(( ($(date -d "$EXPIRY_DATE" +%s 2>/dev/null || echo 0) - $(date +%s)) / 86400 ))
    log "当前证书有效期至: $EXPIRY_DATE (剩余 ${DAYS_LEFT} 天)"
fi

# ===================== 完成 =====================
log "SSL 证书续期任务完成"
log_separator
echo ""