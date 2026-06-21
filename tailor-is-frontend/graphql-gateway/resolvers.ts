import { GraphQLResolveInfo } from 'graphql'
import axios from 'axios'
import DataLoader from 'dataloader'
import { getOrSetCached, invalidateEntityCache, invalidateEntityById } from './src/cache/dataLoaderFactory'
import { shouldBypassCache } from './src/cache/cacheConfig'

// Backend API base URL
const API_BASE = process.env.API_BASE_URL || process.env.BACKEND_API_URL || 'http://localhost:8080/api'

const api = axios.create({
  baseURL: API_BASE,
  timeout: 15000
})

// Helper: extract auth token from context
function getAuth(context: unknown): string | null {
  const ctx = context as { headers?: Record<string, string> }
  return ctx?.headers?.authorization || null
}

// Helper: extract headers from context
function getHeaders(context: unknown): Record<string, string> {
  const ctx = context as { headers?: Record<string, string> }
  return ctx?.headers || {}
}

// Helper: wrap API call with optional auth
async function request<T>(url: string, method: 'get' | 'post' = 'get', data?: unknown, token?: string | null): Promise<T> {
  const headers: Record<string, string> = {}
  if (token) headers.Authorization = token

  const response = await api({ url, method, data, headers })
  const { code, data: result, message } = response.data
  if (code !== 200 && code !== 0) {
    throw new Error(message || 'API request failed')
  }
  return result as T
}

// Transform backend product response
function transformProduct(raw: Record<string, unknown>) {
  const skus = raw.skus as Array<Record<string, unknown>> | undefined
  return {
    ...raw,
    skus: skus?.map(sku => ({
      ...sku,
      attributes: Object.entries(sku.attributes as Record<string, string>).map(
        ([key, value]) => ({ key, value })
      )
    }))
  }
}

// Helper: recursively find a category by id in a category tree
function findCategoryInTree(
  cats: Array<Record<string, unknown>>,
  categoryId: number
): Record<string, unknown> | null {
  for (const cat of cats) {
    if (cat.id === categoryId) return cat
    if (cat.children) {
      const found = findCategoryInTree(cat.children as Array<Record<string, unknown>>, categoryId)
      if (found) return found
    }
  }
  return null
}

// GraphQL request context type
export interface GraphqlContext {
  headers: Record<string, string>
  categoryLoader: DataLoader<number, Record<string, unknown> | null>
}

/**
 * 构建每请求的 GraphQL 上下文.
 *
 * <p>每个请求创建独立的 DataLoader 实例，避免分类缓存跨请求泄露。
 * DataLoader 在同一请求内对相同 categoryId 的多次调用合并为一次后端请求，
 * 解决 Product.category 解析器在列表查询中的 N+1 问题。
 */
export function createContext(headers: Record<string, string>): GraphqlContext {
  const token = headers?.authorization || null
  const categoryLoader = new DataLoader<number, Record<string, unknown> | null>(
    async (categoryIds: readonly number[]) => {
      try {
        const categories = await request<Array<Record<string, unknown>>>(
          '/products/categories',
          'get',
          undefined,
          token
        )
        return categoryIds.map(id => findCategoryInTree(categories, id))
      } catch {
        return categoryIds.map(() => null)
      }
    }
  )
  return { headers, categoryLoader }
}

