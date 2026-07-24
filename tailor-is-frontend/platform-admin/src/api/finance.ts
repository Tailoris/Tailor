import request from './request';
import type { PageResult } from './role';

// ==================== 类型定义 ====================

/** 交易流水 */
export interface TransactionInfo {
  id: number;
  transactionNo: string;
  orderNo: string;
  type: 'payment' | 'refund' | 'settlement' | 'platform_fee' | 'deposit';
  amount: number;
  fee: number;
  netAmount: number;
  payerId: number;
  payerName: string;
  payeeId: number;
  payeeName: string;
  channel: 'wechat' | 'alipay' | 'paypal' | 'stripe' | 'balance';
  status: 'pending' | 'success' | 'failed' | 'cancelled';
  remark: string;
  createTime: string;
  completeTime: string;
}

/** 收入统计 */
export interface RevenueStats {
  todayRevenue: number;
  yesterdayRevenue: number;
  thisMonthRevenue: number;
  lastMonthRevenue: number;
  totalRevenue: number;
  platformFee: number;
  settlementAmount: number;
  refundAmount: number;
  revenueGrowth: number;
  feeGrowth: number;
  dailyTrend: { date: string; revenue: number; fee: number }[];
  channelDistribution: { name: string; value: number }[];
  categoryDistribution: { name: string; value: number }[];
}

/** 结算记录 */
export interface SettlementInfo {
  id: number;
  settlementNo: string;
  merchantId: number;
  merchantName: string;
  merchantType: 'designer' | 'pattern_maker' | 'shop';
  period: string;
  orderCount: number;
  totalAmount: number;
  platformFee: number;
  settlementAmount: number;
  status: 'pending' | 'approved' | 'paid' | 'rejected';
  applyTime: string;
  approveTime: string;
  payTime: string;
  remark: string;
}

/** 退款记录 */
export interface RefundInfo {
  id: number;
  refundNo: string;
  orderNo: string;
  userId: number;
  userName: string;
  amount: number;
  reason: string;
  evidence: string[];
  status: 'pending' | 'approved' | 'rejected' | 'completed';
  applyTime: string;
  approveTime: string;
  completeTime: string;
  remark: string;
}

/** 财务报表 */
export interface FinanceReport {
  id: number;
  name: string;
  type: 'daily' | 'weekly' | 'monthly' | 'quarterly' | 'yearly';
  period: string;
  totalRevenue: number;
  totalFee: number;
  totalSettlement: number;
  totalRefund: number;
  netProfit: number;
  status: 'generated' | 'generating';
  createTime: string;
}

/** 平台费用配置 */
export interface FeeConfig {
  id: number;
  name: string;
  configKey: string;
  rate: number;
  fixedAmount: number;
  minAmount: number;
  maxAmount: number;
  description: string;
  status: 'active' | 'inactive';
  updateTime: string;
}

/** 查询参数 */
export interface FinanceQueryParams {
  keyword?: string;
  type?: string;
  status?: string;
  channel?: string;
  startDate?: string;
  endDate?: string;
  current?: number;
  size?: number;
}

// ==================== Mock数据 ====================

