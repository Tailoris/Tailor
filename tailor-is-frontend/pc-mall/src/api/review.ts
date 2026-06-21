import request from './request'
import type { ProductReview, PageResponse } from '@/types'

export function getProductReviews(productId: number, params?: { pageNum?: number; pageSize?: number }) {
  return request<Record<string, unknown>, PageResponse<ProductReview>>({
    url: `/product/review/${productId}`,
    method: 'get',
    params: {
      pageNum: params?.pageNum ?? 1,
      pageSize: params?.pageSize ?? 10
    }
  })
}

export function createReview(data: {
  productId: number
  skuId?: number
  orderId?: number
  orderItemId?: number
  rating: number
  content: string
  images?: string[]
  isAnonymous?: number
}) {
  return request<Record<string, unknown>, number>({
    url: '/product/review',
    method: 'post',
    data
  })
}
