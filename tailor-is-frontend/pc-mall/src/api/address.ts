import request from './request'
import type { Address } from '@/types'

interface CreateAddressData {
  name: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault?: number
}

interface UpdateAddressData {
  name?: string
  phone?: string
  province?: string
  city?: string
  district?: string
  detail?: string
  isDefault?: number
}

export function getAddresses() {
  return request<Record<string, unknown>, Address[]>({
    url: '/addresses',
    method: 'get'
  })
}

export function createAddress(data: CreateAddressData) {
  return request<Record<string, unknown>, number>({
    url: '/addresses',
    method: 'post',
    data
  })
}

export function updateAddress(id: number, data: UpdateAddressData) {
  return request<Record<string, unknown>, boolean>({
    url: `/addresses/${id}`,
    method: 'put',
    data
  })
}

export function deleteAddress(id: number) {
  return request<Record<string, unknown>, boolean>({
    url: `/addresses/${id}`,
    method: 'delete'
  })
}

export function setDefaultAddress(id: number) {
  return request<Record<string, unknown>, boolean>({
    url: `/addresses/${id}/default`,
    method: 'put'
  })
}
