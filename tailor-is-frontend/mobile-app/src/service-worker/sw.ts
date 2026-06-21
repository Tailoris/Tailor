// ==============================================================================
// Tailor IS - Mobile App Service Worker
// 离线浏览支持 (UX-P2-01)
// ==============================================================================
//
// 缓存策略:
//   - Network First: API 请求（优先网络，失败时回退缓存）
//   - Cache First: 静态资源（JS/CSS/字体/图片）
//   - Stale While Revalidate: 产品列表页
//
// 功能:
//   - 预缓存关键资源（App Shell）
//   - 离线浏览产品列表
//   - 产品图片缓存（带大小限制）
//   - 离线操作队列同步（通过 postMessage 与主线程通信）
//   - Push 通知支持
// ==============================================================================

/// <reference lib="webworker" />

declare const self: ServiceWorkerGlobalScope

// ==================== 常量 ====================

const CACHE_VERSION = 'v1'
const STATIC_CACHE = `tailor-is-static-${CACHE_VERSION}`
const API_CACHE = `tailor-is-api-${CACHE_VERSION}`
const IMAGE_CACHE = `tailor-is-images-${CACHE_VERSION}`
const PAGE_CACHE = `tailor-is-pages-${CACHE_VERSION}`

// 图片缓存最大数量（约 50MB）
const MAX_IMAGE_CACHE_ITEMS = 200
// 图片缓存最大期限（7 天）
const IMAGE_CACHE_MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000
// API 缓存最大期限（1 小时）
const API_CACHE_MAX_AGE_MS = 60 * 60 * 1000

// 预缓存的关键资源列表
const PRECACHE_ASSETS: string[] = [
  '/',
  '/index.html',
]

// 需要缓存的 API 路径模式
const API_CACHE_PATTERNS: RegExp[] = [
  /^\/api\/front\/products/,
  /^\/api\/front\/categories/,
  /^\/api\/front\/seckill/,
  /^\/api\/front\/hot/,
]

// 需要缓存的图片路径模式
const IMAGE_CACHE_PATTERNS: RegExp[] = [
  /\.(png|jpg|jpeg|gif|svg|webp|ico)(\?.*)?$/i,
  /\/images\//,
  /\/product-images\//,
]

// 推送通知相关
let pushSubscription: PushSubscription | null = null

// ==================== 安装事件 ====================

self.addEventListener('install', (event: ExtendableEvent) => {
  console.log('[SW] Installing...')

  event.waitUntil(
    caches.open(STATIC_CACHE).then((cache) => {
      console.log('[SW] Pre-caching critical assets')
      return cache.addAll(PRECACHE_ASSETS)
    }).then(() => {
      console.log('[SW] Skip waiting')
      return self.skipWaiting()
    })
  )
})

// ==================== 激活事件 ====================

self.addEventListener('activate', (event: ExtendableEvent) => {
  console.log('[SW] Activating...')

  event.waitUntil(
    Promise.all([
      // 清理旧版本缓存
      cleanupOldCaches(),
      // 立即接管所有客户端
      self.clients.claim(),
    ]).then(() => {
      console.log('[SW] Activated')
    })
  )
})

async function cleanupOldCaches(): Promise<void> {
  const cacheNames = await caches.keys()
  const validCaches = [STATIC_CACHE, API_CACHE, IMAGE_CACHE, PAGE_CACHE]

  const deletePromises = cacheNames
    .filter((name) => !validCaches.includes(name))
    .map((name) => {
      console.log(`[SW] Deleting old cache: ${name}`)
      return caches.delete(name)
    })

  await Promise.all(deletePromises)
}

// ==================== 请求拦截 ====================

