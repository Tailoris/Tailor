// ==============================================================================
// Tailor IS - 离线数据库工具 (IndexedDB Wrapper)
// 离线浏览支持 (UX-P2-01)
// ==============================================================================
//
// 提供统一的 IndexedDB 操作接口，用于离线数据存储：
// - 产品列表和详情缓存
// - 用户偏好设置
// - 购物车数据
// - 离线操作同步队列
//
// 注：此模块运行在主线程，与 Service Worker 的 Cache API 互补。
//     SW 负责静态资源和 API 响应缓存，此模块负责结构化数据存储。
// ==============================================================================

const DB_NAME = 'TailorISOffline'
const DB_VERSION = 2

// 对象存储名称
const STORES = {
  PRODUCTS: 'products',
  PRODUCT_LIST: 'productList',
  USER_PREFS: 'userPrefs',
  CART: 'cart',
  SYNC_QUEUE: 'syncQueue',
  WISHLIST: 'wishlist',
} as const

// 同步队列项类型
export interface SyncQueueItem {
  id: string
  type: 'addToCart' | 'removeFromCart' | 'updateCart' | 'addToWishlist' | 'removeFromWishlist'
  payload: Record<string, unknown>
  timestamp: number
  retryCount: number
  status: 'pending' | 'syncing' | 'failed'
}

// 产品数据类型
export interface ProductData {
  id: number
  name: string
  price: number
  image: string
  description?: string
  category?: string
  merchantId?: number
  [key: string]: unknown
}

// 购物车项类型
export interface CartItemData {
  productId: number
  productName: string
  price: number
  quantity: number
  image: string
  selected: boolean
  specId?: number
  specName?: string
}

// 用户偏好类型
export interface UserPrefs {
  theme?: 'light' | 'dark' | 'auto'
  fontSize?: 'small' | 'medium' | 'large'
  language?: 'zh-CN' | 'en-US'
  lastVisited?: number
  [key: string]: unknown
}

// ==================== 数据库连接 ====================

let dbInstance: IDBDatabase | null = null

function openDB(): Promise<IDBDatabase> {
  if (dbInstance) return Promise.resolve(dbInstance)

  return new Promise((resolve, reject) => {
    if (typeof indexedDB === 'undefined') {
      reject(new Error('IndexedDB not supported'))
      return
    }

    const request = indexedDB.open(DB_NAME, DB_VERSION)

    request.onerror = () => reject(request.error)
    request.onsuccess = () => {
      dbInstance = request.result
      resolve(dbInstance)
    }

    request.onupgradeneeded = (event) => {
      const db = (event.target as IDBOpenDBRequest).result
      const oldVersion = event.oldVersion

      // 创建产品存储
      if (!db.objectStoreNames.contains(STORES.PRODUCTS)) {
        const productStore = db.createObjectStore(STORES.PRODUCTS, { keyPath: 'id' })
        productStore.createIndex('category', 'category', { unique: false })
        productStore.createIndex('merchantId', 'merchantId', { unique: false })
      }

      // 创建产品列表存储（缓存列表页数据）
      if (!db.objectStoreNames.contains(STORES.PRODUCT_LIST)) {
        db.createObjectStore(STORES.PRODUCT_LIST, { keyPath: 'key' })
      }

      // 创建用户偏好存储
      if (!db.objectStoreNames.contains(STORES.USER_PREFS)) {
        db.createObjectStore(STORES.USER_PREFS, { keyPath: 'key' })
      }

      // 创建购物车存储
      if (!db.objectStoreNames.contains(STORES.CART)) {
        const cartStore = db.createObjectStore(STORES.CART, { keyPath: 'productId' })
        cartStore.createIndex('specId', 'specId', { unique: false })
      }

      // 创建同步队列存储
      if (!db.objectStoreNames.contains(STORES.SYNC_QUEUE)) {
        const syncStore = db.createObjectStore(STORES.SYNC_QUEUE, { keyPath: 'id' })
        syncStore.createIndex('status', 'status', { unique: false })
        syncStore.createIndex('timestamp', 'timestamp', { unique: false })
      }

      // 创建心愿单存储
      if (!db.objectStoreNames.contains(STORES.WISHLIST)) {
        db.createObjectStore(STORES.WISHLIST, { keyPath: 'productId' })
      }
    }
  })
}

/**
 * 关闭数据库连接
 */
export function closeDB(): void {
  if (dbInstance) {
    dbInstance.close()
    dbInstance = null
  }
}

