import { vi, describe, it, expect, beforeEach } from 'vitest'

vi.mock('../request', () => ({
  default: vi.fn().mockResolvedValue({})
}))

import request from '../request'
import { getCart, addToCart, updateCart, deleteCart, checkoutCart, clearCart } from '../cart'

describe('cart API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getCart', () => {
    it('should get cart items successfully', async () => {
      const mockCartItems = [
        { id: 1, productId: 1, skuId: 1, quantity: 2, price: 99.9, checked: true, productName: 'Product 1' },
        { id: 2, productId: 2, skuId: 2, quantity: 1, price: 199.9, checked: false, productName: 'Product 2' }
      ]
      ;(request as vi.Mock).mockResolvedValue(mockCartItems)

      const result = await getCart()
      expect(result).toEqual(mockCartItems)
      expect(result.length).toBe(2)
    })

    it('should handle empty cart', async () => {
      ;(request as vi.Mock).mockResolvedValue([])

      const result = await getCart()
      expect(result).toEqual([])
    })

    it('should handle unauthorized error', async () => {
      ;(request as vi.Mock).mockRejectedValue(new Error('登录已过期'))

      await expect(getCart()).rejects.toThrow('登录已过期')
    })
  })

  describe('addToCart', () => {
    it('should add item to cart successfully', async () => {
      ;(request as vi.Mock).mockResolvedValue(1)

      const result = await addToCart({ productId: 1, skuId: 1, quantity: 2 })
      expect(result).toBe(1)
    })

    it('should handle add failure - insufficient stock', async () => {
      ;(request as vi.Mock).mockRejectedValue(new Error('库存不足'))

      await expect(addToCart({ productId: 1, skuId: 1, quantity: 1000 })).rejects.toThrow('库存不足')
    })

    it('should handle add failure - product not found', async () => {
      ;(request as vi.Mock).mockRejectedValue(new Error('商品不存在'))

      await expect(addToCart({ productId: 999, skuId: 999, quantity: 1 })).rejects.toThrow('商品不存在')
    })
  })

  describe('updateCart', () => {
    it('should update cart item quantity successfully', async () => {
      ;(request as vi.Mock).mockResolvedValue(true)

      const result = await updateCart(1, { quantity: 5 })
      expect(result).toBe(true)
    })

    it('should update cart item checked status successfully', async () => {
      ;(request as vi.Mock).mockResolvedValue(true)

      const result = await updateCart(1, { checked: true })
      expect(result).toBe(true)
    })

    it('should handle update failure - item not found', async () => {
      ;(request as vi.Mock).mockRejectedValue(new Error('购物车项不存在'))

      await expect(updateCart(999, { quantity: 2 })).rejects.toThrow('购物车项不存在')
    })

    it('should handle update failure - invalid quantity', async () => {
      ;(request as vi.Mock).mockRejectedValue(new Error('数量无效'))

      await expect(updateCart(1, { quantity: 0 })).rejects.toThrow('数量无效')
    })
  })

  describe('deleteCart', () => {
    it('should delete cart item successfully', async () => {
      ;(request as vi.Mock).mockResolvedValue(true)

      const result = await deleteCart(1)
      expect(result).toBe(true)
    })

    it('should handle delete failure - item not found', async () => {
      ;(request as vi.Mock).mockRejectedValue(new Error('购物车项不存在'))

      await expect(deleteCart(999)).rejects.toThrow('购物车项不存在')
    })
  })

  describe('checkoutCart', () => {
    it('should checkout cart items successfully', async () => {
      const mockCheckout = {
        items: [
          { id: 1, productId: 1, skuId: 1, quantity: 2, price: 99.9, checked: true }
        ]
      }
      ;(request as vi.Mock).mockResolvedValue(mockCheckout)

      const result = await checkoutCart([1])
      expect(result).toEqual(mockCheckout)
      expect(result.items.length).toBe(1)
    })

    it('should handle checkout failure - no items selected', async () => {
      ;(request as vi.Mock).mockRejectedValue(new Error('请选择商品'))

      await expect(checkoutCart([])).rejects.toThrow('请选择商品')
    })
  })

  describe('clearCart', () => {
    it('should clear cart successfully', async () => {
      ;(request as vi.Mock).mockResolvedValue(true)

      const result = await clearCart()
      expect(result).toBe(true)
    })
  })
})