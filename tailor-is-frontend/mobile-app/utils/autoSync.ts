/**
 * 自动同步机制 - 离线操作队列管理与自动同步
 *
 * 功能：
 * - 管理待同步操作队列
 * - 网络恢复时自动触发同步
 * - 指数退避重试策略
 * - 同步冲突检测与处理
 * - 同步进度通知
 */

import {
  getSyncQueueItems,
  dequeueSync,
  updateSyncRetryCount,
  clearSyncQueue,
  SyncQueueItem
} from './offlineStorage'
import { isOnline, isWeakNetwork, onNetworkChange } from './networkMonitor'

// 同步配置
const SYNC_CONFIG = {
  maxRetries: 5,                  // 最大重试次数
  baseDelay: 2000,                // 基础延迟 (ms)
  maxDelay: 60000,                // 最大延迟 (ms)
  concurrentLimit: 3,             // 并发同步数
  conflictStrategy: 'server' as ConflictStrategy  // 冲突处理策略
}

export type ConflictStrategy = 'server' | 'local' | 'newest' | 'manual'

export interface SyncResult {
  success: boolean
  itemId: string
  error?: string
  conflict?: boolean
}

export type SyncProgressCallback = (progress: SyncProgress) => void

export interface SyncProgress {
  total: number
  completed: number
  current: SyncQueueItem | null
  status: 'idle' | 'syncing' | 'paused' | 'error'
  results: SyncResult[]
}

// 同步状态
let syncProgress: SyncProgress = {
  total: 0,
  completed: 0,
  current: null,
  status: 'idle',
  results: []
}

let isSyncing = false
let isPaused = false
let progressCallbacks: Set<SyncProgressCallback> = new Set()
let syncTimer: number | null = null

// ==================== 同步 API 映射 ====================

// 不同实体的同步处理函数映射
type SyncHandler = (item: SyncQueueItem) => Promise<void>
const syncHandlers: Record<string, SyncHandler> = {}

/**
 * 注册实体同步处理函数
 * @param entity - 实体名称
 * @param handler - 同步处理函数
 */
export function registerSyncHandler(entity: string, handler: SyncHandler): void {
  syncHandlers[entity] = handler
}

// ==================== 冲突处理 ====================

export interface ConflictInfo {
  itemId: string
  entity: string
  localData: Record<string, unknown>
  serverData?: Record<string, unknown>
  strategy: ConflictStrategy
}

/**
 * 处理同步冲突
 * @param conflict - 冲突信息
 * @returns 最终使用的数据
 */
export async function handleConflict(conflict: ConflictInfo): Promise<Record<string, unknown> | null> {
  switch (conflict.strategy) {
    case 'server':
      // 以服务器数据为准
      return conflict.serverData || null

    case 'local':
      // 以本地数据为准
      return conflict.localData

    case 'newest':
      // 以时间最新的数据为准
      return conflict.localData  // 本地通常更新

    case 'manual':
      // 手动处理：显示冲突对话框
      return await showConflictDialog(conflict)

    default:
      return conflict.serverData || conflict.localData
  }
}

async function showConflictDialog(conflict: ConflictInfo): Promise<Record<string, unknown> | null> {
  return new Promise((resolve) => {
    uni.showModal({
      title: '数据冲突',
      content: `本地数据与服务器数据存在冲突，是否保留本地修改？\n\n本地: ${JSON.stringify(conflict.localData).substring(0, 50)}...`,
      confirmText: '保留本地',
      cancelText: '使用服务器',
      success: (res) => {
        resolve(res.confirm ? conflict.localData : null)
      },
      fail: () => {
        resolve(null)
      }
    })
  })
}

// ==================== 重试策略 ====================

/**
 * 计算重试延迟（指数退避 + 随机抖动）
 * @param retryCount - 当前重试次数
 * @returns 延迟时间 (ms)
 */
function calculateRetryDelay(retryCount: number): number {
  const exponentialDelay = SYNC_CONFIG.baseDelay * Math.pow(2, retryCount)
  const jitter = Math.random() * 1000  // 随机抖动避免雷击效应
  return Math.min(exponentialDelay + jitter, SYNC_CONFIG.maxDelay)
}

/**
 * 检查是否应该重试
 * @param retryCount - 当前重试次数
 * @returns 是否应该重试
 */
function shouldRetry(retryCount: number): boolean {
  return retryCount < SYNC_CONFIG.maxRetries
}

// ==================== 核心同步逻辑 ====================

/**
 * 执行单个同步项
 * @param item - 同步队列项
 * @returns 同步结果
 */
async function syncSingleItem(item: SyncQueueItem): Promise<SyncResult> {
  const handler = syncHandlers[item.entity]

  if (!handler) {
    console.warn(`[autoSync] No handler registered for entity: ${item.entity}`)
    return {
      success: false,
      itemId: item.id,
      error: `No handler for entity: ${item.entity}`
    }
  }

  try {
    await handler(item)
    return { success: true, itemId: item.id }
  } catch (error) {
    const errorMsg = error instanceof Error ? error.message : 'Unknown error'
    return { success: false, itemId: item.id, error: errorMsg }
  }
}

/**
 * 执行同步队列
 */
