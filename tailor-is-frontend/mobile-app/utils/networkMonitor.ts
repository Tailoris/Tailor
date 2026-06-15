/**
 * 网络状态监控工具
 *
 * 功能：
 * - 实时监测在线/离线状态
 * - 检测弱网络条件（高延迟、低带宽）
 * - 提供状态订阅机制
 * - 网络恢复时触发事件
 */

import { ref, readonly } from 'vue'

export interface NetworkStatus {
  online: boolean
  weak: boolean
  type: 'none' | 'unknown' | 'wifi' | 'cellular' | 'ethernet' | 'bluetooth'
  speed: number          // 预估下载速度 (Mbps)
  rtt: number            // 往返延迟 (ms)
  effectiveType: 'slow-2g' | '2g' | '3g' | '4g' | 'unknown'
}

// 响应式网络状态
const _online = ref(true)
const _weakNetwork = ref(false)
const _networkType = ref<NetworkStatus['type']>('unknown')
const _networkSpeed = ref(0)
const _networkRtt = ref(0)
const _effectiveType = ref<NetworkStatus['effectiveType']>('unknown')

// 只读导出
export const isOnline = readonly(_online)
export const isWeakNetwork = readonly(_weakNetwork)
export const networkType = readonly(_networkType)
export const networkSpeed = readonly(_networkSpeed)
export const networkRtt = readonly(_networkRtt)
export const effectiveType = readonly(_effectiveType)

// 事件回调集合
type NetworkChangeCallback = (status: NetworkStatus) => void
const callbacks: Set<NetworkChangeCallback> = new Set()

// 弱网络判断阈值
const WEAK_NETWORK_RTT_THRESHOLD = 500      // RTT > 500ms 视为弱网
const WEAK_NETWORK_SPEED_THRESHOLD = 0.5     // 速度 < 0.5Mbps 视为弱网

let initialized = false

/**
 * 获取当前网络状态
 */
function getCurrentNetworkStatus(): NetworkStatus {
  return {
    online: _online.value,
    weak: _weakNetwork.value,
    type: _networkType.value,
    speed: _networkSpeed.value,
    rtt: _networkRtt.value,
    effectiveType: _effectiveType.value
  }
}

/**
 * 更新网络状态并通知订阅者
 */
function updateNetworkStatus(status: Partial<NetworkStatus>): void {
  if (status.online !== undefined) _online.value = status.online
  if (status.type !== undefined) _networkType.value = status.type
  if (status.speed !== undefined) _networkSpeed.value = status.speed
  if (status.rtt !== undefined) _networkRtt.value = status.rtt
  if (status.effectiveType !== undefined) _effectiveType.value = status.effectiveType

  // 判断弱网
  const isWeak = _networkRtt.value > WEAK_NETWORK_RTT_THRESHOLD ||
    _networkSpeed.value < WEAK_NETWORK_SPEED_THRESHOLD ||
    _effectiveType.value === 'slow-2g' ||
    _effectiveType.value === '2g'
  _weakNetwork.value = isWeak

  // 通知所有订阅者
  const currentStatus = getCurrentNetworkStatus()
  callbacks.forEach(cb => cb(currentStatus))
}

/**
 * 处理网络连接变化事件
 */
function handleOnline(): void {
  updateNetworkStatus({ online: true })
  console.log('[networkMonitor] Network online')
}

function handleOffline(): void {
  updateNetworkStatus({ online: false, weak: false, type: 'none', speed: 0, rtt: 0, effectiveType: 'unknown' })
  console.log('[networkMonitor] Network offline')
}

/**
 * 尝试使用 Network Information API 获取详细网络信息
 * 注意: navigator.connection 是实验性 API，Safari 不支持
 */
function detectNetworkInfo(): void {
  // #ifdef H5
  if (typeof navigator !== 'undefined') {
    // 安全地获取 navigator.connection (实验性 API)
    const conn = (navigator as Navigator & {
      connection?: {
        effectiveType?: string
        downlink?: number
        rtt?: number
        type?: string
        saveData?: boolean
        addEventListener?: (event: string, cb: () => void) => void
      }
    }).connection

    if (conn) {
      updateNetworkStatus({
        effectiveType: (conn.effectiveType as NetworkStatus['effectiveType']) || 'unknown',
        speed: conn.downlink || 0,
        rtt: conn.rtt || 0,
        type: (conn.type as NetworkStatus['type']) || 'unknown'
      })

      // 监听网络信息变化
      if (conn.addEventListener) {
        conn.addEventListener('change', () => {
          updateNetworkStatus({
            effectiveType: (conn.effectiveType as NetworkStatus['effectiveType']) || 'unknown',
            speed: conn.downlink || 0,
            rtt: conn.rtt || 0,
            type: (conn.type as NetworkStatus['type']) || 'unknown'
          })
        })
      }
    } else {
      // Safari 等不支持 navigator.connection 的浏览器，使用默认值
      console.log('[networkMonitor] navigator.connection not available, using default values')
    }
  }
  // #endif
}