const mockTransactions: TransactionInfo[] = [
  { id: 1, transactionNo: 'TXN202607230001', orderNo: 'ORD20260723001', type: 'payment', amount: 299, fee: 29.9, netAmount: 269.1, payerId: 1001, payerName: '张设计', payeeId: 2001, payeeName: '华纺集团', channel: 'wechat', status: 'success', remark: '纸样购买付款', createTime: '2026-07-23 10:30:00', completeTime: '2026-07-23 10:30:05' },
  { id: 2, transactionNo: 'TXN202607230002', orderNo: 'ORD20260722005', type: 'refund', amount: 199, fee: 0, netAmount: 199, payerId: 2001, payerName: '华纺集团', payeeId: 1002, payeeName: '李用户', channel: 'alipay', status: 'success', remark: '用户申请退款', createTime: '2026-07-23 09:15:00', completeTime: '2026-07-23 14:20:00' },
  { id: 3, transactionNo: 'TXN202607230003', orderNo: 'SET20260723001', type: 'settlement', amount: 2390, fee: 239, netAmount: 2151, payerId: 0, payerName: '平台', payeeId: 2001, payeeName: '华纺集团', channel: 'balance', status: 'success', remark: '7月第三周结算', createTime: '2026-07-23 08:00:00', completeTime: '2026-07-23 08:05:00' },
  { id: 4, transactionNo: 'TXN202607220001', orderNo: 'ORD20260722010', type: 'payment', amount: 499, fee: 49.9, netAmount: 449.1, payerId: 1003, payerName: '王用户', payeeId: 2002, payeeName: '恒力集团', channel: 'paypal', status: 'success', remark: '跨境订单付款', createTime: '2026-07-22 16:45:00', completeTime: '2026-07-22 16:45:08' },
  { id: 5, transactionNo: 'TXN202607220002', orderNo: 'ORD20260721003', type: 'deposit', amount: 5000, fee: 0, netAmount: 5000, payerId: 3001, payerName: '王版师', payeeId: 0, payeeName: '平台', channel: 'alipay', status: 'success', remark: '版师入驻质保金', createTime: '2026-07-22 11:00:00', completeTime: '2026-07-22 11:00:03' },
  { id: 6, transactionNo: 'TXN202607210001', orderNo: 'ORD20260721008', type: 'platform_fee', amount: 59.8, fee: 0, netAmount: 59.8, payerId: 2002, payerName: '恒力集团', payeeId: 0, payeeName: '平台', channel: 'balance', status: 'success', remark: '平台交易抽成', createTime: '2026-07-21 18:30:00', completeTime: '2026-07-21 18:30:02' },
];

const mockRevenueStats: RevenueStats = {
  todayRevenue: 18500, yesterdayRevenue: 16300, thisMonthRevenue: 128500, lastMonthRevenue: 115200,
  totalRevenue: 1520000, platformFee: 152000, settlementAmount: 1280000, refundAmount: 88000,
  revenueGrowth: 12.5, feeGrowth: 10.8,
  dailyTrend: [
    { date: '07-17', revenue: 15200, fee: 1520 }, { date: '07-18', revenue: 18300, fee: 1830 },
    { date: '07-19', revenue: 21500, fee: 2150 }, { date: '07-20', revenue: 16800, fee: 1680 },
    { date: '07-21', revenue: 22100, fee: 2210 }, { date: '07-22', revenue: 16300, fee: 1630 },
    { date: '07-23', revenue: 18500, fee: 1850 },
  ],
  channelDistribution: [
    { name: '微信支付', value: 45 }, { name: '支付宝', value: 30 }, { name: 'PayPal', value: 15 }, { name: 'Stripe', value: 10 },
  ],
  categoryDistribution: [
    { name: '纸样交易', value: 55 }, { name: '定制服务', value: 25 }, { name: '课程付费', value: 12 }, { name: '其他', value: 8 },
  ],
};

const mockSettlements: SettlementInfo[] = [
  { id: 1, settlementNo: 'SET20260723001', merchantId: 2001, merchantName: '华纺集团', merchantType: 'shop', period: '2026-07 W3', orderCount: 15, totalAmount: 5980, platformFee: 598, settlementAmount: 5382, status: 'paid', applyTime: '2026-07-23 08:00:00', approveTime: '2026-07-23 08:03:00', payTime: '2026-07-23 08:05:00', remark: '' },
  { id: 2, settlementNo: 'SET20260723002', merchantId: 2002, merchantName: '恒力集团', merchantType: 'shop', period: '2026-07 W3', orderCount: 8, totalAmount: 3200, platformFee: 320, settlementAmount: 2880, status: 'approved', applyTime: '2026-07-23 09:00:00', approveTime: '2026-07-23 10:00:00', payTime: '', remark: '' },
  { id: 3, settlementNo: 'SET20260723003', merchantId: 3001, merchantName: '王版师', merchantType: 'pattern_maker', period: '2026-07 W3', orderCount: 5, totalAmount: 3500, platformFee: 350, settlementAmount: 3150, status: 'pending', applyTime: '2026-07-23 11:00:00', approveTime: '', payTime: '', remark: '申请提前结算' },
  { id: 4, settlementNo: 'SET20260722001', merchantId: 2003, merchantName: '张设计工作室', merchantType: 'designer', period: '2026-07 W2', orderCount: 3, totalAmount: 1800, platformFee: 180, settlementAmount: 1620, status: 'paid', applyTime: '2026-07-22 10:00:00', approveTime: '2026-07-22 14:00:00', payTime: '2026-07-22 16:00:00', remark: '' },
];

