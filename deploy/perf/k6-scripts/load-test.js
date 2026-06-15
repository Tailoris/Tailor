// ==============================================================================
// Tailor IS - k6 负载测试脚本
// Phase 3 P3-2: 全链路性能压测 (P95 ≤ 200ms)
// ==============================================================================

import { check, sleep, group } from 'k6';
import http from 'k6/http';
import { Trend, Rate, Counter } from 'k6/metrics';

// 自定义指标
const apiResponseTime = new Trend('api_response_time', true);
const errorRate = new Rate('error_rate');
const businessTxnCount = new Counter('business_txn_count');

// 测试配置 (可通过环境变量覆盖)
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const VUS = parseInt(__ENV.VUS) || 50;
const DURATION = __ENV.DURATION || '60s';

export const options = {
  vus: VUS,
  duration: DURATION,
  thresholds: {
    // P95 响应时间 ≤ 200ms
    'http_req_duration{name:product-list}': ['p(95)<200'],
    'http_req_duration{name:user-info}': ['p(95)<200'],
    'http_req_duration{name:order-create}': ['p(95)<500'],
    // 错误率 < 1%
    error_rate: ['rate<0.01'],
    // 总请求失败率 < 1%
    'http_req_failed': ['rate<0.01'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

// 测试用户池
const TEST_USERS = Array.from({ length: 10 }, (_, i) => ({
  username: `testuser${i + 1}`,
  password: 'Test@123456',
}));

// 缓存 token
let cachedToken = '';

/**
 * 获取认证 Token
 */
function getAuthToken() {
  if (cachedToken) return cachedToken;

  const res = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
    username: TEST_USERS[0].username,
    password: TEST_USERS[0].password,
  }), {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'auth-login' },
  });

  if (res.status === 200) {
    try {
      const body = JSON.parse(res.body);
      cachedToken = body.data?.token || '';
      return cachedToken;
    } catch (e) {
      return '';
    }
  }
  return '';
}

/**
 * 带认证标记的请求头
 */
function authHeaders() {
  const token = getAuthToken();
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };
}

// ============================================================================
// 默认函数 - 混合业务场景
// ============================================================================
export default function () {
  const token = getAuthToken();

  group('01-商品浏览', () => {
    // 商品列表 (高频)
    const listRes = http.get(`${BASE_URL}/api/product/list?page=1&size=20`, {
      tags: { name: 'product-list' },
    });
    check(listRes, {
      'product-list status 200': (r) => r.status === 200,
    }) || errorRate.add(1);
    apiResponseTime.add(listRes.timings.duration);
    businessTxnCount.add(1);
    sleep(0.5);

    // 商品详情 (中频)
    const detailRes = http.get(`${BASE_URL}/api/product/1`, {
      tags: { name: 'product-detail' },
    });
    check(detailRes, {
      'product-detail status 200': (r) => r.status === 200 || r.status === 404,
    }) || errorRate.add(1);
    apiResponseTime.add(detailRes.timings.duration);
    businessTxnCount.add(1);
    sleep(0.3);
  });

  group('02-用户信息', () => {
    // 用户信息查询
    if (token) {
      const userRes = http.get(`${BASE_URL}/api/auth/userinfo`, {
        headers: authHeaders(),
        tags: { name: 'user-info' },
      });
      check(userRes, {
        'user-info status 200': (r) => r.status === 200,
      }) || errorRate.add(1);
      apiResponseTime.add(userRes.timings.duration);
      businessTxnCount.add(1);
    }
    sleep(0.2);
  });

  group('03-社区帖子', () => {
    // 社区帖子列表
    const postRes = http.get(`${BASE_URL}/api/community/post/list?page=1&size=10`, {
      tags: { name: 'community-post-list' },
    });
    check(postRes, {
      'community-post-list status 200': (r) => r.status === 200,
    }) || errorRate.add(1);
    apiResponseTime.add(postRes.timings.duration);
    businessTxnCount.add(1);
    sleep(0.3);
  });

  group('04-营销活动', () => {
    // 优惠券列表
    const couponRes = http.get(`${BASE_URL}/api/marketing/coupon/available`, {
      tags: { name: 'coupon-list' },
    });
    check(couponRes, {
      'coupon-list status 200': (r) => r.status === 200,
    }) || errorRate.add(1);
    apiResponseTime.add(couponRes.timings.duration);
    businessTxnCount.add(1);
    sleep(0.2);
  });

  group('05-商户信息', () => {
    // 商户列表
    const merchantRes = http.get(`${BASE_URL}/api/merchant/list?page=1&size=10`, {
      tags: { name: 'merchant-list' },
    });
    check(merchantRes, {
      'merchant-list status 200': (r) => r.status === 200,
    }) || errorRate.add(1);
    apiResponseTime.add(merchantRes.timings.duration);
    businessTxnCount.add(1);
    sleep(0.3);
  });
}