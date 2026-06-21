import request from './request'

interface LoginParams {
  username: string
  password: string
}

interface SendCodeParams {
  target: string
  type: 'phone' | 'email'
}

interface AdminUserInfo {
  id: number
  username: string
  phone: string
  email: string
  realName: string
  roles: string[]
  permissions: string[]
}

interface LoginResult {
  token: string
  userInfo: AdminUserInfo
}

export function login(data: LoginParams) {
  return request<any, LoginResult>({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function sendVerificationCode(target: string, type: 'phone' | 'email') {
  return request<any, void>({
    url: type === 'phone' ? '/auth/sms-code' : '/auth/email-code',
    method: 'post',
    data: type === 'phone' ? { phone: target } : { email: target }
  })
}

export function loginByCode(data: { target: string; code: string; type: 'phone' | 'email' }) {
  return request<any, LoginResult>({
    url: '/auth/login/code',
    method: 'post',
    data
  })
}

export function sendResetCode(data: SendCodeParams) {
  return request<any, void>({
    url: '/auth/reset/code',
    method: 'post',
    data
  })
}

export function resetPassword(data: { target: string; code: string; newPassword: string; type: 'phone' | 'email' }) {
  return request<any, void>({
    url: '/auth/reset-password',
    method: 'post',
    data
  })
}
