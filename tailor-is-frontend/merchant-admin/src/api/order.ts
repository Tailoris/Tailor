import request from './request'
import type { Order, PageResponse } from '@/types'

interface OrderListParams {
  orderNo?: string
  status?: string
  startDate?: string
  endDate?: string
  current?: number
  size?: number
}

interface ShipParams {
  logisticsCompany: string
  trackingNumber: string
}

export const listOrders = (params: OrderListParams) => {
  return request<any, PageResponse<Order>>({
    url: '/orders',
    method: 'GET',
    params,
  })
}

export const getOrderDetail = (orderNo: string) => {
  return request<any, Order>({
    url: `/orders/${orderNo}`,
    method: 'GET',
  })
}

export const shipOrder = (orderNo: string, data: ShipParams) => {
  return request<any, Order>({
    url: `/orders/${orderNo}/ship`,
    method: 'POST',
    data,
  })
}

export const updatePrice = (orderNo: string, price: number) => {
  return request<any, Order>({
    url: `/orders/${orderNo}/price`,
    method: 'PUT',
    data: { price },
  })
}

export const getOrderByShop = (shopId: number, params: OrderListParams) => {
  return request<any, PageResponse<Order>>({
    url: `/shops/${shopId}/orders`,
    method: 'GET',
    params,
  })
}
