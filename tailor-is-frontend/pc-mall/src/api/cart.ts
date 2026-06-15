import request from './request'
import type { CartItem } from '@/types'

interface AddCartData {
  productId: number
  skuId: number
  quantity: number
}

interface UpdateCartData {
  quantity?: number
  checked?: boolean
}

export function getCart() {
  return request<any, CartItem[]>({
    url: '/cart',
    method: 'get'
  })
}

export function addToCart(data: AddCartData) {
  return request<any, number>({
    url: '/cart',
    method: 'post',
    data
  })
}

export function updateCart(id: number, data: UpdateCartData) {
  return request<any, boolean>({
    url: `/cart/${id}`,
    method: 'put',
    data
  })
}

export function deleteCart(id: number) {
  return request<any, boolean>({
    url: `/cart/${id}`,
    method: 'delete'
  })
}

export function checkoutCart(ids: number[]) {
  return request<any, { items: CartItem[] }>({
    url: '/cart/checkout',
    method: 'post',
    data: { ids }
  })
}

export function clearCart() {
  return request<any, boolean>({
    url: '/cart/clear',
    method: 'delete'
  })
}