self.addEventListener('fetch', (event: FetchEvent) => {
  const { request } = event

  // 跳过非 GET 请求
  if (request.method !== 'GET') return

  // 跳过浏览器扩展请求
  if (!request.url.startsWith('http')) return

  const url = new URL(request.url)

  // API 请求：Network First
  if (isApiRequest(url)) {
    event.respondWith(networkFirst(request, url))
    return
  }

  // 图片请求：Cache First（带大小限制）
  if (isImageRequest(url)) {
    event.respondWith(cacheFirstWithSizeLimit(request, url))
    return
  }

  // 页面请求：Stale While Revalidate
  if (isPageRequest(url)) {
    event.respondWith(staleWhileRevalidate(request, url))
    return
  }

  // 静态资源：Cache First
  if (isStaticAsset(url)) {
    event.respondWith(cacheFirst(request, url))
    return
  }

  // 默认：Network First
  event.respondWith(networkFirst(request, url))
})

// ==================== 缓存策略实现 ====================

/**
 * Network First 策略：优先网络，失败时回退缓存
 */
async function networkFirst(request: Request, url: URL): Promise<Response> {
  const cache = await caches.open(API_CACHE)

  try {
    const networkResponse = await fetch(request)

    // 缓存成功的响应（仅缓存 API 响应）
    if (networkResponse.ok && isCacheableApiRequest(url)) {
      const clonedResponse = networkResponse.clone()
      cache.put(request, clonedResponse)
    }

    return networkResponse
  } catch (error) {
    // 网络失败，尝试从缓存获取
    const cachedResponse = await cache.match(request)
    if (cachedResponse) {
      // 添加离线标记头
      const headers = new Headers(cachedResponse.headers)
      headers.set('X-Offline-Cache', 'true')
      return new Response(cachedResponse.body, {
        status: cachedResponse.status,
        statusText: cachedResponse.statusText,
        headers,
      })
    }

    // 无缓存，返回离线页面
    return getOfflineFallback(request)
  }
}

/**
 * Cache First 策略：优先缓存，缓存未命中时请求网络
 */
async function cacheFirst(request: Request, url: URL): Promise<Response> {
  const cache = await caches.open(STATIC_CACHE)
  const cachedResponse = await cache.match(request)

  if (cachedResponse) {
    return cachedResponse
  }

  try {
    const networkResponse = await fetch(request)

    if (networkResponse.ok) {
      const clonedResponse = networkResponse.clone()
      cache.put(request, clonedResponse)
    }

    return networkResponse
  } catch {
    return getOfflineFallback(request)
  }
}

/**
 * Cache First with Size Limit：图片缓存（带大小限制）
 */
async function cacheFirstWithSizeLimit(request: Request, url: URL): Promise<Response> {
  const imageCache = await caches.open(IMAGE_CACHE)
  const cachedResponse = await imageCache.match(request)

  if (cachedResponse) {
    // 后台更新缓存（Stale While Revalidate 模式）
    updateImageCache(request, imageCache)
    return cachedResponse
  }

  try {
    const networkResponse = await fetch(request)

    if (networkResponse.ok) {
      const clonedResponse = networkResponse.clone()
      const contentLength = parseInt(networkResponse.headers.get('content-length') || '0', 10)

      // 仅缓存小于 5MB 的图片
      if (contentLength > 0 && contentLength < 5 * 1024 * 1024) {
        await enforceImageCacheLimit(imageCache)
        imageCache.put(request, clonedResponse)
      } else if (contentLength === 0) {
        // 无 content-length，尝试缓存
        await enforceImageCacheLimit(imageCache)
        imageCache.put(request, clonedResponse)
      }
    }

    return networkResponse
  } catch {
    return getOfflineFallback(request)
  }
}

/**
 * Stale While Revalidate：立即返回缓存，后台更新
 */
async function staleWhileRevalidate(request: Request, url: URL): Promise<Response> {
  const cache = await caches.open(PAGE_CACHE)
  const cachedResponse = await cache.match(request)

  const networkPromise = fetch(request)
    .then((networkResponse) => {
      if (networkResponse.ok) {
        const clonedResponse = networkResponse.clone()
        cache.put(request, clonedResponse)
      }
      return networkResponse
    })
    .catch(() => cachedResponse)

  // 如果缓存存在，立即返回；否则等待网络
  return cachedResponse || networkPromise
}

// ==================== 图片缓存管理 ====================

