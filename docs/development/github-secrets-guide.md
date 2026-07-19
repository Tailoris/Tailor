# GitHub Secrets 配置指南

## 1. 概述

本指南描述了 Tailor IS 项目 CI/CD 管道所需的 GitHub Secrets 配置。

## 2. 需要配置的 Secrets

| Secret 名称 | 类型 | 说明 | 示例值 |
|-------------|------|------|--------|
| `DOCKERHUB_USERNAME` | 字符串 | Docker Hub 用户名 | `tailoris` |
| `DOCKERHUB_TOKEN` | 字符串 | Docker Hub 访问令牌 | `dckr_pat_xxx` |
| `STAGING_HOST` | 字符串 | 测试环境服务器 IP 或域名 | `192.168.1.100` |
| `STAGING_USER` | 字符串 | 测试环境 SSH 用户名 | `deploy` |
| `STAGING_SSH_KEY` | 字符串 | 测试环境 SSH 私钥 | `-----BEGIN RSA PRIVATE KEY-----...` |
| `NEXUS_USERNAME` | 字符串 | Maven 私服用户名（可选） | `admin` |
| `NEXUS_PASSWORD` | 字符串 | Maven 私服密码（可选） | `password` |

## 3. 配置步骤

### 3.1 获取 Docker Hub 访问令牌

1. 登录 [Docker Hub](https://hub.docker.com/)
2. 进入 Account Settings > Security
3. 点击 "New Access Token"
4. 输入描述并选择权限（推荐只读权限用于 CI）
5. 复制生成的令牌

### 3.2 生成 SSH 密钥对（用于部署）

```bash
# 生成密钥对（使用 ED25519 算法）
ssh-keygen -t ed25519 -C "ci-cd@tailoris.com" -f ~/.ssh/tailoris-ci

# 将公钥添加到目标服务器
ssh-copy-id -i ~/.ssh/tailoris-ci.pub deploy@staging.tailoris.com
```

### 3.3 在 GitHub 中配置 Secrets

1. 进入项目仓库 > Settings > Secrets and variables > Actions
2. 点击 "New repository secret"
3. 逐个添加上表中的 Secrets

## 4. Secrets 使用说明

### 4.1 CI/CD 工作流中的使用

```yaml
# .github/workflows/ci-cd.yml
- name: Login to Docker Hub
  uses: docker/login-action@v3
  with:
    username: ${{ secrets.DOCKERHUB_USERNAME }}
    password: ${{ secrets.DOCKERHUB_TOKEN }}

- name: Deploy to Staging
  uses: appleboy/ssh-action@v1.0.0
  with:
    host: ${{ secrets.STAGING_HOST }}
    username: ${{ secrets.STAGING_USER }}
    key: ${{ secrets.STAGING_SSH_KEY }}
```

## 5. 安全最佳实践

- **最小权限原则**: 为 CI 创建专用用户，仅授予必要权限
- **定期轮换**: 建议每 90 天轮换一次令牌和密钥
- **密钥存储**: 不要将密钥提交到代码仓库
- **审计日志**: 定期检查 GitHub Actions 执行日志

## 6. 测试环境服务器配置

### 6.1 服务器要求

| 项目 | 要求 |
|------|------|
| OS | Ubuntu 22.04 LTS |
| CPU | 4 核以上 |
| 内存 | 8GB 以上 |
| 存储 | 50GB 以上 |

### 6.2 必备软件

```bash
# 安装 Docker
curl -fsSL https://get.docker.com | sh

# 安装 Docker Compose
apt-get update && apt-get install -y docker-compose-plugin

# 创建部署目录
mkdir -p /opt/tailoris/deploy
```

### 6.3 SSH 配置

```bash
# 允许部署用户免密登录
echo "deploy ALL=(ALL) NOPASSWD: /usr/bin/docker" >> /etc/sudoers
```
