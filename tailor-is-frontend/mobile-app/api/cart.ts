import { get, post, del } from './request'
import type { CartItem } from './types'

interface CartAddRequest {
  productId: number
  quantity: number
  skuId?: number
}

interface CartUpdateRequest {
  cartId: number
  quantity: number
}

// 本地缓存机制 - 修复 F-M11
interface CacheEntry<T> {
  data: T
  timestamp: number
}

const CACHE_TTL = 30 * 1000 // 30秒缓存过期时间
const cartCache: { entry: CacheEntry<CartItem[]> | null } = { entry: null }

function isCacheValid(entry: CacheEntry<unknown> | null): boolean {
  if (!entry) return false
  return Date.now() - entry.timestamp < CACHE_TTL
}

export async function getCart(forceRefresh = false) {
  if (!forceRefresh && isCacheValid(cartCache.entry)) {
    return cartCache.entry!.data
  }
  const res = await get<CartItem[]>('cart/list')
  if (res.code === 200) {
    cartCache.entry = { data: res.data, timestamp: Date.now() }
  }
  return res.data
}

export function invalidateCartCache() {
  cartCache.entry = null
}

export async function addToCart(data: CartAddRequest) {
  const res = await post<void>('cart/add', data)
  invalidateCartCache()
  return res
}

export async function updateCart(data: CartUpdateRequest) {
  const res = await post<void>('cart/update', data)
  invalidateCartCache()
  return res
}

export async function deleteCart(cartId: number) {
  const res = await del<void>(`cart/delete/${cartId}`)
  invalidateCartCache()
  return res
}

export async function updateCartQuantity(data: CartUpdateRequest) {
  const res = await post<void>('cart/quantity', data)
  invalidateCartCache()
  return res
}

export async function clearCart() {
  const res = await del<void>('cart/clear')
  invalidateCartCache()
  return res
}
