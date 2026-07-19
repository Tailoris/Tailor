# Secrets 安全配置与管理指南

## 1. 敏感信息清单

### 1.1 数据库相关

| Secret 名称 | 用途 | 环境 | 关联服务 |
|-------------|------|------|----------|
| `MYSQL_ROOT_PASSWORD` | MySQL 根密码 | 所有环境 | 全部服务 |
| `MYSQL_PASSWORD` | 应用数据库密码 | 所有环境 | 全部服务 |
| `MYSQL_USERNAME` | 应用数据库用户名 | 所有环境 | 全部服务 |

### 1.2 缓存相关

| Secret 名称 | 用途 | 环境 | 关联服务 |
|-------------|------|------|----------|
| `REDIS_PASSWORD` | Redis 密码 | 所有环境 | 全部服务 |

### 1.3 消息队列相关

| Secret 名称 | 用途 | 环境 | 关联服务 |
|-------------|------|------|----------|
| `RABBITMQ_PASSWORD` | RabbitMQ 密码 | 所有环境 | 消息相关服务 |

### 1.4 服务注册/配置中心

| Secret 名称 | 用途 | 环境 | 关联服务 |
|-------------|------|------|----------|
| `NACOS_PASSWORD` | Nacos 密码 | 所有环境 | 全部服务 |
| `NACOS_AUTH_TOKEN` | Nacos 认证令牌 | 生产环境 | 全部服务 |

### 1.5 API 密钥

| Secret 名称 | 用途 | 环境 | 关联服务 |
|-------------|------|------|----------|
| `JWT_SECRET` | JWT 签名密钥 | 所有环境 | 网关、认证服务 |
| `AES_KEY` | AES 加密密钥 | 所有环境 | 加密工具类 |
| `GATEWAY_HMAC_SECRET` | 网关 HMAC 签名密钥 | 所有环境 | 核心网关 |

### 1.6 第三方服务

| Secret 名称 | 用途 | 环境 | 关联服务 |
|-------------|------|------|----------|
| `WECHAT_PAY_APP_ID` | 微信支付 AppID | 测试/生产 | 支付服务 |
| `WECHAT_PAY_MCH_ID` | 微信支付商户号 | 测试/生产 | 支付服务 |
| `WECHAT_PAY_API_KEY` | 微信支付 API Key | 测试/生产 | 支付服务 |
| `ALIPAY_APP_ID` | 支付宝 AppID | 测试/生产 | 支付服务 |
| `ALIPAY_PRIVATE_KEY` | 支付宝私钥 | 测试/生产 | 支付服务 |
| `ALIPAY_PUBLIC_KEY` | 支付宝公钥 | 测试/生产 | 支付服务 |

### 1.7 CI/CD 相关

| Secret 名称 | 用途 | 环境 | 关联服务 |
|-------------|------|------|----------|
| `DOCKERHUB_USERNAME` | Docker Hub 用户名 | CI | GitHub Actions |
| `DOCKERHUB_TOKEN` | Docker Hub 访问令牌 | CI | GitHub Actions |
| `STAGING_HOST` | 测试环境主机 | CI | GitHub Actions |
| `STAGING_USER` | 测试环境用户名 | CI | GitHub Actions |
| `STAGING_SSH_KEY` | 测试环境 SSH 密钥 | CI | GitHub Actions |

### 1.8 告警通知

| Secret 名称 | 用途 | 环境 | 关联服务 |
|-------------|------|------|----------|
| `DINGTALK_WEBHOOK` | 钉钉告警 Webhook | 测试/生产 | alert-webhook |
| `DINGTALK_SECRET` | 钉钉告警签名密钥 | 测试/生产 | alert-webhook |
| `FEISHU_WEBHOOK` | 飞书告警 Webhook | 测试/生产 | alert-webhook |
| `WECOM_WEBHOOK` | 企业微信告警 Webhook | 测试/生产 | alert-webhook |

## 2. 安全命名规范

### 2.1 命名格式

```
ENV_SERVICE_NAME_KEY_TYPE
```

**示例：**
- `PROD_DB_MYSQL_PASSWORD` - 生产环境数据库密码
- `STAGING_CACHE_REDIS_PASSWORD` - 测试环境缓存密码
- `CI_DOCKER_TOKEN` - CI 环境 Docker 令牌

### 2.2 命名规则

| 组件 | 说明 | 示例 |
|------|------|------|
| ENV | 环境标识 | PROD, STAGING, TEST, DEV, CI |
| SERVICE | 服务/组件标识 | DB, CACHE, MQ, NACOS, PAY |
| NAME | 具体名称 | MYSQL, REDIS, RABBITMQ |
| KEY | 密钥类型 | PASSWORD, SECRET, TOKEN, KEY |
| TYPE | 附加类型（可选） | API, PRIVATE, PUBLIC |

