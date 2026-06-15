import request from './request'
import type { DashboardStats } from '@/types'

export const getDashboardStats = () => {
  return request<any, DashboardStats>({
    url: '/dashboard/stats',
    method: 'GET',
  })
}
