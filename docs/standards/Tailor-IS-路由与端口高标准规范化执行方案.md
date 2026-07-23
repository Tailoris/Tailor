# Tailor IS 路由与端口高标准规范化执行方案

> **版本**: v1.0  
> **日期**: 2026-07-20  
> **适用范围**: 全项目（后端微服务、网关、Nginx、前端）  
> **关联规范**: [Tailor-IS 企业级微服务路由与端口规范（v4.0 优化版）](../Tailor-IS%20企业级微服务路由与端口规范（v4.0%20优化版）.md)

---

## 一、路由命名规范

### 1.1 命名格式标准

#### API 路由格式
```
/api/v{major}/{module}/{resource}[/{id}][/{sub-resource}]
```

| 段 | 格式 | 示例 | 说明 |
|----|------|------|------|
| 前缀 | `/api` | `/api` | 固定前缀，不可修改 |
| 版本 | `/v{major}` | `/v1` | 仅主版本号，大版本升级时递增 |
| 模块 | 小写横线分隔 | `user`, `product`, `order` | 与微服务模块名一致 |
| 资源 | 小写复数 | `users`, `products`, `orders` | RESTful 资源命名 |
| 子资源 | 小写单数 | `profile`, `address`, `items` | 子资源命名 |

#### 前端路由格式
```
{base-path}/{module}/{page}[/{id}]
```

| 项目 | Base 路径 | 示例 |
|------|----------|------|
| PC商城 | `/` | `/product/1`, `/cart`, `/orders` |
| 商家后台 | `/merchant/` | `/merchant/dashboard`, `/merchant/product/list` |
| 平台管理 | `/admin/` | `/admin/dashboard`, `/admin/merchant/audit` |
| 移动端 | `/pages/` | `/pages/index/index`, `/pages/product/detail` |

### 1.2 关键字使用规则

#### 禁止使用的关键字
| 关键字 | 原因 | 替代方案 |
|--------|------|---------|
| `get`, `set` | 非RESTful | 使用HTTP方法区分 |
| `do`, `exec` | 语义不清 | 使用具体业务动作 |
| `temp`, `test` | 非正式 | 仅允许在开发环境 |
| 中文拼音 | 非标准 | 使用英文翻译 |
| 下划线 `_` | 非URL友好 | 使用横线 `-` |
| 大写字母 | 非URL友好 | 统一小写 |

#### 推荐的业务动作关键字
| 动作 | 关键字 | 示例 |
|------|--------|------|
| 创建 | `create` (POST) | `POST /api/v1/order/orders` |
| 查询 | 无特殊前缀 (GET) | `GET /api/v1/order/orders/{id}` |
| 更新 | `update` (PUT/PATCH) | `PUT /api/v1/order/orders/{id}` |
| 删除 | `delete` (DELETE) | `DELETE /api/v1/order/orders/{id}` |
| 搜索 | `search` | `GET /api/v1/product/products/search` |
| 批量操作 | `batch` | `POST /api/v1/order/orders/batch` |
| 导出 | `export` | `GET /api/v1/analytics/reports/export` |
| 审核 | `audit`, `review` | `POST /api/v1/admin/merchants/{id}/audit` |

### 1.3 版本控制策略

```
版本演进规则:
  v1 → v2: 破坏性 API 变更（字段删除、响应结构变更）
  v1.1 → v1.2: 向后兼容的功能新增（新增字段、新增接口）
  
兼容期: 旧版本 API 保留至少 6 个月
废弃通知: 通过响应头 X-API-Deprecated 告知客户端
```

---

## 二、路径设计原则

### 2.1 层级结构标准

