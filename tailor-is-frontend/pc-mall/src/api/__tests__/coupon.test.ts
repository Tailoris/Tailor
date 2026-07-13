import { describe, it, expect, vi, beforeEach } from 'vitest';
import request from '../request';
import { getCouponList, getMyCoupons, receiveCoupon, checkCoupon } from '../coupon';

vi.mock('../request', () => ({
  default: vi.fn(),
}));

describe('Coupon API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getCouponList', () => {
    it('should fetch coupon list successfully', async () => {
      const mockCoupons = [
        {
          id: 1,
          name: '满100减20',
          type: 1,
          discount: 20,
          minAmount: 100,
          maxDiscount: 20,
          startTime: '2026-01-01',
          endTime: '2026-12-31',
          status: 1,
          usedCount: 100,
          totalCount: 1000,
        },
        {
          id: 2,
          name: '新人专享50元券',
          type: 2,
          discount: 50,
          minAmount: 0,
          maxDiscount: 50,
          startTime: '2026-01-01',
          endTime: '2026-12-31',
          status: 1,
          usedCount: 500,
          totalCount: 2000,
        },
      ];
      (request as vi.Mock).mockResolvedValue(mockCoupons);

      const result = await getCouponList();

      expect(request).toHaveBeenCalledWith({
        url: '/coupon/list',
        method: 'get',
      });
      expect(result).toEqual(mockCoupons);
    });

    it('should handle fetch failure', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('获取优惠券列表失败'));

      await expect(getCouponList()).rejects.toThrow('获取优惠券列表失败');
    });

    it('should return empty array when no coupons', async () => {
      (request as vi.Mock).mockResolvedValue([]);

      const result = await getCouponList();

      expect(result).toEqual([]);
    });
  });

  describe('getMyCoupons', () => {
    it('should fetch my coupons successfully', async () => {
      const mockCoupons = [
        {
          id: 1,
          name: '满100减20',
          type: 1,
          discount: 20,
          minAmount: 100,
          maxDiscount: 20,
          startTime: '2026-01-01',
          endTime: '2026-12-31',
          status: 1,
          usedCount: 0,
          totalCount: 1,
        },
      ];
      (request as vi.Mock).mockResolvedValue(mockCoupons);

      const result = await getMyCoupons();

      expect(request).toHaveBeenCalledWith({
        url: '/coupon/my',
        method: 'get',
      });
      expect(result).toEqual(mockCoupons);
    });

    it('should handle fetch failure', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('获取我的优惠券失败'));

      await expect(getMyCoupons()).rejects.toThrow('获取我的优惠券失败');
    });

    it('should return empty array when no coupons received', async () => {
      (request as vi.Mock).mockResolvedValue([]);

      const result = await getMyCoupons();

      expect(result).toEqual([]);
    });
  });

  describe('receiveCoupon', () => {
    it('should receive coupon successfully', async () => {
      (request as vi.Mock).mockResolvedValue(true);

      const result = await receiveCoupon(1);

      expect(request).toHaveBeenCalledWith({
        url: '/coupon/1/receive',
        method: 'post',
      });
      expect(result).toBe(true);
    });

    it('should handle receive failure - coupon not found', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('优惠券不存在'));

      await expect(receiveCoupon(999)).rejects.toThrow('优惠券不存在');
    });

    it('should handle receive failure - coupon expired', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('优惠券已过期'));

      await expect(receiveCoupon(1)).rejects.toThrow('优惠券已过期');
    });

    it('should handle receive failure - coupon already received', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('您已领取过该优惠券'));

      await expect(receiveCoupon(1)).rejects.toThrow('您已领取过该优惠券');
    });

    it('should handle receive failure - coupon sold out', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('优惠券已领完'));

      await expect(receiveCoupon(1)).rejects.toThrow('优惠券已领完');
    });
  });

  describe('checkCoupon', () => {
    it('should check coupon valid successfully', async () => {
      (request as vi.Mock).mockResolvedValue({ discount: 20, valid: true, message: '可用' });

      const result = await checkCoupon(1, 150);

      expect(request).toHaveBeenCalledWith({
        url: '/coupon/1/check',
        method: 'get',
        params: { amount: 150 },
      });
      expect(result).toEqual({ discount: 20, valid: true, message: '可用' });
    });

    it('should check coupon invalid - not meet min amount', async () => {
      (request as vi.Mock).mockResolvedValue({ discount: 0, valid: false, message: '未满足最低消费金额' });

      const result = await checkCoupon(1, 50);

      expect(result).toEqual({ discount: 0, valid: false, message: '未满足最低消费金额' });
    });

    it('should check coupon invalid - expired', async () => {
      (request as vi.Mock).mockResolvedValue({ discount: 0, valid: false, message: '优惠券已过期' });

      const result = await checkCoupon(1, 200);

      expect(result).toEqual({ discount: 0, valid: false, message: '优惠券已过期' });
    });

    it('should check coupon invalid - not received', async () => {
      (request as vi.Mock).mockResolvedValue({ discount: 0, valid: false, message: '您未领取该优惠券' });

      const result = await checkCoupon(1, 200);

      expect(result).toEqual({ discount: 0, valid: false, message: '您未领取该优惠券' });
    });
  });
});
