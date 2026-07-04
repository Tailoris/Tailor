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
  userCount: number
  merchantCount: number
  orderCount: number
  todayRevenue: number
  todayOrders: number
  pendingAuditCount: number
}

export function getDashboardStats(params?: { range?: 'week' | 'month' }) {
  return request<unknown, DashboardStats>({
    url: '/admin/dashboard/stats',
    method: 'get',
    params
  })
}
