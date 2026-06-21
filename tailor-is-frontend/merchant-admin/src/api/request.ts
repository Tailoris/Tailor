import axios, {
  AxiosError,
  AxiosInstance,
  AxiosRequestConfig,
  InternalAxiosRequestConfig
} from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types'
import { decryptSync } from '@/utils/crypto'

// 简单日志工具（生产环境由 terser/esbuild 移除 console 调用）
const log = {
  info: (_msg: string) => {},
  warn: (msg: string) => console.warn(`[WARN] ${msg}`),
  error: (msg: string) => console.error(`[ERROR] ${msg}`)
}

// 🔒 F-H05: 重试配置
const MAX_RETRY_COUNT = 3
const RETRY_DELAY_MS = 1000

// 🔒 F-H05: 不可重试的状态码
const NON_RETRYABLE_STATUS = new Set([400, 401, 403, 404, 422])

// 🔒 F-H05: 不可重试的请求方法
const NON_RETRYABLE_METHODS = new Set(['post', 'put', 'delete', 'patch'])

interface RetryConfig extends InternalAxiosRequestConfig {
  __retryCount?: number
}

function generateCsrfToken(): string {
  const stored = localStorage.getItem('csrf_token')
  if (stored) return stored
  const token = generateSecureToken()
  if (token) {
    localStorage.setItem('csrf_token', token)
  }
  return token
}

/**
 * 使用 crypto.getRandomValues 生成安全的 CSRF Token
 */
function generateSecureToken(): string {
  if (typeof crypto !== 'undefined' && crypto.getRandomValues) {
    const array = new Uint8Array(32)
    crypto.getRandomValues(array)
    return Array.from(array, b => b.toString(16).padStart(2, '0')).join('')
  }
  // For environments without crypto, return empty (server should provide CSRF tokens)
  return ''
}

/**
 * 🔒 F-H05: 延迟函数
 */
function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms))
}

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

request.interceptors.request.use(
  (config) => {
    const token = decryptSync(localStorage.getItem('token') || '')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    config.headers['X-CSRF-Token'] = generateCsrfToken()
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse<unknown>
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        localStorage.removeItem('token')
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data
  },
  async (error: AxiosError<ApiResponse<unknown>>) => {
    // 🔒 F-H05: 自动重试逻辑
    const config = error.config as RetryConfig | undefined
    if (config && shouldRetry(error, config)) {
      config.__retryCount = (config.__retryCount || 0) + 1
      const delay = RETRY_DELAY_MS * config.__retryCount // 指数退避
      log.info(`请求失败，${delay}ms后第${config.__retryCount}次重试: ${config.url}`)
      await sleep(delay)
      return request.request(config)
    }

    if (error.response) {
      const { status, data } = error.response
      const message = data?.message || ''

      if (message.toLowerCase().includes('csrf')) {
        ElMessage.error('安全验证失败，请刷新页面重试')
        return Promise.reject(error)
      }

      switch (status) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          localStorage.removeItem('token')
          window.location.href = '/login'
          break
        case 403:
          ElMessage.error('权限不足，请联系管理员')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误，请稍后重试')
          break
        default:
          ElMessage.error(data?.message || '请求失败，请稍后重试')
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else {
      ElMessage.error('网络连接异常，请检查网络')
    }
    return Promise.reject(error)
  }
)

/**
 * 🔒 F-H05: 判断是否应该重试
 */
function shouldRetry(error: AxiosError, config: RetryConfig): boolean {
  // 超过最大重试次数
  if ((config.__retryCount || 0) >= MAX_RETRY_COUNT) {
    return false
  }
  // 写操作不重试（避免重复提交）
  const method = (config.method || 'get').toLowerCase()
  if (NON_RETRYABLE_METHODS.has(method)) {
    return false
  }
  // 网络错误重试
  if (!error.response) {
    return true
  }
  // 特定状态码不重试
  const status = error.response.status
  if (NON_RETRYABLE_STATUS.has(status)) {
    return false
  }
  // 5xx 错误重试
  return status >= 500
}

export default request
