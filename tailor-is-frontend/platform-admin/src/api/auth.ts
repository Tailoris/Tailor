import request from './request'

interface LoginParams {
  username: string
  password: string
}

interface SendCodeParams {
  target: string
  type: 'phone' | 'email'
}

export function login(data: LoginParams) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function sendVerificationCode(target: string, type: 'phone' | 'email') {
  return request({
    url: type === 'phone' ? '/auth/sms-code' : '/auth/email-code',
    method: 'post',
    data: type === 'phone' ? { phone: target } : { email: target }
  })
}

export function loginByCode(data: { target: string; code: string; type: 'phone' | 'email' }) {
  return request({
    url: '/auth/login/code',
    method: 'post',
    data
  })
}

export function sendResetCode(data: SendCodeParams) {
  return request({
    url: '/auth/reset/code',
    method: 'post',
    data
  })
}

export function resetPassword(data: { target: string; code: string; newPassword: string; type: 'phone' | 'email' }) {
  return request({
    url: '/auth/reset-password',
    method: 'post',
    data
  })
}
