/**
 * 共享公共类型定义
 * 由 pc-mall、merchant-admin、mobile-app 等前端项目共同引用，
 * 消除 ApiResponse / PageResponse / User 等类型的多处重复定义。
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
 * 字段与 pc-mall / merchant-admin 的后端分页结构保持一致。
 */
export interface PageResponse<T> {
  records: T[]
  total: number
  pages: number
  current: number
  size: number
}

/**
 * 统一用户类型。
 * 字段命名统一使用 nickname，nickName 保留用于兼容旧版 API 响应（v4.1 移除）。
 */
export interface User {
  id: number
  username: string
  phone: string
  /** @deprecated 使用 nickname 替代，v4.1 移除 */
  nickName?: string
  nickname?: string
  email?: string
  avatar?: string
  realName?: string
  gender?: number
  birthday?: string
  status?: number
  createdAt?: string
}
