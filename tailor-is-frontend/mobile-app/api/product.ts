import { get } from './request'
import type { ProductInfo, PaginationParams, PageResult } from './types'

interface CategoryInfo {
  id: number
  name: string
  icon: string
  parentId: number
  children?: CategoryInfo[]
}

export function getProducts(params?: PaginationParams) {
  return get<PageResult<ProductInfo>>('product/list', params)
}

export function getProductDetail(id: number) {
  return get<ProductInfo>(`product/detail/${id}`)
}

export function getCategories() {
  return get<CategoryInfo[]>('product/categories')
}

export function getCategoryProducts(categoryId: number, params?: PaginationParams) {
  return get<PageResult<ProductInfo>>(`product/category/${categoryId}`, params)
}

export function searchProducts(params: PaginationParams & { keyword: string }) {
  return get<PageResult<ProductInfo>>('product/search', params)
}

export function getSeckillProducts() {
  return get<ProductInfo[]>('product/seckill')
}

export function getHotProducts() {
  return get<ProductInfo[]>('product/hot')
}