const mockRefunds: RefundInfo[] = [
  { id: 1, refundNo: 'REF20260723001', orderNo: 'ORD20260722005', userId: 1002, userName: '李用户', amount: 199, reason: '纸样与实际描述不符，版型偏差较大', evidence: [], status: 'approved', applyTime: '2026-07-23 09:00:00', approveTime: '2026-07-23 14:00:00', completeTime: '2026-07-23 14:20:00', remark: '经核实，同意退款' },
  { id: 2, refundNo: 'REF20260723002', orderNo: 'ORD20260720012', userId: 1004, userName: '赵用户', amount: 299, reason: '未收到定制纸样，商家联系不上', evidence: [], status: 'pending', applyTime: '2026-07-23 10:30:00', approveTime: '', completeTime: '', remark: '' },
  { id: 3, refundNo: 'REF20260722001', orderNo: 'ORD20260718003', userId: 1005, userName: '陈用户', amount: 499, reason: '重复付款，需退回其中一笔', evidence: [], status: 'completed', applyTime: '2026-07-22 14:00:00', approveTime: '2026-07-22 15:00:00', completeTime: '2026-07-22 16:00:00', remark: '已核实重复付款，退款完成' },
  { id: 4, refundNo: 'REF20260721001', orderNo: 'ORD20260715007', userId: 1006, userName: '刘用户', amount: 150, reason: '商品质量不符合预期', evidence: [], status: 'rejected', applyTime: '2026-07-21 08:00:00', approveTime: '2026-07-21 10:00:00', completeTime: '', remark: '经核实，商品符合描述，驳回退款' },
];

const mockFinanceReports: FinanceReport[] = [
  { id: 1, name: '2026年7月23日日报', type: 'daily', period: '2026-07-23', totalRevenue: 18500, totalFee: 1850, totalSettlement: 12000, totalRefund: 500, netProfit: 1500, status: 'generated', createTime: '2026-07-23 23:00:00' },
  { id: 2, name: '2026年7月第3周周报', type: 'weekly', period: '2026-07 W3', totalRevenue: 128500, totalFee: 12850, totalSettlement: 98000, totalRefund: 3500, netProfit: 11500, status: 'generated', createTime: '2026-07-21 00:00:00' },
  { id: 3, name: '2026年7月月报', type: 'monthly', period: '2026-07', totalRevenue: 128500, totalFee: 12850, totalSettlement: 98000, totalRefund: 3500, netProfit: 11500, status: 'generating', createTime: '2026-07-23 08:00:00' },
  { id: 4, name: '2026年Q2季报', type: 'quarterly', period: '2026-Q2', totalRevenue: 385000, totalFee: 38500, totalSettlement: 320000, totalRefund: 12000, netProfit: 38500, status: 'generated', createTime: '2026-07-01 00:00:00' },
];

