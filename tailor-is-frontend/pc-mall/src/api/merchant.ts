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
  return request<any, boolean>({
    url: '/merchant/apply',
    method: 'post',
    data
  })
}

export function getMerchantInfo() {
  return request<any, Merchant>({
    url: '/merchant/info',
    method: 'get'
  })
}
