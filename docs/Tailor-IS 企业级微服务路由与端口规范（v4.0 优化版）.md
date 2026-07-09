# Tailor-IS 企业级微服务路由与端口规范（v4.0 优化版）

## 文档基础信息

- 版本：v4.0
- 生效日期：2026-07-06
- 基准来源：原 v3.0 规范全量重构优化
- 核心优化目标
  1. **彻底落地网关统一收敛架构**，清除所有绕过网关直连服务的临时补丁
  2. 全链路消除端口 / 路由配置多源不一致问题
  3. 新增全套自动化校验工具 + CI 门禁强制约束，规范不再依赖人工自觉
  4. 修复原文档所有技术债务、架构冲突、安全短板
  5. 兼容原有 1Panel 端口硬性约束，不改动现有中间件部署方案
- 权威声明：本文档为项目路由、端口、网关、Nginx、容器配置**唯一标准**，所有代码、配置、脚本必须对齐，冲突以本文档为准；任何变更必须走审批流程并同步文档，CI 校验不通过禁止合并上线。

# 目录

1. 环境硬性约束（1Panel 不可变更端口）
2. 端口分层分配标准（重构分区，消除混乱）
3. 网络通信架构（统一网关收敛设计，无旁路直连）
4. API 路由全局统一规范（统一版本前缀，消除两套格式）
5. 双网关路由权威配置（清理重复路由，分层职责清晰）
6. Nginx 统一反向代理标准（仅作为入口，不转发业务服务）
7. 前后端路由标准
8. 鉴权统一分层设计（网关前置拦截 + 业务细粒度校验）
9. 全套自动化校验工具（端口 / 路由 / 配置一致性检测）
10. CI/CD 强制门禁规则（违反规范直接阻断合并）
11. 端口 & 路由变更审批与回滚流程（强化强制校验卡点）
12. 遗留技术债务整改清单（原 v3.0 待办全闭环）
13. 安全强制约束

***

# 一、1Panel 环境硬性约束（完全继承，不可修改）

## 1.1 宿主机永久禁用端口列表

所有 Docker Compose、Nginx、业务容器**禁止映射 / 占用**以下宿主机端口：

| 端口    | 归属服务           | 风险说明           |
| ----- | -------------- | -------------- |
| 3306  | MySQL          | 1Panel 托管数据库   |
| 6379  | Redis Master   | 缓存             |
| 26379 | Redis Sentinel | 哨兵集群           |
| 5672  | RabbitMQ AMQP  | 消息队列           |
| 15672 | RabbitMQ 管理后台  | MQ 控制台         |
| 8080  | Nacos 管理界面     | 核心冲突高危端口       |
| 8848  | Nacos API      | 服务注册 / 配置中心    |
| 9848  | Nacos gRPC     | Nacos 2.x 集群通信 |
| 11336 | 1Panel 运维面板    | 服务器管理入口        |

## 1.2 网关强制端口规则（固化，禁止修改）

1. core-gateway 容器内监听端口：`8080`（代码内置不可改）
2. core-gateway 宿主机映射端口：`9000`（规避 1Panel Nacos 8080）
3. lite-gateway 容器内监听端口：`8081`，宿主机映射端口：`8081`
4. 规则：所有业务流量**仅允许 Nginx → 网关**，禁止 Nginx 直接转发微服务

***

# 二、端口分层分配标准（重构优化，分区隔离更强）

## 2.1 端口分段设计原则

1. 业务微服务统一区间：`8100-8119`（已占用 8100\8115，预留 8116\8119 扩容）
2. 网关专属区间：`9000-9099`（core-gateway=9000，预留扩展多网关）
3. 前端 SSR / 静态服务：容器内部端口，**宿主机不做端口映射**，仅 Nginx 80/443 对外暴露
4. 监控运维独立分区：`9200-9299`（单独隔离，不和业务端口混放）
5. 公共工具服务：`4000-4099`（GraphQL、文件上传等）
6. 开发测试专用区间：`9500+`，与生产环境完全隔离，避免本地调试冲突

## 2.2 完整端口分配总表（优化版）

### 2.2.1 网关层（统一流量入口）

