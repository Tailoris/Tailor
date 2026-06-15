import { post, get, del } from './request'
import type { OrderInfo, PaginationParams, PageResult } from './types'

interface PayRequest {
  payType: number
}

export function createOrder(data: Record<string, unknown>) {
  return post<OrderInfo>('order/create', data)
}

export function getOrders(params?: PaginationParams & { status?: number }) {
  return get<PageResult<OrderInfo>>('order/list', params)
}

export function getOrderDetail(orderId: number) {
  return get<OrderInfo>(`order/detail/${orderId}`)
}

export function payOrder(orderId: number, data: PayRequest) {
  return post<void>(`order/pay/${orderId}`, data)
}

export function cancelOrder(orderId: number) {
  return post<void>(`order/cancel/${orderId}`)
}

export function confirmReceive(orderId: number) {
  return post<void>(`order/confirm/${orderId}`)
}

export function deleteOrder(orderId: number) {
  return del<void>(`order/delete/${orderId}`)
}
