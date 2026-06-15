# 部署指南

本文档提供 Tailor IS（裁智云）的部署指引。

## 部署方式

项目支持以下部署方式：

1. **Docker Compose** — 开发/测试环境推荐，见项目根目录 `docker-compose.yml`
2. **Kubernetes** — 生产环境推荐，见 `tailor-is/deploy/k8s/` 目录
3. **1Panel 部署** — 详见 [1PANEL-DEPLOYMENT.md](1PANEL-DEPLOYMENT.md)

## Docker Compose 快速部署

```bash
# 1. 配置环境变量
cp .env.example .env
# 编辑 .env，填写数据库密码、Redis 密码等

# 2. 启动服务
docker-compose up -d

# 3. 验证部署
docker-compose ps
```

## Kubernetes 部署

```bash
kubectl apply -f tailor-is/deploy/k8s/namespace.yaml
kubectl apply -f tailor-is/deploy/k8s/configmap.yaml
kubectl apply -f tailor-is/deploy/k8s/secret.yaml
kubectl apply -f tailor-is/deploy/k8s/core-services/
kubectl apply -f tailor-is/deploy/k8s/lite-services/
kubectl apply -f tailor-is/deploy/k8s/ingress.yaml
```

详细步骤请参考 [K8S-DEPLOYMENT-GUIDE.md](K8S-DEPLOYMENT-GUIDE.md)。

## 服务依赖

| 服务 | 端口 | 说明 |
|------|------|------|
| Nacos | 8848 | 注册中心与配置中心 |
| Gateway | 8080 | API 网关 |
| MySQL | 3306 | 主数据库 |
| Redis | 6379 | 缓存 |
| RabbitMQ | 5672 / 15672 | 消息队列 |

## 注意事项

- 生产环境务必修改默认密码
- 启用 HTTPS（参考 `deploy/nginx/ssl.conf`）
- 配置数据库定期备份（参考 `deploy/database/backup.sh`）
