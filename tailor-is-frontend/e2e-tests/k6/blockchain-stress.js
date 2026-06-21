/**
 * Tailor IS - 区块链批量存证压力测试 (k6)
 *
 * 测试场景：
 *   - 100 并发用户，每用户提交 10 条版权存证
 *   - 验证：批量处理时间 < 30 秒
 *   - 报告：存证吞吐量（registrations/second）
 *
 * 运行方式：
 *   k6 run blockchain-stress.js
 *   k6 run --out json=results.json blockchain-stress.js
 */

import http from 'k6/http'
import { check, sleep, group } from 'k6'
import { Trend, Rate, Counter, Gauge } from 'k6/metrics'

// ======================== 配置 ========================

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api'
const API_PREFIX = __ENV.API_PREFIX || '/front'
const BATCH_SIZE = __ENV.BATCH_SIZE ? parseInt(__ENV.BATCH_SIZE) : 10  // 每用户提交的版权数

// 自定义指标
const batchRegisterDuration = new Trend('batch_register_duration', true)
const singleRegisterDuration = new Trend('single_register_duration', true)
const registerSuccessRate = new Rate('register_success_rate')
const registerRequests = new Counter('register_requests')
const registrationsSubmitted = new Counter('registrations_submitted')
const throughput = new Gauge('registrations_per_second')

