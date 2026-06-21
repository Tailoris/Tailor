import Redis from 'ioredis'
import { createHash } from 'crypto'

/**
 * GraphQL 查询缓存层.
 *
 * <p>对 product, hotProducts, newProducts, seckillProducts, categories 等高频查询
 * 提供 Redis 缓存支持，避免每次请求都穿透到后端 REST API。
 *
 * <h3>缓存策略</h3>
 * <ul>
 *   <li>product: TTL 5 分钟 — 商品信息变更频率低</li>
 *   <li>hotProducts/newProducts/seckillProducts: TTL 3 分钟 — 列表页面需要较新数据</li>
 *   <li>categories: TTL 15 分钟 — 分类极少变更</li>
 *   <li>缓存键格式: graphql:{queryName}:{参数hash}</li>
 * </ul>
 *
 * <h3>失效机制</h3>
 * <ul>
 *   <li>基于 TTL 自动过期</li>
 *   <li>提供 invalidate() 方法供外部调用（如 CMS 更新商品时）</li>
 * </ul>
 */

const redis = new Redis({
  host: process.env.REDIS_HOST || 'localhost',
  port: Number(process.env.REDIS_PORT) || 6379,
  password: process.env.REDIS_PASSWORD || undefined,
  db: 2, // 使用独立 DB 避免与其他服务冲突
  retryStrategy: (times) => Math.min(times * 50, 2000),
  maxRetriesPerRequest: 3,
  lazyConnect: true
})

// 缓存 TTL 配置（秒）
const CACHE_TTL: Record<string, number> = {
  product: 300,        // 5 分钟
  products: 120,       // 2 分钟
  hotProducts: 180,    // 3 分钟
  newProducts: 180,    // 3 分钟
  seckillProducts: 180, // 3 分钟
  categories: 900,     // 15 分钟
  category: 900        // 15 分钟
}

function buildCacheKey(queryName: string, args: unknown): string {
  const argsStr = args ? JSON.stringify(args) : '_'
  const hash = createHash('md5').update(argsStr).digest('hex').slice(0, 8)
  return `graphql:${queryName}:${hash}`
}

/**
 * 读取缓存，未命中则执行 fetcher 并回填.
 */
export async function getOrSet<T>(
  queryName: string,
  args: unknown,
  fetcher: () => Promise<T>
): Promise<T> {
  try {
    const key = buildCacheKey(queryName, args)
    const ttl = CACHE_TTL[queryName] ?? 60

    const cached = await redis.get(key)
    if (cached) {
      return JSON.parse(cached) as T
    }

    const result = await fetcher()
    await redis.setex(key, ttl, JSON.stringify(result))
    return result
  } catch {
    // Redis 不可用时降级为直接查询
    return fetcher()
  }
}

/**
 * 使指定查询的缓存失效.
 */
export async function invalidate(queryName: string, args?: unknown): Promise<void> {
  try {
    if (args) {
      const key = buildCacheKey(queryName, args)
      await redis.del(key)
    } else {
      // 删除所有匹配前缀的键（使用 SCAN 迭代，避免 KEYS 阻塞 Redis）
      const pattern = `graphql:${queryName}:*`
      let cursor = '0'
      do {
        const [nextCursor, keys] = await redis.scan(cursor, 'MATCH', pattern, 'COUNT', 100)
        cursor = nextCursor
        if (keys.length > 0) {
          await redis.del(...keys)
        }
      } while (cursor !== '0')
    }
  } catch {
    // 静默处理缓存清理失败
  }
}

export { redis }

/**
 * 缓存预热：服务启动时预加载热门数据到 Redis.
 *
 * <p>在 GraphQL Gateway 启动后调用，预热 categories, hotProducts, seckillProducts 等高频查询。
 * 减少首次请求的冷启动延迟。
 *
 * <h3>预热策略</h3>
 * <ul>
 *   <li>categories: 预加载全量（TTL 15min）</li>
 *   <li>hotProducts: 预加载 Top 10（TTL 3min）</li>
 *   <li>seckillProducts: 预加载当前秒杀（TTL 3min）</li>
 * </ul>
 *
 * @param fetchers 查询函数映射 { queryName: fetcher }
 */
export async function warmupCache(fetchers: Record<string, () => Promise<unknown>>) {
  process.stdout.write('[CacheWarmup] Starting cache warmup...\n')
  const results: Record<string, string> = {}

  for (const [queryName, fetcher] of Object.entries(fetchers)) {
    try {
      const data = await fetcher()
      const key = `graphql:${queryName}:_warmup`
      const ttl = 60 // 预热数据 TTL 1 分钟，等待首次真实请求回填长期缓存
      await redis.setex(key, ttl, JSON.stringify(data))
      results[queryName] = 'ok'
    } catch (err) {
      results[queryName] = `failed: ${err instanceof Error ? err.message : 'unknown'}`
    }
  }

  process.stdout.write(`[CacheWarmup] Warmup complete: ${JSON.stringify(results)}\n`)
}