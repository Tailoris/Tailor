import { post, get } from './request'
import { uploadFile } from './request'
import type { MerchantApplyRequest } from './types'

interface MerchantInfo {
  id: number
  merchantName: string
  merchantType: number
  status: number
  logo: string
  description: string
  contactName: string
  contactPhone: string
}

export function applyMerchant(data: MerchantApplyRequest) {
  return post<void>('merchant/apply', data)
}

export function getMerchantInfo() {
  return get<MerchantInfo>('merchant/info')
}

export { uploadFile }