| 服务名称         | 容器端口 | 宿主机端口 | 承载路由前缀                                                                                                                                                                     | 职责划分                   |
| ------------ | ---- | ----- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------- |
| core-gateway | 8080 | 9000  | `/api/v1/{user,auth,product,favorite,order,cart,payment,settlement,account,sandbox,marketing,coupon,points,seckill,ai,body-size,copyright,merchant,shop,admin,pattern}/**` | 核心交易、支付、商家、平台管理等高优先级业务 |
| lite-gateway | 8081 | 8081  | `/api/v1/{community,post,comment,supply,message,notice,academy,course,analytics,metrics,dashboard,im,im-message}/**`                                                       | 社区、IM、内容、数据统计低延迟非核心流量  |

### 2.2.2 核心业务微服务（core-gateway 转发）

统一规范：所有服务新增接口强制使用 `/api/v1/` 前缀

| 服务名               | 容器端口 | 宿主机端口 | 路由前缀                                                                              |
| ----------------- | ---- | ----- | --------------------------------------------------------------------------------- |
| admin-service     | 8100 | 8100  | /api/v1/admin/\\*\\*                                                              |
| user-service      | 8101 | 8101  | /api/v1/user/**, /api/v1/auth/**                                                  |
| product-service   | 8102 | 8102  | /api/v1/product/**, /api/v1/favorite/**                                           |
| order-service     | 8103 | 8103  | /api/v1/order/**, /api/v1/cart/**                                                 |
| payment-service   | 8104 | 8104  | /api/v1/payment/**, /api/v1/settlement/**, /api/v1/account/**, /api/v1/sandbox/** |
| marketing-service | 8105 | 8105  | /api/v1/marketing/**, /api/v1/coupon/**, /api/v1/points/**, /api/v1/seckill/**    |
| ai-service        | 8106 | 8106  | /api/v1/ai/**, /api/v1/body-size/**                                               |
| copyright-service | 8107 | 8107  | /api/v1/copyright/\\*\\*                                                          |
| merchant-service  | 8110 | 8110  | /api/v1/merchant/**, /api/v1/shop/**                                              |
| pattern-service   | 8115 | 8115  | /api/v1/pattern/\\*\\*                                                            |

### 2.2.3 轻量业务微服务（lite-gateway 转发）

| 服务名                | 容器端口 | 宿主机端口 | 路由前缀                                                               |
| ------------------ | ---- | ----- | ------------------------------------------------------------------ |
| community-service  | 8108 | 8108  | /api/v1/community/**, /api/v1/post/**, /api/v1/comment/\\*\\*      |
| supply-service     | 8109 | 8109  | /api/v1/supply/\\*\\*                                              |
| message-service    | 8111 | 8111  | /api/v1/message/**, /api/v1/notice/**                              |
| academy-service    | 8112 | 8112  | /api/v1/academy/**, /api/v1/course/**                              |
| analytics-service  | 8113 | 8113  | /api/v1/analytics/**, /api/v1/metrics/**, /api/v1/dashboard/\\*\\* |
| message-im-service | 8114 | 8114  | /api/v1/im/**, /ws/**（WebSocket 统一经网关转发）                           |

### 2.2.4 公共工具服务

| 服务              | 容器端口 | 宿主机端口 | 路由                                 |
| --------------- | ---- | ----- | ---------------------------------- |
| file-upload     | 8082 | 8082  | /api/v1/upload/**, /api/v1/file/** |
| graphql-gateway | 4000 | 4000  | /graphql                           |

### 2.2.5 前端服务（优化点：宿主机无端口映射）

所有前端容器仅容器内部端口，宿主机不暴露端口，由 Nginx 统一 80/443 反向代理

| 前端服务           | 容器内部端口 | 绑定域名                                                   |
| -------------- | ------ | ------------------------------------------------------ |
| pc-mall SSR    | 3000   | [www.tailoris.com](https://www.tailoris.com)           |
| merchant-admin | 3002   | [merchant.tailoris.com](https://merchant.tailoris.com) |
| platform-admin | 3003   | [admin.tailoris.com](https://admin.tailoris.com)       |
| mobile-h5      | 3004   | [m.tailoris.com](https://m.tailoris.com)               |

### 2.2.6 监控运维分区（独立 9200 段，原分散端口统一收拢）

| 组件                 | 宿主机端口 | 用途                   |
| ------------------ | ----- | -------------------- |
| Prometheus         | 9200  | 指标采集服务               |
| Grafana            | 9201  | 监控大盘（彻底脱离 3000 前端区间） |
| Alertmanager       | 9202  | 告警管理                 |
| Alert Webhook      | 9203  | 钉钉 / 飞书通知分发          |
| Node Exporter      | 9210  | 服务器指标                |
| Mysql Exporter     | 9211  | 数据库指标                |
| Redis Exporter     | 9212  | 缓存指标                 |
| Blackbox Exporter  | 9213  | 接口拨测                 |
| Sentinel Dashboard | 9220  | 流量管控面板               |

## 2.3 端口使用强制禁令

1. 禁止跨区间占用端口：业务服务不得占用 9200 监控段、网关不得占用 8100 业务段
2. 禁止前端容器直接暴露宿主机端口，必须由 Nginx 统一入口
3. 禁止任何业务容器映射 1Panel 托管端口
4. 新增业务服务优先使用 8116\~8119 预留端口，新增监控组件使用 9200 + 区间

***

# 三、网络通信架构（核心优化：网关统一收敛，无旁路直连）

## 3.1 标准流量链路（唯一合法链路，废除所有直连补丁）

```Plain
客户端（浏览器/小程序）
        ↓
Nginx（80/443，仅做SSL、静态资源、路由分发）
        ↓
├─ /api/v1/core业务/** → core-gateway:9000
│       ↓
│       Nacos服务发现 lb://xxx → 对应微服务（容器内网通信）
│
├─ /api/v1/lite内容/** /ws/** → lite-gateway:8081
│       ↓
│       Nacos服务发现 lb://xxx → 社区/IM/学院服务
│
├─ /graphql → graphql-gateway:4000
├─ 域名根路径 → pc-mall/商户/平台前端静态资源
```

## 3.2 架构整改强制要求（解决原 v3.0 最大架构缺陷）

1. **彻底删除 Nginx 直连业务服务配置**
   移除 `^~ /api/products/`、`^~ /api/community/` 直连规则，所有 API 统一经过网关；
   收益：全部流量复用网关统一鉴权、限流、熔断、日志、灰度、跨域能力。
2. **WebSocket 长连接统一走 lite-gateway**
   `/ws/**` 转发至 lite-gateway，由网关转发 message-im-service，网关统一校验 IM 登录 token、拦截非法连接、限制在线连接数。
3. **所有网关路由统一使用 Nacos 服务发现 lb://**
   删除原 product-service 硬编码`http://host.docker.internal:8102`，全部标准化`lb://tailor-is-{service}`，支持自动扩缩容、负载均衡。
4. 清理重复路由：core-gateway 删除 community 相关路由，社区流量完全交由 lite-gateway，消除路由冲突隐患。

## 3.3 容器网络规范

1. 所有业务容器、网关加入统一 Docker 网桥 `tailoris-services`
2. 容器访问 1Panel 中间件统一使用宿主机地址 `127.0.0.1:端口`，不额外部署 MySQL/Redis/RabbitMQ/Nacos 容器
3. 容器间通信仅内网互通，业务微服务**禁止宿主机端口对外暴露**，仅网关对外开放 9000/8081

***

# 四、API 路由全局统一规范（消除两套前缀历史债务）

## 4.1 URL 强制标准（全项目统一）

### 标准结构

```Plain
https://api.tailoris.com/api/v1/{domain}/{resource}/{action}/{id}
```

- 强制全局前缀：`/api/v1/`，废除无 v1 的旧接口格式
- 存量旧接口 `/api/xxx` 分 3 个迭代版本逐步下线，过渡期网关做兼容转发（仅临时过渡，有下线时间节点）
- 禁止新增不带 v1 前缀的接口

## 4.2 强制编码规范

1. 资源名词统一使用单数：`/api/v1/product` 禁止 `/api/v1/products`
2. 操作语义由 HTTP Method 区分，URL 禁止带 delete/update 等动词
   ✅ `DELETE /api/v1/cart/{id}`  ❌ `POST /api/v1/cart/delete`
3. 身份参数统一由网关解析 Token 注入 Header `X-User-Id`，下游服务禁止重复解析 Token
4. 文件上传统一路径 `/api/v1/upload/{bizType}`，multipart/form-data
5. 分页、筛选统一使用 Query 参数，资源 ID 使用路径参数

## 4.3 鉴权注解统一标准（Sa-Token）

| 注解                               | 使用场景                    | 生效层级   |
| -------------------------------- | ----------------------- | ------ |
| `@SaIgnore`                      | 登录、注册、首页公开商品、健康检查       | 方法级    |
| `@SaCheckLogin`                  | C 端用户所有操作（购物车、订单、IM）    | 类级统一标注 |
| `@SaCheckRole("merchant")`       | 商家后台接口                  | 类级     |
| `@SaCheckRole("merchant_owner")` | 商家账号、权限管理               | 方法级    |
| `@SaCheckRole("admin")`          | 平台管理 `/api/v1/admin/**` | 类级强制标注 |

## 4.4 管理端收敛优化

所有平台管理接口`/api/v1/admin/**`仅允许定义在 admin-service，其他微服务删除 Admin 相关 Controller，统一收敛管理流量，便于统一审计、限流。

***

# 五、双网关路由权威配置（唯一配置源，杜绝多文件冲突）

## 5.1 网关配置强制约束

1. **路由唯一权威来源**：仅允许在 `CoreGatewayRouteConfig.java` / `LiteGatewayRouteConfig.java` 定义路由
2. 禁止在 `application.yml`、`bootstrap.yml` 中配置任何路由规则，CI 静态检测拦截违规代码
3. 路由 ID 统一规范：`{domain}-route`，例如 `user-route`、`community-route`
4. 转发统一配置 `StripPrefix=0`，完整传递 `/api/v1/xxx` 路径至下游服务
5. uri 必须使用 `lb://tailor-is-{service-name}` 服务发现格式，禁止硬编码 IP / 端口

## 5.2 core-gateway 标准路由表（无 community 重复路由）

| route-id        | 匹配 Path                                                                        | 转发目标                     |
| --------------- | ------------------------------------------------------------------------------ | ------------------------ |
| user-route      | /api/v1/user/**,/api/v1/auth/**                                                | lb://tailor-is-user      |
| product-route   | /api/v1/product/**,/api/v1/favorite/**                                         | lb://tailor-is-product   |
| order-route     | /api/v1/order/**,/api/v1/cart/**                                               | lb://tailor-is-order     |
| payment-route   | /api/v1/payment/**,/api/v1/settlement/**,/api/v1/account/**,/api/v1/sandbox/** | lb://tailor-is-payment   |
| marketing-route | /api/v1/marketing/**,/api/v1/coupon/**,/api/v1/points/**,/api/v1/seckill/**    | lb://tailor-is-marketing |
| ai-route        | /api/v1/ai/**,/api/v1/body-size/**                                             | lb://tailor-is-ai        |
| copyright-route | /api/v1/copyright/\\*\\*                                                       | lb://tailor-is-copyright |
| merchant-route  | /api/v1/merchant/**,/api/v1/shop/**                                            | lb://tailor-is-merchant  |
| admin-route     | /api/v1/admin/\\*\\*                                                           | lb://tailor-is-admin     |
| pattern-route   | /api/v1/pattern/\\*\\*                                                         | lb://tailor-is-pattern   |

## 5.3 lite-gateway 标准路由表（承载全部内容 / IM 流量）

| route-id        | 匹配 Path                                                          | 转发目标                      |
| --------------- | ---------------------------------------------------------------- | ------------------------- |
| community-route | /api/v1/community/**,/api/v1/post/**,/api/v1/comment/\\*\\*      | lb://tailor-is-community  |
| supply-route    | /api/v1/supply/\\*\\*                                            | lb://tailor-is-supply     |
| message-route   | /api/v1/message/**,/api/v1/notice/**                             | lb://tailor-is-message    |
| academy-route   | /api/v1/academy/**,/api/v1/course/**                             | lb://tailor-is-academy    |
| analytics-route | /api/v1/analytics/**,/api/v1/metrics/**,/api/v1/dashboard/\\*\\* | lb://tailor-is-analytics  |
| im-route        | /api/v1/im/**,/ws/**                                             | lb://tailor-is-message-im |

## 5.4 网关统一前置能力（所有流量强制经过，不可绕过）

1. Token 解析、用户身份注入 Header
2. 统一跨域处理
3. 接口限流、IP 黑名单、登录防刷
4. 请求日志统一采集
5. 灰度发布、流量染色
6. 统一异常封装返回体
7. 非法 Token、黑名单用户直接拦截（不转发下游服务）

***

# 六、Nginx 统一反向代理标准（精简配置，消除多文件冲突）

## 6.1 配置文件统一治理

1. 废弃 `default.conf`，仅保留 `frontend.conf` 作为唯一 Nginx 业务配置文件，消除两套上游地址不一致 BUG
2. 拆分独立片段：ssl.conf、security-headers.conf、rate-limit.conf、cdn-cache.conf，统一引入
3. 所有 upstream 仅指向两个网关 + graphql，**无任何业务微服务直连 upstream**

## 6.2 标准 upstream 定义

```nginx
upstream tailoris_core_gateway {
    server host.docker.internal:9000;
}
upstream tailoris_lite_gateway {
    server host.docker.internal:8081;
}
upstream tailoris_graphql {
    server host.docker.internal:4000;
}
```

## 6.3 Location 路由优先级规则

1. `= /healthz`：健康检查，直接返回 ok
2. `~ ^/api/v1/(community|post|comment|supply|message|academy|analytics|im)(/|$)` → lite-gateway
3. `~ ^/api/v1/(user|auth|product|order|payment|marketing|ai|copyright|merchant|admin|pattern)(/|$)` → core-gateway
4. `/graphql` → graphql-gateway
5. `/ws/` → lite-gateway（WebSocket 长连接统一网关转发）
6. 域名根路径匹配前端静态资源（pc/merchant/admin/h5）
7. 全局兜底 `/api/v1/` → core-gateway

## 6.4 Nginx 安全强制规则

1. 外网全部拦截 `/actuator/**`，禁止暴露监控端点
2. `/api/v1/auth/login` 单独限流，防暴力破解
3. 全站 HTTPS 强制跳转，开启 HSTS
4. 静态资源 CDN 缓存策略统一配置
5. 禁止直接转发至后端微服务，所有 API 必须经过网关

***

# 七、鉴权分层设计优化（强化网关拦截，减轻服务负担）

1. **网关层（前置拦截）**
   - 无 Token、过期 Token、黑名单用户直接 401 拦截，不转发下游
   - 解析 Token，注入`X-User-Id`、`X-User-Role`请求头
   - 统一拦截跨域、非法请求参数、高频攻击 IP
2. **微服务层（细粒度业务校验）**
   - 仅校验角色权限、数据行权限（商家只能操作自己店铺、管理员全量权限）
   - 不重复解析 Token，依赖网关注入的身份 Header
3. 公开接口白名单统一在网关配置，无需每个服务单独维护

***

# 八、全套自动化校验工具（强化强制约束核心模块）

存放路径：`deploy/scripts/`，CI 流水线自动执行，不通过阻断部署

## 8.1 端口冲突检测脚本 `check-port-conflicts.sh`（增强版）

### 新增能力（原脚本仅检测占用，现增加多维度校验）

1. 检测宿主机端口占用，对比本文档端口表，识别冲突
2. 扫描所有`docker-compose*.yml`端口映射：
   - 禁止映射 1Panel 禁用端口
   - 校验业务服务端口是否在 8100 区间、网关是否 9000/8081
   - 前端容器禁止配置宿主机端口映射
3. 扫描 Nginx 配置 upstream，禁止出现业务微服务 IP 直连
4. 校验监控组件端口是否统一在 9200 区间
5. 输出 CRITICAL/WARNING/INFO 三级告警，生产部署 CRITICAL 告警直接阻断

### 执行命令

```bash
# 生产强制校验（存在冲突直接退出非0）
bash deploy/scripts/check-port-conflicts.sh
# CI展示模式（仅输出，不阻断流水线）
bash deploy/scripts/check-port-conflicts.sh --no-fail
```

## 8.2 网关路由一致性检测脚本 `check-gateway-routes.sh`

### 核心校验逻辑

1. 读取 Core/Lite 网关 Java Config 全部路由
2. 和本文档路由表比对，检测缺失路由、多余路由、重复路由
3. 检测路由 uri 是否使用`lb://`，拦截硬编码 IP / 端口
4. 检测 core-gateway 是否包含 community 相关路由（违规直接报错）
5. 检测是否在 yml 文件存在路由配置，拦截违规代码

## 8.3 Nginx 配置静态校验脚本 `check-nginx-config.sh`

1. 执行`nginx -t`语法校验
2. 扫描配置文件，拦截直连业务服务的 proxy\_pass
3. 校验所有上游网关端口为 9000/8081，拦截旧 8080 地址
4. 校验 WebSocket 转发目标为 lite-gateway，禁止直连 IM 服务

## 8.4 接口前缀统一校验脚本 `check-api-prefix.sh`

1. 扫描所有 Controller `@RequestMapping`
2. 拦截新增不带`/api/v1/`前缀的接口
3. 统计存量旧接口，输出下线进度报表

## 8.5 全量一键校验脚本 `pre-deploy-check.sh`

整合以上所有脚本，部署前一键执行，任何 CRITICAL 错误终止发布流程。

***

# 九、CI/CD 强制门禁（规范落地不靠人工，流水线强制管控）

## 9.1 代码提交门禁（Git Pre-Commit 钩子）

提交代码自动执行：

1. `check-gateway-routes.sh` 路由校验
2. `check-api-prefix.sh` 接口前缀校验
3. 检测 docker-compose 端口是否违规
   存在违规直接阻断 git commit，无法提交代码。

## 9.2 MR/PR 合并门禁（流水线强制校验）

合并分支前自动执行全套校验：

1. 全量端口冲突检测
2. 网关路由合规检测
3. Nginx 配置合规检测
4. 代码静态检查（禁止 yml 写路由、硬编码端口）
5. 校验：修改端口 / 网关 / Nginx 配置时，必须同步更新本文档，文档未同步阻断合并

## 9.3 生产部署门禁

生产服务器执行`pre-deploy-check.sh`，出现 CRITICAL 端口 / 路由冲突直接终止 docker compose 启动。

***

# 十、端口 & 路由变更审批流程（强化自动化卡点）

## 10.1 变更前置强制条件

1. 提交变更申请表
2. 本地执行全套自动化脚本，无 CRITICAL 告警
3. 更新本文档端口 / 路由表格，同步提交 PR

## 10.2 审核角色

架构师（架构合规）+ 运维工程师（部署 / 端口合规）+ 业务服务负责人（业务影响评估），双角色批准才可合并。

## 10.3 上线验证步骤（自动化校验优先）

1. 预发环境部署，执行端口检测脚本
2. 全链路接口连通性自动化测试
3. Nginx 语法校验、网关路由打印核对
4. WebSocket 长连接连通测试
5. 监控大盘正常采集指标

## 10.4 回滚标准化流程

配套一键回滚脚本，回滚后自动执行校验脚本确认恢复正常。

***

# 十一、原 v3.0 技术债务闭环整改清单（全部强制落地）

| 原问题 ID    | 问题描述                            | v4.0 整改方案                                  | 完成状态   |
| --------- | ------------------------------- | ------------------------------------------ | ------ |
| PORT-008  | default.conf 仍指向 8080           | 废弃 default.conf，统一 frontend.conf，所有上游 9000 | ✅ 永久修复 |
| ROUTE-005 | product 路由硬编码 http 地址           | 全部改为 lb:// 服务发现                            | ✅ 永久修复 |
| ROUTE-006 | core/lite 重复 community 路由       | core 删除 community 路由，统一 lite 承载            | ✅ 永久修复 |
| ROUTE-007 | Nginx 直连 product/community 绕过网关 | 删除所有直连规则，全流量走网关                            | ✅ 永久修复 |
| ROUTE-008 | 接口 v1 前缀不统一                     | 新增强制 v1，存量分版本下线，脚本自动拦截违规新增                 | 迭代落地中  |

***

# 十二、安全强制约束补充

1. 所有业务微服务 Actuator 仅内网访问，外网 Nginx 全部拦截
2. 网关限流分级：登录接口限流最严格，秒杀单独限流分组
3. 宿主机防火墙仅放行 80/443/9000/8081，其余业务端口内网互通，禁止外网访问
4. 监控面板 9201 (Grafana) 增加 IP 白名单，仅运维内网可访问
5. WebSocket 连接增加频率限制，防止恶意刷屏攻击

***

# 交付物说明

本 v4.0 优化版达成两大核心诉求：

1. **网关统一收敛架构完全落地**：清除所有旁路直连历史补丁，所有 API、WebSocket 流量统一经过网关，最大化复用网关限流、鉴权、灰度、日志统一能力，架构干净无折中方案；
2. **全链路自动化强制约束闭环**：从本地提交钩子、MR 流水线、生产部署三层拦截，配套 5 套检测脚本，规范不再依靠开发 / 运维人工记忆，违规操作会被自动化工具直接阻断，从根源杜绝端口冲突、路由不一致、配置多源错乱问题。

可直接替换原有 v3.0 规范作为企业级正式标准，配套脚本可同步落地到项目`deploy/scripts`目录投入使用。