// ==================== 通用操作 ====================

async function storePut<T>(storeName: string, value: T): Promise<void> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, 'readwrite')
    const store = tx.objectStore(storeName)
    const request = store.put(value)
    request.onsuccess = () => resolve()
    request.onerror = () => reject(request.error)
  })
}

async function storeGet<T>(storeName: string, key: IDBValidKey): Promise<T | undefined> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, 'readonly')
    const store = tx.objectStore(storeName)
    const request = store.get(key)
    request.onsuccess = () => resolve(request.result as T | undefined)
    request.onerror = () => reject(request.error)
  })
}

async function storeGetAll<T>(storeName: string): Promise<T[]> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, 'readonly')
    const store = tx.objectStore(storeName)
    const request = store.getAll()
    request.onsuccess = () => resolve(request.result as T[])
    request.onerror = () => reject(request.error)
  })
}

async function storeDelete(storeName: string, key: IDBValidKey): Promise<void> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, 'readwrite')
    const store = tx.objectStore(storeName)
    const request = store.delete(key)
    request.onsuccess = () => resolve()
    request.onerror = () => reject(request.error)
  })
}

async function storeClear(storeName: string): Promise<void> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, 'readwrite')
    const store = tx.objectStore(storeName)
    const request = store.clear()
    request.onsuccess = () => resolve()
    request.onerror = () => reject(request.error)
  })
}

// ==================== 产品操作 ====================

/**
 * 保存产品数据
 * @param product - 产品数据
 */
export async function saveProduct(product: ProductData): Promise<void> {
  await storePut(STORES.PRODUCTS, product)
}

/**
 * 批量保存产品
 * @param products - 产品数组
 */
export async function saveProducts(products: ProductData[]): Promise<void> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORES.PRODUCTS, 'readwrite')
    const store = tx.objectStore(STORES.PRODUCTS)

    for (const product of products) {
      store.put(product)
    }

    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

/**
 * 获取单个产品
 * @param productId - 产品 ID
 * @returns 产品数据，不存在则返回 undefined
 */
export async function getProduct(productId: number): Promise<ProductData | undefined> {
  return storeGet<ProductData>(STORES.PRODUCTS, productId)
}

/**
 * 获取所有已缓存的产品
 * @returns 产品数组
 */
export async function getAllProducts(): Promise<ProductData[]> {
  return storeGetAll<ProductData>(STORES.PRODUCTS)
}

/**
 * 根据分类获取产品
 * @param category - 分类名称
 * @returns 产品数组
 */
export async function getProductsByCategory(category: string): Promise<ProductData[]> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORES.PRODUCTS, 'readonly')
    const store = tx.objectStore(STORES.PRODUCTS)
    const index = store.index('category')
    const request = index.getAll(category)
    request.onsuccess = () => resolve(request.result as ProductData[])
    request.onerror = () => reject(request.error)
  })
}

/**
 * 删除产品缓存
 * @param productId - 产品 ID
 */
export async function deleteProduct(productId: number): Promise<void> {
  await storeDelete(STORES.PRODUCTS, productId)
}

/**
 * 清除所有产品缓存
 */
export async function clearProducts(): Promise<void> {
  await storeClear(STORES.PRODUCTS)
}

/**
 * 获取缓存的产品数量
 */
export async function getProductCount(): Promise<number> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORES.PRODUCTS, 'readonly')
    const store = tx.objectStore(STORES.PRODUCTS)
    const request = store.count()
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
}

// ==================== 产品列表缓存 ====================

/**
 * 保存产品列表缓存
 * @param key - 缓存键（如 'hot', 'new', 'search:keyword'）
 * @param data - 列表数据
 */
export async function saveProductList(key: string, data: unknown): Promise<void> {
  await storePut(STORES.PRODUCT_LIST, { key, data, timestamp: Date.now() })
}

/**
 * 获取产品列表缓存
 * @param key - 缓存键
 * @returns 列表数据
 */
export async function getProductList(key: string): Promise<unknown | undefined> {
  const result = await storeGet<{ key: string; data: unknown; timestamp: number }>(
    STORES.PRODUCT_LIST,
    key
  )
  return result?.data
}

// ==================== 购物车操作 ====================

/**
 * 保存购物车商品
 * @param item - 购物车项
 */
export async function saveCartItem(item: CartItemData): Promise<void> {
  await storePut(STORES.CART, item)
}

/**
 * 批量保存购物车
 * @param items - 购物车项数组
 */
