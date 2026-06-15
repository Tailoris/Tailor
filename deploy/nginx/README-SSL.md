# Tailor IS SSL/TLS 证书配置指南

## 概述

本文档说明如何为 Tailor IS 平台获取和配置 SSL/TLS 证书，以实现 HTTPS 安全通信。

## 目录

1. [证书获取方式](#证书获取方式)
2. [Let's Encrypt 免费证书 (推荐)](#lets-encrypt-免费证书-推荐)
3. [商业证书](#商业证书)
4. [证书安装](#证书安装)
5. [证书更新](#证书更新)
6. [故障排查](#故障排查)

---

## 证书获取方式

| 方式 | 成本 | 自动续期 | 适用场景 |
|------|------|----------|----------|
| Let's Encrypt | 免费 | ✅ 支持 | 开发/测试/生产 |
| 商业 CA (DigiCert, GlobalSign) | 付费 | ❌ 手动 | 企业级生产 |
| 云厂商证书 (阿里云, 腾讯云) | 免费/付费 | 部分支持 | 云平台部署 |

---

## Let's Encrypt 免费证书 (推荐)

### 方式一: Certbot (推荐)

```bash
# 1. 安装 Certbot
sudo apt update
sudo apt install certbot python3-certbot-nginx

# 2. 获取证书 (自动配置 Nginx)
sudo certbot --nginx -d api.tailoris.com -d www.tailoris.com

# 3. 手动获取证书 (不修改 Nginx 配置)
sudo certbot certonly --webroot \
  -w /usr/share/nginx/html \
  -d api.tailoris.com

# 4. 测试自动续期
sudo certbot renew --dry-run
```

### 方式二: DNS 验证 (适合内网/无公网)

```bash
# 使用 DNS API 自动验证 (以 Cloudflare 为例)
export CF_API_TOKEN="your_cloudflare_api_token"
sudo certbot certonly --dns-cloudflare \
  -d api.tailoris.com \
  --dns-cloudflare-credentials ~/.secrets/certbot/cloudflare.ini
```

### 方式三: Docker 方式

```bash
docker run --rm \
  -v /etc/letsencrypt:/etc/letsencrypt \
  -v /var/lib/letsencrypt:/var/lib/letsencrypt \
  certbot/certbot certonly --standalone \
  -d api.tailoris.com \
  --email admin@tailoris.com \
  --agree-tos
```

---

## 商业证书

1. 从 CA 提供商购买 SSL 证书
2. 完成域名验证 (DNS/CNAME/文件验证)
3. 下载证书文件 (通常包含):
   - `fullchain.pem` (或 `api.tailoris.com.crt`) - 证书链
   - `privkey.pem` (或 `api.tailoris.com.key`) - 私钥
4. 将文件放到 `/etc/nginx/ssl/` 目录

---

## 证书安装

### 创建 SSL 目录

```bash
sudo mkdir -p /etc/nginx/ssl
sudo chmod 700 /etc/nginx/ssl
```

### 复制证书文件

```bash
# Let's Encrypt 证书路径
sudo cp /etc/letsencrypt/live/api.tailoris.com/fullchain.pem /etc/nginx/ssl/
sudo cp /etc/letsencrypt/live/api.tailoris.com/privkey.pem /etc/nginx/ssl/

# 设置权限
sudo chmod 600 /etc/nginx/ssl/fullchain.pem
sudo chmod 600 /etc/nginx/ssl/privkey.pem
```

### Docker Compose 方式

在 `docker-compose.yml` 中挂载 SSL 证书:

```yaml
nginx:
  volumes:
    - /etc/nginx/ssl:/etc/nginx/ssl:ro
```

### 重启 Nginx

```bash
# Docker
docker restart tailor-is-nginx

# 系统服务
sudo systemctl reload nginx
```

### 验证配置

```bash
# 测试 Nginx 配置
nginx -t

# 检查 SSL 配置
curl -I https://api.tailoris.com

# 在线检查: https://www.ssllabs.com/ssltest/
```

---

## 证书更新

### Let's Encrypt 自动续期

Certbot 会自动创建 systemd timer 或 cron job:

```bash
# 查看续期 timer
sudo systemctl list-timers | grep certbot

# 手动续期
sudo certbot renew

# 续期后重启 Nginx (如果使用 Docker)
sudo certbot renew --deploy-hook "docker restart tailor-is-nginx"
```

### Docker Compose 自动续期

创建续期脚本 `/opt/tailor-is/renew-ssl.sh`:

```bash
#!/bin/bash
certbot renew --quiet --deploy-hook "docker restart tailor-is-nginx"
```

添加 cron 任务:

```bash
# 每周检查一次 (certbot 仅在到期前30天内续期)
0 3 * * 0 /opt/tailor-is/renew-ssl.sh
```

---

## 故障排查

### 证书过期

```bash
# 检查证书过期时间
echo | openssl s_client -servername api.tailoris.com -connect api.tailoris.com:443 2>/dev/null | openssl x509 -noout -dates
```

### SSL 配置测试

```bash
# 测试 SSL 连接
openssl s_client -connect api.tailoris.com:443 -servername api.tailoris.com

# 检查支持的协议
nmap --script ssl-enum-ciphers -p 443 api.tailoris.com
```

### Nginx 错误日志

```bash
# 查看错误日志
docker logs tailor-is-nginx 2>&1 | grep -i ssl
tail -f /var/log/nginx/error.log
```

### 常见错误

| 错误 | 原因 | 解决方案 |
|------|------|----------|
| `SSL_ERROR_BAD_CERT_DOMAIN` | 证书域名不匹配 | 重新申请包含正确域名的证书 |
| `NET::ERR_CERT_DATE_INVALID` | 证书过期 | 续期或重新申请证书 |
| `ssl_error_unsupported_protocol` | 协议不支持 | 启用 TLSv1.2 或 TLSv1.3 |
| `permission denied` | 文件权限问题 | 确保 privkey.pem 权限为 600 |
