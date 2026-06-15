/**
 * 离线存储工具 - 基于 IndexedDB + localStorage 的离线数据管理
 *
 * 提供统一的离线数据存储接口，支持：
 * - 大数据量使用 IndexedDB 存储
 * - 小数据量使用 localStorage 存储
 * - 待同步数据队列管理
 */

const DB_NAME = 'TailorISOfflineDB'
const DB_VERSION = 1
const STORE_NAME = 'offlineData'
const SYNC_QUEUE_KEY = '__offline_sync_queue__'

// ==================== 存储限制常量 ====================
/** 微信小程序 localStorage 上限约 10MB，留 5MB 安全余量 */
const MAX_STORAGE_SIZE = 5 * 1024 * 1024 // 5MB
/** 单条数据阈值，超过此值强制使用 IndexedDB */
const SINGLE_ITEM_THRESHOLD = 1024 * 1024 // 1MB
/** 分块大小，每块不超过 500KB 以避免单次写入过大 */
const CHUNK_SIZE = 500 * 1024

export interface SyncQueueItem {
  id: string
  type: 'create' | 'update' | 'delete'
  entity: string
  payload: Record<string, unknown>
  timestamp: number
  retryCount: number
}

// ==================== IndexedDB 操作 ====================

function openDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    if (typeof indexedDB === 'undefined') {
      reject(new Error('IndexedDB not supported'))
      return
    }
    const request = indexedDB.open(DB_NAME, DB_VERSION)

    request.onerror = () => reject(request.error)
    request.onsuccess = () => resolve(request.result)

    request.onupgradeneeded = (event) => {
      const db = (event.target as IDBOpenDBRequest).result
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME, { keyPath: 'key' })
      }
    }
  })
}

async function idbPut(key: string, value: unknown): Promise<void> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    const store = tx.objectStore(STORE_NAME)
    const request = store.put({ key, value, timestamp: Date.now() })
    request.onsuccess = () => resolve()
    request.onerror = () => reject(request.error)
  })
}

async function idbGet(key: string): Promise<unknown | undefined> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readonly')
    const store = tx.objectStore(STORE_NAME)
    const request = store.get(key)
    request.onsuccess = () => {
      resolve(request.result ? request.result.value : undefined)
    }
    request.onerror = () => reject(request.error)
  })
}

async function idbDelete(key: string): Promise<void> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    const store = tx.objectStore(STORE_NAME)
    const request = store.delete(key)
    request.onsuccess = () => resolve()
    request.onerror = () => reject(request.error)
  })
}

async function idbClear(): Promise<void> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    const store = tx.objectStore(STORE_NAME)
    const request = store.clear()
    request.onsuccess = () => resolve()
    request.onerror = () => reject(request.error)
  })
}

async function idbGetAllKeys(): Promise<string[]> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readonly')
    const store = tx.objectStore(STORE_NAME)
    const request = store.getAllKeys()
    request.onsuccess = () => resolve(request.result as string[])
    request.onerror = () => reject(request.error)
  })
}

// ==================== 存储大小工具 ====================

/** 计算 JSON 数据的字节大小 */
function estimateSize(value: unknown): number {
  return new Blob([typeof value === 'string' ? value : JSON.stringify(value)]).size
}

/** 估算 localStorage 已用大小（粗略估计） */
export function getLsUsedSize(): number {
  let total = 0
  try {
    // uni.getStorageInfoSync 返回 storageInfo，包含 currentSize 和 limitSize
    const info: { currentSize?: number } = uni.getStorageInfoSync()
    total = info.currentSize ?? 0
  } catch {
    // 降级：不抛出异常
  }
  return total
}

/** 检查写入后是否超出安全上限 */
function isStorageSpaceSufficient(key: string, dataSize: number): boolean {
  const currentSize = getLsUsedSize()
  // 如果是更新操作，减去旧值大小
  const oldData = lsGetRaw(key)
  if (oldData !== undefined) {
    const oldSize = estimateSize(oldData)
    const expectedSize = currentSize - oldSize + dataSize
    return expectedSize <= MAX_STORAGE_SIZE
  }
  return (currentSize + dataSize) <= MAX_STORAGE_SIZE
}

