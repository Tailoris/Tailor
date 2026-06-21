import { createHash } from 'crypto'

export const CACHE_KEY_PREFIX = 'graphql'
export const CACHE_KEY_SEPARATOR = ':'
export const DEFAULT_TTL = 60

export const CACHE_TTL: Record<string, number> = {
  product: 300,
  products: 120,
  user: 1800,
  order: 60,
  orders: 60,
  community: 120,
  communityDetail: 120,
  hotProducts: 180,
  newProducts: 180,
  seckillProducts: 180,
  categories: 900,
  category: 900,
}

export const CACHE_BYPASS_HEADERS = [
  'x-bypass-cache',
  'x-no-cache',
  'cache-control'
]

export const CACHE_BYPASS_VALUES = ['no-cache', 'no-store', 'max-age=0']

export function shouldBypassCache(headers: { [key: string]: string }): boolean {
  for (const header of CACHE_BYPASS_HEADERS) {
    const value = headers[header]
    if (value) {
      for (const bypassValue of CACHE_BYPASS_VALUES) {
        if (value.toLowerCase().includes(bypassValue)) {
          return true
        }
      }
    }
  }
  return false
}

export function getTTL(queryName: string): number {
  return CACHE_TTL[queryName] !== undefined ? CACHE_TTL[queryName] : DEFAULT_TTL
}

export function buildCacheKey(queryName: string, args: unknown): string {
  const argsStr = args ? JSON.stringify(args) : '_'
  const hash = createHash('md5').update(argsStr).digest('hex').slice(0, 8)
  return `${CACHE_KEY_PREFIX}${CACHE_KEY_SEPARATOR}${queryName}${CACHE_KEY_SEPARATOR}${hash}`
}

export function buildInvalidatePattern(queryName: string): string {
  return `${CACHE_KEY_PREFIX}${CACHE_KEY_SEPARATOR}${queryName}${CACHE_KEY_SEPARATOR}*`
}