```
/api/v1                              ← 根路径
├── /auth/**                         ← 认证授权
│   ├── /login                       ← 登录
│   ├── /register                    ← 注册
│   └── /refresh                     ← Token 刷新
├── /user/**                         ← 用户中心
│   ├── /users                       ← 用户列表
│   ├── /users/{id}                  ← 用户详情
│   ├── /users/{id}/addresses        ← 用户地址
│   └── /users/{id}/profile          ← 用户资料
├── /product/**                      ← 商品中心
│   ├── /products                    ← 商品列表
│   ├── /products/{id}               ← 商品详情
│   ├── /products/search             ← 商品搜索
│   ├── /categories                  ← 商品分类
│   └── /specifications              ← 商品规格
├── /order/**                        ← 订单中心
│   ├── /orders                      ← 订单列表
│   ├── /orders/{id}                 ← 订单详情
│   ├── /orders/{id}/cancel          ← 取消订单
│   ├── /orders/{id}/after-sale      ← 售后申请
│   └── /cart                        ← 购物车
├── /payment/**                      ← 支付中心
│   ├── /payments                    ← 支付记录
│   ├── /payments/{id}/refund        ← 退款
│   └── /accounts                    ← 账户管理
├── /marketing/**                    ← 营销中心
│   ├── /coupons                     ← 优惠券
│   ├── /seckill                     ← 秒杀活动
│   └── /points                      ← 积分体系
├── /ai/**                           ← AI 服务
│   ├── /body-size                   ← 人体尺寸
│   └── /pattern/generate            ← 版型生成
├── /copyright/**                    ← 版权服务
│   ├── /registrations               ← 版权登记
│   └── /infringements               ← 侵权投诉
├── /merchant/**                     ← 商家中心
│   ├── /merchants                   ← 商家管理
│   └── /shops                       ← 店铺管理
├── /admin/**                        ← 平台管理
│   ├── /dashboard                   ← 数据看板
│   ├── /merchants/audit             ← 商家审核
│   └── /reports                     ← 财务报表
├── /community/**                    ← 社区
│   ├── /posts                       ← 帖子
│   └── /comments                    ← 评论
├── /supply/**                       ← 供应链
│   ├── /demands                     ← 需求发布
│   └── /suppliers                   ← 供应商
├── /message/**                      ← 消息中心
│   ├── /notices                     ← 系统通知
│   └── /messages                    ← 私信
├── /academy/**                      ← 学院
│   ├── /courses                     ← 课程
│   └── /progress                    ← 学习进度
├── /analytics/**                    ← 数据分析
│   ├── /metrics                     ← 指标数据
│   └── /dashboard                   ← 数据大屏
├── /im/**                           ← 即时通讯
│   ├── /conversations               ← 会话
│   └── /messages                    ← 消息
├── /pattern/**                      ← 纸样服务
│   ├── /patterns                    ← 纸样库
│   └── /requirements                ← 纸样需求
├── /design/**                       ← 设计服务
│   ├── /designs                     ← 设计作品
│   └── /reviews                     ← 设计审核
└── /knowledge/**                    ← 知识库
    ├── /articles                    ← 知识文章
    └── /categories                  ← 知识分类
```

### 2.2 参数传递方式

| 参数类型 | 传递方式 | 示例 | 适用场景 |
|---------|---------|------|---------|
| 资源ID | 路径参数 | `/users/{id}` | 单个资源定位 |
| 查询条件 | Query String | `?page=1&size=20&status=active` | 列表查询、过滤 |
| 排序 | Query String | `?sort=createTime,desc` | 列表排序 |
| 分页 | Query String | `?page=1&size=20` | 分页查询 |
| 创建/更新 | Request Body (JSON) | `{"name":"xxx"}` | 数据提交 |
| 批量操作 | Request Body (JSON Array) | `[{"id":1},{"id":2}]` | 批量处理 |
| 文件上传 | Multipart Form | `multipart/form-data` | 文件上传 |
| 认证 | Header | `Authorization: Bearer xxx` | 身份认证 |
| 幂等键 | Header | `X-Idempotency-Key: xxx` | 防重复提交 |

### 2.3 RESTful API 设计规范

