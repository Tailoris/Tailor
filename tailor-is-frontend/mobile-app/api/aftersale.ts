import { post, get } from './request'
import type { PaginationParams, PageResult } from './types'

export interface AfterSaleInfo {
  id?: number
  orderNo: string
  type: string
  productName: string
  quantity: number
  reason: string
  description: string
  images?: string[]
  phone?: string
  status?: number
  createdAt?: string
}

export function createAfterSale(data: AfterSaleInfo) {
  return post<AfterSaleInfo>('aftersale/create', data)
}

export function getAfterSales(params?: PaginationParams & { status?: number }) {
  return get<PageResult<AfterSaleInfo>>('aftersale/list', params)
}

export function getAfterSaleDetail(aftersaleId: number) {
  return get<AfterSaleInfo>(`aftersale/detail/${aftersaleId}`)
}

export function cancelAfterSale(aftersaleId: number) {
  return post<void>(`aftersale/cancel/${aftersaleId}`)
}
