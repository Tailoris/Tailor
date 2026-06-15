# Tailor IS 微服务路由与端口规范化方案

> 制定高标准、规范化的路由与端口分配体系，杜绝随意化与冲突

## 一、规范化原则

### 1.1 端口分配原则
- **唯一性**: 全环境端口不可重复（开发/测试/生产保持一致）
- **业务分层**: 端口号段严格按业务层级划分
- **可记忆性**: 端口号与业务语义有强关联
- **预留扩展**: 每个区段保留 20% 端口给未来扩展

### 1.2 路由命名原则
- **统一前缀**: 所有业务接口使用 `/api/{version}/{service}/{resource}`
- **RESTful**: 资源导向，使用HTTP方法表达操作
- **版本管理**: URL Path 版本化（`/v1`、`/v2`）
- **网关收敛**: 客户端只访问 Gateway，绕过直接访问服务

### 1.3 服务命名原则
- **小写连字符**: `tailor-is-{business-name}`
- **禁用下划线/驼峰**: 避免Nacos显示混乱
- **业务语义清晰**: 一看名字知道职责

---

## 二、端口号段规划（官方权威）

> 来源: `platform-admin/src/views/dashboard/DashboardView.vue` 官方前端配置

| 号段 | 范围 | 用途 | 服务 |
|------|------|------|------|
| **0** | 8080 | API 网关 | gateway |
| **1** | 8100-8100 | 管理 | admin (合并) |
| **2** | 8101 | 用户域 | user |
| **3** | 8102 | 商品域 | product |
| **4** | 8103 | 订单域 | order |
| **5** | 8104 | 支付域 | payment |
| **6** | 8105 | 营销域 | marketing |
| **7** | 8106 | AI域 | ai |
| **8** | 8107 | 版权域 | copyright |
| **9** | 8108 | 社区域 | community |
| **10** | 8109 | 供应链 | supply |
| **11** | 8110-8112 | 预留 | - |
| **12** | 8113 | 消息 | message |
| **13** | 8114 | IM | message-im |
| **14** | 8115 | 学院 | academy |
| **15** | 8116 | 分析 | analytics |
| **16** | 8117-8119 | 扩展 | - |

---

## 三、官方端口映射表（13个微服务）

| # | 服务名 | 端口 | 当前配置 | 标准配置 | 状态 |
|---|--------|------|----------|----------|------|
| 1 | tailor-is-gateway | 8080 | 8080 | 8080 | ✅ |
| 2 | tailor-is-admin | 8100 | 8111 | 8100 | ⚠️ 需修改 |
| 3 | tailor-is-user | 8101 | 8101 | 8101 | ✅ |
| 4 | tailor-is-product | 8102 | 8102 | 8102 | ⚠️ 冲突 |
| 5 | tailor-is-merchant | 8102 | 8102 | 需调整 | ⚠️ 冲突 |
| 6 | tailor-is-order | 8103 | 8103 | 8103 | ✅ |
| 7 | tailor-is-payment | 8104 | 8104 | 8104 | ✅ |
| 8 | tailor-is-marketing | 8105 | 8106 | 8105 | ⚠️ 需修改 |
| 9 | tailor-is-ai | 8106 | 8107 | 8106 | ⚠️ 需修改 |
| 10 | tailor-is-copyright | 8107 | 8108 | 8107 | ⚠️ 需修改 |
| 11 | tailor-is-community | 8108 | 8109 | 8108 | ⚠️ 需修改 |
| 12 | tailor-is-supply | 8109 | 8110 | 8109 | ⚠️ 需修改 |
| 13 | tailor-is-message | 8110 | 8111 | 8110 | ⚠️ 需修改 |
| 14 | tailor-is-message-im | 8111 | 8114 | 8111 | ⚠️ 需修改 |
| 15 | tailor-is-academy | 8112 | 8116 | 8112 | ⚠️ 需修改 |
| 16 | tailor-is-analytics | 8113 | 8116 | 8113 | ⚠️ 需修改 |
| - | tailor-is-pattern | - | 8114 | 暂不部署 | ❌ |

