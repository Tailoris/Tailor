# Tailor IS 平台 API 使用指南

> **版本**: v1.0  
> **更新日期**: 2026-06-09  
> **基础 URL**: `https://api.tailoris.com`  
> **网关端口**: 8080

本指南覆盖 Tailor IS 微服务平台所有核心 API 的调用方法。所有接口通过 API Gateway 统一路由分发，下游包含 16 个微服务。

---

## 目录

1. [通用说明](#1-通用说明)
2. [认证接口](#2-认证接口)
3. [用户接口](#3-用户接口)
4. [商品接口](#4-商品接口)
5. [订单接口](#5-订单接口)
6. [购物车接口](#6-购物车接口)
7. [支付接口](#7-支付接口)
8. [营销接口](#8-营销接口)
9. [商户接口](#9-商户接口)
10. [社区接口](#10-社区接口)
11. [版权接口](#11-版权接口)
12. [消息接口](#12-消息接口)
13. [文件上传接口](#13-文件上传接口)
14. [管理后台接口](#14-管理后台接口)
15. [其他服务接口速查](#15-其他服务接口速查)
16. [错误码速查表](#16-错误码速查表)

---

## 1. 通用说明

### 1.1 统一响应格式 `Result<T>`

所有接口统一返回 `Result<T>` 结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | 业务状态码，200 表示成功 |
| `message` | string | 提示信息 |
| `data` | T | 业务数据，失败时为 `null` |

**成功响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "zhangsan"
  }
}
```

**失败响应示例**：

```json
{
  "code": 4001,
  "message": "订单不存在",
  "data": null
}
```

### 1.2 分页请求 `PageRequest`

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `pageNum` | int | 否 | 1 | 页码，从 1 开始 |
| `pageSize` | int | 否 | 10 | 每页数量，最大 100 |
| `sortBy` | string | 否 | - | 排序字段 |
| `sortOrder` | string | 否 | desc | 排序方向: `asc` / `desc` |

### 1.3 分页响应 `PageResponse<T>`

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 1000,
    "pageNum": 1,
    "pageSize": 20,
    "totalPages": 50,
    "list": []
  }
}
```

### 1.4 认证方式

需要认证的接口在请求头中添加 JWT Token：

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

Token 通过登录接口获取。免认证接口包括：登录、注册、短信验证码、健康检查、公开商品列表。

### 1.5 限流说明

敏感接口已接入 `@RateLimit` 注解限流：

| 接口 | 限流规则 |
|------|----------|
| 登录 | 10 次/秒，容量 60 |
| 注册 | 5 次/秒，容量 60 |
| 短信验证码 | 1 次/秒，容量 60 |
| 微信登录 | 5 次/秒，容量 30 |

### 1.6 微服务路由映射

| 路径前缀 | 下游服务 | 服务端口 |
|----------|----------|----------|
| `/api/auth/**`, `/api/user/**` | tailor-is-user | 8101 |
| `/api/product/**`, `/api/favorite/**` | tailor-is-product | 8102 |
| `/api/order/**`, `/api/cart/**` | tailor-is-order | 8103 |
| `/api/payment/**`, `/api/settlement/**` | tailor-is-payment | 8104 |
| `/api/marketing/**`, `/api/coupon/**` | tailor-is-marketing | 8105 |
| `/api/ai/**`, `/api/body-size/**` | tailor-is-ai | 8106 |
| `/api/copyright/**` | tailor-is-copyright | 8107 |
| `/api/community/**`, `/api/post/**` | tailor-is-community | 8108 |
| `/api/supply/**` | tailor-is-supply | 8109 |
| `/api/merchant/**`, `/api/shop/**` | tailor-is-merchant | 8110 |
| `/api/message/**`, `/api/notice/**` | tailor-is-message | 8111 |
| `/api/academy/**`, `/api/course/**` | tailor-is-academy | 8112 |
| `/api/analytics/**`, `/api/metrics/**` | tailor-is-analytics | 8113 |
| `/api/im/**`, `/api/im-message/**` | tailor-is-message-im | 8114 |
| `/api/pattern/**` | tailor-is-pattern | 8115 |
| `/api/admin/**` | tailor-is-admin | 8100 |

---

## 2. 认证接口

**服务**: tailor-is-user  
**基础路径**: `/api/auth`

### 2.1 用户登录

```
POST /api/auth/login
Content-Type: application/json
```

**请求体**：

```json
{
  "username": "zhangsan",
  "password": "Abc123456"
}
```

> 支持用户名或手机号 + 密码登录。登录失败 5 次将锁定账号 30 分钟。

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "userInfo": {
      "id": 1001,
      "username": "zhangsan",
      "phone": "138****8000",
      "nickname": "张三",
      "avatar": "https://example.com/avatar.jpg",
      "realNameAuthStatus": 1
    }
  }
}
```

**认证要求**: 无

---

### 2.2 用户注册

```
POST /api/auth/register
Content-Type: application/json
```

**请求体**：

```json
{
  "phone": "13800138000",
  "password": "Abc123456",
  "smsCode": "123456",
  "username": "zhangsan"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `phone` | string | 是 | 手机号，需符合中国手机号格式 |
| `password` | string | 是 | 密码，至少 8 位，含大小写字母和数字 |
| `smsCode` | string | 是 | 短信验证码 |
| `username` | string | 是 | 用户名 |

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**认证要求**: 无

---

### 2.3 发送短信验证码

```
POST /api/auth/sms-code
Content-Type: application/json
```

**请求体**：

```json
{
  "phone": "13800138000"
}
```

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**认证要求**: 无

---

### 2.4 微信授权登录

```
POST /api/auth/wechat-login
Content-Type: application/json
```

**请求体**：

```json
{
  "type": "MINI",
  "jsCode": "081aBcDe2fGhIj3kLmN4"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `type` | string | 是 | `MP`(公众号) 或 `MINI`(小程序) |
| `jsCode` | string | 是 | wx.login() 获取的临时登录凭证 |

**响应**：同 [2.1 用户登录](#21-用户登录)

**认证要求**: 无

---

### 2.5 用户登出

```
POST /api/auth/logout
Authorization: Bearer {token}
```

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**认证要求**: 需要登录

---

### 2.6 刷新 Token

```
POST /api/auth/refresh
Authorization: Bearer {token}
```

**说明**: Token 有效期 30 分钟（1800 秒），通过此接口可在过期前主动刷新。刷新成功后旧 Token 立即失效。

**响应**：同 [2.1 用户登录](#21-用户登录)

**认证要求**: 需要有效 Token

---

## 3. 用户接口

**服务**: tailor-is-user  
**基础路径**: `/api/user`

### 3.1 获取当前用户信息

```
GET /api/user/info
Authorization: Bearer {token}
```

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1001,
    "username": "zhangsan",
    "phone": "138****8000",
    "nickname": "张三",
    "avatar": "https://example.com/avatar.jpg",
    "gender": 1,
    "birthday": "1990-01-01",
    "realNameAuthStatus": 1,
    "createTime": "2024-01-01T10:00:00",
    "lastLoginTime": "2024-06-01T15:30:00"
  }
}
```

**认证要求**: 需要登录

---

### 3.2 更新用户信息

```
PUT /api/user/info
Authorization: Bearer {token}
Content-Type: application/json
```

**请求体**：

```json
{
  "nickname": "张三",
  "avatar": "https://example.com/new-avatar.jpg",
  "gender": 1,
  "birthday": "1990-01-01"
}
```

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**认证要求**: 需要登录

---

### 3.3 实名认证

```
PUT /api/user/real-name-auth
Authorization: Bearer {token}
Content-Type: application/json
```

**请求体**：

```json
{
  "realName": "张三",
  "idCard": "110101199001011234"
}
```

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**认证要求**: 需要登录

---

### 3.4 收货地址管理

**基础路径**: `/api/user/address`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `GET` | `/api/user/address` | 获取地址列表 | 是 |
| `GET` | `/api/user/address/default` | 获取默认地址 | 是 |
| `POST` | `/api/user/address` | 新增地址 | 是 |
| `PUT` | `/api/user/address/{id}` | 更新地址 | 是 |
| `DELETE` | `/api/user/address/{id}` | 删除地址 | 是 |
| `PUT` | `/api/user/address/{id}/default` | 设为默认地址 | 是 |

**新增地址请求示例**：

```
POST /api/user/address
Authorization: Bearer {token}
Content-Type: application/json

{
  "receiverName": "张三",
  "receiverPhone": "13800138000",
  "province": "北京市",
  "city": "北京市",
  "district": "朝阳区",
  "detailAddress": "建国路88号",
  "isDefault": false
}
```

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

## 4. 商品接口

**服务**: tailor-is-product  
**基础路径**: `/api/product`

### 4.1 创建商品

```
POST /api/product
Authorization: Bearer {token}
Content-Type: application/json
```

**请求体**：

```json
{
  "name": "高级定制衬衫",
  "description": "优质面料，量身定制，舒适透气",
  "price": 299.00,
  "originalPrice": 599.00,
  "categoryId": 10,
  "productType": "CUSTOM",
  "shopId": 1,
  "images": [
    "https://example.com/img1.jpg",
    "https://example.com/img2.jpg"
  ],
  "mainImage": "https://example.com/main.jpg",
  "tags": ["新品", "热销"]
}
```

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**认证要求**: 需要登录

---

### 4.2 查询商品详情

```
GET /api/product/{id}
```

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1001,
    "name": "高级定制衬衫",
    "description": "优质面料，量身定制",
    "price": 299.00,
    "originalPrice": 599.00,
    "categoryId": 10,
    "productType": "CUSTOM",
    "shopId": 1,
    "status": 1,
    "mainImage": "https://example.com/main.jpg",
    "images": ["https://example.com/img1.jpg"],
    "tags": ["新品", "热销"],
    "salesCount": 128,
    "createTime": "2024-01-01T10:00:00",
    "updateTime": "2024-01-05T12:00:00"
  }
}
```

**认证要求**: 无

---

### 4.3 商品列表

```
GET /api/product/list?pageNum=1&pageSize=20&categoryId=10
```

**查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `pageNum` | int | 否 | 页码，默认 1 |
| `pageSize` | int | 否 | 每页数量，默认 10 |
| `categoryId` | long | 否 | 分类 ID |
| `shopId` | long | 否 | 店铺 ID |

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 520,
    "pageNum": 1,
    "pageSize": 20,
    "totalPages": 26,
    "list": [
      {
        "id": 1001,
        "name": "高级定制衬衫",
        "price": 299.00,
        "mainImage": "https://example.com/main.jpg",
        "salesCount": 128
      }
    ]
  }
}
```

**认证要求**: 无

---

### 4.4 店铺商品列表

```
GET /api/product/shop/{shopId}?pageNum=1&pageSize=20
```

**认证要求**: 无

---

### 4.5 搜索商品

```
GET /api/product/search?keyword=衬衫&categoryId=10&pageNum=1&pageSize=20
```

**认证要求**: 无

---

### 4.6 更新商品状态

```
PUT /api/product/{id}/status
Authorization: Bearer {token}
Content-Type: application/json

{
  "status": 0
}
```

> `status`: 0=下架, 1=上架

**认证要求**: 需要登录

---

### 4.7 商品 SKU 管理

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `POST` | `/api/product/sku/{productId}` | 为商品创建 SKU | 是 |
| `PUT` | `/api/product/sku/{id}` | 更新 SKU | 是 |
| `DELETE` | `/api/product/sku/{id}` | 删除 SKU | 是 |
| `GET` | `/api/product/sku/product/{productId}` | 查询商品 SKU 列表 | 否 |
| `PUT` | `/api/product/sku/{skuId}/stock` | 更新 SKU 库存 | 是 |

---

### 4.8 商品评价

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `POST` | `/api/product/review` | 发表评价 | 是 |
| `GET` | `/api/product/review/{productId}` | 查询商品评价列表 | 否 |
| `DELETE` | `/api/product/review/{id}` | 删除评价 | 是 |
| `PUT` | `/api/product/review/{id}/audit` | 审核评价 | 是(管理员) |

---

### 4.9 商品收藏

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `POST` | `/api/favorite/{productId}` | 收藏商品 | 是 |
| `POST` | `/api/favorite/cancel/{productId}` | 取消收藏 | 是 |
| `GET` | `/api/favorite/check/{productId}` | 检查是否已收藏 | 是 |
| `GET` | `/api/favorite/list` | 我的收藏列表 | 是 |

---

### 4.10 商品分类

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `GET` | `/api/product/category/tree` | 获取分类树 | 否 |
| `GET` | `/api/product/category/list` | 获取分类列表 | 否 |
| `POST` | `/api/product/category` | 创建分类 | 是(管理员) |
| `PUT` | `/api/product/category/{id}` | 更新分类 | 是(管理员) |
| `PUT` | `/api/product/category/{id}/status` | 更新分类状态 | 是(管理员) |

---

## 5. 订单接口

**服务**: tailor-is-order  
**基础路径**: `/api/order`

### 5.1 创建订单

```
POST /api/order
Authorization: Bearer {token}
Content-Type: application/json
```

**请求体**：

```json
{
  "items": [
    {
      "productId": 1001,
      "skuId": 2001,
      "quantity": 2,
      "price": 299.00
    }
  ],
  "addressId": 3001,
  "couponId": 4001,
  "remark": "请尽快发货"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `items` | array | 是 | 订单商品明细 |
| `items[].productId` | long | 是 | 商品 ID |
| `items[].skuId` | long | 是 | SKU ID |
| `items[].quantity` | int | 是 | 购买数量 |
| `addressId` | long | 是 | 收货地址 ID |
| `couponId` | long | 否 | 优惠券 ID |
| `remark` | string | 否 | 订单备注 |

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": "ORD20260609123456789"
}
```

**认证要求**: 需要登录

---

### 5.2 查询订单列表

```
GET /api/order/list?pageNum=1&pageSize=20&status=1
Authorization: Bearer {token}
```

**查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `pageNum` | int | 否 | 页码 |
| `pageSize` | int | 否 | 每页数量 |
| `status` | int | 否 | 订单状态筛选 |

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 50,
    "pageNum": 1,
    "pageSize": 20,
    "totalPages": 3,
    "list": [
      {
        "id": 1,
        "orderNo": "ORD20260609123456789",
        "userId": 1001,
        "status": 1,
        "totalAmount": 598.00,
        "payAmount": 548.00,
        "addressId": 3001,
        "remark": "请尽快发货",
        "createTime": "2026-06-09T12:00:00"
      }
    ]
  }
}
```

**认证要求**: 需要登录

---

### 5.3 查询订单详情

```
GET /api/order/{orderNo}
Authorization: Bearer {token}
```

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "orderNo": "ORD20260609123456789",
    "userId": 1001,
    "status": 2,
    "totalAmount": 598.00,
    "payAmount": 548.00,
    "discountAmount": 50.00,
    "items": [
      {
        "productId": 1001,
        "productName": "高级定制衬衫",
        "skuId": 2001,
        "quantity": 2,
        "price": 299.00,
        "subtotal": 598.00
      }
    ],
    "address": {
      "receiverName": "张三",
      "receiverPhone": "138****8000",
      "province": "北京市",
      "city": "北京市",
      "district": "朝阳区",
      "detailAddress": "建国路88号"
    },
    "createTime": "2026-06-09T12:00:00",
    "payTime": "2026-06-09T12:05:00"
  }
}
```

**认证要求**: 需要登录

---

### 5.4 支付订单

```
PUT /api/order/{orderNo}/pay?payType=1
Authorization: Bearer {token}
```

**查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `payType` | int | 是 | 支付方式: 1=微信, 2=支付宝 |

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**认证要求**: 需要登录

---

### 5.5 确认收货

```
PUT /api/order/{orderNo}/confirm
Authorization: Bearer {token}
```

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**认证要求**: 需要登录

---

### 5.6 取消订单

```
PUT /api/order/{orderNo}/cancel?reason=不想要了
Authorization: Bearer {token}
```

**认证要求**: 需要登录

---

## 6. 购物车接口

**服务**: tailor-is-order  
**基础路径**: `/api/cart`

### 6.1 添加到购物车

```
POST /api/cart
Authorization: Bearer {token}
Content-Type: application/json

{
  "productId": 1001,
  "skuId": 2001,
  "quantity": 2
}
```

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**认证要求**: 需要登录

---

### 6.2 查询购物车列表

```
GET /api/cart
Authorization: Bearer {token}
```

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "userId": 1001,
      "productId": 1001,
      "productName": "高级定制衬衫",
      "skuId": 2001,
      "quantity": 2,
      "price": 299.00,
      "selected": true,
      "createTime": "2026-06-09T10:00:00"
    }
  ]
}
```

**认证要求**: 需要登录

---

### 6.3 更新购物车商品

```
PUT /api/cart/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
  "quantity": 3
}
```

**认证要求**: 需要登录

---

### 6.4 删除购物车商品

```
DELETE /api/cart/{id}
Authorization: Bearer {token}
```

**认证要求**: 需要登录

---

### 6.5 批量结算

```
POST /api/cart/checkout
Authorization: Bearer {token}
Content-Type: application/json

[1, 2, 3]
```

> 传入需要结算的购物车项 ID 列表

**认证要求**: 需要登录

---

## 7. 支付接口

**服务**: tailor-is-payment  
**基础路径**: `/api/v1/payment`

### 7.1 微信支付

```
POST /api/v1/payment/wechat
Authorization: Bearer {token}
Content-Type: application/json
```

**请求体**：

```json
{
  "orderId": "ORD20260609123456789",
  "amount": 548.00,
  "openId": "oUpF8uMuAJOM2b4p9a8wB4Z5F8",
  "body": "Tailor IS 订单支付"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `orderId` | string | 是 | 订单编号 |
| `amount` | decimal | 是 | 支付金额（元） |
| `openId` | string | 是 | 微信 OpenID |
| `body` | string | 否 | 商品描述 |

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "appId": "wx1234567890abcdef",
    "timeStamp": "1717891200",
    "nonceStr": "5K8264ILTKCH16CQ2502SI8ZNMTM67VS",
    "package": "prepay_id=wx20260609120000abcdef1234567890",
    "signType": "RSA",
    "paySign": "oR9d8PuhnIc+YZ8cBHFCwfgpaK9gd7vaRvkYD7rthRAZ...",
    "paymentNo": "PAY20260609120000",
    "orderId": "ORD20260609123456789"
  }
}
```

**认证要求**: 需要登录

---

### 7.2 支付宝支付

```
POST /api/v1/payment/alipay
Authorization: Bearer {token}
Content-Type: application/json
```

**请求体**：

```json
{
  "orderId": "ORD20260609123456789",
  "amount": 548.00,
  "subject": "Tailor IS 订单支付",
  "body": "高级定制衬衫 x2"
}
```

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": "<form id='alipaysubmit' name='alipaysubmit' action='https://openapi.alipay.com/...' method='POST'>...</form>"
}
```

> 返回 HTML 表单字符串，前端直接提交即可跳转支付宝收银台。

**认证要求**: 需要登录

---

### 7.3 查询支付状态

```
GET /api/v1/payment/status?paymentNo=PAY20260609120000
Authorization: Bearer {token}
```

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "paymentNo": "PAY20260609120000",
    "orderId": "ORD20260609123456789",
    "userId": 1001,
    "amount": 548.00,
    "payMethod": "wechat",
    "payStatus": 2,
    "transactionId": "4200001234567890",
    "createTime": "2026-06-09T12:00:00",
    "payTime": "2026-06-09T12:01:30"
  }
}
```

> `payStatus`: 0=待支付, 1=支付中, 2=已支付, 3=已退款, 4=已关闭

**认证要求**: 需要登录

---

### 7.4 申请退款

```
POST /api/v1/payment/refund?paymentNo=PAY20260609120000&refundAmount=299.00&refundReason=商品质量问题
Authorization: Bearer {token}
```

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "trade_no": "2026060922001234567890",
    "out_refund_no": "REF1717891200000",
    "refundNo": "REF1717891200000"
  }
}
```

**认证要求**: 需要登录

---

### 7.5 微信支付回调（系统接口）

```
POST /api/v1/payment/wechat/callback
```

> 由微信支付平台异步调用，用户无需主动调用。回调处理具有幂等性保护。

---

### 7.6 支付宝支付回调（系统接口）

```
POST /api/v1/payment/alipay/callback
```

> 由支付宝平台异步调用，用户无需主动调用。

---

## 8. 营销接口

**服务**: tailor-is-marketing  
**基础路径**: `/api/marketing`

### 8.1 优惠券管理

**基础路径**: `/api/marketing/coupon`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `POST` | `/api/marketing/coupon/create` | 创建优惠券模板 | 是 |
| `POST` | `/api/marketing/coupon/receive` | 领取优惠券 | 是 |
| `POST` | `/api/marketing/coupon/use` | 使用优惠券 | 是 |
| `GET` | `/api/marketing/coupon/list` | 我的优惠券列表 | 是 |
| `GET` | `/api/marketing/coupon/available` | 可用优惠券查询 | 是 |
| `GET` | `/api/marketing/coupon/detail` | 优惠券模板详情 | 否 |

**领取优惠券示例**：

```
POST /api/marketing/coupon/receive
Authorization: Bearer {token}
Content-Type: application/json

{
  "couponId": 100
}
```

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

### 8.2 秒杀活动

**基础路径**: `/api/marketing/seckill`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `GET` | `/api/marketing/seckill/list` | 秒杀活动列表 | 否 |
| `GET` | `/api/marketing/seckill/{id}` | 秒杀活动详情 | 否 |
| `GET` | `/api/marketing/seckill/{id}/products` | 秒杀商品列表 | 否 |
| `POST` | `/api/marketing/seckill/{id}/kill` | 参与秒杀 | 是 |

---

### 8.3 拼团

**基础路径**: `/api/marketing/group-buy`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `GET` | `/api/marketing/group-buy/list` | 拼团活动列表 | 否 |
| `GET` | `/api/marketing/group-buy/{id}` | 拼团活动详情 | 否 |
| `POST` | `/api/marketing/group-buy/{id}/join` | 参与拼团 | 是 |

---

### 8.4 积分

**基础路径**: `/api/marketing/points`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `GET` | `/api/marketing/points/balance` | 查询积分余额 | 是 |
| `GET` | `/api/marketing/points/records` | 积分明细 | 是 |

---

### 8.5 会员

**基础路径**: `/api/marketing/member`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `GET` | `/api/marketing/member/info` | 会员信息 | 是 |
| `GET` | `/api/marketing/member/levels` | 会员等级列表 | 否 |

---

## 9. 商户接口

**服务**: tailor-is-merchant  
**基础路径**: `/api/merchant`

### 9.1 商户入驻

```
POST /api/merchant/apply
Content-Type: application/json

{
  "name": "张三服饰店",
  "contactName": "张三",
  "contactPhone": "13800138000",
  "businessLicense": "https://example.com/license.jpg",
  "idCardFront": "https://example.com/id-front.jpg",
  "idCardBack": "https://example.com/id-back.jpg",
  "description": "专业定制服装"
}
```

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**认证要求**: 需要登录

---

### 9.2 查询商户信息

```
GET /api/merchant/info
Authorization: Bearer {token}
```

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "name": "张三服饰店",
    "status": 1,
    "contactName": "张三",
    "contactPhone": "138****8000",
    "description": "专业定制服装",
    "auditStatus": 1,
    "createTime": "2024-01-01T10:00:00"
  }
}
```

**认证要求**: 需要登录

---

### 9.3 商户列表

```
GET /api/merchant/list?pageNum=1&pageSize=20
```

**认证要求**: 无

---

### 9.4 店铺管理

**基础路径**: `/api/merchant/shop`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `POST` | `/api/merchant/shop` | 创建店铺 | 是 |
| `PUT` | `/api/merchant/shop/{id}` | 更新店铺 | 是 |
| `GET` | `/api/merchant/shop/{id}` | 店铺详情 | 否 |
| `GET` | `/api/merchant/shop/list/{merchantId}` | 商户店铺列表 | 否 |
| `PUT` | `/api/merchant/shop/{id}/status` | 更新店铺状态 | 是 |

---

### 9.5 商户 Dashboard

**基础路径**: `/api/v1/merchant/dashboard`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `GET` | `/api/v1/merchant/dashboard/summary` | 经营概览 | 是 |
| `GET` | `/api/v1/merchant/dashboard/summary/shop/{shopId}` | 店铺概览 | 是 |
| `GET` | `/api/v1/merchant/dashboard/today` | 今日数据 | 是 |
| `GET` | `/api/v1/merchant/dashboard/trend` | 经营趋势 | 是 |

---

### 9.6 员工管理

**基础路径**: `/api/merchant/employee`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `POST` | `/api/merchant/employee` | 添加员工 | 是 |
| `DELETE` | `/api/merchant/employee/{id}` | 删除员工 | 是 |
| `GET` | `/api/merchant/employee/list` | 员工列表 | 是 |
| `GET` | `/api/merchant/employee/list/{shopId}` | 店铺员工列表 | 是 |
| `GET` | `/api/merchant/employee/permission` | 权限检查 | 是 |

---

## 10. 社区接口

**服务**: tailor-is-community  
**基础路径**: `/api/community`

### 10.1 帖子管理

**基础路径**: `/api/community/post`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `POST` | `/api/community/post/create` | 创建帖子 | 是 |
| `PUT` | `/api/community/post/update/{postId}` | 更新帖子 | 是 |
| `DELETE` | `/api/community/post/delete/{postId}` | 删除帖子 | 是 |
| `GET` | `/api/community/post/detail/{postId}` | 帖子详情 | 否 |
| `GET` | `/api/community/post/list?pageNum=1&pageSize=20` | 帖子列表 | 否 |
| `GET` | `/api/community/post/my` | 我的帖子 | 是 |
| `POST` | `/api/community/post/top/{postId}` | 置顶帖子 | 是 |
| `POST` | `/api/community/post/essence/{postId}` | 设为精华 | 是 |

**创建帖子示例**：

```
POST /api/community/post/create
Authorization: Bearer {token}
Content-Type: application/json