export async function saveCart(items: CartItemData[]): Promise<void> {
  await storeClear(STORES.CART)
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORES.CART, 'readwrite')
    const store = tx.objectStore(STORES.CART)

    for (const item of items) {
      store.put(item)
    }

    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

/**
 * 获取购物车所有商品
 * @returns 购物车项数组
 */
export async function getCart(): Promise<CartItemData[]> {
  return storeGetAll<CartItemData>(STORES.CART)
}

/**
 * 获取购物车商品数量
 */
export async function getCartCount(): Promise<number> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORES.CART, 'readonly')
    const store = tx.objectStore(STORES.CART)
    const request = store.count()
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
}

/**
 * 从购物车中移除商品
 * @param productId - 产品 ID
 */
export async function removeCartItem(productId: number): Promise<void> {
  await storeDelete(STORES.CART, productId)
}

/**
 * 清除购物车
 */
export async function clearCart(): Promise<void> {
  await storeClear(STORES.CART)
}

// ==================== 心愿单操作 ====================

/**
 * 添加到心愿单
 * @param productId - 产品 ID
 * @param productName - 产品名称
 */
export async function addToWishlist(productId: number, productName: string): Promise<void> {
  await storePut(STORES.WISHLIST, {
    productId,
    productName,
    addedAt: Date.now(),
  })
}

/**
 * 从心愿单移除
 * @param productId - 产品 ID
 */
export async function removeFromWishlist(productId: number): Promise<void> {
  await storeDelete(STORES.WISHLIST, productId)
}

/**
 * 获取心愿单所有商品 ID
 */
export async function getWishlist(): Promise<number[]> {
  const items = await storeGetAll<{ productId: number }>(STORES.WISHLIST)
  return items.map((item) => item.productId)
}

/**
 * 检查是否在心愿单中
 * @param productId - 产品 ID
 */
export async function isInWishlist(productId: number): Promise<boolean> {
  const result = await storeGet(STORES.WISHLIST, productId)
  return result !== undefined
}

// ==================== 用户偏好 ====================

/**
 * 保存用户偏好
 * @param key - 偏好键
 * @param value - 偏好值
 */
export async function saveUserPref(key: string, value: unknown): Promise<void> {
  await storePut(STORES.USER_PREFS, { key, value })
}

/**
 * 获取用户偏好
 * @param key - 偏好键
 * @returns 偏好值
 */
export async function getUserPref<T = unknown>(key: string): Promise<T | undefined> {
  const result = await storeGet<{ key: string; value: T }>(STORES.USER_PREFS, key)
  return result?.value
}

/**
 * 获取所有用户偏好
 */
export async function getAllUserPrefs(): Promise<UserPrefs> {
  const items = await storeGetAll<{ key: string; value: unknown }>(STORES.USER_PREFS)
  const prefs: UserPrefs = {}
  for (const item of items) {
    prefs[item.key] = item.value
  }
  return prefs
}

/**
 * 删除用户偏好
 * @param key - 偏好键
 */
export async function deleteUserPref(key: string): Promise<void> {
  await storeDelete(STORES.USER_PREFS, key)
}

// ==================== 同步队列操作 ====================

/**
 * 将操作加入离线同步队列
 * @param type - 操作类型
 * @param payload - 操作数据
 */
export async function enqueueOfflineAction(
  type: SyncQueueItem['type'],
  payload: Record<string, unknown>
): Promise<void> {
  const item: SyncQueueItem = {
    id: `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`,
    type,
    payload,
    timestamp: Date.now(),
    retryCount: 0,
    status: 'pending',
  }
  await storePut(STORES.SYNC_QUEUE, item)
}

/**
 * 获取所有待同步的操作
 * @returns 同步队列项数组
 */
export async function getPendingSyncActions(): Promise<SyncQueueItem[]> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORES.SYNC_QUEUE, 'readonly')
    const store = tx.objectStore(STORES.SYNC_QUEUE)
    const index = store.index('status')
    const request = index.getAll('pending')
    request.onsuccess = () => resolve(request.result as SyncQueueItem[])
    request.onerror = () => reject(request.error)
  })
}

/**
 * 获取待同步操作数量
 */
export async function getPendingSyncCount(): Promise<number> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORES.SYNC_QUEUE, 'readonly')
    const store = tx.objectStore(STORES.SYNC_QUEUE)
    const index = store.index('status')
    const request = index.count('pending')
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
}

/**
 * 更新同步队列项状态
 * @param id - 队列项 ID
 * @param status - 新状态
 * @param retryCount - 重试次数
 */