---

## 四、问题诊断

### 4.1 已发现的端口冲突

```
冲突1: tailor-is-product (8102)  与  tailor-is-merchant (8102)
冲突2: tailor-is-admin (8111)    与  tailor-is-message (8111)  与  tailor-is-message-im (8111)
冲突3: tailor-is-academy (8116)   与  tailor-is-analytics (8116)
冲突4: tailor-is-pattern (8114)   与  tailor-is-message-im (8114)
```

### 4.2 与官方规范错位

```yaml
# 当前错位 (示例)
tailor-is-marketing:  port 8106  # 应为 8105
tailor-is-ai:         port 8107  # 应为 8106
tailor-is-copyright:  port 8108  # 应为 8107
tailor-is-community:  port 8109  # 应为 8108
tailor-is-supply:     port 8110  # 应为 8109
tailor-is-message:    port 8111  # 应为 8110
tailor-is-message-im: port 8114  # 应为 8111
tailor-is-academy:    port 8116  # 应为 8112
tailor-is-analytics:  port 8116  # 应为 8113
tailor-is-admin:      port 8111  # 应为 8100
```

### 4.3 缺失配置
- `tailor-is-pattern`: 重复占用 8114（与 message-im 冲突）
- 部分服务缺少 `host: 0.0.0.0` 配置（容器化时无法外部访问）

---

## 五、规范化解决方案

### 5.1 端口重规划（16服务完整分配）

> 严格按 `platform-admin/DashboardView.vue` 的官方定义

```yaml
# 基础设施层（不占微服务端口）
infrastructure:
  mysql: 3306
  redis: 6379
  nacos: 8848
  rabbitmq: 5672
  elasticsearch: 9200
  prometheus: 9090
  grafana: 3000

# API网关层
gateway:
  - name: tailor-is-gateway
    port: 8080
    protocol: HTTP

# 业务服务层 (按官方顺序)
business-services:
  - name: tailor-is-admin          # 运营管理后台
    port: 8100

  - name: tailor-is-user           # 用户中心
    port: 8101

  - name: tailor-is-product        # 商品中心
    port: 8102

  - name: tailor-is-order          # 订单中心
    port: 8103

  - name: tailor-is-payment        # 支付中心
    port: 8104

  - name: tailor-is-marketing      # 营销中心
    port: 8105

  - name: tailor-is-ai             # AI智能
    port: 8106

  - name: tailor-is-copyright      # 版权保护
    port: 8107

  - name: tailor-is-community      # 社区互动
    port: 8108

  - name: tailor-is-supply         # 供应链
    port: 8109

  - name: tailor-is-merchant       # 商家管理
    port: 8110

  - name: tailor-is-message        # 消息中心
    port: 8111

  - name: tailor-is-academy        # 在线学院
    port: 8112

  - name: tailor-is-analytics      # 数据分析
    port: 8113

  - name: tailor-is-message-im     # IM即时通讯
    port: 8114

# 预留端口 (未来扩展)
reserved:
  - 8115-8119  # 未来新业务
  - 8120-8129  # 第二业务集群
```

### 5.2 路由规范化（Gateway层）