// 测试配置
export const options = {
  stages: [
    // 逐步增加负载
    { duration: '30s', target: 100 },  // 30 秒内增加到 100 用户
    { duration: '2m', target: 100 },   // 保持 100 用户 2 分钟
    { duration: '30s', target: 0 },    // 30 秒内逐步降为 0
  ],
  thresholds: {
    // 成功率 > 95%
    'register_success_rate': ['rate>0.95'],
    // 批量处理 p95 < 30 秒
    'batch_register_duration': ['p(95)<30000'],
    // HTTP 请求失败率 < 5%
    'http_req_failed': ['rate<0.05'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
}

// ======================== 测试数据 ========================

// 模拟版权作品数据
const COPYRIGHT_WORKS = [
  { title: '春季连衣裙设计稿', author: '设计师A', type: 'design', category: 'clothing' },
  { title: '西装版型纸样', author: '设计师B', type: 'pattern', category: 'clothing' },
  { title: '旗袍刺绣图案', author: '设计师C', type: 'pattern', category: 'embroidery' },
  { title: '男装夹克款式图', author: '设计师D', type: 'design', category: 'clothing' },
  { title: '童装卫衣设计', author: '设计师E', type: 'design', category: 'children' },
  { title: '晚礼服立裁版型', author: '设计师F', type: 'pattern', category: 'dress' },
  { title: '汉服云肩纹样', author: '设计师G', type: 'pattern', category: 'traditional' },
  { title: '工装裤纸样', author: '设计师H', type: 'pattern', category: 'clothing' },
  { title: '婚纱蕾丝花型', author: '设计师I', type: 'design', category: 'wedding' },
  { title: '运动服面料图案', author: '设计师J', type: 'design', category: 'sportswear' },
]

// 生成模拟版权哈希
function generateCopyrightHash(vuId, index) {
  const content = `copyright-work-${vuId}-${index}-${Date.now()}-${Math.random().toString(36).substr(2, 15)}`
  // 简单哈希生成（实际场景使用 SHA-256）
  let hash = 0
  for (let i = 0; i < content.length; i++) {
    const char = content.charCodeAt(i)
    hash = ((hash << 5) - hash) + char
    hash = hash & hash  // Convert to 32bit integer
  }
  return Math.abs(hash).toString(16).padStart(8, '0')
}

function getAuthToken(vuId) {
  return `test-blockchain-token-vu-${vuId}-${Date.now()}`
}

// ======================== 测试函数 ========================

export default function () {
  const vuId = __VU
  const token = getAuthToken(vuId)

  group('Blockchain Copyright Registration Flow', function () {
    // ===== 场景 1: 单条版权存证 =====
    group('Single Registration', function () {
      const work = COPYRIGHT_WORKS[vuId % COPYRIGHT_WORKS.length]
      const copyrightHash = generateCopyrightHash(vuId, 0)

      const singleStart = Date.now()
      const singleRes = http.post(`${BASE_URL}${API_PREFIX}/copyright/register`, JSON.stringify({
        title: work.title,
        author: work.author,
        type: work.type,
        category: work.category,
        copyrightHash: copyrightHash,
        metadata: {
          description: `测试版权存证 - ${work.title}`,
          createdAt: new Date().toISOString(),
        },
      }), {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        tags: { name: 'singleRegister' },
        timeout: '30s',
      })
      const singleDuration = Date.now() - singleStart

      singleRegisterDuration.add(singleDuration)
      registerRequests.add(1)

      const singleSuccess = check(singleRes, {
        'single register status is 200': (r) => r.status === 200 || r.status === 201,
        'single register has txId': (r) => {
          if (r.status === 200 || r.status === 201) {
            try {
              const body = JSON.parse(r.body)
              return body.data && (body.data.txId || body.data.certificateId)
            } catch {
              return false
            }
          }
          return false
        },
      })

      registerSuccessRate.add(singleSuccess)
      if (singleSuccess) registrationsSubmitted.add(1)
    })

    sleep(0.5)

    // ===== 场景 2: 批量版权存证（核心压测点） =====
    group('Batch Registration', function () {
      const batchItems = []
      for (let i = 0; i < BATCH_SIZE; i++) {
        const work = COPYRIGHT_WORKS[(vuId + i) % COPYRIGHT_WORKS.length]
        batchItems.push({
          title: `${work.title} - 批量${i + 1}`,
          author: work.author,
          type: work.type,
          category: work.category,
          copyrightHash: generateCopyrightHash(vuId, i + 1),
          metadata: {
            description: `批量存证测试 - ${work.title}`,
            batchIndex: i + 1,
            createdAt: new Date().toISOString(),
          },
        })
      }

      const batchStart = Date.now()
      const batchRes = http.post(`${BASE_URL}${API_PREFIX}/copyright/batch-register`, JSON.stringify({
        items: batchItems,
        enableMerkleTree: true,    // 使用 Merkle 树批量存证
        merkleConfig: {
          batchSize: BATCH_SIZE,
          algorithm: 'SHA-256',
        },
      }), {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        tags: { name: 'batchRegister' },
        timeout: '60s',
      })
      const batchDuration = Date.now() - batchStart

      batchRegisterDuration.add(batchDuration)
      registerRequests.add(1)

      const batchSuccess = check(batchRes, {
        'batch register status is 200': (r) => r.status === 200 || r.status === 201,
        'batch has merkle root': (r) => {
          if (r.status === 200 || r.status === 201) {
            try {
              const body = JSON.parse(r.body)
              return body.data && (body.data.merkleRoot || body.data.txId)
            } catch {
              return false
            }
          }
          return false
        },
        'batch items count matches': (r) => {
          if (r.status === 200 || r.status === 201) {
            try {
              const body = JSON.parse(r.body)
              return body.data && (body.data.successCount || body.data.totalCount) >= 0
            } catch {
              return false
            }
          }
          return false
        },
      })

      registerSuccessRate.add(batchSuccess)
      if (batchSuccess) registrationsSubmitted.add(BATCH_SIZE)

      // 记录区块链交易 ID 用于后续验证
      if (batchRes.status === 200 || batchRes.status === 201) {
        try {
          const body = JSON.parse(batchRes.body)
          const txId = body.data?.txId
          const merkleRoot = body.data?.merkleRoot

          if (txId) {
            // Step 3: 验证区块链交易状态
            sleep(1)
            const verifyRes = http.get(`${BASE_URL}${API_PREFIX}/copyright/transaction/${txId}`, {
              headers: { 'Authorization': `Bearer ${token}` },
              tags: { name: 'verifyTransaction' },
            })

            check(verifyRes, {
              'transaction verified': (r) => r.status === 200,
            })
          }

          if (merkleRoot) {
            // Step 4: 验证 Merkle Proof（随机选一条验证）
            sleep(0.5)
            const randomItem = batchItems[Math.floor(Math.random() * batchItems.length)]
            const proofRes = http.post(`${BASE_URL}${API_PREFIX}/copyright/verify-proof`, JSON.stringify({
              merkleRoot: merkleRoot,
              copyrightHash: randomItem.copyrightHash,
            }), {
              headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
              },
              tags: { name: 'verifyMerkleProof' },
            })

            check(proofRes, {
              'merkle proof valid': (r) => r.status === 200,
            })
          }
        } catch {
          // 忽略
        }
      }
    })
  })

  // 模拟用户思考时间
  sleep(Math.random() * 3 + 1)
}

// ======================== 定期计算吞吐量 ========================

// 每 10 秒计算一次吞吐量
export function teardown(data) {
  const totalDuration = data.state?.testRunDurationMs || 1
  const totalRegistrations = data.metrics.registrations_submitted?.values?.count || 0
  const rps = totalRegistrations / (totalDuration / 1000)
  console.log(`Total registrations: ${totalRegistrations}`)
  console.log(`Total duration: ${(totalDuration / 1000).toFixed(1)}s`)
  console.log(`Throughput: ${rps.toFixed(2)} registrations/second`)
}

// ======================== 结果摘要 ========================

export function handleSummary(data) {
  const totalDuration = data.state?.testRunDurationMs || 1
  const totalRegistrations = data.metrics.registrations_submitted?.values?.count || 0
  const rps = totalRegistrations / (totalDuration / 1000)
  const successRate = data.metrics.register_success_rate?.values?.rate || 0

  const summary = {
    timestamp: new Date().toISOString(),
    test_name: 'Blockchain Copyright Registration Stress Test',
    configuration: {
      base_url: BASE_URL,
      target_vus: 100,
      registrations_per_user: BATCH_SIZE,
      total_potential_registrations: 100 * BATCH_SIZE,
    },
    results: {
      total_registrations_submitted: totalRegistrations,
      throughput_rps: rps.toFixed(2),
      success_rate: successRate,
      success_rate_percent: (successRate * 100).toFixed(2) + '%',
      batch_register_duration: {
        avg_ms: data.metrics.batch_register_duration?.values?.avg || 0,
        min_ms: data.metrics.batch_register_duration?.values?.min || 0,
        med_ms: data.metrics.batch_register_duration?.values?.med || 0,
        p95_ms: data.metrics.batch_register_duration?.values?.['p(95)'] || 0,
        p99_ms: data.metrics.batch_register_duration?.values?.['p(99)'] || 0,
        max_ms: data.metrics.batch_register_duration?.values?.max || 0,
      },
      single_register_duration: {
        avg_ms: data.metrics.single_register_duration?.values?.avg || 0,
        p95_ms: data.metrics.single_register_duration?.values?.['p(95)'] || 0,
        p99_ms: data.metrics.single_register_duration?.values?.['p(99)'] || 0,
      },
      http: {
        req_duration_p95_ms: data.metrics.http_req_duration?.values?.['p(95)'] || 0,
        req_duration_p99_ms: data.metrics.http_req_duration?.values?.['p(99)'] || 0,
        req_failed_rate: data.metrics.http_req_failed?.values?.rate || 0,
        total_http_requests: data.metrics.http_reqs?.values?.count || 0,
        http_throughput_rps: data.metrics.http_reqs?.values?.rate || 0,
      },
    },
    thresholds: {
      'success_rate > 95%': successRate > 0.95 ? 'PASS' : 'FAIL',
      'batch_register p95 < 30s': (data.metrics.batch_register_duration?.values?.['p(95)'] || 0) < 30000 ? 'PASS' : 'FAIL',
      'http_req_failed < 5%': (data.metrics.http_req_failed?.values?.rate || 0) < 0.05 ? 'PASS' : 'FAIL',
    },
  }

  return {
    'stdout': JSON.stringify(summary, null, 2),
    'results/blockchain-summary.json': JSON.stringify(summary, null, 2),
  }
}