/**
 * 共享 API 类型定义
 * 由 pc-mall、merchant-admin、platform-admin、mobile-app 等前端项目共同引用，
 * 提供统一的 API 请求/响应类型。
 */

/**
 * 统一后端响应包装类型。
 * data 字段统一为可选且可为 null，兼容无数据返回的接口。
 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data?: T | null
}

/**
 * 统一分页响应类型。
 */
export interface PaginatedResponse<T> {
  records: T[]
  total: number
  pages: number
  current: number
  size: number
}

/**
 * API 错误类型。
 */
export interface ApiError {
  code: number
  message: string
  timestamp?: number
  path?: string
}

/**
 * 通用请求分页参数。
 */
export interface PaginationParams {
  pageNum?: number
  pageSize?: number
  current?: number
  size?: number
}

/**
 * 通用排序参数。
 */
export interface SortParams {
  sortField?: string
  sortOrder?: 'asc' | 'desc'
}

/**
 * 通用查询请求参数（组合分页 + 排序）。
 */
export interface QueryParams extends PaginationParams, SortParams {
  keyword?: string
  startDate?: string
  endDate?: string
}