import axios, {
  AxiosInstance,
  AxiosResponse,
  AxiosError,
  InternalAxiosRequestConfig
} from 'axios'
import { ElMessage } from 'element-plus'
import { decryptSync } from '@/utils/crypto'

// 延迟引用 router 以避免循环依赖（request.ts → router → LoginView → auth.ts → request.ts）
let _router: ReturnType<typeof import('@/router')['default']> | null = null
export function injectRouter(router: typeof _router) {
  _router = router
}

export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data?: T | null
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
  return ''
}

const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token') || ''
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    config.headers['X-CSRF-Token'] = generateCsrfToken()
    return config
  },
  (error: AxiosError) => Promise.reject(error)
)

// 🔒 M-23修复: 替换 any 为 unknown，保持类型安全
request.interceptors.response.use(
  (response: AxiosResponse<unknown>) => {
    const apiResponse = response.data as Record<string, unknown> | null
    if (apiResponse && typeof apiResponse === 'object' && 'code' in apiResponse) {
      const { code, message, data } = apiResponse as unknown as ApiResponse
      if (code === 200 || code === 0) {
        return data
      }
      ElMessage.error(message || '请求失败')
      return Promise.reject(new Error(message))
    }
    return response.data
  },
  (error: AxiosError<ApiResponse>) => {
    if (error.response) {
      const { status } = error.response
      const responseData = error.response.data
      const errMessage = responseData?.message || ''

      // CSRF错误
      if (errMessage.toLowerCase().includes('csrf')) {
        ElMessage.error('安全验证失败，请刷新页面重试')
        return Promise.reject(error)
      }

      switch (status) {
        case 401:
          localStorage.removeItem('token')
          ElMessage.error('登录已过期，请重新登录')
          _router?.push('/login')
          break
        case 403:
          ElMessage.error('没有权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        case 503:
          ElMessage.error('服务暂时不可用')
          break
        default:
          ElMessage.error(errMessage || `请求失败 (${status})`)
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else {
      ElMessage.error('网络连接异常，请检查网络')
    }
    return Promise.reject(error)
  }
)

export default request
