// ==============================================================================
// Tailor IS - Service Worker 注册工具
// 离线浏览支持 (UX-P2-01)
// ==============================================================================

/**
 * Service Worker 注册状态
 */
export type SWStatus = 'unsupported' | 'registering' | 'registered' | 'updated' | 'error'

/**
 * Service Worker 更新回调
 */
export interface SWUpdateCallback {
  (registration: ServiceWorkerRegistration): void
}

// 当前状态
let swStatus: SWStatus = 'unsupported'
let swRegistration: ServiceWorkerRegistration | null = null
let updateCallback: SWUpdateCallback | null = null

/**
 * 检查浏览器是否支持 Service Worker
 */
export function isSWSupported(): boolean {
  return 'serviceWorker' in navigator && 'indexedDB' in window
}

/**
 * 注册 Service Worker
 * 在页面加载时调用（仅 H5 平台）
 * @param swPath - Service Worker 文件路径，默认 '/sw.js'
 * @param onUpdate - 检测到更新时的回调
 * @returns Promise<ServiceWorkerRegistration | null>
 */
export async function registerSW(
  swPath: string = '/sw.js',
  onUpdate?: SWUpdateCallback
): Promise<ServiceWorkerRegistration | null> {
  if (!isSWSupported()) {
    console.warn('[SW Register] Service Worker not supported by this browser')
    swStatus = 'unsupported'
    return null
  }

  if (swRegistration) {
    console.log('[SW Register] Already registered')
    return swRegistration
  }

  swStatus = 'registering'
  updateCallback = onUpdate || null

  try {
    swRegistration = await navigator.serviceWorker.register(swPath, {
      scope: '/',
    })

    swStatus = 'registered'
    console.log(`[SW Register] Registered with scope: ${swRegistration.scope}`)

    // 监听更新
    setupUpdateDetection(swRegistration)

    return swRegistration
  } catch (error) {
    console.error('[SW Register] Registration failed:', error)
    swStatus = 'error'
    return null
  }
}

/**
 * 设置 Service Worker 更新检测
 */
function setupUpdateDetection(registration: ServiceWorkerRegistration): void {
  // 检查是否有更新的 SW 在等待
  if (registration.waiting) {
    handleUpdateReady(registration)
  }

  // 监听新 SW 的安装
  registration.addEventListener('updatefound', () => {
    const installingWorker = registration.installing
    if (!installingWorker) return

    console.log('[SW Register] New version found, installing...')

    installingWorker.addEventListener('statechange', () => {
      if (installingWorker.state === 'installed' && navigator.serviceWorker.controller) {
        console.log('[SW Register] Update ready')
        handleUpdateReady(registration)
      }
    })
  })

  // 监听 SW 控制器变化
  navigator.serviceWorker.addEventListener('controllerchange', () => {
    console.log('[SW Register] Controller changed, reloading...')
    window.location.reload()
  })

  // 定期检查更新（每 60 分钟）
  setInterval(() => {
    registration.update().catch((err) => {
      console.warn('[SW Register] Update check failed:', err)
    })
  }, 60 * 60 * 1000)
}

/**
 * 处理 SW 更新就绪
 */
function handleUpdateReady(registration: ServiceWorkerRegistration): void {
  swStatus = 'updated'

  if (updateCallback) {
    updateCallback(registration)
  } else {
    // 默认行为：提示用户刷新
    showUpdatePrompt(registration)
  }
}

/**
 * 显示更新提示
 */
