import request from './request'
import type { User, PageResponse, Product } from '@/types'

interface UpdateProfileData {
  nickname?: string
  avatar?: string
  gender?: number
  birthday?: string
  email?: string
}

export function updateProfile(data: UpdateProfileData) {
  return request<Record<string, unknown>, User>({
    url: '/user/profile',
    method: 'put',
    data
  })
}

export function updatePassword(oldPassword: string, newPassword: string) {
  return request<Record<string, unknown>, boolean>({
    url: '/user/password',
    method: 'put',
    data: { oldPassword, newPassword }
  })
}

export function getFavorites() {
  return request<Record<string, unknown>, PageResponse<Product>>({
    url: '/user/favorites',
    method: 'get'
  })
}

export function getPoints() {
  return request<Record<string, unknown>, { points: number; records: { id: number; points: number; type: number; description: string; createdAt: string }[] }>({
    url: '/user/points',
    method: 'get'
  })
}

export function addFavorite(productId: number) {
  return request<Record<string, unknown>, boolean>({
    url: `/user/favorites/${productId}`,
    method: 'post'
  })
}

export function removeFavorite(productId: number) {
  return request<Record<string, unknown>, boolean>({
    url: `/user/favorites/${productId}`,
    method: 'delete'
  })
}
