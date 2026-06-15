/**
 * 移动端购物车本地缓存 - 修复 F-M11
 */

const CART_STORAGE_KEY = 'tailor_is_cart_cache'
const CACHE_EXPIRE_MS = 24 * 60 * 60 * 1000 // 24小时

interface CartCache {
  items: unknown[]
  timestamp: number
}

/**
 * 保存购物车数据到本地
 */
export function saveCartCache(items: unknown[]): void {
  try {
    const cache: CartCache = {
      items,
      timestamp: Date.now()
    }
    uni.setStorageSync(CART_STORAGE_KEY, JSON.stringify(cache))
  } catch (e) {
    console.warn('购物车本地缓存保存失败:', e)
  }
}

/**
 * 从本地加载购物车数据
 */
export function loadCartCache(): unknown[] {
  try {
    const cached = uni.getStorageSync(CART_STORAGE_KEY)
    if (!cached) return []
    const cache: CartCache = JSON.parse(cached)
    // 过期检查
    if (Date.now() - cache.timestamp > CACHE_EXPIRE_MS) {
      uni.removeStorageSync(CART_STORAGE_KEY)
      return []
    }
    return cache.items || []
  } catch {
    return []
  }
}

/**
 * 清除购物车本地缓存
 */
export function clearCartCache(): void {
  try {
    uni.removeStorageSync(CART_STORAGE_KEY)
  } catch (e) {
    console.warn('购物车本地缓存清除失败:', e)
  }
}
