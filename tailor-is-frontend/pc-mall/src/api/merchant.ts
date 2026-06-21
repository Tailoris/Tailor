import request from './request'
import type { Merchant } from '@/types'

interface MerchantApplyData {
  merchantType: number
  companyName: string
  licenseNo: string
  licenseImage: string
  certImages: string[]
  contactName: string
  contactPhone: string
  contactEmail: string
}

export function applyMerchant(data: MerchantApplyData) {
  return request<Record<string, unknown>, boolean>({
    url: '/merchant/apply',
    method: 'post',
    data
  })
}

export function getMerchantInfo() {
  return request<Record<string, unknown>, Merchant>({
    url: '/merchant/info',
    method: 'get'
  })
}