```yaml
# Spring Cloud Gateway 路由配置标准
gateway-routes:
  # 用户中心
  - id: user-route
    uri: lb://tailor-is-user
    predicates:
      - Path=/api/v1/user/**,/api/v1/auth/**
    filters:
      - StripPrefix=2  # 去除 /api/v1，user服务只看到 /user/**

  # 商品中心
  - id: product-route
    uri: lb://tailor-is-product
    predicates:
      - Path=/api/v1/product/**,/api/v1/category/**,/api/v1/sku/**
    filters:
      - StripPrefix=2

  # 订单中心
  - id: order-route
    uri: lb://tailor-is-order
    predicates:
      - Path=/api/v1/order/**,/api/v1/cart/**
    filters:
      - StripPrefix=2

  # 支付中心
  - id: payment-route
    uri: lb://tailor-is-payment
    predicates:
      - Path=/api/v1/payment/**,/api/v1/refund/**,/api/v1/settlement/**,/api/v1/escrow/**
    filters:
      - StripPrefix=2

  # 营销中心
  - id: marketing-route
    uri: lb://tailor-is-marketing
    predicates:
      - Path=/api/v1/marketing/**,/api/v1/coupon/**,/api/v1/points/**,/api/v1/seckill/**
    filters:
      - StripPrefix=2

  # AI智能
  - id: ai-route
    uri: lb://tailor-is-ai
    predicates:
      - Path=/api/v1/ai/**,/api/v1/pattern/**,/api/v1/body-size/**
    filters:
      - StripPrefix=2

  # 版权保护
  - id: copyright-route
    uri: lb://tailor-is-copyright
    predicates:
      - Path=/api/v1/copyright/**
    filters:
      - StripPrefix=2

  # 社区互动
  - id: community-route
    uri: lb://tailor-is-community
    predicates:
      - Path=/api/v1/community/**,/api/v1/post/**,/api/v1/comment/**
    filters:
      - StripPrefix=2

  # 供应链
  - id: supply-route
    uri: lb://tailor-is-supply
    predicates:
      - Path=/api/v1/supply/**
    filters:
      - StripPrefix=2

  # 商家管理
  - id: merchant-route
    uri: lb://tailor-is-merchant
    predicates:
      - Path=/api/v1/merchant/**,/api/v1/shop/**
    filters:
      - StripPrefix=2

  # 消息中心
  - id: message-route
    uri: lb://tailor-is-message
    predicates:
      - Path=/api/v1/message/**
    filters:
      - StripPrefix=2

  # 学院
  - id: academy-route
    uri: lb://tailor-is-academy
    predicates:
      - Path=/api/v1/academy/**
    filters:
      - StripPrefix=2

  # 分析
  - id: analytics-route
    uri: lb://tailor-is-analytics
    predicates:
      - Path=/api/v1/analytics/**
    filters:
      - StripPrefix=2

  # IM
  - id: im-route
    uri: lb://tailor-is-message-im
    predicates:
      - Path=/api/v1/im/**
    filters:
      - StripPrefix=2

  # 管理后台
  - id: admin-route
    uri: lb://tailor-is-admin
    predicates:
      - Path=/api/v1/admin/**
    filters:
      - StripPrefix=2
```

### 5.3 服务内部URL规范

```yaml
# 每个服务的Controller应该遵循的URL格式
url-convention:
  pattern: /api/v1/{resource}
  examples:
    - POST   /api/v1/users              # 创建用户
    - GET    /api/v1/users/{id}         # 查询用户
    - PUT    /api/v1/users/{id}         # 更新用户
    - DELETE /api/v1/users/{id}         # 删除用户
    - GET    /api/v1/users              # 列表/分页
    - POST   /api/v1/users/login        # 登录（特例）
    - POST   /api/v1/users/logout       # 登出（特例）
```

---

## 六、执行计划

### 阶段1: 修复端口冲突（立即执行）
- [ ] 调整 9 个服务的 `application.yml` 端口配置
- [ ] 统一添加 `host: 0.0.0.0` 配置
- [ ] 移除 `tailor-is-pattern` 重复定义（8114）

### 阶段2: 统一运行环境配置
- [ ] 创建 `/tmp/services-runtime.yml` 模板
- [ ] 所有服务使用同一外部配置覆盖
- [ ] 注入 AES_KEY / JWT_SECRET 等环境变量