/**
 * 后台更新图片缓存
 */
async function updateImageCache(request: Request, cache: Cache): Promise<void> {
  try {
    const response = await fetch(request)
    if (response.ok) {
      cache.put(request, response.clone())
    }
  } catch {
    // 后台更新失败，忽略
  }
}

/**
 * 强制图片缓存大小限制
 */
async function enforceImageCacheLimit(cache: Cache): Promise<void> {
  const keys = await cache.keys()
  if (keys.length >= MAX_IMAGE_CACHE_ITEMS) {
    // 删除最旧的 20% 条目
    const deleteCount = Math.ceil(keys.length * 0.2)
    for (let i = 0; i < deleteCount; i++) {
      await cache.delete(keys[i])
    }
    console.log(`[SW] Image cache cleanup: removed ${deleteCount} items`)
  }
}

// ==================== 请求分类 ====================

function isApiRequest(url: URL): boolean {
  return url.pathname.startsWith('/api/')
}

function isCacheableApiRequest(url: URL): boolean {
  return API_CACHE_PATTERNS.some((pattern) => pattern.test(url.pathname))
}

function isImageRequest(url: URL): boolean {
  return IMAGE_CACHE_PATTERNS.some((pattern) => pattern.test(url.pathname))
}

function isPageRequest(url: URL): boolean {
  // 判断是否为页面请求（非静态资源，非 API）
  const pathname = url.pathname
  if (pathname.startsWith('/api/')) return false
  if (pathname.startsWith('/assets/')) return false
  if (pathname.startsWith('/static/')) return false
  if (/\.(js|css|png|jpg|jpeg|gif|svg|webp|ico|woff2?|ttf|eot|json|xml|map)(\?.*)?$/i.test(pathname)) return false
  return true
}

function isStaticAsset(url: URL): boolean {
  const pathname = url.pathname
  return /\.(js|css|woff2?|ttf|eot|json|xml|map)(\?.*)?$/i.test(pathname)
    || pathname.startsWith('/assets/')
    || pathname.startsWith('/static/')
}

// ==================== 离线回退 ====================

function getOfflineFallback(request: Request): Response {
  // 如果是页面请求，返回离线页面
  if (request.headers.get('accept')?.includes('text/html')) {
    return new Response(
      `<!DOCTYPE html>
      <html lang="zh-CN">
      <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>离线模式 - Tailor IS</title>
        <style>
          body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            height: 100vh;
            margin: 0;
            background: #f8f8f8;
            color: #333;
          }
          .offline-icon { font-size: 64px; margin-bottom: 20px; }
          h1 { font-size: 24px; margin: 0 0 8px 0; }
          p { color: #666; font-size: 14px; margin: 0 0 24px 0; }
          button {
            background: #ff4d4f;
            color: #fff;
            border: none;
            padding: 12px 32px;
            border-radius: 24px;
            font-size: 16px;
            cursor: pointer;
          }
        </style>
      </head>
      <body>
        <div class="offline-icon">📡</div>
        <h1>当前处于离线模式</h1>
        <p>请检查你的网络连接</p>
        <button onclick="location.reload()">重试连接</button>
      </body>
      </html>`,
      {
        status: 200,
        statusText: 'OK',
        headers: {
          'Content-Type': 'text/html; charset=utf-8',
          'X-Offline-Page': 'true',
        },
      }
    )
  }

  // 非 HTML 请求返回错误
  return new Response(
    JSON.stringify({ error: 'offline', message: 'Network unavailable' }),
    {
      status: 503,
      statusText: 'Service Unavailable',
      headers: {
        'Content-Type': 'application/json',
        'X-Offline-Cache': 'true',
      },
    }
  )
}

// ==================== 消息通信 ====================

