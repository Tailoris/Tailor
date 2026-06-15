import request from './request'
import type { SettlementRecord, PageResponse } from '@/types'

interface SettlementParams {
  current?: number
  size?: number
}

interface WithdrawParams {
  amount: number
  bankAccount: string
  bankName: string
}

interface BalanceInfo {
  withdrawableAmount: number
  totalEarnings: number
  totalWithdrawn: number
}

interface CommissionInfo {
  totalCommission: number
  commissionRate: number
  monthlyCommission: number
}

export const getSettlementInfo = (params: SettlementParams) => {
  return request<any, PageResponse<SettlementRecord>>({
    url: '/finance/settlements',
    method: 'GET',
    params,
  })
}

export const getBalance = () => {
  return request<any, BalanceInfo>({
    url: '/finance/balance',
    method: 'GET',
  })
}

export const withdraw = (data: WithdrawParams) => {
  return request<any, { id: number }>({
    url: '/finance/withdraw',
    method: 'POST',
    data,
  })
}

export const getWithdrawRecords = (params: SettlementParams) => {
  return request<any, PageResponse<SettlementRecord>>({
    url: '/finance/withdraw-records',
    method: 'GET',
    params,
  })
}

export const getCommissionDetail = () => {
  return request<any, CommissionInfo>({
    url: '/finance/commission',
    method: 'GET',
  })
}
