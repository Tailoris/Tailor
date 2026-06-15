/**
 * 产品缓存工具 - 基于 TTL 的离线产品数据缓存
 *
 * 支持：
 * - 产品列表缓存（带分页参数区分）
 * - 产品详情缓存
 * - 分类数据缓存
 * - TTL 过期自动失效
 * - 缓存大小控制
 */

import { saveOfflineData, getOfflineData, deleteOfflineData, getAllOfflineKeys } from './offlineStorage'

export interface CacheEntry<T = unknown> {
  data: T
  timestamp: number
  ttl: number
  version?: string
}

// 默认 TTL 配置（毫秒）
const DEFAULT_TTL = {
  productDetail: 30 * 60 * 1000,       // 产品详情 30 分钟
  productList: 15 * 60 * 1000,         // 产品列表 15 分钟
  categories: 60 * 60 * 1000,          // 分类 60 分钟
  seckill: 5 * 60 * 1000,              // 秒杀 5 分钟
  hot: 10 * 60 * 1000,                 // 热门 10 分钟
  search: 10 * 60 * 1000               // 搜索 10 分钟
}

// 缓存键前缀
const CACHE_PREFIX = 'product_cache_'
const MAX_CACHE_ITEMS = 200             // 最大缓存条目数

// ==================== 内部辅助函数 ====================

function buildCacheKey(type: string, params?: Record<string, unknown>): string {
  const paramsStr = params ? JSON.stringify(params) : ''
  return `${CACHE_PREFIX}${type}${paramsStr ? '_' + btoa(encodeURIComponent(paramsStr)) : ''}`
}

async function setCache<T>(key: string, data: T, ttl: number): Promise<void> {
  const entry: CacheEntry<T> = {
    data,
    timestamp: Date.now(),
    ttl
  }
  await saveOfflineData(key, entry)
}

async function getCache<T>(key: string): Promise<T | null> {
  const raw = await getOfflineData(key)
  if (!raw) return null

  const entry = raw as CacheEntry<T>
  const now = Date.now()

  // 检查 TTL 是否过期
  if (now - entry.timestamp > entry.ttl) {
    await deleteOfflineData(key)
    return null
  }

  return entry.data
}

async function cleanupExpiredCache(): Promise<void> {
  const keys = await getAllOfflineKeys()
  const productKeys = keys.filter(k => k.startsWith(CACHE_PREFIX))

  for (const key of productKeys) {
    const raw = await getOfflineData(key)
    if (raw) {
      const entry = raw as CacheEntry
      if (Date.now() - entry.timestamp > entry.ttl) {
        await deleteOfflineData(key)
      }
    }
  }
}

async function enforceMaxCacheSize(): Promise<void> {
  const keys = await getAllOfflineKeys()
  const productKeys = keys.filter(k => k.startsWith(CACHE_PREFIX))

  if (productKeys.length > MAX_CACHE_ITEMS) {
    // 按时间排序，删除最旧的
    const entries: { key: string; timestamp: number }[] = []

    for (const key of productKeys) {
      const raw = await getOfflineData(key)
      if (raw) {
        const entry = raw as CacheEntry
        entries.push({ key, timestamp: entry.timestamp })
      }
    }

    entries.sort((a, b) => a.timestamp - b.timestamp)
    const toDelete = entries.slice(0, entries.length - MAX_CACHE_ITEMS)

    for (const item of toDelete) {
      await deleteOfflineData(item.key)
    }
  }
}

// ==================== 公共 API ====================

/**
 * 缓存产品详情
 * @param productId - 产品 ID
 * @param data - 产品数据
 * @param ttl - 过期时间（毫秒），默认 30 分钟
 */
export async function cacheProductDetail(productId: number, data: unknown, ttl?: number): Promise<void> {
  const key = buildCacheKey('productDetail', { id: productId })
  await setCache(key, data, ttl ?? DEFAULT_TTL.productDetail)
  await enforceMaxCacheSize()
}

/**
 * 获取缓存的产品详情
 * @param productId - 产品 ID
 * @returns 产品数据，缓存不存在或过期则返回 null
 */
export async function getCachedProductDetail(productId: number): Promise<unknown | null> {
  const key = buildCacheKey('productDetail', { id: productId })
  return getCache(key)
}

/**
 * 缓存产品列表
 * @param params - 分页参数
 * @param data - 列表数据
 * @param ttl - 过期时间（毫秒），默认 15 分钟
 */
export async function cacheProductList(params: Record<string, unknown>, data: unknown, ttl?: number): Promise<void> {
  const key = buildCacheKey('productList', params)
  await setCache(key, data, ttl ?? DEFAULT_TTL.productList)
  await enforceMaxCacheSize()
}

/**
 * 获取缓存的产品列表
 * @param params - 分页参数
 * @returns 列表数据，缓存不存在或过期则返回 null
 */
