/**
 * DataLoader 工厂.
 *
 * <p>为每个实体类型（User, Product, Order, Community 等）创建 DataLoader 实例。
 * 在单次 GraphQL 请求内，对相同 ID 的多次查询合并为一次批量后端请求，
 * 解决 N+1 查询问题。同时集成 Redis 缓存以减少跨请求的重复查询。
 *
 * <h3>缓存策略</h3>
 * <ul>
 *   <li>每个 DataLoader 查询结果自动写入 Redis</li>
 *   <li>TTL 由 cacheConfig 配置决定</li>
 *   <li>Mutation 时通过 invalidate 方法清除相关缓存</li>
 * </ul>
 */

import DataLoader from 'dataloader'
import { get, set, del, clear } from './redisCache'
import {
  buildCacheKey,
  getTTL,
  buildInvalidatePattern,
  shouldBypassCache
} from './cacheConfig'

/**
 * 批量加载函数签名：接收 ID 数组，返回对应实体数组（顺序一致）
 */
export type BatchLoadFn<K, V> = (keys: readonly K[]) => Promise<(V | Error)[]>

/**
 * DataLoader 工厂配置.
 */
export interface DataLoaderFactoryConfig<K = number, V = unknown> {
  /** 实体类型名称（用于缓存键前缀） */
  entityName: string
  /** 批量加载函数 */
  batchLoadFn: BatchLoadFn<K, V>
  /** 请求头（用于缓存绕过判断） */
  headers?: Record<string, string>
}

/**
 * 创建带 Redis 缓存层的 DataLoader 实例.
 *
 * <p>每个请求创建独立的 DataLoader，确保缓存隔离。
 * DataLoader 在单次请求内合并调用，结果同时写入 Redis 以实现跨请求缓存.
 *
 * @param config 配置
 * @returns DataLoader 实例
 */
export function createDataLoader<K = number, V = unknown>(
  config: DataLoaderFactoryConfig<K, V>
): DataLoader<K, V | null> {
  const { entityName, batchLoadFn, headers } = config
  const ttl = getTTL(entityName)
  const bypassCache = headers ? shouldBypassCache(headers) : false

  return new DataLoader<K, V | null>(async (keys: readonly K[]) => {
    if (bypassCache) {
      return batchLoadFn(keys) as Promise<(V | null)[]>
    }

    // 先检查 Redis 缓存中已有哪些结果
    const cacheKeys = keys.map((key) =>
      buildCacheKey(entityName, { id: key })
    )
    const cachedResults: (V | null)[] = []
    const missingKeys: K[] = []
    const missingIndices: number[] = []

    for (let i = 0; i < keys.length; i++) {
      const cached = await get<V>(cacheKeys[i])
      if (cached !== null) {
        cachedResults[i] = cached
      } else {
        missingKeys.push(keys[i])
        missingIndices.push(i)
      }
    }

    // 批量加载缓存未命中的 ID
    if (missingKeys.length > 0) {
      const loaded = await batchLoadFn(missingKeys)

      // 回填 Redis 缓存
      for (let i = 0; i < missingKeys.length; i++) {
        const result = loaded[i]
        if (result instanceof Error) {
          cachedResults[missingIndices[i]] = null
        } else {
          cachedResults[missingIndices[i]] = result as V | null
          // 异步写入缓存，不阻塞响应
          set(cacheKeys[missingIndices[i]], result, ttl).catch(() => {})
        }
      }
    }

    return cachedResults
  })
}

/**
 * 使指定实体的缓存失效.
 *
 * <p>当对应实体发生 mutation 时调用，清除所有相关缓存键.
 *
 * @param entityName 实体类型名称
 */
export async function invalidateEntityCache(entityName: string): Promise<void> {
  const pattern = buildInvalidatePattern(entityName)
  await clear(pattern)
}

/**
 * 使指定实体的单个 ID 缓存失效.
 *
 * @param entityName 实体类型名称
 * @param id 实体 ID
 */
export async function invalidateEntityById(entityName: string, id: number | string): Promise<void> {
  const key = buildCacheKey(entityName, { id })
  await del(key)
}

/**
 * 获取或设置缓存（带 Redis 回退）.
 *
 * <p>用于非 DataLoader 场景的直接缓存读写.
 *
 * @param queryName 查询名称
 * @param args 查询参数
 * @param fetcher 数据获取函数
 * @returns 数据
 */
export async function getOrSetCached<T>(
  queryName: string,
  args: unknown,
  fetcher: () => Promise<T>
): Promise<T> {
  const key = buildCacheKey(queryName, args)
  const ttl = getTTL(queryName)
  const { getOrSet } = await import('./redisCache')
  return getOrSet(key, ttl, fetcher)
}

/**
 * 预定义的 DataLoader 工厂集合.
 *
 * <p>每个工厂函数接收 headers 参数，返回该实体类型的 DataLoader 实例.
 * 在每请求的 createContext 中调用，确保请求隔离.
 */
export const dataLoaderFactories = {
  /** 商品 DataLoader */
  product: (headers: Record<string, string>, batchFn: BatchLoadFn<number, unknown>) =>
    createDataLoader<number, unknown>({ entityName: 'product', batchLoadFn: batchFn, headers }),

  /** 用户 DataLoader */
  user: (headers: Record<string, string>, batchFn: BatchLoadFn<number, unknown>) =>
    createDataLoader<number, unknown>({ entityName: 'user', batchLoadFn: batchFn, headers }),

  /** 订单 DataLoader */
  order: (headers: Record<string, string>, batchFn: BatchLoadFn<number, unknown>) =>
    createDataLoader<number, unknown>({ entityName: 'order', batchLoadFn: batchFn, headers }),

  /** 社区 DataLoader */
  community: (headers: Record<string, string>, batchFn: BatchLoadFn<number, unknown>) =>
    createDataLoader<number, unknown>({ entityName: 'community', batchLoadFn: batchFn, headers }),

  /** 分类 DataLoader */
  category: (headers: Record<string, string>, batchFn: BatchLoadFn<number, unknown>) =>
    createDataLoader<number, unknown>({ entityName: 'category', batchLoadFn: batchFn, headers }),
}