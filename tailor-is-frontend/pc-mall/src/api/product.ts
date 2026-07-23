import request from './request';
import type { Product, ProductCategory, PageResponse } from '@/types';

interface ProductListParams {
  categoryId?: number;
  keyword?: string;
  sort?: string;
  minPrice?: number;
  maxPrice?: number;
  current?: number;
  size?: number;
}

export function getProducts(params: ProductListParams) {
  return request<Record<string, unknown>, PageResponse<Product>>({
    url: '/api/v1/products',
    method: 'get',
    params: {
      keyword: params.keyword,
      categoryId: params.categoryId,
      sort: params.sort,
      pageNum: params.current || 1,
      pageSize: params.size || 10,
    },
  });
}

export function getProductDetail(id: number) {
  return request<Record<string, unknown>, Product>({
    url: `/api/v1/product/${id}`,
    method: 'get',
  });
}

export function getCategories(options?: { showError?: boolean }) {
  return request<Record<string, unknown>, ProductCategory[]>({
    url: '/api/v1/products/categories',
    method: 'get',
    showError: options?.showError,
  });
}

export function searchProducts(keyword: string, params: ProductListParams) {
  return request<Record<string, unknown>, PageResponse<Product>>({
    url: '/api/v1/products/search',
    method: 'get',
    params: { keyword, ...params },
  });
}

export function getHotProducts(options?: { showError?: boolean }) {
  return request<Record<string, unknown>, Product[]>({
    url: '/api/v1/products/hot',
    method: 'get',
    showError: options?.showError,
  });
}

export function getNewProducts(options?: { showError?: boolean }) {
  return request<Record<string, unknown>, Product[]>({
    url: '/api/v1/products/new',
    method: 'get',
    showError: options?.showError,
  });
}

export function getSeckillProducts(options?: { showError?: boolean }) {
  return request<Record<string, unknown>, Product[]>({
    url: '/api/v1/products/seckill',
    method: 'get',
    showError: options?.showError,
  });
}

export function getProductSkus(productId: number) {
  return request<Record<string, unknown>, { id: number; attributes: string; price: number; stock: number }[]>({
    url: `/api/v1/product/${productId}/skus`,
    method: 'get',
  });
}
