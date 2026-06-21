# Tailor IS API 接口总览

> 版本：v1.0 | 更新时间：2026-06-20 | 基于 Knife4j (OpenAPI 3.0)

## API 版本管理

- **API 版本策略**：URL 路径版本控制（`/api/v1/`）
- **当前版本**：v1（默认使用 `/api/` 前缀兼容旧版）
- **文档地址**：启动后访问 `http://<host>:<port>/doc.html` 查看 Knife4j 接口文档
- **OpenAPI JSON**：`http://<host>:<port>/v3/api-docs`

## 模块概览

| 模块 | 服务名 | API 前缀 | 说明 |
|------|--------|----------|------|
| 用户模块 | tailor-is-user | `/api/auth/`、`/api/user/` | 认证、用户管理、地址管理、角色管理 |
| 商品模块 | tailor-is-product | `/api/product/`、`/api/admin/product/`、`/api/favorite/` | 商品管理、搜索、分类、收藏、评价 |
| 订单模块 | tailor-is-order | `/api/order/`、`/api/cart/`、`/api/admin/` | 订单、购物车、物流、售后 |
| 支付模块 | tailor-is-payment | `/api/payment/` | 支付、退款、提现 |
| 商家模块 | tailor-is-merchant | `/api/merchant/` | 商家入驻、管理 |
| 营销模块 | tailor-is-marketing | `/api/marketing/` | 优惠券、拼团、会员 |
| AI 制版 | tailor-is-ai | `/api/ai/` | AI 量体、版型生成、版型迭代 |
| 版权模块 | tailor-is-copyright | `/api/copyright/` | 区块链版权存证 |
| 社区模块 | tailor-is-community | `/api/community/` | 内容社区 |
| 即时通讯 | tailor-is-message-im | `/api/im/` | 即时通讯 |
| 消息模块 | tailor-is-message | `/api/message/` | 站内消息 |
| 管理后台 | tailor-is-admin | `/api/admin/` | 平台管理后台 |
| 供应链 | tailor-is-supply | `/api/supply/` | 供应链匹配 |

---

## 一、用户模块 (tailor-is-user)

### 1.1 认证管理 — `/api/auth/`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/auth/login` | 用户登录（用户名/手机号+密码） | 否 |
| POST | `/api/auth/register` | 用户注册（手机号+密码+短信验证码） | 否 |
| POST | `/api/auth/wechat-login` | 微信授权登录（公众号/小程序） | 否 |
| POST | `/api/auth/sms-code` | 发送短信验证码 | 否 |
| POST | `/api/auth/logout` | 用户登出 | 是 |
| POST | `/api/auth/refresh` | 刷新 Token | 是 |

### 1.2 用户管理 — `/api/user/`（已弃用，请使用 `/api/v1/user/`）

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/user/info` | 获取当前用户信息 | 是 |
| PUT | `/api/user/info` | 更新用户信息（昵称、头像、性别、生日） | 是 |
| PUT | `/api/user/real-name-auth` | 实名认证 | 是 |
| GET | `/api/user/list` | 用户列表（管理端分页） | admin |

### 1.3 地址管理 — `/api/user/address/`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/user/address` | 获取地址列表 | 是 |
| GET | `/api/user/address/default` | 获取默认地址 | 是 |
| POST | `/api/user/address` | 新增地址 | 是 |
| PUT | `/api/user/address/{id}` | 更新地址 | 是 |
| DELETE | `/api/user/address/{id}` | 删除地址 | 是 |
| PUT | `/api/user/address/{id}/default` | 设为默认地址 | 是 |

### 1.4 角色管理 — `/api/user/roles/`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/user/roles/{userId}` | 为用户分配角色 | admin |
| DELETE | `/api/user/roles/{userId}` | 移除用户的角色 | admin |

---

## 二、商品模块 (tailor-is-product)

### 2.1 商品管理 — `/api/product/`（已弃用，请使用 `/api/v1/product/`）

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/product` | 创建商品 | 是 |
| PUT | `/api/product/{id}` | 更新商品 | 是 |
| DELETE | `/api/product/{id}` | 删除商品 | 是 |
| GET | `/api/product/{id}` | 获取商品详情 | 否 |
| GET | `/api/product/list` | 查询商品列表 | 否 |
| GET | `/api/product/shop/{shopId}` | 查询店铺商品列表 | 否 |
| PUT | `/api/product/{id}/status` | 更新商品状态 | 是 |
| GET | `/api/product/type/{productType}` | 根据商品类型查询 | 否 |
| GET | `/api/product/{id}/skus` | 获取商品 SKU 列表 | 否 |
| GET | `/api/product/{id}/tags` | 获取商品标签 | 否 |

### 2.2 商品搜索 — `/api/product/search/`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/product/search` | 商品搜索（关键词、类目、价格、店铺、标签多维筛选） | 否 |

### 2.3 商品分类 — `/api/product/category/`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/product/category/tree` | 获取分类树 | 是 |
| GET | `/api/product/category/list` | 获取分类列表 | 是 |
| POST | `/api/product/category` | 创建分类 | 是 |
| PUT | `/api/product/category/{id}` | 更新分类 | 是 |
| PUT | `/api/product/category/{id}/status` | 更新分类状态 | 是 |