const mockFeeConfigs: FeeConfig[] = [
  { id: 1, name: '平台交易抽成', configKey: 'platform_fee', rate: 10, fixedAmount: 0, minAmount: 1, maxAmount: 0, description: '每笔交易按成交金额的百分比收取平台服务费', status: 'active', updateTime: '2026-07-01 10:00:00' },
  { id: 2, name: '版师入驻质保金', configKey: 'deposit', rate: 0, fixedAmount: 5000, minAmount: 0, maxAmount: 0, description: '版师入驻时需缴纳的质保金，无违约可退还', status: 'active', updateTime: '2026-07-01 10:00:00' },
  { id: 3, name: '提现手续费', configKey: 'withdraw_fee', rate: 0.5, fixedAmount: 2, minAmount: 0, maxAmount: 50, description: '商家/版师提现时收取的手续费，费率0.5%+固定2元，上限50元', status: 'active', updateTime: '2026-07-15 14:00:00' },
  { id: 4, name: '跨境交易手续费', configKey: 'cross_border_fee', rate: 3, fixedAmount: 5, minAmount: 0, maxAmount: 0, description: '跨境支付额外收取的手续费，费率3%+固定5元', status: 'active', updateTime: '2026-07-10 09:00:00' },
  { id: 5, name: '加急订单服务费', configKey: 'urgent_fee', rate: 50, fixedAmount: 0, minAmount: 0, maxAmount: 0, description: '加急订单在原价基础上额外收取50%服务费', status: 'active', updateTime: '2026-07-01 10:00:00' },
  { id: 6, name: '课程分销佣金', configKey: 'course_commission', rate: 20, fixedAmount: 0, minAmount: 0, maxAmount: 0, description: '课程销售给讲师的佣金分成比例', status: 'inactive', updateTime: '2026-07-20 08:00:00' },
];

// ==================== API函数 ====================

/** 交易流水 */
export async function listTransactions(params: FinanceQueryParams): Promise<PageResult<TransactionInfo>> {
  try {
    return await request<unknown, PageResult<TransactionInfo>>({ url: '/admin/finance/transactions', method: 'get', params, showError: false });
  } catch {
    let filtered = [...mockTransactions];
    if (params.keyword) {
      const kw = params.keyword.toLowerCase();
      filtered = filtered.filter(t => t.transactionNo.toLowerCase().includes(kw) || t.orderNo.toLowerCase().includes(kw) || t.payerName.includes(kw) || t.payeeName.includes(kw));
    }
    if (params.type) filtered = filtered.filter(t => t.type === params.type);
    if (params.status) filtered = filtered.filter(t => t.status === params.status);
    if (params.channel) filtered = filtered.filter(t => t.channel === params.channel);
    const total = filtered.length;
    const start = ((params.current || 1) - 1) * (params.size || 10);
    return { records: filtered.slice(start, start + (params.size || 10)), total, current: params.current || 1, size: params.size || 10 };
  }
}

/** 收入统计 */
export async function getRevenueStats(params: { range?: 'today' | 'week' | 'month' | 'quarter' }): Promise<RevenueStats> {
  try {
    return await request<unknown, RevenueStats>({ url: '/admin/finance/revenue-stats', method: 'get', params, showError: false });
  } catch {
    return mockRevenueStats;
  }
}

/** 结算管理 */
export async function listSettlements(params: FinanceQueryParams): Promise<PageResult<SettlementInfo>> {
  try {
    return await request<unknown, PageResult<SettlementInfo>>({ url: '/admin/finance/settlements', method: 'get', params, showError: false });
  } catch {
    let filtered = [...mockSettlements];
    if (params.keyword) {
      const kw = params.keyword.toLowerCase();
      filtered = filtered.filter(s => s.settlementNo.toLowerCase().includes(kw) || s.merchantName.includes(kw));
    }
    if (params.status) filtered = filtered.filter(s => s.status === params.status);
    const total = filtered.length;
    const start = ((params.current || 1) - 1) * (params.size || 10);
    return { records: filtered.slice(start, start + (params.size || 10)), total, current: params.current || 1, size: params.size || 10 };
  }
}

export async function approveSettlement(id: number, remark?: string): Promise<void> {
  try {
    await request<unknown, void>({ url: `/admin/finance/settlements/${id}/approve`, method: 'post', data: { remark }, showError: false });
  } catch { /* mock */ }
}

export async function rejectSettlement(id: number, remark: string): Promise<void> {
  try {
    await request<unknown, void>({ url: `/admin/finance/settlements/${id}/reject`, method: 'post', data: { remark }, showError: false });
  } catch { /* mock */ }
}

export async function paySettlement(id: number): Promise<void> {
  try {
    await request<unknown, void>({ url: `/admin/finance/settlements/${id}/pay`, method: 'post', showError: false });
  } catch { /* mock */ }
}