self.addEventListener('message', (event: ExtendableMessageEvent) => {
  const { type, payload } = event.data || {}

  switch (type) {
    case 'CACHE_URLS':
      // 主线程请求预缓存指定 URL
      event.waitUntil(precacheUrls(payload.urls))
      break

    case 'CLEAR_CACHE':
      // 清除所有缓存
      event.waitUntil(clearAllCaches())
      break

    case 'GET_CACHE_STATS':
      // 获取缓存统计信息
      event.waitUntil(
        getCacheStats().then((stats) => {
          if (event.ports?.[0]) {
            event.ports[0].postMessage({ type: 'CACHE_STATS', payload: stats })
          }
        })
      )
      break

    case 'SKIP_WAITING':
      self.skipWaiting()
      break

    default:
      console.log(`[SW] Unknown message type: ${type}`)
  }
})

/**
 * 预缓存指定 URL 列表
 */
async function precacheUrls(urls: string[]): Promise<void> {
  const cache = await caches.open(STATIC_CACHE)
  const promises = urls.map(async (url) => {
    try {
      const response = await fetch(url)
      if (response.ok) {
        await cache.put(url, response)
      }
    } catch (error) {
      console.warn(`[SW] Failed to precache: ${url}`, error)
    }
  })
  await Promise.allSettled(promises)
}

/**
 * 清除所有缓存
 */
async function clearAllCaches(): Promise<void> {
  const cacheNames = await caches.keys()
  await Promise.all(cacheNames.map((name) => caches.delete(name)))
  console.log('[SW] All caches cleared')
}

/**
 * 获取缓存统计信息
 */
async function getCacheStats(): Promise<Record<string, number>> {
  const stats: Record<string, number> = {}
  const cacheNames = await caches.keys()

  for (const name of cacheNames) {
    const cache = await caches.open(name)
    const keys = await cache.keys()
    stats[name] = keys.length
  }

  stats.totalCaches = cacheNames.length
  return stats
}

// ==================== Push 通知 ====================

self.addEventListener('push', (event: PushEvent) => {
  console.log('[SW] Push received')

  let data: { title?: string; body?: string; icon?: string; url?: string } = {}

  if (event.data) {
    try {
      data = event.data.json()
    } catch {
      data = { title: 'Tailor IS', body: event.data.text() }
    }
  }

  const options: NotificationOptions = {
    body: data.body || '你有新的消息',
    icon: data.icon || '/static/logo.png',
    badge: '/static/badge.png',
    vibrate: [200, 100, 200],
    data: {
      url: data.url || '/',
    },
    actions: [
      { action: 'open', title: '查看详情' },
      { action: 'close', title: '关闭' },
    ],
  }

  event.waitUntil(
    self.registration.showNotification(data.title || 'Tailor IS', options)
  )
})

self.addEventListener('notificationclick', (event: NotificationEvent) => {
  event.notification.close()

  const url = event.notification.data?.url || '/'

  if (event.action === 'close') {
    return
  }

  event.waitUntil(
    self.clients.matchAll({ type: 'window' }).then((clientList) => {
      // 如果已有打开的窗口，聚焦并导航
      for (const client of clientList) {
        if (client.url.includes(url) && 'focus' in client) {
          return client.focus()
        }
      }
      // 否则打开新窗口
      if (self.clients.openWindow) {
        return self.clients.openWindow(url)
      }
    })
  )
})

// ==================== 推送订阅管理 ====================

self.addEventListener('pushsubscriptionchange', () => {
  console.log('[SW] Push subscription changed')
  // 通知所有客户端订阅已变更
  self.clients.matchAll().then((clients) => {
    clients.forEach((client) => {
      client.postMessage({ type: 'PUSH_SUBSCRIPTION_CHANGED' })
    })
  })
})

// ==================== 定期同步 ====================

// 定期后台同步（如果浏览器支持）
self.addEventListener('periodicsync', (event: PeriodicSyncEvent) => {
  if (event.tag === 'sync-offline-actions') {
    event.waitUntil(
      // 通知所有客户端执行离线操作同步
      self.clients.matchAll().then((clients) => {
        clients.forEach((client) => {
          client.postMessage({ type: 'TRIGGER_SYNC' })
        })
      })
    )
  }
})

// ==================== 导出（供类型声明） ====================

export {}