### 阶段3: 启动顺序编排
```
依赖顺序:
1. 基础设施 (MySQL/Redis/Nacos/RabbitMQ)         ← 已就绪
2. 用户服务 (tailor-is-user: 8101)               ← 基础
3. 商品服务 (tailor-is-product: 8102)
4. 营销服务 (tailor-is-marketing: 8105)
5. 商家服务 (tailor-is-merchant: 8110)
6. 订单服务 (tailor-is-order: 8103)               ← 依赖 user/product
7. 支付服务 (tailor-is-payment: 8104)              ← 依赖 order
8. 版权服务 (tailor-is-copyright: 8107)
9. 社区服务 (tailor-is-community: 8108)
10. 供应链 (tailor-is-supply: 8109)
11. AI服务 (tailor-is-ai: 8106)
12. 消息 (tailor-is-message: 8111)
13. IM (tailor-is-message-im: 8114)
14. 学院 (tailor-is-academy: 8112)
15. 分析 (tailor-is-analytics: 8113)
16. 运营管理 (tailor-is-admin: 8100)
17. API网关 (tailor-is-gateway: 8080)             ← 最后
```

### 阶段4: 验证 & 报告
- [ ] 所有服务健康检查 UP
- [ ] Nacos 中显示所有服务
- [ ] 路由可达性测试

---

## 七、禁止条款

> **违反以下条款的代码/配置一律拒绝合并**

1. ❌ 禁止硬编码端口到代码中（必须通过 `application.yml` 配置）
2. ❌ 禁止在 `application.yml` 中使用 `localhost`（应使用 `${HOST:localhost}`）
3. ❌ 禁止服务之间直接通过 IP 调用（必须通过 Nacos 服务名）
4. ❌ 禁止Controller直接使用 `@RequestMapping("/user/list")` （必须 `@RequestMapping("/api/v1/users")`）
5. ❌ 禁止在生产环境使用 8000-8099 端口（保留给基础设施）
6. ❌ 禁止私自占用未分配端口（如 8120+）
7. ❌ 禁止将数据库密码硬编码（必须通过环境变量）
8. ❌ 禁止多个服务共享同一个端口

---

## 八、配套脚本

将提供以下自动化脚本：
- `apply-port-standard.sh` - 一键应用端口标准
- `start-services-by-deps.sh` - 按依赖顺序启动
- `health-check-all.sh` - 全服务健康检查
- `port-collision-detector.sh` - 端口冲突检测

---

## 九、Gateway 路由权威配置（16 服务完整版）

> ⚠️ **关键约束**：以下路由是项目最终标准，所有 `application.yml` 与 `GatewayRouteConfig.java` 必须**完全对齐**此表

### 9.1 完整路由列表（17 条，含 pattern）

| # | route id | uri | predicates | filters |
|---|----------|-----|------------|---------|
| 1 | user-route | lb://tailor-is-user | /api/user/**, /api/auth/** | StripPrefix=0 |
| 2 | product-route | lb://tailor-is-product | /api/product/**, /api/favorite/** | StripPrefix=0 |
| 3 | order-route | lb://tailor-is-order | /api/order/**, /api/cart/** | StripPrefix=0 |
| 4 | payment-route | lb://tailor-is-payment | /api/payment/**, /api/settlement/**, /api/account/**, /api/sandbox/** | StripPrefix=0 |
| 5 | marketing-route | lb://tailor-is-marketing | /api/marketing/**, /api/coupon/**, /api/points/**, /api/seckill/** | StripPrefix=0 |
| 6 | ai-route | lb://tailor-is-ai | /api/ai/**, /api/body-size/** | StripPrefix=0 |
| 7 | copyright-route | lb://tailor-is-copyright | /api/copyright/** | StripPrefix=0 |
| 8 | community-route | lb://tailor-is-community | /api/community/**, /api/post/**, /api/comment/** | StripPrefix=0 |
| 9 | supply-route | lb://tailor-is-supply | /api/supply/** | StripPrefix=0 |
| 10 | merchant-route | lb://tailor-is-merchant | /api/merchant/**, /api/shop/** | StripPrefix=0 |
| 11 | message-route | lb://tailor-is-message | /api/message/**, /api/notice/** | StripPrefix=0 |
| 12 | im-route | lb://tailor-is-message-im | /api/im/**, /api/im-message/** | StripPrefix=0 |
| 13 | academy-route | lb://tailor-is-academy | /api/academy/**, /api/course/** | StripPrefix=0 |
| 14 | analytics-route | lb://tailor-is-analytics | /api/analytics/**, /api/metrics/**, /api/dashboard/** | StripPrefix=0 |
| 15 | admin-route | lb://tailor-is-admin | /api/admin/** | StripPrefix=0 |
| 16 | pattern-route | lb://tailor-is-pattern | /api/pattern/** | StripPrefix=0 |
| 17 | file-route | lb://tailor-is-common | /api/file/**, /api/upload/** | StripPrefix=0 |

