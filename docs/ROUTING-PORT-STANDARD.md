# Tailor IS 路由与端口标准化规范

**文档编号**: TAILOR-IS-ROUTING-PORT-STD-2026-0611  
**版本**: v1.0  
**适用范围**: 生产环境 + 预发布环境  
**更新日期**: 2026-06-11

---

## 一、端口分配总表

### 1.1 核心服务端口（8000-8199）

| 端口 | 服务名称 | 模块 | 协议 | 对外开放 | 内部网段 |
|-----|---------|-----|-----|---------|---------|
| **8080** | Core Gateway 核心网关 | tailor-is-core-gateway | HTTP | ❌ (Nginx反代) | 172.18.0.0/24 |
| **8081** | Lite Gateway 轻量网关 | tailor-is-lite-gateway | HTTP | ❌ (Nginx反代) | 172.18.0.0/24 |
| **8101** | 用户服务 User Service | tailor-is-user | HTTP | ❌ | 172.18.0.0/24 |
| **8102** | 商品服务 Product Service | tailor-is-product | HTTP | ❌ | 172.18.0.0/24 |
| **8103** | 订单服务 Order Service | tailor-is-order | HTTP | ❌ | 172.18.0.0/24 |
| **8104** | 支付服务 Payment Service | tailor-is-payment | HTTP | ❌ | 172.18.0.0/24 |
| **8105** | 营销服务 Marketing Service | tailor-is-marketing | HTTP | ❌ | 172.18.0.0/24 |
| **8106** | AI服务 AI Service | tailor-is-ai | HTTP | ❌ | 172.18.0.0/24 |
| **8107** | 版权服务 Copyright Service | tailor-is-copyright | HTTP | ❌ | 172.18.0.0/24 |
| **8108** | 社区服务 Community Service | tailor-is-community | HTTP | ❌ | 172.18.0.0/24 |
| **8109** | 供应链服务 Supply Service | tailor-is-supply | HTTP | ❌ | 172.18.0.0/24 |
| **8110** | 消息服务 Message Service | tailor-is-message | HTTP | ❌ | 172.18.0.0/24 |
| **8111** | 商户服务 Merchant Service | tailor-is-merchant | HTTP | ❌ | 172.18.0.0/24 |
| **8112** | 学院服务 Academy Service | tailor-is-academy | HTTP | ❌ | 172.18.0.0/24 |
| **8113** | 数据分析 Analytics Service | tailor-is-analytics | HTTP | ❌ | 172.18.0.0/24 |
| **8114** | 图案设计 Pattern Service | tailor-is-pattern | HTTP | ❌ | 172.18.0.0/24 |
| **8115** | 管理后台 Admin Service | tailor-is-admin | HTTP | ❌ | 172.18.0.0/24 |

### 1.2 前端与API聚合层（3000-4999）

| 端口 | 服务名称 | 模块 | 协议 | 对外开放 |
|-----|---------|-----|-----|---------|
| **4000** | GraphQL API Gateway | graphql-gateway | HTTP | ❌ (Nginx反代) |
| **3001** | PC商城 SSR服务 | pc-mall (SSR) | HTTP | ❌ (Nginx反代) |
| **3002** | 商户管理后台 | merchant-admin | HTTP | ❌ (Nginx反代) |
| **3003** | 平台管理后台 | platform-admin | HTTP | ❌ (Nginx反代) |
| **3004** | 移动端H5 (含H5 SSR) | mobile-app H5 | HTTP | ❌ (Nginx反代) |

### 1.3 基础设施端口

| 端口 | 服务名称 | 用途 | 协议 | 对外开放 |
|-----|---------|-----|-----|---------|
| **3306** | MySQL | 关系型数据库 | TCP | ❌ (仅容器网络) |
| **6379** | Redis | 缓存/消息 | TCP | ❌ (仅容器网络) |
| **5672** | RabbitMQ | 消息队列 (AMQP) | TCP | ❌ (仅容器网络) |
| **15672** | RabbitMQ | 管理控制台 | HTTP | ❌ (需1Panel面板访问) |
| **8848** | Nacos | 配置中心/注册中心 | HTTP/gRPC | ❌ (需安全入口) |
| **9848** | Nacos | gRPC通信端口 | TCP | ❌ |
| **9090** | Prometheus | 指标采集 | HTTP | ❌ (仅内网) |
| **3000** | Grafana | 监控可视化 | HTTP | ❌ (仅内网+密码) |
| **11336** | 1Panel | 服务器管理面板 | HTTP | ✅ (IP白名单+安全入口) |

### 1.4 HTTP标准端口