/**
 * 通过实际请求延迟检测弱网
 * @param url - 测试 URL
 * @param timeout - 超时时间 (ms)
 * @returns 延迟时间 (ms)，失败返回 Infinity
 */
export async function detectLatency(url: string = '/api/front/ping', timeout: number = 5000): Promise<number> {
  const startTime = Date.now()

  return new Promise<number>((resolve) => {
    const timer = setTimeout(() => resolve(Infinity), timeout)

    // #ifdef H5
    fetch(url, { method: 'HEAD', cache: 'no-store' })
      .then(() => {
        clearTimeout(timer)
        resolve(Date.now() - startTime)
      })
      .catch(() => {
        clearTimeout(timer)
        resolve(Infinity)
      })
    // #endif

    // #ifndef H5
    uni.request({
      url,
      method: 'HEAD',
      timeout,
      success: () => {
        clearTimeout(timer)
        resolve(Date.now() - startTime)
      },
      fail: () => {
        clearTimeout(timer)
        resolve(Infinity)
      }
    })
    // #endif
  })
}

/**
 * 初始化网络监控
 * 在应用启动时调用
 */
export function initNetworkMonitor(): void {
  if (initialized) return
  initialized = true

  // 初始状态检测
  // #ifdef H5
  _online.value = navigator.onLine
  // #endif
  // #ifndef H5
  _online.value = true  // 小程序默认在线，通过请求错误判断离线
  // #endif

  // 监听浏览器在线/离线事件
  // #ifdef H5
  window.addEventListener('online', handleOnline)
  window.addEventListener('offline', handleOffline)
  // #endif

  // 尝试获取详细网络信息
  detectNetworkInfo()

  console.log(`[networkMonitor] Initialized, online: ${_online.value}`)
}

/**
 * 销毁网络监控
 */
export function destroyNetworkMonitor(): void {
  if (!initialized) return

  // #ifdef H5
  window.removeEventListener('online', handleOnline)
  window.removeEventListener('offline', handleOffline)
  // #endif

  callbacks.clear()
  initialized = false
  console.log('[networkMonitor] Destroyed')
}

/**
 * 订阅网络状态变化
 * @param callback - 状态变化回调函数
 * @returns 取消订阅函数
 */
export function onNetworkChange(callback: NetworkChangeCallback): () => void {
  callbacks.add(callback)

  // 立即通知当前状态
  callback(getCurrentNetworkStatus())

  return () => {
    callbacks.delete(callback)
  }
}

/**
 * 取消订阅（使用 onNetworkChange 返回的函数）
 */
export function offNetworkChange(callback: NetworkChangeCallback): void {
  callbacks.delete(callback)
}

/**
 * 手动更新在线状态（用于小程序端通过请求错误判断）
 * @param online - 是否在线
 */
export function setOnlineStatus(online: boolean): void {
  if (online !== _online.value) {
    updateNetworkStatus({ online })
  }
}

/**
 * 根据当前网络状况获取推荐的请求超时时间
 * @returns 超时时间 (ms)
 */
export function getRecommendedTimeout(): number {
  if (!_online.value) return 0  // 离线不发起请求

  if (_weakNetwork.value) {
    // 弱网延长超时
    return 30000
  }

  return 15000  // 正常网络
}

/**
 * 判断是否应该降级请求（减少不必要的 API 调用）
 * @returns 是否需要降级
 */
export function shouldDegradeRequests(): boolean {
  return _weakNetwork.value || !_online.value
}

/**
 * 判断是否应该使用低分辨率图片
 * @returns 是否应该使用低分辨率图片
 */
export function shouldUseLowResImage(): boolean {
  return _weakNetwork.value ||
    _effectiveType.value === 'slow-2g' ||
    _effectiveType.value === '2g' ||
    _networkSpeed.value < 1
}

/**
 * 获取网络状态描述文本
 */
export function getNetworkStatusText(): string {
  if (!_online.value) return '网络已断开'
  if (_weakNetwork.value) return '网络信号较弱'
  return '网络连接正常'
}

/**
 * 获取网络状态图标
 */
export function getNetworkStatusIcon(): string {
  if (!_online.value) return '🔴'
  if (_weakNetwork.value) return '🟡'
  return '🟢'
}

export default {
  initNetworkMonitor,
  destroyNetworkMonitor,
  onNetworkChange,
  offNetworkChange,
  detectLatency,
  setOnlineStatus,
  getRecommendedTimeout,
  shouldDegradeRequests,
  shouldUseLowResImage,
  getNetworkStatusText,
  getNetworkStatusIcon,
  // 响应式状态
  isOnline,
  isWeakNetwork,
  networkType,
  networkSpeed,
  networkRtt,
  effectiveType
}
