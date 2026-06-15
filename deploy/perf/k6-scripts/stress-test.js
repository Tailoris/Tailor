// ==============================================================================
// Tailor IS - k6 压力测试脚本 (逐步加压)
// Phase 3 P3-2: 全链路性能压测
// ==============================================================================

import { check, sleep } from 'k6';
import http from 'k6/http';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  stages: [
    { duration: '2m', target: 20 },   // 预热: 0 → 20 VUs
    { duration: '3m', target: 50 },   // 加压: 20 → 50 VUs
    { duration: '3m', target: 100 },  // 加压: 50 → 100 VUs
    { duration: '2m', target: 150 },  // 加压: 100 → 150 VUs
    { duration: '2m', target: 200 },  // 峰值: 150 → 200 VUs
    { duration: '3m', target: 0 },    // 冷却: 200 → 0 VUs
  ],
  thresholds: {
    'http_req_duration': ['p(95)<500'],
    'http_req_failed': ['rate<0.05'],
  },
};

export default function () {
  // 混合业务请求
  const requests = [
    { method: 'GET', path: '/api/product/list?page=1&size=20', weight: 30 },
    { method: 'GET', path: '/api/product/category/tree', weight: 10 },
    { method: 'GET', path: '/api/merchant/list?page=1&size=10', weight: 15 },
    { method: 'GET', path: '/api/community/post/list?page=1&size=10', weight: 15 },
    { method: 'GET', path: '/api/marketing/coupon/available', weight: 10 },
    { method: 'GET', path: '/api/product/1', weight: 10 },
    { method: 'GET', path: '/actuator/health', weight: 10 },
  ];

  // 加权随机选择
  const totalWeight = requests.reduce((sum, r) => sum + r.weight, 0);
  let random = Math.random() * totalWeight;
  let selected = requests[0];

  for (const req of requests) {
    random -= req.weight;
    if (random <= 0) {
      selected = req;
      break;
    }
  }

  const res = http.request(selected.method, `${BASE_URL}${selected.path}`);
  check(res, { 'status ok': (r) => r.status === 200 || r.status === 404 });
  sleep(0.5);
}