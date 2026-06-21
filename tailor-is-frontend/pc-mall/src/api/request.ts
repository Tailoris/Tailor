import axios, {
  AxiosInstance,
  AxiosResponse,
  AxiosError,
  AxiosRequestConfig,
  InternalAxiosRequestConfig
} from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { decryptSync, encryptSync } from '@/utils/crypto'

// 🔒 F-H01: 严格类型化响应数据
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data?: T | null
}

// 🔒 F-H08: Token刷新相关类型
interface RefreshResponse {
  token: string
  refreshToken?: string
  expiresIn?: number
}

interface PendingRequest {
  resolve: (value: unknown) => void
  reject: (reason?: unknown) => void
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

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  // 🔒 F-H02/F-M02修复: 缩短超时时间从30秒到15秒
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

// 🔒 F-H08: Token刷新状态管理
let isRefreshing = false
const pendingRequests: PendingRequest[] = []

function addPendingRequest(request: PendingRequest): void {
  pendingRequests.push(request)
}

function processPendingRequests(token: string | null, error: unknown = null): void {
  pendingRequests.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error)
    } else if (token) {
      resolve(token)
    }
  })
  pendingRequests.length = 0
}

async function refreshToken(): Promise<string | null> {
  try {
    // TODO: refresh_token 应迁移至服务端 httpOnly cookie，前端不应持久化存储。
    // 当前使用 sessionStorage 降低泄露窗口（关闭浏览器即清除）。
    const refreshTokenValue = sessionStorage.getItem('refresh_token')
    if (!refreshTokenValue) {
      return null
    }
    // 🔒 F-H08: 调用刷新Token接口（无Authorization头，避免循环）
    const response = await axios.post<ApiResponse<RefreshResponse>>(
      `${import.meta.env.VITE_API_BASE_URL || '/api'}/api/auth/refresh`,
      { refreshToken: refreshTokenValue }
    )
    if (response.data.code === 200 && response.data.data?.token) {
      const newToken = response.data.data.token
      localStorage.setItem('token', encryptSync(newToken))
      if (response.data.data.refreshToken) {
        sessionStorage.setItem('refresh_token', response.data.data.refreshToken)
      }
      return newToken
    }
    return null
  } catch (e) {
    return null
  }
}

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = decryptSync(localStorage.getItem('token') || '')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    config.headers['X-CSRF-Token'] = generateCsrfToken()
    return config
  },
  (error: AxiosError) => Promise.reject(error)
)

// 响应拦截器：提取 data 字段并处理错误
service.interceptors.response.use(
  async (response: AxiosResponse<ApiResponse<unknown>>) => {
    const apiResponse = response.data
    if (!apiResponse || typeof apiResponse !== 'object') {
      ElMessage.error('响应格式错误')
      return Promise.reject(new Error('Invalid response format'))
    }
    const { code, message, data } = apiResponse
    if (code === 200 || code === 0) {
      return data as any
    }
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  async (error: AxiosError<ApiResponse>) => {
    // 🔒 F-H04: 严格类型，替换 as any
    if (error.response) {
      const { status } = error.response
      const responseData = error.response.data
      const errMessage = responseData?.message || ''

      // CSRF错误
      if (errMessage.toLowerCase().includes('csrf')) {
        ElMessage.error('安全验证失败，请刷新页面重试')
        return Promise.reject(error)
      }

      // 🔒 F-H08: 处理401 - 自动刷新Token
      if (status === 401 && !error.config?.url?.includes('/auth/refresh')) {
        if (isRefreshing) {
          // 正在刷新中，加入队列
          return new Promise((resolve, reject) => {
            addPendingRequest({ resolve, reject })
          }).then((token) => {
            if (error.config && token) {
              error.config.headers.Authorization = `Bearer ${token}`
            }
            return service.request(error.config as AxiosRequestConfig)
          })
        }

        isRefreshing = true
        const newToken = await refreshToken()
        isRefreshing = false

        if (newToken) {
          processPendingRequests(newToken)
          if (error.config) {
            error.config.headers.Authorization = `Bearer ${newToken}`
          }
          return service.request(error.config as AxiosRequestConfig)
        } else {
          // 刷新失败，跳转登录
          processPendingRequests(null, new Error('Token refresh failed'))
          localStorage.removeItem('token')
          sessionStorage.removeItem('refresh_token')
          ElMessage.error('登录已过期，请重新登录')
          router.push('/login')
          return Promise.reject(error)
        }
      }

      switch (status) {
        case 401:
          // 已在上面处理过刷新Token，这里是刷新失败或非auth请求的兜底
          localStorage.removeItem('token')
          sessionStorage.removeItem('refresh_token')
          ElMessage.error('登录已过期，请重新登录')
          router.push('/login')
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

export default service