{
  "title": "我的定制经验分享",
  "content": "今天分享一下我的定制经验...",
  "images": ["https://example.com/img1.jpg"],
  "topicId": 1,
  "tags": ["经验分享", "定制"]
}
```

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

### 10.2 评论

**基础路径**: `/api/community/comment`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `POST` | `/api/community/comment/create` | 发表评论 | 是 |
| `GET` | `/api/community/comment/list/{postId}` | 评论列表 | 否 |
| `DELETE` | `/api/community/comment/delete/{commentId}` | 删除评论 | 是 |
| `GET` | `/api/community/comment/my` | 我的评论 | 是 |

---

### 10.3 互动操作

**基础路径**: `/api/community/interaction`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `POST` | `/api/community/interaction/like` | 点赞 | 是 |
| `DELETE` | `/api/community/interaction/unlike` | 取消点赞 | 是 |
| `POST` | `/api/community/interaction/favorite` | 收藏 | 是 |
| `DELETE` | `/api/community/interaction/unfavorite` | 取消收藏 | 是 |
| `POST` | `/api/community/interaction/follow` | 关注用户 | 是 |
| `DELETE` | `/api/community/interaction/unfollow` | 取消关注 | 是 |
| `GET` | `/api/community/interaction/is-liked` | 检查点赞状态 | 是 |
| `GET` | `/api/community/interaction/is-favorited` | 检查收藏状态 | 是 |
| `GET` | `/api/community/interaction/is-followed` | 检查关注状态 | 是 |

---

### 10.4 发现页

**基础路径**: `/api/community/discover`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `GET` | `/api/community/discover/hot` | 热门帖子 | 否 |
| `GET` | `/api/community/discover/latest` | 最新帖子 | 否 |
| `GET` | `/api/community/discover/following` | 关注的人帖子 | 是 |
| `GET` | `/api/community/discover/topic/{topicId}` | 话题帖子 | 否 |
| `GET` | `/api/community/discover/recommend` | 推荐帖子 | 否 |
| `GET` | `/api/community/discover/topics/hot` | 热门话题 | 否 |
| `GET` | `/api/community/discover/user/{userId}` | 用户帖子 | 否 |
| `GET` | `/api/community/discover/search?keyword=xxx` | 搜索帖子 | 否 |

---

### 10.5 话题

**基础路径**: `/api/community/topic`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `POST` | `/api/community/topic` | 创建话题 | 是(管理员) |
| `PUT` | `/api/community/topic/{id}` | 更新话题 | 是(管理员) |
| `DELETE` | `/api/community/topic/{id}` | 删除话题 | 是(管理员) |
| `GET` | `/api/community/topic/{id}` | 话题详情 | 否 |
| `GET` | `/api/community/topic/hot` | 热门话题 | 否 |
| `GET` | `/api/community/topic/list` | 话题列表 | 否 |

---

### 10.6 举报

**基础路径**: `/api/community/report`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `POST` | `/api/community/report` | 提交举报 | 是 |
| `GET` | `/api/community/report/list` | 举报列表 | 是(管理员) |

---

### 10.7 黑名单

**基础路径**: `/api/community/block`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `POST` | `/api/community/block` | 加入黑名单 | 是 |
| `DELETE` | `/api/community/block/{blockedUserId}` | 移除黑名单 | 是 |
| `GET` | `/api/community/block/list` | 黑名单列表 | 是 |

---

## 11. 版权接口

**服务**: tailor-is-copyright  
**基础路径**: `/api/v1/copyright`

### 11.1 版权登记

```
POST /api/v1/copyright/register
Authorization: Bearer {token}
Content-Type: application/json

