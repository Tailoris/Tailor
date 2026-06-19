import http from 'k6/http';
import { check, group, sleep } from 'k6';

export const options = {
  vus: 3,
  duration: '20s',
  thresholds: {
    'http_req_duration': ['p(95)<5000'],
    'http_req_failed': ['rate<0.1'],
  },
};

export default function () {
  group('Infrastructure Health', function () {
    let res = http.get('http://localhost:9090/-/healthy');
    check(res, { 'Prometheus healthy': (r) => r.status === 200 });

    res = http.get('http://localhost:8719/');
    check(res, { 'Sentinel Dashboard accessible': (r) => r.status === 200 });

    res = http.get('http://localhost:9095/health');
    check(res, { 'Alert Webhook healthy': (r) => r.status === 200 });

    res = http.get('http://localhost:3000/api/health');
    check(res, { 'Grafana accessible': (r) => r.status === 200 || r.status === 302 });
  });

  group('Frontend CDN', function () {
    let res = http.get('http://localhost:8080/');
    check(res, { 'PC Mall root OK': (r) => r.status === 200 });

    res = http.get('http://localhost:8080/merchant/');
    check(res, { 'Merchant Admin OK': (r) => r.status === 200 });

    res = http.get('http://localhost:8080/admin/');
    check(res, { 'Platform Admin OK': (r) => r.status === 200 });

    res = http.get('http://localhost:8080/healthz');
    check(res, { 'Nginx healthz OK': (r) => r.status === 200 });
  });

  group('Redis Sentinel Health', function () {
    let res;
    res = http.get('http://localhost:6390/');
    check(res, { 'Redis master reachable': (r) => r.status !== 0 });
  });

  group('Alert Webhook Alert Test', function () {
    const payload = JSON.stringify({
      version: '4',
      status: 'firing',
      commonLabels: { severity: 'warning', alertname: 'SmokeTest', cluster: 'tailor-is' },
      alerts: [{
        labels: { severity: 'warning', alertname: 'SmokeTest' },
        annotations: { description: 'k6 冒烟测试告警' },
        startsAt: new Date().toISOString(),
      }]
    });
    const params = { headers: { 'Content-Type': 'application/json' } };
    const res = http.post('http://localhost:9095/api/v1/alerts/warning', payload, params);
    check(res, { 'Alert webhook accepts warnings': (r) => r.status === 200 });
  });

  sleep(1);
}
