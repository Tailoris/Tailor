import request from './request'
import type { Product, ProductCategory, PageResponse } from '@/types'

interface ProductListParams {
  categoryId?: number
  keyword?: string
  sort?: string
  minPrice?: number
  maxPrice?: number
  current?: number
  size?: number
}

export function getProducts(params: ProductListParams) {
  return request<any, PageResponse<Product>>({
    url: '/products',
    method: 'get',
    params
  })
}

export function getProductDetail(id: number) {
  return request<any, Product>({
    url: `/products/${id}`,
    method: 'get'
  })
}

export function getCategories() {
  return request<any, ProductCategory[]>({
    url: '/products/categories',
    method: 'get'
  })
}

export function searchProducts(keyword: string, params: ProductListParams) {
  return request<any, PageResponse<Product>>({
    url: '/products/search',
    method: 'get',
    params: { keyword, ...params }
  })
}

export function getHotProducts() {
  return request<any, Product[]>({
    url: '/products/hot',
    method: 'get'
  })
}

export function getNewProducts() {
  return request<any, Product[]>({
    url: '/products/new',
    method: 'get'
  })
}

export function getSeckillProducts() {
  return request<any, Product[]>({
    url: '/products/seckill',
    method: 'get'
  })
}