## 3. 最小权限原则

### 3.1 按环境划分

| 环境 | 权限范围 |
|------|----------|
| DEV | 仅开发环境必要密钥 |
| TEST | 测试环境密钥 + 测试第三方服务密钥 |
| STAGING | 预发环境密钥 + 部分生产第三方服务 |
| PROD | 完整生产密钥 |

### 3.2 按服务划分

| 服务 | 所需密钥 |
|------|----------|
| core-gateway | JWT_SECRET, GATEWAY_HMAC_SECRET, NACOS_PASSWORD |
| user-service | MYSQL_PASSWORD, REDIS_PASSWORD, RABBITMQ_PASSWORD |
| payment-service | MYSQL_PASSWORD, REDIS_PASSWORD, WECHAT_PAY_*, ALIPAY_* |

## 4. Secrets 验证脚本

### 4.1 验证脚本

```bash
#!/bin/bash
# secrets-validator.sh - GitHub Secrets 验证脚本

echo "=== GitHub Secrets 验证 ==="

# 检查必需的 Secrets
REQUIRED_SECRETS=(
    "DOCKERHUB_USERNAME"
    "DOCKERHUB_TOKEN"
    "MYSQL_ROOT_PASSWORD"
    "MYSQL_PASSWORD"
    "REDIS_PASSWORD"
    "RABBITMQ_PASSWORD"
    "JWT_SECRET"
    "AES_KEY"
)

echo ""
echo "--- 检查必需 Secrets ---"
ALL_OK=true

for SECRET in "${REQUIRED_SECRETS[@]}"; do
    if [ -z "${!SECRET}" ]; then
        echo "❌ $SECRET 未设置"
        ALL_OK=false
    else
        echo "✅ $SECRET 已设置"
    fi
done

echo ""
if [ "$ALL_OK" = true ]; then
    echo "✅ 所有必需 Secrets 已配置"
    exit 0
else
    echo "❌ 存在未配置的 Secrets"
    exit 1
fi
```

### 4.2 CI 集成

```yaml
# .github/workflows/secrets-check.yml
name: Secrets Validation

on:
  push:
    branches: [ main, develop ]
  workflow_dispatch:

jobs:
  validate-secrets:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Run Secrets Validator
        run: |
          chmod +x scripts/secrets-validator.sh
          scripts/secrets-validator.sh
        env:
          DOCKERHUB_USERNAME: ${{ secrets.DOCKERHUB_USERNAME }}
          DOCKERHUB_TOKEN: ${{ secrets.DOCKERHUB_TOKEN }}
          MYSQL_ROOT_PASSWORD: ${{ secrets.MYSQL_ROOT_PASSWORD }}
          MYSQL_PASSWORD: ${{ secrets.MYSQL_PASSWORD }}
          REDIS_PASSWORD: ${{ secrets.REDIS_PASSWORD }}
          RABBITMQ_PASSWORD: ${{ secrets.RABBITMQ_PASSWORD }}
          JWT_SECRET: ${{ secrets.JWT_SECRET }}
          AES_KEY: ${{ secrets.AES_KEY }}
```

## 5. 定期轮换机制

### 5.1 轮换周期

| 密钥类型 | 轮换周期 | 负责人 |
|----------|----------|--------|
| 数据库密码 | 90 天 | DevOps |
| 缓存密码 | 90 天 | DevOps |
| API 密钥 | 90 天 | 安全负责人 |
| CI/CD 令牌 | 30 天 | DevOps |
| 第三方服务密钥 | 按服务商要求 | 对应服务负责人 |

### 5.2 轮换流程

```
1. 生成新密钥
2. 在 GitHub Secrets 中更新
3. 在测试环境验证
4. 在生产环境更新
5. 监控服务运行状态
6. 确认无误后删除旧密钥
```

## 6. 安全审计日志

### 6.1 日志记录内容

| 字段 | 说明 |
|------|------|
| timestamp | 操作时间 |
| operator | 操作者 |
| action | 操作类型（创建/更新/删除） |
| secret_name | 密钥名称 |
| environment | 环境 |
| ip_address | IP 地址 |
| reason | 操作原因 |

### 6.2 审计责任

- **审计员**: 每周审查密钥操作日志
- **安全负责人**: 每月进行安全审计
- **管理员**: 定期清理过期密钥

## 7. 紧急响应流程

### 7.1 密钥泄露处理

1. **立即撤销**泄露的密钥
2. **生成新密钥**并更新所有相关配置
3. **通知受影响团队**评估影响范围
4. **记录事件**并进行事后分析
5. **更新安全策略**防止再次发生

### 7.2 联系人

| 角色 | 联系方式 |
|------|----------|
| 安全负责人 | security@tailoris.com |
| DevOps 负责人 | devops@tailoris.com |
| 应急响应团队 | emergency@tailoris.com |
