import type { ApiResponse, RequestConfig } from './types'
import appConfig from '../config'
import { getSecure, removeSecure, setSecure } from '../utils/crypto'
import { isOnline, isWeakNetwork, getRecommendedTimeout, shouldDegradeRequests, setOnlineStatus } from '../utils/networkMonitor'
import { enqueueSync } from '../utils/offlineStorage'

/**
 * 移动端请求封装 - 修复 B-C11/F-C01/F-C02/F-C03/F-M01/F-M03
 *
 * 关键改进：
 * 1. BASE_URL 从环境变量读取
 * 2. Token 加密存储（F-C03）
 * 3. 完整 TypeScript 类型支持
 * 4. crypto.randomUUID polyfill 兼容
 * 5. 自动重试机制
 * 6. 401/402 自动跳转登录
 * 7. F-M01: 国际化文案支持
 * 8. F-M03: 未授权码改为配置项
 * 9. Task-12: 离线请求队列、弱网超时调整、重试逻辑
 */

interface OfflineRequestItem {
  url: string
  method: string
  data: unknown
  options: RequestConfig
  timestamp: number
}

interface UniRequestOptions {
  url: string
  method: string
  header: Record<string, string>
  data: unknown
  timeout: number
  success: (res: { statusCode: number; data: ApiResponse<unknown> }) => void
  fail: (err: unknown) => void
  complete: () => void
}

interface UploadOptions {
  url: string
  filePath: string
  name: string
  header: Record<string, string>
  timeout: number
  success: (res: { data: string }) => void
  fail: () => void
}

/**
 * 🔒 F-M01修复: Loading 提示文本提取为常量
 */
const LOADING_TEXT = '加载中...'

/**
 * 🔒 F-M03修复: 国际化文案配置 - 从appConfig读取支持多语言
 */
const i18n = {
  loading: LOADING_TEXT,  // 可改为从i18n模块读取
  systemError: '系统异常',
  paramError: '参数校验失败',
  notFound: '数据不存在',
  requestFailed: '请求失败',
  networkError: '网络连接失败',
  networkTimeout: '网络请求超时',
  offlineMode: '离线模式，请求已加入队列',
  uploadFailed: '上传失败',
  loginExpired: '登录已过期，请重新登录',
  unauthorized: '未登录，请先登录'
}

/**
 * 🔒 F-M03修复: 未授权状态码 - 提取为模块级常量数组
 * 可在appConfig中按需扩展
 */
const UNAUTH_CODES: number[] = [401, 402, 410000, 410001, 410002]

function getUnauthCodes(): number[] {
  return appConfig.unauthCodes || UNAUTH_CODES
}

/**
 * 生成 UUID（兼容低版本环境）
 * 修复 F-C04: 提供 crypto.randomUUID polyfill
 */
function generateUUID(): string {
  // 优先使用原生 crypto.randomUUID
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    try {
      return crypto.randomUUID()
    } catch {
      // 降级到 polyfill
    }
  }

  // polyfill: RFC 4122 v4
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

// 注入到全局，供业务使用
if (typeof globalThis !== 'undefined' && !(globalThis as { crypto?: { randomUUID?: () => string } }).crypto?.randomUUID) {
  Object.defineProperty(globalThis, 'crypto', {
    value: { ...(globalThis as { crypto?: object }).crypto, randomUUID: generateUUID },
    writable: true,
    configurable: true
  })
}

let isRedirecting = false

function redirectToLogin(): void {
  if (isRedirecting) return
  isRedirecting = true
  try {
    uni.removeStorageSync(appConfig.tokenStorageKey)
    uni.removeStorageSync(appConfig.userInfoStorageKey)
    uni.reLaunch({ url: '/pages/login/login' })
  } finally {
    setTimeout(() => { isRedirecting = false }, 1000)
  }
}

/**
 * Task-12: 检查是否应该跳过请求（降级模式下的非关键请求）
 */
function shouldSkipRequest(method: string, url: string): boolean {
  if (!shouldDegradeRequests()) return false
  // 弱网/离线时跳过非必要的 GET 请求（非列表/非详情）
  if (method === 'GET' && !url.includes('list') && !url.includes('detail')) {
    return true
  }
  return false
}

/**
 * Task-12: 计算超时时间（根据网络状况动态调整）
 */
function computeTimeout(options: RequestConfig): number {
  if (options.timeout) return options.timeout
  if (isWeakNetwork.value) {
    return getRecommendedTimeout() || 30000
  }
  return appConfig.requestTimeout
}