{
  "title": "原创服装设计图案",
  "description": "2026夏季系列原创图案设计",
  "contentHash": "sha256:a1b2c3d4e5f6...",
  "fileUrl": "https://example.com/design.png",
  "category": "PATTERN"
}
```

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "title": "原创服装设计图案",
    "registerNo": "CR202606090001",
    "contentHash": "sha256:a1b2c3d4e5f6...",
    "blockchainTxHash": "0xabc123...",
    "status": 1,
    "createTime": "2026-06-09T12:00:00"
  }
}
```

**认证要求**: 需要登录

---

### 11.2 版权验证

```
GET /api/v1/copyright/verify?registerNo=CR202606090001
```

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "valid": true,
    "title": "原创服装设计图案",
    "registerNo": "CR202606090001",
    "blockchainTxHash": "0xabc123...",
    "registerTime": "2026-06-09T12:00:00"
  }
}
```

**认证要求**: 无

---

### 11.3 版权证书

```
GET /api/v1/copyright/certificate?registerNo=CR202606090001
Authorization: Bearer {token}
```

**认证要求**: 需要登录

---

### 11.4 我的版权列表

```
GET /api/v1/copyright/list?pageNum=1&pageSize=20
Authorization: Bearer {token}
```

**认证要求**: 需要登录

---

### 11.5 侵权举报

**基础路径**: `/api/v1/copyright/infringement`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `POST` | `/api/v1/copyright/infringement/report` | 提交侵权举报 | 是 |
| `GET` | `/api/v1/copyright/infringement/list` | 侵权举报列表 | 是 |
| `POST` | `/api/v1/copyright/infringement/arbitration/create` | 创建仲裁 | 是 |
| `POST` | `/api/v1/copyright/infringement/arbitration/complete` | 完成仲裁 | 是 |

---

## 12. 消息接口

**服务**: tailor-is-message  
**基础路径**: `/api/message`

### 12.1 消息列表

```
GET /api/message/list?pageNum=1&pageSize=20&isRead=0&type=1
Authorization: Bearer {token}
```

**查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `pageNum` | int | 否 | 页码 |
| `pageSize` | int | 否 | 每页数量 |
| `isRead` | int | 否 | 已读状态: 0=未读, 1=已读 |
| `type` | int | 否 | 消息类型筛选 |

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 10,
    "pageNum": 1,
    "pageSize": 20,
    "totalPages": 1,
    "list": [
      {
        "id": 1,
        "userId": 1001,
        "title": "订单已发货",
        "content": "您的订单 ORD20260609123456789 已发货",
        "type": 1,
        "isRead": 0,
        "relatedType": "order",
        "relatedId": 1,
        "createTime": "2026-06-09T12:00:00"
      }
    ]
  }
}
```

