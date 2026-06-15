import request from './request'
import type { Order, PageResponse } from '@/types'

interface CreateOrderData {
  items: { productId: number; skuId: number; quantity: number }[]
  addressId: number
  remark?: string
  couponId?: number
}

interface OrderListParams {
  status?: number
  current?: number
  size?: number
}

export function createOrder(data: CreateOrderData) {
  return request<any, { orderNo: string }>({
    url: '/orders',
    method: 'post',
    data
  })
}

export function getOrders(params: OrderListParams) {
  return request<any, PageResponse<Order>>({
    url: '/orders',
    method: 'get',
    params
  })
}

export function getOrderDetail(orderNo: string) {
  return request<any, Order>({
    url: `/orders/${orderNo}`,
    method: 'get'
  })
}

export function payOrder(orderNo: string) {
  return request<any, boolean>({
    url: `/orders/${orderNo}/pay`,
    method: 'post'
  })
}

export function confirmOrder(orderNo: string) {
  return request<any, boolean>({
    url: `/orders/${orderNo}/confirm`,
    method: 'post'
  })
}

export function cancelOrder(orderNo: string) {
  return request<any, boolean>({
    url: `/orders/${orderNo}/cancel`,
    method: 'post'
  })
}