const resolvers = {
  Query: {
    product: async (
      _parent: unknown,
      { id }: { id: string },
      context: unknown
    ) => {
      const token = getAuth(context)
      const headers = getHeaders(context)

      // 如果请求头指示绕过缓存，直接查询
      if (shouldBypassCache(headers)) {
        const raw = await request<Record<string, unknown>>(`/products/${id}`, 'get', undefined, token)
        return transformProduct(raw)
      }

      return getOrSetCached('product', { id }, async () => {
        const raw = await request<Record<string, unknown>>(`/products/${id}`, 'get', undefined, token)
        return transformProduct(raw)
      })
    },

    products: async (
      _parent: unknown,
      args: {
        categoryId?: number
        keyword?: string
        sort?: string
        minPrice?: number
        maxPrice?: number
        current?: number
        size?: number
      },
      context: unknown
    ) => {
      const token = getAuth(context)
      const headers = getHeaders(context)

      if (shouldBypassCache(headers)) {
        return fetchProducts(args, token)
      }

      return getOrSetCached('products', args, async () => {
        return fetchProducts(args, token)
      })
    },

    order: async (
      _parent: unknown,
      { id }: { id: string },
      context: unknown
    ) => {
      const token = getAuth(context)
      const raw = await request<Record<string, unknown>>(`/orders/${id}`, 'get', undefined, token)
      const items = raw.items as Array<Record<string, unknown>> | undefined
      return {
        ...raw,
        items: items?.map(item => ({
          ...item,
          skuAttributes: item.skuAttributes
            ? Object.entries(item.skuAttributes as Record<string, string>).map(
                ([key, value]) => ({ key, value })
              )
            : []
        }))
      }
    },

    orders: async (
      _parent: unknown,
      args: { status?: number; current?: number; size?: number },
      context: unknown
    ) => {
      const token = getAuth(context)
      const params = new URLSearchParams()
      if (args.status !== undefined) params.set('status', String(args.status))
      params.set('current', String(args.current ?? 1))
      params.set('size', String(args.size ?? 20))

      return request<Record<string, unknown>>(`/orders?${params.toString()}`, 'get', undefined, token)
    },

    hotProducts: async (
      _parent: unknown,
      { limit = 10 }: { limit: number },
      context: unknown
    ) => {
      const token = getAuth(context)
      const headers = getHeaders(context)

      if (shouldBypassCache(headers)) {
        const raw = await request<Array<Record<string, unknown>>>('/products/hot', 'get', undefined, token)
        return raw.map(transformProduct)
      }

      return getOrSetCached('hotProducts', { limit }, async () => {
        const raw = await request<Array<Record<string, unknown>>>('/products/hot', 'get', undefined, token)
        return raw.map(transformProduct)
      })
    },

    newProducts: async (
      _parent: unknown,
      { limit = 10 }: { limit: number },
      context: unknown
    ) => {
      const token = getAuth(context)
      const headers = getHeaders(context)

      if (shouldBypassCache(headers)) {
        const raw = await request<Array<Record<string, unknown>>>('/products/new', 'get', undefined, token)
        return raw.map(transformProduct)
      }

      return getOrSetCached('newProducts', { limit }, async () => {
        const raw = await request<Array<Record<string, unknown>>>('/products/new', 'get', undefined, token)
        return raw.map(transformProduct)
      })
    },

    seckillProducts: async (
      _parent: unknown,
      { limit = 10 }: { limit: number },
      context: unknown
    ) => {
      const token = getAuth(context)
      const headers = getHeaders(context)

      if (shouldBypassCache(headers)) {
        const raw = await request<Array<Record<string, unknown>>>('/products/seckill', 'get', undefined, token)
        return raw.map(transformProduct)
      }

      return getOrSetCached('seckillProducts', { limit }, async () => {
        const raw = await request<Array<Record<string, unknown>>>('/products/seckill', 'get', undefined, token)
        return raw.map(transformProduct)
      })
    },

    categories: async (
      _parent: unknown,
      _args: unknown,
      context: unknown
    ) => {
      const token = getAuth(context)
      const headers = getHeaders(context)

      if (shouldBypassCache(headers)) {
        return request<Array<Record<string, unknown>>>('/products/categories', 'get', undefined, token)
      }

      return getOrSetCached('categories', '_', async () => {
        return request<Array<Record<string, unknown>>>('/products/categories', 'get', undefined, token)
      })
    }
  },

  Mutation: {
    addToCart: async (
      _parent: unknown,
      { input }: { input: { productId: number; skuId: number; quantity: number } },
      context: unknown
    ) => {
      const token = getAuth(context)
      return request<Record<string, unknown>>('/cart', 'post', input, token)
    },

    updateCartItem: async (
      _parent: unknown,
      { input }: { input: { id: string; quantity: number } },
      context: unknown
    ) => {
      const token = getAuth(context)
      return request<Record<string, unknown>>(`/cart/${input.id}`, 'post', { quantity: input.quantity }, token)
    },

    deleteCartItem: async (
      _parent: unknown,
      { id }: { id: string },
      context: unknown
    ) => {
      const token = getAuth(context)
      await request(`/cart/${id}`, 'post', undefined, token)
      return true
    },

    clearCart: async (
      _parent: unknown,
      _args: unknown,
      context: unknown
    ) => {
      const token = getAuth(context)
      await request('/cart/clear', 'post', undefined, token)
      return true
    },

    submitOrder: async (
      _parent: unknown,
      { input }: { input: { addressId: number; remark?: string; items: Array<{ productId: number; skuId: number; quantity: number }> } },
      context: unknown
    ) => {
      const token = getAuth(context)
      const result = await request<Record<string, unknown>>('/orders', 'post', input, token)
      // 提交订单后使相关商品缓存失效
      for (const item of input.items) {
        await invalidateEntityById('product', item.productId)
      }
      // 使热门商品和新品列表缓存失效
      await Promise.all([
        invalidateEntityCache('hotProducts'),
        invalidateEntityCache('newProducts'),
        invalidateEntityCache('orders'),
      ])
      return result
    }
  },

  Product: {
    category: async (parent: { categoryId: number }, _args: unknown, context: unknown) => {
      const ctx = context as GraphqlContext
      return ctx.categoryLoader.load(parent.categoryId)
    }
  }
}

/**
 * 获取商品列表（提取为独立函数，便于缓存和直接调用）.
 */
async function fetchProducts(
  args: {
    categoryId?: number
    keyword?: string
    sort?: string
    minPrice?: number
    maxPrice?: number
    current?: number
    size?: number
  },
  token: string | null
): Promise<Record<string, unknown>> {
  const params = new URLSearchParams()
  if (args.categoryId) params.set('categoryId', String(args.categoryId))
  if (args.keyword) params.set('keyword', args.keyword)
  if (args.sort) params.set('sort', args.sort)
  if (args.minPrice !== undefined) params.set('minPrice', String(args.minPrice))
  if (args.maxPrice !== undefined) params.set('maxPrice', String(args.maxPrice))
  params.set('current', String(args.current ?? 1))
  params.set('size', String(args.size ?? 20))

  const raw = await request<Record<string, unknown>>(
    `/products?${params.toString()}`,
    'get',
    undefined,
    token
  )
  return {
    ...raw,
    records: (raw.records as Array<Record<string, unknown>>).map(transformProduct)
  }
}

export default resolvers