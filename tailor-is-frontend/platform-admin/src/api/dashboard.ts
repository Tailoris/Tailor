import request from './request'

export interface ModuleStatus {
  name: string
  port: number
  status: 'running' | 'stopped'
}

export interface RevenueTrendItem {
  date: string
  amount: number
}

export interface DashboardStats {
  totalUsers: number
  totalOrders: number
  totalRevenue: number
  activeModules: number
  userGrowth: number
  orderGrowth: number
  revenueGrowth: number
  moduleStatus: ModuleStatus[]
  revenueTrend: RevenueTrendItem[]
}

export function getDashboardStats(params?: { range?: 'week' | 'month' }) {
  return request<DashboardStats>({
    url: '/admin/dashboard/stats',
    method: 'get',
    params
  })
}
