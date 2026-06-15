import request from './request'
import type { AfterSaleTicket, PageResponse } from '@/types'

interface TicketListParams {
  status?: string
  ticketNo?: string
  current?: number
  size?: number
}

interface ProcessParams {
  action: 'approve' | 'reject'
  refundAmount?: number
  remark: string
}

export const listTickets = (params: TicketListParams) => {
  return request<any, PageResponse<AfterSaleTicket>>({
    url: '/aftersale/tickets',
    method: 'GET',
    params,
  })
}

export const getTicketDetail = (ticketNo: string) => {
  return request<any, AfterSaleTicket>({
    url: `/aftersale/tickets/${ticketNo}`,
    method: 'GET',
  })
}

export const processTicket = (ticketNo: string, data: ProcessParams) => {
  return request<any, AfterSaleTicket>({
    url: `/aftersale/tickets/${ticketNo}/process`,
    method: 'POST',
    data,
  })
}

export const approveRefund = (ticketNo: string, refundAmount: number, remark: string) => {
  return request<any, AfterSaleTicket>({
    url: `/aftersale/tickets/${ticketNo}/approve`,
    method: 'POST',
    data: { refundAmount, remark },
  })
}

export const rejectTicket = (ticketNo: string, remark: string) => {
  return request<any, AfterSaleTicket>({
    url: `/aftersale/tickets/${ticketNo}/reject`,
    method: 'POST',
    data: { remark },
  })
}