function showUpdatePrompt(registration: ServiceWorkerRegistration): void {
  // 避免重复提示
  if (document.querySelector('.sw-update-toast')) return

  const toast = document.createElement('div')
  toast.className = 'sw-update-toast'
  toast.innerHTML = `
    <div class="sw-update-toast-content">
      <span>有新版本可用</span>
      <button id="sw-update-refresh">立即刷新</button>
      <button id="sw-update-dismiss">稍后</button>
    </div>
  `
  // 添加样式
  const style = document.createElement('style')
  style.textContent = `
    .sw-update-toast {
      position: fixed;
      bottom: 20px;
      left: 50%;
      transform: translateX(-50%);
      z-index: 99999;
      background: #1d39c4;
      color: #fff;
      border-radius: 24px;
      padding: 12px 24px;
      box-shadow: 0 4px 12px rgba(0,0,0,0.3);
      animation: sw-update-slide-in 0.3s ease-out;
    }
    .sw-update-toast-content {
      display: flex;
      align-items: center;
      gap: 12px;
      font-size: 14px;
    }
    .sw-update-toast-content button {
      border: 1px solid #fff;
      background: transparent;
      color: #fff;
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 12px;
      cursor: pointer;
    }
    #sw-update-refresh {
      background: #fff;
      color: #1d39c4;
      border: none;
    }
    @keyframes sw-update-slide-in {
      from { transform: translateX(-50%) translateY(100px); opacity: 0; }
      to { transform: translateX(-50%) translateY(0); opacity: 1; }
    }
  `

  document.head.appendChild(style)
  document.body.appendChild(toast)

  document.getElementById('sw-update-refresh')?.addEventListener('click', () => {
    if (registration.waiting) {
      // 通知等待中的 SW 立即激活
      registration.waiting.postMessage({ type: 'SKIP_WAITING' })
    }
    toast.remove()
    style.remove()
  })

  document.getElementById('sw-update-dismiss')?.addEventListener('click', () => {
    toast.remove()
    style.remove()
  })

  // 自动消失（30 秒后）
  setTimeout(() => {
    toast.remove()
    style.remove()
  }, 30000)
}

/**
 * 注销 Service Worker
 */
export async function unregisterSW(): Promise<boolean> {
  if (!swRegistration) {
    console.warn('[SW Register] No SW to unregister')
    return false
  }

  try {
    const result = await swRegistration.unregister()
    if (result) {
      console.log('[SW Register] Unregistered')
      swRegistration = null
      swStatus = 'unsupported'
    }
    return result
  } catch (error) {
    console.error('[SW Register] Unregister failed:', error)
    return false
  }
}

/**
 * 获取 SW 缓存统计信息
 */
export async function getSWCacheStats(): Promise<Record<string, number> | null> {
  if (!swRegistration || !swRegistration.active) {
    return null
  }

  return new Promise((resolve) => {
    const channel = new MessageChannel()
    channel.port1.onmessage = (event) => {
      if (event.data?.type === 'CACHE_STATS') {
        resolve(event.data.payload)
      } else {
        resolve(null)
      }
    }

    swRegistration!.active!.postMessage(
      { type: 'GET_CACHE_STATS' },
      [channel.port2]
    )

    // 超时处理
    setTimeout(() => resolve(null), 3000)
  })
}

/**
 * 预缓存指定 URL 列表
 * @param urls - 需要预缓存的 URL 列表
 */
export function precacheURLs(urls: string[]): void {
  if (!swRegistration || !swRegistration.active) {
    console.warn('[SW Register] Cannot precache: SW not active')
    return
  }

  swRegistration.active.postMessage({
    type: 'CACHE_URLS',
    payload: { urls },
  })
}

/**
 * 清除所有 SW 缓存
 */
export function clearSWCache(): void {
  if (!swRegistration || !swRegistration.active) {
    console.warn('[SW Register] Cannot clear cache: SW not active')
    return
  }

  swRegistration.active.postMessage({ type: 'CLEAR_CACHE' })
}

/**
 * 获取当前 SW 注册状态
 */
export function getSWStatus(): SWStatus {
  return swStatus
}

/**
 * 获取当前 SW 注册对象
 */
export function getSWRegistration(): ServiceWorkerRegistration | null {
  return swRegistration
}

/**
 * 监听 SW 消息
 * @param handler - 消息处理函数
 */
export function onSWMessage(handler: (event: MessageEvent) => void): () => void {
  navigator.serviceWorker.addEventListener('message', handler)
  return () => {
    navigator.serviceWorker.removeEventListener('message', handler)
  }
}

export default {
  registerSW,
  unregisterSW,
  isSWSupported,
  getSWCacheStats,
  precacheURLs,
  clearSWCache,
  getSWStatus,
  getSWRegistration,
  onSWMessage,
}