export async function updateSyncActionStatus(
  id: string,
  status: SyncQueueItem['status'],
  retryCount?: number
): Promise<void> {
  const item = await storeGet<SyncQueueItem>(STORES.SYNC_QUEUE, id)
  if (!item) return

  item.status = status
  if (retryCount !== undefined) {
    item.retryCount = retryCount
  }
  await storePut(STORES.SYNC_QUEUE, item)
}

/**
 * 从同步队列移除已完成项
 * @param id - 队列项 ID
 */
export async function removeSyncAction(id: string): Promise<void> {
  await storeDelete(STORES.SYNC_QUEUE, id)
}

/**
 * 清除所有同步队列
 */
export async function clearSyncQueue(): Promise<void> {
  await storeClear(STORES.SYNC_QUEUE)
}

/**
 * 同步所有离线操作
 * 遍历待同步队列，逐个执行回调
 * @param onSyncAction - 同步操作回调，返回 true 表示成功
 * @returns 同步结果统计
 */
export async function syncOfflineActions(
  onSyncAction: (item: SyncQueueItem) => Promise<boolean>
): Promise<{ success: number; failed: number }> {
  const pendingItems = await getPendingSyncActions()
  let success = 0
  let failed = 0

  for (const item of pendingItems) {
    try {
      // 标记为同步中
      await updateSyncActionStatus(item.id, 'syncing')

      const result = await onSyncAction(item)

      if (result) {
        await removeSyncAction(item.id)
        success++
      } else {
        const newRetryCount = item.retryCount + 1
        if (newRetryCount >= 5) {
          await updateSyncActionStatus(item.id, 'failed', newRetryCount)
        } else {
          await updateSyncActionStatus(item.id, 'pending', newRetryCount)
        }
        failed++
      }
    } catch {
      await updateSyncActionStatus(item.id, 'failed', item.retryCount + 1)
      failed++
    }
  }

  return { success, failed }
}

// ==================== 数据库管理 ====================

/**
 * 获取数据库使用统计
 */
export async function getDBStats(): Promise<{
  products: number
  productList: number
  cart: number
  wishlist: number
  syncQueue: number
}> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(
      [STORES.PRODUCTS, STORES.PRODUCT_LIST, STORES.CART, STORES.WISHLIST, STORES.SYNC_QUEUE],
      'readonly'
    )

    const stats = {
      products: 0,
      productList: 0,
      cart: 0,
      wishlist: 0,
      syncQueue: 0,
    }

    tx.objectStore(STORES.PRODUCTS).count().onsuccess = (e) => {
      stats.products = (e.target as IDBRequest).result
    }
    tx.objectStore(STORES.PRODUCT_LIST).count().onsuccess = (e) => {
      stats.productList = (e.target as IDBRequest).result
    }
    tx.objectStore(STORES.CART).count().onsuccess = (e) => {
      stats.cart = (e.target as IDBRequest).result
    }
    tx.objectStore(STORES.WISHLIST).count().onsuccess = (e) => {
      stats.wishlist = (e.target as IDBRequest).result
    }
    tx.objectStore(STORES.SYNC_QUEUE).count().onsuccess = (e) => {
      stats.syncQueue = (e.target as IDBRequest).result
    }

    tx.oncomplete = () => resolve(stats)
    tx.onerror = () => reject(tx.error)
  })
}

/**
 * 清除所有数据
 */
export async function clearAllData(): Promise<void> {
  const storeNames = Object.values(STORES)
  for (const name of storeNames) {
    await storeClear(name)
  }
}

export default {
  // 产品
  saveProduct,
  saveProducts,
  getProduct,
  getAllProducts,
  getProductsByCategory,
  deleteProduct,
  clearProducts,
  getProductCount,
  // 产品列表
  saveProductList,
  getProductList,
  // 购物车
  saveCartItem,
  saveCart,
  getCart,
  getCartCount,
  removeCartItem,
  clearCart,
  // 心愿单
  addToWishlist,
  removeFromWishlist,
  getWishlist,
  isInWishlist,
  // 用户偏好
  saveUserPref,
  getUserPref,
  getAllUserPrefs,
  deleteUserPref,
  // 同步队列
  enqueueOfflineAction,
  getPendingSyncActions,
  getPendingSyncCount,
  updateSyncActionStatus,
  removeSyncAction,
  clearSyncQueue,
  syncOfflineActions,
  // 管理
  getDBStats,
  clearAllData,
  closeDB,
}