export async function getCachedProductList(params: Record<string, unknown>): Promise<unknown | null> {
  const key = buildCacheKey('productList', params)
  return getCache(key)
}

/**
 * 缓存分类数据
 * @param data - 分类数据
 * @param ttl - 过期时间（毫秒），默认 60 分钟
 */
export async function cacheCategories(data: unknown, ttl?: number): Promise<void> {
  const key = buildCacheKey('categories')
  await setCache(key, data, ttl ?? DEFAULT_TTL.categories)
}

/**
 * 获取缓存的分类数据
 * @returns 分类数据，缓存不存在或过期则返回 null
 */
export async function getCachedCategories(): Promise<unknown | null> {
  const key = buildCacheKey('categories')
  return getCache(key)
}

/**
 * 缓存秒杀产品
 * @param data - 秒杀产品数据
 * @param ttl - 过期时间（毫秒），默认 5 分钟
 */
export async function cacheSeckillProducts(data: unknown, ttl?: number): Promise<void> {
  const key = buildCacheKey('seckill')
  await setCache(key, data, ttl ?? DEFAULT_TTL.seckill)
}

/**
 * 获取缓存的秒杀产品
 * @returns 秒杀产品数据，缓存不存在或过期则返回 null
 */
export async function getCachedSeckillProducts(): Promise<unknown | null> {
  const key = buildCacheKey('seckill')
  return getCache(key)
}

/**
 * 缓存热门产品
 * @param data - 热门产品数据
 * @param ttl - 过期时间（毫秒），默认 10 分钟
 */
export async function cacheHotProducts(data: unknown, ttl?: number): Promise<void> {
  const key = buildCacheKey('hot')
  await setCache(key, data, ttl ?? DEFAULT_TTL.hot)
}

/**
 * 获取缓存的热门产品
 * @returns 热门产品数据，缓存不存在或过期则返回 null
 */
export async function getCachedHotProducts(): Promise<unknown | null> {
  const key = buildCacheKey('hot')
  return getCache(key)
}

/**
 * 缓存搜索结果
 * @param keyword - 搜索关键词
 * @param params - 分页参数
 * @param data - 搜索结果数据
 * @param ttl - 过期时间（毫秒），默认 10 分钟
 */
export async function cacheSearchResults(
  keyword: string,
  params: Record<string, unknown>,
  data: unknown,
  ttl?: number
): Promise<void> {
  const key = buildCacheKey('search', { keyword, ...params })
  await setCache(key, data, ttl ?? DEFAULT_TTL.search)
  await enforceMaxCacheSize()
}

/**
 * 获取缓存的搜索结果
 * @param keyword - 搜索关键词
 * @param params - 分页参数
 * @returns 搜索结果数据，缓存不存在或过期则返回 null
 */
export async function getCachedSearchResults(
  keyword: string,
  params: Record<string, unknown>
): Promise<unknown | null> {
  const key = buildCacheKey('search', { keyword, ...params })
  return getCache(key)
}

/**
 * 清除指定产品 ID 的详情缓存
 * @param productId - 产品 ID
 */
export async function invalidateProductCache(productId: number): Promise<void> {
  const key = buildCacheKey('productDetail', { id: productId })
  await deleteOfflineData(key)
}

/**
 * 清除所有产品缓存
 */
export async function clearAllProductCache(): Promise<void> {
  const keys = await getAllOfflineKeys()
  const productKeys = keys.filter(k => k.startsWith(CACHE_PREFIX))
  for (const key of productKeys) {
    await deleteOfflineData(key)
  }
}

/**
 * 清理过期缓存（建议定时调用）
 */
export async function cleanupProductCache(): Promise<void> {
  await cleanupExpiredCache()
  await enforceMaxCacheSize()
}

/**
 * 获取缓存统计信息
 */
export async function getProductCacheStats(): Promise<{ count: number; keys: string[] }> {
  const keys = await getAllOfflineKeys()
  const productKeys = keys.filter(k => k.startsWith(CACHE_PREFIX))
  return { count: productKeys.length, keys: productKeys }
}

/**
 * 检查产品是否有可用缓存
 * @param productId - 产品 ID
 * @returns 是否有未过期的缓存
 */
export async function hasProductCache(productId: number): Promise<boolean> {
  const cached = await getCachedProductDetail(productId)
  return cached !== null
}

export default {
  cacheProductDetail,
  getCachedProductDetail,
  cacheProductList,
  getCachedProductList,
  cacheCategories,
  getCachedCategories,
  cacheSeckillProducts,
  getCachedSeckillProducts,
  cacheHotProducts,
  getCachedHotProducts,
  cacheSearchResults,
  getCachedSearchResults,
  invalidateProductCache,
  clearAllProductCache,
  cleanupProductCache,
  getProductCacheStats,
  hasProductCache
}
