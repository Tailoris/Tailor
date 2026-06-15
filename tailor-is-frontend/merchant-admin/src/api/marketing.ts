import request from './request'
import type { Coupon, PageResponse } from '@/types'

interface CouponListParams {
  status?: string
  current?: number
  size?: number
}

interface CreateCouponParams {
  name: string
  type: 'discount' | 'fixed' | 'percentage'
  discountValue: number
  minAmount: number
  validFrom: string
  validTo: string
  totalQuantity: number
}

export interface SeckillActivity {
  id: number
  name: string
  startTime: string
  endTime: string
  status: 'upcoming' | 'ongoing' | 'ended'
  products: { productId: number; seckillPrice: number; stock: number }[]
}

interface SeckillListParams {
  current?: number
  size?: number
}

interface CreateSeckillParams {
  name: string
  startTime: string
  endTime: string
}

export const getCoupons = (params: CouponListParams) => {
  return request<any, PageResponse<Coupon>>({
    url: '/marketing/coupons',
    method: 'GET',
    params,
  })
}

export const createCoupon = (data: CreateCouponParams) => {
  return request<any, Coupon>({
    url: '/marketing/coupons',
    method: 'POST',
    data,
  })
}

export const updateCoupon = (id: number, data: Partial<CreateCouponParams>) => {
  return request<any, Coupon>({
    url: `/marketing/coupons/${id}`,
    method: 'PUT',
    data,
  })
}

export const deleteCoupon = (id: number) => {
  return request<any, void>({
    url: `/marketing/coupons/${id}`,
    method: 'DELETE',
  })
}

export const getSeckillActivities = (params?: SeckillListParams) => {
  return request<any, PageResponse<SeckillActivity>>({
    url: '/marketing/seckill',
    method: 'GET',
    params,
  })
}

export const createSeckill = (data: CreateSeckillParams) => {
  return request<any, SeckillActivity>({
    url: '/marketing/seckill',
    method: 'POST',
    data,
  })
}

export const updateSeckill = (id: number, data: Partial<CreateSeckillParams>) => {
  return request<any, SeckillActivity>({
    url: `/marketing/seckill/${id}`,
    method: 'PUT',
    data,
  })
}

export const deleteSeckill = (id: number) => {
  return request<any, void>({
    url: `/marketing/seckill/${id}`,
    method: 'DELETE',
  })
}

export const joinSeckill = (activityId: number, productId: number, seckillPrice: number, stock: number) => {
  return request<any, void>({
    url: `/marketing/seckill/${activityId}/join`,
    method: 'POST',
    data: { productId, seckillPrice, stock },
  })
}
