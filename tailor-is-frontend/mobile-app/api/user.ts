import { get, post, put, del } from './request'
import type { UserInfo, AddressInfo, AddressRequest, PaginationParams, PageResult, ProductInfo } from './types'

interface CouponInfo {
  id: number
  name: string
  discount: number
  minAmount: number
  expireTime: string
  status: number
}

interface PointsLogItem {
  id: number
  points: number
  type: number
  description: string
  createTime: string
}

export function updateProfile(data: Partial<UserInfo>) {
  return put<void>('user/profile', data)
}

export function getAddresses() {
  return get<AddressInfo[]>('user/addresses')
}

export function getAddressDetail(addressId: number) {
  return get<AddressInfo>(`user/address/${addressId}`)
}

export function addAddress(data: AddressRequest) {
  return post<void>('user/address', data)
}

export function updateAddress(addressId: number, data: AddressRequest) {
  return put<void>(`user/address/${addressId}`, data)
}

export function deleteAddress(addressId: number) {
  return del<void>(`user/address/${addressId}`)
}

export function setDefaultAddress(addressId: number) {
  return put<void>(`user/address/default/${addressId}`)
}

export function getFavorites(params?: PaginationParams) {
  return get<PageResult<ProductInfo>>('user/favorites', params)
}

export function addFavorite(productId: number) {
  return post<void>('user/favorite', { productId })
}

export function removeFavorite(productId: number) {
  return del<void>(`user/favorite/${productId}`)
}

export function getCoupons() {
  return get<CouponInfo[]>('user/coupons')
}

export function getPointsLog(params?: PaginationParams) {
  return get<PageResult<PointsLogItem>>('user/points', params)
}
