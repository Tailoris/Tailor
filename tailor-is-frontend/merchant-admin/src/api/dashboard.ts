import request from './request'
import type { DashboardStats } from '@/types'

export const getDashboardStats = () => {
  return request<Record<string, unknown>, DashboardStats>({
    url: '/dashboard/stats',
    method: 'GET',
  })
}