#### HTTP 方法语义
| 方法 | 语义 | 幂等性 | 安全 | 示例 |
|------|------|--------|------|------|
| GET | 查询资源 | 是 | 是 | `GET /api/v1/product/products/1` |
| POST | 创建资源 | 否 | 否 | `POST /api/v1/order/orders` |
| PUT | 全量更新 | 是 | 否 | `PUT /api/v1/user/users/1` |
| PATCH | 部分更新 | 否 | 否 | `PATCH /api/v1/user/users/1/profile` |
| DELETE | 删除资源 | 是 | 否 | `DELETE /api/v1/product/products/1` |

#### 响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "error": null
}
```

#### 分页响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 100,
    "pages": 10,
    "current": 1,
    "size": 10
  }
}
```

---

## 三、端口分配策略

### 3.1 端口范围划分机制

| 端口段 | 用途 | 服务类型 | 数量 |
|--------|------|---------|------|
| 80/443 | Nginx 反向代理 | Web 入口 | 2 |
| 3306 | MySQL | 数据库 | 1 |
| 6379 | Redis | 缓存 | 1 |
| 8848/9848 | Nacos | 注册中心 | 2 |
| 5672/15672 | RabbitMQ | 消息队列 | 2 |
| 4000-4099 | 公共工具服务 | GraphQL Gateway | 1 |
| 8080/8081 | 网关容器内端口 | 网关内部 | 2 |
| 8100-8117 | 业务微服务 | 核心+轻量服务 | 18 |
| 9000 | Core Gateway 宿主机端口 | 核心网关入口 | 1 |
| 9200-9299 | 监控运维 | Prometheus/Grafana | 4 |
| 9500+ | 开发测试 | 本地开发 | 预留 |

### 3.2 服务类型与端口对应关系

| 序号 | 服务 | 容器端口 | 宿主机端口 | 分组 | 类型 |
|------|------|---------|-----------|------|------|
| 1 | core-gateway | 8080 | 9000 | 网关 | 核心 |
| 2 | lite-gateway | 8081 | 8081 | 网关 | 轻量 |
| 3 | admin-service | 8100 | 8100 | core-trade | 核心 |
| 4 | user-service | 8101 | 8101 | core-trade | 核心 |
| 5 | product-service | 8102 | 8102 | core-trade | 核心 |
| 6 | order-service | 8103 | 8103 | core-trade | 核心 |
| 7 | payment-service | 8104 | 8104 | core-trade | 核心 |
| 8 | marketing-service | 8105 | 8105 | core-ai-chain | 核心 |
| 9 | ai-service | 8106 | 8106 | core-ai-chain | 核心 |
| 10 | copyright-service | 8107 | 8107 | core-ai-chain | 核心 |
| 11 | community-service | 8108 | 8108 | light-forum | 轻量 |
| 12 | supply-service | 8109 | 8109 | light-data-supply | 轻量 |
| 13 | merchant-service | 8110 | 8110 | core-trade | 核心 |
| 14 | message-service | 8111 | 8111 | light-forum | 轻量 |
| 15 | academy-service | 8112 | 8112 | light-edu | 轻量 |
| 16 | analytics-service | 8113 | 8113 | light-data-supply | 轻量 |
| 17 | message-im-service | 8114 | 8114 | light-forum | 轻量 |
| 18 | pattern-service | 8115 | 8115 | core-ai-chain | 核心 |
| 19 | design-service | 8116 | 8116 | core-ai-chain | 核心 |
| 20 | knowledge-service | 8117 | 8117 | light-edu | 轻量 |

### 3.3 端口分配原则

1. **连续分配**: 业务服务端口必须在 8100-8117 连续区间内
2. **唯一性**: 每个服务独占一个端口，禁止多服务共享
3. **环境统一**: 开发/测试/生产环境端口必须一致
4. **禁止暴露**: 业务服务宿主机端口仅用于本地调试，生产环境禁止对外暴露
5. **网关唯一**: 仅 core-gateway(9000) 和 lite-gateway(8081) 对外暴露