### 9.2 Gateway 配置硬性约束

1. **唯一来源**：路由**只能**在 `GatewayRouteConfig.java` 中定义，**严禁**在 `application.yml` 重复定义
2. **uri 格式**：必须使用 `lb://tailor-is-{service-name}` 服务发现形式，**严禁** `http://localhost:PORT`
3. **route id 命名**：统一使用 `{purpose}-route`（如 `user-route`），便于跨模块理解
4. **predicates 必填**：所有路由必须至少一个 Path 谓词，**禁止**空 predicates（会捕获所有流量）
5. **filters**：内部调用使用 `StripPrefix=0`（保持原 path），外部访问可加 `StripPrefix=1/2`

### 9.3 路径前缀规范

| 类别 | 前缀 | 用途 | 示例 |
|------|------|------|------|
| 业务 C 端 | `/api/{service}/**` | 用户端访问 | `/api/product/list` |
| 管理端 | `/api/admin/**` | 平台管理后台 | `/api/admin/merchant/audit` |
| 商家端 | `/api/merchant/**` | 商家后台 | `/api/merchant/shop/update` |
| 文件 | `/api/file/**` | 文件上传下载 | `/api/file/upload` |
| 监控 | `/actuator/**` | 健康检查 | `/actuator/health` |
| 内部 | `/inner/**` | 服务间调用（不走网关） | `/inner/order/create` |

---

## 十、CI/CD 防护策略

### 10.1 pre-commit 钩子（强制）

```bash
#!/bin/bash
# .git/hooks/pre-commit

# 1. 端口冲突检测
bash deploy/scripts/port-collision-detector.sh || exit 1

# 2. 端口规范校验
bash deploy/scripts/apply-port-standard.sh --check || exit 1

# 3. 路由单源校验（gateway）
if grep -q "spring.cloud.gateway.routes" tailor-is-gateway/src/main/resources/application.yml; then
  echo "❌ 禁止在 gateway/application.yml 中定义路由，必须仅在 GatewayRouteConfig.java"
  exit 1
fi

# 4. 禁止字段注入
if grep -rE "@Autowired\s*$" --include="*.java" tailor-is-*/src/main/java/; then
  echo "❌ 禁止 @Autowired 字段注入，请使用构造器注入"
  exit 1
fi

# 5. 禁止 System.out
if grep -rE "System\.(out|err)\.print" --include="*.java" tailor-is-*/src/main/java/; then
  echo "❌ 禁止使用 System.out/err.print，请使用 SLF4J Logger"
  exit 1
fi

echo "✅ pre-commit 检查通过"
```

### 10.2 CI Pipeline 强制门禁

```yaml
# .github/workflows/ci.yml (新增检查步骤)
- name: 端口冲突检测
  run: bash deploy/scripts/port-collision-detector.sh

- name: 端口规范检查
  run: bash deploy/scripts/apply-port-standard.sh --check

- name: 路由单源校验
  run: |
    if grep -q "routes:" tailor-is-gateway/src/main/resources/application.yml; then
      echo "Gateway 路由必须在 Java 中定义"
      exit 1
    fi

- name: SonarQube 扫描
  run: mvn sonar:sonar -Dsonar.qualitygate.wait=true

- name: 测试覆盖率门禁（>=30%）
  run: mvn verify -Pcoverage
```

---

**版本**: v1.1
**生效日期**: 2026-06-09
**维护人**: Tailor IS 架构组
