import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import request from './request';
import {
  listTransactions,
  getRevenueStats,
  listSettlements,
  approveSettlement,
  rejectSettlement,
  paySettlement,
  listRefunds,
  approveRefund,
  rejectRefund,
  completeRefund,
  listFinanceReports,
  generateFinanceReport,
  exportFinanceReport,
  listFeeConfigs,
  updateFeeConfig,
} from './finance';

vi.mock('./request');

describe('Finance API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Transaction API', () => {
    it('listTransactions should return transaction list', async () => {
      const mockResponse = {
        records: [],
        total: 0,
        current: 1,
        size: 10,
      };
      (request as vi.Mock).mockResolvedValue(mockResponse);

      const result = await listTransactions({ current: 1, size: 10 });
      expect(result).toEqual(mockResponse);
      expect(request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/admin/finance/transactions',
          method: 'get',
        })
      );
    });

    it('listTransactions should return mock data when API fails', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('API Error'));

      const result = await listTransactions({ current: 1, size: 10 });
      expect(result).toBeDefined();
      expect(result.records).toBeInstanceOf(Array);
      expect(result.total).toBeGreaterThan(0);
    });
  });

  describe('Revenue API', () => {
    it('getRevenueStats should return revenue data', async () => {
      const mockResponse = {
        todayRevenue: 1000,
        yesterdayRevenue: 800,
        thisMonthRevenue: 20000,
        lastMonthRevenue: 18000,
        totalRevenue: 100000,
        platformFee: 5000,
        settlementAmount: 80000,
        refundAmount: 5000,
        revenueGrowth: 10,
        feeGrowth: 5,
        dailyTrend: [],
        channelDistribution: [],
        categoryDistribution: [],
      };
      (request as vi.Mock).mockResolvedValue(mockResponse);

      const result = await getRevenueStats({ range: 'month' });
      expect(result).toEqual(mockResponse);
    });

    it('getRevenueStats should return mock data when API fails', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('API Error'));

      const result = await getRevenueStats({});
      expect(result).toBeDefined();
      expect(result.totalRevenue).toBeGreaterThan(0);
    });
  });

  describe('Settlement API', () => {
    it('listSettlements should return settlement list', async () => {
      const mockResponse = {
        records: [],
        total: 0,
        current: 1,
        size: 10,
      };
      (request as vi.Mock).mockResolvedValue(mockResponse);

      const result = await listSettlements({ current: 1, size: 10 });
      expect(result).toEqual(mockResponse);
    });

    it('approveSettlement should call API correctly', async () => {
      await approveSettlement(1, 'approved');
      expect(request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/admin/finance/settlements/1/approve',
          method: 'post',
        })
      );
    });

    it('rejectSettlement should call API correctly', async () => {
      await rejectSettlement(1, 'rejected');
      expect(request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/admin/finance/settlements/1/reject',
          method: 'post',
        })
      );
    });

    it('paySettlement should call API correctly', async () => {
      await paySettlement(1);
      expect(request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/admin/finance/settlements/1/pay',
          method: 'post',
        })
      );
    });
  });

  describe('Refund API', () => {
    it('listRefunds should return refund list', async () => {
      const mockResponse = {
        records: [],
        total: 0,
        current: 1,
        size: 10,
      };
      (request as vi.Mock).mockResolvedValue(mockResponse);

      const result = await listRefunds({ current: 1, size: 10 });
      expect(result).toEqual(mockResponse);
    });

    it('approveRefund should call API correctly', async () => {
      await approveRefund(1, 'approved');
      expect(request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/admin/finance/refunds/1/approve',
          method: 'post',
        })
      );
    });

    it('rejectRefund should call API correctly', async () => {
      await rejectRefund(1, 'rejected');
      expect(request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/admin/finance/refunds/1/reject',
          method: 'post',
        })
      );
    });

    it('completeRefund should call API correctly', async () => {
      await completeRefund(1);
      expect(request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/admin/finance/refunds/1/complete',
          method: 'post',
        })
      );
    });
  });

  describe('Finance Report API', () => {
    it('listFinanceReports should return report list', async () => {
      const mockResponse = {
        records: [],
        total: 0,
        current: 1,
        size: 10,
      };
      (request as vi.Mock).mockResolvedValue(mockResponse);

      const result = await listFinanceReports({ current: 1, size: 10 });
      expect(result).toEqual(mockResponse);
    });

    it('generateFinanceReport should call API correctly', async () => {
      const mockResponse = {
        id: 1,
        name: '2024-01-01 日报',
        type: 'daily',
        period: '2024-01-01',
        totalRevenue: 1000,
        totalFee: 50,
        totalSettlement: 900,
        totalRefund: 50,
        netProfit: 0,
        status: 'generated',
        createTime: '2024-01-01 00:00:00',
      };
      (request as vi.Mock).mockResolvedValue(mockResponse);

      const result = await generateFinanceReport('daily', '2024-01-01');
      expect(result).toEqual(mockResponse);
    });

    it('exportFinanceReport should return blob', async () => {
      const mockBlob = new Blob(['test'], { type: 'application/pdf' });
      (request as vi.Mock).mockResolvedValue(mockBlob);

      const result = await exportFinanceReport(1);
      expect(result).toBeInstanceOf(Blob);
    });
  });

  describe('Fee Config API', () => {
    it('listFeeConfigs should return config list', async () => {
      const mockResponse = [];
      (request as vi.Mock).mockResolvedValue(mockResponse);

      const result = await listFeeConfigs();
      expect(result).toEqual(mockResponse);
    });

    it('updateFeeConfig should call API correctly', async () => {
      const mockResponse = {
        id: 1,
        name: '交易手续费',
        rate: 3,
        fixedAmount: 0,
        minAmount: 0,
        maxAmount: 100,
        description: '交易手续费',
        status: 'active',
        createTime: '2024-01-01',
        updateTime: '2024-01-02',
      };
      (request as vi.Mock).mockResolvedValue(mockResponse);

      const result = await updateFeeConfig(1, { rate: 3 });
      expect(result).toEqual(mockResponse);
    });
  });
});
