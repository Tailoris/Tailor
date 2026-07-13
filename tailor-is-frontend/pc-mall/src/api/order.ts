import request from './request';
import type { Order, PageResponse } from '@/types';

interface OrderItemRequest {
  productId: number;
  skuId: number;
  quantity: number;
  price?: number;
  shopId?: number;
  merchantId?: number;
}

interface CreateOrderData {
  cartIds?: number[];
  items?: OrderItemRequest[];
  addressId: number;
  remark?: string;
  couponId?: number;
  requestId: string;
}

interface OrderListParams {
  status?: number;
  current?: number;
  size?: number;
}

export function createOrder(data: CreateOrderData) {
  return request<Record<string, unknown>, { orderNo: string }>({
    url: '/api/v1/order',
    method: 'post',
    data,
  });
}

export function getOrders(params: OrderListParams) {
  return request<Record<string, unknown>, PageResponse<Order>>({
    url: '/api/v1/order/list',
    method: 'get',
    params,
  });
}

export function getOrderDetail(orderNo: string) {
  return request<Record<string, unknown>, Order>({
    url: `/api/v1/order/${orderNo}`,
    method: 'get',
  });
}

export function payOrder(orderNo: string) {
  return request<Record<string, unknown>, boolean>({
    url: `/api/v1/order/${orderNo}/pay`,
    method: 'put',
    params: { payType: 1 },
  });
}

export function confirmOrder(orderNo: string) {
  return request<Record<string, unknown>, boolean>({
    url: `/api/v1/order/${orderNo}/confirm`,
    method: 'put',
  });
}

export function cancelOrder(orderNo: string) {
  return request<Record<string, unknown>, boolean>({
    url: `/api/v1/order/${orderNo}/cancel`,
    method: 'put',
    params: { reason: '用户取消' },
  });
}