export async function executeSync(): Promise<SyncResult[]> {
  if (isSyncing) {
    console.log('[autoSync] Sync already in progress')
    return syncProgress.results
  }

  if (!isOnline.value) {
    console.log('[autoSync] Offline, skipping sync')
    return []
  }

  if (isWeakNetwork.value) {
    console.log('[autoSync] Weak network, delaying sync')
    // 弱网时延迟同步
    scheduleRetry(calculateRetryDelay(0))
    return []
  }

  isSyncing = true
  isPaused = false
  syncProgress = {
    total: 0,
    completed: 0,
    current: null,
    status: 'syncing',
    results: []
  }

  notifyProgress()

  const queue = getSyncQueueItems()
  syncProgress.total = queue.length

  if (queue.length === 0) {
    syncProgress.status = 'idle'
    isSyncing = false
    notifyProgress()
    return []
  }

  console.log(`[autoSync] Starting sync with ${queue.length} items`)

  // 分批同步（控制并发）
  const results: SyncResult[] = []

  for (let i = 0; i < queue.length; i += SYNC_CONFIG.concurrentLimit) {
    if (isPaused || !isOnline.value) {
      syncProgress.status = isOnline.value ? 'paused' : 'idle'
      break
    }

    const batch = queue.slice(i, i + SYNC_CONFIG.concurrentLimit)
    const batchPromises = batch.map(async (item) => {
      syncProgress.current = item
      notifyProgress()

      const result = await syncSingleItem(item)
      results.push(result)

      if (result.success) {
        // 同步成功，从队列移除
        dequeueSync(item.id)
      } else {
        // 同步失败，增加重试计数
        updateSyncRetryCount(item.id, item.retryCount + 1)

        // 检查是否超过最大重试次数
        if (!shouldRetry(item.retryCount + 1)) {
          console.warn(`[autoSync] Item ${item.id} exceeded max retries`)
        }
      }

      syncProgress.completed++
      syncProgress.results = results
      notifyProgress()

      return result
    })

    await Promise.allSettled(batchPromises)

    // 批次间小延迟，避免对服务器造成压力
    if (i + SYNC_CONFIG.concurrentLimit < queue.length) {
      await delay(500)
    }
  }

  // 检查是否有需要重试的项
  const remainingQueue = getSyncQueueItems()
  if (remainingQueue.length > 0 && isOnline.value) {
    // 还有未同步的项，安排重试
    scheduleRetry(calculateRetryDelay(
      remainingQueue.length > 0
        ? Math.min(...remainingQueue.map(i => i.retryCount))
        : 1
    ))
  }

  isSyncing = false
  syncProgress.status = 'idle'
  syncProgress.current = null
  notifyProgress()

  console.log(`[autoSync] Sync completed: ${results.filter(r => r.success).length}/${results.length} successful`)
  return results
}

/**
 * 触发同步（供外部调用）
 */
export function triggerSync(): void {
  if (isSyncing) return
  executeSync()
}

/**
 * 暂停同步
 */
export function pauseSync(): void {
  isPaused = true
  if (syncTimer !== null) {
    clearTimeout(syncTimer)
    syncTimer = null
  }
  syncProgress.status = 'paused'
  notifyProgress()
}

/**
 * 恢复同步
 */
export function resumeSync(): void {
  isPaused = false
  if (isOnline.value && !isSyncing) {
    executeSync()
  }
}

/**
 * 取消所有待同步项
 */
export function cancelAllSync(): void {
  clearSyncQueue()
  if (syncTimer !== null) {
    clearTimeout(syncTimer)
    syncTimer = null
  }
  isSyncing = false
  isPaused = false
  syncProgress = {
    total: 0,
    completed: 0,
    current: null,
    status: 'idle',
    results: []
  }
  notifyProgress()
}

/**
 * 安排重试
 * @param delayMs - 延迟时间 (ms)
 */
function scheduleRetry(delayMs: number): void {
  if (syncTimer !== null) {
    clearTimeout(syncTimer)
  }

  syncTimer = window.setTimeout(() => {
    syncTimer = null
    if (isOnline.value && !isWeakNetwork.value && !isSyncing) {
      executeSync()
    }
  }, delayMs)
}

// ==================== 进度通知 ====================

/**
 * 订阅同步进度
 * @param callback - 进度回调
 * @returns 取消订阅函数
 */
export function onSyncProgress(callback: SyncProgressCallback): () => void {
  progressCallbacks.add(callback)
  // 立即通知当前状态
  callback(syncProgress)
  return () => {
    progressCallbacks.delete(callback)
  }
}

function notifyProgress(): void {
  progressCallbacks.forEach(cb => cb({ ...syncProgress }))
}

// ==================== 自动初始化 ====================

/**
 * 初始化自动同步
 * 在应用启动时调用
 */
export function initAutoSync(): void {
  // 监听网络变化，在线时自动触发同步
  onNetworkChange((status) => {
    if (status.online && !status.weak) {
      console.log('[autoSync] Network recovered, triggering sync')
      // 延迟 3 秒后同步，确保网络稳定
      setTimeout(() => {
        triggerSync()
      }, 3000)
    }
  })

  console.log('[autoSync] Initialized')
}

/**
 * 获取当前同步状态
 */
export function getSyncStatus(): SyncProgress {
  return { ...syncProgress }
}

/**
 * 检查是否有待同步项
 */
export function hasPendingSync(): boolean {
  return getSyncQueueItems().length > 0
}

/**
 * 获取待同步项数量
 */
export function getPendingSyncCount(): number {
  return getSyncQueueItems().length
}

// ==================== 工具函数 ====================

function delay(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms))
}

export default {
  initAutoSync,
  triggerSync,
  executeSync,
  pauseSync,
  resumeSync,
  cancelAllSync,
  registerSyncHandler,
  handleConflict,
  onSyncProgress,
  getSyncStatus,
  hasPendingSync,
  getPendingSyncCount
}