| 端口 | 服务 | 协议 | 说明 |
|-----|-----|-----|-----|
| **80** | Nginx HTTP入口 | HTTP | 强制跳转至HTTPS (301) |
| **443** | Nginx HTTPS入口 | HTTPS (TLS 1.2+) | 唯一对外HTTPS入口 |

---

## 二、路由规划标准

### 2.1 API路由设计规范

#### 2.1.1 URL结构

```
https://api.tailoris.com/{版本}/{业务域}/{资源}/{操作?}
       ↑            ↑        ↑         ↑
       |            |        |         └── 可选操作: list/detail/create/update/delete
       |            |        └──────────── 资源名称: 复数形式 (users/products/orders)
       |            └──────────────────── 业务域: user/product/order/payment/community
       └───────────────────────────────── 版本号: v1/v2/v3
```

#### 2.1.2 路由表（生产环境）

| 路由前缀 | 转发目标 | 说明 | 鉴权要求 |
|---------|---------|-----|---------|
| `/api/v1/user/**` | `http://core-gateway:8080/user-service` | 用户中心API | Bearer Token |
| `/api/v1/product/**` | `http://core-gateway:8080/product-service` | 商品服务API | 部分公开 |
| `/api/v1/order/**` | `http://core-gateway:8080/order-service` | 订单服务API | Bearer Token |
| `/api/v1/payment/**` | `http://core-gateway:8080/payment-service` | 支付服务API | Bearer Token |
| `/api/v1/marketing/**` | `http://core-gateway:8080/marketing-service` | 营销服务API | 部分公开 |
| `/api/v1/merchant/**` | `http://core-gateway:8080/merchant-service` | 商户服务API | Bearer Token |
| `/api/v1/ai/**` | `http://core-gateway:8080/ai-service` | AI服务API | Bearer Token |
| `/api/v1/copyright/**` | `http://lite-gateway:8081/copyright-service` | 版权服务API | Bearer Token |
| `/api/v1/community/**` | `http://lite-gateway:8081/community-service` | 社区服务API | 部分公开 |
| `/api/v1/supply/**` | `http://lite-gateway:8081/supply-service` | 供应链API | Bearer Token |
| `/api/v1/message/**` | `http://core-gateway:8080/message-service` | 消息服务API | Bearer Token |
| `/api/v1/academy/**` | `http://lite-gateway:8081/academy-service` | 学院服务API | Bearer Token |
| `/graphql` | `http://graphql-gateway:4000/graphql` | GraphQL聚合层 | Bearer Token |
| `/health/**` | `各服务` | 健康检查端点 | 公开 |
| `/actuator/**` | `各服务` | Spring Boot监控 | ❌ 禁止外网 |

#### 2.1.3 前端页面路由

| 域名 | 服务 | 路由前缀 | 说明 |
|-----|-----|---------|-----|
| `www.tailoris.com` | pc-mall | `/` | PC商城首页 |
| `www.tailoris.com` | pc-mall | `/products/**` | 商品列表/详情 |
| `www.tailoris.com` | pc-mall | `/cart` | 购物车 |
| `www.tailoris.com` | pc-mall | `/order/**` | 订单管理 |
| `www.tailoris.com` | pc-mall | `/login` | 登录/注册 |
| `www.tailoris.com` | pc-mall | `/user/**` | 用户中心 |
| `merchant.tailoris.com` | merchant-admin | `/**` | 商户后台管理 |
| `admin.tailoris.com` | platform-admin | `/**` | 平台运营后台 |
| `m.tailoris.com` | mobile-app H5 | `/**` | 移动端H5 |
| `api.tailoris.com` | Nginx | `/api/**` | REST API入口 |
| `graphql.tailoris.com` | Nginx | `/graphql` | GraphQL API入口 |

### 2.2 内部路由（容器间通信）

| 服务内部地址 | 协议 | 认证方式 |
|-------------|-----|---------|
| `mysql:3306/tailor_is` | MySQL TCP | 用户名+密码 |
| `redis:6379` (DB 0-5) | Redis TCP | 密码认证 |
| `rabbitmq:5672` | AMQP | 用户名+密码 |
| `nacos:8848` | HTTP | 身份密钥+Token |
| `core-gateway:8080` | HTTP | 内部JWT |
| `lite-gateway:8081` | HTTP | 内部JWT |

---

## 三、网络安全策略

### 3.1 防火墙规则（ufw/iptables）

```bash
# 允许SSH (限制IP白名单)
ufw allow from 192.168.0.0/16 to any port 22 proto tcp

# 允许HTTP/HTTPS
ufw allow 80/tcp
ufw allow 443/tcp

# 允许1Panel面板 (IP白名单)
ufw allow from 192.168.0.0/16 to any port 11336 proto tcp

# 默认策略
ufw default deny incoming
ufw default allow outgoing

# Docker容器网络信任 (容器间通信)
ufw allow in on docker0 from 172.18.0.0/16
ufw allow in on docker0 from 172.19.0.0/16

# 启用防火墙
ufw enable
```