// ==================== localStorage 操作 ====================

function lsPut(key: string, value: unknown): boolean {
  const serialized = JSON.stringify({ value, timestamp: Date.now() })
  const dataSize = new Blob([serialized]).size

  if (!isStorageSpaceSufficient(key, dataSize)) {
    console.warn('[offlineStorage] localStorage 空间不足，数据将被截断或降级')
  }

  // 如果单条数据超过分块阈值，使用分块存储
  if (dataSize > SINGLE_ITEM_THRESHOLD) {
    return lsPutChunked(key, value)
  }

  try {
    uni.setStorageSync(key, serialized)
    return true
  } catch (e) {
    console.error('[offlineStorage] localStorage write error:', e)
    return false
  }
}

/** 分块写入大数据到 localStorage */
function lsPutChunked(key: string, value: unknown): boolean {
  const serialized = JSON.stringify(value)
  const chunks: string[] = []
  for (let i = 0; i < serialized.length; i += CHUNK_SIZE) {
    chunks.push(serialized.substring(i, i + CHUNK_SIZE))
  }

  const meta = {
    chunked: true,
    total: chunks.length,
    timestamp: Date.now(),
  }

  try {
    // 先写入元数据
    uni.setStorageSync(key + ':meta', JSON.stringify(meta))
    // 逐个写入分块
    for (let i = 0; i < chunks.length; i++) {
      uni.setStorageSync(key + ':chunk:' + i, chunks[i])
    }
    return true
  } catch (e) {
    console.error('[offlineStorage] chunked localStorage write error:', e)
    // 清理已写入的部分
    lsDelete(key + ':meta')
    for (let i = 0; i < chunks.length; i++) {
      lsDelete(key + ':chunk:' + i)
    }
    return false
  }
}

/** 分块读取 localStorage 数据 */
function lsGetChunked(key: string): unknown | undefined {
  try {
    const metaRaw = uni.getStorageSync(key + ':meta')
    if (!metaRaw) return undefined
    const meta = typeof metaRaw === 'string' ? JSON.parse(metaRaw) : metaRaw
    if (!meta.chunked) return undefined

    const chunks: string[] = []
    for (let i = 0; i < meta.total; i++) {
      const chunk = uni.getStorageSync(key + ':chunk:' + i)
      if (chunk === null || chunk === undefined) {
        console.warn(`[offlineStorage] chunk ${i} missing for key ${key}`)
        return undefined
      }
      chunks.push(chunk)
    }
    return JSON.parse(chunks.join(''))
  } catch (e) {
    console.error('[offlineStorage] chunked localStorage read error:', e)
    return undefined
  }
}

/** 分块删除 localStorage 数据 */
function lsDeleteChunked(key: string): void {
  lsDeleteRaw(key + ':meta')
  // 尝试删除可能的分块（最大 100 个）
  for (let i = 0; i < 100; i++) {
    const chunkKey = key + ':chunk:' + i
    try {
      if (uni.getStorageSync(chunkKey) === null) break
      uni.removeStorageSync(chunkKey)
    } catch {
      break
    }
  }
}

/** 统一的 lsGet：自动检测分块数据 */
function lsGet(key: string): unknown | undefined {
  // 先尝试分块读取
  const chunkedData = lsGetChunked(key)
  if (chunkedData !== undefined) return chunkedData
  // 降级到普通读取
  return lsGetRaw(key)
}

/** 统一的 lsDelete：自动处理分块数据 */
function lsDelete(key: string): void {
  // 先尝试分块删除
  lsDeleteChunked(key)
  // 再执行普通删除（兼容非分块数据）
  lsDeleteRaw(key)
}

// ==================== 同步队列操作 ====================

function getSyncQueue(): SyncQueueItem[] {
  try {
    const raw = uni.getStorageSync(SYNC_QUEUE_KEY)
    if (!raw) return []
    return typeof raw === 'string' ? JSON.parse(raw) : raw
  } catch {
    return []
  }
}

