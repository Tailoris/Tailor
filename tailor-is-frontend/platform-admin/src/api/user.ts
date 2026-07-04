import request from './request'
import type { PageResult } from './role'

export interface UserInfo {
  id: number
  username: string
  realName: string
  phone: string
  email: string
  status: number
  roles: string[]
  createTime: string
}

export interface UserForm {
  id?: number
  username: string
  realName: string
  phone: string
  email: string
  password?: string
  roleIds: number[]
  status: number
}

export interface UserQueryParams {
  keyword?: string
  status?: string
  current?: number
  size?: number
}

export interface RoleInfo {
  id: number
  name: string
  code: string
}

export function listUsers(params: UserQueryParams) {
  return request<unknown, PageResult<UserInfo>>({
    url: '/admin/users',
    method: 'get',
    params
  })
}

export function createUser(data: UserForm) {
  return request<unknown, UserInfo>({
    url: '/admin/users',
    method: 'post',
    data
  })
}

export function updateUser(id: number, data: UserForm) {
  return request<unknown, UserInfo>({
    url: `/admin/users/${id}`,
    method: 'put',
    data
  })
}

export function toggleUserStatus(id: number, status: number) {
  return request<unknown, void>({
    url: `/admin/users/${id}/status`,
    method: 'put',
    data: { status }
  })
}

export function resetUserPassword(id: number) {
  return request<unknown, string>({
    url: `/admin/users/${id}/password/reset`,
    method: 'post'
  })
}

export function assignUserRoles(id: number, roleIds: number[]) {
  return request<unknown, void>({
    url: `/admin/users/${id}/roles`,
    method: 'put',
    data: { roleIds }
  })
}

export function listAvailableRoles() {
  return request<unknown, RoleInfo[]>({
    url: '/admin/roles/all',
    method: 'get'
  })
}