/** 退款管理 */
export async function listRefunds(params: FinanceQueryParams): Promise<PageResult<RefundInfo>> {
  try {
    return await request<unknown, PageResult<RefundInfo>>({ url: '/admin/finance/refunds', method: 'get', params, showError: false });
  } catch {
    let filtered = [...mockRefunds];
    if (params.keyword) {
      const kw = params.keyword.toLowerCase();
      filtered = filtered.filter(r => r.refundNo.toLowerCase().includes(kw) || r.orderNo.toLowerCase().includes(kw) || r.userName.includes(kw));
    }
    if (params.status) filtered = filtered.filter(r => r.status === params.status);
    const total = filtered.length;
    const start = ((params.current || 1) - 1) * (params.size || 10);
    return { records: filtered.slice(start, start + (params.size || 10)), total, current: params.current || 1, size: params.size || 10 };
  }
}

export async function approveRefund(id: number, remark?: string): Promise<void> {
  try {
    await request<unknown, void>({ url: `/admin/finance/refunds/${id}/approve`, method: 'post', data: { remark }, showError: false });
  } catch { /* mock */ }
}

export async function rejectRefund(id: number, remark: string): Promise<void> {
  try {
    await request<unknown, void>({ url: `/admin/finance/refunds/${id}/reject`, method: 'post', data: { remark }, showError: false });
  } catch { /* mock */ }
}

export async function completeRefund(id: number): Promise<void> {
  try {
    await request<unknown, void>({ url: `/admin/finance/refunds/${id}/complete`, method: 'post', showError: false });
  } catch { /* mock */ }
}

/** 财务报表 */
export async function listFinanceReports(params: FinanceQueryParams): Promise<PageResult<FinanceReport>> {
  try {
    return await request<unknown, PageResult<FinanceReport>>({ url: '/admin/finance/reports', method: 'get', params, showError: false });
  } catch {
    let filtered = [...mockFinanceReports];
    if (params.type) filtered = filtered.filter(r => r.type === params.type);
    const total = filtered.length;
    const start = ((params.current || 1) - 1) * (params.size || 10);
    return { records: filtered.slice(start, start + (params.size || 10)), total, current: params.current || 1, size: params.size || 10 };
  }
}

export async function generateFinanceReport(type: string, period: string): Promise<FinanceReport> {
  try {
    return await request<unknown, FinanceReport>({ url: '/admin/finance/reports/generate', method: 'post', data: { type, period }, showError: false });
  } catch {
    return { id: Date.now(), name: `${period} ${type === 'daily' ? '日报' : type === 'weekly' ? '周报' : type === 'monthly' ? '月报' : type === 'quarterly' ? '季报' : '年报'}`, type: type as FinanceReport['type'], period, totalRevenue: 0, totalFee: 0, totalSettlement: 0, totalRefund: 0, netProfit: 0, status: 'generated', createTime: new Date().toISOString().replace('T', ' ').slice(0, 19) };
  }
}

export async function exportFinanceReport(id: number): Promise<Blob> {
  try {
    return await request<unknown, Blob>({ url: `/admin/finance/reports/${id}/export`, method: 'get', responseType: 'blob', showError: false });
  } catch {
    return new Blob(['模拟报告导出数据'], { type: 'application/pdf' });
  }
}

/** 费用配置 */
export async function listFeeConfigs(): Promise<FeeConfig[]> {
  try {
    return await request<unknown, FeeConfig[]>({ url: '/admin/finance/fee-configs', method: 'get', showError: false });
  } catch {
    return [...mockFeeConfigs];
  }
}

export async function updateFeeConfig(id: number, data: Partial<FeeConfig>): Promise<FeeConfig> {
  try {
    return await request<unknown, FeeConfig>({ url: `/admin/finance/fee-configs/${id}`, method: 'put', data, showError: false });
  } catch {
    const existing = mockFeeConfigs.find(c => c.id === id);
    return { ...existing!, ...data, updateTime: new Date().toISOString().replace('T', ' ').slice(0, 19) };
  }
}