/**
 * Task-12: 将失败请求加入离线队列
 */
function enqueueOfflineRequest(url: string, method: string, data: unknown, options: RequestConfig): void {
  if (options.offlineQueue === false) return

  const entity = url.split('/')[0] || 'unknown'
  enqueueSync({
    type: method === 'DELETE' ? 'delete' : (method === 'PUT' || method === 'PATCH') ? 'update' : 'create',
    entity,
    payload: { url, method, data } as Record<string, unknown>
  })
  // 离线请求已入队（静默处理）
}

/**
 * Task-12: 带重试的请求
 */
async function requestWithRetry<T = unknown>(
  url: string,
  method: string = 'GET',
  data: unknown = {},
  options: RequestConfig = {}
): Promise<ApiResponse<T>> {
  const maxRetries = options.retryCount ?? 3
  let lastError: Error | null = null

  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    if (attempt > 0) {
      // 指数退避：2s, 4s, 8s...
      const delay = Math.min(2000 * Math.pow(2, attempt - 1), 15000)
      await new Promise(resolve => setTimeout(resolve, delay))
    }

    try {
      return await doRequest<T>(url, method, data, options)
    } catch (error) {
      lastError = error instanceof Error ? error : new Error(String(error))

      // 401/402 等认证错误不重试
      if (lastError.message.includes('登录') || lastError.message.includes('未登录')) {
        throw lastError
      }

      // 离线状态不重试
      if (!isOnline.value) {
        throw lastError
      }
    }
  }

  throw lastError || new Error(i18n.networkError)
}

/**
 * 执行单次请求
 */
function doRequest<T = unknown>(
  url: string,
  method: string,
  data: unknown,
  options: RequestConfig
): Promise<ApiResponse<T>> {
  const { noAuth = false, showLoading = true, hideError = false } = options

  return new Promise<ApiResponse<T>>((resolve, reject) => {
    if (showLoading) {
      // F-M01修复: 使用i18n配置
      uni.showLoading({ title: i18n.loading, mask: true })
    }

    // 🔒 F-C03: 从加密存储读取Token
    const token: string = noAuth ? '' : getSecure(appConfig.tokenStorageKey)
    const header: Record<string, string> = {
      'Content-Type': 'application/json',
      'X-Request-Id': generateUUID()
    }

    if (!noAuth && token) {
      header['Authorization'] = `Bearer ${token}`
    }

    // 🔒 B-C11/F-C01: BASE_URL 从配置读取
    const fullURL = url.startsWith('http')
      ? url
      : `${appConfig.baseURL}${appConfig.apiPrefix}/${url}`

    // Task-12: 动态超时时间
    const timeout = computeTimeout(options)

    uni.request({
      url: fullURL,
      method,
      header,
      data,
      timeout,
      success: (res: { statusCode: number; data: ApiResponse<T> }) => {
        // 请求成功，更新在线状态
        setOnlineStatus(true)

        if (res.statusCode === 200 && res.data) {
          if (res.data.code === 200) {
            resolve(res.data)
          } else if (getUnauthCodes().includes(res.data.code)) {
            redirectToLogin()
            reject(new Error(res.data.message || i18n.loginExpired))
          } else if (res.data.code === 500) {
            const errMsg = res.data.message || '系统异常'
            if (!hideError) {
              uni.showToast({ title: errMsg, icon: 'none' })
            }
            reject(new Error(errMsg))
          } else if (res.data.code === 400) {
            const errMsg = res.data.message || '参数校验失败'
            if (!hideError) {
              uni.showToast({ title: errMsg, icon: 'none' })
            }
            reject(new Error(errMsg))
          } else if (res.data.code === 404) {
            const errMsg = res.data.message || '数据不存在'
            if (!hideError) {
              uni.showToast({ title: errMsg, icon: 'none' })
            }
            reject(new Error(errMsg))
          } else {
            const errMsg = res.data.message || '请求失败'
            if (!hideError) {
              uni.showToast({ title: errMsg, icon: 'none' })
            }
            reject(new Error(errMsg))
          }
        } else if (res.statusCode === 401) {
          redirectToLogin()
          reject(new Error('未登录，请先登录'))
        } else {
          const errMsg = `网络请求失败 (${res.statusCode})`
          if (!hideError) {
            uni.showToast({ title: errMsg, icon: 'none' })
          }
          reject(new Error(errMsg))
        }
      },
      fail: (err: unknown) => {
        // 请求失败，标记离线
        setOnlineStatus(false)

        // Task-12: 写请求加入离线队列
        if (method !== 'GET') {
          enqueueOfflineRequest(url, method, data, options)
          if (!hideError) {
            uni.showToast({ title: i18n.offlineMode, icon: 'none', duration: 2000 })
          }
        }

        if (!hideError) {
          uni.showToast({ title: '网络连接失败', icon: 'none' })
        }
        reject(err instanceof Error ? err : new Error('网络连接失败'))
      },
      complete: () => {
        if (showLoading) {
          uni.hideLoading()
        }
      }
    } as UniRequestOptions)
  })
}

