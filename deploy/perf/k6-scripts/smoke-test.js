// ==============================================================================
// Tailor IS - k6 冒烟测试脚本
// Phase 3 P3-2: 全链路性能压测
// ==============================================================================

import { check } from 'k6';
import http from 'k6/http';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  vus: 1,
  duration: '10s',
  thresholds: {
    'http_req_duration': ['p(95)<1000'],
    'http_req_failed': ['rate<0.1'],
  },
};

const ENDPOINTS = [
  { path: '/api/product/list?page=1&size=5', method: 'GET', name: 'product-list' },
  { path: '/api/product/category/tree', method: 'GET', name: 'category-tree' },
  { path: '/api/merchant/list?page=1&size=5', method: 'GET', name: 'merchant-list' },
  { path: '/api/community/post/list?page=1&size=5', method: 'GET', name: 'post-list' },
  { path: '/actuator/health', method: 'GET', name: 'health-check' },
];

export default function () {
  for (const ep of ENDPOINTS) {
    const res = http.request(ep.method, `${BASE_URL}${ep.path}`, null, {
      tags: { name: ep.name },
    });
    check(res, {
      [`${ep.name} status ok`]: (r) => r.status === 200 || r.status === 404,
    });
  }
}