import request from './request';
import type { PageResponse } from '@/types';

/**
 * 纸样/版型实体
 */
export interface Pattern {
  id: number;
  name: string;
  styleType: string;
  description: string;
  imageUrl: string;
  price: number;
  status: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * 获取版型列表
 * @param pageNum 页码
 * @param pageSize 每页数量
 */
export function getPatternList(params?: { pageNum?: number; pageSize?: number }) {
  return request<Record<string, unknown>, PageResponse<Pattern>>({
    url: '/api/v1/pattern/list',
    method: 'get',
    params: {
      pageNum: params?.pageNum ?? 1,
      pageSize: params?.pageSize ?? 10,
    },
  });
}

/**
 * 获取版型详情
 * @param id 版型ID
 */
export function getPatternDetail(id: number) {
  return request<Record<string, unknown>, Pattern>({
    url: `/api/v1/pattern/${id}`,
    method: 'get',
  });
}
