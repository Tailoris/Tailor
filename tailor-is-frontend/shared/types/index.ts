/**
 * 共享公共类型定义
 * 由 pc-mall、merchant-admin、mobile-app 等前端项目共同引用，
 * 消除 ApiResponse / PageResponse / User 等类型的多处重复定义。
 */

// ==========================================================================
// 枚举定义
// ==========================================================================

/**
 * 通用状态枚举.
 * 替代散落在各处的 number 类型 status 字段，增强类型安全.
 */
export enum StatusEnum {
  /** 禁用 / 下架 / 已删除 */
  DISABLED = 0,
  /** 启用 / 上架 / 正常 */
  ENABLED = 1,
  /** 审核中 */
  AUDITING = 2,
  /** 已拒绝 */
  REJECTED = 3,
}

/**
 * 审核状态枚举.
 */
export enum AuditStatusEnum {
  /** 待审核 */
  PENDING = 0,
  /** 审核通过 */
  APPROVED = 1,
  /** 审核拒绝 */
  REJECTED = 2,
}

/**
 * 版权标记枚举.
 */
export enum CopyrightFlagEnum {
  /** 未检测 */
  NONE = 0,
  /** 已存证 */
  REGISTERED = 1,
  /** 疑似侵权 */
  SUSPECTED = 2,
  /** 已确认侵权 */
  CONFIRMED = 3,
}

// ==========================================================================
// 接口类型
// ==========================================================================

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
  /** 用户状态: 0=禁用, 1=启用 */
  status?: StatusEnum
  createdAt?: string
}
