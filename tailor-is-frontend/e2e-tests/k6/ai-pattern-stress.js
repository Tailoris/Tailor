/**
 * Tailor IS - AI 纸样生成压力测试 (k6)
 *
 * 测试场景：
 *   - 50 并发用户同时生成纸样
 *   - 30 秒内从 1 用户逐步增加到 50 用户
 *   - 验证：响应时间 < 5 秒 (p95)
 *   - 报告：p95, p99 延迟
 *
 * 运行方式：
 *   k6 run ai-pattern-stress.js
 *   k6 run --out json=results.json ai-pattern-stress.js
 */

import http from 'k6/http'
import { check, sleep, group } from 'k6'
import { Trend, Rate, Counter } from 'k6/metrics'

// ======================== 配置 ========================

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api'
const API_PREFIX = __ENV.API_PREFIX || '/front'

// 自定义指标
const patternGenDuration = new Trend('pattern_generation_duration', true)
const patternGenSuccessRate = new Rate('pattern_generation_success_rate')
const patternGenRequests = new Counter('pattern_generation_requests')

// 测试配置
export const options = {
  stages: [
    // 逐步增加负载
    { duration: '30s', target: 50 },   // 30 秒内增加到 50 用户
    { duration: '1m', target: 50 },    // 保持 50 用户 1 分钟
    { duration: '30s', target: 0 },    // 30 秒内逐步降为 0
  ],
  thresholds: {
    // p95 响应时间 < 5 秒
    'pattern_generation_duration': ['p(95)<5000'],
    // 成功率 > 95%
    'pattern_generation_success_rate': ['rate>0.95'],
    // HTTP 请求失败率 < 1%
    'http_req_failed': ['rate<0.01'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
}

// ======================== 测试数据 ========================

const MEASUREMENTS = [
  { chest: 96, waist: 82, hip: 100, shoulder: 44, height: 175, style: 'business' },
  { chest: 88, waist: 72, hip: 94, shoulder: 40, height: 165, style: 'casual' },
  { chest: 104, waist: 90, hip: 108, shoulder: 48, height: 182, style: 'formal' },
  { chest: 92, waist: 78, hip: 98, shoulder: 42, height: 170, style: 'slim' },
  { chest: 100, waist: 86, hip: 104, shoulder: 46, height: 178, style: 'loose' },
]

// 模拟认证 token
function getAuthToken(vuId) {
  // 实际场景中应从登录接口获取，这里用模拟 token
  return `test-token-vu-${vuId}-${Date.now()}`
}

// ======================== 测试函数 ========================

export default function () {
  const vuId = __VU
  const token = getAuthToken(vuId)

  group('AI Pattern Generation Flow', function () {
    // Step 1: 获取纸样生成配置
    const configRes = http.get(`${BASE_URL}${API_PREFIX}/pattern/config`, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      tags: { name: 'getPatternConfig' },
    })

    check(configRes, {
      'config status is 200': (r) => r.status === 200,
    })

    // Step 2: 提交纸样生成请求
    const measurement = MEASUREMENTS[vuId % MEASUREMENTS.length]

    const genStart = Date.now()
    const genRes = http.post(`${BASE_URL}${API_PREFIX}/pattern/generate`, JSON.stringify({
      measurements: measurement,
      fabricType: 'cotton',
      patternType: 'shirt',
      options: {
        includeSeamAllowance: true,
        outputFormat: 'dxf',
      },
    }), {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      tags: { name: 'generatePattern' },
      timeout: '30s',  // 纸样生成可能较慢
    })
    const genDuration = Date.now() - genStart

    patternGenDuration.add(genDuration)
    patternGenRequests.add(1)

    const genSuccess = check(genRes, {
      'generation status is 200': (r) => r.status === 200 || r.status === 202,
      'response has pattern data': (r) => {
        if (r.status === 200) {
          const body = JSON.parse(r.body)
          return body.data && (body.data.patternUrl || body.data.taskId)
        }
        return true  // 202 异步处理也算成功
      },
    })

    patternGenSuccessRate.add(genSuccess)

    // Step 3: 如果是异步任务，轮询获取结果
    if (genRes.status === 202) {
      const body = JSON.parse(genRes.body)
      const taskId = body.data?.taskId

      if (taskId) {
        let completed = false
        let pollCount = 0
        const maxPolls = 30  // 最多轮询 30 次（共 150 秒）

        while (!completed && pollCount < maxPolls) {
          sleep(5)  // 每 5 秒轮询一次

          const pollRes = http.get(`${BASE_URL}${API_PREFIX}/pattern/task/${taskId}`, {
            headers: {
              'Authorization': `Bearer ${token}`,
            },
            tags: { name: 'pollPatternTask' },
          })

          if (pollRes.status === 200) {
            const pollBody = JSON.parse(pollRes.body)
            if (pollBody.data?.status === 'completed') {
              completed = true
              check(pollRes, {
                'task completed with pattern': (r) => {
                  const b = JSON.parse(r.body)
                  return b.data?.patternUrl != null
                },
              })
            } else if (pollBody.data?.status === 'failed') {
              completed = true
              console.error(`Pattern generation failed for task ${taskId}`)
            }
          }

          pollCount++
        }
      }
    }
  })

  // 模拟用户思考时间
  sleep(Math.random() * 3 + 1)
}

// ======================== 结果摘要 ========================

export function handleSummary(data) {
  const summary = {
    timestamp: new Date().toISOString(),
    test_name: 'AI Pattern Generation Stress Test',
    configuration: {
      base_url: BASE_URL,
      target_vus: 50,
      ramp_up_duration: '30s',
    },
    results: {
      total_requests: data.metrics.pattern_generation_requests?.values?.count || 0,
      success_rate: data.metrics.pattern_generation_success_rate?.values?.rate || 0,
      duration_p95_ms: data.metrics.pattern_generation_duration?.values?.['p(95)'] || 0,
      duration_p99_ms: data.metrics.pattern_generation_duration?.values?.['p(99)'] || 0,
      duration_avg_ms: data.metrics.pattern_generation_duration?.values?.avg || 0,
      duration_min_ms: data.metrics.pattern_generation_duration?.values?.min || 0,
      duration_max_ms: data.metrics.pattern_generation_duration?.values?.max || 0,
      http_req_failed_rate: data.metrics.http_req_failed?.values?.rate || 0,
      http_req_duration_p95_ms: data.metrics.http_req_duration?.values?.['p(95)'] || 0,
      http_req_duration_p99_ms: data.metrics.http_req_duration?.values?.['p(99)'] || 0,
    },
    thresholds: {
      'p95 < 5s': (data.metrics.pattern_generation_duration?.values?.['p(95)'] || 0) < 5000 ? 'PASS' : 'FAIL',
      'success_rate > 95%': (data.metrics.pattern_generation_success_rate?.values?.rate || 0) > 0.95 ? 'PASS' : 'FAIL',
    },
  }

  return {
    'stdout': JSON.stringify(summary, null, 2),
    'results/ai-pattern-summary.json': JSON.stringify(summary, null, 2),
  }
}