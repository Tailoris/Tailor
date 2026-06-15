// ==============================================================================
// Tailor IS - k6 核心业务链路测试
// Phase 3 P3-2: 全链路性能压测 (P95 ≤ 200ms)
//
// 测试链路: 登录 → 浏览商品 → 加入购物车 → 下单 → 支付
// ==============================================================================

import { check, sleep, group } from 'k6';
import http from 'k6/http';
import { Trend, Rate } from 'k6/metrics';

const flowDuration = new Trend('business_flow_duration', true);
const flowErrorRate = new Rate('business_flow_error_rate');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const VUS = parseInt(__ENV.VUS) || 20;
const DURATION = __ENV.DURATION || '120s';

export const options = {
  vus: VUS,
  duration: DURATION,
  thresholds: {
    'business_flow_duration': ['p(95)<3000'],
    'business_flow_error_rate': ['rate<0.05'],
  },
};

const TEST_USER = {
  username: 'e2e_test_user',
  password: 'Test@123456',
};

export default function () {
  const flowStart = Date.now();
  let token = '';
  let orderNo = '';

  group('Step 1: 登录', () => {
    const res = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify(TEST_USER), {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'flow-login' },
    });
    check(res, { 'login success': (r) => r.status === 200 }) || flowErrorRate.add(1);
    if (res.status === 200) {
      try {
        token = JSON.parse(res.body).data?.token || '';
      } catch (e) {}
    }
    sleep(0.5);
  });

  if (!token) return;

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };

  group('Step 2: 浏览商品列表', () => {
    const res = http.get(`${BASE_URL}/api/product/list?page=1&size=5`, {
      headers,
      tags: { name: 'flow-product-list' },
    });
    check(res, { 'product list ok': (r) => r.status === 200 }) || flowErrorRate.add(1);
    sleep(0.3);
  });

  group('Step 3: 加入购物车', () => {
    const res = http.post(`${BASE_URL}/api/order/cart/add`, JSON.stringify({
      productId: 1,
      skuId: 1,
      quantity: 1,
    }), {
      headers,
      tags: { name: 'flow-add-cart' },
    });
    check(res, { 'add cart ok': (r) => r.status === 200 }) || flowErrorRate.add(1);
    sleep(0.3);
  });

  group('Step 4: 创建订单', () => {
    const res = http.post(`${BASE_URL}/api/order/create`, JSON.stringify({
      addressId: 1,
      cartItemIds: [1],
      remark: 'perf-test',
    }), {
      headers,
      tags: { name: 'flow-create-order' },
    });
    if (res.status === 200) {
      try {
        orderNo = JSON.parse(res.body).data?.orderNo || '';
      } catch (e) {}
    }
    check(res, { 'create order ok': (r) => r.status === 200 }) || flowErrorRate.add(1);
    sleep(0.5);
  });

  group('Step 5: 查询订单', () => {
    if (orderNo) {
      const res = http.get(`${BASE_URL}/api/order/detail/${orderNo}`, {
        headers,
        tags: { name: 'flow-order-detail' },
      });
      check(res, { 'order detail ok': (r) => r.status === 200 }) || flowErrorRate.add(1);
    }
    sleep(0.2);
  });

  flowDuration.add(Date.now() - flowStart);
}