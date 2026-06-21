import request from './request'
import type { Shop, Employee, PageResponse } from '@/types'

interface ShopUpdateData {
  name?: string
  logo?: string
  description?: string
  businessHours?: string
  announcement?: string
}

interface AddEmployeeParams {
  userId: number
  role: 'admin' | 'operator' | 'viewer'
}

export const getShopInfo = (shopId: number) => {
  return request<Record<string, unknown>, Shop>({
    url: `/shops/${shopId}`,
    method: 'GET',
  })
}

export interface ShopItem {
  id: number
  name: string
}

export const getMerchantShops = () => {
  return request<Record<string, unknown>, ShopItem[]>({
    url: '/shops/my',
    method: 'GET',
  })
}

export const updateShopInfo = (shopId: number, data: ShopUpdateData) => {
  return request<Record<string, unknown>, Shop>({
    url: `/shops/${shopId}`,
    method: 'PUT',
    data,
  })
}

export const listEmployees = (shopId: number, current?: number, size?: number) => {
  return request<Record<string, unknown>, PageResponse<Employee>>({
    url: `/shops/${shopId}/employees`,
    method: 'GET',
    params: { current, size },
  })
}

export const addEmployee = (shopId: number, data: AddEmployeeParams) => {
  return request<Record<string, unknown>, Employee>({
    url: `/shops/${shopId}/employees`,
    method: 'POST',
    data,
  })
}

export const removeEmployee = (shopId: number, employeeId: number) => {
  return request<Record<string, unknown>, void>({
    url: `/shops/${shopId}/employees/${employeeId}`,
    method: 'DELETE',
  })
}

export const updateEmployeeRole = (shopId: number, employeeId: number, role: string) => {
  return request<Record<string, unknown>, Employee>({
    url: `/shops/${shopId}/employees/${employeeId}/role`,
    method: 'PUT',
    data: { role },
  })
}
