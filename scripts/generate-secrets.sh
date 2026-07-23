#!/bin/bash
# ==============================================================================
# Tailor IS - 安全密钥生成脚本
# 用途: 为 .env 文件生成安全的随机密钥
# 用法: bash scripts/generate-secrets.sh
# ==============================================================================
set -euo pipefail

echo "=============================================="
echo "  Tailor IS 安全密钥生成工具"
echo "=============================================="
echo ""
echo "生成以下密钥 (请妥善保管，不要提交到版本控制):"
echo ""

# AES-256 密钥 (32 字节十六进制 = 64个字符)
AES_KEY=$(openssl rand -hex 32)
echo "AES_KEY=${AES_KEY}"

# JWT 密钥 (Base64 编码的 32 字节随机数)
JWT_SECRET=$(openssl rand -base64 32)
echo "JWT_SECRET=${JWT_SECRET}"

# HMAC 密钥 (Base64 编码的 32 字节随机数)
GATEWAY_HMAC_SECRET=$(openssl rand -base64 32)
echo "GATEWAY_HMAC_SECRET=${GATEWAY_HMAC_SECRET}"

# Nacos Auth Token (32 字节十六进制)
NACOS_AUTH_TOKEN=$(openssl rand -hex 32)
echo "NACOS_AUTH_TOKEN=${NACOS_AUTH_TOKEN}"

# MySQL 密码 (随机生成 16 位)
MYSQL_PASSWORD=$(openssl rand -base64 12 | tr -d '=+/' | cut -c1-16)
echo "MYSQL_PASSWORD=${MYSQL_PASSWORD}"

# Redis 密码 (随机生成 16 位)
REDIS_PASSWORD=$(openssl rand -base64 12 | tr -d '=+/' | cut -c1-16)
echo "REDIS_PASSWORD=${REDIS_PASSWORD}"

echo ""
echo "=============================================="
echo "  请将以上密钥复制到 deploy/.env 文件中"
echo "  注意: 每次运行都会生成不同的密钥"
echo "=============================================="