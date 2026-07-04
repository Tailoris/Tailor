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

export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data?: T | null
}

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

function generateSecureToken(): string {
  if (typeof crypto !== 'undefined' && crypto.getRandomValues) {
    const array = new Uint8Array(32)
    crypto.getRandomValues(array)
    return Array.from(array, b => b.toString(16).padStart(2, '0')).join('')
  }
  return ''
}

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

let isRefreshing = false
const pendingRequests: PendingRequest[] = []

function isPublicPage(): boolean {
  const currentPath = router.currentRoute.value.path
  return currentPath === '/login' || currentPath === '/register'
}

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
    const refreshTokenValue = sessionStorage.getItem('refresh_token')
    if (!refreshTokenValue) {
      return null
    }
    const response = await axios.post<ApiResponse<RefreshResponse>>(
      `${import.meta.env.VITE_API_BASE_URL || '/api'}/auth/refresh`,
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
    const userId = localStorage.getItem('user_id')
    if (userId) {
      config.headers['X-User-Id'] = userId
    }
    config.headers['X-CSRF-Token'] = generateCsrfToken()
    return config
  },
  (error: AxiosError) => Promise.reject(error)
)

service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<unknown>>) => {
    const apiResponse = response.data
    if (!apiResponse || typeof apiResponse !== 'object') {
      ElMessage.error('响应格式错误')
      return Promise.reject(new Error('Invalid response format'))
    }
    const { code, message } = apiResponse
    if (code !== 200 && code !== 0) {
      ElMessage.error(message || '请求失败')
      return Promise.reject(new Error(message))
    }
    return response
  },
  async (error: AxiosError<ApiResponse>) => {
    if (error.response) {
      const { status } = error.response
      const responseData = error.response.data
      const errMessage = responseData?.message || ''

      if (errMessage.toLowerCase().includes('csrf')) {
        ElMessage.error('安全验证失败，请刷新页面重试')
        return Promise.reject(error)
      }

      if (status === 401 && !error.config?.url?.includes('/auth/refresh')) {
        if (isRefreshing) {
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
          processPendingRequests(null, new Error('Token refresh failed'))
          localStorage.removeItem('token')
          sessionStorage.removeItem('refresh_token')
          if (!isPublicPage()) {
            ElMessage.error('登录已过期，请重新登录')
            router.push('/login')
          }
          return Promise.reject(error)
        }
      }

      switch (status) {
        case 401:
          localStorage.removeItem('token')
          sessionStorage.removeItem('refresh_token')
          if (!isPublicPage()) {
            ElMessage.error('登录已过期，请重新登录')
            router.push('/login')
          }
          break
        case 403:
          ElMessage.error('没有权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 429:
          ElMessage.error('请求过于频繁，请稍后再试')
          break
        case 500:
          ElMessage.error(errMessage || '服务器内部错误，请稍后重试')
          break
        case 502:
          ElMessage.error('网关错误，服务暂时不可用')
          break
        case 503:
          ElMessage.error('服务暂时不可用，请检查网络或稍后重试')
          break
        case 504:
          ElMessage.error('网关超时，请稍后重试')
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

export default async function request<D = unknown, T = unknown>(config: AxiosRequestConfig): Promise<T> {
  const response = await service.request<ApiResponse<T>>(config)
  return response.data.data as T
}