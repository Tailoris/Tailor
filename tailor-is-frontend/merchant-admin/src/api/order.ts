import request from './request';
import type { Order, PageResponse } from '@/types';

interface OrderListParams {
  orderNo?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  current?: number;
  size?: number;
}

interface ShipParams {
  logisticsCompany: string;
  trackingNumber: string;
}

export const listOrders = (params: OrderListParams) => {
  return request<Record<string, unknown>, PageResponse<Order>>({
    url: '/api/v1/order/list',
    method: 'GET',
    params,
  });
};

export const getOrderDetail = (orderNo: string) => {
  return request<Record<string, unknown>, Order>({
    url: `/api/v1/order/${orderNo}`,
    method: 'GET',
  });
};

export const shipOrder = (orderNo: string, data: ShipParams) => {
  return request<Record<string, unknown>, Order>({
    url: `/api/v1/order/${orderNo}/logistics`,
    method: 'PUT',
    data,
  });
};

export const updatePrice = (orderNo: string, price: number) => {
  return request<Record<string, unknown>, Order>({
    url: `/api/v1/order/${orderNo}/price`,
    method: 'PUT',
    data: { price },
  });
};

export const getOrderByShop = (shopId: number, params: OrderListParams) => {
  return request<Record<string, unknown>, PageResponse<Order>>({
    url: `/api/v1/order/list`,
    method: 'GET',
    params: { ...params, shopId },
  });
};

/** FE-H-4: 更新订单备注 */
export const updateOrderRemark = (orderNo: string, remark: string) => {
  return request<Record<string, unknown>, Order>({
    url: `/api/v1/order/${orderNo}/remark`,
    method: 'PUT',
    data: { remark },
  });
};
