import request from './request';

export interface ModuleStatus {
  name: string;
  port: number;
  status: 'running' | 'stopped';
}

export interface RevenueTrendItem {
  date: string;
  amount: number;
}

export interface DashboardStats {
  userCount: number;
  merchantCount: number;
  orderCount: number;
  todayRevenue: number;
  todayOrders: number;
  pendingAuditCount: number;
  userGrowth?: number;
  orderGrowth?: number;
  revenueGrowth?: number;
  moduleStatus?: ModuleStatus[];
  revenueTrend?: RevenueTrendItem[];
}

const mockWeeklyData: RevenueTrendItem[] = [
  { date: '2026-07-17', amount: 12500 },
  { date: '2026-07-18', amount: 15800 },
  { date: '2026-07-19', amount: 9600 },
  { date: '2026-07-20', amount: 18200 },
  { date: '2026-07-21', amount: 22400 },
  { date: '2026-07-22', amount: 16700 },
  { date: '2026-07-23', amount: 19300 },
];

const mockMonthlyData: RevenueTrendItem[] = Array.from({ length: 30 }, (_, i) => ({
  date: `2026-07-${String(i + 1).padStart(2, '0')}`,
  amount: Math.floor(Math.random() * 20000) + 8000,
}));

const mockModuleStatus: ModuleStatus[] = [
  { name: '用户服务', port: 8101, status: 'running' },
  { name: '订单服务', port: 8103, status: 'running' },
  { name: '商品服务', port: 8102, status: 'running' },
  { name: '支付服务', port: 8104, status: 'running' },
  { name: '营销服务', port: 8105, status: 'running' },
  { name: '纸样服务', port: 8115, status: 'running' },
  { name: '版权服务', port: 8112, status: 'running' },
  { name: '社区服务', port: 8108, status: 'running' },
];

const mockStats: DashboardStats = {
  userCount: 12580,
  merchantCount: 326,
  orderCount: 45280,
  todayRevenue: 19300,
  todayOrders: 156,
  pendingAuditCount: 28,
  userGrowth: 12.5,
  orderGrowth: 8.3,
  revenueGrowth: 15.7,
};

export async function getDashboardStats(params?: { range?: 'week' | 'month' }): Promise<DashboardStats> {
  try {
    const data = await request<unknown, DashboardStats>({
      url: '/admin/dashboard/stats',
      method: 'get',
      params,
      showError: false,
    });
    return {
      ...mockStats,
      moduleStatus: mockModuleStatus,
      revenueTrend: params?.range === 'month' ? mockMonthlyData : mockWeeklyData,
      ...data,
    };
  } catch {
    return {
      ...mockStats,
      moduleStatus: mockModuleStatus,
      revenueTrend: params?.range === 'month' ? mockMonthlyData : mockWeeklyData,
    };
  }
}