---

## 四、冲突解决机制

### 4.1 端口冲突检测方法

#### 自动检测脚本
```bash
# deploy/scripts/check-port-conflicts.sh
# 检测项目: 端口占用、多服务重复、超出规范范围
```

#### 检测流程
1. 读取 docker-compose.services.yml 中所有端口映射
2. 读取 .env 中配置的端口变量
3. 读取各微服务 application.yml 中的 server.port
4. 对比 RoutePortConstant.java 中的常量定义
5. 检查宿主机端口是否已被占用
6. 输出冲突报告

#### CI/CD 集成
```yaml
# GitHub Actions 中集成
- name: 端口冲突检测
  run: bash deploy/scripts/check-port-conflicts.sh
```

### 4.2 冲突解决方案

| 冲突类型 | 检测方式 | 解决方式 |
|---------|---------|---------|
| 两服务同端口 | 静态扫描 | 重新分配端口，确保唯一性 |
| 占用 1Panel 端口 | 白名单对比 | 迁移到非 1Panel 端口段 |
| 超出规范范围 | 范围校验 | 调整到 8100-8117 区间 |
| 网关端口冲突 | 特殊检查 | core-gateway 仅 9000, lite-gateway 仅 8081 |
| 中间件端口冲突 | 已知端口列表 | 3306/6379/8848/5672 不可占用 |

### 4.3 路由冲突检测

| 冲突类型 | 检测方式 | 解决方式 |
|---------|---------|---------|
| 重复路由前缀 | 网关路由扫描 | 合并路由或拆分模块 |
| 旧版路由残留 | `/api/` 前缀检测 | 迁移到 `/api/v1/` 或添加兼容路由 |
| Nginx 路由不一致 | 两文件对比 | 统一 frontend.conf 和 frontend-docker.conf |
| WebSocket 路由冲突 | `/ws/**` 检测 | 确保 ws 路由仅指向 lite-gateway |

### 4.4 强制校验卡点

1. **代码提交前**: `pre-commit` 钩子执行端口冲突检测
2. **PR 合并前**: CI 自动执行路由规范校验
3. **Maven 打包前**: `check-route-port-spec.sh` 脚本校验
4. **部署前**: `pre-deploy-check.sh` 全量检查
5. **服务启动时**: 网关启动时自动对比路由标准常量

---

## 五、自动化校验脚本

### 5.1 端口校验脚本
- `deploy/scripts/check-port-conflicts.sh` - 端口冲突检测
- `deploy/scripts/check-port-allocation.sh` - 端口分配合规性
- `deploy/scripts/check-container-ports.sh` - 容器端口映射检查

### 5.2 路由校验脚本
- `deploy/scripts/check-route-port-spec.sh` - 路由端口规范全量检查
- `deploy/scripts/check-nginx-config.sh` - Nginx 配置合规性
- `deploy/scripts/check-api-prefix.sh` - API 前缀一致性检查

### 5.3 部署前校验
- `deploy/scripts/pre-deploy-check.sh` - 部署前全量预检
- `deploy/scripts/ci-port-check.sh` - CI 端口检查

---

## 六、管理规范

### 6.1 端口变更审批流程
```
变更申请 → 技术评审 → 本地校验 → 文档更新 → PR提交 → CI校验 → 合并部署
```

### 6.2 路由变更审批流程
```
需求评审 → 路由设计 → Controller实现 → 网关配置 → Nginx配置 → 文档更新 → PR提交 → 合并
```

### 6.3 文档同步要求
- 端口变更必须同步更新: `RoutePortConstant.java` + `.env.example` + `docker-compose.services.yml` + 本文档
- 路由变更必须同步更新: `Controller` + `GatewayRouteConfig` + `Nginx配置` + 本文档

---

> **编制**: AI辅助规范系统  
> **日期**: 2026-07-20  
> **下次审查**: 2026-08-20