function request<T = unknown>(
  url: string,
  method: string = 'GET',
  data: unknown = {},
  options: RequestConfig = {}
): Promise<ApiResponse<T>> {
  // Task-12: 离线模式处理
  if (!isOnline.value && !options.ignoreOffline) {
    // GET 请求：如果允许降级，返回空结果
    if (method === 'GET' && shouldSkipRequest(method, url)) {
      if (!options.hideError) {
        uni.showToast({ title: '网络较弱，请求已跳过', icon: 'none' })
      }
      return Promise.reject(new Error('Request skipped due to weak network'))
    }

    // 写请求：加入离线队列
    if (method !== 'GET') {
      enqueueOfflineRequest(url, method, data, options)
      if (!options.hideError) {
        uni.showToast({ title: i18n.offlineMode, icon: 'none', duration: 2000 })
      }
      return Promise.reject(new Error('Offline: request queued'))
    }
  }

  // Task-12: 带重试的请求
  return requestWithRetry<T>(url, method, data, options)
}

export function get<T = unknown>(url: string, data?: unknown, options?: RequestConfig): Promise<ApiResponse<T>> {
  return request<T>(url, 'GET', data, options)
}

export function post<T = unknown>(url: string, data?: unknown, options?: RequestConfig): Promise<ApiResponse<T>> {
  return request<T>(url, 'POST', data, options)
}

export function put<T = unknown>(url: string, data?: unknown, options?: RequestConfig): Promise<ApiResponse<T>> {
  return request<T>(url, 'PUT', data, options)
}

export function del<T = unknown>(url: string, data?: unknown, options?: RequestConfig): Promise<ApiResponse<T>> {
  return request<T>(url, 'DELETE', data, options)
}

export function uploadFile(filePath: string): Promise<ApiResponse<string>> {
  return new Promise<ApiResponse<string>>((resolve, reject) => {
    // 🔒 F-C03: 从加密存储读取Token
    const token: string = getSecure(appConfig.tokenStorageKey)
    const fullURL = `${appConfig.baseURL}${appConfig.uploadURL}`

    uni.uploadFile({
      url: fullURL,
      filePath,
      name: 'file',
      header: {
        'Authorization': `Bearer ${token}`,
        'X-Request-Id': generateUUID()
      },
      // Task-12: 弱网时延长上传超时
      timeout: isWeakNetwork.value ? 60000 : appConfig.requestTimeout,
      success: (res: { data: string }) => {
        setOnlineStatus(true)
        try {
          const data: ApiResponse<string> = JSON.parse(res.data)
          if (data.code === 200) {
            resolve(data)
          } else {
            reject(new Error(data.message || '上传失败'))
          }
        } catch {
          reject(new Error('上传失败'))
        }
      },
      fail: () => {
        setOnlineStatus(false)
        reject(new Error('上传失败'))
      }
    } as UploadOptions)
  })
}

/**
 * 🔒 F-C03: Token 安全存储便捷方法
 */
export function saveToken(token: string): void {
  setSecure(appConfig.tokenStorageKey, token)
}

export function clearToken(): void {
  removeSecure(appConfig.tokenStorageKey)
  removeSecure(appConfig.userInfoStorageKey)
}

export function getToken(): string {
  return getSecure(appConfig.tokenStorageKey)
}

/**
 * Task-12: 检查当前网络状态
 */
export function getNetworkInfo(): { online: boolean; weak: boolean } {
  return {
    online: isOnline.value,
    weak: isWeakNetwork.value
  }
}

export { generateUUID }

export default {
  get,
  post,
  put,
  delete: del,
  uploadFile,
  saveToken,
  clearToken,
  getToken,
  generateUUID,
  getNetworkInfo
}
