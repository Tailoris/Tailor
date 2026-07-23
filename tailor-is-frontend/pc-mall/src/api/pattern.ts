import request from './request';
import type { PageResponse } from '@/types';

/**
 * 纸样/版型实体
 */
export interface Pattern {
  id: number;
  name: string;
  category: string;
  styleType?: string;
  description: string;
  imageUrl: string;
  price?: number;
  status: number;
  merchantId?: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * 创建版型请求
 */
export interface CreatePatternRequest {
  styleType: string;
  patternStyle: string;
  targetAudience: string;
  minSize?: number;
  maxSize?: number;
  techRequirements?: string[];
  description?: string;
  minBudget?: number;
  maxBudget?: number;
  deliveryDate?: string;
  isUrgent?: boolean;
  imageUrl?: string;
}

/**
 * 获取版型列表（公开）
 * @param pageNum 页码
 * @param pageSize 每页数量
 */
export function getPatternList(params?: { pageNum?: number; pageSize?: number }) {
  return request<PageResponse<Pattern>>({
    url: '/api/v1/pattern/list',
    method: 'get',
    params: {
      pageNum: params?.pageNum ?? 1,
      pageSize: params?.pageSize ?? 10,
    },
  });
}

/**
 * 获取版型详情（公开）
 * @param id 版型ID
 */
export function getPatternDetail(id: number) {
  return request<Pattern>({
    url: `/api/v1/pattern/${id}`,
    method: 'get',
  });
}

/**
 * 创建版型
 * @param data 版型数据
 */
export function createPattern(data: CreatePatternRequest) {
  return request<number>({
    url: '/api/v1/pattern',
    method: 'post',
    data,
  });
}

/**
 * 更新版型
 * @param id 版型ID
 * @param data 版型数据
 */
export function updatePattern(id: number, data: Partial<CreatePatternRequest>) {
  return request<void>({
    url: `/api/v1/pattern/${id}`,
    method: 'put',
    data,
  });
}

/**
 * 删除版型
 * @param id 版型ID
 */
export function deletePattern(id: number) {
  return request<void>({
    url: `/api/v1/pattern/${id}`,
    method: 'delete',
  });
}

/**
 * 获取当前用户的版型列表
 */
export function getMyPatterns() {
  return request<Pattern[]>({
    url: '/api/v1/pattern/my-patterns',
    method: 'get',
  });
}

/**
 * 获取商家版型列表
 * @param pageNum 页码
 * @param pageSize 每页数量
 */
export function getMerchantPatterns(params?: { pageNum?: number; pageSize?: number }) {
  return request<PageResponse<Pattern>>({
    url: '/api/v1/pattern/merchant/list',
    method: 'get',
    params: {
      pageNum: params?.pageNum ?? 1,
      pageSize: params?.pageSize ?? 10,
    },
  });
}
