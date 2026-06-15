import request from './request'
import type { Product, ProductSku, PageResponse } from '@/types'

interface ProductListParams {
  keyword?: string
  status?: string
  category?: string
  current?: number
  size?: number
}

interface ProductFormData {
  name: string
  category: string
  type: 'physical' | 'virtual'
  description: string
  images: string[]
  skus: Omit<ProductSku, 'id' | 'productId'>[]
}

export const listProducts = (params: ProductListParams) => {
  return request<any, PageResponse<Product>>({
    url: '/products',
    method: 'GET',
    params,
  })
}

export const getProductDetail = (id: number) => {
  return request<any, Product & { skus: ProductSku[] }>({
    url: `/products/${id}`,
    method: 'GET',
  })
}

export const createProduct = (data: ProductFormData) => {
  return request<any, Product>({
    url: '/products',
    method: 'POST',
    data,
  })
}

export const updateProduct = (id: number, data: ProductFormData) => {
  return request<any, Product>({
    url: `/products/${id}`,
    method: 'PUT',
    data,
  })
}

export const deleteProduct = (id: number) => {
  return request<any, void>({
    url: `/products/${id}`,
    method: 'DELETE',
  })
}

export const updateStatus = (id: number, status: string) => {
  return request<any, Product>({
    url: `/products/${id}/status`,
    method: 'PUT',
    data: { status },
  })
}

export const uploadImage = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request<any, { url: string }>({
    url: '/upload/image',
    method: 'POST',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}