**认证要求**: 需要登录

---

### 12.2 标记已读

```
POST /api/message/{messageId}/read
Authorization: Bearer {token}
```

**认证要求**: 需要登录

---

### 12.3 全部已读

```
POST /api/message/read-all
Authorization: Bearer {token}
```

**认证要求**: 需要登录

---

### 12.4 未读数量

```
GET /api/message/unread-count
Authorization: Bearer {token}
```

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": 5
}
```

**认证要求**: 需要登录

---

### 12.5 社区消息

**基础路径**: `/api/community/message`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `GET` | `/api/community/message/list` | 社区消息列表 | 是 |
| `GET` | `/api/community/message/unread-count` | 未读数量 | 是 |
| `POST` | `/api/community/message/{id}/read` | 标记已读 | 是 |
| `POST` | `/api/community/message/read-all` | 全部已读 | 是 |

---

## 13. 文件上传接口

**服务**: tailor-is-common  
**基础路径**: `/api/upload`

### 13.1 上传文件

```
POST /api/upload/{bizType}
Authorization: Bearer {token}
Content-Type: multipart/form-data

file: (binary)
```

**路径参数**：

| 参数 | 说明 |
|------|------|
| `bizType` | 业务类型: `avatar`, `product`, `certificate`, `community`, `merchant` 等 |

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "fileName": "avatar_1001_1717891200.jpg",
    "fileUrl": "https://cdn.tailoris.com/avatar/avatar_1001_1717891200.jpg",
    "fileSize": 102400,
    "contentType": "image/jpeg"
  }
}
```

