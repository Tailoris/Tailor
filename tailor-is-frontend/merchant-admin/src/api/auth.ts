import request from './request'
import type { Merchant } from '@/types'

interface LoginParams {
  shopName: string
  username: string
  password: string
}

interface LoginResult {
  token: string
  userInfo: Merchant
}

export const adminLogin = (data: LoginParams) => {
  return request<any, LoginResult>({
    url: '/auth/login',
    method: 'POST',
    data,
  })
}

export const getAdminInfo = () => {
  return request<any, Merchant>({
    url: '/auth/info',
    method: 'GET',
  })
}

// 注册相关接口
export interface RegisterByPhoneParams {
  phone: string
  code: string
  password: string
  shopName: string
}

export interface RegisterByEmailParams {
  email: string
  code: string
  password: string
  shopName: string
}

export const registerByPhone = (data: RegisterByPhoneParams) => {
  return request<any, void>({
    url: '/auth/register/phone',
    method: 'POST',
    data,
  })
}

export const registerByEmail = (data: RegisterByEmailParams) => {
  return request<any, void>({
    url: '/auth/register/email',
    method: 'POST',
    data,
  })
}

export const sendSmsCode = (phone: string) => {
  return request<any, void>({
    url: '/auth/sms-code',
    method: 'POST',
    data: { phone },
  })
}

export const sendEmailCode = (email: string) => {
  return request<any, void>({
    url: '/auth/email-code',
    method: 'POST',
    data: { email },
  })
}

export const loginByCode = (data: { target: string; code: string; type: 'phone' | 'email' }) => {
  return request<any, { token: string; userInfo: Merchant }>({
    url: '/auth/login/code',
    method: 'POST',
    data,
  })
}

export const logout = () => {
  return request<any, void>({
    url: '/auth/logout',
    method: 'POST',
  })
}

// 找回密码相关接口
export const sendResetCode = (data: { target: string; type: 'phone' | 'email' }) => {
  return request<any, void>({
    url: '/auth/reset/code',
    method: 'POST',
    data,
  })
}

export const resetPassword = (data: { target: string; code: string; newPassword: string; type: 'phone' | 'email'; shopName: string }) => {
  return request<any, void>({
    url: '/auth/reset-password',
    method: 'POST',
    data,
  })
}
