import request from './request';

export interface Coupon {
  id: number;
  name: string;
  type: number;
  discount: number;
  minAmount: number;
  maxDiscount: number;
  startTime: string;
  endTime: string;
  status: number;
  usedCount: number;
  totalCount: number;
}

export function getCouponList() {
  return request<Record<string, unknown>, Coupon[]>({
    url: '/api/v1/coupon/list',
    method: 'get',
  });
}

export function getMyCoupons() {
  return request<Record<string, unknown>, Coupon[]>({
    url: '/api/v1/coupon/my',
    method: 'get',
  });
}

export function receiveCoupon(couponId: number) {
  return request<Record<string, unknown>, boolean>({
    url: `/api/v1/coupon/${couponId}/receive`,
    method: 'post',
  });
}

export function checkCoupon(couponId: number, amount: number) {
  return request<Record<string, unknown>, { discount: number; valid: boolean; message: string }>({
    url: `/api/v1/coupon/${couponId}/check`,
    method: 'get',
    params: { amount },
  });
}
