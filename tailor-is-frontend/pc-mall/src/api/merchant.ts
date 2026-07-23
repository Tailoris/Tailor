import request from './request';
import type { Merchant } from '@/types';

export interface MerchantApplyData {
  shopName: string;
  merchantType: number;
  businessCategories: string[];
  companyName?: string;
  licenseNo?: string;
  idCardNo?: string;
  idCardFront?: string;
  idCardBack?: string;
  contactName: string;
  contactPhone: string;
  contactEmail: string;
  province?: string;
  city?: string;
  district?: string;
  address?: string;
  serviceArea?: string;
  businessScope?: string;
  shopDesc?: string;
  licenseImage?: string;
  qualifications?: Array<{
    certType: number;
    certName: string;
    certNo?: string;
    certUrl: string;
  }>;
  patternWorks?: string[];
  designWorks?: string[];
  fabricMaterials?: string[];
  factoryMaterials?: string[];
  bankName?: string;
  bankCard?: string;
  bankHolder?: string;
  agreeMerchantAgreement: boolean;
  agreeCopyrightAgreement: boolean;
  agreeTransactionRules: boolean;
}

export interface UploadResult {
  objectKey: string;
  url: string;
  size: number;
  contentType: string;
}

export function applyMerchant(data: MerchantApplyData) {
  return request<Record<string, unknown>, boolean>({
    url: '/api/v1/merchant/apply',
    method: 'post',
    data,
    showError: false,
  });
}

export function uploadFile(file: File, bizType: string) {
  const formData = new FormData();
  formData.append('file', file);
  return request<Record<string, unknown>, UploadResult>({
    url: `/api/v1/upload/${bizType}`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    showError: false,
  });
}

export function getMerchantInfo() {
  return request<Record<string, unknown>, Merchant>({
    url: '/api/v1/merchant/info',
    method: 'get',
  });
}

export function getApplyStatus() {
  return request<Record<string, unknown>, Merchant>({
    url: '/api/v1/merchant/apply-status',
    method: 'get',
    showError: false,
  });
}
