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
  return request<Record<string, unknown>, { orderNo: string }>({
    url: '/order',
    method: 'post',
    data
  })
}

export function getOrders(params: OrderListParams) {
  return request<Record<string, unknown>, PageResponse<Order>>({
    url: '/order/list',
    method: 'get',
    params
  })
}

export function getOrderDetail(orderNo: string) {
  return request<Record<string, unknown>, Order>({
    url: `/order/${orderNo}`,
    method: 'get'
  })
}

export function payOrder(orderNo: string) {
  return request<Record<string, unknown>, boolean>({
    url: `/order/${orderNo}/pay`,
    method: 'put',
    params: { payType: 1 }
  })
}

export function confirmOrder(orderNo: string) {
  return request<Record<string, unknown>, boolean>({
    url: `/order/${orderNo}/confirm`,
    method: 'put'
  })
}

export function cancelOrder(orderNo: string) {
  return request<Record<string, unknown>, boolean>({
    url: `/order/${orderNo}/cancel`,
    method: 'put',
    params: { reason: '用户取消' }
  })
}
