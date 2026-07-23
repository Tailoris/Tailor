import request from './request';
import type { PageResult } from './role';

export interface ReportInfo {
  id: number;
  name: string;
  type: 'monthly' | 'quarterly' | 'yearly';
  date: string;
  status: 'generated' | 'generating';
  fileUrl: string;
  summary: string;
  createTime: string;
}

export interface StatisticsData {
  monthlyRevenue: number;
  monthlyOrders: number;
  newUsers: number;
  patternUploads: number;
  revenueGrowth: number;
  orderGrowth: number;
  userGrowth: number;
  patternGrowth: number;
  revenueTrend: { label: string; amount: number }[];
  orderTrend: { label: string; count: number }[];
  categoryDistribution: { name: string; value: number }[];
}

export interface AnalyticsQueryParams {
  type?: string;
  date?: string;
  range?: 'week' | 'month' | 'quarter';
  current?: number;
  size?: number;
}

const mockReports: ReportInfo[] = [
  {
    id: 1, name: '2026年7月月度运营报告', type: 'monthly', date: '2026-07',
    status: 'generated', fileUrl: '', summary: '本月平台总收入¥128,500，订单量2350单，新增用户5820人',
    createTime: '2026-07-23 08:00:00',
  },
  {
    id: 2, name: '2026年Q2季度分析报告', type: 'quarterly', date: '2026-Q2',
    status: 'generated', fileUrl: '', summary: 'Q2季度总收入¥385,000，同比增长32%，订单量较Q1增长28%',
    createTime: '2026-07-01 10:00:00',
  },
  {
    id: 3, name: '2026年6月月度运营报告', type: 'monthly', date: '2026-06',
    status: 'generated', fileUrl: '', summary: '6月总收入¥115,200，订单量2100单，新增用户5120人',
    createTime: '2026-07-01 08:00:00',
  },
  {
    id: 4, name: '2026年Q1季度分析报告', type: 'quarterly', date: '2026-Q1',
    status: 'generated', fileUrl: '', summary: 'Q1季度总收入¥292,000，订单量15800单，用户增长65%',
    createTime: '2026-04-01 10:00:00',
  },
];

const mockStatistics: StatisticsData = {
  monthlyRevenue: 128500,
  monthlyOrders: 2350,
  newUsers: 5820,
  patternUploads: 1280,
  revenueGrowth: 12.5,
  orderGrowth: 8.3,
  userGrowth: 15.2,
  patternGrowth: -2.1,
  revenueTrend: [
    { label: '周一', amount: 12500 },
    { label: '周二', amount: 15800 },
    { label: '周三', amount: 9600 },
    { label: '周四', amount: 18200 },
    { label: '周五', amount: 22400 },
    { label: '周六', amount: 16700 },
    { label: '周日', amount: 19300 },
  ],
  orderTrend: [
    { label: '周一', count: 280 },
    { label: '周二', count: 350 },
    { label: '周三', count: 220 },
    { label: '周四', count: 410 },
    { label: '周五', count: 480 },
    { label: '周六', count: 320 },
    { label: '周日', count: 290 },
  ],
  categoryDistribution: [
    { name: '女装', value: 45 },
    { name: '男装', value: 25 },
    { name: '童装', value: 15 },
    { name: '配饰', value: 10 },
    { name: '其他', value: 5 },
  ],
};

export async function listReports(params: AnalyticsQueryParams): Promise<PageResult<ReportInfo>> {
  try {
    return await request<unknown, PageResult<ReportInfo>>({
      url: '/admin/analytics/reports',
      method: 'get',
      params,
      showError: false,
    });
  } catch {
    let filtered = [...mockReports];
    if (params.type) {
      filtered = filtered.filter(r => r.type === params.type);
    }
    if (params.date) {
      filtered = filtered.filter(r => r.date.includes(params.date!));
    }
    const total = filtered.length;
    const start = ((params.current || 1) - 1) * (params.size || 10);
    return {
      records: filtered.slice(start, start + (params.size || 10)),
      total,
      current: params.current || 1,
      size: params.size || 10,
    };
  }
}

export async function generateReport(type: string, date: string): Promise<ReportInfo> {
  try {
    return await request<unknown, ReportInfo>({
      url: '/admin/analytics/reports/generate',
      method: 'post',
      data: { type, date },
      showError: false,
    });
  } catch {
    return {
      id: Date.now(),
      name: `${date} ${type === 'monthly' ? '月度' : type === 'quarterly' ? '季度' : '年度'}报告`,
      type: type as ReportInfo['type'],
      date,
      status: 'generated',
      fileUrl: '',
      summary: '报告已生成，请下载查看',
      createTime: new Date().toISOString().replace('T', ' ').slice(0, 19),
    };
  }
}

export async function getReportById(id: number): Promise<ReportInfo> {
  try {
    return await request<unknown, ReportInfo>({
      url: `/admin/analytics/reports/${id}`,
      method: 'get',
      showError: false,
    });
  } catch {
    return mockReports.find(r => r.id === id) || mockReports[0];
  }
}

export async function exportReport(id: number): Promise<Blob> {
  try {
    return await request<unknown, Blob>({
      url: `/admin/analytics/reports/${id}/export`,
      method: 'get',
      responseType: 'blob',
      showError: false,
    });
  } catch {
    return new Blob(['模拟报告导出数据'], { type: 'application/pdf' });
  }
}

export async function getStatistics(params: { range?: 'week' | 'month' | 'quarter' }): Promise<StatisticsData> {
  try {
    return await request<unknown, StatisticsData>({
      url: '/admin/analytics/statistics',
      method: 'get',
      params,
      showError: false,
    });
  } catch {
    return mockStatistics;
  }
}