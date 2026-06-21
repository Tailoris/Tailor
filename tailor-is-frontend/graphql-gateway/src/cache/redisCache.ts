/**
 * Redis 缓存服务.
 *
 * <p>封装 ioredis 客户端，提供 JSON 序列化/反序列化的缓存读写操作。
 * 支持 get/set/del/clear 基本操作，以及基于 SCAN 的模式匹配清除。
 *
 * <h3>连接配置</h3>
 * <ul>
 *   <li>host: REDIS_HOST 环境变量，默认 localhost</li>
 *   <li>port: REDIS_PORT 环境变量，默认 6379</li>
 *   <li>password: REDIS_PASSWORD 环境变量</li>
 *   <li>db: 2（使用独立 DB 避免与其他服务冲突）</li>
 * </ul>
 */

import Redis from 'ioredis'

/** Redis 客户端实例 */
const redisClient = new Redis({
  host: process.env.REDIS_HOST || 'localhost',
  port: Number(process.env.REDIS_PORT) || 6379,
  password: process.env.REDIS_PASSWORD || undefined,
  db: 2,
  retryStrategy: (times: number) => Math.min(times * 50, 2000),
  maxRetriesPerRequest: 3,
  lazyConnect: true
})

/**
 * 从缓存读取值并反序列化.
 *
 * @param key 缓存键
 * @returns 反序列化后的值，未命中时返回 null
 */
export async function get<T = unknown>(key: string): Promise<T | null> {
  try {
    const cached = await redisClient.get(key)
    if (cached === null) {
      return null
    }
    return JSON.parse(cached) as T
  } catch {
    return null
  }
}

/**
 * 写入缓存并序列化，设置过期时间.
 *
 * @param key 缓存键
 * @param value 值（将被 JSON 序列化）
 * @param ttl 过期时间（秒）
 */
export async function set<T = unknown>(key: string, value: T, ttl: number): Promise<void> {
  try {
    const serialized = JSON.stringify(value)
    await redisClient.setex(key, ttl, serialized)
  } catch {
    // 缓存写入失败时静默处理，不影响业务流程
  }
}

/**
 * 删除指定缓存键.
 *
 * @param key 缓存键
 */
export async function del(key: string): Promise<void> {
  try {
    await redisClient.del(key)
  } catch {
    // 静默处理
  }
}

/**
 * 批量删除匹配模式的所有缓存键.
 *
 * <p>使用 SCAN 命令迭代匹配，避免 KEYS 命令阻塞 Redis.
 *
 * @param pattern 匹配模式（如 "graphql:product:*"）
 */
export async function clear(pattern: string): Promise<void> {
  try {
    let cursor = '0'
    do {
      const [nextCursor, keys] = await redisClient.scan(
        cursor,
        'MATCH',
        pattern,
        'COUNT',
        100
      )
      cursor = nextCursor
      if (keys.length > 0) {
        await redisClient.del(...keys)
      }
    } while (cursor !== '0')
  } catch {
    // 静默处理
  }
}

/**
 * 读取缓存，未命中则执行 fetcher 并回填.
 *
 * <p>核心缓存模式：先读缓存，未命中时调用 fetcher 获取数据并写入缓存。
 * Redis 不可用时自动降级为直接调用 fetcher.
 *
 * @param key 缓存键
 * @param ttl 过期时间（秒）
 * @param fetcher 数据获取函数
 * @returns 缓存或实时数据
 */
export async function getOrSet<T>(
  key: string,
  ttl: number,
  fetcher: () => Promise<T>
): Promise<T> {
  try {
    const cached = await get<T>(key)
    if (cached !== null) {
      return cached
    }

    const result = await fetcher()
    await set(key, result, ttl)
    return result
  } catch {
    // Redis 不可用时降级为直接查询
    return fetcher()
  }
}

/**
 * 判断 Redis 连接是否可用.
 *
 * @returns 连接状态
 */
export function isConnected(): boolean {
  return redisClient.status === 'ready'
}

/**
 * 获取 Redis 客户端实例（供高级用法使用）.
 */
export function getRedisClient(): Redis {
  return redisClient
}

export { redisClient }
export default redisClient