**认证要求**: 需要登录

---

### 13.2 批量上传

```
POST /api/upload/{bizType}/batch
Authorization: Bearer {token}
Content-Type: multipart/form-data

files: (binary array)
```

---

### 13.3 删除文件

```
POST /api/upload/delete
Authorization: Bearer {token}
Content-Type: application/json

{
  "fileUrl": "https://cdn.tailoris.com/avatar/avatar_1001_1717891200.jpg"
}
```

**认证要求**: 需要登录

---

## 14. 管理后台接口

**服务**: tailor-is-admin  
**基础路径**: `/api/admin`

### 14.1 Dashboard

```
GET /api/admin/dashboard/stats
Authorization: Bearer {token}
```

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalUsers": 10000,
    "totalMerchants": 500,
    "totalProducts": 50000,
    "totalOrders": 100000,
    "todayOrders": 1200,
    "todayRevenue": 580000.00,
    "pendingMerchantAudit": 20,
    "pendingProductAudit": 50
  }
}
```

**认证要求**: 需要管理员权限

---

### 14.2 用户管理

**基础路径**: `/api/admin/user`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `GET` | `/api/admin/user/list` | 用户列表 | 是(管理员) |
| `GET` | `/api/admin/user/{id}` | 用户详情 | 是(管理员) |
| `PUT` | `/api/admin/user/freeze/{id}` | 冻结用户 | 是(管理员) |
| `PUT` | `/api/admin/user/unfreeze/{id}` | 解冻用户 | 是(管理员) |

---

### 14.3 商户审核

**基础路径**: `/api/admin/merchant`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `GET` | `/api/admin/merchant/pending` | 待审核商户 | 是(管理员) |
| `PUT` | `/api/admin/merchant/audit/{id}` | 审核商户 | 是(管理员) |
| `PUT` | `/api/admin/merchant/freeze/{id}` | 冻结商户 | 是(管理员) |
| `PUT` | `/api/admin/merchant/unfreeze/{id}` | 解冻商户 | 是(管理员) |
| `GET` | `/api/admin/merchant/list` | 商户列表 | 是(管理员) |
| `GET` | `/api/admin/merchant/{id}` | 商户详情 | 是(管理员) |

---

### 14.4 商品审核

**基础路径**: `/api/admin/product`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `GET` | `/api/admin/product/pending` | 待审核商品 | 是(管理员) |
| `PUT` | `/api/admin/product/audit/{id}` | 审核通过 | 是(管理员) |
| `PUT` | `/api/admin/product/reject/{id}` | 审核驳回 | 是(管理员) |

---

### 14.5 订单管理

**基础路径**: `/api/admin/order`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `GET` | `/api/admin/order/list` | 订单列表 | 是(管理员) |
| `GET` | `/api/admin/order/{orderNo}` | 订单详情 | 是(管理员) |
| `PUT` | `/api/admin/order/arbitrate` | 仲裁处理 | 是(管理员) |

---

### 14.6 社区管理

**基础路径**: `/api/admin/community`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `GET` | `/api/admin/community/reports` | 举报列表 | 是(管理员) |
| `PUT` | `/api/admin/community/report/process` | 处理举报 | 是(管理员) |
| `DELETE` | `/api/admin/community/post/{id}` | 删除帖子 | 是(管理员) |
| `PUT` | `/api/admin/community/post/audit/{id}` | 帖子审核 | 是(管理员) |

---

## 15. 其他服务接口速查

### 15.1 AI 服务 (`tailor-is-ai`, 端口 8115)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `POST` | `/api/ai/pattern/generate` | AI 生成图案 | 是 |
| `POST` | `/api/ai/pattern/check` | AI 图案检查 | 是 |
| `POST` | `/api/ai/pattern/iterate` | AI 图案迭代优化 | 是 |
| `POST` | `/api/body-size/analyze` | 体型数据分析 | 是 |

### 15.2 供应链服务 (`tailor-is-supply`, 端口 8109)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `GET` | `/api/supply/order/list` | 供应订单列表 | 是 |
| `POST` | `/api/supply/order/create` | 创建供应订单 | 是 |
| `GET` | `/api/supply/material/list` | 材料列表 | 否 |
| `POST` | `/api/supply/material/create` | 添加材料 | 是 |

### 15.3 学院服务 (`tailor-is-academy`, 端口 8112)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `GET` | `/api/academy/course/list` | 课程列表 | 否 |
| `GET` | `/api/academy/course/{id}` | 课程详情 | 否 |
| `GET` | `/api/course/{id}/chapters` | 课程章节 | 否 |
| `POST` | `/api/academy/course/create` | 创建课程 | 是 |

### 15.4 数据分析服务 (`tailor-is-analytics`, 端口 8113)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `GET` | `/api/analytics/user-behavior` | 用户行为分析 | 是 |
| `GET` | `/api/metrics/dashboard` | 指标看板 | 是 |
| `GET` | `/api/metrics/trend` | 趋势数据 | 是 |
| `GET` | `/api/dashboard/stats` | 看板统计 | 是 |

### 15.5 IM 即时通讯 (`tailor-is-message-im`, 端口 8114)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `GET` | `/api/im/conversations` | 会话列表 | 是 |
| `POST` | `/api/im/conversation/create` | 创建会话 | 是 |
| `GET` | `/api/im/messages/{conversationId}` | 消息历史 | 是 |
| `POST` | `/api/im/message/send` | 发送消息 | 是 |
| `GET` | `/api/im-message/unread-count` | 未读消息数 | 是 |

### 15.6 图案服务 (`tailor-is-pattern`, 端口 8115)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `GET` | `/api/pattern/list` | 图案列表 | 否 |
| `GET` | `/api/pattern/{id}` | 图案详情 | 否 |
| `POST` | `/api/pattern/create` | 上传图案 | 是 |
| `PUT` | `/api/pattern/{id}` | 更新图案 | 是 |

---

## 16. 错误码速查表

### 16.1 通用错误码

| 错误码 | HTTP 状态 | 说明 |
|--------|-----------|------|
| `0` | 200 | 操作成功 |
| `200` | 200 | 操作成功（兼容码） |
| `400` | 400 | 请求参数错误 |
| `401` | 401 | 未登录 |
| `403` | 403 | 无权限 |
| `404` | 404 | 资源不存在 |
| `500` | 500 | 操作失败 |
| `4000` | 400 | 业务异常 |
| `5000` | 500 | 系统异常 |

### 16.2 系统级错误码

| 错误码 | 说明 |
|--------|------|
| `1000` | 系统内部错误 |
| `1001` | 参数校验失败 |
| `1002` | 资源不存在 |

### 16.3 认证相关错误码

| 错误码 | 说明 |
|--------|------|
| `2000` | 未登录或 Token 已过期 |
| `2001` | 权限不足 |
| `2002` | Token 已过期，请重新登录 |
| `2003` | Token 无效 |

### 16.4 业务模块错误码

| 错误码 | 说明 |
|--------|------|
| `3001` | 订单不存在 |
| `3002` | 商品不存在 |
| `3003` | 库存不足 |
| `3004` | 支付失败 |
| `3005` | 余额不足 |
| `3006` | 商户不存在 |
| `3007` | 版权已存在 |

---

## 附录

### A. curl 快速测试

```bash
# 登录
curl -X POST https://api.tailoris.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"zhangsan","password":"Abc123456"}'

# 携带 Token 请求
curl -X GET https://api.tailoris.com/api/user/info \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."

# 创建商品
curl -X POST https://api.tailoris.com/api/product \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..." \
  -H "Content-Type: application/json" \
  -d '{"name":"定制衬衫","price":299,"categoryId":10,"shopId":1}'

# 创建订单
curl -X POST https://api.tailoris.com/api/order \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..." \
  -H "Content-Type: application/json" \
  -d '{"items":[{"productId":1001,"skuId":2001,"quantity":2,"price":299}],"addressId":3001}'

# 微信支付
curl -X POST https://api.tailoris.com/api/v1/payment/wechat \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..." \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORD20260609123456789","amount":548,"openId":"oUpF8uMuAJOM2b4p9a8wB4Z5F8"}'
```

### B. Swagger 文档

启动服务后访问 `http://localhost:8080/swagger-ui.html` 查看在线 API 文档。

### C. 常用 HTTP 请求头

| 头部 | 说明 | 示例 |
|------|------|------|
| `Authorization` | JWT Bearer Token | `Bearer eyJhbG...` |
| `Content-Type` | 请求体格式 | `application/json` |
| `Accept` | 期望响应格式 | `application/json` |
| `X-Request-Id` | 请求追踪 ID | UUID |