### 3.2 Nginx访问控制

```nginx
# 管理后台IP白名单
location /admin/ {
    allow 192.168.0.0/16;
    allow 10.0.0.0/8;
    deny all;
    proxy_pass http://platform-admin:3003;
}

# Actuator端点完全禁止外网访问
location ~* /actuator/ {
    deny all;
    access_log off;
    log_not_found off;
}

# 健康检查端点允许 (仅监控系统)
location = /health {
    allow 172.18.0.0/24;
    allow 127.0.0.1;
    deny all;
    proxy_pass http://core-gateway:8080;
}
```

### 3.3 网关路由验证

```yaml
# Nacos 配置 (tailor-is-gateway.yml)
spring:
  cloud:
    gateway:
      routes:
        # 用户服务
        - id: user-service
          uri: lb://tailor-is-user
          predicates:
            - Path=/api/v1/user/**
          filters:
            - StripPrefix=3
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
            - RemoveRequestHeader=Cookie,Set-Cookie
            - AddResponseHeader=X-Service, user-service
        # 商品服务
        - id: product-service
          uri: lb://tailor-is-product
          predicates:
            - Path=/api/v1/product/**
          filters:
            - StripPrefix=3
            - RemoveRequestHeader=Cookie,Set-Cookie
        # 订单服务
        - id: order-service
          uri: lb://tailor-is-order
          predicates:
            - Path=/api/v1/order/**
          filters:
            - StripPrefix=3
        # 支付服务
        - id: payment-service
          uri: lb://tailor-is-payment
          predicates:
            - Path=/api/v1/payment/**
          filters:
            - StripPrefix=3
```

---

## 四、端口变更流程

### 4.1 端口申请

1. 填写端口使用申请单（端口号、服务名、用途、使用周期）
2. 提交至运维团队审核
3. 审核通过后更新本规范文档
4. 同步更新Nacos配置、Nginx配置、docker-compose.yml

### 4.2 端口释放

1. 服务下线时及时释放端口
2. 通知运维更新防火墙规则
3. 更新端口使用台账

---

## 五、环境变量配置标准

### 5.1 核心环境变量（.env.production）

```
# =============== 服务器配置 ===============
SERVER_HOST=0.0.0.0
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod

# =============== 数据库配置 ===============
DB_HOST=172.18.0.2
DB_PORT=3306
DB_NAME=tailor_is
DB_USERNAME=tailor_is_user
DB_PASSWORD=<strong_random_password>

# =============== 缓存配置 ===============
REDIS_HOST=172.18.0.4
REDIS_PORT=6379
REDIS_PASSWORD=<strong_random_password>
REDIS_DB=0

# =============== 消息队列 ===============
MQ_HOST=172.18.0.3
MQ_PORT=5672
MQ_USERNAME=tailor_is
MQ_PASSWORD=<strong_random_password>

# =============== 服务注册/配置中心 ===============
NACOS_ADDR=172.19.0.2:8848
NACOS_NAMESPACE=prod
NACOS_USERNAME=nacos
NACOS_PASSWORD=<strong_random_password>
NACOS_TOKEN=<64位随机字符串>

# =============== 安全配置 ===============
JWT_SECRET=<64位随机密钥>
AES_KEY=<32位随机密钥>
ENCRYPTION_SALT=<16位随机盐>

# =============== 域名与HTTPS ===============
API_DOMAIN=api.tailoris.com
WWW_DOMAIN=www.tailoris.com
ADMIN_DOMAIN=admin.tailoris.com
MERCHANT_DOMAIN=merchant.tailoris.com
MOBILE_DOMAIN=m.tailoris.com
SSL_CERT_PATH=/etc/letsencrypt/live/tailoris.com/fullchain.pem
SSL_KEY_PATH=/etc/letsencrypt/live/tailoris.com/privkey.pem
```

---

## 六、路由与端口规范执行检查清单

- [x] 端口号唯一不冲突
- [x] 路由URL结构统一规范
- [x] 敏感端口不对外开放
- [x] 防火墙规则已同步
- [x] Nacos配置已同步
- [x] Nginx配置已同步
- [x] 环境变量配置文件已更新
- [x] 服务间调用已使用内部地址
- [x] 健康检查端点已配置
- [x] Actuator端点外网已禁止

---

**审批人**: 架构师  
**执行日期**: 2026-06-11  
**下次复审**: 2026-09-11