### 2.4 商品 SKU — `/api/product/sku/`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/product/sku/{productId}` | 创建 SKU | 是 |
| PUT | `/api/product/sku/{id}` | 更新 SKU | 是 |
| DELETE | `/api/product/sku/{id}` | 删除 SKU | 是 |
| GET | `/api/product/sku/product/{productId}` | 查询商品 SKU 列表 | 是 |
| PUT | `/api/product/sku/{skuId}/stock` | 更新库存 | 是 |

### 2.5 商品评价 — `/api/product/review/`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/product/review` | 提交商品评价 | 是 |
| GET | `/api/product/review/{productId}` | 查询商品评价列表 | 否 |
| DELETE | `/api/product/review/{id}` | 删除评价 | 是 |
| GET | `/api/product/review/admin/pending` | 查询待审核评价 | admin |
| PUT | `/api/product/review/{id}/audit` | 审核评价 | admin |

### 2.6 商品收藏 — `/api/favorite/`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/favorite/{productId}` | 添加收藏 | 是 |
| POST | `/api/favorite/cancel/{productId}` | 取消收藏 | 是 |
| GET | `/api/favorite/check/{productId}` | 判断是否已收藏 | 是 |
| GET | `/api/favorite/list` | 我的收藏列表 | 是 |

### 2.7 后台商品管理 — `/api/admin/product/`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/admin/product/list` | 查询商品列表（管理端） | admin |
| PUT | `/api/admin/product/audit/{id}` | 审核商品 | admin |
| GET | `/api/admin/product/stats` | 获取商品统计信息 | admin |

---

## 三、订单模块 (tailor-is-order)

### 3.1 订单管理 — `/api/order/`（已弃用，请使用 `/api/v1/order/`）

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/order` | 创建订单 | 是 |
| GET | `/api/order/{orderNo}` | 获取订单详情 | 是 |
| GET | `/api/order/list` | 查询订单列表 | 是 |
| PUT | `/api/order/{orderNo}/pay` | 支付订单 | 是 |
| PUT | `/api/order/{orderNo}/confirm` | 确认收货 | 是 |
| PUT | `/api/order/{orderNo}/cancel` | 取消订单 | 是 |

### 3.2 购物车 — `/api/cart/`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/cart` | 添加到购物车 | 是 |
| PUT | `/api/cart/{id}` | 更新购物车商品 | 是 |
| DELETE | `/api/cart/{id}` | 删除购物车商品 | 是 |
| GET | `/api/cart` | 查询购物车列表 | 是 |
| POST | `/api/cart/checkout` | 批量结算 | 是 |

### 3.3 物流管理 — `/api/order/`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| PUT | `/api/order/{orderId}/logistics` | 更新物流信息 | 是 |
| GET | `/api/order/{orderId}/logistics` | 获取物流信息 | 是 |
| GET | `/api/order/logistics/track` | 物流轨迹查询 | 是 |

### 3.4 售后服务 — `/api/order/after-sale/`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/order/after-sale` | 创建售后工单 | 是 |
| GET | `/api/order/after-sale/{ticketNo}` | 获取售后工单详情 | 是 |
| GET | `/api/order/after-sale/list` | 查询用户售后工单列表 | 是 |
| PUT | `/api/order/after-sale/{ticketNo}/process` | 处理售后工单 | 是 |

### 3.5 后台订单管理 — `/api/admin/`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/admin/order/list` | 后台查询订单列表 | admin |
| GET | `/api/admin/order/stats` | 订单统计 | admin |
| GET | `/api/admin/after-sale/pending` | 待处理售后工单 | admin |
| PUT | `/api/admin/after-sale/arbitrate` | 平台仲裁售后工单 | admin |

---

## 四、支付模块 (tailor-is-payment)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/payment/pay` | 发起支付 | 是 |
| POST | `/api/payment/refund` | 发起退款 | 是 |
| POST | `/api/payment/withdraw` | 商家提现 | 是 |
| GET | `/api/payment/callback/alipay` | 支付宝回调 | 否 |
| GET | `/api/payment/record` | 查询支付记录 | 是 |

---

## 五、AI 制版模块 (tailor-is-ai)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/ai/pattern/generate` | 生成版型 | 是 |
| POST | `/api/ai/pattern/iterate` | 版型迭代 | 是 |
| GET | `/api/ai/pattern/{id}` | 获取版型详情 | 是 |
| GET | `/api/ai/pattern/list` | 版型列表 | 是 |
| POST | `/api/ai/body-size/analyze` | 量体数据分析 | 是 |
| POST | `/api/ai/body-size/input` | 录入量体数据 | 是 |

---

## 通用说明

### 认证方式
- 使用 Sa-Token 框架，登录后返回 Token
- 请求头携带：`Authorization: Bearer <token>`
- 部分接口使用 `X-User-Id` 请求头传递用户 ID

### 统一响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 错误码
参见 `tailor-is-common` 模块中的 `ErrorCode.java` 和 `ResultCode.java`

### 限流
- 登录接口：IP 级别每分钟最多 10 次
- 注册接口：每分钟最多 5 次
- 短信验证码：每分钟最多 1 次
- 微信登录：每分钟最多 5 次