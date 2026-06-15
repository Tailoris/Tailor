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

export function getCart() {
  return get<CartItem[]>('cart/list')
}

export function addToCart(data: CartAddRequest) {
  return post<void>('cart/add', data)
}

export function updateCart(data: CartUpdateRequest) {
  return post<void>('cart/update', data)
}

export function deleteCart(cartId: number) {
  return del<void>(`cart/delete/${cartId}`)
}

export function updateCartQuantity(data: CartUpdateRequest) {
  return post<void>('cart/quantity', data)
}

export function clearCart() {
  return del<void>('cart/clear')
}
