#!/bin/bash
# SSL证书有效期检查脚本
# 用法: check-ssl-cert.sh [domain.crt]

CERT_FILE="${1:-/home/tailor/Tailoris/deploy/nginx/ssl/server.crt}"
WARN_DAYS=30
CRITICAL_DAYS=7

if [ ! -f "$CERT_FILE" ]; then
    echo "[ERROR] 证书文件不存在: $CERT_FILE"
    exit 2
fi

EXPIRY_DATE=$(openssl x509 -in "$CERT_FILE" -noout -enddate | cut -d= -f2)
EXPIRY_EPOCH=$(date -d "$EXPIRY_DATE" +%s 2>/dev/null || echo 0)
NOW_EPOCH=$(date +%s)
DAYS_LEFT=$(( (EXPIRY_EPOCH - NOW_EPOCH) / 86400))

echo "证书文件: $CERT_FILE"
echo "有效期至: $EXPIRY_DATE"
echo "剩余天数: $DAYS_LEFT 天"

if [ $DAYS_LEFT -le $CRITICAL_DAYS ]; then
    echo "[CRITICAL] 证书即将过期! 立即更新!"
    exit 2
elif [ $DAYS_LEFT -le $WARN_DAYS ]; then
    echo "[WARNING] 证书30天内到期, 准备更新"
    exit 1
else
    echo "[OK] 证书状态正常"
    exit 0
fi
