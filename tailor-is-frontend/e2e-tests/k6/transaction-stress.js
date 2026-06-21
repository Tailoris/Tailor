/**
 * Tailor IS - 高并发下单压力测试 (k6)
 *
 * 测试场景：
 *   - 200 并发用户同时下单
 *   - 60 秒内从 1 用户逐步增加到 200 用户
 *   - 验证：成功率 > 95%
 *   - 报告：p95, p99 延迟
 *
 * 运行方式：
 *   k6 run transaction-stress.js
 *   k6 run --out json=results.json transaction-stress.js
 */

import http from 'k6/http'
import { check, sleep, group } from 'k6'
import { Trend, Rate, Counter } from 'k6/metrics'

// ======================== 配置 ========================

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api'
const API_PREFIX = __ENV.API_PREFIX || '/front'

// 自定义指标
const orderCreateDuration = new Trend('order_create_duration', true)
const orderSuccessRate = new Rate('order_success_rate')
const orderRequests = new Counter('order_requests')
const orderSuccessCount = new Counter('order_success_count')

// 测试配置
export const options = {
  stages: [
    // 逐步增加负载
    { duration: '60s', target: 200 },  // 60 秒内增加到 200 用户
    { duration: '2m', target: 200 },   // 保持 200 用户 2 分钟
    { duration: '60s', target: 0 },    // 60 秒内逐步降为 0
  ],
  thresholds: {
    // 成功率 > 95%
    'order_success_rate': ['rate>0.95'],
    // HTTP 请求失败率 < 5%
    'http_req_failed': ['rate<0.05'],
    // p95 响应时间 < 10 秒
    'http_req_duration': ['p(95)<10000'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
}

// ======================== 测试数据 ========================

// 模拟商品数据（应从数据库预加载真实商品 ID）
const PRODUCTS = [
  { productId: 10001, skuId: 20001, quantity: 1 },
  { productId: 10002, skuId: 20002, quantity: 2 },
  { productId: 10003, skuId: 20003, quantity: 1 },
  { productId: 10004, skuId: 20004, quantity: 1 },
  { productId: 10005, skuId: 20005, quantity: 3 },
]

// 模拟收货地址
const ADDRESS = {
  receiverName: '测试用户',
  receiverPhone: '13800138000',
  province: '广东省',
  city: '深圳市',
  district: '南山区',
  detail: '科技园路1号',
}

// 模拟用户 token
function getAuthToken(vuId) {
  return `test-order-token-vu-${vuId}-${Date.now()}`
}

// ======================== 测试函数 ========================

export default function () {
  const vuId = __VU

  group('High Concurrency Order Flow', function () {
    // Step 1: 用户登录获取 token
    const token = getAuthToken(vuId)

    // Step 2: 添加商品到购物车
    const product = PRODUCTS[vuId % PRODUCTS.length]
    const cartRes = http.post(`${BASE_URL}${API_PREFIX}/cart/add`, JSON.stringify({
      productId: product.productId,
      skuId: product.skuId,
      quantity: product.quantity,
    }), {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      tags: { name: 'addToCart' },
    })

    check(cartRes, {
      'cart add success': (r) => r.status === 200,
    })

    // Step 3: 创建订单（核心压测点）
    const orderStart = Date.now()
    const requestId = `k6-order-${vuId}-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`

    const orderRes = http.post(`${BASE_URL}${API_PREFIX}/order/create`, JSON.stringify({
      cartIds: [],  // 实际场景中应使用购物车返回的 cartId
      addressId: 1,
      addressSnapshot: ADDRESS,
      remark: 'k6 stress test',
      couponId: null,
      promotionId: null,
      requestId: requestId,
    }), {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
        'X-Request-Id': requestId,
      },
      tags: { name: 'createOrder' },
      timeout: '30s',
    })
    const orderDuration = Date.now() - orderStart

    orderCreateDuration.add(orderDuration)
    orderRequests.add(1)

    const orderSuccess = check(orderRes, {
      'order create status is 200': (r) => r.status === 200,
      'order has orderNo': (r) => {
        if (r.status === 200) {
          try {
            const body = JSON.parse(r.body)
            return body.data && body.data.orderNo
          } catch {
            return false
          }
        }
        return false
      },
    })

    if (orderSuccess) {
      orderSuccessCount.add(1)
    }
    orderSuccessRate.add(orderSuccess)

    // 记录订单号用于后续验证
    if (orderRes.status === 200) {
      try {
        const body = JSON.parse(r.body)
        const orderNo = body.data?.orderNo
        if (orderNo) {
          // Step 4: 查询订单详情（验证订单创建成功）
          sleep(0.5)
          const detailRes = http.get(`${BASE_URL}${API_PREFIX}/order/detail/${orderNo}`, {
            headers: { 'Authorization': `Bearer ${token}` },
            tags: { name: 'getOrderDetail' },
          })

          check(detailRes, {
            'order detail found': (r) => r.status === 200,
          })
        }
      } catch {
        // 忽略解析错误
      }
    }

    // Step 5: 模拟支付（可选，根据业务需要）
    // 注：支付接口通常需要 mock 或使用测试支付通道
    if (orderRes.status === 200) {
      try {
        const body = JSON.parse(orderRes.body)
        const orderNo = body.data?.orderNo
        if (orderNo && Math.random() < 0.3) {  // 30% 用户立即支付
          sleep(0.5)
          const payRes = http.post(`${BASE_URL}${API_PREFIX}/order/pay`, JSON.stringify({
            orderNo: orderNo,
            payType: 1,  // 微信支付
          }), {
            headers: {
              'Authorization': `Bearer ${token}`,
              'Content-Type': 'application/json',
            },
            tags: { name: 'payOrder' },
          })

          check(payRes, {
            'pay status is 200': (r) => r.status === 200,
          })
        }
      } catch {
        // 忽略
      }
    }
  })

  // 模拟用户思考时间
  sleep(Math.random() * 2 + 0.5)
}

// ======================== 结果摘要 ========================

export function handleSummary(data) {
  const totalRequests = data.metrics.order_requests?.values?.count || 0
  const successCount = data.metrics.order_success_count?.values?.count || 0
  const successRate = data.metrics.order_success_rate?.values?.rate || 0

  const summary = {
    timestamp: new Date().toISOString(),
    test_name: 'High Concurrency Order Stress Test',
    configuration: {
      base_url: BASE_URL,
      target_vus: 200,
      ramp_up_duration: '60s',
      total_duration: '4m',
    },
    results: {
      total_order_requests: totalRequests,
      successful_orders: successCount,
      success_rate: successRate,
      success_rate_percent: (successRate * 100).toFixed(2) + '%',
      order_create_duration: {
        avg_ms: data.metrics.order_create_duration?.values?.avg || 0,
        min_ms: data.metrics.order_create_duration?.values?.min || 0,
        med_ms: data.metrics.order_create_duration?.values?.med || 0,
        p95_ms: data.metrics.order_create_duration?.values?.['p(95)'] || 0,
        p99_ms: data.metrics.order_create_duration?.values?.['p(99)'] || 0,
        max_ms: data.metrics.order_create_duration?.values?.max || 0,
      },
      http: {
        req_duration_p95_ms: data.metrics.http_req_duration?.values?.['p(95)'] || 0,
        req_duration_p99_ms: data.metrics.http_req_duration?.values?.['p(99)'] || 0,
        req_failed_rate: data.metrics.http_req_failed?.values?.rate || 0,
        total_http_requests: data.metrics.http_reqs?.values?.count || 0,
        throughput_rps: data.metrics.http_reqs?.values?.rate || 0,
      },
    },
    thresholds: {
      'success_rate > 95%': successRate > 0.95 ? 'PASS' : 'FAIL',
      'http_req_duration p95 < 10s': (data.metrics.http_req_duration?.values?.['p(95)'] || 0) < 10000 ? 'PASS' : 'FAIL',
      'http_req_failed < 5%': (data.metrics.http_req_failed?.values?.rate || 0) < 0.05 ? 'PASS' : 'FAIL',
    },
  }

  return {
    'stdout': JSON.stringify(summary, null, 2),
    'results/transaction-summary.json': JSON.stringify(summary, null, 2),
  }
}