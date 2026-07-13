import { vi, describe, it, expect, beforeEach } from 'vitest';

vi.mock('../request', () => ({
  default: vi.fn().mockResolvedValue({}),
}));

import request from '../request';
import { createOrder, getOrders, getOrderDetail, payOrder, confirmOrder, cancelOrder } from '../order';

describe('order API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('createOrder', () => {
    it('should create order successfully', async () => {
      (request as vi.Mock).mockResolvedValue({ orderNo: 'ORD202401010001' });

      const result = await createOrder({
        items: [{ productId: 1, skuId: 1, quantity: 2 }],
        addressId: 1,
      });
      expect(result.orderNo).toBe('ORD202401010001');
    });

    it('should handle create failure - insufficient stock', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('库存不足'));

      await expect(
        createOrder({
          items: [{ productId: 1, skuId: 1, quantity: 1000 }],
          addressId: 1,
        })
      ).rejects.toThrow('库存不足');
    });

    it('should handle create failure - address not found', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('收货地址不存在'));

      await expect(
        createOrder({
          items: [{ productId: 1, skuId: 1, quantity: 1 }],
          addressId: 999,
        })
      ).rejects.toThrow('收货地址不存在');
    });

    it('should handle create failure - coupon invalid', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('优惠券无效'));

      await expect(
        createOrder({
          items: [{ productId: 1, skuId: 1, quantity: 1 }],
          addressId: 1,
          couponId: 999,
        })
      ).rejects.toThrow('优惠券无效');
    });
  });

  describe('getOrders', () => {
    it('should get order list successfully', async () => {
      const mockOrders = {
        records: [
          { orderNo: 'ORD001', status: 1, totalAmount: 99.9, createTime: '2024-01-01' },
          { orderNo: 'ORD002', status: 2, totalAmount: 199.9, createTime: '2024-01-02' },
        ],
        current: 1,
        size: 10,
        total: 2,
      };
      (request as vi.Mock).mockResolvedValue(mockOrders);

      const result = await getOrders({ status: 1, current: 1, size: 10 });
      expect(result).toEqual(mockOrders);
      expect(result.records.length).toBe(2);
    });

    it('should get order list with default params', async () => {
      const mockOrders = {
        records: [],
        current: 1,
        size: 10,
        total: 0,
      };
      (request as vi.Mock).mockResolvedValue(mockOrders);

      const result = await getOrders({});
      expect(result).toEqual(mockOrders);
    });
  });

  describe('getOrderDetail', () => {
    it('should get order detail successfully', async () => {
      const mockOrder = {
        orderNo: 'ORD001',
        status: 1,
        totalAmount: 99.9,
        items: [{ productName: 'Product 1', price: 99.9, quantity: 1 }],
        address: { name: 'Test User', phone: '13800138000', detail: 'Address' },
      };
      (request as vi.Mock).mockResolvedValue(mockOrder);

      const result = await getOrderDetail('ORD001');
      expect(result).toEqual(mockOrder);
      expect(result.orderNo).toBe('ORD001');
    });

    it('should handle order not found', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('订单不存在'));

      await expect(getOrderDetail('ORD999')).rejects.toThrow('订单不存在');
    });
  });

  describe('payOrder', () => {
    it('should pay order successfully', async () => {
      (request as vi.Mock).mockResolvedValue(true);

      const result = await payOrder('ORD001');
      expect(result).toBe(true);
    });

    it('should handle pay failure - order already paid', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('订单已支付'));

      await expect(payOrder('ORD001')).rejects.toThrow('订单已支付');
    });

    it('should handle pay failure - order expired', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('订单已过期'));

      await expect(payOrder('ORD001')).rejects.toThrow('订单已过期');
    });

    it('should handle pay failure - insufficient balance', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('余额不足'));

      await expect(payOrder('ORD001')).rejects.toThrow('余额不足');
    });
  });

  describe('confirmOrder', () => {
    it('should confirm order successfully', async () => {
      (request as vi.Mock).mockResolvedValue(true);

      const result = await confirmOrder('ORD001');
      expect(result).toBe(true);
    });

    it('should handle confirm failure - order status invalid', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('订单状态不允许确认'));

      await expect(confirmOrder('ORD001')).rejects.toThrow('订单状态不允许确认');
    });
  });

  describe('cancelOrder', () => {
    it('should cancel order successfully', async () => {
      (request as vi.Mock).mockResolvedValue(true);

      const result = await cancelOrder('ORD001');
      expect(result).toBe(true);
    });

    it('should handle cancel failure - order status invalid', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('订单状态不允许取消'));

      await expect(cancelOrder('ORD001')).rejects.toThrow('订单状态不允许取消');
    });
  });
});
