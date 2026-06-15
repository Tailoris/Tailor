# 1Panel 部署指南

本文档介绍如何使用 1Panel 面板部署 Tailor IS（裁智云）项目。

## 前置条件

- 已安装 1Panel（推荐最新版本）
- 服务器配置：4 核 8G 以上
- 域名已解析到服务器 IP

## 部署步骤

### 1. 准备环境

登录 1Panel 面板，在「应用商店」中安装以下应用：

| 应用 | 用途 |
|------|------|
| MySQL | 主数据库 |
| Redis | 缓存服务 |
| RabbitMQ | 消息队列 |
| Nginx | 反向代理 |

### 2. 创建数据库

在 1Panel「数据库」中创建项目所需的数据库：

- `tailor_is_user` — 用户服务
- `tailor_is_merchant` — 商户服务
- `tailor_is_product` — 商品服务
- `tailor_is_order` — 订单服务
- `tailor_is_payment` — 支付服务
- `tailor_is_community` — 社区服务
- `tailor_is_marketing` — 营销服务
- `tailor_is_pattern` — 纸样定制
- `tailor_is_copyright` — 版权服务
- `tailor_is_message` — 消息服务
- `tailor_is_supply` — 供应链服务

导入 SQL 初始化脚本（`tailor-is/sql/` 目录下的 `.sql` 文件）。

### 3. 配置 Nacos

在 1Panel「容器」中部署 Nacos：

```bash
docker run -d \
  --name nacos \
  -p 8848:8848 \
  -p 9848:9848 \
  -e MODE=standalone \
  -e SPRING_DATASOURCE_PLATFORM=mysql \
  -e MYSQL_SERVICE_HOST=mysql.internal \
  -e MYSQL_SERVICE_DB_NAME=nacos \
  -e MYSQL_SERVICE_USER=root \
  -e MYSQL_SERVICE_PASSWORD=yourpassword \
  nacos/nacos-server:latest
```

### 4. 部署后端服务

将后端 JAR 包上传至服务器，通过 1Panel「容器」创建 Docker 容器：

```bash
# 以 Gateway 为例
docker run -d \
  --name tailor-is-gateway \
  -p 8080:8080 \
  -e NACOS_ADDR=nacos.internal:8848 \
  -e MYSQL_HOST=mysql.internal \
  -e REDIS_HOST=redis.internal \
  -v /data/tailor-is/gateway/logs:/app/logs \
  tailor-is-gateway:1.0.0
```

按依赖顺序依次部署：
1. Gateway（网关）
2. User Service（用户服务）
3. Product Service（商品服务）
4. Order Service（订单服务）
5. Payment Service（支付服务）
6. 其他业务服务

### 5. 部署前端

在 1Panel「网站」中添加静态网站：

- PC 商城：将 `tailor-is-frontend/pc-mall/dist/` 上传至网站目录
- 商户后台：将 `tailor-is-frontend/merchant-admin/dist/` 上传至网站目录
- 移动端 H5：将 `tailor-is-frontend/mobile-app/dist/` 上传至网站目录

### 6. 配置 Nginx 反向代理

在 1Panel「网站」中配置反向代理：

```nginx
# API 代理
location /api/ {
    proxy_pass http://127.0.0.1:8080/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}

# WebSocket 代理（IM 消息）
location /ws/ {
    proxy_pass http://127.0.0.1:8080/ws/;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
}
```

### 7. 配置 SSL 证书

在 1Panel「网站」→ SSL 中申请或上传证书，启用 HTTPS。

### 8. 配置定时任务

在 1Panel「计划任务」中配置：

- 数据库每日备份（参考 `deploy/database/backup.sh`）
- 日志定期清理
- 健康检查脚本

## 常用操作

### 查看服务日志

```bash
# 通过 1Panel「容器」界面查看
# 或使用命令行
docker logs -f tailor-is-gateway --tail 100
```

### 服务重启

```bash
docker restart tailor-is-gateway
```

### 更新服务

```bash
# 1. 停止旧容器
docker stop tailor-is-gateway && docker rm tailor-is-gateway

# 2. 拉取新镜像或上传新 JAR

# 3. 启动新容器
docker run -d --name tailor-is-gateway ...
```

## 故障排查

| 问题 | 排查方法 |
|------|----------|
| 服务无法启动 | 检查 `docker logs` 查看错误日志 |
| 无法连接 Nacos | 检查 Nacos 是否运行，网络是否通畅 |
| 数据库连接失败 | 检查 MySQL 容器状态、密码、数据库是否已创建 |
| 前端页面白屏 | 检查 Nginx 配置、API 地址是否正确 |

## 注意事项

- 生产环境务必修改所有默认密码
- 建议配置数据库主从复制提高可用性
- 定期更新 1Panel 面板和容器镜像
- 配置安全组规则，仅开放必要端口
