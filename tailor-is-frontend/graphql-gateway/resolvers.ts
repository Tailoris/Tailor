import { GraphQLResolveInfo } from 'graphql'
import axios from 'axios'
import { getOrSet, invalidate } from './cache'

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

const resolvers = {
  Query: {
    product: async (
      _parent: unknown,
      { id }: { id: string },
      context: unknown
    ) => {
      const token = getAuth(context)
      return getOrSet('product', { id }, async () => {
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
      return getOrSet('products', args, async () => {
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
      return getOrSet('hotProducts', { limit }, async () => {
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
      return getOrSet('newProducts', { limit }, async () => {
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
      return getOrSet('seckillProducts', { limit }, async () => {
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
      return getOrSet('categories', '_', async () => {
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
        invalidate('product', { id: String(item.productId) })
      }
      invalidate('hotProducts')
      return result
    }
  },

  Product: {
    category: async (parent: { categoryId: number }, _args: unknown, context: unknown) => {
      const token = getAuth(context)
      try {
        const categories = await request<Array<Record<string, unknown>>>('/products/categories', 'get', undefined, token)
        const findCategory = (cats: Array<Record<string, unknown>>): Record<string, unknown> | null => {
          for (const cat of cats) {
            if (cat.id === parent.categoryId) return cat
            if (cat.children) {
              const found = findCategory(cat.children as Array<Record<string, unknown>>)
              if (found) return found
            }
          }
          return null
        }
        return findCategory(categories)
      } catch {
        return null
      }
    }
  }
}

export default resolvers