function saveSyncQueue(queue: SyncQueueItem[]): void {
  try {
    uni.setStorageSync(SYNC_QUEUE_KEY, JSON.stringify(queue))
  } catch (e) {
    console.error('[offlineStorage] sync queue save error:', e)
  }
}

// ==================== 公共 API ====================

/**
 * 保存离线数据
 * @param key - 数据键
 * @param data - 数据值
 * @param useIDB - 是否强制使用 IndexedDB（默认自动判断：大于 5KB 使用 IDB）
 */
export async function saveOfflineData(key: string, data: unknown, useIDB?: boolean): Promise<void> {
  const dataStr = typeof data === 'string' ? data : JSON.stringify(data)
  const shouldUseIDB = useIDB ?? (new Blob([dataStr]).size > 5 * 1024)

  if (shouldUseIDB) {
    await idbPut(key, data)
  } else {
    lsPut(key, data)
  }
}

/**
 * 获取离线数据
 * @param key - 数据键
 * @returns 数据值，不存在则返回 undefined
 */
export async function getOfflineData(key: string): Promise<unknown | undefined> {
  // 优先从 IndexedDB 获取
  try {
    const idbData = await idbGet(key)
    if (idbData !== undefined) return idbData
  } catch {
    // IDB 不可用时降级到 localStorage
  }
  return lsGet(key)
}

/**
 * 删除离线数据
 * @param key - 数据键
 */
export async function deleteOfflineData(key: string): Promise<void> {
  try {
    await idbDelete(key)
  } catch {
    // ignore
  }
  lsDelete(key)
}

/**
 * 清空所有离线数据
 */
export async function clearAllOfflineData(): Promise<void> {
  try {
    await idbClear()
  } catch {
    // ignore
  }
  try {
    uni.removeStorageSync(SYNC_QUEUE_KEY)
  } catch {
    // ignore
  }
}

/**
 * 获取所有已存储的离线数据键
 */
export async function getAllOfflineKeys(): Promise<string[]> {
  const keys: string[] = []
  try {
    keys.push(...(await idbGetAllKeys()))
  } catch {
    // ignore
  }
  return keys
}

/**
 * 将操作加入同步队列
 * @param item - 同步队列项
 */
export function enqueueSync(item: Omit<SyncQueueItem, 'id' | 'timestamp' | 'retryCount'>): void {
  const queue = getSyncQueue()
  const newItem: SyncQueueItem = {
    ...item,
    id: generateId(),
    timestamp: Date.now(),
    retryCount: 0
  }
  queue.push(newItem)
  saveSyncQueue(queue)
}

/**
 * 获取同步队列
 */
export function getSyncQueueItems(): SyncQueueItem[] {
  return getSyncQueue()
}

/**
 * 从同步队列移除指定项
 * @param id - 队列项 ID
 */
export function dequeueSync(id: string): void {
  const queue = getSyncQueue().filter(item => item.id !== id)
  saveSyncQueue(queue)
}

/**
 * 更新同步队列项的重试次数
 * @param id - 队列项 ID
 * @param retryCount - 新的重试次数
 */
export function updateSyncRetryCount(id: string, retryCount: number): void {
  const queue = getSyncQueue()
  const item = queue.find(i => i.id === id)
  if (item) {
    item.retryCount = retryCount
    saveSyncQueue(queue)
  }
}

/**
 * 清空同步队列
 */
export function clearSyncQueue(): void {
  saveSyncQueue([])
}

/**
 * 获取存储使用统计
 */
export async function getStorageStats(): Promise<{ idbKeys: number; syncQueueLength: number }> {
  let idbKeys = 0
  try {
    idbKeys = (await idbGetAllKeys()).length
  } catch {
    // ignore
  }
  return {
    idbKeys,
    syncQueueLength: getSyncQueue().length
  }
}

function generateId(): string {
  return `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`
}

export default {
  saveOfflineData,
  getOfflineData,
  deleteOfflineData,
  clearAllOfflineData,
  getAllOfflineKeys,
  enqueueSync,
  getSyncQueueItems,
  dequeueSync,
  updateSyncRetryCount,
  clearSyncQueue,
  getStorageStats
}
